"use client"

import { useState } from "react"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { CheckCircle2, XCircle, AlertCircle, RefreshCw, Loader2 } from "lucide-react"
import { getUIMenuStructure, countUIMenus } from "@/lib/menu-sync-util"
import { menuApi } from "@/lib/api/menus"

interface MenuAuditResult {
  totalUIMenus: number
  totalDBMenus: number
  missingInDB: string[]
  extraInDB: string[]
  matched: string[]
  loading: boolean
}

export function MenuAuditReport() {
  const [auditResult, setAuditResult] = useState<MenuAuditResult | null>(null)
  const [loading, setLoading] = useState(false)

  async function runAudit() {
    setLoading(true)
    try {
      // Get UI menus
      const uiMenus = getUIMenuStructure()
      const uiMenuCodes = new Set<string>()

      function collectCodes(menus: any[]) {
        menus.forEach(menu => {
          uiMenuCodes.add(menu.code)
          if (menu.children) {
            collectCodes(menu.children)
          }
        })
      }
      collectCodes(uiMenus)

      // Get DB menus
      const dbMenusResponse = await menuApi.getMenus({ page: 0, size: 1000 })
      const dbMenus = dbMenusResponse.content || []
      const dbMenuCodes = new Set(dbMenus.map(m => m.code))

      // Compare
      const missingInDB: string[] = []
      const matched: string[] = []

      uiMenuCodes.forEach(code => {
        if (dbMenuCodes.has(code)) {
          matched.push(code)
        } else {
          missingInDB.push(code)
        }
      })

      const extraInDB: string[] = []
      dbMenuCodes.forEach(code => {
        if (!uiMenuCodes.has(code)) {
          extraInDB.push(code)
        }
      })

      setAuditResult({
        totalUIMenus: uiMenuCodes.size,
        totalDBMenus: dbMenus.length,
        missingInDB,
        extraInDB,
        matched,
        loading: false,
      })
    } catch (error: any) {
      console.error('Audit failed:', error)
      alert('Failed to run audit: ' + error.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <Card className="border-4 border-black shadow-[8px_8px_0_#000]">
      <CardHeader>
        <CardTitle className="font-heading text-xl flex items-center gap-2">
          <AlertCircle className="h-5 w-5" />
          Menu Synchronization Status
        </CardTitle>
        <CardDescription>
          Check which menus are in your UI code vs. database
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-4">
        {!auditResult ? (
          <div className="text-center py-8">
            <p className="text-sm text-muted-foreground mb-4">
              Run an audit to check menu synchronization status
            </p>
            <Button
              onClick={runAudit}
              disabled={loading}
              className="bg-blue-500 text-white border-2 border-black"
            >
              {loading ? (
                <>
                  <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                  Scanning...
                </>
              ) : (
                <>
                  <RefreshCw className="h-4 w-4 mr-2" />
                  Run Audit
                </>
              )}
            </Button>
          </div>
        ) : (
          <>
            {/* Summary */}
            <div className="grid grid-cols-3 gap-3">
              <div className="border-2 border-black p-4 bg-secondary-background">
                <div className="text-2xl font-heading">{auditResult.totalUIMenus}</div>
                <div className="text-xs text-muted-foreground">UI Menus</div>
              </div>
              <div className="border-2 border-black p-4 bg-secondary-background">
                <div className="text-2xl font-heading">{auditResult.totalDBMenus}</div>
                <div className="text-xs text-muted-foreground">DB Menus</div>
              </div>
              <div className="border-2 border-black p-4 bg-secondary-background">
                <div className="text-2xl font-heading">{auditResult.matched.length}</div>
                <div className="text-xs text-muted-foreground">Matched</div>
              </div>
            </div>

            {/* Missing in DB */}
            {auditResult.missingInDB.length > 0 && (
              <div className="border-2 border-red-500 bg-red-50 dark:bg-red-950/20 p-4">
                <div className="flex items-center gap-2 mb-2">
                  <XCircle className="h-5 w-5 text-red-600" />
                  <h3 className="font-heading text-sm">
                    Missing in Database ({auditResult.missingInDB.length})
                  </h3>
                </div>
                <p className="text-xs text-muted-foreground mb-2">
                  These menus exist in your UI code but not in the database. Click "Sync from UI" to add them.
                </p>
                <div className="flex flex-wrap gap-2">
                  {auditResult.missingInDB.map(code => (
                    <Badge key={code} variant="destructive" className="font-mono text-xs">
                      {code}
                    </Badge>
                  ))}
                </div>
              </div>
            )}

            {/* Extra in DB */}
            {auditResult.extraInDB.length > 0 && (
              <div className="border-2 border-yellow-500 bg-yellow-50 dark:bg-yellow-950/20 p-4">
                <div className="flex items-center gap-2 mb-2">
                  <AlertCircle className="h-5 w-5 text-yellow-600" />
                  <h3 className="font-heading text-sm">
                    Extra in Database ({auditResult.extraInDB.length})
                  </h3>
                </div>
                <p className="text-xs text-muted-foreground mb-2">
                  These menus exist in the database but not in your UI code. They may be custom menus or old menus.
                </p>
                <div className="flex flex-wrap gap-2">
                  {auditResult.extraInDB.map(code => (
                    <Badge key={code} className="bg-yellow-500 text-white font-mono text-xs">
                      {code}
                    </Badge>
                  ))}
                </div>
              </div>
            )}

            {/* Matched */}
            {auditResult.matched.length > 0 && (
              <div className="border-2 border-green-500 bg-green-50 dark:bg-green-950/20 p-4">
                <div className="flex items-center gap-2 mb-2">
                  <CheckCircle2 className="h-5 w-5 text-green-600" />
                  <h3 className="font-heading text-sm">
                    Synced ({auditResult.matched.length})
                  </h3>
                </div>
                <p className="text-xs text-muted-foreground mb-2">
                  These menus are synchronized between UI and database.
                </p>
                <div className="flex flex-wrap gap-2 max-h-32 overflow-auto">
                  {auditResult.matched.map(code => (
                    <Badge key={code} variant="default" className="font-mono text-xs">
                      {code}
                    </Badge>
                  ))}
                </div>
              </div>
            )}

            {/* Perfect sync */}
            {auditResult.missingInDB.length === 0 && auditResult.extraInDB.length === 0 && (
              <div className="border-2 border-green-500 bg-green-50 dark:bg-green-950/20 p-4 text-center">
                <CheckCircle2 className="h-12 w-12 text-green-600 mx-auto mb-2" />
                <h3 className="font-heading text-lg mb-1">Perfect Sync!</h3>
                <p className="text-sm text-muted-foreground">
                  All UI menus are synchronized with the database.
                </p>
              </div>
            )}

            <Button
              onClick={runAudit}
              variant="noShadow"
              size="sm"
              className="w-full"
            >
              <RefreshCw className="h-4 w-4 mr-2" />
              Refresh Audit
            </Button>
          </>
        )}
      </CardContent>
    </Card>
  )
}
