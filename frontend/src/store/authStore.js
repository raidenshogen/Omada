import { create } from 'zustand'
import { persist, createJSONStorage } from 'zustand/middleware'

export const useAuthStore = create(
  persist(
    (set, get) => ({
      user: null,
      accessToken: null,
      refreshToken: null,
      isAuthenticated: false,
      /** Workspace memberships for the authenticated user. */
      memberships: [],
      /** Currently selected workspace context. */
      currentWorkspace: null,
      /** Role within the current workspace. */
      role: null,
      /** Pending workspace invitations received by the user. */
      pendingInvitations: [],
      login: (user, accessToken, refreshToken) => set({ user, accessToken, refreshToken, isAuthenticated: true }),
      setUser: (u) => set({ user: u, isAuthenticated: !!u }),
      setTokens: (accessToken, refreshToken) => set({ accessToken, refreshToken }),
      /** Replace the whole memberships array (after loading from the API). */
      setMemberships: (memberships) => set({ memberships }),
      /** Set the current workspace context. */
      setCurrentWorkspace: (workspace) => set({ currentWorkspace: workspace }),
      /** Set the user's role within the current workspace. */
      setRole: (role) => set({ role }),
      addMembership: (membership) =>
        set((s) => ({
          memberships: [
            ...s.memberships.filter((m) => m.workspaceId !== membership.workspaceId),
            membership,
          ],
        })),
      /** Add a pending invitation (deduped by id). */
      addInvitation: (invitation) =>
        set((s) => ({
          pendingInvitations: s.pendingInvitations.some((i) => i.id === invitation.id)
            ? s.pendingInvitations
            : [...s.pendingInvitations, invitation],
        })),
      /** Remove a pending invitation by id. */
      removeInvitation: (invitationId) =>
        set((s) => ({
          pendingInvitations: s.pendingInvitations.filter((i) => i.id !== invitationId),
        })),
      logout: () => {
        localStorage.removeItem('accessToken')
        localStorage.removeItem('refreshToken')
        set({
          user: null,
          accessToken: null,
          refreshToken: null,
          isAuthenticated: false,
          memberships: [],
          currentWorkspace: null,
          role: null,
          pendingInvitations: [],
        })
      },
      initFromStorage: () => {
        const accessToken = localStorage.getItem('accessToken')
        const refreshToken = localStorage.getItem('refreshToken')
        if (accessToken && refreshToken) {
          try {
            const payload = JSON.parse(atob(accessToken.split('.')[1]))
            const now = Math.floor(Date.now() / 1000)
            if (payload.exp && payload.exp < now) {
              localStorage.removeItem('accessToken')
              localStorage.removeItem('refreshToken')
              window.location.href = '/'
              return
            }
          } catch {}
          set({ accessToken, refreshToken, isAuthenticated: true })
        }
      }
    }),
    {
      name: 'omada-auth',
      storage: createJSONStorage(() => localStorage),
      partialize: (state) => ({ user: state.user, accessToken: state.accessToken, refreshToken: state.refreshToken })
    }
  )
)
