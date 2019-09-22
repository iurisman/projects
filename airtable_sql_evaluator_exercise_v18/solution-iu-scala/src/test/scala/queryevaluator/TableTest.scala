package queryevaluator

import org.scalatest.WordSpec
import org.scalatest.MustMatchers

class TableTest extends WordSpec with MustMatchers {
  
	implicit val tablesDir = "../examples"

	"Table a" must {
		
		"load from file" in {
			
			val table = Table.fromFile("a")
			
			table.name mustBe "a"
			table.arity mustBe 2
			table.cardinality mustBe 3
			
			val col1 = table.columnByShortName("name").get
			col1.datatype mustBe Datatype.String
			col1.index mustBe 0
			col1.name mustBe Table.Column.Name("a","name")
			
			val col2 = table.columnByShortName("age").get
			col2.datatype mustBe Datatype.Integer
			col2.index mustBe 1
			col2.name mustBe Table.Column.Name("a","age")
			
			table.columnByShortName("foo") mustBe None
		}
	}
	
	"Table b" must {
		
		"load from file" in {
			
			val table = Table.fromFile("b")
			
			table.name mustBe "b"
			table.arity mustBe 2
			table.cardinality mustBe 2
		}
	}

	"Table cities" must {
		
		"load from file" in {
			
			val table = Table.fromFile("cities")
			
			table.arity mustBe 3
			table.cardinality mustBe 16
			table.name mustBe "cities"
			
			val col1 = table.columnByShortName("name").get
			table.columnByName(Table.Column.Name("cities", "name")).get mustBe col1
			col1.datatype mustBe Datatype.String
			col1.index mustBe 0
			col1.name mustBe Table.Column.Name("cities", "name")
			
			val col2 = table.columnByShortName("country").get
			table.columnByName(Table.Column.Name("cities", "country")).get mustBe col2
			col2.datatype mustBe Datatype.String
			col2.index mustBe 1
			col2.name mustBe Table.Column.Name("cities", "country")
			
			val col3 = table.columnByShortName("population").get
			table.columnByName(Table.Column.Name("cities", "population")).get mustBe col3
			col3.datatype mustBe Datatype.Integer
			col3.index mustBe 2
			col3.name mustBe Table.Column.Name("cities", "population")

			table.columnByShortName("foo") mustBe None

		}
	}

	"Table countries" must {
		
		"load from file" in {
			
			val table = Table.fromFile("countries")
			
			table.arity mustBe 2
			table.cardinality mustBe 4
			table.name mustBe "countries"
		}
	}

	"Table big" must {
		
		"load from string" in {
			val json = TableDataGenerator.generate(12345, ("fname", Datatype.String), ("lname", Datatype.String), ("age", Datatype.Integer), ("bdate", Datatype.String))
			//println(json)
			val table = Table.fromString("big", json)
			
			table.arity mustBe 4
			table.cardinality mustBe 12345
			table.name mustBe "big"
			
			val col1 = table.columnByShortName("fname").get
			table.columnByName(Table.Column.Name("big", "fname")).get mustBe col1
			col1.datatype mustBe Datatype.String
			col1.index mustBe 0
			col1.name mustBe Table.Column.Name("big", "fname")
			
			val col2 = table.columnByShortName("lname").get
			table.columnByName(Table.Column.Name("big", "lname")).get mustBe col2
			col2.datatype mustBe Datatype.String
			col2.index mustBe 1
			col2.name mustBe Table.Column.Name("big", "lname")
			
			val col3 = table.columnByShortName("age").get
			table.columnByName(Table.Column.Name("big", "age")).get mustBe col3
			col3.datatype mustBe Datatype.Integer
			col3.index mustBe 2
			col3.name mustBe Table.Column.Name("big", "age")

			val col4 = table.columnByShortName("bdate").get
			table.columnByName(Table.Column.Name("big", "bdate")).get mustBe col4
			col4.datatype mustBe Datatype.String
			col4.index mustBe 3
			col4.name mustBe Table.Column.Name("big", "bdate")
			
			table.columnByShortName("foo") mustBe None
			table.columnByShortName("") mustBe None
		}
	}
	
	"Tables" must {
		
		"lookup columns" in {
			
			val t1 = Tables.lookupTable("a")
			t1.name mustBe "a"
			
			assertThrows[RuntimeException] {
				Tables.lookupTable("foo")
			}		
		}
	}
}