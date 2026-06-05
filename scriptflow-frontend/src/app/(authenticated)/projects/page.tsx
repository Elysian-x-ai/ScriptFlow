"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { projectApi } from "@/lib/api-client";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogTrigger, DialogFooter } from "@/components/ui/dialog";
import { Label } from "@/components/ui/label";
import { Plus, Search } from "lucide-react";

export default function ProjectsPage() {
  const [projects, setProjects] = useState<any[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [keyword, setKeyword] = useState("");
  const [loading, setLoading] = useState(false);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [form, setForm] = useState({ name: "", novelTitle: "", author: "", description: "" });
  const router = useRouter();

  const loadProjects = async () => {
    setLoading(true);
    try {
      const res = await projectApi.page({ page, pageSize: 20, keyword: keyword || undefined });
      setProjects(res.records || []);
      setTotal(res.total);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadProjects();
  }, [page]);

  const handleSearch = () => {
    setPage(1);
    loadProjects();
  };

  const handleCreate = async () => {
    try {
      const p = await projectApi.create(form);
      setDialogOpen(false);
      setForm({ name: "", novelTitle: "", author: "", description: "" });
      router.push(`/projects/${p.id}`);
    } catch (err: any) {
      alert(err.message || "创建失败");
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">项目列表</h1>
        <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
          <DialogTrigger asChild>
            <Button>
              <Plus className="h-4 w-4 mr-2" />
              新建项目
            </Button>
          </DialogTrigger>
          <DialogContent>
            <DialogHeader>
              <DialogTitle>新建项目</DialogTitle>
            </DialogHeader>
            <div className="space-y-4 py-4">
              <div className="space-y-2">
                <Label>项目名称 *</Label>
                <Input value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} placeholder="给我起个名字" />
              </div>
              <div className="space-y-2">
                <Label>小说标题</Label>
                <Input value={form.novelTitle} onChange={(e) => setForm({ ...form, novelTitle: e.target.value })} placeholder="原著小说名称" />
              </div>
              <div className="space-y-2">
                <Label>作者</Label>
                <Input value={form.author} onChange={(e) => setForm({ ...form, author: e.target.value })} placeholder="原著作者" />
              </div>
              <div className="space-y-2">
                <Label>描述</Label>
                <Input value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} placeholder="项目描述" />
              </div>
            </div>
            <DialogFooter>
              <Button variant="outline" onClick={() => setDialogOpen(false)}>取消</Button>
              <Button onClick={handleCreate} disabled={!form.name}>创建</Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>
      </div>

      <div className="flex gap-2">
        <div className="relative flex-1 max-w-sm">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
          <Input
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
            onKeyDown={(e) => e.key === "Enter" && handleSearch()}
            placeholder="搜索项目名称/小说/作者..."
            className="pl-10"
          />
        </div>
        <Button variant="secondary" onClick={handleSearch}>搜索</Button>
      </div>

      {loading ? (
        <p className="text-muted-foreground">加载中...</p>
      ) : projects.length === 0 ? (
        <Card>
          <CardContent className="p-12 text-center text-muted-foreground">
            暂无项目，点击上方按钮创建第一个项目
          </CardContent>
        </Card>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {projects.map((p) => (
            <Card
              key={p.id}
              className="cursor-pointer hover:shadow-md transition-shadow"
              onClick={() => router.push(`/projects/${p.id}`)}
            >
              <CardHeader>
                <div className="flex items-start justify-between">
                  <CardTitle className="text-lg">{p.name}</CardTitle>
                  <Badge variant={p.status === 1 ? "success" : "secondary"}>
                    {p.status === 1 ? "活跃" : "已归档"}
                  </Badge>
                </div>
              </CardHeader>
              <CardContent>
                <div className="space-y-2 text-sm">
                  {p.novelTitle && (
                    <p><span className="text-muted-foreground">小说：</span>{p.novelTitle}</p>
                  )}
                  {p.author && (
                    <p><span className="text-muted-foreground">作者：</span>{p.author}</p>
                  )}
                  <p><span className="text-muted-foreground">章节：</span>{p.chapterCount || 0}</p>
                  <p className="text-xs text-muted-foreground">
                    更新于 {p.updateTime ? new Date(p.updateTime).toLocaleDateString() : "-"}
                  </p>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      {total > 20 && (
        <div className="flex justify-center gap-2">
          <Button variant="outline" disabled={page <= 1} onClick={() => setPage(page - 1)}>上一页</Button>
          <Button variant="outline" disabled={page * 20 >= total} onClick={() => setPage(page + 1)}>下一页</Button>
        </div>
      )}
    </div>
  );
}
