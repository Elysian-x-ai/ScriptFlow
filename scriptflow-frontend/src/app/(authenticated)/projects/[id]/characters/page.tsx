"use client";

import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import { characterApi } from "@/lib/api-client";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogTrigger, DialogFooter } from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Plus, Pencil, Trash2 } from "lucide-react";

export default function CharactersPage() {
  const params = useParams();
  const projectId = Number(params.id);
  const [characters, setCharacters] = useState<any[]>([]);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [form, setForm] = useState<any>({});

  const load = () => {
    characterApi.listByProject(projectId).then(setCharacters).catch(console.error);
  };

  useEffect(() => { load(); }, [projectId]);

  const openCreate = () => {
    setEditingId(null);
    setForm({ projectId, name: "", alias: "", gender: "", age: "", personality: "", appearance: "", background: "", description: "", roleType: "" });
    setDialogOpen(true);
  };

  const openEdit = (c: any) => {
    setEditingId(c.id);
    setForm(c);
    setDialogOpen(true);
  };

  const handleSave = async () => {
    try {
      if (editingId) {
        await characterApi.update(editingId, form);
      } else {
        await characterApi.create(form);
      }
      setDialogOpen(false);
      load();
    } catch (err: any) {
      alert(err.message || "操作失败");
    }
  };

  const handleDelete = async (id: number) => {
    if (!confirm("确定删除此角色？")) return;
    try {
      await characterApi.delete(id);
      load();
    } catch (err: any) {
      alert(err.message || "删除失败");
    }
  };

  const field = (label: string, key: string, placeholder?: string) => (
    <div className="space-y-1">
      <Label className="text-xs">{label}</Label>
      <Input value={form[key] || ""} onChange={(e) => setForm({ ...form, [key]: e.target.value })} placeholder={placeholder} />
    </div>
  );

  return (
    <div className="space-y-4 h-full overflow-auto">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-semibold">角色管理 ({characters.length})</h2>
        <Button size="sm" onClick={openCreate}><Plus className="h-4 w-4 mr-1" />新建角色</Button>
      </div>

      {characters.length === 0 ? (
        <p className="text-muted-foreground text-sm py-8 text-center">暂无角色，点击新建角色开始添加</p>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {characters.map((c) => (
            <Card key={c.id}>
              <CardHeader className="pb-2">
                <div className="flex items-start justify-between">
                  <div>
                    <CardTitle className="text-base">{c.name}</CardTitle>
                    {c.alias && <p className="text-xs text-muted-foreground">{c.alias}</p>}
                  </div>
                  <div className="flex gap-1">
                    <Button variant="ghost" size="icon" className="h-7 w-7" onClick={() => openEdit(c)}>
                      <Pencil className="h-3.5 w-3.5" />
                    </Button>
                    <Button variant="ghost" size="icon" className="h-7 w-7 text-destructive" onClick={() => handleDelete(c.id)}>
                      <Trash2 className="h-3.5 w-3.5" />
                    </Button>
                  </div>
                </div>
              </CardHeader>
              <CardContent className="text-xs space-y-1">
                <div className="flex gap-2">
                  {c.gender && <Badge variant="outline">{c.gender}</Badge>}
                  {c.roleType && <Badge variant="secondary">{c.roleType}</Badge>}
                  {c.age && <span className="text-muted-foreground">{c.age}岁</span>}
                </div>
                {c.personality && <p><span className="text-muted-foreground">性格：</span>{c.personality}</p>}
                {c.description && <p className="text-muted-foreground line-clamp-2">{c.description}</p>}
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
        <DialogContent className="max-w-lg max-h-[80vh] overflow-auto">
          <DialogHeader>
            <DialogTitle>{editingId ? "编辑角色" : "新建角色"}</DialogTitle>
          </DialogHeader>
          <div className="grid grid-cols-2 gap-3 py-2">
            {field("名称 *", "name", "角色名称")}
            {field("别名", "alias", "别名/绰号")}
            <div className="space-y-1">
              <Label className="text-xs">性别</Label>
              <Select value={form.gender || ""} onValueChange={(v) => setForm({ ...form, gender: v })}>
                <SelectTrigger><SelectValue placeholder="选择性别" /></SelectTrigger>
                <SelectContent>
                  <SelectItem value="男">男</SelectItem>
                  <SelectItem value="女">女</SelectItem>
                  <SelectItem value="其他">其他</SelectItem>
                </SelectContent>
              </Select>
            </div>
            {field("年龄", "age", "如：25")}
            {field("角色类型", "roleType", "如：主角、反派")}
            {field("性格", "personality", "性格描述")}
            <div className="col-span-2">{field("外貌", "appearance", "外貌描述")}</div>
            <div className="col-span-2">{field("背景", "background", "背景故事")}</div>
            <div className="col-span-2">{field("描述", "description", "详细描述")}</div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setDialogOpen(false)}>取消</Button>
            <Button onClick={handleSave} disabled={!form.name}>保存</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
