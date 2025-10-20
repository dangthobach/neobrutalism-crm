"use client"

import { useEffect, useState } from "react"
import { Users, Shield, Lock, TrendingUp, Activity, Clock } from "lucide-react"
import { generateUsers } from "@/lib/mock"

type StatCard = {
  title: string
  value: number | string
  icon: React.ElementType
  trend?: string
  trendUp?: boolean
}

type ActivityItem = {
  id: string
  user: string
  action: string
  timestamp: Date
  type: "create" | "update" | "delete"
}

type TrafficData = {
  date: string
  visits: number
  users: number
}

export default function AdminDashboard() {
  const [users, setUsers] = useState<any[]>([])
  const [activities, setActivities] = useState<ActivityItem[]>([])
  const [trafficData, setTrafficData] = useState<TrafficData[]>([])

  useEffect(() => {
    // Load users data
    const userData = generateUsers(75)
    setUsers(userData)

    // Generate mock activity history
    const mockActivities: ActivityItem[] = [
      { id: "1", user: "John Doe", action: "Created new user account", timestamp: new Date(Date.now() - 1000 * 60 * 5), type: "create" },
      { id: "2", user: "Jane Smith", action: "Updated role permissions", timestamp: new Date(Date.now() - 1000 * 60 * 15), type: "update" },
      { id: "3", user: "Admin", action: "Deleted inactive user", timestamp: new Date(Date.now() - 1000 * 60 * 30), type: "delete" },
      { id: "4", user: "Alex Lee", action: "Created new role", timestamp: new Date(Date.now() - 1000 * 60 * 45), type: "create" },
      { id: "5", user: "Sam Brown", action: "Updated user profile", timestamp: new Date(Date.now() - 1000 * 60 * 60), type: "update" },
      { id: "6", user: "Chris Wilson", action: "Added new permission", timestamp: new Date(Date.now() - 1000 * 60 * 90), type: "create" },
      { id: "7", user: "Taylor Davis", action: "Modified system settings", timestamp: new Date(Date.now() - 1000 * 60 * 120), type: "update" },
      { id: "8", user: "Jordan Miller", action: "Removed expired role", timestamp: new Date(Date.now() - 1000 * 60 * 180), type: "delete" },
    ]
    setActivities(mockActivities)

    // Generate mock traffic data for the last 7 days
    const mockTraffic: TrafficData[] = []
    for (let i = 6; i >= 0; i--) {
      const date = new Date()
      date.setDate(date.getDate() - i)
      mockTraffic.push({
        date: date.toLocaleDateString("en-US", { month: "short", day: "numeric" }),
        visits: Math.floor(Math.random() * 500) + 300,
        users: Math.floor(Math.random() * 200) + 100,
      })
    }
    setTrafficData(mockTraffic)
  }, [])

  const stats: StatCard[] = [
    {
      title: "Total Users",
      value: users.length,
      icon: Users,
      trend: "+12%",
      trendUp: true,
    },
    {
      title: "Active Roles",
      value: 4,
      icon: Shield,
      trend: "+2",
      trendUp: true,
    },
    {
      title: "Permissions",
      value: 8,
      icon: Lock,
      trend: "0%",
      trendUp: false,
    },
    {
      title: "Activity Today",
      value: activities.length,
      icon: Activity,
      trend: "+5",
      trendUp: true,
    },
  ]

  const getActivityColor = (type: ActivityItem["type"]) => {
    switch (type) {
      case "create":
        return "bg-green-500"
      case "update":
        return "bg-blue-500"
      case "delete":
        return "bg-red-500"
      default:
        return "bg-gray-500"
    }
  }

  const formatTimeAgo = (date: Date) => {
    const seconds = Math.floor((new Date().getTime() - date.getTime()) / 1000)
    if (seconds < 60) return `${seconds}s ago`
    const minutes = Math.floor(seconds / 60)
    if (minutes < 60) return `${minutes}m ago`
    const hours = Math.floor(minutes / 60)
    if (hours < 24) return `${hours}h ago`
    return `${Math.floor(hours / 24)}d ago`
  }

  const maxVisits = Math.max(...trafficData.map(d => d.visits))

  return (
    <div className="space-y-4">
      <header className="border-4 border-black bg-main text-main-foreground p-4 shadow-[8px_8px_0_#000]">
        <h1 className="text-2xl font-heading">Dashboard</h1>
        <p className="text-sm font-base mt-1 opacity-90">Welcome back! Here&apos;s what&apos;s happening today.</p>
      </header>

      {/* Stats Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {stats.map((stat, index) => {
          const Icon = stat.icon
          return (
            <div
              key={index}
              className="border-4 border-black bg-background p-4 shadow-[8px_8px_0_#000] hover:translate-x-1 hover:translate-y-1 transition-all"
            >
              <div className="flex items-start justify-between">
                <div className="flex-1">
                  <p className="text-sm font-base text-foreground/70">{stat.title}</p>
                  <p className="text-3xl font-heading mt-2">{stat.value}</p>
                  {stat.trend && (
                    <div className="flex items-center gap-1 mt-2">
                      <TrendingUp className={`h-3 w-3 ${stat.trendUp ? "text-green-600" : "text-red-600"}`} />
                      <span className={`text-xs font-base ${stat.trendUp ? "text-green-600" : "text-red-600"}`}>
                        {stat.trend}
                      </span>
                      <span className="text-xs font-base text-foreground/50">vs last week</span>
                    </div>
                  )}
                </div>
                <div className="border-2 border-black p-2 bg-main">
                  <Icon className="h-5 w-5 text-main-foreground" />
                </div>
              </div>
            </div>
          )
        })}
      </div>

      {/* Traffic Chart and Activity History */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
        {/* Traffic Chart */}
        <div className="lg:col-span-2 border-4 border-black bg-background p-4 shadow-[8px_8px_0_#000]">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-xl font-heading">Traffic Overview</h2>
            <div className="flex gap-4 text-sm font-base">
              <div className="flex items-center gap-2">
                <div className="w-3 h-3 bg-main border-2 border-black"></div>
                <span>Visits</span>
              </div>
              <div className="flex items-center gap-2">
                <div className="w-3 h-3 bg-secondary border-2 border-black"></div>
                <span>Users</span>
              </div>
            </div>
          </div>

          <div className="space-y-3">
            {trafficData.map((data, index) => (
              <div key={index} className="space-y-1">
                <div className="flex items-center justify-between text-sm font-base">
                  <span className="w-16">{data.date}</span>
                  <div className="flex-1 ml-4 grid grid-cols-2 gap-2">
                    <div className="relative h-8 border-2 border-black bg-secondary-background">
                      <div
                        className="h-full bg-main border-r-2 border-black transition-all"
                        style={{ width: `${(data.visits / maxVisits) * 100}%` }}
                      ></div>
                      <span className="absolute inset-0 flex items-center justify-center text-xs font-heading">
                        {data.visits}
                      </span>
                    </div>
                    <div className="relative h-8 border-2 border-black bg-secondary-background">
                      <div
                        className="h-full bg-secondary border-r-2 border-black transition-all"
                        style={{ width: `${(data.users / maxVisits) * 100}%` }}
                      ></div>
                      <span className="absolute inset-0 flex items-center justify-center text-xs font-heading">
                        {data.users}
                      </span>
                    </div>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Activity History */}
        <div className="border-4 border-black bg-background p-4 shadow-[8px_8px_0_#000]">
          <div className="flex items-center gap-2 mb-4">
            <Clock className="h-5 w-5" />
            <h2 className="text-xl font-heading">Recent Activity</h2>
          </div>

          <div className="space-y-3 max-h-[400px] overflow-y-auto">
            {activities.map((activity) => (
              <div
                key={activity.id}
                className="border-2 border-black p-3 bg-secondary-background hover:translate-x-1 transition-all"
              >
                <div className="flex items-start gap-3">
                  <div className={`w-2 h-2 rounded-full mt-2 ${getActivityColor(activity.type)} border border-black`}></div>
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-heading truncate">{activity.user}</p>
                    <p className="text-xs font-base text-foreground/70 mt-1">{activity.action}</p>
                    <p className="text-xs font-base text-foreground/50 mt-1">{formatTimeAgo(activity.timestamp)}</p>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* User Distribution by Role */}
      <div className="border-4 border-black bg-background p-4 shadow-[8px_8px_0_#000]">
        <h2 className="text-xl font-heading mb-4">User Distribution by Role</h2>
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
          {["Admin", "Manager", "User"].map((role) => {
            const count = users.filter((u) => u.role === role).length
            const percentage = users.length > 0 ? Math.round((count / users.length) * 100) : 0
            return (
              <div key={role} className="border-2 border-black p-4 bg-secondary-background">
                <div className="flex items-center justify-between mb-2">
                  <span className="font-heading">{role}</span>
                  <span className="font-base text-sm">{count}</span>
                </div>
                <div className="relative h-4 border-2 border-black bg-background">
                  <div
                    className="h-full bg-main transition-all"
                    style={{ width: `${percentage}%` }}
                  ></div>
                </div>
                <p className="text-xs font-base text-foreground/60 mt-1">{percentage}% of total users</p>
              </div>
            )
          })}
        </div>
      </div>
    </div>
  )
}
