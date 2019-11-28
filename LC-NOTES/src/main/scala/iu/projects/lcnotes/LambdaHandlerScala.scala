package iu.projects.lcnotes

import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder
import com.amazonaws.regions.Regions
import com.amazonaws.services.simpleemail.model._

object LambdaHandlerScala {
  
  // Replace sender@example.com with your "From" address.
  // This address must be verified with Amazon SES.
  val FROM = "igor.urisman@gmail.com";

  // Replace recipient@example.com with a "To" address. If your account
  // is still in the sandbox, this address must be verified.
  val TO = "igor.urisman@gmail.com";

  // The configuration set to use for this email. If you do not want to use a
  // configuration set, comment the following variable and the 
  // .withConfigurationSetName(CONFIGSET); argument below.
  //static final String CONFIGSET = "ConfigSet";

  // The subject line for the email.
  val SUBJECT = "Amazon SES test (AWS SDK for Java)";
  
  // The HTML body for the email.
  val HTMLBODY = """<h1>Amazon SES test (AWS SDK for Java)</h1>
    <p>This email was sent with <a href='https://aws.amazon.com/ses/'>
    Amazon SES</a> using the <a href='https://aws.amazon.com/sdk-for-java/'>
    AWS SDK for Java</a>"""

  // The email body for recipients with non-HTML email clients.
  val TEXTBODY = "This email was sent through Amazon SES using the AWS SDK for Java.";

	def sendEmail {

	  val client = 
			  AmazonSimpleEmailServiceClientBuilder.standard()
			  // Replace US_WEST_2 with the AWS Region you're using for
			  // Amazon SES.
			  .withRegion(Regions.US_WEST_2).build();
	  val request = new SendEmailRequest()
			  .withDestination(
				  new Destination()
				  	.withToAddresses(TO))
		  			.withMessage(new Message()
		  			.withBody(new Body()
                    .withHtml(new Content().withCharset("UTF-8").withData(HTMLBODY))
                    .withText(new Content().withCharset("UTF-8").withData(TEXTBODY)))
		  			.withSubject(new Content().withCharset("UTF-8").withData(SUBJECT)))
			  .withSource(FROM);
	  		  // Comment or remove the next line if you are not using a
              // configuration set
              //.withConfigurationSetName(CONFIGSET);
	  client.sendEmail(request);
  }

	
}