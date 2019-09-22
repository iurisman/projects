package queryevaluator

import scala.collection.{mutable, immutable}
	
/**
 * 
 */
class FromClause(sources: (String, Option[String])*) {
		
	// Convert to map keyed by the alias, or own name, if no alias.
	// This will ensure that aliases don not conflict.
	private[this] val tablesMap = new mutable.LinkedHashMap[String, TableRef]() {
		sources.map { case (name, optAlias) =>
			// the actual table
			val table = Tables.lookupTable(name)
			val alias = optAlias.getOrElse(name)
			val tableRef = TableRef(alias, table)
			if (getOrElseUpdate(alias, tableRef) != tableRef) {
				throw new RuntimeException("Duplicate table name ${alias}")
			}
		}
	}
	
	/**
	 * All table refs in ordinal order
	 */
	def tables: Seq[TableRef] = {
		tablesMap.values.toSeq
	}
	
	/**
	 * Lookup a table by name.
	 */
	def lookupTable(name: String): TableRef = {
		tablesMap.get(name).getOrElse {
			throw new RuntimeException(s"Table '$name' does not exist")
		}
	}

	/**
	 * Lookup a data column by column alias + optionally table alias.
	 * If no tale have given, column name must be unique across all tables.
	 */
	def lookupColumn(colName: String, optTableName: Option[String]): Table.Column = {
		
		optTableName match {
			
			case Some(tableName) =>
				// Fully qualified column name
				lookupTable(tableName).table.columnByShortName(colName).getOrElse {
					throw new RuntimeException(s"Column '${tableName}.${colName}' does not exist")
				}
			case None =>
				// Infer table name
				val colList = tablesMap.values.flatMap { _.table.columnByShortName(colName)  }
				if (colList.size == 0)
					throw new RuntimeException(s"Column '$colName' does not exist")
				else if (colList.size == 1)
					colList.head
				else  {
					val ambiguousTables = colList.map(col => s"'${col.table.name}'").mkString(", ")
					throw new RuntimeException(s"Column name '$colName' is ambiguous: present in ${ambiguousTables}")
				}
		}
	}
}