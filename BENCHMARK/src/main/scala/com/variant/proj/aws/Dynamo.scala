package com.variant.proj.aws

import scala.collection.JavaConverters._

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder
import com.amazonaws.services.dynamodbv2.model.AttributeValue

class Dynamo {

	val dynamoDbClient = AmazonDynamoDBClientBuilder.defaultClient();

	
	def writeItem(table: String, values: Map[String, Any]) {
		
		// Map normal map to that of AttributeValue's
		val attrValuesMap = values.map { case (k,v) => k -> new AttributeValue(v.toString) }
		// Write the item.
	   dynamoDbClient.putItem(table,  attrValuesMap.asJava)
	}
}

/**
 * Testing...
 */
object Test extends App {
	
	val dynamo = new Dynamo

	dynamo.writeItem(
			"benchmark", 
			Map(
					"type_plus_run_id" -> "12-summary",
					"processors" -> 1,
					"parallelism" -> 4
			)
	)
	
	dynamo.writeItem(
			"benchmark", 
			Map(
					"type_plus_run_id" -> "12-op",
					"op" -> "CREATE SESSION",
					"total" -> 4,
					"min" -> 0
			)
	)

}