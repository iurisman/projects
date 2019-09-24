package queryevaluator

/**
 * Base column ref is applicable to both SELECT list and the WHERE expressions.
 */
class ColumnRef(val tableRef: TableRef, val column: Table.Column) {
	
	if (tableRef.table != column.table) 
		throw new RuntimeException("Internal Error: Inconsistent column reference")

	private val logicalName = tableRef.alias.getOrElse(tableRef.table.name) + "." + column.name
	
	override def equals(other: Any) = {
		other match {
			case otherRef: ColumnRef => otherRef.logicalName == this.logicalName
			case _ => false
		}
	}
	
	override def hashCode() = logicalName.hashCode
	
	override def toString() = getClass.getSimpleName + "(" + logicalName + ")"

}

/**
 * How a column is referenced by a WHERE expression.
 */
class ExprColumnRef(override val tableRef: TableRef, override val column: Table.Column) 
	extends ColumnRef(tableRef, column) with Expression.Term {
	
	override val datatype = column.datatype

}

/**
 * Column references on the SELECT list (but not on the WHERE clause) may contain an optional alias.
 */
class SelectColumnRef(override val tableRef: TableRef, override val column: Table.Column, alias: Option[String]) 
	extends ColumnRef(tableRef, column) {

	val nameForDisplay = alias.getOrElse(column.name)
	
	override def toString = super.toString() + "(" + alias.getOrElse("") + ")"
}