"use client"

import { PanelLeftOpen, PanelLeftClose } from "lucide-react"

import { useSidebar } from "@/components/app/sidebar-context"
import { cn } from "@/lib/utils"

type SidebarToggleProps = {
  className?: string
}

export function SidebarToggle({ className }: SidebarToggleProps) {
  const { collapsed, toggle } = useSidebar()

  return (
    <button
      type="button"
      onClick={toggle}
      title={collapsed ? "Expand sidebar" : "Collapse sidebar"}
      className={cn(
        "group relative inline-flex h-10 w-10 items-center justify-center border-2 border-black bg-white font-heading text-sm uppercase tracking-wide shadow-[4px_4px_0_#000] transition-all hover:translate-x-[2px] hover:translate-y-[2px] hover:shadow-none focus:outline-none focus:ring-2 focus:ring-black",
        className,
      )}
    >
      {collapsed ? <PanelLeftOpen className="h-5 w-5" /> : <PanelLeftClose className="h-5 w-5" />}
      <span className="pointer-events-none absolute -bottom-8 left-1/2 -translate-x-1/2 whitespace-nowrap rounded-sm border-2 border-black bg-white px-2 py-1 text-[10px] font-semibold shadow-[2px_2px_0_#000] opacity-0 transition-opacity group-hover:opacity-100">
        {collapsed ? "Expand" : "Collapse"}
      </span>
    </button>
  )
}

