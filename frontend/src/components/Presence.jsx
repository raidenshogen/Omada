import { AnimatePresence, motion } from "framer-motion"
import Avatar from "./Avatar"

/**
 * Who is here right now. Teammates slide in and out as they join or leave,
 * with a quiet live dot. users = [{ userId, name }] (same shape usePresence returns).
 */
export default function Presence({ users = [] }) {
  return (
    <div className="flex items-center gap-3" aria-label={`${users.length} online`}>
      <div className="flex -space-x-2">
        <AnimatePresence initial={false}>
          {users.map((u) => (
            <motion.span key={u.userId} initial={{ opacity: 0, scale: 0.6, x: 8 }} animate={{ opacity: 1, scale: 1, x: 0 }}
              exit={{ opacity: 0, scale: 0.6 }} transition={{ type: "spring", stiffness: 380, damping: 26 }}
              className="rounded-full ring-2 ring-paper">
              <Avatar id={u.userId} name={u.name} size={28} />
            </motion.span>
          ))}
        </AnimatePresence>
      </div>
      {users.length > 0 && (
        <span className="flex items-center gap-1.5 text-xs text-ink-soft">
          <span className="relative flex h-2 w-2">
            <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-person-leaf opacity-60" />
            <span className="relative inline-flex h-2 w-2 rounded-full bg-person-leaf" />
          </span>
          {users.length} here now
        </span>
      )}
    </div>
  )
}