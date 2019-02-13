package com.variant.proj.aws

import scala.collection.JavaConverters._


import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder
import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.variant.proj.aws.AWS._

object Dynamo {
	def apply(implicit credentials: (String,String)) = new Dynamo(credentials)
}

class Dynamo(credentials: (String,String)) {

	val awsCreds = new BasicAWSCredentials(credentials._1, credentials._2);
	val dynamoDbClient = AmazonDynamoDBClientBuilder
		.standard()
		.withCredentials(new AWSStaticCredentialsProvider(awsCreds))
		.build()

	
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
	

	val dynamo = Dynamo(implicitly)

	dynamo.writeItem(
			"benchmark", 
			Map(
					"key" -> "testing",
					"foo" -> 1,
					"bar" -> "bar"
			)
	)
	
}