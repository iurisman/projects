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
			
			val col1 = table.byName("name").get
			col1.datatype mustBe Datatype.String
			col1.index mustBe 0
			col1.name mustBe "name"
			
			val col2 = table.byName("age").get
			col2.datatype mustBe Datatype.Integer
			col2.index mustBe 1
			col2.name mustBe "age"
			
			table.byName("foo") mustBe None
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
			
			val col1 = table.byName("name").get
			col1.datatype mustBe Datatype.String
			col1.index mustBe 0
			col1.name mustBe "name"
			
			val col2 = table.byName("country").get
			col2.datatype mustBe Datatype.String
			col2.index mustBe 1
			col2.name mustBe "country"
			
			val col3 = table.byName("population").get
			col3.datatype mustBe Datatype.Integer
			col3.index mustBe 2
			col3.name mustBe "population"

			table.byName("foo") mustBe None

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
			
			val col1 = table.byName("fname").get
			col1.datatype mustBe Datatype.String
			col1.index mustBe 0
			col1.name mustBe "fname"
			
			val col2 = table.byName("lname").get
			col2.datatype mustBe Datatype.String
			col2.index mustBe 1
			col2.name mustBe "lname"
			
			val col3 = table.byName("age").get
			col3.datatype mustBe Datatype.Integer
			col3.index mustBe 2
			col3.name mustBe "age"

			val col4 = table.byName("bdate").get
			col4.datatype mustBe Datatype.String
			col4.index mustBe 3
			col4.name mustBe "bdate"
			
			table.byName("foo") mustBe None
			table.byName("") mustBe None
		}
	}
	
	"Tables" must {
		
		"lookup tables by name" in {
			
			val t1 = Tables.byName("a")
			t1.name mustBe "a"
			
			assertThrows[RuntimeException] {
				Tables.byName("foo")
			}		
		}
	}
}