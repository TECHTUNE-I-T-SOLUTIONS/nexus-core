import { NextRequest, NextResponse } from "next/server"
import { cookies } from "next/headers"
import { fetchSchedules } from "@/lib/api-client"

export async function GET(request: NextRequest) {
  const cookieStore = await cookies()
  const token = cookieStore.get("nexus_token")?.value
  
  if (!token) {
    return NextResponse.json({ error: "Not authenticated" }, { status: 401 })
  }

  const tenantId = request.nextUrl.searchParams.get("tenantId")
  
  try {
    const schedules = await fetchSchedules(token, tenantId || undefined)
    return NextResponse.json(schedules)
  } catch (error) {
    console.error("Schedules fetch error:", error)
    return NextResponse.json({ error: "Failed to fetch schedules" }, { status: 502 })
  }
}
