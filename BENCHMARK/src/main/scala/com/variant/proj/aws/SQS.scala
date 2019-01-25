package com.variant.proj.aws

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import org.slf4j.LoggerFactory

object SQS {
  
	val log = LoggerFactory.getLogger(getClass)

	val sqs = AmazonSQSClientBuilder.defaultClient()
	
	def enqueue(queue: String, msg: String) {
		
		val req = new SendMessageRequest()
    	.withQueueUrl(queue)
      .withMessageBody(msg)
		
    sqs.sendMessage(req)
    log.debug(s"Enqueued message [${msg}]")
    
	}
	
}