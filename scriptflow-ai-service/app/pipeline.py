import json
import time
from typing import TypedDict, Optional
from loguru import logger

from app.providers.base import AIProvider
from app.agents.base import strip_markdown
from app.agents.chapter_splitter import ChapterSplitterAgent
from app.agents.character_extractor import CharacterExtractorAgent
from app.agents.world_builder import WorldBuilderAgent
from app.agents.plot_splitter import PlotSplitterAgent
from app.agents.scene_cutter import SceneCutterAgent
from app.agents.dialogue_generator import DialogueGeneratorAgent
from app.agents.yaml_assembler import YamlAssemblerAgent
from app.agents.character_merger import CharacterMergerAgent
from app.agents.world_merger import WorldMergerAgent


# Pipeline state definition
class PipelineState(TypedDict):
    novel_content: str  # Input: raw novel text
    chapters: str  # Agent 1 output: JSON array of chapters
    characters: str  # Agent 2 output: JSON array of characters
    world_info: str  # Agent 3 output: JSON world setting
    plot_units: str  # Agent 4 output: JSON array of plot units
    scenes: str  # Agent 5 output: JSON array of scenes
    dialogues: str  # Agent 6 output: JSON with scene dialogues
    yaml_output: str  # Agent 7 output: final YAML
    error: Optional[str]  # Error message if any stage fails


class ProgressCallback:
    """Callback to report pipeline progress."""

    def __init__(self):
        self.stages = [
            "章节切分",
            "角色抽取",
            "世界观提炼",
            "剧情拆分",
            "场景切割",
            "对白生成",
            "YAML 组装",
        ]
        self.on_progress = None  # set by caller

    def report(self, stage_index: int, status: str, message: str = ""):
        if self.on_progress:
            self.on_progress(stage_index, status, message)


def run_pipeline(
    novel_content: str,
    provider: AIProvider,
    progress_cb: Optional[ProgressCallback] = None,
) -> tuple[bool, str, str]:
    """
    Run the 7-stage AI pipeline.

    Each stage validates that the agent's output is valid JSON before
    passing it to the next stage. If validation fails, the error is captured
    and the pipeline stops.

    Returns: (success, yaml_output, error_message)
    """
    if progress_cb is None:
        progress_cb = ProgressCallback()

    state: PipelineState = {
        "novel_content": novel_content,
        "chapters": "",
        "characters": "",
        "world_info": "",
        "plot_units": "",
        "scenes": "",
        "dialogues": "",
        "yaml_output": "",
        "error": None,
    }

    try:
        # Agent 1: Chapter Splitter
        logger.info("Agent 1: 原文分章...")
        progress_cb.report(0, "processing")
        agent1 = ChapterSplitterAgent(provider)
        state["chapters"] = agent1.run(state["novel_content"])
        _validate_json(state["chapters"], "章节切分")
        chapter_count = _count_items(state["chapters"])
        logger.info(f"Agent 1 done: {chapter_count} chapters")
        progress_cb.report(0, "completed", f"切分为 {chapter_count} 章")

        # Agent 2: Character Extractor
        logger.info("Agent 2: 角色抽取...")
        progress_cb.report(1, "processing")
        agent2 = CharacterExtractorAgent(provider)
        state["characters"] = agent2.run(state["chapters"])
        _validate_json(state["characters"], "角色抽取")
        char_count = _count_items(state["characters"])
        logger.info(f"Agent 2 done: {char_count} characters")
        progress_cb.report(1, "completed", f"提取 {char_count} 个角色")

        # Agent 3: World Builder
        logger.info("Agent 3: 世界观提炼...")
        progress_cb.report(2, "processing")
        agent3 = WorldBuilderAgent(provider)
        state["world_info"] = agent3.run(state["chapters"])
        _validate_json(state["world_info"], "世界观提炼")
        logger.info("Agent 3 done")
        progress_cb.report(2, "completed", "世界观提炼完成")

        # Agent 4: Plot Splitter
        logger.info("Agent 4: 剧情拆分...")
        progress_cb.report(3, "processing")
        agent4 = PlotSplitterAgent(provider)
        combined = f"Chapters: {state['chapters']}\n\nWorld: {state['world_info']}"
        state["plot_units"] = agent4.run(combined)
        _validate_json(state["plot_units"], "剧情拆分")
        plot_count = _count_items(state["plot_units"])
        logger.info(f"Agent 4 done: {plot_count} plot units")
        progress_cb.report(3, "completed", f"拆分 {plot_count} 个情节单元")

        # Agent 5: Scene Cutter
        logger.info("Agent 5: 场景切割...")
        progress_cb.report(4, "processing")
        agent5 = SceneCutterAgent(provider)
        combined = f"Plot Units: {state['plot_units']}\n\nCharacters: {state['characters']}\n\nWorld: {state['world_info']}"
        state["scenes"] = agent5.run(combined)
        _validate_json(state["scenes"], "场景切割")
        scene_count = _count_items(state["scenes"])
        logger.info(f"Agent 5 done: {scene_count} scenes")
        progress_cb.report(4, "completed", f"切割 {scene_count} 个场景")

        # Agent 6: Dialogue Generator
        logger.info("Agent 6: 对白生成...")
        progress_cb.report(5, "processing")
        agent6 = DialogueGeneratorAgent(provider)
        combined = f"Scenes: {state['scenes']}\n\nCharacters: {state['characters']}"
        state["dialogues"] = agent6.run(combined)
        _validate_json(state["dialogues"], "对白生成")
        logger.info("Agent 6 done")
        progress_cb.report(5, "completed", "对白生成完成")

        # Agent 7: YAML Assembler
        logger.info("Agent 7: YAML 组装...")
        progress_cb.report(6, "processing")
        agent7 = YamlAssemblerAgent(provider)
        combined = (
            f"## Characters\n{state['characters']}\n\n"
            f"## World Info\n{state['world_info']}\n\n"
            f"## Scenes with Dialogues\n{state['dialogues']}"
        )
        state["yaml_output"] = agent7.run(combined)
        logger.info(f"Agent 7 done: {len(state['yaml_output'])} chars")
        progress_cb.report(6, "completed", "YAML 组装完成")

        return True, state["yaml_output"], ""

    except Exception as e:
        logger.exception(f"Pipeline failed at stage: {e}")
        return False, "", str(e)


