const API_BASE = process.env.NEXT_PUBLIC_API_BASE || "http://localhost:8080";

class ApiError extends Error {
  constructor(public status: number, message: string, public code?: number) {
    super(message);
    this.name = "ApiError";
  }
}

async function request<T>(
  path: string,
  options: RequestInit = {}
): Promise<T> {
  const token = typeof window !== "undefined" ? localStorage.getItem("token") : null;

  const headers: Record<string, string> = {
    ...(options.headers as Record<string, string>),
  };

  if (token) {
    headers["Authorization"] = token;
  }

  // Default Content-Type for JSON, but allow override
  if (!(options.body instanceof FormData) && !headers["Content-Type"]) {
    headers["Content-Type"] = "application/json";
  }

  const res = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers,
  });

  // Handle blob/binary responses
  if (res.headers.get("content-type")?.includes("application/octet-stream") ||
      res.headers.get("content-type")?.includes("application/pdf")) {
    return res as unknown as T;
  }

  let data: any;
  try {
    data = await res.json();
  } catch {
    // If JSON parsing fails, throw generic error
    throw new ApiError(res.status, "Server response error");
  }

  if (!res.ok || (data.code && data.code !== 200)) {
    const errorMessage = data.message || `Request failed with status ${res.status}`;
    const errorCode = data.code || res.status;
    
    // Handle 401 Unauthorized
    if (res.status === 401 || errorCode === 401) {
      if (typeof window !== "undefined") {
        localStorage.removeItem("token");
        // Optionally, you can redirect to login
        window.dispatchEvent(new CustomEvent("auth:logout"));
      }
    }
    
    throw new ApiError(res.status, errorMessage, errorCode);
  }

  return data.data !== undefined ? data.data : data;
}

function buildQuery(params: Record<string, string | number | boolean | undefined | null>): string {
  const qs = new URLSearchParams();
  Object.entries(params).forEach(([k, v]) => {
    if (v !== undefined && v !== null && v !== "") {
      qs.set(k, String(v));
    }
  });
  const s = qs.toString();
  return s ? `?${s}` : "";
}

// Auth
export const authApi = {
  login: (data: { username: string; password: string }) =>
    request<{ token: string; user: any }>("/api/system/auth/login", {
      method: "POST",
      body: JSON.stringify(data),
    }),
  register: (data: { username: string; password: string; nickname?: string; email?: string; phone?: string }) =>
    request<any>("/api/system/auth/register", {
      method: "POST",
      body: JSON.stringify(data),
    }),
  info: () => request<any>("/api/system/auth/info"),
  logout: () => request<void>("/api/system/auth/logout", { method: "POST" }),
};

// Dashboard
export const dashboardApi = {
  stats: () => request<{
    projectCount: number;
    projectMonthCount: number;
    taskCount: number;
    taskCompletedCount: number;
    taskProcessingCount: number;
    taskPendingCount: number;
    characterCount: number;
    chapterCount: number;
  }>("/api/dashboard/stats"),
};

// Projects
export const projectApi = {
  page: (params: { page?: number; pageSize?: number; userId?: number; keyword?: string }) =>
    request<{ page: number; pageSize: number; total: number; records: any[] }>(
      `/api/project/page${buildQuery(params as any)}`
    ),
  getById: (id: number) => request<any>(`/api/project/${id}`),
  create: (data: any) =>
    request<any>("/api/project", { method: "POST", body: JSON.stringify(data) }),
  update: (data: any) =>
    request<any>("/api/project", { method: "PUT", body: JSON.stringify(data) }),
  delete: (id: number) =>
    request<void>(`/api/project/${id}`, { method: "DELETE" }),
};

// Characters
export const characterApi = {
  listByProject: (projectId: number) =>
    request<any[]>(`/api/project/character/list/${projectId}`),
  getById: (id: number) => request<any>(`/api/project/character/${id}`),
  create: (data: any) =>
    request<any>("/api/project/character", { method: "POST", body: JSON.stringify(data) }),
  update: (id: number, data: any) =>
    request<any>(`/api/project/character/${id}`, { method: "PUT", body: JSON.stringify(data) }),
  delete: (id: number) =>
    request<void>(`/api/project/character/${id}`, { method: "DELETE" }),
};

