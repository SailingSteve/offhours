package main.java.com.podell;

import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;


/**
 * Send a GMail email
 * @author stevepodell
 *
 */
public class SendEmail {
	
	public void sendGMail( String subject, String text ) {
	    
	    Properties props = new Properties();
	    try {
	        //load a properties file from class path, inside static method
	        props.load(getClass().getResourceAsStream("/gmail.properties"));
	    } 
	    catch (Exception ex) {
			System.out.println("ERR: Loading gmail.properties caught: " + ex);
	    }
	    
	    
	    // set any needed mail.smtps.* properties here
	    Session session = Session.getInstance(props);
	    MimeMessage msg = new MimeMessage(session);
		// Set From: header field of the header.
		try {
			msg.setFrom(new InternetAddress(props.getProperty("from")));

			// Set To: header field of the header.
			msg.addRecipient(Message.RecipientType.TO,
	                               new InternetAddress(props.getProperty("to")));
	
			// Set Subject: header field
			msg.setSubject(subject);
	
			// Now set the actual message
			msg.setText(text);
			} catch (Exception e1) {
				System.out.println("ERR: Caught: " + e1);
			}

	    	Transport t;
			try {
				t = session.getTransport("smtps");
			    try {
					t.connect(props.getProperty("host"), 
							  props.getProperty("username"), 
							  props.getProperty("password"));
					t.sendMessage(msg, msg.getAllRecipients());
					System.out.println("Successfully sent a gmail email ... " + subject);
			} catch (MessagingException e) {
				System.out.println("ERR: Caught MessagingException 1: " + e);
			} finally {
				try {
					t.close();
				} catch (MessagingException e) {
					System.out.println("ERR: CaughtMessagingException 2: " + e);
				}
			}
		} catch (NoSuchProviderException e) {
			System.out.println("ERR: Caught NoSuchProviderException: " + e);
		}
	}
}

