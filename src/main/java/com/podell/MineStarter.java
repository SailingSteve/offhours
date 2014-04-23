package main.java.com.podell;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.ec2.model.DescribeAvailabilityZonesRequest;
import com.amazonaws.services.ec2.model.DescribeAvailabilityZonesResult;
import com.amazonaws.services.ec2.model.DescribeInstanceStatusRequest;
import com.amazonaws.services.ec2.model.DescribeInstanceStatusResult;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceStateChange;
import com.amazonaws.services.ec2.model.InstanceStatus;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.StartInstancesRequest;
import com.amazonaws.services.ec2.model.StartInstancesResult;
import com.amazonaws.services.ec2.model.StopInstancesRequest;
import com.amazonaws.services.ec2.model.StopInstancesResult;

/**
 * Start and stop the ec2 instance that contains the minecraft server, so that it is available
 * during allowed play hours, for all kids.  If manually started on an off day, 
 * and then not manually stopped, then kill it at midnight.
*/
public class MineStarter extends AmazonBase {


	static PlayDay[] playDays = new PlayDay[] {
  		new PlayDay(Calendar.SUNDAY, 6, 22),
 		new PlayDay(Calendar.MONDAY, 15, 22),
 		new PlayDay(Calendar.TUESDAY, 15, 22),
 		new PlayDay(Calendar.WEDNESDAY, 15, 22),
 		new PlayDay(Calendar.THURSDAY, 15, 22),
 		new PlayDay(Calendar.FRIDAY, 15, 22),
		new PlayDay(Calendar.SATURDAY, 6, 22)
  	};
	
    protected MineStarter(PlayDay[] playDays, String[] args) {
		super(playDays);
		       
        if ( args.length > 0 ) {
        	String arg0 = args[0];
        	if( arg0.equalsIgnoreCase("start") ) {
                System.out.println("\nStart em:");
                startStopInstance( true, "i-f7ace9ff" );
                s3logger("Starting ec2 instance i-f7ace9ff"); 
        	}
        	else if( arg0.equalsIgnoreCase("stop") ) {
                System.out.println("\nStop em:");
                startStopInstance( false, "i-f7ace9ff" );
                s3logger("Stopping ec2 instance i-f7ace9ff");         	
            }
        	else if( arg0.equalsIgnoreCase("status") ) {
        		status(true);
        	}
        	else if( arg0.equalsIgnoreCase("daemon") ) {
        		daemon();
        	}
        	else
        		System.out.println("Unknown argument '" + arg0 + "'!");
        } 
        else
        	System.out.println("No arguments passed to main!");
	}
        
    private static List<String> getInstanceList(String sInstanceId) {
    	ArrayList<String> al = new ArrayList<String>();
    	al.add(sInstanceId);
    	
        DescribeInstanceStatusRequest describeInstanceRequest = new DescribeInstanceStatusRequest().withInstanceIds( al );
        DescribeInstanceStatusResult describeInstanceResult = ec2.describeInstanceStatus(describeInstanceRequest);
        List<InstanceStatus> statusList  = describeInstanceResult.getInstanceStatuses();
  
        for( InstanceStatus status : statusList ) {
        	System.out.println("Instance is '" + status.getInstanceState().getName() + "' with code = " + status.getInstanceState().getCode() );
        }
        return al;
    }
    

    private static boolean startStopInstance( boolean bStart, String sInstanceId )
    {
    	List<String> al = getInstanceList(sInstanceId);
    	
    	long start = System.currentTimeMillis();
    	InstanceStateChange isc;
    	if( bStart ) {
    		StartInstancesResult sir = ec2.startInstances( new StartInstancesRequest( al ) );
    		isc = sir.getStartingInstances().get(0);
    		System.out.println( "\tStart request, from " + isc.getPreviousState() + " to " + isc.getCurrentState() );
    	}
    	else {
    		StopInstancesResult sir =ec2.stopInstances( new StopInstancesRequest ( al ) );
    		isc = sir.getStoppingInstances().get(0);
    		System.out.println( "\tStop request, from " + isc.getPreviousState() + " to " + isc.getCurrentState() );
    	} 	
    	
    	if( ( isc != null ) && ( isc.getPreviousState().getCode() == isc.getCurrentState().getCode() ) )
    			System.out.println("\t******** Ooops! State did not change!  Still in the '" + isc.getCurrentState().getName() + "' state!");

    	System.out.println( "\tIt took " + (long)( System.currentTimeMillis() - start ) + "ms for '" + sInstanceId + "' to respond.\n");
    	return true;
    }

    private static Date status() {
    	return status( false );
    }

