"use client";

import { useEffect, useState, useRef } from "react";
import { useParams } from "next/navigation";
import { chapterApi } from "@/lib/api-client";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogTrigger, DialogFooter } from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Plus, Pencil, Trash2, FileText, Upload, Loader2 } from "lucide-react";

export default function ChaptersPage() {
  const params = useParams();
  const projectId = Number(params.id);
  const [chapters, setChapters] = useState<any[]>([]);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [form, setForm] = useState<any>({});
  const [importDialogOpen, setImportDialogOpen] = useState(false);
  const [importText, setImportText] = useState("");
  const [importMethod, setImportMethod] = useState<"text" | "file">("text");
  const [importing, setImporting] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const load = () => {
    chapterApi.listByProject(projectId).then(setChapters).catch(console.error);
  };

  useEffect(() => { load(); }, [projectId]);

  const openCreate = () => {
    setEditingId(null);
    const nextNo = chapters.length > 0 ? Math.max(...chapters.map((c) => c.chapterNo || 0)) + 1 : 1;
    setForm({ projectId, chapterNo: nextNo, title: "", content: "" });
    setDialogOpen(true);
  };

  const openEdit = (c: any) => {
    setEditingId(c.id);
    setForm({ projectId, chapterNo: c.chapterNo, title: c.title, content: c.content });
    setDialogOpen(true);
  };

  const handleSave = async () => {
    try {
      if (editingId) {
        await chapterApi.update({ id: editingId, ...form });
      } else {
        await chapterApi.create(form);
      }
      setDialogOpen(false);
      load();
    } catch (err: any) {
      alert(err.message || "操作失败");
    }
  };

  const handleDelete = async (id: number) => {
    if (!confirm("确定删除此章节？")) return;
    try {
      await chapterApi.delete(id);
      load();
    } catch (err: any) {
      alert(err.message || "删除失败");
    }
  };

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = (ev) => {
      const text = ev.target?.result as string;
      setImportText(text);
    };
    reader.readAsText(file, "UTF-8");
  };

  const handleImport = async () => {
    if (!importText.trim()) return;
    setImporting(true);
    try {
      // Try to split by common chapter markers
      const lines = importText.split("\n");
      const chapterPattern = /^(第[零一二三四五六七八九十百千万\d]+[章节部回集]|Chapter\s+\d+|\d+\.)/i;
      let currentChapters: { chapterNo: number; title: string; content: string[] }[] = [];
      let currentLines: string[] = [];
      let chapterNo = 0;

      for (const line of lines) {
        if (chapterPattern.test(line.trim())) {
          if (currentLines.length > 0 || currentChapters.length > 0) {
            chapterNo++;
            currentChapters.push({
              chapterNo,
              title: currentChapters.length === 0 ? "序章" : currentChapters[currentChapters.length - 1].title,
              content: [...currentLines],
            });
            currentLines = [];
          }
          currentChapters.push({
            chapterNo: currentChapters.length + 1,
            title: line.trim(),
            content: [],
          });
        } else {
          currentLines.push(line);
        }
      }
      // Push remaining content
      if (currentLines.length > 0) {
        if (currentChapters.length === 0) {
          // No chapter markers found, create a single chapter
          currentChapters.push({ chapterNo: 1, title: "全文", content: currentLines });
        } else {
          currentChapters[currentChapters.length - 1].content.push(...currentLines);
        }
      }

      // If no structure detected, create single chapter
      if (currentChapters.length === 0) {
        currentChapters = [{ chapterNo: 1, title: "导入文本", content: lines }];
      }

      // Create chapters in sequence
      for (const ch of currentChapters) {
        // Skip empty chapters
        const content = ch.content.join("\n").trim();
        if (!content) continue;
        // Check for duplicate chapter numbers
        const existingNos = chapters.map((c) => c.chapterNo);
        let finalNo = ch.chapterNo;
        while (existingNos.includes(finalNo)) {
          finalNo++;
        }
        await chapterApi.create({
          projectId,
          chapterNo: finalNo,
          title: ch.title || `第${finalNo}章`,
          content,
        });
      }

      setImportDialogOpen(false);
      setImportText("");
      load();
    } catch (err: any) {
      alert("导入失败: " + err.message);
    } finally {
      setImporting(false);
    }
  };

  const wordCount = (text: string) => text?.length || 0;

  return (
    <div className="space-y-4 h-full overflow-auto">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-semibold">章节管理 ({chapters.length})</h2>
        <div className="flex gap-2">
          <Button variant="outline" size="sm" onClick={() => setImportDialogOpen(true)}>
            <Upload className="h-4 w-4 mr-1" />
            导入小说
          </Button>
          <Button size="sm" onClick={openCreate}><Plus className="h-4 w-4 mr-1" />新建章节</Button>
        </div>
      </div>

      {/* Import dialog */}
      <Dialog open={importDialogOpen} onOpenChange={setImportDialogOpen}>
        <DialogContent className="max-w-3xl max-h-[85vh]">
          <DialogHeader>
            <DialogTitle>导入小说</DialogTitle>
          </DialogHeader>
          <div className="space-y-4 py-2">
            <div className="flex gap-2">
              <Button
                variant={importMethod === "text" ? "default" : "outline"}
                size="sm"
                onClick={() => setImportMethod("text")}
              >
                粘贴文本
              </Button>
              <Button
                variant={importMethod === "file" ? "default" : "outline"}
                size="sm"
                onClick={() => {
                  setImportMethod("file");
                  setTimeout(() => fileInputRef.current?.click(), 100);
                }}
              >
                上传文件
              </Button>
            </div>

            {importMethod === "file" && (
              <div>
                <input
                  ref={fileInputRef}
                  type="file"
                  accept=".txt,.md,.html"
                  onChange={handleFileChange}
                  className="hidden"
                />
                <div
                  className="border-2 border-dashed rounded-lg p-8 text-center cursor-pointer hover:bg-accent"
                  onClick={() => fileInputRef.current?.click()}
                >
                  <Upload className="h-8 w-8 mx-auto mb-2 text-muted-foreground" />
                  <p className="text-sm text-muted-foreground">点击选择 .txt / .md 文件</p>
                  {importText && (
                    <p className="text-xs text-green-600 mt-1">已加载：{importText.length} 字符</p>
                  )}
                </div>
              </div>
            )}

            {importMethod === "text" && (
              <div className="space-y-1">
                <Label>粘贴小说全文（系统会自动识别章节分隔）</Label>
                <Textarea
                  value={importText}
                  onChange={(e) => setImportText(e.target.value)}
                  rows={16}
                  placeholder={`在此粘贴小说全文...

支持自动识别以下章节格式：
- 第一章 / 第1章 / 第 一 章
- Chapter 1
- 1. 标题

也可以粘贴无章节标记的纯文本，会作为单章导入`}
                  className="font-mono text-sm"
                />
              </div>
            )}

            <p className="text-xs text-muted-foreground">
              {importText ? `共 ${importText.length} 字符` : "未选择内容"}
            </p>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setImportDialogOpen(false)}>取消</Button>
            <Button onClick={handleImport} disabled={!importText.trim() || importing}>
              {importing ? <Loader2 className="h-4 w-4 mr-1 animate-spin" /> : null}
              {importing ? "导入中..." : "确认导入"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {chapters.length === 0 ? (
        <p className="text-muted-foreground text-sm py-8 text-center">暂无章节，点击导入小说或新建章节开始添加</p>
      ) : (
        <div className="space-y-3">
          {chapters.map((c) => (
            <Card key={c.id}>
              <CardContent className="p-4 flex items-start justify-between">
                <div className="flex-1">
                  <div className="flex items-center gap-2">
                    <FileText className="h-4 w-4 text-muted-foreground" />
                    <span className="font-medium">第 {c.chapterNo} 章</span>
                    <span className="text-sm">{c.title}</span>
                    <span className="text-xs text-muted-foreground">({wordCount(c.content)} 字)</span>
                  </div>
                  {c.contentPreview && (
                    <p className="text-sm text-muted-foreground mt-1 line-clamp-2">{c.contentPreview}</p>
                  )}
                </div>
                <div className="flex gap-1 ml-4">
                  <Button variant="ghost" size="icon" className="h-7 w-7" onClick={() => openEdit(c)}>
                    <Pencil className="h-3.5 w-3.5" />
                  </Button>
                  <Button variant="ghost" size="icon" className="h-7 w-7 text-destructive" onClick={() => handleDelete(c.id)}>
                    <Trash2 className="h-3.5 w-3.5" />
                  </Button>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
        <DialogContent className="max-w-2xl">
          <DialogHeader>
            <DialogTitle>{editingId ? "编辑章节" : "新建章节"}</DialogTitle>
          </DialogHeader>
          <div className="space-y-4 py-2">
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-1">
                <Label>章节号</Label>
                <Input type="number" value={form.chapterNo || ""} onChange={(e) => setForm({ ...form, chapterNo: parseInt(e.target.value) || 0 })} />
              </div>
              <div className="space-y-1">
                <Label>标题</Label>
                <Input value={form.title || ""} onChange={(e) => setForm({ ...form, title: e.target.value })} placeholder="章节标题" />
              </div>
            </div>
            <div className="space-y-1">
              <Label>内容 ({wordCount(form.content)} 字)</Label>
              <Textarea
                value={form.content || ""}
                onChange={(e) => setForm({ ...form, content: e.target.value })}
                rows={12}
                placeholder="在此输入章节内容..."
              />
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setDialogOpen(false)}>取消</Button>
            <Button onClick={handleSave} disabled={!form.content}>保存</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
