"use server"

import { cookies } from "next/headers"
import { login, register, getCurrentUser, forgotPassword, type LoginRequest, type RegisterRequest, type ForgotPasswordRequest } from "@/lib/auth-client"

const API_URL = process.env.NEXUS_API_URL || "http://localhost:8080"

export async function registerUser(data: RegisterRequest) {
  try {
    const user = await register(data)
    return { success: true, user }
  } catch (error: any) {
    console.error("Registration error:", error.message)
    return { success: false, error: error.message || "Registration failed" }
  }
}

export async function loginUser(data: LoginRequest) {
  try {
    const response = await login(data)
    const cookieStore = await cookies()
    cookieStore.set("nexus_token", response.token, {
      httpOnly: true,
      secure: process.env.NODE_ENV === "production",
      sameSite: "lax",
      maxAge: 60 * 60 * 24 * 7, // 7 days
      path: "/",
    })
    return { success: true, user: response.user }
  } catch (error: any) {
    console.error("Login error:", error.message)
    return { success: false, error: "Invalid email or password" }
  }
}

export async function requestPasswordReset(data: ForgotPasswordRequest) {
  try {
    await forgotPassword(data)
    return { success: true }
  } catch (error: any) {
    console.error("Password reset error:", error.message)
    return { success: false, error: error.message || "Failed to request password reset" }
  }
}

export async function logoutUser() {
  const cookieStore = await cookies()
  cookieStore.delete("nexus_token")
  return { success: true }
}

export async function getAuthUser() {
  const cookieStore = await cookies()
  const token = cookieStore.get("nexus_token")?.value
  if (!token) {
    return null
  }
  try {
    return await getCurrentUser(token)
  } catch {
    return null
  }
}
