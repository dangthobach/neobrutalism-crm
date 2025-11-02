// Lightweight mock generators used by client pages

export type DashboardUser = {
  id: string
  role: "Admin" | "Manager" | "User"
}

export function generateUsers(count: number = 50): DashboardUser[] {
  const roles: DashboardUser["role"][] = ["Admin", "Manager", "User"]
  const users: DashboardUser[] = []
  for (let i = 0; i < count; i++) {
    users.push({ id: `u_${i + 1}`, role: roles[i % roles.length] })
  }
  return users
}

export type SimplePermission = {
  id: string
  key: string
  description?: string
}

export function generatePermissions(): SimplePermission[] {
  const base: Omit<SimplePermission, "id">[] = [
    { key: "user.view", description: "View users" },
    { key: "user.create", description: "Create users" },
    { key: "user.edit", description: "Edit users" },
    { key: "user.delete", description: "Delete users" },
    { key: "role.view", description: "View roles" },
    { key: "role.create", description: "Create roles" },
    { key: "role.edit", description: "Edit roles" },
    { key: "role.delete", description: "Delete roles" },
    { key: "menu.view", description: "View menus" },
    { key: "menu.manage", description: "Manage menu structure" },
    { key: "permission.view", description: "View permissions" },
    { key: "permission.manage", description: "Manage permissions" },
    { key: "audit.view", description: "View audit logs" },
    { key: "settings.manage", description: "Manage system settings" },
  ]

  return base.map((p, idx) => ({ id: `perm_${idx + 1}`, ...p }))
}

