"use client";

import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import { chapterApi } from "@/lib/api-client";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogTrigger, DialogFooter } from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Plus, Pencil, Trash2, FileText } from "lucide-react";

export default function ChaptersPage() {
  const params = useParams();
  const projectId = Number(params.id);
  const [chapters, setChapters] = useState<any[]>([]);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [form, setForm] = useState<any>({});

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

  const wordCount = (text: string) => text?.length || 0;

  return (
    <div className="space-y-4 h-full overflow-auto">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-semibold">章节管理 ({chapters.length})</h2>
        <Button size="sm" onClick={openCreate}><Plus className="h-4 w-4 mr-1" />新建章节</Button>
      </div>

      {chapters.length === 0 ? (
        <p className="text-muted-foreground text-sm py-8 text-center">暂无章节，点击新建章节开始添加</p>
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
