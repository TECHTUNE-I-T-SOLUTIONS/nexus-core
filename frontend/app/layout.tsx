import type { Metadata } from "next"
import "./globals.css"

export const metadata: Metadata = { title: "NEXUS CORE | Operations", description: "Operational dashboard for NEXUS CORE job processing." }

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) { return <html lang="en" className="dark"><body>{children}</body></html> }
