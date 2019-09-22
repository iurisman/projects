package queryevaluator

/**
 * Column's datatype. Ugly. Real enums are coming in scala 3.
 */
object Datatype extends Enumeration {
	type Datatype = Value
	val String, Integer = Value

	def parse(str: String): Datatype = {
		if ("str".equalsIgnoreCase(str)) String
		else if ("int".equalsIgnoreCase(str)) Integer
		else throw new RuntimeException(s"Unknown column type ${str}")
	}
}

case class Op(token: String)

object Op {
	def parse(op: String): Op = {
		// Assuming clean grammar here.
		Seq(LT, LE, EQ, GE, GT, NE).find(_.token == op).head
	}
	
	val LT = new Op("<")
	val LE = new Op("<=")
	val EQ = new Op("=")
	val GE = new Op(">=")
	val GT = new Op(">")
	val NE = new Op("!=")
}
