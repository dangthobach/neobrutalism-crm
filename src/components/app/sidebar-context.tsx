"use client"

import { createContext, useCallback, useContext, useEffect, useState } from "react"

type SidebarContextType = {
  collapsed: boolean
  ready: boolean
  toggle: () => void
  setCollapsed: (value: boolean) => void
}

const SidebarContext = createContext<SidebarContextType | undefined>(undefined)
const STORAGE_KEY = "sidebar-collapsed"

export function SidebarProvider({ children }: { children: React.ReactNode }) {
  const [collapsed, setCollapsed] = useState(false)
  const [ready, setReady] = useState(false)

  useEffect(() => {
    if (typeof window === "undefined") return
    const stored = window.localStorage.getItem(STORAGE_KEY)
    if (stored !== null) {
      setCollapsed(stored === "true")
    }
    setReady(true)
  }, [])

  const persist = useCallback((value: boolean) => {
    if (typeof window === "undefined") return
    window.localStorage.setItem(STORAGE_KEY, String(value))
  }, [])

  const toggle = useCallback(() => {
    setCollapsed((prev) => {
      const next = !prev
      persist(next)
      return next
    })
  }, [persist])

  const handleSetCollapsed = useCallback(
    (value: boolean) => {
      setCollapsed(value)
      persist(value)
    },
    [persist],
  )

  return (
    <SidebarContext.Provider value={{ collapsed, ready, toggle, setCollapsed: handleSetCollapsed }}>
      {children}
    </SidebarContext.Provider>
  )
}

export function useSidebar() {
  const ctx = useContext(SidebarContext)
  if (!ctx) {
    throw new Error("useSidebar must be used within SidebarProvider")
  }
  return ctx
}

