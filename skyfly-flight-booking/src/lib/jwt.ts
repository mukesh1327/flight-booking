type JwtPayload = {
  realm_access?: { roles?: string[] }
  resource_access?: Record<string, { roles?: string[] }>
}

export const decodeRoles = (token: string): string[] => {
  if (!token) {
    return []
  }

  try {
    const encodedPayload = token.split('.')[1]
    if (!encodedPayload) {
      return []
    }

    const payload = atob(encodedPayload.replace(/-/g, '+').replace(/_/g, '/'))
    const parsed = JSON.parse(payload) as JwtPayload
    const roles = new Set<string>()

    parsed.realm_access?.roles?.forEach((role) => roles.add(role))
    parsed.resource_access?.['authservice-client']?.roles?.forEach((role) => roles.add(role))

    return Array.from(roles)
  } catch {
    return []
  }
}
