package urisman.wp.ping

import com.amazonaws.regions.Regions
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder
import com.amazonaws.services.simpleemail.model.*

/**
 * Send email via SES
 */
object Email {
    
	/**
	 * Send text email
	 */
	def sendText(to: String, from: String, subject: String, body: String) = {

		val client =
			AmazonSimpleEmailServiceClientBuilder.standard()
      // Replace US_WEST_2 with the AWS Region you're using for
      // Amazon SES.
			.withRegion(Regions.US_WEST_2).build();

		val request = new SendEmailRequest()
			.withDestination(new Destination().withToAddresses(to))
			.withMessage(
				new Message()
					.withBody(
						new Body()
							.withText(new Content().withCharset("UTF-8").withData(body))
					)
					.withSubject(new Content().withCharset("UTF-8").withData(subject))
			)
			.withSource(from)
		client.sendEmail(request);
	}

	/**
	 * Send HTML email
	 */
	def sendHtml(to: String, from: String, subject: String, body: String) = {

		val client =
			AmazonSimpleEmailServiceClientBuilder.standard()
      // Replace US_WEST_2 with the AWS Region you're using for
      // Amazon SES.
			.withRegion(Regions.US_WEST_2).build();

		val request = new SendEmailRequest()
			.withDestination(
				new Destination().withToAddresses(to))
			.withMessage(new Message()
				.withBody(
					new Body()
					.withHtml(new Content().withCharset("UTF-8").withData(body))
				)
				.withSubject(new Content().withCharset("UTF-8").withData(subject)))
			.withSource(from);

		client.sendEmail(request);
	}
	
}