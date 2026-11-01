"use client"

import Image from "next/image"
import { useMemo, useState, useEffect } from "react"
import { useRouter } from "next/navigation"
import { Activity, AlertTriangle, BarChart3, Bell, CheckCircle2, ChevronDown, CircleHelp, Clock3, Code2, ExternalLink, FileText, Gauge, Layers3, ListChecks, MoreHorizontal, Package, Play, Plus, Search, Server, Settings2, ShieldCheck, Terminal, Webhook, XCircle, Zap, LogOut } from "lucide-react"
import { Badge, Card } from "@/components/ui"
import { cn } from "@/lib/utils"
import { logoutUser, getAuthUser } from "@/app/actions/auth"
import { fetchJobs, fetchSchedules, fetchWebhooks } from "@/lib/api-client"

type NavItem = { label: string; icon: typeof Activity; count?: string }
const nav: NavItem[] = [
  { label: "Overview", icon: Gauge }, { label: "Jobs", icon: ListChecks, count: "—" }, { label: "Schedules", icon: Clock3 }, { label: "Webhooks", icon: Webhook }, { label: "Dead letters", icon: AlertTriangle, count: "—" }, { label: "Tenants", icon: Layers3 },
]

function Sidebar({ active, onChange }: { active: string; onChange: (value: string) => void }) {
  return <aside className="hidden w-[238px] shrink-0 border-r border-[var(--border)] bg-[#0e151d] p-4 md:flex md:flex-col">
    <div className="flex items-center gap-3 px-2 py-3"><div className="flex size-9 items-center justify-center rounded-lg bg-[var(--primary)] text-white shadow-[0_0_24px_#3157d555]"><Image src="/logo.png" alt="NEXUS CORE" width={36} height={36} className="rounded-lg" /></div><div><p className="text-[13px] font-bold tracking-[0.18em] text-white">NEXUS</p><p className="text-[10px] tracking-[0.14em] text-[var(--muted-foreground)]">CORE OPS</p></div></div>
    <div className="my-5 flex items-center gap-2 rounded-lg border border-[var(--border)] bg-[var(--muted)] px-3 py-2"><div className="size-2 rounded-full bg-[var(--success)]" /><span className="text-xs font-medium text-white">Production</span><ChevronDown className="ml-auto size-3.5 text-[var(--muted-foreground)]" /></div>
    <p className="px-2 pb-2 text-[10px] font-semibold uppercase tracking-[0.16em] text-[var(--muted-foreground)]">Workspace</p>
    <nav className="flex flex-col gap-1">{nav.map(({ label, icon: Icon, count }) => <button key={label} onClick={() => onChange(label)} className={cn("flex items-center gap-3 rounded-lg px-3 py-2.5 text-left text-sm transition", active === label ? "bg-[#1c2a42] text-white" : "text-[var(--muted-foreground)] hover:bg-[var(--muted)] hover:text-white")}><Icon className="size-4" /><span>{label}</span>{count && <span className="ml-auto rounded bg-[var(--muted)] px-1.5 py-0.5 text-[10px]">{count}</span>}</button>)}</nav>
    <p className="mt-7 px-2 pb-2 text-[10px] font-semibold uppercase tracking-[0.16em] text-[var(--muted-foreground)]">System</p>
    <nav className="flex flex-col gap-1">{[[Settings2, "Settings"], [FileText, "API docs"], [Activity, "Activity log"]].map(([Icon, label]) => <button key={label as string} className="flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm text-[var(--muted-foreground)] hover:bg-[var(--muted)] hover:text-white"><Icon className="size-4" />{label as string}</button>)}</nav>
    <div className="mt-auto rounded-xl border border-[var(--border)] bg-[#121d27] p-3"><div className="mb-2 flex items-center gap-2"><ShieldCheck className="size-4 text-[var(--success)]" /><span className="text-xs font-semibold text-white">API status</span></div><p className="text-[11px] leading-4 text-[var(--muted-foreground)]">Secure bearer authentication is configured by the backend.</p><button className="mt-3 flex items-center gap-1 text-[11px] font-semibold text-[var(--primary)]">View security settings <ExternalLink className="size-3" /></button></div>
  </aside>
}

