"use client";

import { useEffect, useState, useCallback, useRef } from "react";
import { useParams } from "next/navigation";
import dynamic from "next/dynamic";
import { scriptApi, projectApi, taskApi, MinioYamlVO } from "@/lib/api-client";
import ScriptOutline from "@/components/script/script-outline";
import ScriptPreview from "@/components/script/script-preview";
import AiAssistant from "@/components/ai/ai-assistant";
import { Button } from "@/components/ui/button";
import { Separator } from "@/components/ui/separator";
import { Badge } from "@/components/ui/badge";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogTrigger, DialogFooter } from "@/components/ui/dialog";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { FileDown, FileCheck, Eye, FileCode, Save, Maximize2, Minimize2, Sparkles, Loader2 } from "lucide-react";

const MonacoEditor = dynamic(() => import("@monaco-editor/react"), { ssr: false });

const toGenerationStatus = (status: unknown): GenProgressStatus => {
  if (status === 1 || status === "processing") return "processing";
  if (status === 2 || status === "completed") return "completed";
  if (status === 3 || status === "failed") return "failed";
  if (status === 0 || status === "submitted") return "submitted";
  return "processing";
};

type GenProgressStatus = "" | "submitting" | "processing" | "submitted" | "completed" | "failed";

interface GenProgress {
  taskId: number | null;
  status: GenProgressStatus;
  progress: number;
  stage: string;
}

