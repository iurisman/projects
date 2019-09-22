package queryevaluator

import scala.collection.{mutable, immutable}

/**
 * Encapsulates select list of a query.
 * Each tuple = (name, optTableAias, optColAlias)
 */
class SelectList(columnRefs: List[ColumnRef]) {
  	
	// Convert to map keyed by the column alias, if given, or own full name, if not.
	// This will ensure that thre are no duplicate aliases.
	private[this] val columnMap = new mutable.LinkedHashMap[String, ColumnRef]() {
		
		columnRefs.map { colRef =>
			if (getOrElseUpdate(colRef.columnAlias, colRef) != colRef) {
				throw new RuntimeException(s"Duplicate column name ${colRef.columnAlias}")
			}
		}		
	}

	/**
	 * 
	 */
	val arity = columnMap.size
	
	/**
	 * Column references, as referenced in the select list.
	 */
	def colRefs: Seq[ColumnRef] = columnMap.values.toSeq
	
	/**
	 * Actual table columns, corresponding the column references above
	 */
	def columns: Seq[Table.Column] = colRefs.map(_.column)

}