import type { Metadata } from "next"
import "./globals.css"
import { Toaster } from "react-hot-toast"

export const metadata: Metadata = { 
  title: "NEXUS CORE | Operations", 
  description: "Operational dashboard for NEXUS CORE job processing.",
  icons: {
    icon: "/logo.png",
  }
}

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) { 
  return (
    <html lang="en" className="dark">
      <body>
        {children}
        <Toaster position="top-right" />
      </body>
    </html>
  )
}
