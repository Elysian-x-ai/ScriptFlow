"use client";

import { useMemo, useState } from "react";
import { Maximize2, Minimize2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog";

interface ScriptPreviewProps {
  yamlContent: string;
}

interface ContentItem {
  type: string;
  character?: string;
  text: string;
}

interface SceneBlock {
  sceneNumber: number;
  title: string;
  location: string;
  time: string;
  atmosphere: string;
  content: ContentItem[];
}

interface ActBlock {
  actId: string;
  title: string;
  scenes: SceneBlock[];
}

interface ParsedScript {
  title: string;
  sourceNovel: string;
  characters: { name: string; description: string }[];
  acts: ActBlock[];
}

function unquote(s: string): string {
  if (!s) return "";
  if ((s.startsWith('"') && s.endsWith('"')) || (s.startsWith("'") && s.endsWith("'"))) {
    return s.slice(1, -1);
  }
  return s;
}

function yamlValue(line: string): string {
  const idx = line.indexOf(":");
  if (idx < 0) return "";
  return unquote(line.slice(idx + 1).trim());
}

function yamlKey(line: string): string {
  const idx = line.indexOf(":");
  if (idx < 0) return line.trim();
  return line.slice(0, idx).trim();
}

/**
 * Parse LLM-generated YAML into structured script data.
 *
 * The YAML uses 2-space indentation:
 *   0: meta:, characters:, acts:
 *   2:   title:, - id:, - act_id:
 *   4:     name:, description:, title:, scenes:
 *   6:       - scene_number:
 *   8:         title:, location:, time:, atmosphere:, content:
 *  10:           - type:
 *  12:             character_id:, text:
 */
function parseYamlPreview(yaml: string): ParsedScript {
  const result: ParsedScript = { title: "", sourceNovel: "", characters: [], acts: [] };
  const lines = yaml.split("\n");

  let section: "meta" | "characters" | "acts" | "none" = "none";
  let currentAct: ActBlock | null = null;
  let currentScene: SceneBlock | null = null;
  let inScenes = false;
  let inContent = false;

  for (const raw of lines) {
    const line = raw.replace(/\t/g, "  ");
    const trimmed = line.trim();
    if (!trimmed || trimmed.startsWith("#")) continue;
    const indent = line.length - trimmed.length;

    // === Root-level keys (indent 0) ===
    if (indent === 0 && trimmed.endsWith(":") && !trimmed.startsWith("-")) {
      const key = yamlKey(trimmed);
      // Flush previous act on section switch
      if (section === "acts" && currentAct) {
        result.acts.push(currentAct);
        currentAct = null;
        currentScene = null;
      }
      section = key === "meta" ? "meta" : key === "characters" ? "characters" : key === "acts" ? "acts" : "none";
      inScenes = false;
      inContent = false;
      continue;
    }

    // === Meta section (indent 2 fields) ===
    if (section === "meta" && indent === 2) {
      const key = yamlKey(trimmed);
      const val = yamlValue(trimmed);
      if (key === "title" && val) result.title = val;
      if (key === "source_novel" && val) result.sourceNovel = val;
      continue;
    }

    // === Characters section ===
    if (section === "characters") {
      // Array item: `- id: "char_xxx"` at indent 2
      // or `- name: "xxx"` at indent 2
      if (indent === 2 && trimmed.startsWith("- ")) {
        // Start of a new character
        const afterDash = trimmed.slice(2).trim();
        if (yamlKey(afterDash) === "name") {
          result.characters.push({ name: yamlValue(afterDash), description: "" });
        }
        continue;
      }
      // Fields within a character item (indent 4)
      if (indent === 4) {
        const key = yamlKey(trimmed);
        const val = yamlValue(trimmed);
        if (key === "name" && val) {
          result.characters.push({ name: val, description: "" });
        } else if (key === "description" && val && result.characters.length > 0) {
          const last = result.characters[result.characters.length - 1];
          if (!last.description) last.description = val;
        }
      }
      continue;
    }

    // === Acts section ===
    if (section === "acts") {
      // `  - act_id: "act_01"` at indent 2
      if (indent === 2 && trimmed.startsWith("- ") && yamlKey(trimmed.slice(2)) === "act_id") {
        if (currentAct) result.acts.push(currentAct);
        currentAct = { actId: yamlValue(trimmed), title: "", scenes: [] };
        currentScene = null;
        inScenes = false;
        inContent = false;
        continue;
      }
      if (!currentAct) continue;

      // Act title at indent 4 (before `scenes:` is encountered)
      if (indent === 4 && yamlKey(trimmed) === "title" && !inScenes) {
        currentAct.title = yamlValue(trimmed);
        continue;
      }

      // `scenes:` at indent 4
      if (indent === 4 && yamlKey(trimmed) === "scenes") {
        inScenes = true;
        inContent = false;
        continue;
      }

      // `      - scene_number: 1` at indent 6
      if (inScenes && indent === 6 && trimmed.startsWith("- ") && yamlKey(trimmed.slice(2)) === "scene_number") {
        if (currentScene && currentAct) currentAct.scenes.push(currentScene);
        currentScene = {
          sceneNumber: parseInt(yamlValue(trimmed)) || 0,
          title: "", location: "", time: "", atmosphere: "",
          content: [],
        };
        inContent = false;
        continue;
      }
      if (!currentScene) continue;

      // Scene fields at indent 8
      if (inScenes && indent === 8) {
        const key = yamlKey(trimmed);
        const val = yamlValue(trimmed);
        if (key === "title") currentScene.title = val;
        else if (key === "location") currentScene.location = val;
        else if (key === "time") currentScene.time = val;
        else if (key === "atmosphere") currentScene.atmosphere = val;
        else if (key === "content") inContent = true;
        else if (key === "present_char") {
          // Skip: `present_char: ["char_xxx"]` — inline array, not used in preview
        }
        continue;
      }

      // Content item: `          - type: action` at indent 10
      if (inContent && indent === 10 && trimmed.startsWith("- ")) {
        const afterDash = trimmed.slice(2).trim();
        const item: ContentItem = { type: "", character: "", text: "" };
        if (yamlKey(afterDash) === "type") {
          item.type = yamlValue(afterDash);
        }
        currentScene.content.push(item);
        continue;
      }

      // Content fields at indent 12: `character_id:` or `text:`
      if (inContent && indent === 12 && currentScene.content.length > 0) {
        const last = currentScene.content[currentScene.content.length - 1];
        const key = yamlKey(trimmed);
        const val = yamlValue(trimmed);
        if (key === "type") last.type = val;
        else if (key === "character_id") last.character = val;
        else if (key === "text") last.text = val;
        continue;
      }
    }
  }

  // Flush last items
  if (currentScene && currentAct) currentAct.scenes.push(currentScene);
  if (currentAct) result.acts.push(currentAct);

  return result;
}

function PreviewContent({ script }: { script: ParsedScript }) {
  return (
    <div className="space-y-6">
      {/* Title page header */}
      {script.title && (
        <div className="text-center mb-6 pb-4 border-b border-amber-300 dark:border-amber-700">
          <h2 className="text-2xl font-bold tracking-tight">{script.title}</h2>
          {script.sourceNovel && (
            <p className="text-sm text-muted-foreground mt-1">改编自《{script.sourceNovel}》</p>
          )}
        </div>
      )}

      {/* Character list */}
      {script.characters.length > 0 && (
        <div className="mb-6">
          <h3 className="text-xs font-bold uppercase tracking-widest text-muted-foreground mb-2 border-b pb-1">
            角色表
          </h3>
          <div className="grid grid-cols-2 gap-x-4 gap-y-1 text-sm">
            {script.characters.map((c, i) => (
              <div key={i} className="flex items-baseline gap-1">
                <span className="font-semibold">{c.name}</span>
                {c.description && (
                  <span className="text-muted-foreground text-xs">—— {c.description}</span>
                )}
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Acts */}
      {script.acts.map((act, ai) => (
        <div key={ai} className="mb-8">
          {/* Act heading */}
          <div className="text-center mb-4">
            <h3 className="text-lg font-bold uppercase tracking-wider">
              {act.title || `第${ai + 1}幕`}
            </h3>
            <div className="w-16 h-0.5 bg-amber-400 dark:bg-amber-600 mx-auto mt-2" />
          </div>

          {act.scenes.length === 0 ? (
            <p className="text-sm text-muted-foreground italic text-center">
              （场景内容生成中...）
            </p>
          ) : (
            act.scenes.map((scene, si) => (
              <div key={si} className="mb-6">
                {/* Scene heading — proper screenplay format */}
                <div className="uppercase text-center mb-3">
                  <p className="text-sm font-bold tracking-wider">
                    {scene.location || "场景"} {scene.time ? `- ${scene.time}` : ""}
                  </p>
                  {scene.sceneNumber ? (
                    <p className="text-xs text-muted-foreground">第{scene.sceneNumber}场</p>
                  ) : null}
                  {scene.atmosphere && (
                    <p className="text-xs text-muted-foreground italic">{scene.atmosphere}</p>
                  )}
                </div>
                {scene.title && (
                  <p className="text-sm italic text-center text-muted-foreground mb-3">{scene.title}</p>
                )}

                {/* Scene content */}
                <div className="space-y-2 max-w-2xl mx-auto">
                  {scene.content.map((item, ci) => {
                    if (item.type === "action") {
                      return (
                        <p key={ci} className="text-sm leading-relaxed text-gray-700 dark:text-gray-300 italic">
                          {item.text}
                        </p>
                      );
                    }
                    if (item.type === "dialogue") {
                      return (
                        <div key={ci} className="ml-8 mb-2">
                          <p className="text-sm font-bold uppercase tracking-wide text-gray-800 dark:text-gray-200">
                            {item.character || "(未知角色)"}
                          </p>
                          <p className="text-sm ml-4 text-gray-600 dark:text-gray-400 max-w-lg">
                            {item.text}
                          </p>
                        </div>
                      );
                    }
                    if (item.type === "parenthetical") {
                      return (
                        <p key={ci} className="text-xs italic text-gray-500 ml-16 mb-1">
                          ({item.text || item.character || ""})
                        </p>
                      );
                    }
                    return null;
                  })}
                </div>
              </div>
            ))
          )}
        </div>
      ))}

      {/* Empty state */}
      {script.acts.length === 0 && script.title && (
        <p className="text-sm text-muted-foreground italic text-center">
          剧本结构已生成，正在等待场景内容...
        </p>
      )}
    </div>
  );
}

export default function ScriptPreview({ yamlContent }: ScriptPreviewProps) {
  const script = useMemo(() => parseYamlPreview(yamlContent), [yamlContent]);
  const [fullscreen, setFullscreen] = useState(false);

  const hasContent = yamlContent && yamlContent.trim() !== "";
  const isParsed = script.title || script.acts.length > 0 || script.characters.length > 0;

  return (
    <>
      <div className="flex flex-col h-full">
        {/* Preview panel header with fullscreen button */}
        <div className="flex items-center justify-between px-3 py-1.5 border-b bg-card shrink-0">
          <h3 className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">
            剧本预览
          </h3>
          {hasContent && isParsed && (
            <Button variant="ghost" size="sm" className="h-6 w-6 p-0" onClick={() => setFullscreen(true)} title="全屏预览">
              <Maximize2 className="h-3.5 w-3.5" />
            </Button>
          )}
        </div>
        <div className="flex-1 overflow-auto p-6 font-serif bg-gradient-to-b from-amber-50 to-white dark:from-amber-950/20 dark:to-background">
          {!hasContent ? (
            <p className="text-sm text-muted-foreground italic text-center pt-8">
              在编辑器中编写 YAML 剧本，此处将显示预览
            </p>
          ) : !isParsed ? (
            <p className="text-xs text-muted-foreground text-center pt-8">
              无法解析预览 — 请先生成剧本，或检查 YAML 格式是否符合剧本 Schema
            </p>
          ) : (
            <PreviewContent script={script} />
          )}
        </div>
      </div>

      {/* Fullscreen dialog */}
      <Dialog open={fullscreen} onOpenChange={setFullscreen}>
        <DialogContent className="max-w-5xl max-h-[90vh]">
          <DialogHeader className="flex flex-row items-center justify-between">
            <DialogTitle>剧本预览</DialogTitle>
            <Button variant="ghost" size="sm" className="h-7 w-7 p-0" onClick={() => setFullscreen(false)}>
              <Minimize2 className="h-3.5 w-3.5" />
            </Button>
          </DialogHeader>
          <div className="overflow-auto" style={{ maxHeight: "calc(90vh - 80px)" }}>
            <PreviewContent script={script} />
          </div>
        </DialogContent>
      </Dialog>
    </>
  );
}
