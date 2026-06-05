"use client";

import { useMemo } from "react";

interface ScriptPreviewProps {
  yamlContent: string;
}

function parsePreview(yaml: string) {
  const lines = yaml.split("\n");
  const sections: { type: string; text: string; character?: string }[] = [];

  for (const line of lines) {
    const trimmed = line.trim();
    if (!trimmed) continue;

    if (trimmed.startsWith("title:") && !trimmed.startsWith("  ")) {
      // Scene title found
    }

    // Dialogue: character name in ALL CAPS followed by dialogue
    const dialogueMatch = trimmed.match(/character_id:\s*["']?(\w+)["']?/);
    if (dialogueMatch) {
      const textLine = lines[lines.indexOf(line) + 1];
      if (textLine && textLine.includes("text:")) {
        const text = textLine.replace(/.*text:\s*["']?/, "").replace(/["']?$/, "");
        sections.push({
          type: "dialogue",
          character: dialogueMatch[1],
          text,
        });
      }
    }

    // Action
    if (trimmed.startsWith("type: action") || trimmed.startsWith('- type: action')) {
      const nextLine = lines[lines.indexOf(line) + 1];
      if (nextLine) {
        const text = nextLine.replace(/.*text:\s*["']?/, "").replace(/["']?$/, "");
        sections.push({ type: "action", text });
      }
    }
  }

  return sections;
}

export default function ScriptPreview({ yamlContent }: ScriptPreviewProps) {
  const preview = useMemo(() => parsePreview(yamlContent), [yamlContent]);

  return (
    <div className="p-4 overflow-auto font-serif bg-amber-50 dark:bg-amber-950/20 h-full" style={{ maxHeight: "100%" }}>
      <h3 className="text-xs font-semibold text-muted-foreground uppercase tracking-wider mb-3">
        剧本预览
      </h3>
      {!yamlContent || yamlContent.trim() === "" ? (
        <p className="text-sm text-muted-foreground italic">
          在编辑器中编写 YAML 剧本，此处将显示预览
        </p>
      ) : (
        <div className="space-y-3">
          {preview.length === 0 ? (
            <p className="text-xs text-muted-foreground">
              （无法解析预览 - 请检查 YAML 格式是否符合剧本 Schema）
            </p>
          ) : (
            preview.map((s, i) => {
              if (s.type === "action") {
                return (
                  <p key={i} className="text-sm leading-relaxed text-gray-700 dark:text-gray-300">
                    {s.text}
                  </p>
                );
              }
              if (s.type === "dialogue") {
                return (
                  <div key={i}>
                    <p className="text-sm font-bold uppercase text-gray-800 dark:text-gray-200">
                      {s.character}
                    </p>
                    <p className="text-sm ml-4 text-gray-600 dark:text-gray-400">
                      {s.text}
                    </p>
                  </div>
                );
              }
              return null;
            })
          )}
        </div>
      )}
    </div>
  );
}
