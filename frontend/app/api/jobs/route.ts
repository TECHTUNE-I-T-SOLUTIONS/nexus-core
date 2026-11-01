import { NextRequest, NextResponse } from "next/server"
import { fetchJobs } from "@/lib/api-client"

export async function GET(request: NextRequest) {
  const token = request.cookies.get("nexus_token")?.value
  if (!token) {
    return NextResponse.json({ message: "Not authenticated" }, { status: 401 })
  }

  const tenantId = request.nextUrl.searchParams.get("tenantId")
  
  try {
    const jobs = await fetchJobs(token, tenantId || undefined)
    return NextResponse.json(jobs)
  } catch (error) {
    const message = error instanceof Error ? error.message : "Unable to reach NEXUS CORE."
    return NextResponse.json({ message }, { status: 502 })
  }
}