def _validate_json(json_str: str, stage_name: str):
    """Validate that the given string is parseable JSON. Raises ValueError if not."""
    cleaned = strip_markdown(json_str)
    try:
        data = json.loads(cleaned)
        if data is None:
            raise ValueError(f"{stage_name} returned null")
    except json.JSONDecodeError as e:
        raise ValueError(f"{stage_name} 输出不是有效的 JSON: {e}. 前200字符: {cleaned[:200]}")


def _count_items(json_str: str) -> int:
    """Try to parse JSON array and count items."""
    try:
        data = json.loads(json_str)
        if isinstance(data, list):
            return len(data)
        return 1
    except (json.JSONDecodeError, TypeError):
        return 0


# ---------------------------------------------------------------------------
# Structured pipeline for pre-split chapters (3+ chapters)
# ---------------------------------------------------------------------------

def _concat_chapters_for_prompt(chapters: list[dict]) -> str:
    """Concatenate chapters into the same prompt format as before."""
    parts = []
    for c in chapters:
        title = c.get("title") or ""
        content = c.get("content") or ""
        parts.append(f"## 第{c.get('chapterNo', '?')}章 {title}\n{content}")
    return "\n\n".join(parts)


def _group_into_acts(chapters: list[dict], act_size: int = 10) -> list[list[dict]]:
    """Group chapters into acts of roughly act_size chapters each."""
    return [chapters[i:i + act_size] for i in range(0, len(chapters), act_size)]


