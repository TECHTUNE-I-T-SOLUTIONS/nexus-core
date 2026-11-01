"use client"

import Link from "next/link"
import Image from "next/image"
import { ArrowLeft, Eye, EyeOff, LockKeyhole } from "lucide-react"
import { useState } from "react"
import { useRouter } from "next/navigation"
import { loginUser, registerUser, requestPasswordReset } from "@/app/actions/auth"
import toast from "react-hot-toast"

export function AuthPage({ mode }: { mode: "login" | "signup" | "forgot" }) {
  const [showPassword, setShowPassword] = useState(false)
  const [loading, setLoading] = useState(false)
  const router = useRouter()
  
  const copy = mode === "login" 
    ? { title: "Welcome back", subtitle: "Sign in to your NEXUS CORE workspace.", action: "Sign in", footer: "New to NEXUS CORE?", link: "Create an account", href: "/signup" } 
    : mode === "signup" 
    ? { title: "Create your workspace", subtitle: "Start exploring the NEXUS CORE developer platform.", action: "Create account", footer: "Already have an account?", link: "Sign in", href: "/login" } 
    : { title: "Recover access", subtitle: "Request a password recovery link for your workspace.", action: "Request recovery link", footer: "Remembered your password?", link: "Back to sign in", href: "/login" }

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setLoading(true)

    const formData = new FormData(event.currentTarget)
    
    try {
      if (mode === "signup") {
        const result = await registerUser({
          email: formData.get("email") as string,
          password: formData.get("password") as string,
          fullName: formData.get("fullName") as string,
        })
        if (!result.success) {
          toast.error(result.error || "Registration failed")
        } else {
          toast.success("Account created! Please sign in.")
          setTimeout(() => router.push("/login"), 2000)
        }
      } else if (mode === "login") {
        const result = await loginUser({
          email: formData.get("email") as string,
          password: formData.get("password") as string,
        })
        if (!result.success) {
          toast.error(result.error || "Login failed")
        } else {
          toast.success("Login successful!")
          router.push("/dashboard")
        }
      } else if (mode === "forgot") {
        const result = await requestPasswordReset({
          email: formData.get("email") as string,
        })
        if (!result.success) {
          toast.error(result.error || "Failed to request password reset")
        } else {
          toast.success("Password reset email sent if email exists.")
        }
      }
    } catch (err: any) {
      toast.error(err.message || "An error occurred")
    } finally {
      setLoading(false)
    }
  }

  return (
    <main className="auth-page">
      <div className="auth-aside">
        <Link href="/" className="brand">
          <span className="brand-mark">
            <Image src="/logo.png" alt="NEXUS CORE" width={36} height={36} />
          </span>
          <span><strong>NEXUS</strong><small>CORE</small></span>
        </Link>
        <div>
          <span className="eyebrow">Developer operations, without the guesswork.</span>
          <h1>Build reliable work into your product.</h1>
          <p>Secure authentication with httpOnly cookies.</p>
        </div>
        <Link href="/" className="back-link"><ArrowLeft /> Back to website</Link>
      </div>
      <section className="auth-panel">
        <div className="auth-card">
          <div className="auth-card-heading">
            <div className="auth-icon"><LockKeyhole /></div>
            <h2>{copy.title}</h2>
            <p>{copy.subtitle}</p>
          </div>
          <form onSubmit={handleSubmit}>
            <label>Email address
              <input type="email" name="email" placeholder="you@company.com" required />
            </label>
            {mode !== "forgot" && (
              <label>Password
                <div className="password-field">
                  <input 
                    type={showPassword ? "text" : "password"} 
                    name="password" 
                    placeholder="••••••••" 
                    minLength={8} 
                    required 
                  />
                  <button 
                    type="button" 
                    aria-label={showPassword ? "Hide password" : "Show password"} 
                    onClick={() => setShowPassword(!showPassword)}
                  >
                    {showPassword ? <EyeOff /> : <Eye />}
                  </button>
                </div>
              </label>
            )}
            {mode === "signup" && (
              <label>Full name
                <input type="text" name="fullName" placeholder="Your name" required />
              </label>
            )}
            {mode === "signup" && (
              <label className="checkbox-row">
                <input type="checkbox" required /> 
                <span>I agree to the workspace terms and understand access is governed by backend authorization.</span>
              </label>
            )}
            <button className="button button-primary auth-submit" type="submit" disabled={loading}>
              {loading ? "Processing..." : copy.action}
            </button>
          </form>
          <p className="auth-footer">{copy.footer} <Link href={copy.href}>{copy.link}</Link></p>
          {mode === "login" && (
            <p className="auth-legal"><Link href="/forgot-password">Forgot password?</Link></p>
          )}
        </div>
      </section>
    </main>
  )
}
