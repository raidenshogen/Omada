import { motion } from 'framer-motion'
import { useState } from 'react'
import { useAuthStore } from '../store/authStore'
import api from '../services/api'

const ROLE_LABELS = { owner: 'Owner', admin: 'Admin', member: 'Member', viewer: 'Viewer' }
const ROLE_DESCRIPTIONS = {
  owner: 'Full workspace control',
  admin: 'Can manage workspace members and structure',
  member: 'Can edit workspace content',
  viewer: 'Can view workspace content only',
}
const ROLE_RANK = { owner: 4, admin: 3, member: 2, viewer: 1 }

const ROLE_BADGE = {
  owner: 'bg-amber-100 text-amber-700',
  admin: 'bg-brand-100 text-brand-700',
  member: 'bg-neutral-100 text-neutral-700',
  viewer: 'bg-blue-100 text-blue-700',
}

/**
 * MemberRow: displays a single workspace member with their role badge.
 * Owner/Admin viewers see a role-change dropdown and a remove button.
 * Member/Viewer viewers see read-only information only.
 */
export default function MemberRow({ member, workspaceId, onRemoved, onRoleChanged }) {
  const role = useAuthStore((s) => s.role)
  const [editing, setEditing] = useState(false)
  const [newRole, setNewRole] = useState(member.role)
  const [saving, setSaving] = useState(false)
  const [removing, setRemoving] = useState(false)
  const [error, setError] = useState('')
  const isManager = role === 'owner' || role === 'admin'
  const isSelf = member.userId === useAuthStore.getState().user?.id
  const memberRole = (member.role || '').toLowerCase()
  const canManageMember = isManager && !isSelf && (ROLE_RANK[role] || 0) > (ROLE_RANK[memberRole] || 0)
  
  const handleRoleChange = async (e) => {
    e.preventDefault()
    if (!canManageMember || saving) return
    setSaving(true)
    setError('')
    try {
      await api.patch(`/workspaces/${workspaceId}/members/${member.userId}`, {
        role: newRole,
      })
      onRoleChanged?.(member.userId, newRole)
      setEditing(false)
    } catch (e) {
      setError(e?.response?.data?.message || 'Could not change role')
    } finally {
      setSaving(false)
    }
  }
  
  const handleRemove = async () => {
      if (!canManageMember || removing) return
      if (!window.confirm(`Remove ${member.email} from this workspace?`)) return
      setRemoving(true)
      try {
        await api.delete(`/workspaces/${workspaceId}/members/${member.userId}`)
        onRemoved?.(member.userId)
      } catch (e) {
        setError(e?.response?.data?.message || 'Could not remove member')
      } finally {
        setRemoving(false)
      }
    }

  return (
    <motion.div
      initial={{ opacity: 0, height: 0 }}
      animate={{ opacity: 1, height: 'auto' }}
      exit={{ opacity: 0, height: 0 }}
      className="p-3 bg-white rounded-lg flex items-center justify-between gap-4"
    >
      <div className="flex items-center gap-3 flex-1 min-w-0">
        <div className="w-8 h-8 rounded-full bg-neutral-200 flex items-center justify-center text-neutral-500 text-sm font-medium shrink-0">
          {member.email?.charAt(0).toUpperCase() || '?'}
        </div>
        <div className="flex-1 min-w-0">
          <div className="text-sm font-medium">{member.email}</div>
          {member.name && (
            <div className="text-xs text-neutral-500">{member.name}</div>
          )}
        </div>
      </div>

      <div className="flex items-center gap-3">
        <span
          title={ROLE_DESCRIPTIONS[memberRole] || 'Workspace role'}
          aria-label={`Workspace role: ${ROLE_LABELS[memberRole] || member.role}`}
          className={`text-xs font-medium px-2.5 py-1 rounded-full ${ROLE_BADGE[memberRole] || ROLE_BADGE.member}`}
        >
          {ROLE_LABELS[memberRole] || member.role}
        </span>

        {canManageMember && (
          <>
            {!editing ? (
              <button
                onClick={() => setEditing(true)}
                className="text-xs text-neutral-500 hover:text-brand-600 underline underline-offset-4 font-medium"
              >
                Change role
              </button>
            ) : (
              <form onSubmit={handleRoleChange} className="flex items-center gap-2">
                <select
                  value={newRole}
                  onChange={(e) => setNewRole(e.target.value)}
                  disabled={saving}
                  className="input-field text-xs w-24"
                >
                  {['owner', 'admin', 'member', 'viewer'].map((r) => (
                    <option key={r} value={r}>
                      {ROLE_LABELS[r]}
                    </option>
                  ))}
                </select>
                <button
                  type="submit"
                  disabled={saving}
                  className="btn-primary text-xs px-3 py-1.5"
                >
                  {saving ? 'Saving…' : 'Save'}
                </button>
                <button
                  type="button"
                  onClick={() => setEditing(false)}
                  disabled={saving}
                  className="btn-ghost text-xs px-3 py-1.5"
                >
                  Cancel
                </button>
              </form>
            )}
            {error && (
              <span className="text-xs text-red-600">{error}</span>
            )}
          </>
        )}

        {canManageMember && (
          <button
            onClick={handleRemove}
            disabled={removing}
            className="text-xs text-red-600 hover:text-red-800 font-medium"
          >
            {removing ? 'Removing…' : 'Remove'}
          </button>
        )}
      </div>
    </motion.div>
  )
}
