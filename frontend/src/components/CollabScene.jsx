import { motion, useReducedMotion } from "framer-motion"

/**
 * Looping motion graphic: two teammates working on the same board.
 * Amal drags a card from "To do" to "Doing" while Youssef types in "Done".
 * One 9s loop; every moving part shares the same keyframe times so it stays in sync.
 * Static when the visitor prefers reduced motion.
 */
const T = [0, 0.2, 0.5, 0.88, 1]
const loop = { duration: 9, times: T, repeat: Infinity, ease: "easeInOut" }
const COLS = ["To do", "Doing", "Done"]
const TINT = ["#FFF1E0", "#E6EEFF", "#E3F5EA"]
const DOT = ["#E4A23A", "#4F7CF0", "#3E9B4F"]

function Cursor({ name, color, animate, transition }) {
  return (
    <motion.div className="pointer-events-none absolute left-0 top-0 z-10" animate={animate} transition={transition} initial={false}>
      <svg width="18" height="18" viewBox="0 0 18 18" aria-hidden="true">
        <path d="M2 1l13 7-5.5 1.6L7.6 15 2 1z" fill={color} stroke="#fff" strokeWidth="1.2" strokeLinejoin="round" />
      </svg>
      <span className="ml-3 -mt-1 inline-block rounded-md px-1.5 py-0.5 text-[11px] font-medium text-white" style={{ backgroundColor: color }}>
        {name}
      </span>
    </motion.div>
  )
}

export default function CollabScene() {
  const still = useReducedMotion()
  const a = (v, t = loop) => (still ? {} : { animate: v, transition: t })
  return (
    <div role="img" aria-label="Two teammates moving and editing cards on a shared board" className="relative h-[250px] w-[450px] max-w-full">
      {COLS.map((c, i) => (
        <div key={c} className="absolute top-0 h-full w-[140px] rounded-lg" style={{ left: i * 155, backgroundColor: TINT[i] }}>
          <div className="flex items-center gap-1.5 px-3 pt-2.5 text-xs font-medium text-ink"><span className="h-2 w-2 rounded-full" style={{ backgroundColor: DOT[i] }} />{c}</div>
          {/* resting cards */}
          <div className="absolute left-2 top-[88px] h-11 w-[124px] rounded-md bg-white/70" />
          {i === 2 && <div className="absolute left-2 top-[36px] h-11 w-[124px] rounded-md bg-white/70" />}
          {i === 0 && <div className="absolute left-2 top-[140px] h-11 w-[124px] rounded-md bg-white/70" />}
        </div>
      ))}

      {/* the card being moved */}
      <motion.div
        className="absolute left-2 top-[36px] z-[5] flex h-11 w-[124px] items-center rounded-md border bg-white px-2.5 text-xs font-medium text-ink"
        initial={false}
        style={{ borderColor: "#E4572E" }}
        {...a({ x: [0, 0, 155, 155, 0], scale: [1, 1.05, 1.05, 1, 1], opacity: [1, 1, 1, 1, 1] })}
      >
        Pricing page
      </motion.div>

      {/* Youssef typing in a card */}
      <div className="absolute left-[318px] top-[36px] z-[5] flex h-11 w-[124px] items-center rounded-md border bg-white px-2.5" style={{ borderColor: "#2F6FDE" }}>
        <motion.span className="block h-1.5 rounded-full bg-ink/70" initial={{ width: 70 }} {...a({ width: [30, 90, 50, 100, 30] })} />
      </div>

      <Cursor name="Amal" color="#E4572E"
        {...a({ x: [380, 90, 245, 245, 380], y: [200, 66, 66, 66, 200] })} />
      <Cursor name="Youssef" color="#2F6FDE"
        {...a({ x: [400, 395, 405, 395, 400], y: [120, 62, 70, 62, 120] }, { ...loop, duration: 9 })} />
    </div>
  )
}