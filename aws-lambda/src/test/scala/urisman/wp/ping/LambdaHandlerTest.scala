package urisman.wp.ping

import org.scalatest.wordspec.AnyWordSpec
import com.amazonaws.services.lambda.runtime.{Context, LambdaLogger}
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent
import org.scalatest.matchers.must.Matchers

import java.time.LocalDateTime
import java.util.logging.Logger

class LambdaHandlerTest extends AnyWordSpec with Matchers {
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
    override def getLogger = new LambdaLogger {
      override def log(message: String): Unit = println(message)
      override def log(message: Array[Byte]): Unit = ???
    }
  }

  "LambdaHandler" should {
    "succeed" in {
      val resp = new LambdaHandler().handleRequest(new ScheduledEvent(), context)
      resp mustBe "urisman.net: OK"
    }
  }
}
