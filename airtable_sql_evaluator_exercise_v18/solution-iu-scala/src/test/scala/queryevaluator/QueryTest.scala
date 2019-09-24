package queryevaluator

import org.scalatest.WordSpec
import org.scalatest.MustMatchers
import play.api.libs.json._

class QueryTest extends WordSpec with MustMatchers {
 
	/**
	 * Our own exception intercept
	 */
	def coolIntercept(bloc: =>Unit)(msg: String) {		
			val ex = intercept[RuntimeException] { bloc }			
			if (msg != ex.getMessage) ex.printStackTrace()
			ex.getMessage mustBe msg
	}
	
	"Query" must {
/*
		"detect ambiguous column" in {
			
			coolIntercept {
				Query.fromFile("../examples/error-1.sql.json")
			}("Column name 'name' is ambiguous: present in 'countries', 'cities'")
			
		}

		"detect missing table" in {
			
			intercept[RuntimeException] {
				Query.fromFile("../examples/error-2.sql.json")
			}.getMessage mustBe "Table 'cities' does not exist"
		}

		"detect incompatible types" in {
			
			intercept[RuntimeException] {
				Query.fromFile("../examples/error-3.sql.json")
			}.getMessage mustBe "Incompatible types to '!=': String and Integer"
		}

		"work fast with a negative monad" in {
			
			val qJson = """
{
    "select": [
        {
            "column": {"name": "name", "table": null}
        },
        {
            "column": {"name": "population", "table": null}
        }
    ],
    "from": [
        {
            "source": "cities"
        }
    ],
    "where": [
        {
            "op": "=",
            "left": {"literal": "US"},
            "right": {"literal": "Japan"}
        },
        {
            "op": "=",
            "left": {"column": {"name": "country", "table": null}},
            "right": {"literal": "Japan"}
        },
        {
            "op": ">",
            "left": {"column": {"name": "population", "table": null}},
            "right": {"literal": 8000}
        }
    ]
}
"""
			val rs = Query.fromString(qJson).execute()
			rs.asJson
			Json.stringify(Json.parse(rs.asJsonString)) mustBe """[[["name","String"],["population","Integer"]]]"""
		}

		"load and execute cities-1.sql.json" in {
			
			val rs = Query.fromFile("../examples/cities-1.sql.json").execute
			rs.asJson
			Json.stringify(Json.parse(rs.asJsonString)) mustBe """[[["name","String"],["population","Integer"]],["Tokyo",13513],["Kanagawa",9127],["Osaka",8838]]"""		
		}
*/
		"load and execute cities-2.sql.json" in {
			
			val rs = Query.fromFile("../examples/cities-2.sql.json").execute
			rs.asJson
			Json.stringify(Json.parse(rs.asJsonString)) mustBe """[[["name","String"],["country","String"],["population","Integer"]],["Washington DC","USA",681],["Ottawa","Canada",934],["Tokyo","Japan",13513]]"""
		}
/*
		"load and execute cities-3.sql.json" in {
			
			val rs = Query.fromFile("../examples/cities-3.sql.json").execute
			rs.asJson
			
		}
		* 
		*/
	}
}