export default function ProjectWorkspacePage() {
  const params = useParams();
  const projectId = Number(params.id);

  const [yamlContent, setYamlContent] = useState(`meta:
  title: "剧本名称"
  source_novel: "原著小说名称"
  author: "作者"
  version: "v1.0"

characters:
  - id: "char_001"
    name: "角色名"
    description: "角色描述"

acts:
  - act_id: "act_01"
    title: "第一幕"
    scenes:
      - scene_number: 1
        title: "第一场"
        location: "内景-房间"
        time: "日"
        atmosphere: "平静"
        content:
          - type: action
            text: "场景描述"
`);
  const [scriptId, setScriptId] = useState<number | undefined>();
  const [project, setProject] = useState<any>(null);
  const [saving, setSaving] = useState(false);
  const [validationResult, setValidationResult] = useState<{ valid: boolean; errors: string[] } | null>(null);
  const [bottomPanel, setBottomPanel] = useState<"preview" | "editor">("preview");
  const [rightPanelOpen, setRightPanelOpen] = useState(true);
  const [leftPanelOpen, setLeftPanelOpen] = useState(true);
  const [scriptExists, setScriptExists] = useState(false);
  const [genProgress, setGenProgress] = useState<GenProgress>({ taskId: null, status: "", progress: 0, stage: "" });
  const [displayProgress, setDisplayProgress] = useState(0);
  const [yamlVersions, setYamlVersions] = useState<MinioYamlVO[]>([]);
  const [loadingYamlVersions, setLoadingYamlVersions] = useState(false);
  const [leftPanelTab, setLeftPanelTab] = useState<"outline" | "versions">("outline");
  const editorRef = useRef<any>(null);

  const loadYamlVersions = useCallback(async () => {
    setLoadingYamlVersions(true);
    try {
      const list = await scriptApi.listYamlVersions(projectId);
      setYamlVersions(list);
    } catch (err: any) {
      console.error("加载YAML版本列表失败", err);
    } finally {
      setLoadingYamlVersions(false);
    }
  }, [projectId]);

  const loadYamlVersionContent = async (vo: MinioYamlVO) => {
    try {
      const content = await scriptApi.getYamlContent(vo.objectKey);
      setYamlContent(content);
      setActiveYamlObjectKey(vo.objectKey);
    } catch (err: any) {
      alert("加载失败: " + err.message);
    }
  };

  const formatFileSize = (bytes: number) => {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  };

  useEffect(() => {
    projectApi.getById(projectId).then(setProject).catch(console.error);
    loadScript();
    loadYamlVersions();
  }, [projectId]);

  const loadScript = () => {
    scriptApi.getByProject(projectId).then((s) => {
      setScriptId(s.id);
      setScriptExists(true);
      if (s.yamlContent) setYamlContent(s.yamlContent);
    }).catch(() => {
      setScriptExists(false);
    });
  };

  useEffect(() => {
    const status = genProgress.status;

    if (status === "submitting" || status === "submitted") {
      const target = status === "submitting" ? 5 : 12;
      const maxProgress = status === "submitting" ? 10 : 25;
      setDisplayProgress((prev) => Math.max(prev, target));

      const timer = window.setInterval(() => {
        setDisplayProgress((prev) => Math.min(prev + 1, maxProgress));
      }, 1500);

      return () => window.clearInterval(timer);
    }

    if (status === "processing") {
      const realProgress = Math.min(Math.max(genProgress.progress || 0, 0), 95);
      setDisplayProgress((prev) => Math.max(prev, realProgress, 8));

      const timer = window.setInterval(() => {
        setDisplayProgress((prev) => {
          const target = Math.max(realProgress, prev);
          if (target >= 95) return target;
          const step = target < 35 ? 3 : target < 70 ? 2 : 1;
          return Math.min(target + step, 95);
        });
      }, 1200);

      return () => window.clearInterval(timer);
    }

    if (status === "completed") {
      setDisplayProgress(100);
      return;
    }

    if (status === "failed") {
      setDisplayProgress((prev) => Math.max(prev, Math.min(genProgress.progress || 0, 95)));
      return;
    }

    setDisplayProgress(0);
  }, [genProgress.status, genProgress.progress]);

  // SSE for generation progress
  useEffect(() => {
    if (!genProgress.taskId) return;

    const token = localStorage.getItem("token") || "";
    const url = `${taskApi.streamUrl(genProgress.taskId)}?token=${encodeURIComponent(token)}`;
    const eventSource = new EventSource(url);

    eventSource.addEventListener("progress", (event) => {
      try {
        const data = JSON.parse(event.data);
        const nextStatus = toGenerationStatus(data.status);
        const incomingProgress = typeof data.progress === "number" ? data.progress : 0;
        setGenProgress((prev) => ({
          ...prev,
          progress: Math.max(prev.progress, incomingProgress),
          status: nextStatus,
        }));

        if (data.status === 2) {
          setGenProgress({ taskId: null, status: "completed", progress: 100, stage: "" });
          eventSource.close();
          // Use result from SSE directly (avoids race with DB transaction commit)
          if (data.result) {
            setYamlContent(data.result);
            setActiveYamlObjectKey(null);
          }
          // Also reload from backend as fallback + to update scriptId/version
          setTimeout(() => loadScript(), 500);
          // Refresh the YAML version list from MinIO
          loadYamlVersions();
        } else if (data.status === 3) {
          setGenProgress((prev) => ({ ...prev, status: "failed" }));
          eventSource.close();
        }
      } catch {}
    });

    eventSource.onerror = () => {
      eventSource.close();
    };

    return () => eventSource.close();
  }, [genProgress.taskId]);

  const handleEditorMount = useCallback((editor: any) => {
    editorRef.current = editor;
  }, []);

  const handleNavigate = useCallback((lineNumber: number) => {
    if (editorRef.current) {
      editorRef.current.revealLineInCenter(lineNumber + 1);
      editorRef.current.setPosition({ lineNumber: lineNumber + 1, column: 1 });
      editorRef.current.focus();
    }
  }, []);

  const openChapterDialog = async () => {
    setLoadingChapters(true);
    setChapterDialogOpen(true);
    try {
      const [list, lastNos] = await Promise.all([
        scriptApi.getChapters(projectId),
        scriptApi.getLastChapterNos(projectId).catch(() => [] as number[]),
      ]);
      setChapters(list);
      // Smart default: only select chapters NOT in last generation
      if (lastNos.length > 0) {
        const newChapters = list.filter((ch: any) => !lastNos.includes(ch.chapterNo));
        if (newChapters.length > 0) {
          setSelectedChapterIds(new Set(newChapters.map((c: any) => c.id || c.chapterNo)));
        } else {
          // All chapters already generated — select none, user picks manually
          setSelectedChapterIds(new Set());
        }
      } else {
        // First time: select all
        setSelectedChapterIds(new Set(list.map((c: any) => c.id || c.chapterNo)));
      }
    } catch (err: any) {
      alert("加载章节列表失败: " + err.message);
      setChapterDialogOpen(false);
    } finally {
      setLoadingChapters(false);
    }
  };

  const handleGenerate = async (chapterIds?: number[]) => {
    setDisplayProgress(0);
    setGenProgress({ taskId: null, status: "submitting", progress: 0, stage: "" });
    try {
      const result = await scriptApi.submitGeneration(projectId, undefined, chapterIds);
      setScriptId(result.id);
      if (result.currentTaskId) {
        setGenProgress({ taskId: result.currentTaskId, status: "processing", progress: 0, stage: "" });
      } else {
        setGenProgress({ taskId: null, status: "submitted", progress: 0, stage: "" });
      }
    } catch (err: any) {
      setGenProgress({ taskId: null, status: "failed", progress: 0, stage: "" });
      alert("生成失败: " + err.message);
    }
  };

  const toggleChapter = (id: number) => {
    setSelectedChapterIds((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  };

  const toggleSelectAll = () => {
    if (selectedChapterIds.size === chapters.length) {
      setSelectedChapterIds(new Set());
    } else {
      setSelectedChapterIds(new Set(chapters.map((c) => c.id || c.chapterNo)));
    }
  };

  const toggleExpand = (id: number) => {
    setExpandedChapters((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  };

  const handleSave = async () => {
    if (!scriptId) return;
    setSaving(true);
    try {
      await scriptApi.createVersion(scriptId, yamlContent, "手动保存");
      setValidationResult(null);
      setActiveYamlObjectKey(null);
      loadYamlVersions();
    } catch (err: any) {
      alert("保存失败: " + err.message);
    } finally {
      setSaving(false);
    }
  };

  const handleValidate = async () => {
    try {
      const result = await scriptApi.validate(yamlContent);
      setValidationResult(result);
    } catch (err: any) {
      alert("校验失败: " + err.message);
    }
  };

  const [activeYamlObjectKey, setActiveYamlObjectKey] = useState<string | null>(null);

  // Chapter selection state
  const [chapterDialogOpen, setChapterDialogOpen] = useState(false);
  const [chapters, setChapters] = useState<any[]>([]);
  const [selectedChapterIds, setSelectedChapterIds] = useState<Set<number>>(new Set());
  const [loadingChapters, setLoadingChapters] = useState(false);
  const [expandedChapters, setExpandedChapters] = useState<Set<number>>(new Set());

  const [exportFormat, setExportFormat] = useState("pdf");
  const [exportDialogOpen, setExportDialogOpen] = useState(false);

  const handleExport = async () => {
    try {
      const token = localStorage.getItem("token");
      const base = process.env.NEXT_PUBLIC_API_BASE || "http://localhost:8080";
      const res = await fetch(
        `${base}/api/export/${scriptId || 0}?format=${exportFormat}`,
        {
          method: "POST",
          headers: {
            Authorization: `Bearer ${token}`,
            "Content-Type": "text/plain",
          },
          body: yamlContent,
        }
      );
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      const blob = await res.blob();
      const a = document.createElement("a");
      a.href = URL.createObjectURL(blob);
      a.download = `script-${projectId}.${exportFormat}`;
      a.click();
      URL.revokeObjectURL(a.href);
      setExportDialogOpen(false);
    } catch (err: any) {
      alert("导出失败: " + err.message);
    }
  };

  return (
    <div className="flex flex-col h-full">
      {/* Toolbar */}
      <div className="flex flex-wrap items-center justify-between gap-2 px-3 py-2 border-b bg-card">
        <div className="flex flex-wrap items-center gap-2">
          <Button variant="ghost" size="sm" onClick={() => setLeftPanelOpen(!leftPanelOpen)} className="h-8">
            {leftPanelOpen ? <Minimize2 className="h-3.5 w-3.5 mr-1" /> : <Maximize2 className="h-3.5 w-3.5 mr-1" />}
            大纲
          </Button>
          <Separator orientation="vertical" className="h-5" />
          <div className="flex items-center gap-1 border rounded-md p-0.5">
            <Button
              variant={bottomPanel === "preview" ? "secondary" : "ghost"}
              size="sm"
              onClick={() => setBottomPanel("preview")}
              className="h-7 text-xs"
            >
              <Eye className="h-3.5 w-3.5 mr-1" />
              预览
            </Button>
            <Button
              variant={bottomPanel === "editor" ? "secondary" : "ghost"}
              size="sm"
              onClick={() => setBottomPanel("editor")}
              className="h-7 text-xs"
            >
              <FileCode className="h-3.5 w-3.5 mr-1" />
              源码
            </Button>
          </div>
        </div>

        <div className="flex flex-wrap items-center justify-end gap-2">
          {/* Generate Script Button */}
          <Button
            size="sm"
            onClick={openChapterDialog}
            disabled={genProgress.status === "processing" || genProgress.status === "submitting"}
            className="h-8 text-xs"
          >
            {genProgress.status === "processing" || genProgress.status === "submitting" ? (
              <Loader2 className="h-3.5 w-3.5 mr-1 animate-spin" />
            ) : (
              <Sparkles className="h-3.5 w-3.5 mr-1" />
            )}
            {genProgress.status === "processing"
              ? `生成中 ${Math.round(displayProgress)}%`
              : genProgress.status === "submitting"
              ? "提交中..."
              : scriptExists ? "重新生成" : "一键生成剧本"}
          </Button>

          <Button variant="outline" size="sm" onClick={handleValidate} className="h-8 text-xs">
            <FileCheck className="h-3.5 w-3.5 mr-1" />
            校验
          </Button>
          <Button variant="outline" size="sm" onClick={handleSave} disabled={saving || !scriptId} className="h-8 text-xs">
            <Save className="h-3.5 w-3.5 mr-1" />
            {saving ? "保存中..." : "保存"}
          </Button>
          {/* Chapter Selection Dialog */}
          <Dialog open={chapterDialogOpen} onOpenChange={setChapterDialogOpen}>
            <DialogContent className="max-w-3xl max-h-[85vh] flex flex-col">
              <DialogHeader>
                <DialogTitle>选择章节生成剧本</DialogTitle>
              </DialogHeader>

              {loadingChapters ? (
                <div className="flex items-center justify-center py-12">
                  <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
                </div>
              ) : chapters.length === 0 ? (
                <p className="text-sm text-muted-foreground py-8 text-center">
                  暂无章节内容，请先在章节管理中导入小说。
                </p>
              ) : (
                <>
                  {/* Selection toolbar */}
                  <div className="flex items-center justify-between px-1 py-2 border-b">
                    <span className="text-sm text-muted-foreground">
                      已选择 <strong>{selectedChapterIds.size}</strong> / {chapters.length} 章
                    </span>
                    <Button variant="ghost" size="sm" className="h-7 text-xs" onClick={toggleSelectAll}>
                      {selectedChapterIds.size === chapters.length ? "取消全选" : "全选"}
                    </Button>
                  </div>

                  {/* Chapter list */}
                  <div className="flex-1 overflow-y-auto space-y-1 py-2">
                    {chapters.map((ch: any, idx: number) => {
                      const key = ch.id || ch.chapterNo || idx;
                      const isSelected = selectedChapterIds.has(key);
                      const isExpanded = expandedChapters.has(key);
                      return (
                        <div
                          key={key}
                          className={`flex flex-col rounded-md border transition-colors ${
                            isSelected ? "border-primary/40 bg-primary/5" : "border-border"
                          }`}
                        >
                          <div
                            className="flex items-center gap-3 px-3 py-2.5 cursor-pointer hover:bg-accent/50"
                            onClick={() => toggleChapter(key)}
                          >
                            <input
                              type="checkbox"
                              checked={isSelected}
                              onChange={() => toggleChapter(key)}
                              className="h-4 w-4 rounded border-gray-300 cursor-pointer"
                              onClick={(e) => e.stopPropagation()}
                            />
                            <span className="text-sm font-medium min-w-[4rem] text-muted-foreground">
                              第{ch.chapterNo || idx + 1}章
                            </span>
                            <span className="text-sm flex-1 truncate">
                              {ch.title || `第${ch.chapterNo || idx + 1}章`}
                            </span>
                            <span className="text-xs text-muted-foreground whitespace-nowrap">
                              {ch.wordCount || 0} 字
                            </span>
                            <Button
                              variant="ghost"
                              size="sm"
                              className="h-6 w-6 p-0"
                              onClick={(e) => { e.stopPropagation(); toggleExpand(key); }}
                            >
                              {isExpanded ? "收起" : "展开"}
                            </Button>
                          </div>
                          {isExpanded && (
                            <div className="px-3 pb-3 pt-0">
                              <div className="text-xs text-muted-foreground leading-relaxed whitespace-pre-wrap bg-muted/50 rounded p-3 max-h-60 overflow-y-auto">
                                {ch.content || "（无内容）"}
                              </div>
                            </div>
                          )}
                        </div>
                      );
                    })}
                  </div>

                  {/* Footer */}
                  <div className="flex items-center justify-end gap-2 pt-3 border-t">
                    <Button variant="outline" size="sm" onClick={() => setChapterDialogOpen(false)}>
                      取消
                    </Button>
                    <Button
                      size="sm"
                      onClick={() => {
                        setChapterDialogOpen(false);
                        // Collect selected chapter IDs by matching against chapters list
                        const selectedIds = chapters
                          .filter((c: any) => selectedChapterIds.has(c.id))
                          .map((c: any) => c.id)
                          .filter((id: any) => id != null);
                        // When all chapters are selected, send empty list (backend interprets as "all")
                        const allSelected = selectedIds.length === chapters.length;
                        handleGenerate(allSelected ? undefined : selectedIds);
                      }}
                      disabled={selectedChapterIds.size === 0}
                    >
                      生成选中章节 ({selectedChapterIds.size}章)
                    </Button>
                  </div>
                </>
              )}
            </DialogContent>
          </Dialog>

          <Dialog open={exportDialogOpen} onOpenChange={setExportDialogOpen}>
            <DialogTrigger asChild>
              <Button size="sm" className="h-8 text-xs">
                <FileDown className="h-3.5 w-3.5 mr-1" />
                导出
              </Button>
            </DialogTrigger>
            <DialogContent>
              <DialogHeader>
                <DialogTitle>导出剧本</DialogTitle>
              </DialogHeader>
              <div className="py-4 space-y-4">
                <div className="space-y-2">
                  <p className="text-sm font-medium">导出格式</p>
                  <Select value={exportFormat} onValueChange={setExportFormat}>
                    <SelectTrigger>
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="pdf">PDF 文档</SelectItem>
                      <SelectItem value="docx">Word 文档 (DOCX)</SelectItem>
                      <SelectItem value="fdx">FDX 剧本格式</SelectItem>
                    </SelectContent>
                  </Select>
                </div>
              </div>
              <DialogFooter>
                <Button variant="outline" onClick={() => setExportDialogOpen(false)}>取消</Button>
                <Button onClick={handleExport}>导出</Button>
              </DialogFooter>
            </DialogContent>
          </Dialog>

          <Separator orientation="vertical" className="h-5" />
          <Button
            variant="ghost"
            size="sm"
            onClick={() => setRightPanelOpen(!rightPanelOpen)}
            className="h-8 text-xs"
          >
            {rightPanelOpen ? <Minimize2 className="h-3.5 w-3.5 mr-1" /> : <Maximize2 className="h-3.5 w-3.5 mr-1" />}
            AI
          </Button>
        </div>
      </div>

      {/* Generation progress bar */}
      {(genProgress.status === "submitting" || genProgress.status === "submitted" || genProgress.status === "processing") && (
        <div className="px-4 py-1.5 bg-blue-50 dark:bg-blue-950 border-b border-blue-200 dark:border-blue-800">
          <div className="flex items-center justify-between text-xs text-blue-700 dark:text-blue-300 mb-1">
            <span className="flex items-center gap-1">
              <Loader2 className="h-3 w-3 animate-spin" />
              {genProgress.status === "submitting"
                ? "正在提交生成任务..."
                : genProgress.status === "submitted"
                ? "任务已提交，等待开始处理..."
                : "AI 剧本生成中..."}
            </span>
            <span>{Math.round(displayProgress)}%</span>
          </div>
          <div className="w-full h-1.5 bg-blue-200 dark:bg-blue-800 rounded-full overflow-hidden">
            <div
              className="h-full bg-blue-600 dark:bg-blue-400 rounded-full transition-all duration-300"
              style={{ width: `${displayProgress}%` }}
            />
          </div>
        </div>
      )}

      {/* Generation failed banner */}
      {genProgress.status === "failed" && (
        <div className="px-4 py-2 text-sm bg-red-50 text-red-700 dark:bg-red-950 dark:text-red-300 border-b">
          剧本生成失败，请检查章节内容后重试。
        </div>
      )}

      {/* Generation completed banner */}
      {genProgress.status === "completed" && (
        <div className="px-4 py-2 text-sm bg-green-50 text-green-700 dark:bg-green-950 dark:text-green-300 border-b">
          剧本生成完成！YAML 内容已自动加载。✅
        </div>
      )}

      {/* History banner when viewing old version */}
      {activeYamlObjectKey && activeYamlObjectKey !== yamlVersions[0]?.objectKey && (
        <div className="px-4 py-2 text-sm bg-amber-50 text-amber-700 dark:bg-amber-950 dark:text-amber-300 border-b flex items-center justify-between">
          <span>正在查看历史版本，当前编辑将保存为新版本</span>
          {yamlVersions[0] && (
            <Button variant="ghost" size="sm" className="h-6 text-xs" onClick={() => loadYamlVersionContent(yamlVersions[0])}>
              回到最新
            </Button>
          )}
        </div>
      )}

      {/* Validation result */}
      {validationResult && (
        <div className={`px-4 py-2 text-sm ${validationResult.valid ? "bg-green-50 text-green-700 dark:bg-green-950 dark:text-green-300" : "bg-red-50 text-red-700 dark:bg-red-950 dark:text-red-300"}`}>
          {validationResult.valid ? (
            <span className="flex items-center gap-1">YAML 格式校验通过</span>
          ) : (
            <div>
              <span className="font-medium">校验发现 {validationResult.errors.length} 个问题：</span>
              <ul className="list-disc list-inside mt-1 text-xs">
                {validationResult.errors.map((e, i) => <li key={i}>{e}</li>)}
              </ul>
            </div>
          )}
        </div>
      )}

      {/* 4-Panel Grid */}
      <div className="flex-1 grid" style={{
        gridTemplateColumns: `${leftPanelOpen ? "220px" : "0px"} 1fr ${rightPanelOpen ? "300px" : "0px"}`,
        gridTemplateRows: "1fr 200px",
        gridTemplateAreas: `
          "outline editor assistant"
          "preview preview assistant"
        `,
      }}>
        {/* Left: Script Outline / Versions */}
        <div style={{ gridArea: "outline", overflow: "hidden", borderRight: "1px solid hsl(var(--border))" }}>
          <div className="flex border-b bg-card">
            <button
              className={`flex-1 py-1.5 text-xs font-medium text-center transition-colors ${
                leftPanelTab === "outline"
                  ? "bg-background text-foreground border-b-2 border-primary"
                  : "text-muted-foreground hover:text-foreground"
              }`}
              onClick={() => setLeftPanelTab("outline")}
            >
              大纲
            </button>
            <button
              className={`flex-1 py-1.5 text-xs font-medium text-center transition-colors ${
                leftPanelTab === "versions"
                  ? "bg-background text-foreground border-b-2 border-primary"
                  : "text-muted-foreground hover:text-foreground"
              }`}
              onClick={() => { setLeftPanelTab("versions"); loadYamlVersions(); }}
            >
              版本
            </button>
          </div>
          <div className="overflow-auto" style={{ height: "calc(100% - 32px)" }}>
            {leftPanelTab === "outline" ? (
              <ScriptOutline yamlContent={yamlContent} onNavigate={handleNavigate} />
            ) : (
              <div className="p-2 space-y-1">
                {loadingYamlVersions ? (
                  <div className="flex items-center justify-center py-12">
                    <Loader2 className="h-4 w-4 animate-spin text-muted-foreground" />
                  </div>
                ) : yamlVersions.length === 0 ? (
                  <p className="text-xs text-muted-foreground text-center py-12">暂无版本记录<br/>生成剧本后自动保存</p>
                ) : (
                  yamlVersions.map((vo, idx) => (
                    <div
                      key={vo.objectKey}
                      className="flex items-center justify-between p-2 border rounded-md hover:bg-accent/50 transition-colors"
                    >
                      <div className="flex-1 min-w-0">
                        <p className="text-xs font-medium">
                          {vo.version ? `v${vo.version}` : "未知版本"}
                          {idx === 0 && <span className="ml-1.5 text-[10px] text-green-600 font-normal">(最新)</span>}
                        </p>
                        <p className="text-[10px] text-muted-foreground mt-0.5">{vo.lastModified}</p>
                        <p className="text-[10px] text-muted-foreground">{formatFileSize(vo.fileSize)}</p>
                      </div>
                      <Button
                        variant="outline"
                        size="sm"
                        className="h-6 text-[10px] ml-2 shrink-0"
                        onClick={() => loadYamlVersionContent(vo)}
                      >
                        加载
                      </Button>
                    </div>
                  ))
                )}
              </div>
            )}
          </div>
        </div>

        {/* Center: Monaco Editor */}
        <div style={{ gridArea: "editor", overflow: "hidden" }}>
          <MonacoEditor
            height="100%"
            defaultLanguage="yaml"
            theme="vs-dark"
            value={yamlContent}
            onChange={(val) => setYamlContent(val || "")}
            onMount={handleEditorMount}
            options={{
              minimap: { enabled: false },
              fontSize: 13,
              lineNumbers: "on",
              scrollBeyondLastLine: false,
              wordWrap: "on",
              tabSize: 2,
              automaticLayout: true,
            }}
          />
        </div>

        {/* Right: AI Assistant */}
        <div className="h-full" style={{ gridArea: "assistant", borderLeft: "1px solid hsl(var(--border))" }}>
          <AiAssistant projectId={projectId} scriptId={scriptId} />
        </div>

        {/* Bottom: Preview or Source */}
        <div style={{ gridArea: "preview", borderTop: "1px solid hsl(var(--border))", overflow: "auto" }}>
          {bottomPanel === "preview" ? (
            <ScriptPreview yamlContent={yamlContent} />
          ) : (
            <div className="p-4 overflow-auto font-mono text-xs" style={{ maxHeight: "100%" }}>
              <pre className="whitespace-pre-wrap text-muted-foreground">{yamlContent}</pre>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
