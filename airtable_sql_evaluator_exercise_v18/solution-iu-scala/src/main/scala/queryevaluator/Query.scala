package queryevaluator

import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

import scala.collection.mutable

import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.libs.json.JsValue
import play.api.libs.json.Json


import Expression._

/**
 * In-memory representation of a query.
 */
object Query {

	/**
	 * Parse an expression term
	 */
	private def parseTerm(json: JsValue, from: FromClause): Term = {
		
		(json \ "column").asOpt[JsObject] match {
			
		case Some(col) => 
			// This is a column ref. 
			val colName = (col \ "name").as[String]
			val optTableAlias = (col \ "table").asOpt[String]
			val (tableRef, column) = from.lookupColumn(colName, optTableAlias)
			new ExprColumnRef(tableRef, column)
				
		case None =>
			// This is a literal.
			val literal = (json \ "literal").get
			if (literal.isInstanceOf[JsString]) StringLiteral(literal.as[String])
			else IntLiteral(literal.as[Int])
		}
	}

	/**
	 * Read query from disk.
	 */
	def fromFile(file: String): Query = {

		val qfile = new File(file)
		if (! qfile.exists )
			throw new RuntimeException(s"Query file [${file}] does not exist")
		
		fromStream(new FileInputStream(qfile))
	}

   /**
	 * Read query from a string.
	 */
	def fromString(json: String): Query = {
		fromStream(new ByteArrayInputStream(json.getBytes()))
	}

	/**
	 * Read query from input stream.
	 */
	def fromStream(is: InputStream): Query = {
		
		val json = Json.parse(is)
		val select = (json \ "select").as[Array[JsValue]]
		val from   = (json \ "from").as[Array[JsValue]]
		val where  = (json \ "where").as[Array[JsValue]]
		
		/*
		 * Parse FROM clause first
		 */
		val sources = new mutable.ListBuffer[(Table, Option[String])]
		for (source <- from) {
			val name = (source \ "source").as[String]
			val optAlias = (source \ "as").asOpt[String]
			
			sources += ((Tables.byName(name), optAlias))
		}
		val fromClause = new FromClause(sources:_*)

		/*
		 * Parse SELECT list
		 */
		val colRefs = new mutable.ListBuffer[SelectColumnRef]()
		for (colJsValue <- select) {

			val colName = (colJsValue \ "column" \ "name").as[String]
			val optTableAlias = (colJsValue \ "column" \ "table").asOpt[String]
			val optColAlias = (colJsValue \ "as").asOpt[String]
			
			// Cross reference this information with the FROM clause
			val (tableRef, column) = fromClause.lookupColumn(colName, optTableAlias)
						
			colRefs +=  new SelectColumnRef(tableRef, column, optColAlias)
		}
		val selectList = new SelectList(colRefs.toList)
		
		/*
		 * Parse WHERE clause
		 */
		val expressions = new mutable.ListBuffer[Expression]
		for (expr <- where) {
			val op = Op.parse((expr \ "op").as[String])
			val lterm = parseTerm((expr \ "left").get, fromClause)
			val rterm = parseTerm((expr \ "right").get, fromClause)
			expressions += new Expression(lterm, op, rterm)
		}
		val whereClause = new WhereClause(expressions.toSet)
		
		new Query(selectList, fromClause, whereClause)
	}

	
}

/**
 * Table type
 */
class Query private (selectList: SelectList, from: FromClause, where: WhereClause) {
  	
	def execute(): ResultSet = {
		
		val result = new ResultSet(selectList, where)
		
		// Apply all fully bound expressions once here. If any returns false we're done.
		if ( where.nilads.foldLeft(true)(_ && _.apply()) ) {

			// Add FROM tables to the result set, filtering each table with expressions
			// resolvable in that table.
			from.tables.foreach { tableRef => result.join(tableRef) }
		}

		result
	}
}

