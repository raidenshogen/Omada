import { useEffect, useState } from "react"
import { NavLink, Link, useLocation, useNavigate } from "react-router-dom"
import { LayoutDashboard, LogOut, ChevronDown, ChevronRight } from "lucide-react"
import { useAuthStore } from "../store/authStore"
import api from "../services/api"
import Avatar from "./Avatar"

const row = "flex items-center gap-2.5 w-full px-2.5 py-1.5 rounded-md text-sm transition-colors"

/**
 * Navigation: dashboard, then each workspace with its boards.
 * App.jsx renders it only for signed-in sessions.
 */
export default function Sidebar() {
  const user = useAuthStore((s) => s.user)
  const logout = useAuthStore((s) => s.logout)
  const navigate = useNavigate()
  const location = useLocation()

  const [tree, setTree] = useState([])
  const [collapsed, setCollapsed] = useState({})
  const [loaded, setLoaded] = useState(false)

  useEffect(() => {
    let cancelled = false
    async function load() {
      try {
        const directWorkspaces = (await api.get("/workspaces/mine")).data || []
        const orgs = (await api.get("/organizations")).data || []
        const orgWorkspaces = orgs[0]
          ? (await api.get(`/workspaces?orgId=${encodeURIComponent(orgs[0].id)}`)).data || []
          : []
        const workspaces = [...orgWorkspaces, ...directWorkspaces.filter((direct) =>
          !orgWorkspaces.some((existing) => existing.id === direct.id))]
        const withBoards = await Promise.all(workspaces.map(async (ws) => {
          const boards = (await api.get(`/workspaces/${ws.id}/boards`)).data || []
          return { id: ws.id, name: ws.name, boards: boards.map((b) => ({ id: b.id, title: b.title })) }
        }))
        if (!cancelled) { setTree(withBoards); setLoaded(true) }
      } catch {
        if (!cancelled) setLoaded(true)
      }
    }
    load()
    return () => { cancelled = true }
  }, [location.pathname])

  const handleLogout = () => { logout(); navigate("/") }

  return (
    <aside className="sticky top-0 flex h-screen w-64 shrink-0 flex-col overflow-y-auto border-r border-line bg-surface px-3 py-4">
      <Link to="/dashboard" className="mb-6 px-2.5 font-serif text-2xl font-semibold tracking-tight text-ink">
        Omada
      </Link>

      <nav aria-label="Main" className="flex-1 space-y-0.5">
        <NavLink
          to="/dashboard"
          className={({ isActive }) => `${row} ${isActive ? "bg-signal/10 font-medium text-signal" : "text-ink-soft hover:bg-ink/5"}`}
        >
          <LayoutDashboard size={16} />
          Dashboard
        </NavLink>

        <div className="px-2.5 pb-1 pt-5 text-xs font-medium text-ink-faint">Workspaces</div>

        {!loaded ? (
          <div className="px-2.5 text-sm text-ink-faint">Loading…</div>
        ) : tree.length === 0 ? (
          <div className="px-2.5 text-sm text-ink-faint">No workspaces yet. Create one from the dashboard.</div>
        ) : (
          tree.map((ws) => {
            const open = !collapsed[ws.id]
            return (
              <div key={ws.id}>
                <button
                  onClick={() => setCollapsed((c) => ({ ...c, [ws.id]: !c[ws.id] }))}
                  aria-expanded={open}
                  className={`${row} font-medium text-ink hover:bg-ink/5`}
                >
                  {open ? <ChevronDown size={14} /> : <ChevronRight size={14} />}
                  <span className="truncate">{ws.name}</span>
                </button>
                {open && (
                  <div className="ml-[18px] border-l border-line pl-2">
                    {ws.boards.length === 0 && <div className="px-2.5 py-1 text-xs text-ink-faint">No boards yet</div>}
                    {ws.boards.map((b) => {
                      const active = location.pathname === `/board/${b.id}`
                      return (
                        <Link
                          key={b.id}
                          to={`/board/${b.id}`}
                          state={{ workspaceId: ws.id, title: b.title }}
                          aria-current={active ? "page" : undefined}
                          className={`${row} ${active ? "bg-signal/10 font-medium text-signal" : "text-ink-soft hover:bg-ink/5"}`}
                        >
                          <span className="truncate">{b.title}</span>
                        </Link>
                      )
                    })}
                  </div>
                )}
              </div>
            )
          })
        )}
      </nav>

      <div className="mt-4 border-t border-line pt-4">
        <Link to="/profile" className="mb-2 flex items-center gap-2.5 px-2.5 py-1.5 rounded-lg hover:bg-ink/5">
          <Avatar id={user?.id} name={user?.name || user?.email} size={28} />
          <div className="min-w-0">
            <div className="truncate text-sm font-medium text-ink">{user?.name || "Signed in"}</div>
            <div className="truncate text-xs text-ink-faint">{user?.email}</div>
          </div>
        </Link>
        <button onClick={handleLogout} className={`${row} text-ink-soft hover:bg-ink/5`}>
          <LogOut size={16} />
          Log out
        </button>
      </div>
    </aside>
  )
}