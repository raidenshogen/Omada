const COLORS = ["#E4572E", "#D99A00", "#3E9B4F", "#0F9AA8", "#2F6FDE", "#7B52D3", "#C2409B", "#5B6B7A"]

/** Same person -> same color, on every screen. Keyed by id, falling back to name/email. */
export function personColor(key = "") {
  let h = 0
  for (let i = 0; i < key.length; i++) h = (h * 31 + key.charCodeAt(i)) >>> 0
  return COLORS[h % COLORS.length]
}

export default function Avatar({ id, name, size = 28 }) {
  const label = name || id || "?"
  return (
    <span
      title={label}
      className="inline-flex shrink-0 items-center justify-center rounded-full font-medium text-white select-none"
      style={{ width: size, height: size, fontSize: size * 0.42, backgroundColor: personColor(id || name || "") }}
    >
      {label.charAt(0).toUpperCase()}
    </span>
  )
}