"use server"

import { cookies } from "next/headers"
import { login, register, getCurrentUser, type LoginRequest, type RegisterRequest } from "@/lib/auth-client"

const API_URL = process.env.NEXUS_API_URL || "http://localhost:8080"

export async function loginUser(data: LoginRequest) {
  try {
    const response = await login(data)
    cookies().set("nexus_token", response.token, {
      httpOnly: true,
      secure: process.env.NODE_ENV === "production",
      sameSite: "lax",
      maxAge: 60 * 60 * 24 * 7, // 7 days
      path: "/",
    })
    return { success: true, user: response.user }
  } catch (error: any) {
    return { success: false, error: error.message }
  }
}

export async function registerUser(data: RegisterRequest) {
  try {
    const user = await register(data)
    return { success: true, user }
  } catch (error: any) {
    return { success: false, error: error.message }
  }
}

export async function logoutUser() {
  cookies().delete("nexus_token")
  return { success: true }
}

export async function getAuthUser() {
  const token = cookies().get("nexus_token")?.value
  if (!token) {
    return null
  }
  try {
    return await getCurrentUser(token)
  } catch {
    return null
  }
}
