package com.variant.proj.aws

import scala.concurrent.duration.Duration
import scala.collection.JavaConverters._
import org.slf4j.LoggerFactory

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazonaws.services.sqs.model.Message
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.services.sqs.AmazonSQSClientBuilder

object SQS {
	def apply(url: String)(implicit credentials: (String, String)) = new SQS(url, credentials)
}


class SQS(url: String, credentials: (String,String)) {
  	
	val log = LoggerFactory.getLogger(getClass)
	val awsCreds = new BasicAWSCredentials(credentials._1, credentials._2);

	val sqs = AmazonSQSClientBuilder
		.standard()
		.withCredentials(new AWSStaticCredentialsProvider(awsCreds))
		.build()
	
	def enqueue(msg: String) {
		
		val req = new SendMessageRequest()
	    	.withQueueUrl(url)
   	   .withMessageBody(msg)
		
    	sqs.sendMessage(req)
    
	}

   def dequeue(timeout: Duration): List[Message] = {
				
   	val timeoutMillis = timeout.toMillis
   	val start = System.currentTimeMillis
    	var messages = sqs.receiveMessage(url).getMessages
    	while (messages.size() == 0 && (System.currentTimeMillis() - start) < timeoutMillis) {
    		Thread.sleep(1000)
    		messages = sqs.receiveMessage(url).getMessages
    	}
   	    	
    	messages.asScala.toList
	}

}