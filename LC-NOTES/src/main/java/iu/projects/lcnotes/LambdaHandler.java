package iu.projects.lcnotes;

import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;

public class LambdaHandler implements RequestHandler<ScheduledEvent, String> {

	/*
	public String myHandler(int anInt) {
        return Main.sendEmail();
    }
    */
	
	public String handleRequest(ScheduledEvent event, Context context) {
		LambdaHandlerScala$.MODULE$.sendEmail();
		return String.format("Email sent %s", event.getTime().toString());
	}
}
