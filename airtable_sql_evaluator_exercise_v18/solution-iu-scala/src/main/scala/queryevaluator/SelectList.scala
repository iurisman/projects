package queryevaluator

import scala.collection.{mutable, immutable}

/**
 * Encapsulates select list of a query.  No requirement of uniqueness.
 */
class SelectList(val columnRefs: immutable.List[SelectColumnRef]) {

	val arity = columnRefs.size
		
	/**
	 * Does this set contain given column reference?
	 */
	def contains(colRef: ColumnRef): Boolean = columnRefs.contains(colRef)
	
}