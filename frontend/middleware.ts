import { cookies } from "next/headers"
import { NextResponse } from "next/server"
import type { NextRequest } from "next/server"

export async function middleware(request: NextRequest) {
  const token = cookies().get("nexus_token")
  
  // Allow public routes
  if (request.nextUrl.pathname === "/" || 
      request.nextUrl.pathname.startsWith("/login") || 
      request.nextUrl.pathname.startsWith("/signup") || 
      request.nextUrl.pathname.startsWith("/forgot-password") ||
      request.nextUrl.pathname.startsWith("/features") ||
      request.nextUrl.pathname.startsWith("/use-cases") ||
      request.nextUrl.pathname.startsWith("/docs")) {
    return NextResponse.next()
  }

  // Protect dashboard and API routes
  if (request.nextUrl.pathname.startsWith("/dashboard") || 
      request.nextUrl.pathname.startsWith("/api")) {
    if (!token) {
      return NextResponse.redirect(new URL("/login", request.url))
    }
  }

  return NextResponse.next()
}

export const config = {
  matcher: ["/", "/login", "/signup", "/forgot-password", "/dashboard/:path*", "/api/:path*"],
}
