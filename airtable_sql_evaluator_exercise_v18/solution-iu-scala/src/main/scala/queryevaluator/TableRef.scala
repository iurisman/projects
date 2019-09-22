package queryevaluator

import scala.collection.mutable
import scala.util.control.Breaks.break
import scala.util.control.Breaks.breakable

/**
 * Logical table, as given in the FROM clause.
 * Multiple table refs may point to the same "physical" Table.
 * This logical table may have been a product of two or more tables.
 */
case class TableRef(alias: String, table: Table) {
  
	// Original table refs, that were joined to produce this logical table.
	private var _joinTrace = Seq[TableRef](this)
	
	// Immutable public view
	def joinTrace: Seq[TableRef] = _joinTrace.toSeq
	
	/**
	 * Is the given column reference contained in this logical table?
	 * I.e. is this logical column's table alias the same as any in the join trace.
	 */
	def contains(colRef: ColumnRef): Boolean = {
		joinTrace.map(_.alias == colRef.tableAlias).fold(false)(_||_)
	}
	
	/**
	 * Filter table data based on a multiplicative list of expressions.
	 * @return new TableRef object with the same alias, but a subset of data tuple,
	 * which satisfy all expressions. 
	 */
	def filter(where: WhereClause): TableRef = {
				
		val resolvableMonads = where.monads(joinTrace)
		
		val newData = table.data.filter { tuple =>
			
			// Apply expressions, fail after first false.
			var passed = true
			breakable {
				for (e <- resolvableMonads) {
					passed = e.eval(tuple, joinTrace)
					if (!passed) break
				}
			}
			passed
		}
		
		TableRef(alias, table.copy(data = newData))
	}

	/**
	 * Join with another logical table. The result table contains the Cartesian
	 * product of the two tables, filtered with the multiplicative list
	 * of resolvable expressions. This table's columns followed by other table's.
	 */
	def joinWith(other: TableRef, where: WhereClause): TableRef = {

		// The name is not significant, just for debugging.
		val newTableName = this.table.name + "*" + other.table.name
		println("*** " + newTableName)
		println("THIS "); println(this.table.metadata)
		println("OTHER "); println(other.table.metadata)
		
		// New metadata is a concat of the two, plus the right side's indices must be shifted.
		// Replace table alias in the eflect the new table name.
		val newMetadata = this.table.metadata joinWith (other.table.metadata, newTableName)

		println("JOIN "); println(newMetadata)

		val resolvableDyads = where.dyads(joinTrace, Seq(other))

		val newData = new mutable.ListBuffer[Array[Any]]
		
		for (
			thisTuple <- this.table.data;
			otherTuple <- other.table.data) {

			// Apply expressions, fail as soon as first false.
			var passed = true
			breakable {
				for (e <- resolvableDyads) {
					passed = e.eval(thisTuple, joinTrace, otherTuple, Seq(other)) 
					if (!passed) break
				}
			}
			if (passed) {
				newData += (thisTuple ++ otherTuple)
			}
		}
		val result = copy(table = table.copy(name = newTableName, metadata = newMetadata, data = newData.toList))
		result._joinTrace = _joinTrace ++ other._joinTrace
		
		result
	}

}