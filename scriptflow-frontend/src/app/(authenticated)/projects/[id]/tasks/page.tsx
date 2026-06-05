"use client";

import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import { taskApi } from "@/lib/api-client";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogTrigger } from "@/components/ui/dialog";
import { ListChecks, XCircle } from "lucide-react";

const statusMap: Record<number, { label: string; variant: "info" | "success" | "warning" | "secondary" | "destructive" }> = {
  0: { label: "待处理", variant: "warning" },
  1: { label: "处理中", variant: "info" },
  2: { label: "已完成", variant: "success" },
  3: { label: "失败", variant: "destructive" },
  4: { label: "已取消", variant: "secondary" },
};

const taskTypeLabels: Record<string, string> = {
  novel_analysis: "小说分析",
  character_extract: "角色提取",
  script_generate: "剧本生成",
  script_revise: "剧本修改",
};

export default function TasksPage() {
  const params = useParams();
  const projectId = Number(params.id);
  const [tasks, setTasks] = useState<any[]>([]);
  const [logs, setLogs] = useState<any[]>([]);
  const [logDialogOpen, setLogDialogOpen] = useState(false);

  const load = () => {
    taskApi.page({ page: 1, pageSize: 50, projectId }).then((res) => {
      setTasks(res.records || []);
    }).catch(console.error);
  };

  useEffect(() => { load(); }, [projectId]);

  const handleCancel = async (id: number) => {
    if (!confirm("确定取消此任务？")) return;
    try {
      await taskApi.cancel(id);
      load();
    } catch (err: any) {
      alert(err.message || "取消失败");
    }
  };

  const showLogs = async (id: number) => {
    try {
      const logsData = await taskApi.listLogs(id);
      setLogs(logsData);
      setLogDialogOpen(true);
    } catch (err: any) {
      alert(err.message || "加载日志失败");
    }
  };

  return (
    <div className="space-y-4 h-full overflow-auto">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-semibold">任务历史 ({tasks.length})</h2>
      </div>

      {tasks.length === 0 ? (
        <p className="text-muted-foreground text-sm py-8 text-center">暂无任务</p>
      ) : (
        <div className="space-y-2">
          {tasks.map((t) => {
            const st = statusMap[t.status] || { label: "未知", variant: "secondary" as const };
            return (
              <Card key={t.id}>
                <CardContent className="p-4">
                  <div className="flex items-center justify-between">
                    <div className="flex-1">
                      <div className="flex items-center gap-2">
                        <span className="font-medium text-sm">{taskTypeLabels[t.taskType] || t.taskType}</span>
                        <Badge variant={st.variant}>{st.label}</Badge>
                      </div>
                      <div className="flex items-center gap-4 mt-1 text-xs text-muted-foreground">
                        <span>任务 ID: {t.id}</span>
                        <span>{t.createTime ? new Date(t.createTime).toLocaleString() : "-"}</span>
                        {t.progress !== undefined && (
                          <span>进度: {t.progress}%</span>
                        )}
                      </div>
                      {t.status === 1 && (
                        <div className="w-full max-w-xs h-1.5 bg-muted rounded-full overflow-hidden mt-2">
                          <div className="h-full bg-primary rounded-full transition-all" style={{ width: `${t.progress || 0}%` }} />
                        </div>
                      )}
                      {t.errorMsg && (
                        <p className="text-xs text-destructive mt-1">{t.errorMsg}</p>
                      )}
                    </div>
                    <div className="flex gap-1 ml-4">
                      <Button variant="ghost" size="sm" className="h-7 text-xs" onClick={() => showLogs(t.id)}>
                        <ListChecks className="h-3.5 w-3.5 mr-1" />
                        日志
                      </Button>
                      {(t.status === 0 || t.status === 1) && (
                        <Button variant="ghost" size="sm" className="h-7 text-xs text-destructive" onClick={() => handleCancel(t.id)}>
                          <XCircle className="h-3.5 w-3.5 mr-1" />
                          取消
                        </Button>
                      )}
                    </div>
                  </div>
                </CardContent>
              </Card>
            );
          })}
        </div>
      )}

      <Dialog open={logDialogOpen} onOpenChange={setLogDialogOpen}>
        <DialogContent className="max-w-lg max-h-[60vh] overflow-auto">
          <DialogHeader>
            <DialogTitle>任务日志</DialogTitle>
          </DialogHeader>
          {logs.length === 0 ? (
            <p className="text-sm text-muted-foreground py-4 text-center">暂无日志</p>
          ) : (
            <div className="space-y-3">
              {logs.map((log) => (
                <div key={log.id} className="text-sm border-l-2 border-primary pl-3">
                  <div className="flex items-center gap-2">
                    <span className="font-medium">{log.stage}</span>
                    <Badge variant={log.status === 2 ? "success" : log.status === 3 ? "destructive" : "warning"}>
                      {log.status === 2 ? "完成" : log.status === 3 ? "失败" : "处理中"}
                    </Badge>
                  </div>
                  {log.message && <p className="text-muted-foreground text-xs mt-1">{log.message}</p>}
                  <p className="text-[10px] text-muted-foreground mt-1">
                    {log.createTime ? new Date(log.createTime).toLocaleString() : ""}
                    {log.costTime ? ` · ${log.costTime}ms` : ""}
                  </p>
                </div>
              ))}
            </div>
          )}
        </DialogContent>
      </Dialog>
    </div>
  );
}
