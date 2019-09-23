package queryevaluator

import scala.collection.{mutable, immutable}

/**
 * Encapsulates select list of a query.  No requirement of uniqueness.
 */
class SelectList(val columnRefs: immutable.List[SelectColumnRef]) {

	val arity = columnRefs.size
		
	def contains(colRef: ColumnRef) = columnRefs.contains(colRef)
	
}