"use client";

import { useEffect, useState, useCallback, useRef } from "react";
import { useParams } from "next/navigation";
import dynamic from "next/dynamic";
import { scriptApi, projectApi, taskApi } from "@/lib/api-client";
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

interface GenProgress {
  taskId: number | null;
  status: string;
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
  const editorRef = useRef<any>(null);

  useEffect(() => {
    projectApi.getById(projectId).then(setProject).catch(console.error);
    loadScript();
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

  // SSE for generation progress
  useEffect(() => {
    if (!genProgress.taskId) return;

    const token = localStorage.getItem("token") || "";
    const url = `${taskApi.streamUrl(genProgress.taskId)}?token=${encodeURIComponent(token)}`;
    const eventSource = new EventSource(url);

    eventSource.addEventListener("progress", (event) => {
      try {
        const data = JSON.parse(event.data);
        setGenProgress((prev) => ({ ...prev, progress: data.progress || 0, status: data.status }));

        if (data.status === 2) {
          setGenProgress({ taskId: null, status: "completed", progress: 100, stage: "" });
          eventSource.close();
          // Reload YAML content after generation completes
          setTimeout(() => loadScript(), 500);
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

  const handleGenerate = async () => {
    setGenProgress({ taskId: null, status: "submitting", progress: 0, stage: "" });
    try {
      const result = await scriptApi.submitGeneration(projectId);
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

  const handleSave = async () => {
    if (!scriptId) return;
    setSaving(true);
    try {
      await scriptApi.createVersion(scriptId, yamlContent, "手动保存");
      setValidationResult(null);
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
      <div className="flex items-center justify-between px-3 py-2 border-b bg-card">
        <div className="flex items-center gap-2">
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

        <div className="flex items-center gap-2">
          {/* Generate Script Button */}
          <Button
            size="sm"
            onClick={handleGenerate}
            disabled={genProgress.status === "processing" || genProgress.status === "submitting"}
            className="h-8 text-xs"
          >
            {genProgress.status === "processing" || genProgress.status === "submitting" ? (
              <Loader2 className="h-3.5 w-3.5 mr-1 animate-spin" />
            ) : (
              <Sparkles className="h-3.5 w-3.5 mr-1" />
            )}
            {genProgress.status === "processing"
              ? `生成中 ${genProgress.progress}%`
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
      {genProgress.status === "processing" && (
        <div className="px-4 py-1.5 bg-blue-50 dark:bg-blue-950 border-b border-blue-200 dark:border-blue-800">
          <div className="flex items-center justify-between text-xs text-blue-700 dark:text-blue-300 mb-1">
            <span className="flex items-center gap-1">
              <Loader2 className="h-3 w-3 animate-spin" />
              AI 剧本生成中...
            </span>
            <span>{genProgress.progress}%</span>
          </div>
          <div className="w-full h-1.5 bg-blue-200 dark:bg-blue-800 rounded-full overflow-hidden">
            <div
              className="h-full bg-blue-600 dark:bg-blue-400 rounded-full transition-all duration-300"
              style={{ width: `${genProgress.progress}%` }}
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
        {/* Left: Script Outline */}
        <div style={{ gridArea: "outline", overflow: "auto", borderRight: "1px solid hsl(var(--border))" }}>
          <ScriptOutline yamlContent={yamlContent} onNavigate={handleNavigate} />
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