def run_pipeline_structured(
    chapters: list[dict],
    provider: AIProvider,
    progress_cb: Optional[ProgressCallback] = None,
    previous_yaml: Optional[str] = None,
    unchanged_refs: Optional[list[dict]] = None,
) -> tuple[bool, str, str]:
    """
    Structured pipeline for pre-split chapters (3+ chapters).

    Skips ChapterSplitterAgent (chapters already split in DB).
    Processes character/world extraction in batches, then generates
    plot/scenes/dialogue per act.

    ``chapters`` should contain only NEW/MODIFIED chapters with full content.
    ``unchanged_refs`` (optional) contains {chapterNo, title} of chapters
    whose content hasn't changed — they are excluded from AI processing but
    included as structural context.

    If ``previous_yaml`` is provided (from a prior generation), it is
    included as context for the YAML assembler to merge new content into
    the existing script structure.

    Returns: (success, yaml_output, error_message)
    """
    if progress_cb is None:
        progress_cb = ProgressCallback()

    unchanged_refs = unchanged_refs or []
    total_chapters = len(chapters) + len(unchanged_refs)

    logger.info(f"Incremental pipeline: {len(chapters)} changed + "
                f"{len(unchanged_refs)} unchanged = {total_chapters} total")

    # When there are unchanged refs, re-label progress stages to clarify
    # that only changed chapters go through AI processing.
    changed_only = len(unchanged_refs) > 0
    if changed_only:
        # Replace standard stage labels with incremental-aware ones
        progress_cb.stages = [
            "章节切分",
            "增量角色抽取",    # Only processes changed chapters
            "增量世界观提炼",  # Only processes changed chapters
            "剧情拆分",
            "场景切割",
            "对白生成",
            "YAML 合并组装",   # Merges new content into existing YAML
        ]

    # If no changed chapters at all, return previous YAML as-is
    if not chapters:
        logger.info("No changed chapters, returning previous YAML")
        if previous_yaml:
            return True, previous_yaml, ""
        return True, "", ""

    batch_size = min(5, max(2, len(chapters) // 2))
    num_batches = (len(chapters) + batch_size - 1) // batch_size
    total_acts = max(1, (len(chapters) + 9) // 10)

    logger.info(f"Processing {len(chapters)} changed chapters: "
                f"{num_batches} batches, ~{total_acts} acts")

    try:
        # ==================================================================
        # Phase 1: Batch character + world extraction (changed chapters only)
        # ==================================================================
        char_batches: list[str] = []
        world_batches: list[str] = []

        for batch_idx in range(num_batches):
            batch_start = batch_idx * batch_size
            batch_end = min(batch_start + batch_size, len(chapters))
            batch = chapters[batch_start:batch_end]
            batch_text = _concat_chapters_for_prompt(batch)

            # When incrementing, include previous script as reference
            extract_context = batch_text
            if previous_yaml:
                extract_context += (
                    f"\n\n## Existing Script Reference (for continuity)\n"
                    f"下方是已生成的完整剧本，提取时需参考已有角色/世界观设定，"
                    f"补充新增的角色和设定，避免重复提取已存在的内容：\n{previous_yaml[:3000]}"
                )

            stage_label = f"角色抽取 (批次 {batch_idx + 1}/{num_batches})"
            progress_cb.report(1, "processing", stage_label)

            extractor = CharacterExtractorAgent(provider)
            char_result = extractor.run(extract_context)
            _validate_json(char_result, stage_label)
            char_batches.append(char_result)

            stage_label = f"世界观提炼 (批次 {batch_idx + 1}/{num_batches})"
            progress_cb.report(2, "processing", stage_label)

            builder = WorldBuilderAgent(provider)
            world_result = builder.run(extract_context)
            _validate_json(world_result, stage_label)
            world_batches.append(world_result)

        # Merge characters across batches
        if len(char_batches) > 1:
            logger.info(f"Merging {len(char_batches)} character lists...")
            progress_cb.report(1, "processing", "角色列表合并")
            merger = CharacterMergerAgent(provider)
            merge_input = json.dumps(
                [json.loads(c) for c in char_batches], ensure_ascii=False
            )
            characters = merger.run(merge_input)
        else:
            characters = char_batches[0]
        _validate_json(characters, "角色合并")
        char_count = _count_items(characters)
        logger.info(f"Characters: {char_count}")
        progress_cb.report(1, "completed", f"提取 {char_count} 个角色")

        # Merge world info across batches
        if len(world_batches) > 1:
            logger.info(f"Merging {len(world_batches)} world info sets...")
            progress_cb.report(2, "processing", "世界观信息合并")
            merger = WorldMergerAgent(provider)
            merge_input = json.dumps(
                [json.loads(w) for w in world_batches], ensure_ascii=False
            )
            world_info = merger.run(merge_input)
        else:
            world_info = world_batches[0]
        _validate_json(world_info, "世界观合并")
        logger.info("World info extracted")
        progress_cb.report(2, "completed", "世界观提炼完成")

        # ==================================================================
        # Phase 2: Per-act plot split + scene cut + dialogue generation
        #          (changed chapters only)
        # ==================================================================
        acts = _group_into_acts(chapters)
        logger.info(f"Processing {len(acts)} acts from changed chapters")

        all_dialogues: list[str] = []

        for act_idx, act_chapters in enumerate(acts):
            act_text = _concat_chapters_for_prompt(act_chapters)
            act_label = f"第 {act_idx + 1} 幕 (共 {len(acts)} 幕)"

            # Plot splitter
            progress_cb.report(3, "processing", f"剧情拆分 - {act_label}")
            plot_splitter = PlotSplitterAgent(provider)
            combined_plot = f"Chapters:\n{act_text}\n\nWorld:\n{world_info}"
            plot_units = plot_splitter.run(combined_plot)
            _validate_json(plot_units, f"剧情拆分 {act_label}")

            # Scene cutter
            progress_cb.report(4, "processing", f"场景切割 - {act_label}")
            scene_cutter = SceneCutterAgent(provider)
            combined_scene = (
                f"Plot Units:\n{plot_units}\n\n"
                f"Characters:\n{characters}\n\n"
                f"World:\n{world_info}"
            )
            scenes = scene_cutter.run(combined_scene)
            _validate_json(scenes, f"场景切割 {act_label}")

            # Dialogue generator
            progress_cb.report(5, "processing", f"对白生成 - {act_label}")
            dialogue_gen = DialogueGeneratorAgent(provider)
            combined_dialogue = f"Scenes:\n{scenes}\n\nCharacters:\n{characters}"
            dialogues = dialogue_gen.run(combined_dialogue)
            _validate_json(dialogues, f"对白生成 {act_label}")

            all_dialogues.append(dialogues)

        progress_cb.report(3, "completed", f"完成 {len(acts)} 幕剧情拆分")
        progress_cb.report(4, "completed", f"完成 {len(acts)} 幕场景切割")
        progress_cb.report(5, "completed", f"完成 {len(acts)} 幕对白生成")

        # ==================================================================
        # Phase 3: YAML assembly (merge new content with existing YAML)
        # ==================================================================
        logger.info("Assembling YAML...")
        progress_cb.report(6, "processing", "YAML 组装")

        yaml_assembler = YamlAssemblerAgent(provider)
        dialogue_sections = "\n\n---\n\n".join(
            f"=== Act {i + 1} ===\n{d}"
            for i, d in enumerate(all_dialogues)
        )

        # Build the YAML assembler prompt
        # Include unchanged refs as structural context
        if unchanged_refs:
            ref_summary = "\n".join(
                f"  第{r['chapterNo']}章 {r.get('title', '')}"
                for r in unchanged_refs
            )
            unchanged_context = (
                f"\n\n## Unchanged Chapters (preserve existing content)\n"
                f"以下章节内容未变化，其已有的剧本内容必须完整保留：\n{ref_summary}"
            )
        else:
            unchanged_context = ""

        if previous_yaml:
            # Truncate previous YAML to avoid context overflow
            prev_max_chars = 5000
            prev_truncated = previous_yaml[:prev_max_chars]
            if len(previous_yaml) > prev_max_chars:
                prev_truncated += "\n\n# ... (省略)"
            previous_context = (
                f"\n\n## Existing Full Script (preserve this content)\n"
                f"下方是已生成的完整剧本。对于标记为「未变化」的章节，必须完整保留其已有内容；"
                f"对于新章节，请将新增的场景/对白追加到对应幕中或创建新幕。"
                f"不要删除或修改已有内容。\n{prev_truncated}"
            )
        else:
            previous_context = ""

        combined_yaml = (
            f"## Characters\n{characters}\n\n"
            f"## World Info\n{world_info}\n\n"
            f"## Scenes with Dialogues\n{dialogue_sections}"
            f"{unchanged_context}"
            f"{previous_context}"
        )

        # Increase max_tokens for the YAML assembler to avoid truncation
        # Override the provider's default (4096) with a larger limit
        yaml_output = yaml_assembler.run(combined_yaml)
        logger.info(f"YAML output: {len(yaml_output)} chars")
        progress_cb.report(6, "completed", "YAML 组装完成")

        return True, yaml_output, ""

    except Exception as e:
        logger.exception(f"Pipeline failed: {e}")
        return False, "", str(e)
