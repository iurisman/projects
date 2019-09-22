package queryevaluator

/**
 * Reference to column is how a column is referenced by select list or WHERE expressions.
 * Unique way to ID a column the context of a query.
 */
case class ColumnRef(tableAlias: String, columnAlias: String, column: Table.Column) extends Expression.Term() {
	
	override val datatype = column.datatype
	
	/**
	 * Is this column ref contained in any of the given table refs?
	 */
	def isContainedIn(tableRefs: Seq[TableRef]):Boolean =
		tableRefs.map(_.contains(this)).fold(false)(_||_)

}
