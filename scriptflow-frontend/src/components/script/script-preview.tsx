"use client";

import { useMemo } from "react";

interface ScriptPreviewProps {
  yamlContent: string;
}

interface SceneBlock {
  sceneNumber: number;
  title: string;
  location: string;
  time: string;
  atmosphere: string;
  content: { type: string; character?: string; text: string }[];
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

function extractValue(line: string): string {
  const m = line.match(/:\s*["']?(.*?)["']?\s*$/);
  return m ? m[1].replace(/["']$/, "").trim() : "";
}

function parseYamlPreview(yaml: string): ParsedScript {
  const result: ParsedScript = { title: "", sourceNovel: "", characters: [], acts: [] };
  const lines = yaml.split("\n");
  let inChars = false, inActs = false, inScenes = false, inContent = false;
  let currentAct: ActBlock | null = null;
  let currentScene: SceneBlock | null = null;

  for (const raw of lines) {
    const line = raw.replace(/\t/g, "  ");
    const indent = line.length - line.trimStart().length;
    const trimmed = line.trim();
    if (!trimmed || trimmed.startsWith("#")) continue;

    // Top-level keys
    if (indent === 0 && trimmed.endsWith(":")) {
      const key = trimmed.replace(":", "");
      inChars = key === "characters";
      inActs = key === "acts";
      if (!inChars && !inActs) { inChars = false; inActs = false; inScenes = false; inContent = false; }
      continue;
    }

    // Meta fields
    if (indent === 2 && !inChars && !inActs) {
      if (trimmed.startsWith("title:")) result.title = extractValue(trimmed);
      if (trimmed.startsWith("source_novel:")) result.sourceNovel = extractValue(trimmed);
    }

    // Characters list
    if (inChars && indent === 4 && trimmed.startsWith("- name:")) {
      result.characters.push({ name: extractValue(trimmed), description: "" });
    }
    if (inChars && indent === 6 && trimmed.startsWith("description:") && result.characters.length > 0) {
      result.characters[result.characters.length - 1].description = extractValue(trimmed);
    }

    // Acts
    if (inActs && indent === 4 && trimmed.startsWith("- act_id:")) {
      if (currentAct) result.acts.push(currentAct);
      currentAct = { actId: extractValue(trimmed), title: "", scenes: [] };
      inScenes = false;
    }
    if (inActs && indent === 6 && trimmed.startsWith("title:") && currentAct) {
      currentAct.title = extractValue(trimmed);
    }
    if (inActs && indent === 6 && trimmed.startsWith("scenes:")) {
      inScenes = true;
    }

    // Scenes
    if (inScenes && indent === 8 && trimmed.startsWith("- scene_number:")) {
      if (currentScene && currentAct) currentAct.scenes.push(currentScene);
      currentScene = {
        sceneNumber: parseInt(extractValue(trimmed)) || 0,
        title: "", location: "", time: "", atmosphere: "",
        content: [],
      };
      inContent = false;
    }
    if (inScenes && indent === 10 && currentScene) {
      if (trimmed.startsWith("title:")) currentScene.title = extractValue(trimmed);
      if (trimmed.startsWith("location:")) currentScene.location = extractValue(trimmed);
      if (trimmed.startsWith("time:")) currentScene.time = extractValue(trimmed);
      if (trimmed.startsWith("atmosphere:")) currentScene.atmosphere = extractValue(trimmed);
      if (trimmed.startsWith("content:")) inContent = true;
    }

    // Scene content (action / dialogue)
    if (inContent && indent === 12 && currentScene) {
      if (trimmed.startsWith("- type:")) {
        const t = extractValue(trimmed);
        currentScene.content.push({ type: t, character: "", text: "" });
      }
    }
    if (inContent && indent === 14 && currentScene && currentScene.content.length > 0) {
      const last = currentScene.content[currentScene.content.length - 1];
      if (trimmed.startsWith("character_id:")) last.character = extractValue(trimmed);
      if (trimmed.startsWith("text:")) last.text = extractValue(trimmed);
    }
  }

  // Flush last act/scene
  if (currentScene && currentAct) currentAct.scenes.push(currentScene);
  if (currentAct) result.acts.push(currentAct);

  return result;
}

export default function ScriptPreview({ yamlContent }: ScriptPreviewProps) {
  const script = useMemo(() => parseYamlPreview(yamlContent), [yamlContent]);

  return (
    <div className="p-4 overflow-auto font-serif bg-amber-50 dark:bg-amber-950/20 h-full" style={{ maxHeight: "100%" }}>
      <h3 className="text-xs font-semibold text-muted-foreground uppercase tracking-wider mb-3">
        剧本预览
      </h3>
      {!yamlContent || yamlContent.trim() === "" ? (
        <p className="text-sm text-muted-foreground italic">
          在编辑器中编写 YAML 剧本，此处将显示预览
        </p>
      ) : !script.title && script.acts.length === 0 ? (
        <p className="text-xs text-muted-foreground">
          （无法解析预览 - 请检查 YAML 格式是否符合剧本 Schema）
        </p>
      ) : (
        <div className="space-y-4">
          {/* Title */}
          {script.title && (
            <div>
              <h2 className="text-lg font-bold text-center">{script.title}</h2>
              {script.sourceNovel && (
                <p className="text-xs text-center text-muted-foreground">改编自: {script.sourceNovel}</p>
              )}
            </div>
          )}

          {/* Characters */}
          {script.characters.length > 0 && (
            <div className="border-b pb-2">
              <h3 className="text-xs font-bold uppercase tracking-wider text-muted-foreground mb-1">角色</h3>
              <div className="flex flex-wrap gap-2">
                {script.characters.map((c, i) => (
                  <span key={i} className="text-xs bg-white dark:bg-amber-900/30 px-2 py-0.5 rounded border">
                    {c.name}{c.description ? ` - ${c.description}` : ""}
                  </span>
                ))}
              </div>
            </div>
          )}

          {/* Acts */}
          {script.acts.map((act, ai) => (
            <div key={ai}>
              <h3 className="text-sm font-bold border-b border-amber-300 dark:border-amber-700 pb-1 mb-2">
                {act.title || `第${ai + 1}幕`}
              </h3>

              {act.scenes.map((scene, si) => (
                <div key={si} className="mb-4">
                  {/* Scene heading */}
                  <div className="flex items-baseline gap-2 mb-1">
                    <span className="text-xs font-bold uppercase text-gray-500">
                      第{scene.sceneNumber}场
                    </span>
                    <span className="text-xs text-muted-foreground">
                      {[scene.location, scene.time, scene.atmosphere].filter(Boolean).join(" - ")}
                    </span>
                  </div>
                  {scene.title && (
                    <p className="text-xs italic text-muted-foreground mb-1">{scene.title}</p>
                  )}

                  {/* Scene content */}
                  <div className="space-y-1.5 ml-2">
                    {scene.content.map((item, ci) => {
                      if (item.type === "action") {
                        return (
                          <p key={ci} className="text-sm leading-relaxed text-gray-700 dark:text-gray-300">
                            {item.text}
                          </p>
                        );
                      }
                      if (item.type === "dialogue") {
                        return (
                          <div key={ci}>
                            <p className="text-sm font-bold uppercase text-gray-800 dark:text-gray-200">
                              {item.character || "(未知角色)"}
                            </p>
                            <p className="text-sm ml-4 text-gray-600 dark:text-gray-400">
                              {item.text}
                            </p>
                          </div>
                        );
                      }
                      return null;
                    })}
                  </div>
                </div>
              ))}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