    /**
     * Do a status check on the instance
     * @param doPrintlns, println a bunch of status messages about the state of the instance
     * @return the date at which the instance was start4d
     */
    private static Date status( boolean doPrintlns ) {
    	try {
        	String[] sa = {"us-west-2a", "us-west-2b", "us-west-2c"};
        	DescribeAvailabilityZonesRequest daz = new DescribeAvailabilityZonesRequest().withZoneNames(sa);
            DescribeAvailabilityZonesResult availabilityZonesResult = ec2.describeAvailabilityZones(daz);
            
        	if ( doPrintlns )
        		System.out.println("You have access to " + availabilityZonesResult.getAvailabilityZones().size() +
        				" Availability Zones.");
            
            DescribeInstancesResult describeInstancesRequest = ec2.describeInstances();
            List<Reservation> reservations = describeInstancesRequest.getReservations();
            Set<Instance> instances = new HashSet<Instance>();

            for (Reservation reservation : reservations) {
                instances.addAll(reservation.getInstances());
            }

        	if ( doPrintlns )
        		System.out.println("You have " + instances.size() + " Amazon EC2 instance(s) running.");
        	
            for( com.amazonaws.services.ec2.model.Instance inst : instances )
            {
            	if ( doPrintlns ) {
	            	System.out.println("Instance:\t " + inst.getInstanceId()  );
	            	System.out.println("Public DNS:\t " + inst.getPublicDnsName() );
	            	System.out.println("Architecture:\t " + inst.getArchitecture() );
	            	System.out.println("Instance type:\t " + inst.getInstanceType() );
	            	System.out.println("Public IP:\t " + inst.getPublicIpAddress() );
	            	System.out.println("Launch:\t\t " + inst.getLaunchTime() );
	            	System.out.println("State:\t\t " + inst.getState() );
	            	System.out.println("Key:\t\t " + inst.getKeyName());
	            	System.out.println("Placement:\t " + inst.getPlacement() );
            	}
            	if( inst.getState().getCode() == 16 ) {
            		// if running, and launch time is not in the correct hour for this day set a hold flag, and do not stop until midnight unless manually stopped
            		return inst.getLaunchTime();
            	}
            }           
        } catch (AmazonServiceException ase) {
        	System.out.println("Caught Exception: " + ase.getMessage());
        	System.out.println("Reponse Status Code: " + ase.getStatusCode());
            System.out.println("Error Code: " + ase.getErrorCode());
            System.out.println("Request ID: " + ase.getRequestId());
        }
    	return null;
    }
    
    /**
     * A daemon method that kills any minecraft processes that are off hours
     */
     private void daemon() {
   		while( true ) {
   			Date launch = status();  
  			if( isOffHours() ) {
  				if( launch != null ) {
  					if( manuallyLaunchedToday(launch) ) {
	  					// don't stop if "off hours" and manually launched today.  
	  					// This logic will kill "forgotten" manual starts at midnight.
	  					System.out.println("Stop skipped, since manually started out of hours at:  " + launch);
  					} else {
		   				System.out.println("\nStop em:");
			            startStopInstance( false, "i-f7ace9ff" );
			            s3logger("Stopping ec2 instance i-f7ace9ff");    
  					}
  				}
	        }
  			else if( launch == null ) {
  				System.out.println("\nStart em:");
  	            startStopInstance( true, "i-f7ace9ff" );
  	            s3logger("Starting ec2 instance i-f7ace9ff"); 
  	        }	
  				
  			try {
  				Thread.sleep(60000);
  			} catch (InterruptedException e) {}
  			//System.out.println("bottom of loop, pid = " + pid);
  		}	
    }

     /**
      * The main
      * @param args, a string command from the set {start, stop, status, daemon}
      * @throws Exception
      */
     public static void main(String[] args) throws Exception {   	
         System.out.println("================== MineStarter =========================");
   		//System.out.println( "STEVE:" + System.getProperty("java.class.path"));

         new MineStarter(playDays, args);
     }   
     

}

/*
Steve-Podells-MacBook-Pro-17:offhours stevepodell$ java -cp "./target/offhours-1.0-SNAPSHOT.jar:./creds:"$(echo lib/*.jar | tr ' ' ':') main.java.com.podell.MineStarter stop
STEVE:./target/offhours-1.0-SNAPSHOT.jar:./creds:lib/aspectjrt.jar:lib/aspectjweaver.jar:lib/aws-java-sdk-1.7.5.jar:lib/commons-codec-1.3.jar:lib/commons-logging-1.1.1.jar:lib/freemarker-2.3.18.jar:lib/httpclient-4.2.3.jar:lib/httpcore-4.2.jar:lib/jackson-annotations-2.1.1.jar:lib/jackson-core-2.1.1.jar:lib/jackson-databind-2.1.1.jar:lib/joda-time-2.2.jar:lib/mail-1.4.3.jar:lib/spring-beans-3.0.7.jar:lib/spring-context-3.0.7.jar:lib/spring-core-3.0.7.jar:lib/stax-1.2.0.jar:lib/stax-api-1.0.1.jar
================== MineStarter =========================
s3 write : /var/folders/ph/xxznbhbj6zbbn72c_9p71twc0000gn/T/offHours756992254800202741tmp
You have 1 Amazon S3 bucket(s), containing 103 objects with a total size of 202144 bytes.

Stop em:
	Stop request, from {Code: 80,Name: stopped} to {Code: 80,Name: stopped}
	******** Ooops! State did not change!  Still in the 'stopped' state!
	It took 236ms for 'i-f7ace9ff' to respond.

s3 write : /var/folders/ph/xxznbhbj6zbbn72c_9p71twc0000gn/T/offHours4746831534786747263tmp
You have 1 Amazon S3 bucket(s), containing 104 objects with a total size of 202177 bytes.
Steve-Podells-MacBook-Pro-17:offhours stevepodell$ 
*/