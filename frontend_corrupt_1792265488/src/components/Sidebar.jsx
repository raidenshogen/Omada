import { useEffect, useState } from 'react'
import { NavLink, Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuthStore } from '../store/authStore'
import api from '../services/api'
import { LayoutDashboard, LogOut, Briefcase, KanbanSquare, ChevronDown, ChevronRight } from 'lucide-react'

/**
 * Trello/Notion-style navigation sidebar: the signed-in user's workspaces
 * with their boards, active-route highlighting, and the account section.
 * Guests never see it — App.jsx renders it only for authenticated sessions.
 */
export default function Sidebar(){
  const user = useAuthStore(s => s.user)
  const logout = useAuthStore(s => s.logout)
  const navigate = useNavigate()
  const location = useLocation()

  const [tree, setTree] = useState([]) // [{ id, name, boards: [{id,title}] }]
  const [collapsed, setCollapsed] = useState({}) // workspaceId -> bool
  const [loaded, setLoaded] = useState(false)

  // (Re)load the workspace/board tree whenever the route changes, so boards
  // created on the Workspace page appear immediately.
  useEffect(() => {
    let cancelled = false
    async function load() {
      try {
        const orgs = (await api.get('/organizations')).data || []
        if (orgs.length === 0) { if (!cancelled) { setTree([]); setLoaded(true) } return }
        const workspaces = (await api.get(`/workspaces?orgId=${encodeURIComponent(orgs[0].id)}`)).data || []
        const withBoards = await Promise.all(workspaces.map(async (ws) => {
          const boards = (await api.get(`/workspaces/${ws.id}/boards`)).data || []
          return { id: ws.id, name: ws.name, boards: boards.map(b => ({ id: b.id, title: b.title })) }
        }))
        if (!cancelled) { setTree(withBoards); setLoaded(true) }
      } catch {
        if (!cancelled) setLoaded(true) // sidebar stays usable even if the tree fails
      }
    }
    load()
    return () => { cancelled = true }
  }, [location.pathname])

  const handleLogout = () => {
    logout()
    navigate('/')
  }

  return (
    <motion.aside initial={{x:-250}} animate={{x:0}} className="w-64 bg-gray-900 text-white h-screen p-4 flex flex-col overflow-y-auto">
      <h1 className="text-xl font-bold mb-6">Omada</h1>

      <nav className="space-y-1 flex-1">
        <NavLink
          to="/dashboard"
          className={({isActive}) => `flex items-center gap-3 p-2 rounded transition ${isActive ? 'bg-gray-800 text-white' : 'hover:bg-gray-800 text-gray-300'}`}
        >
          <LayoutDashboard size={18} />
          <span>Dashboard</span>
        </NavLink>

        <div className="pt-4 pb-1 px-2 text-[11px] font-semibold uppercase tracking-wider text-gray-500">
          Workspaces
        </div>

        {!loaded ? (
          <div className="px-2 text-sm text-gray-500">Loading…</div>
        ) : tree.length === 0 ? (
          <div className="px-2 text-sm text-gray-500">No workspaces yet</div>
        ) : (
          tree.map(ws => (
            <div key={ws.id} className="mb-1">
              <button
                onClick={() => setCollapsed(c => ({ ...c, [ws.id]: !c[ws.id] }))}
                className="flex items-center gap-2 w-full p-2 rounded hover:bg-gray-800 text-gray-200 text-sm"
              >
                {collapsed[ws.id] ? <ChevronRight size={14} /> : <ChevronDown size={14} />}
                <Briefcase size={15} />
                <span className="truncate">{ws.name}</span>
              </button>
              {!collapsed[ws.id] && (
                <div className="ml-6 space-y-0.5">
                  {ws.boards.length === 0 && (
                    <div className="px-2 py-1 text-xs text-gray-500">No boards</div>
                  )}
                  {ws.boards.map(b => {
                    const active = location.pathname === `/board/${b.id}`
                    return (
                      <Link
                        key={b.id}
                        to={`/board/${b.id}`}
                        className={`flex items-center gap-2 p-2 rounded text-sm transition ${active ? 'bg-brand-600/30 text-white' : 'hover:bg-gray-800 text-gray-300'}`}
                      >
                        <KanbanSquare size={14} />
                        <span className="truncate">{b.title}</span>
                      </Link>
                    )
                  })}
                </div>
              )}
            </div>
          ))
        )}
      </nav>

      <div className="border-t border-gray-700 pt-4">
        <div className="text-sm text-gray-400 mb-2 truncate">{user?.email || 'Signed in'}</div>
        <button onClick={handleLogout} className="flex items-center gap-3 w-full p-2 hover:bg-gray-800 rounded transition text-red-400">
          <LogOut size={18} />
          <span>Logout</span>
        </button>
      </div>
    </motion.aside>
  )
}
