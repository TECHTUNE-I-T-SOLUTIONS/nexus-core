import { redirect } from "next/navigation"
import Dashboard from "@/components/dashboard"
import { getAuthUser } from "@/app/actions/auth"

export default async function SettingsPage() {
  const user = await getAuthUser()
  if (!user) redirect("/login?next=/dashboard/settings")
  return <Dashboard user={user} initialActive="Settings" />
}
