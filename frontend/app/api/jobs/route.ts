import { NextRequest, NextResponse } from "next/server"
import { apiClient, ApiClientError } from "@/lib/api-client"

export async function GET(request: NextRequest) {
  const params = new URLSearchParams()
  const status = request.nextUrl.searchParams.get("status")
  const tenantId = request.nextUrl.searchParams.get("tenantId")
  if (status) params.set("status", status)
  if (tenantId) params.set("tenantId", tenantId)

  try {
    const jobs = await apiClient.listJobs(Object.fromEntries(params.entries()))
    return NextResponse.json(jobs)
  } catch (error) {
    const statusCode = error instanceof ApiClientError ? error.status : 502
    const message = error instanceof Error ? error.message : "Unable to reach NEXUS CORE."
    return NextResponse.json({ message }, { status: statusCode })
  }
}
