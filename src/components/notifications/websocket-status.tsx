'use client';

import { useWebSocketOptional } from '@/components/providers/websocket-provider';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Wifi, WifiOff, RefreshCw } from 'lucide-react';
import { cn } from '@/lib/utils';

interface WebSocketStatusProps {
  className?: string;
  showReconnect?: boolean;
}

/**
 * WebSocket Connection Status Indicator
 * Shows real-time connection status with optional reconnect button
 * 
 * Usage:
 * ```tsx
 * <WebSocketStatus showReconnect />
 * ```
 */
export function WebSocketStatus({ className, showReconnect = true }: WebSocketStatusProps) {
  const websocket = useWebSocketOptional();

  // Don't render if WebSocket is not initialized
  if (!websocket) {
    return null;
  }

  const { isConnected, isReconnecting, reconnectAttempt, reconnect } = websocket;

  if (isConnected) {
    return (
      <Badge 
        variant="default" 
        className={cn("gap-1.5 bg-green-50 text-green-700 border-green-200", className)}
      >
        <Wifi className="w-3 h-3" />
        <span className="text-xs">Real-time</span>
      </Badge>
    );
  }

  if (isReconnecting) {
    return (
      <div className={cn("flex items-center gap-2", className)}>
        <Badge 
          variant="default" 
          className="gap-1.5 bg-yellow-50 text-yellow-700 border-yellow-200"
        >
          <RefreshCw className="w-3 h-3 animate-spin" />
          <span className="text-xs">Reconnecting... ({reconnectAttempt})</span>
        </Badge>
      </div>
    );
  }

  return (
    <div className={cn("flex items-center gap-2", className)}>
      <Badge 
        variant="default" 
        className="gap-1.5 bg-red-50 text-red-700 border-red-200"
      >
        <WifiOff className="w-3 h-3" />
        <span className="text-xs">Disconnected</span>
      </Badge>
      
      {showReconnect && (
        <Button
          variant="reverse"
          size="sm"
          onClick={reconnect}
          className="h-6 px-2 text-xs"
        >
          <RefreshCw className="w-3 h-3 mr-1" />
          Reconnect
        </Button>
      )}
    </div>
  );
}

/**
 * Compact version - just shows a status dot
 */
export function WebSocketStatusDot({ className }: { className?: string }) {
  const websocket = useWebSocketOptional();

  if (!websocket) {
    return null;
  }

  const { isConnected, isReconnecting } = websocket;

  return (
    <div 
      className={cn(
        "w-2 h-2 rounded-full",
        isConnected && "bg-green-500 animate-pulse",
        isReconnecting && "bg-yellow-500 animate-pulse",
        !isConnected && !isReconnecting && "bg-red-500",
        className
      )}
      title={
        isConnected ? "Connected" :
        isReconnecting ? "Reconnecting..." :
        "Disconnected"
      }
    />
  );
}
