package queryevaluator

import Datatype._

/**
 * Encapsulates WHERE clause of a query.
 */
case class WhereClause(private val expressions: Expression*) {

	/**
	 * The list of niladic expressions, i.e. 
	 */
	def nilads() = expressions.filter { _.arity == 0 }
	
	/**
	 * The list of monadic expressions, i.e. resolvable in one logical table:
	 *  • one term is a literal and the other a column in the given table.
	 *  • both terms are a column in the given table.
	 *  NB: if two different aliases refer to the same table, they are still different.
	 */
	def monads(tableRefs: Seq[TableRef]): Seq[Expression] = 
		expressions.filter(_.isResolvableMonad(tableRefs))


	/**
	 * The list of dyadic ops, i.e. resolvable in two logical tables:
	 * • both sides are columns, in two given tables.
	 * Order of parameters does not matter, but they must be different.
	 */
	def dyads(table1Refs: Seq[TableRef], table2Refs: Seq[TableRef]): Seq[Expression] = 
		expressions.filter(_.isResolvableDyad(table1Refs, table2Refs))
}