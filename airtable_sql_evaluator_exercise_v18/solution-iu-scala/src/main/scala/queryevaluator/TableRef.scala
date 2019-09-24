package queryevaluator

import scala.collection.mutable
import scala.util.control.Breaks.break
import scala.util.control.Breaks.breakable

/**
 * Logical table, as given in the FROM clause.
 * Multiple table refs may point to the same "physical" Table, but at least one must supply an alias.
 */
case class TableRef(table: Table, alias: Option[String])