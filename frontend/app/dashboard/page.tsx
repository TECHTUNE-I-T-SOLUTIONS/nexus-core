import { redirect } from "next/navigation"
import Dashboard from "@/components/dashboard"
import { getAuthUser } from "@/app/actions/auth"

export default async function DashboardPage() {
  const user = await getAuthUser()
  if (!user) redirect("/login?next=/dashboard")
  return <Dashboard user={user} />
}
