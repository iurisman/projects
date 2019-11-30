package iu.projects.csv

object Tokenizer {
  
	def parse(line: String): Vector[String] = {
		
		val result = new collection.immutable.VectorBuilder[String]
		val token = new StringBuilder
		var inQuotes = false
		
		for (c <- line.toCharArray()) {
			c match {
				case '"' => {
					inQuotes = !inQuotes
				}
				case ',' => {
					if (inQuotes) token += c 
					else {
						result += token.toString
						token.clear()
					}
				}
				case '\n' => 
				case _   => {
					if (inQuotes) token += c
					else throw new Exception(s"Barewood char '$c'. This tokenizer assumes all tokens to be quoted.") 
				}
			}
		}
		result.result
	}
}