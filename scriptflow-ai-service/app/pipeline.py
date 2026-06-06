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
