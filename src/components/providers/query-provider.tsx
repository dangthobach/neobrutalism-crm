"use client"

import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { ReactQueryDevtools } from '@tanstack/react-query-devtools'
import { ReactNode, useState } from 'react'

/**
 * React Query Provider Component
 * Wraps the application with QueryClient for data fetching and caching
 */
export function QueryProvider({ children }: { children: ReactNode }) {
  const [queryClient] = useState(() => new QueryClient({
    defaultOptions: {
      queries: {
        // Stale time: How long data is considered fresh (1 minute)
        staleTime: 60 * 1000,

        // Cache time: How long unused data stays in cache (5 minutes)
        gcTime: 5 * 60 * 1000,

        // Retry failed requests once
        retry: 1,

        // Retry delay: 1 second
        retryDelay: 1000,

        // Refetch on window focus for data consistency
        refetchOnWindowFocus: false,

        // Refetch on reconnect
        refetchOnReconnect: true,

        // Refetch on mount if data is stale
        refetchOnMount: true,
      },
      mutations: {
        // Retry failed mutations once
        retry: 1,

        // Retry delay: 1 second
        retryDelay: 1000,
      },
    },
  }))

  return (
    <QueryClientProvider client={queryClient}>
      {children}
      {/* React Query Devtools - only in development */}
      {process.env.NODE_ENV === 'development' && (
        <ReactQueryDevtools
          initialIsOpen={false}
          position="bottom"
          buttonPosition="bottom-right"
        />
      )}
    </QueryClientProvider>
  )
}
