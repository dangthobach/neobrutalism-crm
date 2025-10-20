export type MockUser = {
  id: string
  name: string
  email: string
  role: string
  createdAt: Date
}

export type MockRole = {
  id: string
  name: string
  description?: string
}

export type MockPermission = {
  id: string
  key: string
  description?: string
}

function randomPick<T>(arr: T[], i: number) {
  return arr[i % arr.length]
}

export function generateUsers(count = 50): MockUser[] {
  const firstNames = ["John", "Jane", "Alex", "Sam", "Taylor", "Jordan", "Morgan", "Riley", "Chris", "Cameron"]
  const lastNames = ["Doe", "Smith", "Lee", "Brown", "Johnson", "Miller", "Davis", "Wilson", "Clark", "Lopez"]
  const roles = ["Admin", "Manager", "User"]

  const list: MockUser[] = []
  const now = new Date()
  for (let i = 0; i < count; i++) {
    const first = randomPick(firstNames, i)
    const last = randomPick(lastNames, count - i - 1)
    const name = `${first} ${last}`
    const email = `${first.toLowerCase()}.${last.toLowerCase()}${i}@example.com`
    // Generate random dates within the last 180 days
    const daysAgo = Math.floor(Math.random() * 180)
    const createdAt = new Date(now.getTime() - daysAgo * 24 * 60 * 60 * 1000)
    list.push({ id: `u${i + 1}`, name, email, role: randomPick(roles, i + 2), createdAt })
  }
  return list
}

export function generateRoles(): MockRole[] {
  return [
    { id: "r1", name: "Admin", description: "Full access to all resources" },
    { id: "r2", name: "Manager", description: "Manage teams and pipelines" },
    { id: "r3", name: "User", description: "Basic access for daily tasks" },
    { id: "r4", name: "Analyst", description: "Read-only analytics access" },
  ]
}

export function generatePermissions(): MockPermission[] {
  const base: MockPermission[] = [
    { id: "p1", key: "user.read", description: "Read users" },
    { id: "p2", key: "user.write", description: "Create and edit users" },
    { id: "p3", key: "role.read", description: "Read roles" },
    { id: "p4", key: "role.write", description: "Create and edit roles" },
    { id: "p5", key: "permission.read", description: "Read permissions" },
    { id: "p6", key: "permission.write", description: "Create and edit permissions" },
    { id: "p7", key: "deal.move", description: "Move deal stage" },
    { id: "p8", key: "task.complete", description: "Complete tasks" },
  ]
  return base
}


