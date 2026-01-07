"use client"

import Link from "next/link"
import { usePathname } from "next/navigation"

import { MAIN_SIDEBAR } from "@/data/sidebar-links"

import { cn } from "@/lib/utils"
import { useSidebar } from "@/components/app/sidebar-context"

export default function Sidebar() {
  const pathname = usePathname()
  const { collapsed } = useSidebar()

  return (
    <aside
      className={cn(
        "scrollbar fixed top-[70px] bg-secondary-background h-[calc(100svh-70px)] max-h-[calc(100svh-70px)] overflow-y-auto border-r-4 lg:block hidden border-border transition-all duration-200",
        collapsed ? "w-[70px]" : "w-[250px]",
      )}
    >
      {MAIN_SIDEBAR.map((item, id) => {
        return typeof item === "string" ? (
          <div
            key={id}
            className={cn(
              "block border-b-4 border-r-4 border-border p-4 text-xl font-heading uppercase tracking-wide",
              collapsed && "text-center text-xs leading-tight py-3",
            )}
          >
            {collapsed ? item.slice(0, 3) : item}
          </div>
        ) : (
          <Link
            key={id}
            href={`${item.href}`}
            className={cn(
              "flex items-center border-b-4 border-r-4 border-border p-4 text-lg font-base text-foreground/90 hover:bg-main/70 hover:text-main-foreground transition-all",
              collapsed ? "justify-center" : "pl-7 gap-3",
              item.href === pathname && "bg-main text-main-foreground hover:bg-main",
            )}
            title={item.text}
          >
            <span
              className={cn(
                "flex h-8 w-8 items-center justify-center rounded-sm border-2 border-black bg-white font-heading text-sm uppercase shadow-[2px_2px_0_#000]",
                collapsed ? "" : "hidden",
              )}
            >
              {item.text.slice(0, 2)}
            </span>
            {!collapsed && <span className="truncate">{item.text}</span>}
          </Link>
        )
      })}
    </aside>
  )
}
