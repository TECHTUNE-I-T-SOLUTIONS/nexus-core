export type JobStatus = "QUEUED" | "RUNNING" | "SUCCEEDED" | "FAILED" | "CANCELLED" | "RETRY_WAIT" | string

export type Job = {
  id: string
  tenantId: string
  jobType: string
  priority: number
  status: JobStatus
  scheduledAt?: string
  attemptCount: number
  maxAttempts: number
  lastErrorMessage?: string
  completedAt?: string
  createdAt: string
  updatedAt?: string
}

export class ApiClientError extends Error {
  constructor(public status: number, message: string) { super(message) }
}

const baseUrl = process.env.NEXUS_API_URL ?? "http://localhost:8080"

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const apiKey = process.env.NEXUS_API_KEY
  const response = await fetch(`${baseUrl}${path}`, {
    ...init,
    headers: { "Content-Type": "application/json", ...(apiKey ? { Authorization: `Bearer ${apiKey}` } : {}), ...init?.headers },
    cache: "no-store",
  })
  if (!response.ok) {
    const message = response.status === 401 ? "The backend requires an API key." : response.status === 403 ? "This API key lacks the required scope." : `Backend request failed (${response.status}).`
    throw new ApiClientError(response.status, message)
  }
  return response.status === 204 ? (undefined as T) : response.json()
}

export const apiClient = {
  listJobs: (params?: { status?: string; tenantId?: string }) => {
    const query = new URLSearchParams(params)
    return request<Job[]>(`/api/v1/jobs${query.size ? `?${query}` : ""}`)
  },
  getJob: (id: string) => request<Job>(`/api/v1/jobs/${id}`),
  cancelJob: (id: string) => request<Job>(`/api/v1/jobs/${id}/cancel`, { method: "POST" }),
  retryJob: (id: string) => request<Job>(`/api/v1/jobs/${id}/retry`, { method: "POST" }),
}
