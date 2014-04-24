package main.java.com.podell;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.ClasspathPropertiesFileCredentialsProvider;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;

public class AmazonBase {
  	private static String computerName;
  	private static String userName;
    static AmazonEC2      ec2;
    static AmazonS3       s3;
	private static String path;
  	private static String logFile;
  	protected PlayDay[] playDays;
  	
  	
  	protected AmazonBase(PlayDay[] playDays) {
  		this.playDays = playDays;
  		initAmazon();
    }
  	
  	protected void setPlayDays( PlayDay[] playDays) {
  		this.playDays = playDays;
  	}
  	
  	private void initAmazon() {
  		try {
  			AWSCredentialsProvider credentialsProvider = new ClasspathPropertiesFileCredentialsProvider();

  			s3  = new AmazonS3Client(credentialsProvider);
  			ec2 = new AmazonEC2Client(credentialsProvider);
  			ec2.setEndpoint("ec2.us-west-2.amazonaws.com");
  			
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
  			s3logger("STARTUP path is '" + path + "', computerName is '" + computerName + "', whoami is '" + userName);
  			logger(  "STARTUP path is '" + path + "', computerName is '" + computerName + "', whoami is '" + userName);
  		}
  		catch(Exception e) {
  			System.out.println( "Caught error exception : " + e.getStackTrace());
  		}
  	}
  	
	protected static void s3logger( String logPayload ) {
		String key = new SimpleDateFormat("MM/dd/yyyy k.m.s.m").format(new Date()) + ":" + userName + ":" + computerName;
	  /*
	   * Amazon S3
	   *
	   * The AWS S3 client allows you to manage buckets and programmatically
	   * put and get objects to those buckets.
	   *
	   * In this sample, we use an S3 client to iterate over all the buckets
	   * owned by the current user, and all the object metadata in each
	   * bucket, to obtain a total object and space usage count. This is done
	   * without ever actually downloading a single object -- the requests
	   * work with object metadata only.
	   */
		try {
			File file = File.createTempFile("offHours", "tmp");
			String filename = file.getAbsolutePath();
			PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(file)));
			out.println( logPayload);	    
			out.close();
			
			file = new File( filename);
			s3.putObject("com.podell.test1", key, file );
			System.out.println(AmazonBase.logDate() + " ->  s3 write : " + file.getPath());
			
			List<Bucket> buckets = s3.listBuckets();
			for( Bucket bucket : buckets) {
				long totalSize  = 0;
				int  totalItems = 0;
	
				ObjectListing objects = s3.listObjects(bucket.getName());
				do {
				    for (S3ObjectSummary objectSummary : objects.getObjectSummaries()) {
				    	totalSize += objectSummary.getSize();
				    	totalItems++;
				    }
				    objects = s3.listNextBatchOfObjects(objects);				    		
				} while (objects.isTruncated());
	
			System.out.println(AmazonBase.logDate() + " ->  You have " + buckets.size() + " Amazon S3 bucket(s), " +
	              "containing " + totalItems + " objects with a total size of " + totalSize + " bytes.");
			}
		} catch (AmazonServiceException ase) {
	      /*
	       * AmazonServiceExceptions represent an error response from an AWS
	       * services, i.e. your request made it to AWS, but the AWS service
	       * either found it invalid or encountered an error trying to execute
	       * it.
	       */
			System.out.println("Error Message:    " + ase.getMessage());
			System.out.println("HTTP Status Code: " + ase.getStatusCode());
			System.out.println("AWS Error Code:   " + ase.getErrorCode());
		    System.out.println("Error Type:       " + ase.getErrorType());
		    System.out.println("Request ID:       " + ase.getRequestId());
		} catch (AmazonClientException ace) {
	      /*
	       * AmazonClientExceptions represent an error that occurred inside
	       * the client on the local host, either while trying to send the
	       * request to AWS or interpret the response. For example, if no
	       * network connection is available, the client won't be able to
	       * connect to AWS to execute a request and will throw an
	       * AmazonClientException.
	       */
			System.out.println("AmazonClientException: " + ace.getMessage());
		} catch (IOException ioe) {
			System.out.println("IOException: " + ioe.getMessage());
		} 
	}
	
	protected static void logger( String s ) {
		try{
			String line = logDate() + ":" + userName + 
					":" + computerName + ":" + s;
		
			PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(logFile, true)));
			out.println(line);	    
			out.close();
		}
		catch(IOException e){} 
	}  
	
	protected static String logDate() {
		return new SimpleDateFormat("MM/dd/yyyy k:m").format(new Date());
	}

	protected int minutesSince(Calendar calIn) {
		Calendar calNow = getNowPST();
		int minIn = ( calIn.get(Calendar.HOUR_OF_DAY) * 60 ) + calIn.get(Calendar.MINUTE);
		int minNow = ( calNow.get(Calendar.HOUR_OF_DAY) * 60 ) + calNow.get(Calendar.MINUTE);
		return minNow - minIn;
	}
	
	
	protected boolean isOffHours() {
		Calendar cal = getNowPST();
	  
		for( PlayDay playDay : playDays ) {
			if(playDay.isInHours(cal))
				return false;
		}
		return true;
	}

	protected boolean manuallyLaunchedToday(Date launch) {
		Calendar cal = getNowPST();
	  
		for( PlayDay playDay : playDays ) {
			if(playDay.isOutOfHoursToday(cal, launch))
				return false;
		}
		return true;
		
	}
	
	protected static Calendar getNowPST() {
		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT-7"));  //November through march is GMT-8
		cal.setTime( new Date() );
		//System.out.println("Created GMT-8 cal with date [" + cal.getTime() + "]");
		return cal;
	}
}
