package urisman.wp.ping

import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent

import java.net.{HttpURLConnection, URL}
import scala.io.Source

class LambdaHandler extends RequestHandler[ScheduledEvent, String] {
	
	val emailAddress = "igor.urisman@gmail.com"
	val emailSubject = "urisman.net: %s"

	def handleRequest(event: ScheduledEvent, context: Context): String = {

		val url = new URL("https://www.urisman.net/")
//		val resp = Source.fromURL(url).getLines.reduceLeft(_+_);
//		println(resp)
//		resp


		val conn = url.openConnection().asInstanceOf[HttpURLConnection]
		conn.setRequestMethod("GET")
		conn.getResponseMessage
	}
}
