package iu.projects.lcnotes

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent
import org.joda.time.DateTime

object LocalApp extends App {

	val context =  new Context {
		override def getAwsRequestId = "?"
		override def getLogGroupName = "?"
		override def getLogStreamName = "?"
  		override def getFunctionName = "?"
		override def getFunctionVersion = "?"
      override def getInvokedFunctionArn = "Local"
		override def getIdentity = null
		override def getClientContext = null
		override def getRemainingTimeInMillis = -1
		override def getMemoryLimitInMB = -1
		override def getLogger = null
	}
	
	val event = new ScheduledEvent()
	event.setTime(new DateTime())

	println(LambdaHandler().handleRequest(event, context))
	
}