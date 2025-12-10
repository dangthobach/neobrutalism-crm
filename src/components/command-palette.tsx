"use client"

import * as React from "react"
import { useRouter } from "next/navigation"
import {
  CommandDialog,
  CommandInput,
  CommandList,
  CommandEmpty,
  CommandGroup,
  CommandItem,
  CommandShortcut,
  CommandSeparator,
} from "@/components/ui/command"
import {
  useSearchCommands,
  useRecentCommands,
  useFavoriteCommands,
  useRecordExecution,
  useAddToFavorites,
  useRemoveFromFavorites,
} from "@/hooks/useCommands"
import { Command, CommandCategory } from "@/lib/api/commands"
import {
  Star,
  Clock,
  Search as SearchIcon,
  UserPlus,
  Users,
  Plus,
  CheckSquare,
  UserCheck,
  Home,
  Settings,
  type LucideIcon,
} from "lucide-react"
import { toast } from "sonner"

// Icon mapping for command icons
const iconMap: Record<string, LucideIcon> = {
  UserPlus,
  Users,
  Plus,
  CheckSquare,
  UserCheck,
  Home,
  Settings,
  Search: SearchIcon,
}

interface CommandPaletteProps {
  open?: boolean
  onOpenChange?: (open: boolean) => void
}

export function CommandPalette({ open, onOpenChange }: CommandPaletteProps) {
  const router = useRouter()
  const [searchQuery, setSearchQuery] = React.useState("")
  const [isOpen, setIsOpen] = React.useState(open ?? false)

  const { data: searchResults, isLoading: isSearching } = useSearchCommands({
    query: searchQuery || undefined,
    size: 50,
  })

  const { data: recentCommands } = useRecentCommands()
  const { data: favoriteCommands } = useFavoriteCommands()
  const recordExecution = useRecordExecution()
  const addToFavorites = useAddToFavorites()
  const removeFromFavorites = useRemoveFromFavorites()

  // Keyboard shortcut: Ctrl+K or Cmd+K
  React.useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if ((e.metaKey || e.ctrlKey) && e.key === "k") {
        e.preventDefault()
        setIsOpen(true)
      }
    }

    window.addEventListener("keydown", handleKeyDown)
    return () => window.removeEventListener("keydown", handleKeyDown)
  }, [])

  React.useEffect(() => {
    if (open !== undefined) {
      setIsOpen(open)
    }
  }, [open])

  const handleOpenChange = (newOpen: boolean) => {
    setIsOpen(newOpen)
    if (!newOpen) {
      setSearchQuery("")
    }
    onOpenChange?.(newOpen)
  }

  const executeCommand = React.useCallback(
    async (command: Command) => {
      const startTime = Date.now()

      try {
        // Parse action payload
        let actionPayload: any = {}
        if (command.actionPayload) {
          try {
            actionPayload = JSON.parse(command.actionPayload)
          } catch (e) {
            console.warn("Failed to parse action payload:", e)
          }
        }

        // Execute based on action type
        switch (command.actionType) {
          case "NAVIGATION":
            if (actionPayload.route) {
              router.push(actionPayload.route)
              handleOpenChange(false)
            }
            break

          case "MODAL":
            if (actionPayload.modal) {
              // Emit custom event for modal opening
              window.dispatchEvent(
                new CustomEvent("open-modal", { detail: { modal: actionPayload.modal } })
              )
              handleOpenChange(false)
            }
            break

          case "API_CALL":
            if (actionPayload.endpoint) {
              // Handle API call
              toast.info(`Executing: ${command.label}`)
              handleOpenChange(false)
            }
            break

          case "EXTERNAL":
            if (actionPayload.url) {
              window.open(actionPayload.url, "_blank")
              handleOpenChange(false)
            }
            break

          default:
            toast.warning(`Unknown action type: ${command.actionType}`)
        }

        // Record execution
        const executionTime = Date.now() - startTime
        recordExecution.mutate({
          commandId: command.id,
          executionTimeMs: executionTime,
          contextData: JSON.stringify({
            page: window.location.pathname,
            query: searchQuery,
          }),
        })
      } catch (error) {
        console.error("Failed to execute command:", error)
        toast.error(`Failed to execute: ${command.label}`)
      }
    },
    [router, recordExecution, searchQuery]
  )

  const toggleFavorite = React.useCallback(
    (command: Command, isFavorite: boolean) => {
      if (isFavorite) {
        removeFromFavorites.mutate(command.id)
      } else {
        addToFavorites.mutate(command.id)
      }
    },
    [addToFavorites, removeFromFavorites]
  )

  const isFavorite = React.useCallback(
    (commandId: string) => {
      return favoriteCommands?.some((c) => c.id === commandId) ?? false
    },
    [favoriteCommands]
  )

  // Group commands by category
  const groupedCommands = React.useMemo(() => {
    if (!searchResults?.commands) return {}

    const groups: Record<string, Command[]> = {}
    searchResults.commands.forEach((cmd) => {
      const category = cmd.category || "OTHER"
      if (!groups[category]) {
        groups[category] = []
      }
      groups[category].push(cmd)
    })
    return groups
  }, [searchResults])

  const showRecent = !searchQuery && recentCommands && recentCommands.length > 0
  const showFavorites = !searchQuery && favoriteCommands && favoriteCommands.length > 0
  const showSearchResults = searchQuery && searchResults?.commands && searchResults.commands.length > 0

  return (
    <CommandDialog open={isOpen} onOpenChange={handleOpenChange}>
      <CommandInput
        placeholder="Type a command or search..."
        value={searchQuery}
        onValueChange={setSearchQuery}
      />
      <CommandList>
        <CommandEmpty>
          {isSearching ? "Searching..." : "No commands found."}
        </CommandEmpty>

        {/* Favorites */}
        {showFavorites && (
          <>
            <CommandGroup heading="Favorites">
              {favoriteCommands.map((cmd) => (
                <CommandItem
                  key={cmd.id}
                  value={cmd.commandId}
                  onSelect={() => executeCommand(cmd)}
                >
                  {cmd.icon && (() => {
                    const Icon = iconMap[cmd.icon] || SearchIcon
                    return <Icon className="mr-2 h-4 w-4" />
                  })()}
                  <span>{cmd.label}</span>
                  {cmd.shortcutKey && (
                    <CommandShortcut>{cmd.shortcutKey}</CommandShortcut>
                  )}
                  <button
                    className="ml-auto"
                    onClick={(e) => {
                      e.stopPropagation()
                      toggleFavorite(cmd, true)
                    }}
                  >
                    <Star className="h-4 w-4 fill-yellow-400 text-yellow-400" />
                  </button>
                </CommandItem>
              ))}
            </CommandGroup>
            <CommandSeparator />
          </>
        )}

        {/* Recent */}
        {showRecent && (
          <>
            <CommandGroup heading="Recent">
              {recentCommands.map((cmd) => (
                <CommandItem
                  key={cmd.id}
                  value={cmd.commandId}
                  onSelect={() => executeCommand(cmd)}
                >
                  <Clock className="mr-2 h-4 w-4" />
                  <span>{cmd.label}</span>
                  {cmd.shortcutKey && (
                    <CommandShortcut>{cmd.shortcutKey}</CommandShortcut>
                  )}
                </CommandItem>
              ))}
            </CommandGroup>
            <CommandSeparator />
          </>
        )}

        {/* Search Results */}
        {showSearchResults &&
          Object.entries(groupedCommands).map(([category, commands]) => (
            <CommandGroup key={category} heading={category}>
              {commands.map((cmd) => (
                <CommandItem
                  key={cmd.id}
                  value={cmd.commandId}
                  onSelect={() => executeCommand(cmd)}
                >
                  {cmd.icon && (() => {
                    const Icon = iconMap[cmd.icon] || SearchIcon
                    return <Icon className="mr-2 h-4 w-4" />
                  })()}
                  <span>{cmd.label}</span>
                  {cmd.description && (
                    <span className="ml-2 text-xs text-muted-foreground">
                      {cmd.description}
                    </span>
                  )}
                  <div className="ml-auto flex items-center gap-2">
                    {cmd.shortcutKey && (
                      <CommandShortcut>{cmd.shortcutKey}</CommandShortcut>
                    )}
                    <button
                      onClick={(e) => {
                        e.stopPropagation()
                        toggleFavorite(cmd, isFavorite(cmd.id))
                      }}
                    >
                      <Star
                        className={`h-4 w-4 ${
                          isFavorite(cmd.id)
                            ? "fill-yellow-400 text-yellow-400"
                            : "text-muted-foreground"
                        }`}
                      />
                    </button>
                  </div>
                </CommandItem>
              ))}
            </CommandGroup>
          ))}
      </CommandList>
    </CommandDialog>
  )
}
