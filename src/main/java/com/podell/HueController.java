package main.java.com.podell;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Properties;

import nl.q42.jue.FullLight;
import nl.q42.jue.HueBridge;
import nl.q42.jue.Light;
import nl.q42.jue.State.AlertMode;
import nl.q42.jue.StateUpdate;
import nl.q42.jue.exceptions.ApiException;

import org.apache.http.client.ClientProtocolException;
import org.apache.commons.io.IOUtils;

/**
 * A small daemon to update hue colors based on jenkins status
 * @author stevepodell
 * https://github.com/Q42/Jue/wiki
 */
public class HueController {   
	
    private static ArrayList<String>  jenkinsJobs = null;
    private static String    urlString;
    private static String    username;
    private static String    password;
    private static HueBridge bridge = null;
    private static String    hueBridgeIP;
    
    public class Lamp {
    	String name;
    	Light light;
    	FullLight fullLight;
    	StateUpdate su;
    	String jenkinsColor;
    	String jenkinsJob;
    }
    
	private static ArrayList<Lamp> arrayLamps = new ArrayList<Lamp>();   
 

    public HueController() {
    	try {
    		if( ! getPropValues() )
    			return;
          	bridge = new HueBridge(hueBridgeIP);
          	bridge.authenticate("ffdcJenkins");
	    	String newUser = bridge.getUsername();
	    	
	    	System.out.println("Connected to the bridge as '" + newUser + "'");
	    	
 	    	// Initialize lamp array
	    	int i = 0;
			for (Light light : bridge.getLights()) {
				String job = jenkinsJobs.get(i++);
			    if( job == null )
			    	break;
			    Lamp lamp = new Lamp();
			    lamp.jenkinsJob = job;
				lamp.light = light;
			    lamp.fullLight = bridge.getLight(light);
			    lamp.name = lamp.fullLight.getName();
			    System.out.println(lamp.name + ", brightness: " + lamp.fullLight.getState().getBrightness() + ", hue: " + lamp.fullLight.getState().getHue() 
			    		+ ", saturation: " + lamp.fullLight.getState().getSaturation() + ", temperature: " + lamp.fullLight.getState().getColorTemperature());
			    arrayLamps.add(lamp);
			}
						
    	} catch (IOException e) {
    		System.out.println("IoException 1 " + e );
		} catch (ApiException e) {
    		System.out.println("ApiException 1 " + e );
		}     	
    }

    // https://ci.dev.financialforce.com/api/xml
    // https://ci.dev.financialforce.com/view/PSA/api/json
    static boolean jenkinsStateUpdater() {
    	try {
			String json = getJenkinsDomCurl(urlString, username, password);
			for(Lamp lamp : arrayLamps) {
				lamp.jenkinsColor = getColor(json, lamp.jenkinsJob);
				lamp.su = buildStateUpdateForAJenkinsColor(lamp.jenkinsColor, 42);
				bridge.setLightState(lamp.light, lamp.su);
		    	System.out.println("\tChanging '" +lamp.name + "' to color '" + lamp.jenkinsColor + "' for job  '" + lamp.jenkinsJob + "'");
			}
		} catch (ClientProtocolException e) {
	  		System.out.println("ClientProtocolException " + e );
		} catch (IOException e) {
	  		System.out.println("IoException 2 " + e );
		} catch (ApiException e) {
	  		System.out.println("ApiException 1 " + e );
		}
        
        return true;
    }
    
    static StateUpdate buildStateUpdateForAJenkinsColor( String jenkinsColor, int brightness ) {
    	if(jenkinsColor.equals("blue"))
    		return new StateUpdate().turnOn().setHue(46920).setBrightness(brightness);
    	if(jenkinsColor.equals("red"))
    		return new StateUpdate().turnOn().setHue(65280).setBrightness(brightness);
    	if(jenkinsColor.equals("blue_anime"))
    		return new StateUpdate().turnOn().setHue(46920).setBrightness(brightness).setAlert(AlertMode.SELECT);
    	if(jenkinsColor.equals("red_anime"))
    		return new StateUpdate().turnOn().setHue(65280).setBrightness(brightness).setAlert(AlertMode.SELECT);
    	// else "notbuilt" or "disabled", is dim white
    		return new StateUpdate().turnOn().setColorTemperature(500).setBrightness(brightness/2);
    }
    
    /**
     * Another hack that avoids doing a dom parsing, just to find the color of the job
     * @param json
     * @param jobName
     * @return String, the color or null if not found
     */
    static String getColor(String json, String jobName) {
    	int nameIndex = json.indexOf(jobName);
    	if(nameIndex > 0) {
    		int colorIndex = json.indexOf("\"color\":\"", nameIndex) + 9; {
    			int endColorIndex = json.indexOf('"', colorIndex); {
    				if(endColorIndex > 0) {
    					String color = json.substring(colorIndex, endColorIndex);
    					//System.out.println("Color is: " + color);
    					return color;
    				}		
    			}
    		}
    	}
    	return null;
    }
     
	/**
	 * A horrible hack, to avoid spending hours on httpclient setup
	 * $ curl -u spodell%40financialforce.com:myapitokene1eb6eb5566fe3173914444802b44056 https://ci.dev.financialforce.com/api/xml
	 * https://ci.dev.financialforce.com/view/PSA/api/json
	 * @param urlString
	 * @param username
	 * @param password
	 * @return
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public static String getJenkinsDomCurl(String urlString, String username, String password) throws ClientProtocolException, IOException {
		String token = username + ":" + password;
	
	    ProcessBuilder pb = new ProcessBuilder(
	            "curl",
	            "-u",
	            token,
	            urlString);
	
	    // pb.redirectErrorStream(true);
	    Process p = pb.start();
	
	    
	    String out = IOUtils.toString(p.getInputStream(), "UTF-8");
	    // System.out.println(out);
	    return out;
	}

	public static boolean getPropValues() throws IOException {
		Properties prop = new Properties();
		
		InputStream in  = HueController.class.getResourceAsStream("config.properties");
		if(in == null) {
			System.out.println("The config.properties file is not on the classpath!");
			return false;
		}
		prop.load(in);
		
		jenkinsJobs = new ArrayList<String>();
		String j = (String) prop.get("job1");
		jenkinsJobs.add(j);		
		j = (String) prop.get("job2");
		if(j != null)
			jenkinsJobs.add(j);
		j = (String) prop.get("job3");
		if(j != null)
			jenkinsJobs.add(j);
		urlString = (String) prop.get("urlString");
		username = (String) prop.get("username");
		password = (String) prop.get("password");
		hueBridgeIP = (String) prop.get("hueBridgeIP");
		return true;
	}
    
    /**
     * Main  Hue Controller
     * https://github.com/SailingSteve/offhours.git
	 * Steve Podell
	 * @param args 0: <computer name>, 0|1: "classpath", if you want to print the classpath on startup
	 */
    public static void main(String[] args) {
   	
  		if( Arrays.asList(args).contains("classpath") )
	  		System.out.println( "Classpath: " + System.getProperty("java.class.path"));

  		new HueController();
	  	while( jenkinsJobs != null ) {
	  		jenkinsStateUpdater(); 
	  		System.out.println( new SimpleDateFormat("MM/dd/yyyy  HH:mm:ss").format(Calendar.getInstance().getTime()) );
	  		try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {}
	  	}
    }  
}
