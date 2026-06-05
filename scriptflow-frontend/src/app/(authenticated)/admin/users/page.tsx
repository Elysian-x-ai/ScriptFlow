"use client";

import { useEffect, useState } from "react";
import { userApi } from "@/lib/api-client";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Search, ShieldCheck, ShieldX } from "lucide-react";

export default function AdminUsersPage() {
  const [users, setUsers] = useState<any[]>([]);
  const [keyword, setKeyword] = useState("");
  const [page, setPage] = useState(1);

  const load = (p: number, kw: string) => {
    userApi.page({ page: p, pageSize: 20, keyword: kw || undefined }).then((res) => {
      setUsers(res.records || []);
    }).catch(console.error);
  };

  useEffect(() => { load(page, keyword); }, [page, keyword]);

  const handleSearch = () => { setPage(1); };

  const toggleStatus = async (id: number, currentStatus: number) => {
    try {
      await userApi.updateStatus(id, currentStatus === 1 ? 0 : 1);
      load(page, keyword);
    } catch (err: any) {
      alert(err.message);
    }
  };

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold">用户管理</h1>

      <div className="flex gap-2">
        <div className="relative flex-1 max-w-sm">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
          <Input
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
            onKeyDown={(e) => e.key === "Enter" && handleSearch()}
            placeholder="搜索用户名/昵称/邮箱..."
            className="pl-10"
          />
        </div>
        <Button variant="secondary" onClick={handleSearch}>搜索</Button>
      </div>

      <Card>
        <CardContent className="p-0">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b bg-muted/50">
                <th className="text-left p-3 font-medium">ID</th>
                <th className="text-left p-3 font-medium">用户名</th>
                <th className="text-left p-3 font-medium">昵称</th>
                <th className="text-left p-3 font-medium">邮箱</th>
                <th className="text-left p-3 font-medium">手机</th>
                <th className="text-left p-3 font-medium">状态</th>
                <th className="text-left p-3 font-medium">创建时间</th>
                <th className="text-left p-3 font-medium">操作</th>
              </tr>
            </thead>
            <tbody>
              {users.map((u) => (
                <tr key={u.id} className="border-b hover:bg-muted/30">
                  <td className="p-3 text-muted-foreground">{u.id}</td>
                  <td className="p-3 font-medium">{u.username}</td>
                  <td className="p-3">{u.nickname || "-"}</td>
                  <td className="p-3">{u.email || "-"}</td>
                  <td className="p-3">{u.phone || "-"}</td>
                  <td className="p-3">
                    <Badge variant={u.status === 1 ? "success" : "secondary"}>
                      {u.status === 1 ? "启用" : "禁用"}
                    </Badge>
                  </td>
                  <td className="p-3 text-xs text-muted-foreground">
                    {u.createTime ? new Date(u.createTime).toLocaleDateString() : "-"}
                  </td>
                  <td className="p-3">
                    <Button
                      variant="ghost"
                      size="sm"
                      className="h-7 text-xs"
                      onClick={() => toggleStatus(u.id, u.status)}
                    >
                      {u.status === 1 ? (
                        <><ShieldX className="h-3.5 w-3.5 mr-1" />禁用</>
                      ) : (
                        <><ShieldCheck className="h-3.5 w-3.5 mr-1" />启用</>
                      )}
                    </Button>
                  </td>
                </tr>
              ))}
              {users.length === 0 && (
                <tr>
                  <td colSpan={8} className="p-8 text-center text-muted-foreground">暂无数据</td>
                </tr>
              )}
            </tbody>
          </table>
        </CardContent>
      </Card>
    </div>
  );
}
