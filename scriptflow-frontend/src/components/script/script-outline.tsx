"use client";

import { useState, useMemo } from "react";

interface OutlineItem {
  key: string;
  label: string;
  type: "act" | "scene" | "character" | "meta";
  children?: OutlineItem[];
  lineNumber?: number;
}

interface ScriptOutlineProps {
  yamlContent: string;
  onNavigate?: (lineNumber: number) => void;
}

function parseOutline(yamlContent: string): OutlineItem[] {
  const items: OutlineItem[] = [];
  const lines = yamlContent.split("\n");

  let currentAct: OutlineItem | null = null;

  for (let i = 0; i < lines.length; i++) {
    const line = lines[i];
    const trimmed = line.trim();

    if (trimmed.startsWith("meta:")) {
      items.push({ key: "meta", label: "meta (元信息)", type: "meta", lineNumber: i });
    } else if (trimmed.startsWith("characters:")) {
      items.push({ key: "characters", label: "characters (角色列表)", type: "character", lineNumber: i });
    } else if (trimmed.startsWith("acts:")) {
      // acts container, scan for act items
      const actItem: OutlineItem = {
        key: "acts",
        label: "acts (幕)",
        type: "act",
        children: [],
        lineNumber: i,
      };
      items.push(actItem);
      currentAct = actItem;
    } else if (currentAct && trimmed.startsWith("- act_id:") || (currentAct && /^\s*-\s+act_id:/.test(trimmed))) {
      // This is an act entry under acts
      // The act title might be on the next few lines
      // For simplicity, scan ahead to find title
      let title = "Act";
      for (let j = i; j < Math.min(i + 10, lines.length); j++) {
        const titleMatch = lines[j].match(/title:\s*(.+)/);
        if (titleMatch) {
          title = titleMatch[1].trim();
          break;
        }
      }
      const actChild: OutlineItem = {
        key: `act_${i}`,
        label: title,
        type: "act",
        children: [],
        lineNumber: i,
      };
      currentAct?.children?.push(actChild);
    } else if (trimmed.startsWith("scene_number:") || trimmed.startsWith("- scene_number:")) {
      // Scene entry
      let sceneTitle = "Scene";
      for (let j = i; j < Math.min(i + 10, lines.length); j++) {
        const tMatch = lines[j].match(/title:\s*(.+)/);
        if (tMatch) {
          sceneTitle = tMatch[1].trim();
          break;
        }
      }
      if (currentAct && currentAct.children && currentAct.children.length > 0) {
        const lastAct = currentAct.children[currentAct.children.length - 1];
        if (!lastAct.children) lastAct.children = [];
        lastAct.children.push({
          key: `scene_${i}`,
          label: sceneTitle,
          type: "scene",
          lineNumber: i,
        });
      }
    }
  }

  return items;
}

export default function ScriptOutline({ yamlContent, onNavigate }: ScriptOutlineProps) {
  const [collapsed, setCollapsed] = useState<Set<string>>(new Set());

  const outline = useMemo(() => parseOutline(yamlContent), [yamlContent]);

  const toggleCollapse = (key: string) => {
    setCollapsed((prev) => {
      const next = new Set(prev);
      if (next.has(key)) next.delete(key);
      else next.add(key);
      return next;
    });
  };

  const renderItem = (item: OutlineItem, depth: number = 0) => {
    const isCollapsed = collapsed.has(item.key);
    const hasChildren = item.children && item.children.length > 0;

    return (
      <div key={item.key}>
        <div
          className="flex items-center gap-1 px-2 py-1 rounded cursor-pointer hover:bg-accent text-sm transition-colors"
          style={{ paddingLeft: `${12 + depth * 16}px` }}
          onClick={() => {
            if (hasChildren) toggleCollapse(item.key);
            else if (item.lineNumber !== undefined && onNavigate) onNavigate(item.lineNumber);
          }}
        >
          {hasChildren && (
            <span className="text-xs text-muted-foreground w-3">
              {isCollapsed ? "▶" : "▼"}
            </span>
          )}
          {!hasChildren && <span className="w-3" />}
          <span
            className={`text-xs ${
              item.type === "act"
                ? "text-orange-600 font-medium"
                : item.type === "scene"
                ? "text-blue-600"
                : item.type === "character"
                ? "text-green-600"
                : "text-muted-foreground"
            }`}
          >
            {item.label}
          </span>
        </div>
        {!isCollapsed && hasChildren && item.children!.map((child) => renderItem(child, depth + 1))}
      </div>
    );
  };

  return (
    <div className="p-2 overflow-auto" style={{ maxHeight: "100%" }}>
      <h3 className="text-xs font-semibold text-muted-foreground uppercase tracking-wider mb-2 px-2">
        剧本大纲
      </h3>
      {outline.length === 0 ? (
        <p className="text-xs text-muted-foreground px-2">暂无内容</p>
      ) : (
        outline.map((item) => renderItem(item))
      )}
    </div>
  );
}
