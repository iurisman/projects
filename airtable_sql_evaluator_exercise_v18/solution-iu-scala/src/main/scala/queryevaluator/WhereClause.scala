package queryevaluator

import scala.collection.{mutable, immutable}
import Datatype._

/**
 * Encapsulates WHERE clause of a query.
 */
class WhereClause(expressions: immutable.List[Expression]) {

	/**
	 * Is any of the expressions references this column ref?
	 *
	def contains(columnRef: ColumnRef) = {
		expressions.map { exp =>
			exp.lterm.isInstanceOf[ColumnRef] && exp.lterm.asInstanceOf[ColumnRef] == columnRef ||
			exp.rterm.isInstanceOf[ColumnRef] && exp.rterm.asInstanceOf[ColumnRef] == columnRef		
		}.fold(false)(_||_)
	}*/
		
	/**
	 * Unique column refs, i.e. if a column ref is used multiple times, include it only once.
	 */
	val columnRefs: Set[ColumnRef] = {
		val result = mutable.HashSet[ColumnRef]()
		expressions.foreach { exp => 
			List(exp.lterm, exp.rterm).foreach { term =>
				term match { 
					case colRef: ExprColumnRef => result += colRef 
					case _ =>
				} 
			}
		}
		result.toSet
	}

	/**
	 * Unique column refs which reference given table ref.
	 */
	//def columnRefs(tableRef: TableRef): Set[ColumnRef] = columnRefs.filter ( _.tableRef == tableRef )
	
	/**
	 * The list of niladic expressions, i.e. with both terms literal.
	 * We want to apply these only once.
	 */
	def nilads() = expressions.filter { _.arity == 0 }
	
	/**
	 * The list of monadic expressions, i.e. resolvable in a single table:
	 *  • one term is a literal and the other a column in the given table.
	 *  • both terms are a column in the given table.
	 */
	def monads(columnRefs: Seq[ColumnRef]): Seq[Expression] = 
		expressions.filter { expr => 
			expr.arity == 1 && 
			List(expr.lterm, expr.rterm).filter(term => columnRefs.contains(term)).size > 0
		}


	/**
	 * The list of dyadic ops, i.e. resolvable in two logical tables:
	 * • both sides are columns, in two given tables.
	 * Order of parameters does not matter, but they must be different.
	 */
	def dyads(columnRefs1: Seq[ColumnRef], columnRefs2: Seq[ColumnRef]): Seq[Expression] = 
		expressions.filter { expr => 
			expr.arity == 2 && 
			List(expr.lterm, expr.rterm).filter(term => columnRefs1.contains(term) || columnRefs2.contains(term)).size > 0
		}
}