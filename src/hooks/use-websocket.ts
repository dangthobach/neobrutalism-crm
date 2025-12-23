/**
 * WebSocket Hook
 * Provides WebSocket subscription and messaging functionality
 */

'use client'

import { useWebSocketContext } from '@/providers/websocket-provider'

interface WebSocketHook {
  subscribe: (topic: string, handler: (message: any) => void) => (() => void) | undefined
  unsubscribe: (topic: string) => void
  send: (destination: string, body: any) => void
  isConnected: boolean
}

/**
 * Hook for WebSocket operations
 * Uses the WebSocketProvider context to manage STOMP connections
 * 
 * @example
 * const { subscribe, unsubscribe, isConnected } = useWebSocket()
 * 
 * useEffect(() => {
 *   if (!isConnected) return
 *   
 *   const unsubscribeFn = subscribe('/topic/tasks/123/comments', (message) => {
 *     console.log('Received:', message)
 *   })
 *   
 *   return () => {
 *     if (unsubscribeFn) unsubscribeFn()
 *   }
 * }, [isConnected])
 */
export function useWebSocket(): WebSocketHook {
  const { subscribe, unsubscribe, send, isConnected } = useWebSocketContext()

  return {
    subscribe,
    unsubscribe,
    send,
    isConnected,
  }
}

/**
 * Example WebSocket Provider setup (to be implemented):
 * 
 * import { Client } from '@stomp/stompjs'
 * import SockJS from 'sockjs-client'
 * 
 * const WebSocketProvider: React.FC = ({ children }) => {
 *   const [stompClient, setStompClient] = useState<Client | null>(null)
 * 
 *   useEffect(() => {
 *     const client = new Client({
 *       webSocketFactory: () => new SockJS('/ws'),
 *       onConnect: () => console.log('Connected'),
 *       onDisconnect: () => console.log('Disconnected'),
 *     })
 *     client.activate()
 *     setStompClient(client)
 *     return () => client.deactivate()
 *   }, [])
 * 
 *   return (
 *     <WebSocketContext.Provider value={{ stompClient }}>
 *       {children}
 *     </WebSocketContext.Provider>
 *   )
 * }
 */
