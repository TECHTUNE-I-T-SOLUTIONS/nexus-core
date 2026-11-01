import { redirect } from "next/navigation"
import Dashboard from "@/components/dashboard"
import { getAuthUser } from "@/app/actions/auth"

export default async function JobsPage() {
  const user = await getAuthUser()
  if (!user) redirect("/login?next=/dashboard/jobs")
  return <Dashboard user={user} initialActive="Jobs" />
}
