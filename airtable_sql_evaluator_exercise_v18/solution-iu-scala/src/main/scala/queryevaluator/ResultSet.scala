package queryevaluator

import scala.collection.{mutable, immutable}
import play.api.libs.json._
import java.io.PrintStream
import java.io.ByteArrayOutputStream

/**
 * Mutable result set, used for building the result of a query execution.
 * Essentially a lot like a table, but metadata is keyed by column ref.
 */
object ResultSet {
	
	/**
	 * Columns in the result set metadata are referenced by the original column ref, augmented with the index
	 * into the result set's data tuples.
	 */
	case class RsColumn(origColumnRef: ColumnRef, index: Int) 
}

class ResultSet(selectList: SelectList, where: WhereClause) {
  
	import ResultSet._
	
	/**
	 * Columns in the result set don't have to be unique wrt original column ref.
	 * I.e. select x as foo, x as foo from bar is legal.  But we maintain the order.
	 */
	private[this] var metadata = immutable.List[RsColumn]()

	private[this] var data = immutable.List[Array[Any]]()
	
	/**
	 * List of all hot columns in this data set. Column is hot if it occurs in select list or the WHERE clause.
	 * Cold columns are not retained to save space.
	 */
	private[this] val hotColumns: List[ColumnRef] = selectList.columnRefs ++ where.columnRefs.toList
	
	/**
	 * Join another table with this result set.
	 */
	def join (tableRef: TableRef) {

		// Construct the cartesian product of the rows currently in this object and the incoming table's.
		// To save space, filter out incoming tuples that wouldn't satisfy the WHERE clause before
		// computing the cartesian product.

		// Keep old metadata for a bit.
		val oldMetadata = metadata

		// List of hot columns in the incoming table.
		val newColRefs = hotColumns.filter(_.tableRef == tableRef)
		
		// Build the new metadata. 
		val offset = metadata.size
		var index = offset
		val newMetadata = mutable.ListBuffer[RsColumn]()
		newColRefs.foreach { cref =>
			newMetadata += RsColumn(cref, index)
			index += 1
		}
		metadata = metadata ++ newMetadata
		
		// Build the cartesian product, retaining only tuples which satisfy the WHERE clause
		val joinedData = mutable.ListBuffer[Array[Any]]()
		
		// Monadic expressions that can be applied to this table in isolation
		// are applied once per incoming tuple.
		val monads = where.monads(newColRefs)

		for (incomingTuple <- tableRef.table.data) {
			
			if (monads.forall(_.apply(incomingTuple, newColRefs))) {
					
				// If this is the first table in, just copy it
				if (oldMetadata.size == 0) {
					joinedData += incomingTuple
				}
				else for (thisTuple <- this.data) {
					
					// Apply dyadic expressions 
					val dyads = where.dyads(newColRefs, oldMetadata.map(_.origColumnRef))
					if (dyads.forall(_.apply(incomingTuple, oldMetadata, thisTuple, newColRefs))) {
	
						val combinedTuple = thisTuple ++ incomingTuple
						joinedData += combinedTuple
					}
				}				
			}
		}
		
		data = joinedData.toList
	}

	/**
	 * Restrict this result to select list only.
	 */
	def project() = ???
	
	/**
	 * Marshal as JSON into given PrintStream
	 */
	def asJson(ps: PrintStream) {
		
		//def isHot(column: RsColumn) = selectList.contains(column.origColumnRef)
		
		val header = selectList.columnRefs.map(colRef => Json.arr(colRef.nameForDisplay, colRef.column.datatype))
		
		ps.append("[\n    ")
		ps.append(Json.toJson(header).toString)

		val rows = data.map { line => 
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