// Chapters
export const chapterApi = {
  listByProject: (projectId: number) =>
    request<any[]>(`/api/project/novel/chapter/list/${projectId}`),
  getById: (id: number) => request<any>(`/api/project/novel/chapter/${id}`),
  create: (data: any) =>
    request<any>("/api/project/novel/chapter", { method: "POST", body: JSON.stringify(data) }),
  update: (data: any) =>
    request<any>("/api/project/novel/chapter", { method: "PUT", body: JSON.stringify(data) }),
  delete: (id: number) =>
    request<void>(`/api/project/novel/chapter/${id}`, { method: "DELETE" }),
};

// Scripts
export const scriptApi = {
  getById: (id: number) => request<any>(`/api/project/script/${id}`),
  getByProject: (projectId: number) => request<any>(`/api/project/script/project/${projectId}`),
  submitGeneration: (projectId: number, userId?: number) =>
    request<any>(`/api/project/script/generate/${projectId}${buildQuery({ userId } as any)}`, { method: "POST" }),
  listVersions: (scriptId: number) => request<any[]>(`/api/project/script/version/list/${scriptId}`),
  createVersion: (scriptId: number, yamlContent: string, changeLog?: string) =>
    request<any>(`/api/project/script/version/${scriptId}`, {
      method: "POST",
      body: JSON.stringify({ yamlContent, changeLog }),
    }),
  validate: (yamlContent: string) =>
    request<{ valid: boolean; errors: string[] }>("/api/project/script/validate", {
      method: "POST",
      headers: { "Content-Type": "text/plain" },
      body: yamlContent,
    }),
};

// Tasks
export const taskApi = {
  submit: (data: any, userId?: number) =>
    request<any>(`/api/task/submit${buildQuery({ userId } as any)}`, { method: "POST", body: JSON.stringify(data) }),
  getById: (id: number) => request<any>(`/api/task/${id}`),
  page: (params: { page?: number; pageSize?: number; projectId?: number; taskType?: string }) =>
    request<{ page: number; pageSize: number; total: number; records: any[] }>(
      `/api/task/page${buildQuery(params as any)}`
    ),
  cancel: (id: number) => request<void>(`/api/task/${id}/cancel`, { method: "POST" }),
  listLogs: (id: number) => request<any[]>(`/api/task/${id}/logs`),
  streamUrl: (id: number) => `${API_BASE}/api/task/${id}/stream`,
};

// Export
export const exportApi = {
  download: (scriptId: number, yamlContent: string, format: string) =>
    request<Blob>(`/api/export/${scriptId}${buildQuery({ format } as any)}`, {
      method: "POST",
      headers: { "Content-Type": "text/plain" },
      body: yamlContent,
    }),
};

// Roles
export const roleApi = {
  page: (params: { page?: number; pageSize?: number }) =>
    request<{ page: number; pageSize: number; total: number; records: any[] }>(
      `/api/system/role/page${buildQuery(params as any)}`
    ),
  list: () => request<any[]>("/api/system/role/list"),
  create: (data: any) => request<any>("/api/system/role", { method: "POST", body: JSON.stringify(data) }),
  update: (data: any) => request<any>("/api/system/role", { method: "PUT", body: JSON.stringify(data) }),
  delete: (id: number) => request<void>(`/api/system/role/${id}`, { method: "DELETE" }),
};

// Users (admin)
export const userApi = {
  page: (params: { page?: number; pageSize?: number; keyword?: string }) =>
    request<{ page: number; pageSize: number; total: number; records: any[] }>(
      `/api/system/user/page${buildQuery(params as any)}`
    ),
  getById: (id: number) => request<any>(`/api/system/user/${id}`),
  updateStatus: (id: number, status: number) =>
    request<void>(`/api/system/user/${id}/status${buildQuery({ status } as any)}`, { method: "PUT" }),
};

// Prompts
export const promptApi = {
  page: (params: { page?: number; pageSize?: number; category?: string; type?: string }) =>
    request<{ page: number; pageSize: number; total: number; records: any[] }>(
      `/api/prompt/page${buildQuery(params as any)}`
    ),
  listByCategory: (category: string) => request<any[]>(`/api/prompt/list/${category}`),
  getById: (id: number) => request<any>(`/api/prompt/${id}`),
  getByCode: (code: string) => request<any>(`/api/prompt/code/${code}`),
  create: (data: any) => request<any>("/api/prompt", { method: "POST", body: JSON.stringify(data) }),
  update: (data: any) => request<any>("/api/prompt", { method: "PUT", body: JSON.stringify(data) }),
  delete: (id: number) => request<void>(`/api/prompt/${id}`, { method: "DELETE" }),
};

export { ApiError };
