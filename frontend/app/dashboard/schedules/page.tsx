import { redirect } from "next/navigation"
import Dashboard from "@/components/dashboard"
import { getAuthUser } from "@/app/actions/auth"

export default async function SchedulesPage() {
  const user = await getAuthUser()
  if (!user) redirect("/login?next=/dashboard/schedules")
  return <Dashboard user={user} initialActive="Schedules" />
}
