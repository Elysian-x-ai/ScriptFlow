"use client";

import { useEffect, useState } from "react";
import { roleApi } from "@/lib/api-client";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Plus, Pencil, Trash2 } from "lucide-react";

export default function AdminRolesPage() {
  const [roles, setRoles] = useState<any[]>([]);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [form, setForm] = useState<any>({});

  const load = () => {
    roleApi.page({ page: 1, pageSize: 50 }).then((res) => {
      setRoles(res.records || []);
    }).catch(console.error);
  };

  useEffect(() => { load(); }, []);

  const openCreate = () => {
    setEditingId(null);
    setForm({ name: "", code: "", description: "" });
    setDialogOpen(true);
  };

  const openEdit = (r: any) => {
    setEditingId(r.id);
    setForm(r);
    setDialogOpen(true);
  };

  const handleSave = async () => {
    try {
      if (editingId) {
        await roleApi.update(form);
      } else {
        await roleApi.create(form);
      }
      setDialogOpen(false);
      load();
    } catch (err: any) {
      alert(err.message || "操作失败");
    }
  };

  const handleDelete = async (id: number) => {
    if (!confirm("确定删除此角色？关联的权限也会被清除。")) return;
    try {
      await roleApi.delete(id);
      load();
    } catch (err: any) {
      alert(err.message || "删除失败");
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">角色管理</h1>
        <Button onClick={openCreate}><Plus className="h-4 w-4 mr-2" />新建角色</Button>
      </div>

      <Card>
        <CardContent className="p-0">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b bg-muted/50">
                <th className="text-left p-3 font-medium">ID</th>
                <th className="text-left p-3 font-medium">名称</th>
                <th className="text-left p-3 font-medium">编码</th>
                <th className="text-left p-3 font-medium">描述</th>
                <th className="text-left p-3 font-medium">状态</th>
                <th className="text-left p-3 font-medium">操作</th>
              </tr>
            </thead>
            <tbody>
              {roles.map((r) => (
                <tr key={r.id} className="border-b hover:bg-muted/30">
                  <td className="p-3 text-muted-foreground">{r.id}</td>
                  <td className="p-3 font-medium">{r.name}</td>
                  <td className="p-3"><code className="text-xs bg-muted px-1 py-0.5 rounded">{r.code}</code></td>
                  <td className="p-3 text-muted-foreground">{r.description || "-"}</td>
                  <td className="p-3">
                    <Badge variant={r.status === 1 ? "success" : "secondary"}>
                      {r.status === 1 ? "启用" : "禁用"}
                    </Badge>
                  </td>
                  <td className="p-3">
                    <div className="flex gap-1">
                      <Button variant="ghost" size="sm" className="h-7 w-7" onClick={() => openEdit(r)}>
                        <Pencil className="h-3.5 w-3.5" />
                      </Button>
                      <Button variant="ghost" size="sm" className="h-7 w-7 text-destructive" onClick={() => handleDelete(r.id)}>
                        <Trash2 className="h-3.5 w-3.5" />
                      </Button>
                    </div>
                  </td>
                </tr>
              ))}
              {roles.length === 0 && (
                <tr><td colSpan={6} className="p-8 text-center text-muted-foreground">暂无数据</td></tr>
              )}
            </tbody>
          </table>
        </CardContent>
      </Card>

      <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{editingId ? "编辑角色" : "新建角色"}</DialogTitle>
          </DialogHeader>
          <div className="space-y-4 py-2">
            <div className="space-y-1">
              <Label>名称 *</Label>
              <Input value={form.name || ""} onChange={(e) => setForm({ ...form, name: e.target.value })} placeholder="如：管理员" />
            </div>
            <div className="space-y-1">
              <Label>编码 *</Label>
              <Input value={form.code || ""} onChange={(e) => setForm({ ...form, code: e.target.value })} placeholder="如：ADMIN（大写字母/下划线）" />
            </div>
            <div className="space-y-1">
              <Label>描述</Label>
              <Input value={form.description || ""} onChange={(e) => setForm({ ...form, description: e.target.value })} placeholder="角色描述" />
            </div>
            <div className="space-y-1">
              <Label>状态</Label>
              <div className="flex gap-4">
                <label className="flex items-center gap-2 text-sm">
                  <input type="radio" name="status" checked={form.status !== 0} onChange={() => setForm({ ...form, status: 1 })} />
                  启用
                </label>
                <label className="flex items-center gap-2 text-sm">
                  <input type="radio" name="status" checked={form.status === 0} onChange={() => setForm({ ...form, status: 0 })} />
                  禁用
                </label>
              </div>
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setDialogOpen(false)}>取消</Button>
            <Button onClick={handleSave} disabled={!form.name || !form.code}>保存</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
