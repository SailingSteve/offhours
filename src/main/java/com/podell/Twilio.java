package main.java.com.podell;

import com.twilio.sdk.TwilioRestClient;
import com.twilio.sdk.TwilioRestException;
import com.twilio.sdk.resource.factory.MessageFactory;
import com.twilio.sdk.resource.instance.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

/**
 * Send an SMS through the twilio API
 * @author stevepodell
 */
public class Twilio {

	 public void sendSMS( String messageStr ) {
		 
		Properties props = new Properties();
		try {
		    //load a properties file from class path, inside static method
		    props.load(getClass().getResourceAsStream("/twilio.properties"));
		} 
		catch (Exception ex) {
			System.out.println("ERR: Loading twilio.properties caught: " + ex);
		}		    
		 
		TwilioRestClient client = new TwilioRestClient(props.getProperty("sid"), props.getProperty("auth"));

		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("Body", messageStr));
		params.add(new BasicNameValuePair("To", props.getProperty("to")));
		params.add(new BasicNameValuePair("From", props.getProperty("from")));
		MessageFactory messageFactory = client.getAccount().getMessageFactory();
		Message message;
		try {
			message = messageFactory.create(params);
			System.out.println(AmazonBase.logDate() + " ->  Successfully sent a twilio sms to " + props.getProperty("to") + "  \"" + messageStr +
					"\"  (" + message.getSid() + ")");
		} catch (TwilioRestException e) {
			System.out.println("Twilio exception: " + e.getErrorMessage());
		}
	 }
}