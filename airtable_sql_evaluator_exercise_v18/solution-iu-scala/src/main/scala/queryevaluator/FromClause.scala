package queryevaluator

import scala.collection.{mutable, immutable}
	
/**
 * FROM clause of a query
 */
class FromClause(sources: (Table, Option[String])*) {  // (phys tale, opt alias)
		
	// Convert to map keyed by table alias, or own name, if none.
	// This will ensure that aliases don not conflict.
	private[this] val tablesMap = new mutable.LinkedHashMap[String, TableRef]() {
		sources foreach { case (table, optAlias) =>
			val key = optAlias.getOrElse(table.name)
			val tableRef = TableRef(table, optAlias)
			if (getOrElseUpdate(key, tableRef) != tableRef) {
				throw new RuntimeException(s"Duplicate table name ${key}")
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
	 * Lookup a table by name, i.e. by alias or proper name if no alias.
	 */
	def lookupTableRef(name: String): TableRef = {
		tablesMap.get(name).getOrElse {
			throw new RuntimeException(s"Table '$name' does not exist")
		}
	}

	/**
	 * Lookup physical column by column name + optionally table alias.
	 * If no tale alias given, column name must be unique across all tables.
	 */
	def lookupColumn(colName: String, optTableName: Option[String]): (TableRef, Table.Column) = {
		
		optTableName match {
			
			case Some(tableName) =>
				// Fully qualified column name
				val tableRef = lookupTableRef(tableName)
				val column = tableRef.table.byName(colName).getOrElse {
					throw new RuntimeException(s"Column '${tableName}.${colName}' does not exist")
				}
				(tableRef, column)
			case None =>
				// Infer table name				
				val colList = tablesMap.values.flatMap { _.table.byName(colName)  }
				if (colList.size == 0)
					throw new RuntimeException(s"Column '$colName' does not exist")
				else if (colList.size == 1) {
					val column = colList.head
					val tableRef = lookupTableRef(column.table.name)
					(tableRef, column)
				}
				else  {
					val ambiguousTables = colList.map(col => s"'${col.table.name}'").mkString(", ")
					throw new RuntimeException(s"Column name '$colName' is ambiguous: present in ${ambiguousTables}")
				}
		}
	}
}