function Topbar({ title }: { title: string }) { 
  const router = useRouter()
  
  const handleLogout = async () => {
    await logoutUser()
    router.push("/login")
  }
  
  return <header className="flex h-[72px] items-center justify-between border-b border-[var(--border)] px-5 md:px-8"><div><p className="text-[11px] uppercase tracking-[0.18em] text-[var(--muted-foreground)]">Operations workspace</p><h1 className="mt-1 text-xl font-semibold tracking-tight text-white">{title}</h1></div><div className="flex items-center gap-2"><button className="hidden items-center gap-2 rounded-lg border border-[var(--border)] px-3 py-2 text-xs text-[var(--muted-foreground)] hover:text-white sm:flex"><Search className="size-3.5" /> Search <kbd className="rounded bg-[var(--muted)] px-1.5 py-0.5 text-[10px]">⌘ K</kbd></button><button className="relative rounded-lg border border-[var(--border)] p-2 text-[var(--muted-foreground)] hover:text-white"><Bell className="size-4" /><span className="absolute right-1.5 top-1.5 size-1.5 rounded-full bg-[var(--danger)]" /></button><button onClick={handleLogout} className="flex items-center gap-2 rounded-lg border border-[var(--border)] px-3 py-2 text-xs text-[var(--muted-foreground)] hover:text-white"><LogOut className="size-3.5" /> Logout</button></div></header> }

const statusTone = (status: string) => status === "SUCCEEDED" ? "success" : status === "RUNNING" ? "info" : status === "FAILED" ? "danger" : status === "RETRY_WAIT" ? "warning" : "neutral"
function Metric({ label, value, delta, icon: Icon, tone }: { label: string; value: string; delta: string; icon: typeof Activity; tone: "success" | "warning" | "danger" | "info" }) { return <Card className="p-4"><div className="flex items-start justify-between"><div><p className="text-xs text-[var(--muted-foreground)]">{label}</p><p className="mt-2 text-2xl font-semibold tracking-tight text-white">{value}</p><p className={cn("mt-2 text-[11px] font-medium", tone === "success" ? "text-[var(--success)]" : tone === "danger" ? "text-[var(--danger)]" : "text-[var(--warning)]")}>{delta}</p></div><div className={cn("rounded-lg p-2", tone === "success" ? "bg-[var(--success)]/10 text-[var(--success)]" : tone === "danger" ? "bg-[var(--danger)]/10 text-[var(--danger)]" : tone === "info" ? "bg-[var(--primary)]/10 text-[var(--primary)]" : "bg-[var(--warning)]/10 text-[var(--warning)]")}><Icon className="size-4" /></div></div></Card> }

