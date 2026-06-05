"use client";

import { useEffect, useState } from "react";
import { useParams, usePathname, useRouter } from "next/navigation";
import { projectApi } from "@/lib/api-client";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Button } from "@/components/ui/button";
import { ArrowLeft } from "lucide-react";

export default function ProjectLayout({ children }: { children: React.ReactNode }) {
  const params = useParams();
  const pathname = usePathname();
  const router = useRouter();
  const [projectName, setProjectName] = useState("");
  const projectId = params.id as string;

  useEffect(() => {
    if (projectId) {
      projectApi.getById(Number(projectId)).then((p) => {
        setProjectName(p.name);
      }).catch(() => {});
    }
  }, [projectId]);

  const currentTab = pathname.endsWith("/characters")
    ? "characters"
    : pathname.endsWith("/chapters")
    ? "chapters"
    : pathname.endsWith("/tasks")
    ? "tasks"
    : "workspace";

  return (
    <div className="h-full flex flex-col">
      <div className="flex items-center gap-4 mb-4">
        <Button variant="ghost" size="icon" onClick={() => router.push("/projects")} className="h-8 w-8">
          <ArrowLeft className="h-4 w-4" />
        </Button>
        <h1 className="text-xl font-bold">{projectName || "加载中..."}</h1>
      </div>

      <Tabs value={currentTab} className="mb-4">
        <TabsList>
          <TabsTrigger value="workspace" onClick={() => router.push(`/projects/${projectId}`)}>
            剧本工作台
          </TabsTrigger>
          <TabsTrigger value="characters" onClick={() => router.push(`/projects/${projectId}/characters`)}>
            角色管理
          </TabsTrigger>
          <TabsTrigger value="chapters" onClick={() => router.push(`/projects/${projectId}/chapters`)}>
            章节管理
          </TabsTrigger>
          <TabsTrigger value="tasks" onClick={() => router.push(`/projects/${projectId}/tasks`)}>
            任务历史
          </TabsTrigger>
        </TabsList>
      </Tabs>

      <div className="flex-1 overflow-hidden">
        {children}
      </div>
    </div>
  );
}
