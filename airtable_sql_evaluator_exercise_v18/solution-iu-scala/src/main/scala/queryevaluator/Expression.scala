package queryevaluator

import Datatype._
import queryevaluator.ResultSet.RsColumn

/**
 * Represents a normalized binary expression on the WHERE clause
 */
object Expression {

	trait Term {
		val datatype: Datatype.Datatype
	}
	
	abstract class Literal extends Term {
		val value: Any
	}
	
	case class IntLiteral(override val value: Integer) extends Literal {
		override val datatype = Datatype.Integer
	}
	
	case class StringLiteral(override val value: String) extends Literal {
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
	private[this] def eval(lval: String, rval: String) = {
		
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
	private[this] def eval(lval: Int, rval: Int) = {
		
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
	 * Resolve given term over a set column references. Caller guarantees it's resolvable.
	 * @returns (datatype, value)
	 */
	private[this] def resolveTerm(term: Term, columnRefs: Seq[ColumnRef], tuple: Array[Any]): (Datatype, Any) = {
		
		term match {
			case colRef: ExprColumnRef => (colRef.column.datatype, tuple(colRef.column.index))
			case literal: Literal => (literal.datatype, literal.value)
		}
	}
	
	/**
	 * Is this a monadic expression, resolvable in a given set of column references?
	 */
	def isResolvableMondad(columnRefs: List[ColumnRef]): Boolean = {
		arity == 1 &&
		List(lterm, rterm).forall { term => term match {
			case colRef: ExprColumnRef => columnRefs.contains(colRef)
			case _ => true  // literal OK
		}}
		
	}
	
	/**
	 * Is this a dyad resolvable over a given pair of column references? 
	 * The order of arguments do not have to match the term order.
	 */
	def isResolvableDyad(columnRefs1: Seq[ColumnRef], columnRefs2: Seq[ColumnRef]): Boolean = {
		arity == 2 && ( 
		List(lterm, rterm).forall { term => term match {
			case colRef: ExprColumnRef => columnRefs1.contains(colRef)
			case _ => false  // literal not OK
		}} ||
		List(lterm, rterm).forall { term => term match {
			case colRef: ExprColumnRef => columnRefs2.contains(colRef)
			case _ => false  // literal not OK
		}})
	}

	/**
	 * We define arity as the number of unbound, i.e non-literal, terms. 0 through 2 inclusive.
	 */
	val arity = {
		(if (lterm.isInstanceOf[ExprColumnRef]) 1 else 0) + (if (rterm.isInstanceOf[ExprColumnRef]) 1 else 0)
	}

	/**
	 * Apply this niladic expression, i.e. both terms are literals.
	 */
	def apply(): Boolean = {		

		if (arity != 0) throw new RuntimeException(s"Expression of arity ${arity} where 0 was expected") 
		
		rterm.datatype match {
			
			case String =>
				eval(
					lterm.asInstanceOf[Literal].value.asInstanceOf[String],
					rterm.asInstanceOf[Literal].value.asInstanceOf[String])		
						
			case Integer =>
				eval(
					lterm.asInstanceOf[Literal].value.asInstanceOf[Int],
					rterm.asInstanceOf[Literal].value.asInstanceOf[Int])				
		}
	}
	
	/**
	 * Apply this monadic expression. Caller ensures it's resolvable.
	 * The tuple is coming from a real table, i.e. no index translation.
	 */
	def apply(tuple: Array[Any], colRefs: Seq[ColumnRef]): Boolean = {		

		//if (arity != 1) throw new RuntimeException(s"Expression of arity ${arity} where 1 was expected") 

		val List(lrez, rrez) = List(lterm, rterm) map { resolveTerm(_, colRefs, tuple) }
	
		if (rterm.datatype == String) {
			eval(lrez.asInstanceOf[String], rrez.asInstanceOf[String])			
		}
		else {
			eval(lrez.asInstanceOf[Int], rrez.asInstanceOf[Int])				
		}
	}
	
	/**
	 * Apply this dyadic expression. Caller ensures it's resolvable.
	 * Order of arguments does not have to map to the term order. 
	 * @tuple1 is coming from a result set tuple => index translation is needed
	 * @tuple2 is coming from a real table, i.e. no index translation
	 */
	def apply(
			tuple1: Array[Any], rsColumns: Seq[RsColumn], 
			tuple2: Array[Any], colRefs: Seq[ColumnRef]): Boolean = {

		if (arity != 2) throw new RuntimeException(s"Expression of arity ${arity} where 2 was expected") 

		val (lrez, rrez) = {
			
			val lref= lterm.asInstanceOf[ExprColumnRef]
			val rref= rterm.asInstanceOf[ExprColumnRef]

			if (colRefs.contains(lref.tableRef))
				// Look for left term in tuple2
				(tuple2(lref.column.index), tuple1(rsColumns.find(_.origColumnRef == rref).get.index))
				
			else if (colRefs.contains(rref.tableRef))
				// Look for left term in tuple1
				(tuple1(rsColumns.find(_.origColumnRef == lref).get.index), tuple2(rref.column.index))
			
			else 
				throw new RuntimeException("Internal Error: Something's off")
		}
		
		if (lterm.datatype == String) 
			eval(lrez.asInstanceOf[String], rrez.asInstanceOf[String])			
		else
			eval(lrez.asInstanceOf[Int], rrez.asInstanceOf[Int])				
	
	}

}
