package main.java.com.podell;

import java.util.Arrays;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

/**
 * Monitor and alert of off hours usage of Minecraft
 * Installed on each of the two Macs that my kids use
 * 
 * @author stevepodell
 */
public class OffHoursMonitor extends AmazonBase {

	private static Pattern pattern = Pattern.compile("(\\d+)");

  	private static PlayDay[] playDays = new PlayDay[] {
  		new PlayDay(Calendar.SUNDAY, 6, 22),
 		new PlayDay(Calendar.FRIDAY, 15, 22),
		new PlayDay(Calendar.SATURDAY, 6, 22)
  	};
	
  	private Calendar calLastLog = null;

  	// Constructor
  	OffHoursMonitor(String computer) {
  		super(playDays);  
  		Calendar calEmailSent = null;
  		 	
  		while( true ) {
  			boolean offHours = isOffHours();

  			String pid = getMineCraftJavaPid();
  			calLastLog = logEvery( calLastLog, 5, AmazonBase.logDate() + " ->  offHours = " + offHours + (String)(( pid.isEmpty() ) ? "" : ", pid = " + pid ));
 

  			if( ( ! pid.isEmpty() ) && ( offHours ) ) {
   				//killPid(pid);
				//warning();
  				if( calEmailSent == null ) {
  					calEmailSent = getNowPST();
  					new SendEmail().sendGMail("Minecraft is running on " + computer + ".  First detection (" + pid + ")", 
  							"Detected by the OffHoursMonitor Java program. First detected.  (" + pid + ")" );
  					new Twilio().sendSMS("Minecraft is running on " + computer + ".  First detection (" + pid + ")");
  				    s3logger("Minecraft is running on " + computer + ".  First detection by the OffHoursMonitor Java program. (" + pid + ")" );
  				} else if( minutesSince(calEmailSent) > 5 ) {
  					calEmailSent = getNowPST();
  					new SendEmail().sendGMail("Minecraft is running on " + computer + ".  5 minute follow up (" + pid + ")", 
  							"Detected by the OffHoursMonitor Java program.  5 minute follow up.  (" + pid + ")" );
  				}
  			} else {
  				calEmailSent = null;
  			}
 	  
  			try {
  				Thread.sleep( offHours ? 5000 : 300000 );
  			} catch (InterruptedException e) {}
  		}
 
  	}
  	
  	private Calendar logEvery( Calendar cal, int min, String line )
  	{
		if( cal == null ) {
			cal = getNowPST();
			System.out.println(line);
		}
		else if( minutesSince(cal) > min ) {
			System.out.println(line);
			cal = getNowPST();
		}			
		return cal;
  	}

    /**
     * Get the pid of the running minecraft java process
     * @return the pid as a string
     */
	private static String getMineCraftJavaPid() {
		BufferedReader reader = null;
		try {
			String[] cmd = { "/bin/bash", "-c", "ps -ax | grep -i minecraft | grep JavaVirtualMachines" };

			Process p = Runtime.getRuntime().exec(cmd);
			p.waitFor();
			reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line;

			for(line = reader.readLine(); line != null; line = reader.readLine() ) { 
				if( line.contains(" grep ") ) {
					continue; 
				}			  
    		  
				//System.out.println(", line = " + line);
				Matcher matcher = pattern.matcher(line);

				if( matcher.find() ) {
					if( ( matcher.groupCount() == 0 ) || ( matcher.group(1).length() < 1 ) ) {
						continue; 
					}
				String pid = matcher.group(1);
				return pid;
				}
			}
		}
		catch(IOException e1) {}
		catch(InterruptedException e2) {}
		finally {
			try {
				reader.close();
			} catch (IOException e) {}
		}
		return "";
	}	
    
	@SuppressWarnings(value = { "unused" })
	private static void killPid( String pid ) {
		try {
			Runtime.getRuntime().exec("kill " + pid);
			s3logger("KILLED " + pid);
			logger("KILLED " + pid);
			System.out.println("KILLED " + pid);
		}
		catch(Exception e) {}
	}

	@SuppressWarnings(value = { "unused" })
	private static void warning() {
		String applescriptCommand =  
	      "tell app \"System Events\"\n" + 
	      "activate\n" +
          "display alert \"Minecraft can only be used on Friday, Saturday, and Sunday!\"\n" + 
          "end tell";

		String[] cmd = {"osascript", "-e", applescriptCommand };
		try {
			Runtime.getRuntime().exec(cmd);
		} catch (Exception e) {}
	} 
 
	/**
	   * Main  Minecraft off hours process killer
	   * https://github.com/SailingSteve/offhours.git
	   * Steve Podell
	   * @param args 0: <computer name>, 0|1: "classpath", if you want to print the classpath on startup
	   */
	  	public static void main(String[] args) {
	  		String computer = "NOT-DEFINED";
	  		if( args.length > 0 )
	  			computer = args[0];
	  		if( Arrays.asList(args).contains("classpath") )
		  		System.out.println( "Classpath: " + System.getProperty("java.class.path"));

	  		new OffHoursMonitor(computer);
	 	} 
}

/*
4/22/14:  Works great from command line or maven in eclipse!
Steve-MacBook-Pro-17:offhours steve$ java -cp "./creds:target/offhours-1.0-SNAPSHOT.jar:$(echo lib/*.jar | tr ' ' ':')" main.java.com.podell.OffHoursMonitor CommandLineStevesComputer classpath

sudo launchctl load /Users/Steve/offhours/com.podell.java.offhours.plist

*/ 
