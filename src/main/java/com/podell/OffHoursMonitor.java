package main.java.com.podell;

import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;


public class OffHoursMonitor extends AmazonBase {

	private static Pattern pattern = Pattern.compile("(\\d+)");

  	static PlayDay[] playDays = new PlayDay[] {
  		new PlayDay(Calendar.SUNDAY, 6, 22),
 		new PlayDay(Calendar.FRIDAY, 15, 22),
		new PlayDay(Calendar.SATURDAY, 6, 22)
  	};

  	OffHoursMonitor() {
  		super(playDays);  		
  		
  		initAmazon();
 	
  		while( true ) {
  			boolean offHours = isOffHours();
  			System.out.println( "offHours = " + offHours );
  			String pid = getMineCraftJavaPid();


  			if( ( ! pid.isEmpty() ) && ( offHours ) ) {
  				System.out.println( "pid = " + pid );
  				killPid(pid);
  				warning();
  			}
 	  
  			try {
  				Thread.sleep( offHours ? 5000 : 300000 );
  			} catch (InterruptedException e) {}
  			//System.out.println("bottom of loop, pid = " + pid);
  		}
 
  	}

    
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
					//System.out.println("Rejected line = " + line);
					continue; 
				}			  
    		  
				System.out.println(", line = " + line);
				Matcher matcher = pattern.matcher(line);

				if( matcher.find() ) {
					if( ( matcher.groupCount() == 0 ) || ( matcher.group(1).length() < 1 ) ) {
						// System.out.println("No match line = " + line);
						continue; 
					}
				String pid = matcher.group(1);
				System.out.println("pid : " + pid + ", line = " + line);
				s3logger("Found running instance => " + line);
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
    
	private static void killPid( String pid ) {
		try {
			Runtime.getRuntime().exec("kill " + pid);
			s3logger("KILLED " + pid);
			logger("KILLED " + pid);
			System.out.println("KILLED " + pid);
		}
		catch(Exception e) {}
	}

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
	   * @param args
	   */
	  	public static void main(String[] args) {
	  		new OffHoursMonitor();
	 	}
	 
}


