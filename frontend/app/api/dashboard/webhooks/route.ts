import { NextRequest, NextResponse } from "next/server"
import { cookies } from "next/headers"
import { fetchWebhooks } from "@/lib/api-client"

export async function GET(request: NextRequest) {
  const cookieStore = await cookies()
  const token = cookieStore.get("nexus_token")?.value
  
  if (!token) {
    return NextResponse.json({ error: "Not authenticated" }, { status: 401 })
  }

  const tenantId = request.nextUrl.searchParams.get("tenantId")
  
  try {
    const webhooks = await fetchWebhooks(token, tenantId || undefined)
    return NextResponse.json(webhooks)
  } catch (error) {
    console.error("Webhooks fetch error:", error)
    return NextResponse.json({ error: "Failed to fetch webhooks" }, { status: 502 })
  }
}
