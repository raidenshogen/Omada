import { motion } from 'framer-motion'

export default function CardItem({ title, description, assignees, labels }) {
  return (
    <motion.div layout className="bg-white p-3 rounded-lg shadow cursor-pointer hover:shadow-md transition-shadow" whileHover={{scale:1.02}}>
      <div className="font-medium text-sm">{title}</div>
      {description && <div className="text-xs text-gray-500 mt-1">{description}</div>}
      {labels && labels.length > 0 && (
        <div className="flex gap-1 mt-2">
          {labels.map(l => (
            <span key={l} className="text-[10px] px-2 py-0.5 bg-indigo-100 text-indigo-700 rounded">{l}</span>
          ))}
        </div>
      )}
      {assignees && assignees.length > 0 && (
        <div className="text-[10px] text-gray-400 mt-2">Assignees: {assignees.join(', ')}</div>
      )}
    </motion.div>
  )
}
