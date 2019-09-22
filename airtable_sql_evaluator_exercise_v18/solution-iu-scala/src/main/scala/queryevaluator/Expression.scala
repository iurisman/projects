package queryevaluator

import Datatype._

/**
 * Represents a normalized binary expression on the WHERE clause
 */
object Expression {

	abstract class Term(val value: Any) {
		val datatype: Datatype.Datatype
	}
	
	abstract class Literal(value: Any) extends Term(value)
	
	case class IntLiteral(override val value: Integer) extends Literal(value) {
		override val datatype = Datatype.Integer
	}
	
	case class StringLiteral(override val value: String) extends Literal(value) {
		override val datatype = Datatype.String
	}
}

class Expression(val lterm: Expression.Term, val op: Op, val rterm: Expression.Term) {
	
	import Expression._
	
	if (lterm.datatype != rterm.datatype) {
		throw new RuntimeException(s"Incompatible types to '${op.token}': ${lterm.datatype} and ${rterm.datatype}")
	}
	
	/**
	 * Actual evaluation of substituted string values.
	 */
	private[this] def evalWithArgs(lval: String, rval: String) = {
		
		(op: @unchecked) match {
			case Op.LT => lval < rval
			case Op.LE => lval <= rval
			case Op.EQ => lval == rval
			case Op.GE => lval >= rval
			case Op.GT => lval > rval
			case Op.NE => lval != rval
		}			
	}
	
	/**
	 * Actual evaluation of substituted int values.
	 */
	private[this] def evalWithArgs(lval: Int, rval: Int) = {
		
		(op: @unchecked) match {
			case Op.LT => lval < rval
			case Op.LE => lval <= rval
			case Op.EQ => lval == rval
			case Op.GE => lval >= rval
			case Op.GT => lval > rval
			case Op.NE => lval != rval
		}			
	}
		
	/**
	 * Resolve given term over a set of logical tables.
	 * @returns the literal or the column ref), wrapped in Some, or None if unresolvable.
	 */
	private[this] def resolveTerm(term: Term, tableRefs: Seq[TableRef]): Option[Any] = {
		
		if (term.isInstanceOf[Literal]) {
			Some(term.value)
		}
		else if (term.asInstanceOf[ColumnRef].isContainedIn(tableRefs)) {
			Some(term.asInstanceOf[ColumnRef].column)
		}
		else None
	}

	/**
	 * Is this a monad resolvable over given set of logical tables?
	 */
	def isResolvableMonad(tableRefs: Seq[TableRef]): Boolean = {
		arity == 1 && 
		resolveTerm(lterm, tableRefs).isDefined && 
		resolveTerm(rterm, tableRefs).isDefined
	}
	
	/**
	 * Is this a dyad resolvable over given sets of logical tables? The order of arguments
	 * do not have to match the term order.
	 */
	def isResolvableDyad(table1Refs: Seq[TableRef], table2Refs: Seq[TableRef]): Boolean = {
		arity == 2 && 
		(resolveTerm(lterm, table1Refs).isDefined && resolveTerm(rterm, table2Refs).isDefined ||		
		resolveTerm(rterm, table1Refs).isDefined && resolveTerm(lterm, table2Refs).isDefined)
	}

	/**
	 * We define arity as the number of unbound, i.e non-literal, terms. 0 through 2 inclusive.
	 */
	val arity = {
		(if (lterm.isInstanceOf[ColumnRef]) 1 else 0) + (if (rterm.isInstanceOf[ColumnRef]) 1 else 0)
	}

	/**
	 * Evaluate this niladic expression, i.e. both terms are literals.
	 */
	def eval(): Boolean = {		

		if (arity != 0) throw new RuntimeException(s"Expression of arity ${arity} where 0 was expected") 
		
		if (rterm.datatype == String) {
			evalWithArgs(
					lterm.asInstanceOf[Literal].value.asInstanceOf[String],
					rterm.asInstanceOf[Literal].value.asInstanceOf[String])			
		}
		else {
			evalWithArgs(
					lterm.asInstanceOf[Literal].value.asInstanceOf[Int],
					rterm.asInstanceOf[Literal].value.asInstanceOf[Int])				
		}			
	}
	
	/**
	 * Evaluate this monadic expression. Caller ensures it's resolvable.
	 */
	def eval(tuple: Array[Any], tableRefs: Seq[TableRef]): Boolean = {		

		if (arity != 1) throw new RuntimeException(s"Expression of arity ${arity} where 1 was expected") 

		val List(lrez, rrez) = List(lterm, rterm) map { term =>
			resolveTerm(term, tableRefs).map { rez => 
				if (rez.isInstanceOf[Table.Column]) tuple(rez.asInstanceOf[Table.Column].index)
				else rez
			}.get
		}

		if (rterm.datatype == String) {
			evalWithArgs(lrez.asInstanceOf[String], rrez.asInstanceOf[String])			
		}
		else {
			evalWithArgs(lrez.asInstanceOf[Int], rrez.asInstanceOf[Int])				
		}
	}
	
	/**
	 * Evaluate this dyadic expression. Caller ensures it's resolvable.
	 * Order of arguments does not have to map to the term order. Safe to assume
	 * that both terms are table refs.
	 */
	def eval(
			tuple1: Array[Any], table1Refs: Seq[TableRef],
			tuple2: Array[Any], table2Refs: Seq[TableRef]): Boolean = {

		if (arity != 2) throw new RuntimeException(s"Expression of arity ${arity} where 2 was expected") 

		val (lrez, rrez) = {
			
			val lref= lterm.asInstanceOf[ColumnRef]
			val rref= rterm.asInstanceOf[ColumnRef]
			
			if (op == Op.GT) 
				println(lref + " " + op + " " + rref)
			
				if (lref.isContainedIn(table1Refs))
				(tuple1(lref.column.index), tuple2(rref.column.index))
			else
				(tuple2(lref.column.index), tuple1(rref.column.index))
		}
		
		if (lterm.datatype == String) 
			evalWithArgs(lrez.asInstanceOf[String], rrez.asInstanceOf[String])			
		else
			evalWithArgs(lrez.asInstanceOf[Int], rrez.asInstanceOf[Int])				
	
	}

}
