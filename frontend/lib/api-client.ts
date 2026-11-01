const API_URL = process.env.NEXUS_API_URL || "http://localhost:8080";

export interface Job {
  id: string;
  tenantId: string;
  jobType: string;
  status: string;
  priority: number;
  createdAt: string;
  completedAt?: string;
}

export interface Schedule {
  id: string;
  tenantId: string;
  jobType: string;
  cronExpression: string;
  enabled: boolean;
  createdAt: string;
}

export interface WebhookEndpoint {
  id: string;
  tenantId: string;
  url: string;
  eventTypes: string[];
  enabled: boolean;
  createdAt: string;
}

export async function fetchJobs(token: string, tenantId?: string): Promise<Job[]> {
  const params = new URLSearchParams();
  if (tenantId) params.append("tenantId", tenantId);
  
  const response = await fetch(`${API_URL}/api/v1/jobs?${params}`, {
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`,
    },
  });

  if (!response.ok) {
    throw new Error("Failed to fetch jobs");
  }

  return response.json();
}

export async function fetchSchedules(token: string, tenantId?: string): Promise<Schedule[]> {
  const params = new URLSearchParams();
  if (tenantId) params.append("tenantId", tenantId);
  
  const response = await fetch(`${API_URL}/api/v1/schedules?${params}`, {
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`,
    },
  });

  if (!response.ok) {
    throw new Error("Failed to fetch schedules");
  }

  return response.json();
}

export async function fetchWebhooks(token: string, tenantId?: string): Promise<WebhookEndpoint[]> {
  const params = new URLSearchParams();
  if (tenantId) params.append("tenantId", tenantId);
  
  const response = await fetch(`${API_URL}/api/v1/webhook-endpoints?${params}`, {
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`,
    },
  });

  if (!response.ok) {
    throw new Error("Failed to fetch webhooks");
  }

  return response.json();
}
