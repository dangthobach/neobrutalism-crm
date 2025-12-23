/**
 * WebSocket Provider
 * Provides real-time communication using STOMP over SockJS
 */

'use client'

import { createContext, useContext, useEffect, useState, useRef } from 'react'
import { Client, IMessage } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { toast } from 'sonner'

interface WebSocketContextValue {
  client: Client | null
  isConnected: boolean
  subscribe: (topic: string, handler: (message: any) => void) => (() => void) | undefined
  unsubscribe: (topic: string) => void
  send: (destination: string, body: any) => void
}

const WebSocketContext = createContext<WebSocketContextValue>({
  client: null,
  isConnected: false,
  subscribe: () => undefined,
  unsubscribe: () => {},
  send: () => {},
})

export function WebSocketProvider({ children }: { children: React.ReactNode }) {
  const [client, setClient] = useState<Client | null>(null)
  const [isConnected, setIsConnected] = useState(false)
  const subscriptionsRef = useRef<Map<string, any>>(new Map())

  useEffect(() => {
    // Only run on client side
    if (typeof window === 'undefined') return

    const stompClient = new Client({
      // Use SockJS for better browser compatibility
      webSocketFactory: () => new SockJS('http://localhost:8080/ws'),
      
      // Reconnection settings
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,

      // Connection callbacks
      onConnect: () => {
        console.log('[WebSocket] Connected to server')
        setIsConnected(true)
        toast.success('Kết nối real-time thành công', {
          duration: 2000,
        })
      },

      onDisconnect: () => {
        console.log('[WebSocket] Disconnected from server')
        setIsConnected(false)
        toast.info('Mất kết nối real-time', {
          duration: 2000,
        })
      },

      onStompError: (frame) => {
        console.error('[WebSocket] STOMP Error:', frame.headers['message'])
        console.error('[WebSocket] Error details:', frame.body)
        toast.error('Lỗi kết nối real-time', {
          description: frame.headers['message'],
        })
      },

      onWebSocketError: (event) => {
        console.error('[WebSocket] WebSocket Error:', event)
      },

      // Debug logging (disable in production)
      debug: (str) => {
        if (process.env.NODE_ENV === 'development') {
          console.log('[WebSocket]', str)
        }
      },
    })

    // Activate the client
    stompClient.activate()
    setClient(stompClient)

    // Cleanup on unmount
    return () => {
      console.log('[WebSocket] Cleaning up connection')
      
      // Unsubscribe from all topics
      subscriptionsRef.current.forEach((subscription) => {
        subscription.unsubscribe()
      })
      subscriptionsRef.current.clear()

      // Deactivate client
      stompClient.deactivate()
    }
  }, [])

  const subscribe = (topic: string, handler: (message: any) => void) => {
    if (!client || !isConnected) {
      console.warn('[WebSocket] Cannot subscribe: client not connected')
      return undefined
    }

    // Check if already subscribed
    if (subscriptionsRef.current.has(topic)) {
      console.warn(`[WebSocket] Already subscribed to ${topic}`)
      return () => {}
    }

    console.log(`[WebSocket] Subscribing to ${topic}`)

    const subscription = client.subscribe(topic, (message: IMessage) => {
      try {
        const body = JSON.parse(message.body)
        console.log(`[WebSocket] Message received from ${topic}:`, body)
        handler(body)
      } catch (error) {
        console.error(`[WebSocket] Error parsing message from ${topic}:`, error)
      }
    })

    subscriptionsRef.current.set(topic, subscription)

    // Return unsubscribe function
    return () => {
      console.log(`[WebSocket] Unsubscribing from ${topic}`)
      subscription.unsubscribe()
      subscriptionsRef.current.delete(topic)
    }
  }

  const unsubscribe = (topic: string) => {
    const subscription = subscriptionsRef.current.get(topic)
    if (subscription) {
      console.log(`[WebSocket] Unsubscribing from ${topic}`)
      subscription.unsubscribe()
      subscriptionsRef.current.delete(topic)
    }
  }

  const send = (destination: string, body: any) => {
    if (!client || !isConnected) {
      console.warn('[WebSocket] Cannot send: client not connected')
      return
    }

    console.log(`[WebSocket] Sending to ${destination}:`, body)
    client.publish({
      destination,
      body: JSON.stringify(body),
    })
  }

  return (
    <WebSocketContext.Provider value={{ client, isConnected, subscribe, unsubscribe, send }}>
      {children}
    </WebSocketContext.Provider>
  )
}

export function useWebSocketContext() {
  const context = useContext(WebSocketContext)
  if (!context) {
    throw new Error('useWebSocketContext must be used within WebSocketProvider')
  }
  return context
}