function Overview() { return <div className="flex flex-col gap-5"><div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4"><Metric label="Jobs processed" value="—" delta="Connect API to load" icon={ListChecks} tone="info" /><Metric label="Success rate" value="—" delta="Metrics endpoint required" icon={CheckCircle2} tone="success" /><Metric label="Avg. execution" value="—" delta="Metrics endpoint required" icon={Activity} tone="warning" /><Metric label="Failed jobs" value="—" delta="Metrics endpoint required" icon={XCircle} tone="danger" /></div><div className="grid gap-5 xl:grid-cols-[1.55fr_1fr]"><Card className="min-h-[306px] p-5"><div className="flex items-center justify-between"><div><h2 className="font-semibold text-white">Throughput</h2><p className="mt-1 text-xs text-[var(--muted-foreground)]">Jobs processed over the last 24 hours</p></div><button className="flex items-center gap-1 rounded-md border border-[var(--border)] px-2 py-1 text-[11px] text-[var(--muted-foreground)]">24 hours <ChevronDown className="size-3" /></button></div><div className="flex h-[220px] flex-col items-center justify-center text-center"><BarChart3 className="mb-3 size-8 text-[var(--muted-foreground)]/50" /><p className="text-sm font-medium text-[var(--muted-foreground)]">Metrics are not connected yet</p><p className="mt-1 max-w-[310px] text-xs leading-5 text-[var(--muted-foreground)]">Expose an authenticated metrics endpoint to visualize throughput and latency here.</p></div></Card><Card className="p-5"><div className="flex items-center justify-between"><div><h2 className="font-semibold text-white">System status</h2><p className="mt-1 text-xs text-[var(--muted-foreground)]">Live service checks</p></div><Badge tone="warning">Awaiting API</Badge></div><div className="mt-6 flex flex-col gap-4">{[["API gateway", Server, "No connection"], ["Job workers", Package, "No data"], ["Webhook delivery", Webhook, "No data"]].map(([name, Icon, status]) => <div key={name as string} className="flex items-center gap-3"><div className="rounded-lg bg-[var(--muted)] p-2 text-[var(--muted-foreground)]"><Icon className="size-4" /></div><div className="min-w-0 flex-1"><p className="text-sm text-white">{name as string}</p><p className="text-[11px] text-[var(--muted-foreground)]">{status as string}</p></div><span className="size-2 rounded-full bg-[var(--warning)]" /></div>)}</div></Card></div><div className="grid gap-5 xl:grid-cols-[1.3fr_1fr]"><Card className="p-5"><div className="flex items-center justify-between"><div><h2 className="font-semibold text-white">Recent activity</h2><p className="mt-1 text-xs text-[var(--muted-foreground)]">Latest events across your workspace</p></div><button className="text-xs font-medium text-[var(--primary)]">View all</button></div><div className="flex min-h-[150px] flex-col items-center justify-center text-center"><Activity className="mb-3 size-7 text-[var(--muted-foreground)]/50" /><p className="text-sm text-[var(--muted-foreground)]">No activity to display</p></div></Card><Card className="p-5"><div className="flex items-center justify-between"><div><h2 className="font-semibold text-white">Quick actions</h2><p className="mt-1 text-xs text-[var(--muted-foreground)]">Common operational tasks</p></div><MoreHorizontal className="size-4 text-[var(--muted-foreground)]" /></div><div className="mt-5 grid gap-2 sm:grid-cols-2 xl:grid-cols-1"><button className="flex items-center gap-3 rounded-lg border border-[var(--border)] p-3 text-left hover:bg-[var(--muted)]"><div className="rounded-md bg-[var(--primary)]/10 p-2 text-[var(--primary)]"><Play className="size-4" /></div><div><p className="text-xs font-semibold text-white">Submit a job</p><p className="text-[11px] text-[var(--muted-foreground)]">Create a new background task</p></div></button><button className="flex items-center gap-3 rounded-lg border border-[var(--border)] p-3 text-left hover:bg-[var(--muted)]"><div className="rounded-md bg-[var(--success)]/10 p-2 text-[var(--success)]"><Webhook className="size-4" /></div><div><p className="text-xs font-semibold text-white">Create webhook</p><p className="text-[11px] text-[var(--muted-foreground)]">Listen for job events</p></div></button></div></Card></div></div> }

function Jobs() { 
  const [filter, setFilter] = useState("All statuses");
  const [jobs, setJobs] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  
  useEffect(() => {
    async function loadJobs() {
      try {
        const token = document.cookie.split('nexus_token=')[1]?.split(';')[0];
        if (!token) {
          setError("Not authenticated");
          setLoading(false);
          return;
        }
        const data = await fetchJobs(token);
        setJobs(data);
        setLoading(false);
      } catch (err: any) {
        setError(err.message);
        setLoading(false);
      }
    }
    loadJobs();
  }, []);

  const filteredJobs = useMemo(() => {
    if (filter === "All statuses") return jobs;
    return jobs.filter((job) => job.status === filter);
  }, [jobs, filter]);

  if (loading) return <div className="flex min-h-[320px] items-center justify-center"><p className="text-sm text-[var(--muted-foreground)]">Loading jobs...</p></div>;
  if (error) return <div className="flex min-h-[320px] items-center justify-center"><p className="text-sm text-[var(--danger)]">{error}</p></div>;

  return (
    <div className="flex flex-col gap-5">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div><p className="text-sm text-[var(--muted-foreground)]">Manage and inspect background work.</p></div>
        <button className="flex items-center gap-2 rounded-lg bg-[var(--primary)] px-3 py-2 text-xs font-semibold text-white"><Plus className="size-4" /> Submit job</button>
      </div>
      <Card className="overflow-hidden">
        <div className="flex flex-wrap items-center justify-between gap-3 border-b border-[var(--border)] p-4">
          <div className="flex items-center gap-2">
            <div className="flex items-center gap-2 rounded-lg border border-[var(--border)] px-3 py-2 text-xs text-[var(--muted-foreground)]"><Search className="size-3.5" /> Filter jobs</div>
            <select value={filter} onChange={(event) => setFilter(event.target.value)} className="rounded-lg border border-[var(--border)] bg-transparent px-3 py-2 text-xs text-[var(--muted-foreground)]">
              <option>All statuses</option>
              <option>QUEUED</option>
              <option>RUNNING</option>
              <option>SUCCEEDED</option>
              <option>FAILED</option>
            </select>
          </div>
          <span className="text-xs text-[var(--muted-foreground)]">{filteredJobs.length} jobs</span>
        </div>
        {filteredJobs.length === 0 ? (
          <div className="flex min-h-[320px] flex-col items-center justify-center px-5 text-center">
            <ListChecks className="mb-3 size-8 text-[var(--muted-foreground)]/50" />
            <p className="text-sm font-medium text-[var(--muted-foreground)]">No jobs found</p>
          </div>
        ) : (
          <div className="divide-y divide-[var(--border)]">
            {filteredJobs.map((job) => (
              <div key={job.id} className="flex items-center justify-between p-4 text-sm">
                <div>
                  <div className="flex items-center gap-2">
                    <Badge tone={statusTone(job.status)}>{job.status}</Badge>
                    <span className="font-medium text-white">{job.jobType}</span>
                  </div>
                  <p className="mt-1 text-xs text-[var(--muted-foreground)]">ID: {job.id.slice(0, 8)}...</p>
                </div>
                <p className="text-xs text-[var(--muted-foreground)]">{new Date(job.createdAt).toLocaleString()}</p>
              </div>
            ))}
          </div>
        )}
      </Card>
    </div>
  ); }

function Schedules() { 
  const [schedules, setSchedules] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  
  useEffect(() => {
    async function loadSchedules() {
      try {
        const token = document.cookie.split('nexus_token=')[1]?.split(';')[0];
        if (!token) {
          setError("Not authenticated");
          setLoading(false);
          return;
        }
        const data = await fetchSchedules(token);
        setSchedules(data);
        setLoading(false);
      } catch (err: any) {
        setError(err.message);
        setLoading(false);
      }
    }
    loadSchedules();
  }, []);

  if (loading) return <div className="flex min-h-[320px] items-center justify-center"><p className="text-sm text-[var(--muted-foreground)]">Loading schedules...</p></div>;
  if (error) return <div className="flex min-h-[320px] items-center justify-center"><p className="text-sm text-[var(--danger)]">{error}</p></div>;

  return (
    <div className="flex flex-col gap-5">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div><p className="text-sm text-[var(--muted-foreground)]">Manage recurring job schedules.</p></div>
        <button className="flex items-center gap-2 rounded-lg bg-[var(--primary)] px-3 py-2 text-xs font-semibold text-white"><Plus className="size-4" /> Create schedule</button>
      </div>
      <Card className="overflow-hidden">
        <div className="flex items-center justify-between border-b border-[var(--border)] p-4">
          <span className="text-xs text-[var(--muted-foreground)]">{schedules.length} schedules</span>
        </div>
        {schedules.length === 0 ? (
          <div className="flex min-h-[320px] flex-col items-center justify-center px-5 text-center">
            <Clock3 className="mb-3 size-8 text-[var(--muted-foreground)]/50" />
            <p className="text-sm font-medium text-[var(--muted-foreground)]">No schedules found</p>
          </div>
        ) : (
          <div className="divide-y divide-[var(--border)]">
            {schedules.map((schedule) => (
              <div key={schedule.id} className="flex items-center justify-between p-4 text-sm">
                <div>
                  <div className="flex items-center gap-2">
                    <Badge tone={schedule.enabled ? "success" : "neutral"}>{schedule.enabled ? "Enabled" : "Disabled"}</Badge>
                    <span className="font-medium text-white">{schedule.jobType}</span>
                  </div>
                  <p className="mt-1 text-xs text-[var(--muted-foreground)]">Cron: {schedule.cronExpression}</p>
                </div>
                <p className="text-xs text-[var(--muted-foreground)]">{new Date(schedule.createdAt).toLocaleString()}</p>
              </div>
            ))}
          </div>
        )}
      </Card>
    </div>
  ); }

function Webhooks() { 
  const [webhooks, setWebhooks] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  
  useEffect(() => {
    async function loadWebhooks() {
      try {
        const token = document.cookie.split('nexus_token=')[1]?.split(';')[0];
        if (!token) {
          setError("Not authenticated");
          setLoading(false);
          return;
        }
        const data = await fetchWebhooks(token);
        setWebhooks(data);
        setLoading(false);
      } catch (err: any) {
        setError(err.message);
        setLoading(false);
      }
    }
    loadWebhooks();
  }, []);

  if (loading) return <div className="flex min-h-[320px] items-center justify-center"><p className="text-sm text-[var(--muted-foreground)]">Loading webhooks...</p></div>;
  if (error) return <div className="flex min-h-[320px] items-center justify-center"><p className="text-sm text-[var(--danger)]">{error}</p></div>;

  return (
    <div className="flex flex-col gap-5">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div><p className="text-sm text-[var(--muted-foreground)]">Manage webhook endpoints for event notifications.</p></div>
        <button className="flex items-center gap-2 rounded-lg bg-[var(--primary)] px-3 py-2 text-xs font-semibold text-white"><Plus className="size-4" /> Create webhook</button>
      </div>
      <Card className="overflow-hidden">
        <div className="flex items-center justify-between border-b border-[var(--border)] p-4">
          <span className="text-xs text-[var(--muted-foreground)]">{webhooks.length} webhooks</span>
        </div>
        {webhooks.length === 0 ? (
          <div className="flex min-h-[320px] flex-col items-center justify-center px-5 text-center">
            <Webhook className="mb-3 size-8 text-[var(--muted-foreground)]/50" />
            <p className="text-sm font-medium text-[var(--muted-foreground)]">No webhooks found</p>
          </div>
        ) : (
          <div className="divide-y divide-[var(--border)]">
            {webhooks.map((webhook) => (
              <div key={webhook.id} className="flex items-center justify-between p-4 text-sm">
                <div>
                  <div className="flex items-center gap-2">
                    <Badge tone={webhook.enabled ? "success" : "neutral"}>{webhook.enabled ? "Enabled" : "Disabled"}</Badge>
                    <span className="font-medium text-white truncate max-w-[300px]">{webhook.url}</span>
                  </div>
                  <p className="mt-1 text-xs text-[var(--muted-foreground)]">Events: {webhook.eventTypes.join(", ")}</p>
                </div>
                <p className="text-xs text-[var(--muted-foreground)]">{new Date(webhook.createdAt).toLocaleString()}</p>
              </div>
            ))}
          </div>
        )}
      </Card>
    </div>
  ); }

export default function Dashboard() { const [active, setActive] = useState("Overview"); const title = active === "Overview" ? "Overview" : active; return <main className="flex min-h-screen bg-[var(--background)]"><Sidebar active={active} onChange={setActive} /><div className="min-w-0 flex-1"><Topbar title={title} /><div className="p-5 md:p-8">{active === "Overview" ? <Overview /> : active === "Jobs" ? <Jobs /> : active === "Schedules" ? <Schedules /> : active === "Webhooks" ? <Webhooks /> : <Card className="flex min-h-[420px] flex-col items-center justify-center p-8 text-center"><Code2 className="mb-4 size-9 text-[var(--primary)]" /><h2 className="text-lg font-semibold text-white">{active} is ready for API wiring</h2><p className="mt-2 max-w-md text-sm leading-6 text-[var(--muted-foreground)]">The navigation and operations shell are in place. This surface will use the corresponding Spring Boot controller once the authenticated dashboard proxy is configured.</p></Card>}</div></div></main> }
