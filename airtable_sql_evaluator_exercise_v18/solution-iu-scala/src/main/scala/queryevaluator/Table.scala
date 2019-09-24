package queryevaluator

import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

import scala.collection.immutable
import scala.collection.mutable
import scala.util.control.Breaks.break
import scala.util.control.Breaks.breakable

import Datatype.Datatype
import play.api.libs.json.JsArray
import play.api.libs.json.JsValue
import play.api.libs.json.Json

/**
 * "Physical" in-memory representation of a table. Read only.
 */
object Table {

	/**
	 * Column of a table.
	 */
	case class Column private (name: String, datatype: Datatype, index: Int) {
				
		// Parent link to the containing table is added after the instantiation.
		private[Table] var _table: Table = _
	 	
		lazy val table: Table = _table
	 	
	 	lazy val qualName = table.name + "." + name
	 	
	 	/**
	 	 * Columns are the same if their qualified names are the same.
	 	 * This identity is maintained across join operation.
	 	 */
	 	override def equals(other: Any) = {
			other != null && other.isInstanceOf[Column] && other.asInstanceOf[Column].name == name
		}
	 	
	 	/**
	 	 * Need to override hashCode accordingly
	 	 */
	 	override def hashCode = name.hashCode	
	}
	
		
		/**
		 * Join two metadata objects. A bit of a hack that we need to know the name of the
		 * table for which this metadata is intended, but which is yet to be constructed.
		 *
		def joinWith(other: Metadata, targetTableName: String): Metadata = {
		
			val newMap = 
				columnMap.map { case (name, col) => {
					val newName = name.copy(tableName = targetTableName)
					val newCol = col.copy(name = newName)
					(newName, newCol)
				}} ++
				other.columnMap.map { case (name, col) => {
					val newName = name.copy(tableName = targetTableName)
					val newCol = col.copy(index = col.index + this.table.arity, name = newName)
					(newName, newCol)
				}}
				
			new Metadata(newMap)
		}
		* 
		*/
	
	/**
	 * Unmarshal from file.
	 */
	def fromFile(tableName: String)(implicit tablesDir: String): Table = {

		if (! new File(tablesDir).exists)
			throw new RuntimeException(s"Table folder [${tablesDir}] does not exist")
		
		val tableFile = new File(s"${tablesDir}/${tableName}.table.json")
		
		if (!tableFile.exists)
			throw new RuntimeException(s"Table ${tableName} does not exist")

		fromStream(tableName, new FileInputStream(tableFile))
	}

   /**
	 * Unmarshal from string.
	 */
	def fromString(tableName: String, json: String): Table = {
		fromStream(tableName, new ByteArrayInputStream(json.getBytes()))
	}

	/**
	 * Unmarshal from input stream.
	 */
	def fromStream(tableName: String, is: InputStream): Table = {

		val metadata = new mutable.LinkedHashMap[String, Column]()
		val data = mutable.ListBuffer[Array[Any]]()
		
		val array = Json.parse(is).asInstanceOf[JsArray]
		
		for ((row, rowIx) <- array.value zipWithIndex) {
			
			if (rowIx == 0) {
				// First row is table's metadata
				for ((col, ix) <- row.as[Array[Array[String]]] zipWithIndex) {
					// Use qualified name. 
					val colName = col(0)
					val colType = Datatype.parse(col(1))
					val newElem = Column(colName, colType, ix)
					if (metadata.getOrElseUpdate(colName, newElem) != newElem)
						throw new RuntimeException(s"Duplicate column name ${colName}") 
				}
			}
			
			else {
				// Rest of rows are data tuples.
				val tuple = new mutable.ArrayBuffer[Any]()
				for ((col, colIx) <- row.as[Array[JsValue]] zipWithIndex) {
					val colRef = metadata.valuesIterator.drop(colIx).next
					tuple += (if (colRef.datatype == Datatype.String) col.as[String] else col.as[Int])
				}
				data += tuple.toArray
			}
		}
		
		new Table(tableName, immutable.ListMap(metadata.toSeq:_*), data.toList)
	}

}

/**
 * Table type.
 * @param metadata Immutable linked map of columns, keyed by name, in the ordinal order.
 * @param data Immutable list of tuple arrays indexed concurrent with column indices. 
 * TODO table data and metadata should be private.
 */
class Table private (val name: String, val metadata: immutable.Map[String, Table.Column], val data: List[Array[Any]]) {
  
	import Table._
	
	// add parent link to each column ref
	metadata.values.foreach(_._table = this)
	
	val arity = metadata.size
	
	val cardinality = data.size
	
 	/**
 	 * Tables are the same if their names are the same.
 	 * This identity is maintained across join operation.
 	 */
 	override def equals(other: Any) = {
		other != null && other.isInstanceOf[Table] && other.asInstanceOf[Table].name == name
	}
 	
 	/**
 	 * Need to override hashCode accordingly
 	 */
 	override def hashCode = name.hashCode

 	/**
 	 * Need copy to make logical tables based on "physical"
 	 */
 	def copy(
 			name: String = this.name, 
 			metadata: immutable.Map[String, Table.Column] = this.metadata, 
 			data: List[Array[Any]] = this.data): Table = {
 		
 	 	new Table(name, metadata, data)
 	}
 	
	/**
	 * Lookup a column.
	 */
	def byName(name: String): Option[Column] = metadata.get(name)

	/**
	 */
	override val toString = name
}
