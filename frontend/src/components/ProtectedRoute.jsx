import { Navigate, useParams } from "react-router-dom"
import { useAuthStore } from "../store/authStore"

/** Role hierarchy: owner > admin > member > viewer. */
const ROLE_RANK = { owner: 4, admin: 3, member: 2, viewer: 1 }

/**
 * Guards routes by authentication, workspace membership and role.
 * Membership checks only run once memberships have been loaded: the store starts
 * empty and is not persisted, so an empty list means "unknown", not "no access".
 * The backend remains the real authority on access.
 */
export function ProtectedRoute({ children, requiredRole, workspaceParam }) {
  const authed = useAuthStore((s) => !!(s.isAuthenticated || s.accessToken))
  const memberships = useAuthStore((s) => s.memberships)
  const params = useParams()

  if (!authed) return <Navigate to="/" replace />

  if (workspaceParam && memberships.length > 0) {
    const workspaceId = params[workspaceParam]
    if (workspaceId) {
      const membership = memberships.find((m) => m.workspaceId === workspaceId)
      if (!membership) return <Navigate to="/dashboard" replace />
      if (requiredRole && (ROLE_RANK[membership.role] ?? 0) < (ROLE_RANK[requiredRole] ?? 0)) {
        return <Navigate to="/dashboard" replace />
      }
    }
  }

  return children
}