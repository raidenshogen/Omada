import { useState } from "react"
import { useNavigate, Link } from "react-router-dom"
import { useAuthStore } from "../store/authStore"
import api from "../services/api"
import CollabScene from "../components/CollabScene"

export default function Login() {
  const [email, setEmail] = useState("")
  const [password, setPassword] = useState("")
  const [error, setError] = useState("")
  const [busy, setBusy] = useState(false)
  const navigate = useNavigate()
  const login = useAuthStore((s) => s.login)

  const handleLogin = async (e) => {
    e.preventDefault()
    if (busy) return
    setBusy(true)
    setError("")
    try {
      const res = await api.post("/auth/login", { email, password })
      const { accessToken, refreshToken, user } = res.data
      localStorage.setItem("accessToken", accessToken)
      localStorage.setItem("refreshToken", refreshToken)
      login(user, accessToken, refreshToken)
      navigate("/dashboard")
    } catch (err) {
      setError(err?.response?.data?.message || "Email or password is incorrect. Check both and try again.")
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="grid min-h-screen bg-surface lg:grid-cols-[1.1fr_1fr]">
      <aside className="hidden flex-col justify-between border-r border-line bg-gradient-to-br from-brand-50 via-paper to-white p-12 lg:flex">
        <span className="font-serif text-2xl font-semibold text-ink">Omada</span>
        <div>
          <h1 className="max-w-md">Build it together, as it happens.</h1>
          <p className="mt-3 max-w-sm text-ink-soft">Boards, documents and activity that update for everyone the moment someone makes a change.</p>
          <div className="mt-10"><CollabScene /></div>
        </div>
        <p className="text-sm text-ink-faint">Move a card or type in a document and your team sees it live.</p>
      </aside>

      <main className="flex items-center justify-center p-6">
        <form onSubmit={handleLogin} className="w-full max-w-sm" noValidate>
          <h2 className="mb-1">Log in</h2>
          <p className="mb-8 text-sm text-ink-soft">Pick up where your team left off.</p>

          {error && (
            <div role="alert" className="mb-4 rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
              {error}
            </div>
          )}

          <label className="label" htmlFor="login-email">Email</label>
          <input id="login-email" type="email" autoComplete="email" className="input-field mb-4" value={email} onChange={(e) => setEmail(e.target.value)} />

          <label className="label" htmlFor="login-password">Password</label>
          <input id="login-password" type="password" autoComplete="current-password" className="input-field mb-6" value={password} onChange={(e) => setPassword(e.target.value)} />

          <button type="submit" disabled={busy || !email || !password} className="btn-primary w-full py-2.5">
            {busy ? "Logging in…" : "Log in"}
          </button>

          <p className="mt-6 text-sm text-ink-soft">
            New to Omada? <Link to="/register" className="font-medium text-signal underline underline-offset-4">Create an account</Link>
          </p>
        </form>
      </main>
    </div>
  )
}