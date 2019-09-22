package queryevaluator

import scala.collection.mutable

/**
 * All data tables are referenced through here.
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
	 * Lookup a column reference at runtime.
	 * Throws if table or column does not exists.
	 */
	def lookupTable(tableName: String): Table = {
		
		tableCache.getOrElseUpdate(tableName, Table.fromFile(tableName))
		
	}
}