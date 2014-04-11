package main.java.com.podell;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PrintWriter;

public class OffHoursMonitor {
  private static String path;
  private static String logFile;
  private static Pattern pattern;
  private static String computerName;
  private static String userName;


  /**
   * Main  Minecraft off hours process killer
   * https://github.com/SailingSteve/offhours.git
   * Steve Podell
   * @param args
   */
  public static void main(String[] args) {
    int[] days = {Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY };
	init();
	
	while( true ) {
 	  boolean offHours = isOffHours( days, 6, 23);
 	  System.out.println( "offHours = " + offHours );
 	  String pid = getMineCraftJavaPid();
 	  System.out.println( "pid = " + pid );

 	  if( ( ! pid.isEmpty() ) && ( offHours ) ) {
 	    killPid(pid);
 	    warning();
   	  }
 	  
	  try {
		Thread.sleep( offHours ? 5000 : 300000 );
	  } catch (InterruptedException e) {}
	  //System.out.println("bottom of loop, pid = " + pid);
	}
  }
 
  private static void init() {
	try {
	  pattern = Pattern.compile("(\\d+)");
	  Process p = Runtime.getRuntime().exec("pwd");
	  p.waitFor();
	  BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
	  String line = reader.readLine();
	  String[] sa = line.split("/");
	  path = "/" + sa[1] + "/" + sa[2] + "/"; 
	  logFile = path + "offhours.log";
	  
	  p = Runtime.getRuntime().exec("scutil --get ComputerName");
	  p.waitFor();
	  reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
	  line = reader.readLine();
	  computerName = line.trim().replaceAll(" ", "-").replaceAll("[Äô‚]", "");
	  
	  p = Runtime.getRuntime().exec("whoami");
	  p.waitFor();
	  reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
	  line = reader.readLine();
	  userName = line.trim();
	  logger("STARTUP path is '" + path + "', computerName is '" + computerName + "', whoami is '" + userName);
	}
    catch(Exception e) {}
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
    	     logger("Found running instance => " + line);
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
  
  private static boolean isOffHours( int[] allowedDates, int startHour, int endHour ) {
	  Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT-7"));  //Novembr -> march is GMT-8
	  cal.setTime( new Date() );
	  System.out.println("Created GMT-8 cal with date [" + cal.getTime() + "]");
	  int thisDayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
	  int hour = cal.get(Calendar.HOUR_OF_DAY);
	  //System.out.println("cal with date [" + hour + "] [" + thisDayOfWeek + "], Wednes is " + Calendar.WEDNESDAY);
	  
	  for( int aDayOfWeek : allowedDates )
	  {
		  if( aDayOfWeek == thisDayOfWeek )
			  return  !( ( hour  >= startHour ) && ( endHour  <= 23 ) );
	  }
	  return true;
 }


  
  private static void killPid( String pid ) {
	try {
	  Runtime.getRuntime().exec("kill " + pid);
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
  
  private static void logger( String s ) {
  	try{
	  String line = new SimpleDateFormat("MM/dd/yyyy k.m").format(new Date()) + ":" + userName + ":" + computerName + ":" + s;
		
	  PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(logFile, true)));
	  out.println(line);	    
	  out.close();
    }
  	catch(IOException e){} 
  }  
}


