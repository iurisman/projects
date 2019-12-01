package iu.projects.lcnotes

import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent
import iu.projects.aws.Email
import scala.io.Source
import java.net.URI
import iu.projects.csv.Tokenizer

object LambdaHandler {

	def apply() = new LambdaHandler
}

class LambdaHandler extends RequestHandler[ScheduledEvent, String] {
	
	val emailAddress = "igor.urisman@gmail.com"
	val emailSubject = "Found %s LC Notes out of %s on %s"
   val fileUri = "http://public-resources.lendingclub.com.s3-website-us-west-2.amazonaws.com/SecondaryMarketAllNotes.csv"
   //val fileUri = "file:///Users/Igor/Desktop/SecondaryMarketAllNotes.csv"
   
	def handleRequest(event: ScheduledEvent, context: Context): String = {
		
		val notes = collection.mutable.Set[Vector[String]]()
		
		val lines = Source.fromURL(fileUri).getLines()
		lines.drop(1) // Ignore header

	   var count = 0
		while (lines.hasNext) {
			val row = Tokenizer.parse(lines.next)
			// Status
			if (row(5).toLowerCase() ==  "current") {
				// Asking price
				if (row(6).toFloat > 300) {
					// Markup
					if (row(7).toFloat < 5) {
						// YTM
						val ytm = row(8)
						if (ytm != "--" && ytm.toFloat > 10) {
							// Credit score
							val fico = row(11).split("\\-").map(_.toInt)
							if (fico(0) >= 730) {
								// Never late
								if (row(13).toBoolean) {
									// Loan maturity
									if (row(15).toInt == 60) {
										// Remaining payments 
										if (row(18).toInt <= 42) {
											notes += row
										}
									}
								}
							}
						}
					}
				}
			}
			count += 1
		}
		
		if (notes.size > 0) {
			Email.sendText(emailAddress, emailAddress, emailSubject.format(notes.size, count, event.getTime().toString()), "")
		}

		val response = emailSubject.format(notes.size, count, event.getTime().toString())
		println(response)
		response
	}
}
