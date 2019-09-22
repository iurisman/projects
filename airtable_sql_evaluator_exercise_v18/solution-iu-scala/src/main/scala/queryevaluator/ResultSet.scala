package queryevaluator

import scala.collection.{mutable, immutable}
import play.api.libs.json._
import java.io.PrintStream
import java.io.ByteArrayOutputStream

/**
 * Mutable result set, used for building the result of a query execution.
 */
class ResultSet(selectList: SelectList, where: WhereClause) {
  
	// Joined data, as a logical table, which encapsulates the joining history trace.
	private[this] var joinedTableRef: Option[TableRef] = None
		
	/**
	 * Join another table with this result set.
	 */
	def joinWith (tableRef: TableRef) {

		// First table goes as is, subsequent tables get joined with it.
		joinedTableRef = joinedTableRef match { 
			case Some(jtf) => Some(jtf.joinWith(tableRef, where))
			case None => Some(tableRef)
		}
	}

	/**
	 * Restrict table to list of columns given in select list.
	 */
	def project() {
		
		joinedTableRef foreach { jtr =>
			
			// Construct new table. New metadata reflects new column indices in the projection.
			val projectedMetadata = new mutable.LinkedHashMap[Table.Column.Name, Table.Column]()
			
			for ((colRef, ix) <- selectList.colRefs zipWithIndex) {
				projectedMetadata += ((colRef.column.name, colRef.column.copy(index = ix))) 
			}
	
			val projectedData = jtr.table.data.map { row =>
				
				val projectedRow = new Array[Any](projectedMetadata.size)
				println(jtr.table.metadata)
				projectedMetadata.values.foreach { col =>
					projectedRow(col.index) = 
						row(jtr.table.columnByName(col.name).get.index)
				}
				projectedRow
			}
			
			val newTable = jtr.table.copy(
					metadata = new Table.Metadata(projectedMetadata), 
					data = projectedData)
					
			joinedTableRef = Some(jtr.copy(table = newTable))
		}
	}
	
	/**
	 * Marshal as JSON into given PrintStream
	 */
	def asJson(ps: PrintStream) {
		
		val header = selectList.colRefs.map(colRef => Json.arr(colRef.columnAlias, colRef.column.datatype))
		ps.append("[\n    ")
		ps.append(Json.toJson(header).toString)

		// If where clause contained a false-valued nilad, we'll have no table here.
		joinedTableRef foreach { jtr =>
			val rows = jtr.table.data.map { line => 
				val jsonCells = line.map { cell => 
					cell match {
						case str: String => JsString(str)
						case int: Int => JsNumber(int)
					}
				}
				Json.toJson(jsonCells)	
			}
		
			// Composing JSON by hand to take advantage of PrintStream's streaming.
			// TODO: Perhaps the json lib we use can do it too???
					
			rows.foreach { row =>
				ps.append(",\n    ")
				ps.append(row.toString)
			}
		}
		
		ps.append("\n]\n")
		ps.flush()
	}

	/**
	 * By default, marshal to stdout.
	 */
	def asJson() { asJson(System.out) }
	
	/**
	 * Marshal into a string. Strictly for testing.
	 */
	def asJsonString(): String = {
		val os: ByteArrayOutputStream = new ByteArrayOutputStream()
		val ps: PrintStream = new PrintStream(os);
		asJson(ps)
		ps.close
		os.toString("UTF8");
	}
	
}