'use client';

import { useState } from 'react';
import { useAuditLogs, useSearchAuditLogs, useCriticalEvents, useFailedAttempts } from '@/hooks/usePermissionAudit';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { DataTablePagination } from '@/components/ui/data-table-pagination';
import { Search, Shield, AlertTriangle, XCircle, Calendar } from 'lucide-react';
import { format } from 'date-fns';
import type { PermissionAuditLog } from '@/lib/api/permission-audit';

/**
 * Permission Audit Log Viewer Page
 *
 * Features:
 * - View all audit logs with pagination
 * - Search audit logs by username/role/resource
 * - Filter by critical events
 * - Filter by failed attempts
 * - Detailed log view with metadata
 */
export default function PermissionAuditLogsPage() {
  const [currentPage, setCurrentPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);
  const [searchTerm, setSearchTerm] = useState('');
  const [activeTab, setActiveTab] = useState('all');

  // Fetch data based on active tab
  const allLogsQuery = useAuditLogs(currentPage, pageSize);
  const searchQuery = useSearchAuditLogs(searchTerm, currentPage, pageSize);
  const criticalQuery = useCriticalEvents(currentPage, pageSize);
  const failedQuery = useFailedAttempts(currentPage, pageSize);

  // Determine which query to use based on active tab and search
  const getActiveQuery = () => {
    if (searchTerm.length >= 2) {
      return searchQuery;
    }
    switch (activeTab) {
      case 'critical':
        return criticalQuery;
      case 'failed':
        return failedQuery;
      default:
        return allLogsQuery;
    }
  };

  const activeQuery = getActiveQuery();
  const logs = activeQuery.data?.content || [];
  const totalPages = activeQuery.data?.totalPages || 0;
  const totalElements = activeQuery.data?.totalElements || 0;

  const getActionTypeBadge = (actionType: string) => {
    const critical = ['UNAUTHORIZED_ACCESS_ATTEMPT', 'PERMISSION_ESCALATION_ATTEMPT', 'DATA_SCOPE_CHANGED'];
    if (critical.includes(actionType)) {
      return <Badge variant="destructive">{actionType}</Badge>;
    }
    if (actionType.includes('REMOVED') || actionType.includes('DELETED')) {
      return <Badge variant="outline" className="border-orange-500 text-orange-700">{actionType}</Badge>;
    }
    if (actionType.includes('ASSIGNED') || actionType.includes('CREATED')) {
      return <Badge variant="outline" className="border-green-500 text-green-700">{actionType}</Badge>;
    }
    return <Badge variant="secondary">{actionType}</Badge>;
  };

  const getSuccessBadge = (success: boolean) => {
    return success ? (
      <Badge variant="outline" className="border-green-500 text-green-700">Success</Badge>
    ) : (
      <Badge variant="destructive">Failed</Badge>
    );
  };

  return (
    <div className="container mx-auto py-6 space-y-6">
      <div>
        <h1 className="text-3xl font-bold">Permission Audit Logs</h1>
        <p className="text-muted-foreground mt-2">
          Track all permission-related changes and security events
        </p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Search & Filter</CardTitle>
          <CardDescription>
            Search by username, role code, or resource
          </CardDescription>
        </CardHeader>
        <CardContent>
          <div className="flex gap-4">
            <div className="flex-1 relative">
              <Search className="absolute left-3 top-3 h-4 w-4 text-muted-foreground" />
              <Input
                placeholder="Search audit logs..."
                value={searchTerm}
                onChange={(e) => {
                  setSearchTerm(e.target.value);
                  setCurrentPage(0);
                }}
                className="pl-10"
              />
            </div>
            <Button
              variant="outline"
              onClick={() => {
                setSearchTerm('');
                setCurrentPage(0);
              }}
            >
              Clear
            </Button>
          </div>
        </CardContent>
      </Card>

      <Tabs value={activeTab} onValueChange={(value) => {
        setActiveTab(value);
        setCurrentPage(0);
      }}>
        <TabsList>
          <TabsTrigger value="all">
            <Shield className="mr-2 h-4 w-4" />
            All Logs
          </TabsTrigger>
          <TabsTrigger value="critical">
            <AlertTriangle className="mr-2 h-4 w-4" />
            Critical Events
          </TabsTrigger>
          <TabsTrigger value="failed">
            <XCircle className="mr-2 h-4 w-4" />
            Failed Attempts
          </TabsTrigger>
        </TabsList>

        <TabsContent value={activeTab} className="space-y-4">
          {activeQuery.isLoading ? (
            <Card>
              <CardContent className="py-10">
                <p className="text-center text-muted-foreground">Loading audit logs...</p>
              </CardContent>
            </Card>
          ) : activeQuery.isError ? (
            <Card>
              <CardContent className="py-10">
                <p className="text-center text-destructive">
                  Error loading audit logs: {String(activeQuery.error)}
                </p>
              </CardContent>
            </Card>
          ) : logs.length === 0 ? (
            <Card>
              <CardContent className="py-10">
                <p className="text-center text-muted-foreground">No audit logs found</p>
              </CardContent>
            </Card>
          ) : (
            <>
              <div className="space-y-4">
                {logs.map((log: PermissionAuditLog) => (
                  <Card key={log.id}>
                    <CardHeader>
                      <div className="flex items-start justify-between">
                        <div className="space-y-1">
                          <div className="flex items-center gap-2">
                            {getActionTypeBadge(log.actionType)}
                            {getSuccessBadge(log.success)}
                          </div>
                          <CardTitle className="text-lg">
                            {log.changedByUsername || 'Unknown User'} performed {log.actionType}
                          </CardTitle>
                          {log.targetUsername && (
                            <CardDescription>
                              Target: {log.targetUsername}
                              {log.targetRoleCode && ` (Role: ${log.targetRoleCode})`}
                            </CardDescription>
                          )}
                        </div>
                        <div className="flex items-center gap-2 text-sm text-muted-foreground">
                          <Calendar className="h-4 w-4" />
                          {format(new Date(log.changedAt), 'PPp')}
                        </div>
                      </div>
                    </CardHeader>
                    <CardContent className="space-y-3">
                      {log.reason && (
                        <div>
                          <p className="text-sm font-medium">Reason:</p>
                          <p className="text-sm text-muted-foreground">{log.reason}</p>
                        </div>
                      )}

                      <div className="grid grid-cols-2 gap-4 text-sm">
                        {log.resource && (
                          <div>
                            <p className="font-medium">Resource:</p>
                            <p className="text-muted-foreground">{log.resource}</p>
                          </div>
                        )}
                        {log.action && (
                          <div>
                            <p className="font-medium">Action:</p>
                            <p className="text-muted-foreground">{log.action}</p>
                          </div>
                        )}
                        {log.dataScope && (
                          <div>
                            <p className="font-medium">Data Scope:</p>
                            <p className="text-muted-foreground">{log.dataScope}</p>
                          </div>
                        )}
                        {log.ipAddress && (
                          <div>
                            <p className="font-medium">IP Address:</p>
                            <p className="text-muted-foreground">{log.ipAddress}</p>
                          </div>
                        )}
                      </div>

                      {(log.oldValue || log.newValue) && (
                        <div className="grid grid-cols-2 gap-4 text-sm">
                          {log.oldValue && (
                            <div>
                              <p className="font-medium">Old Value:</p>
                              <pre className="text-xs bg-muted p-2 rounded mt-1 overflow-x-auto">
                                {log.oldValue}
                              </pre>
                            </div>
                          )}
                          {log.newValue && (
                            <div>
                              <p className="font-medium">New Value:</p>
                              <pre className="text-xs bg-muted p-2 rounded mt-1 overflow-x-auto">
                                {log.newValue}
                              </pre>
                            </div>
                          )}
                        </div>
                      )}

                      {!log.success && log.errorMessage && (
                        <div className="bg-destructive/10 border border-destructive/20 rounded p-3">
                          <p className="text-sm font-medium text-destructive">Error:</p>
                          <p className="text-sm text-destructive/80">{log.errorMessage}</p>
                        </div>
                      )}

                      {log.sessionId && (
                        <div className="text-xs text-muted-foreground">
                          Session ID: {log.sessionId}
                        </div>
                      )}
                    </CardContent>
                  </Card>
                ))}
              </div>

              <Card>
                <CardContent className="pt-6">
                  <DataTablePagination
                    currentPage={currentPage}
                    pageSize={pageSize}
                    totalItems={totalElements}
                    onPageChange={setCurrentPage}
                    onPageSizeChange={(size) => {
                      setPageSize(size);
                      setCurrentPage(0);
                    }}
                  />
                </CardContent>
              </Card>
            </>
          )}
        </TabsContent>
      </Tabs>
    </div>
  );
}
