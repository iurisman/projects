package queryevaluator

import scala.collection.mutable

/**
 * "Physical" data tables are referenced through here.
 */
object Tables {
  
	// Tables map keyed by table name.
	private[this] val tableCache = new mutable.HashMap[String, Table]
	
	implicit val tablesDir = "../examples"
	
	/**
	 * How may table do we have in memory
	 */
	def size = tableCache.size
	
	/**
	 * Lookup a table by its name.
	 */
	def byName(tableName: String): Table = {
		
		tableCache.getOrElseUpdate(tableName, Table.fromFile(tableName))
		
	}
}