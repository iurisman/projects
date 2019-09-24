package queryevaluator

/**
 * Base column ref is applicable to both SELECT list and the WHERE expressions.
 */
abstract class ColumnRef(val tableRef: TableRef, val column: Table.Column) {
	
	if (tableRef.table != column.table) 
		throw new RuntimeException("Internal Error: Inconsistent column reference")
}

/**
 * How a column is referenced by a WHERE expression.
 */
case class ExprColumnRef(override val tableRef: TableRef, override val column: Table.Column) extends ColumnRef(tableRef, column) with Expression.Term {
	override val datatype = column.datatype
	
	/**
	 * Override the default equals to match SelectCloumnRef.equals
	 * Case classes don't inherit, so have to do it by hand.
	 */
	override def equals(other: Any) = {
		other match {
			case scr: SelectColumnRef => super.equals(ExprColumnRef(scr.tableRef, scr.column))
			case ecr: ExprColumnRef =>  super.equals(ecr)
			case _ => false
		}
	}

}

/**
 * Column references on the SELECT list (but not on the WHERE clause) may contain an optional alias.
 */
case class SelectColumnRef(override val tableRef: TableRef, override val column: Table.Column, alias: Option[String]) extends ColumnRef(tableRef, column) {

	val nameForDisplay = alias.getOrElse(column.name)
	
	/**
	 * Override the default equals so that alias is excluded from comparison
	 * by delegating to SelectColumnRef. 
	 * Case classes don't inherit, so have to do it by hand.
	 */
	override def equals(other: Any) = {
		other match {
			case scr: SelectColumnRef => ExprColumnRef(tableRef, column).equals(ExprColumnRef(scr.tableRef, scr.column))
			case ecr: ExprColumnRef =>  ExprColumnRef(tableRef, column).equals(ecr)
			case _ => false
		}
	}
	
	override def hashCode = ExprColumnRef(tableRef, column).hashCode

}