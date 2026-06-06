"use client";

import { useState, useRef, useEffect } from "react";
import { taskApi, chapterApi } from "@/lib/api-client";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { Send, Bot, User, Loader2 } from "lucide-react";

interface Message {
  role: "user" | "assistant";
  content: string;
  time: string;
}

interface AiAssistantProps {
  projectId: number;
  scriptId?: number;
}

// Simple task type commands
const taskCommands = [
  { type: "novel_analysis", label: "分析小说" },
  { type: "character_extract", label: "提取角色" },
  { type: "script_generate", label: "生成剧本" },
  { type: "script_revise", label: "修改剧本" },
];

export default function AiAssistant({ projectId }: AiAssistantProps) {
  const [messages, setMessages] = useState<Message[]>([
    {
      role: "assistant",
      content: "你好！我是 AI 创作助手。我可以帮你分析小说、提取角色、生成剧本。选择下面的任务类型开始，或者直接输入指令。",
      time: new Date().toLocaleTimeString(),
    },
  ]);
  const [input, setInput] = useState("");
  const [loading, setLoading] = useState(false);
  const [taskId, setTaskId] = useState<number | null>(null);
  const [taskProgress, setTaskProgress] = useState(0);
  const [taskStatus, setTaskStatus] = useState<string>("");
  const messagesEndRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages]);

  // SSE for task progress
  useEffect(() => {
    if (!taskId) return;

    const token = localStorage.getItem("token") || "";
    const url = `${taskApi.streamUrl(taskId)}?token=${encodeURIComponent(token)}`;
    const eventSource = new EventSource(url);

    eventSource.addEventListener("connected", () => {
      setTaskStatus("任务已连接，等待更新...");
    });

    eventSource.addEventListener("progress", (event) => {
      try {
        const data = JSON.parse(event.data);
        setTaskProgress(data.progress || 0);

        if (data.status === 2) {
          setTaskStatus("任务已完成！");
          setMessages((prev) => [
            ...prev,
            {
              role: "assistant",
              content: `✅ 任务已完成！进度 100%。${data.result ? "\n\n结果已保存。" : ""}`,
              time: new Date().toLocaleTimeString(),
            },
          ]);
          setTaskId(null);
          setLoading(false);
        } else if (data.status === 3) {
          setTaskStatus("任务失败");
          setMessages((prev) => [
            ...prev,
            {
              role: "assistant",
              content: `❌ 任务处理失败：${data.error || "未知错误"}`,
              time: new Date().toLocaleTimeString(),
            },
          ]);
          setTaskId(null);
          setLoading(false);
        } else {
          setTaskStatus(`处理中... ${data.progress || 0}%`);
        }
      } catch {}
    });

    eventSource.onerror = () => {
      // SSE connection closed
      eventSource.close();
    };

    return () => eventSource.close();
  }, [taskId]);

  /** Fetch all chapter content for this project */
  const loadNovelContent = async (): Promise<string> => {
    try {
      const chapters = await chapterApi.listByProject(projectId);
      if (!chapters || chapters.length === 0) return "";
      return chapters
        .sort((a: any, b: any) => (a.chapterNo || 0) - (b.chapterNo || 0))
        .map((c: any) => `## 第${c.chapterNo}章 ${c.title || ""}\n${c.content || ""}`)
        .join("\n\n");
    } catch {
      return "";
    }
  };

  const handleSend = async () => {
    if (!input.trim() || loading) return;

    const userMsg: Message = {
      role: "user",
      content: input.trim(),
      time: new Date().toLocaleTimeString(),
    };
    setMessages((prev) => [...prev, userMsg]);
    setInput("");
    setLoading(true);

    try {
      // Load actual chapter content to include in the request
      const novelContent = await loadNovelContent();
      const params: Record<string, string> = { instruction: input.trim() };
      if (novelContent) params["novelContent"] = novelContent;
      const result = await taskApi.submit(
        { projectId, taskType: "script_revise", params: JSON.stringify(params) },
        0
      );
      setTaskId(result.id);
      setTaskProgress(0);
      setTaskStatus("任务已提交，等待处理...");
      setMessages((prev) => [
        ...prev,
        {
          role: "assistant",
          content: `📤 任务已提交 (ID: ${result.id})，正在处理中...`,
          time: new Date().toLocaleTimeString(),
        },
      ]);
    } catch (err: any) {
      setMessages((prev) => [
        ...prev,
        {
          role: "assistant",
          content: `❌ 提交失败：${err.message}`,
          time: new Date().toLocaleTimeString(),
        },
      ]);
      setLoading(false);
    }
  };

  const handleQuickTask = async (taskType: string, label: string) => {
    const userMsg: Message = {
      role: "user",
      content: `开始任务：${label}`,
      time: new Date().toLocaleTimeString(),
    };
    setMessages((prev) => [...prev, userMsg]);
    setLoading(true);

    try {
      // Load actual chapter content to include in the request
      const novelContent = await loadNovelContent();
      const params: Record<string, string> = {};
      if (novelContent) params["novelContent"] = novelContent;
      const result = await taskApi.submit({ projectId, taskType, params: JSON.stringify(params) }, 0);
      setTaskId(result.id);
      setTaskProgress(0);
      setTaskStatus("任务已提交，等待处理...");
    } catch (err: any) {
      setMessages((prev) => [
        ...prev,
        {
          role: "assistant",
          content: `❌ 提交失败：${err.message}`,
          time: new Date().toLocaleTimeString(),
        },
      ]);
      setLoading(false);
    }
  };

  return (
    <div className="flex flex-col h-full">
      <div className="p-3 border-b">
        <h3 className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">AI 创作助手</h3>
      </div>

      <div className="flex-1 overflow-auto p-3 space-y-3">
        {messages.map((msg, i) => (
          <div key={i} className={`flex gap-2 ${msg.role === "user" ? "flex-row-reverse" : ""}`}>
            <div className={`flex-shrink-0 w-7 h-7 rounded-full flex items-center justify-center ${
              msg.role === "user" ? "bg-primary text-primary-foreground" : "bg-muted"
            }`}>
              {msg.role === "user" ? <User className="h-3.5 w-3.5" /> : <Bot className="h-3.5 w-3.5" />}
            </div>
            <div className={`max-w-[80%] rounded-lg px-3 py-2 text-sm ${
              msg.role === "user" ? "bg-primary text-primary-foreground" : "bg-muted"
            }`}>
              <p className="whitespace-pre-wrap">{msg.content}</p>
              <p className="text-[10px] mt-1 opacity-60">{msg.time}</p>
            </div>
          </div>
        ))}

        {loading && (
          <div className="flex items-center gap-2 text-sm text-muted-foreground">
            <Loader2 className="h-3 w-3 animate-spin" />
            {taskStatus || "处理中..."}
            {taskId && taskProgress > 0 && (
              <div className="w-16 h-1.5 bg-muted rounded-full overflow-hidden">
                <div className="h-full bg-primary rounded-full" style={{ width: `${taskProgress}%` }} />
              </div>
            )}
          </div>
        )}

        <div ref={messagesEndRef} />
      </div>

      <div className="p-3 border-t space-y-2">
        {!loading && (
          <div className="flex flex-wrap gap-1">
            {taskCommands.map((cmd) => (
              <Badge
                key={cmd.type}
                variant="outline"
                className="cursor-pointer hover:bg-accent text-xs"
                onClick={() => handleQuickTask(cmd.type, cmd.label)}
              >
                {cmd.label}
              </Badge>
            ))}
          </div>
        )}
        <div className="flex gap-2">
          <Input
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={(e) => e.key === "Enter" && handleSend()}
            placeholder="输入指令..."
            disabled={loading}
            className="text-sm"
          />
          <Button size="icon" onClick={handleSend} disabled={loading || !input.trim()}>
            <Send className="h-4 w-4" />
          </Button>
        </div>
      </div>
    </div>
  );
}
