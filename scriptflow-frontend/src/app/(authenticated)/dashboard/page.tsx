"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { dashboardApi, projectApi, taskApi } from "@/lib/api-client";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Separator } from "@/components/ui/separator";
import { FolderOpen, ListChecks, Play, CheckCircle2, Plus } from "lucide-react";

const statusMap: Record<number, { label: string; variant: "info" | "success" | "warning" | "secondary" | "destructive" }> = {
  0: { label: "待处理", variant: "warning" },
  1: { label: "处理中", variant: "info" },
  2: { label: "已完成", variant: "success" },
  3: { label: "失败", variant: "destructive" },
  4: { label: "已取消", variant: "secondary" },
};

export default function DashboardPage() {
  const [stats, setStats] = useState<any>(null);
  const [recentProjects, setRecentProjects] = useState<any[]>([]);
  const [recentTasks, setRecentTasks] = useState<any[]>([]);
  const router = useRouter();

  useEffect(() => {
    Promise.all([
      dashboardApi.stats(),
      projectApi.page({ page: 1, pageSize: 5 }),
      taskApi.page({ page: 1, pageSize: 5 }),
    ]).then(([s, p, t]) => {
      setStats(s);
      setRecentProjects(p.records || []);
      setRecentTasks(t.records || []);
    }).catch(console.error);
  }, []);

  const statCards = stats
    ? [
        { title: "项目总数", value: stats.projectCount, icon: FolderOpen, color: "text-blue-600" },
        { title: "本月新增", value: stats.projectMonthCount, icon: Plus, color: "text-green-600" },
        { title: "进行中任务", value: stats.taskProcessingCount, icon: Play, color: "text-orange-600" },
        { title: "已完成任务", value: stats.taskCompletedCount, icon: CheckCircle2, color: "text-green-600" },
      ]
    : [];

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">看板</h1>
        <Button onClick={() => router.push("/projects")}>
          <Plus className="h-4 w-4 mr-2" />
          新建项目
        </Button>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        {statCards.map((s) => (
          <Card key={s.title}>
            <CardContent className="p-6">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm text-muted-foreground">{s.title}</p>
                  <p className="text-3xl font-bold mt-1">{s.value}</p>
                </div>
                <s.icon className={`h-8 w-8 ${s.color} opacity-70`} />
              </div>
            </CardContent>
          </Card>
        ))}
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <Card>
          <CardHeader>
            <CardTitle className="text-lg">最近项目</CardTitle>
          </CardHeader>
          <CardContent>
            {recentProjects.length === 0 ? (
              <p className="text-sm text-muted-foreground py-4 text-center">暂无项目</p>
            ) : (
              <div className="space-y-3">
                {recentProjects.map((p) => (
                  <div
                    key={p.id}
                    className="flex items-center justify-between p-3 rounded-md hover:bg-accent cursor-pointer transition-colors"
                    onClick={() => router.push(`/projects/${p.id}`)}
                  >
                    <div>
                      <p className="font-medium">{p.name}</p>
                      <p className="text-xs text-muted-foreground">
                        {p.novelTitle || "未设置小说"} · {p.chapterCount || 0} 章节
                      </p>
                    </div>
                    <Badge variant={p.status === 1 ? "success" : "secondary"}>
                      {p.status === 1 ? "活跃" : "已归档"}
                    </Badge>
                  </div>
                ))}
              </div>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="text-lg">最近任务</CardTitle>
          </CardHeader>
          <CardContent>
            {recentTasks.length === 0 ? (
              <p className="text-sm text-muted-foreground py-4 text-center">暂无任务</p>
            ) : (
              <div className="space-y-3">
                {recentTasks.map((t) => {
                  const st = statusMap[t.status] || { label: "未知", variant: "secondary" as const };
                  return (
                    <div key={t.id} className="flex items-center justify-between p-3 rounded-md hover:bg-accent transition-colors">
                      <div>
                        <p className="font-medium text-sm">{t.taskType}</p>
                        <p className="text-xs text-muted-foreground">{new Date(t.createTime).toLocaleString()}</p>
                      </div>
                      <div className="flex items-center gap-2">
                        {t.status === 1 && (
                          <div className="w-16 h-1.5 bg-muted rounded-full overflow-hidden">
                            <div className="h-full bg-primary rounded-full" style={{ width: `${t.progress || 0}%` }} />
                          </div>
                        )}
                        <Badge variant={st.variant}>{st.label}</Badge>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
