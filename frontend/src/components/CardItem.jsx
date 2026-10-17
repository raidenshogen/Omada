import { motion } from 'framer-motion'

export default function CardItem({ title, description, assignees, labels }) {
  return (
    <motion.div layout className="card-base p-3 cursor-pointer" whileHover={{scale:1.02}}>
      <div className="font-medium text-sm text-neutral-900">{title}</div>
      {description && <div className="text-xs text-neutral-500 mt-1">{description}</div>}
      {labels && labels.length > 0 && (
        <div className="flex gap-1 mt-2">
          {labels.map(l => (
            <span key={l} className="text-[10px] px-2 py-0.5 bg-brand-50 text-brand-700 rounded border border-brand-100">{l}</span>
          ))}
        </div>
      )}
      {assignees && assignees.length > 0 && (
        <div className="text-[10px] text-neutral-400 mt-2 font-mono">Assignees: {assignees.join(', ')}</div>
      )}
    </motion.div>
  )
}
