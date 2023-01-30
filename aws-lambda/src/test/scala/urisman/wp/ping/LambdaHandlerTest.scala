package urisman.wp.ping

import org.scalatest.wordspec.AnyWordSpec
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent
import java.time.LocalDateTime

class LambdaHandlerTest extends AnyWordSpec {
  val context: Context = new Context {
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
  //event.setTime(new LocalDateTime())

  "LambdaHandler" should {
    "succeed" in {
      val foo = new LambdaHandler().handleRequest(event, context)
      println(foo);
    }
  }
}
