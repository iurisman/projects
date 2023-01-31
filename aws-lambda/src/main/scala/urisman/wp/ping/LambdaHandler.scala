package urisman.wp.ping

import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent

import java.net.{HttpURLConnection, URL}
import scala.io.Source

class LambdaHandler extends RequestHandler[ScheduledEvent, String] {
	
	private val to = "igor.urisman@gmail.com"
	private def subject(status: Int): String = status match {
		case 200 => "urisman.net: OK"
		case _@code => s"*** urisman.net: $code ***"
	}

	def handleRequest(event: ScheduledEvent, context: Context): String = {
		val url = new URL("https://www.urisman.net/")
		val conn = url.openConnection().asInstanceOf[HttpURLConnection]
		conn.setRequestMethod("GET")
		val subj = subject(conn.getResponseCode)
		Email.sendText(to = to, from = to, subject = subj, body = "")
		context.getLogger.log(subj)
		subj
	}
}
