"use client";

import { useEffect, useState } from "react";
import { promptApi } from "@/lib/api-client";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Textarea } from "@/components/ui/textarea";
import { Plus, Pencil, Trash2 } from "lucide-react";

export default function AdminPromptsPage() {
  const [prompts, setPrompts] = useState<any[]>([]);
  const [category, setCategory] = useState("");
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [form, setForm] = useState<any>({});

  const load = () => {
    promptApi.page({ page: 1, pageSize: 50, category: category || undefined }).then((res) => {
      setPrompts(res.records || []);
    }).catch(console.error);
  };

  useEffect(() => { load(); }, [category]);

  const openCreate = () => {
    setEditingId(null);
    setForm({ name: "", code: "", type: "system", category: "script_generate", content: "", description: "", status: 1 });
    setDialogOpen(true);
  };

  const openEdit = (p: any) => {
    setEditingId(p.id);
    setForm(p);
    setDialogOpen(true);
  };

  const handleSave = async () => {
    try {
      if (editingId) {
        await promptApi.update(form);
      } else {
        await promptApi.create(form);
      }
      setDialogOpen(false);
      load();
    } catch (err: any) {
      alert(err.message || "操作失败");
    }
  };

  const handleDelete = async (id: number) => {
    if (!confirm("确定删除此模板？")) return;
    try {
      await promptApi.delete(id);
      load();
    } catch (err: any) {
      alert(err.message || "删除失败");
    }
  };

  const categoryLabels: Record<string, string> = {
    novel_analysis: "小说分析",
    character_extract: "角色提取",
    script_generate: "剧本生成",
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">提示词模板</h1>
        <Button onClick={openCreate}><Plus className="h-4 w-4 mr-2" />新建模板</Button>
      </div>

      <div className="flex gap-2">
        <Select value={category} onValueChange={setCategory}>
          <SelectTrigger className="w-40">
            <SelectValue placeholder="全部分类" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value=" ">全部分类</SelectItem>
            <SelectItem value="novel_analysis">小说分析</SelectItem>
            <SelectItem value="character_extract">角色提取</SelectItem>
            <SelectItem value="script_generate">剧本生成</SelectItem>
          </SelectContent>
        </Select>
      </div>

      <div className="space-y-3">
        {prompts.map((p) => (
          <Card key={p.id}>
            <CardContent className="p-4">
              <div className="flex items-start justify-between">
                <div className="flex-1">
                  <div className="flex items-center gap-2">
                    <span className="font-medium">{p.name}</span>
                    <code className="text-xs bg-muted px-1 py-0.5 rounded">{p.code}</code>
                    <Badge variant="outline">{categoryLabels[p.category] || p.category}</Badge>
                    <Badge variant={p.type === "system" ? "info" : "secondary"}>
                      {p.type === "system" ? "系统" : "用户"}
                    </Badge>
                    <Badge variant={p.status === 1 ? "success" : "secondary"}>
                      {p.status === 1 ? "启用" : "禁用"}
                    </Badge>
                    <span className="text-xs text-muted-foreground">v{p.version}</span>
                  </div>
                  {p.description && (
                    <p className="text-xs text-muted-foreground mt-1">{p.description}</p>
                  )}
                  <p className="text-xs text-muted-foreground mt-1 line-clamp-2 font-mono">{p.content}</p>
                </div>
                <div className="flex gap-1 ml-4">
                  <Button variant="ghost" size="icon" className="h-7 w-7" onClick={() => openEdit(p)}>
                    <Pencil className="h-3.5 w-3.5" />
                  </Button>
                  <Button variant="ghost" size="icon" className="h-7 w-7 text-destructive" onClick={() => handleDelete(p.id)}>
                    <Trash2 className="h-3.5 w-3.5" />
                  </Button>
                </div>
              </div>
            </CardContent>
          </Card>
        ))}
        {prompts.length === 0 && (
          <p className="text-muted-foreground text-sm py-8 text-center">暂无模板</p>
        )}
      </div>

      <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
        <DialogContent className="max-w-2xl max-h-[80vh] overflow-auto">
          <DialogHeader>
            <DialogTitle>{editingId ? "编辑模板" : "新建模板"}</DialogTitle>
          </DialogHeader>
          <div className="space-y-4 py-2">
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-1">
                <Label>名称 *</Label>
                <Input value={form.name || ""} onChange={(e) => setForm({ ...form, name: e.target.value })} />
              </div>
              <div className="space-y-1">
                <Label>编码 *</Label>
                <Input value={form.code || ""} onChange={(e) => setForm({ ...form, code: e.target.value })} />
              </div>
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-1">
                <Label>分类</Label>
                <Select value={form.category} onValueChange={(v) => setForm({ ...form, category: v })}>
                  <SelectTrigger><SelectValue /></SelectTrigger>
                  <SelectContent>
                    <SelectItem value="novel_analysis">小说分析</SelectItem>
                    <SelectItem value="character_extract">角色提取</SelectItem>
                    <SelectItem value="script_generate">剧本生成</SelectItem>
                  </SelectContent>
                </Select>
              </div>
              <div className="space-y-1">
                <Label>类型</Label>
                <Select value={form.type} onValueChange={(v) => setForm({ ...form, type: v })}>
                  <SelectTrigger><SelectValue /></SelectTrigger>
                  <SelectContent>
                    <SelectItem value="system">系统</SelectItem>
                    <SelectItem value="user">用户</SelectItem>
                  </SelectContent>
                </Select>
              </div>
            </div>
            <div className="space-y-1">
              <Label>描述</Label>
              <Input value={form.description || ""} onChange={(e) => setForm({ ...form, description: e.target.value })} />
            </div>
            <div className="space-y-1">
              <Label>内容 *</Label>
              <Textarea
                value={form.content || ""}
                onChange={(e) => setForm({ ...form, content: e.target.value })}
                rows={10}
                placeholder="在此输入提示词模板内容..."
              />
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setDialogOpen(false)}>取消</Button>
            <Button onClick={handleSave} disabled={!form.name || !form.code || !form.content}>保存</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
