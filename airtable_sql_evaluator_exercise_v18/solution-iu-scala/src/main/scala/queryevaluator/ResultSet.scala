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
	private[this] val metadata = mutable.LinkedHashSet[RsColumn]()

	private[this] val data = mutable.ListBuffer[Array[Any]]()
	
	/**
	 * List of all hot columns in this data set. Column is hot if it occurs in select list or the WHERE clause.
	 * Cold columns are not retained to save space.
	 */
	private[this] val hotColumns: immutable.Set[ColumnRef] = {
		val result = mutable.LinkedHashSet[ColumnRef]()
  		result ++= selectList.columnRefs
	   result ++= where.columnRefs.toList
	   result.toSet
	}
		
	/**
	 * Join another table with this result set, retaining only hot columns,
	 * i.e. those on the select list or the WHERE clause.
	 */
	def join (tableRef: TableRef) {

		// Construct the cartesian product of the rows currently in this object and the incoming table's.
		// To save space, filter out incoming tuples that wouldn't satisfy the WHERE clause before
		// computing the cartesian product.

		// Keep a copy of old metadata for a bit.
		val oldMetadata = metadata.toSet

		// List of hot columns in the incoming table.
		val newColRefs = hotColumns.filter(_.tableRef == tableRef)
		
		// Build the new metadata. 
		val offset = metadata.size
		var index = offset
		val hotMetadata = mutable.ListBuffer[RsColumn]()
		newColRefs.foreach { cref =>
			hotMetadata += RsColumn(cref, index)
			index += 1
		}
		metadata ++= hotMetadata
		
		// Build the cartesian product, retaining only tuples which satisfy the WHERE clause
		val joinedData = mutable.ListBuffer[Array[Any]]()
		
		// Monadic expressions that can be applied to this table in isolation
		// are applied once per incoming tuple.
		val monads = where.monads(newColRefs)

		for (incomingTuple <- tableRef.table.data) {
			
			// Project the incoming tuple on the new metadata, i.e. lose cold columns.
			val hotIncomingTuple = new Array[Any](hotMetadata.size)
			hotMetadata.foreach { rsCol => hotIncomingTuple(rsCol.index) = incomingTuple(rsCol.origColumnRef.column.index) }
			
			
			if (monads.forall(_.apply(incomingTuple, newColRefs))) {
					
				// If this is the first table in, just copy it
				if (oldMetadata.size == 0) {
					joinedData += hotIncomingTuple
				}
				else for (thisTuple <- this.data) {
					
					// Apply dyadic expressions. Use the incoming tuple because that's the domain or column
					// references in the WHERE expressions.
					val dyads = where.dyads(newColRefs, oldMetadata.map(_.origColumnRef))
					if (dyads.forall(_.apply(incomingTuple, oldMetadata, thisTuple, newColRefs))) {
						 
						val combinedTuple = thisTuple ++ hotIncomingTuple
						joinedData += combinedTuple
					}
				}				
			}
		}
		
		data ++=  joinedData
	}

	/**
	 * TODO: Reduce memory footprint by dropping all columns outside of select list.
	 */
	def project() { }
	
	/**
	 * Marshal as JSON into given PrintStream
	 */
	def asJson(ps: PrintStream) {
		
		//def isHot(column: RsColumn) = selectList.contains(column.origColumnRef)
		
		val header = selectList.columnRefs.map(colRef => Json.arr(colRef.nameForDisplay, colRef.column.datatype))
		
		ps.append("[\n    ")
		ps.append(Json.toJson(header).toString)

		// The sublist of metadata with only columns on select list.
		val projectedMetadata = metadata.filter { col => 
			col.origColumnRef.isInstanceOf[SelectColumnRef] &&
			selectList.contains(col.origColumnRef) 
		}
		println("\n*** metadata\n" + metadata.mkString("\n"))
		println("\n*** projected\n" + projectedMetadata.mkString("\n"))

		val rows = data.map { line =>
			val jsonCells = projectedMetadata.map { rsColumn =>
				line(rsColumn.index) match {
						case str: String => JsString(str)
						case int: Int => JsNumber(int)
					}
				}
				Json.toJson(jsonCells)	
		}
		
	
		// Composing JSON by hand to take advantage of PrintStream's streaming.
		// TODO: Perhaps the json lib we use can do it too???
		println("*** " + rows.size)
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