package queryevaluator

import scala.util.Random

import play.api.libs.json.JsArray
import play.api.libs.json.JsNumber
import play.api.libs.json.JsString
import play.api.libs.json.Json
import queryevaluator.Datatype._

object TableDataGenerator {
  
	
	/**
	 * Generate json formatted table data.
	 */
	def generate(cardinality: Int, cols: (String, Datatype)*): String = {
		
		val rand = Random
		val header = cols.map { case (name, colType) => Json.arr(name,  if (colType == Datatype.String) "str" else "int") }
		val data = new Array[JsArray](cardinality + 1)
		data(0) = JsArray(header)

		for (i <- 1 to cardinality) {
			// Generate tuple of random values for the given column types.
			val row = cols.map { case (name, colType) => 
				if (colType == Datatype.String) {
					JsString(rand.alphanumeric.take(8).mkString) 
				}
				else { 
					JsNumber(rand.nextInt) 
				}
			}
			data(i) = JsArray(row)
		}
		
		//Json.prettyPrint(JsArray(data))
		JsArray(data).toString
	}
}