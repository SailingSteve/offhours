package main.java.com.podell;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * Class to encapsulate a day that play is allowed on
 * @author stevepodell
 *
 */
public class PlayDay {
	int dayOfWeek, startHour, endHour;
	
	public PlayDay( int dayOfWeek, int startHour, int endHour ) {
		  this.dayOfWeek = dayOfWeek;
		  this.startHour = startHour;
		  this.endHour = endHour;
	}
	
	public int getDayOfWeek() {
		return dayOfWeek;
	}
	
	public void setDayOfWeek(int dayOfWeek) {
		this.dayOfWeek = dayOfWeek;
	}
	
	public int getStartHour() {
		return startHour;
	}
	
	public void setStartHour(int startHour) {
		this.startHour = startHour;
	}
	
	public int getEndHour() {
		return endHour;
	}
	
	public void setEndHour(int endHour) {
		this.endHour = endHour;
	}
	  	
	public boolean isInHours( Calendar cal ) {
		return( (  cal.get(Calendar.DAY_OF_WEEK) == dayOfWeek ) && 
			(  cal.get(Calendar.HOUR_OF_DAY) >= startHour ) &&
			( cal.get(Calendar.HOUR_OF_DAY) < endHour ) );
	}

	public boolean isOutOfHoursToday( Calendar cal, Date launch ) {
		Calendar launchCal = Calendar.getInstance(TimeZone.getTimeZone("GMT-7"));  //November through march is GMT-8
		launchCal.setTime( launch );
		// Must be today
		if( cal.get(Calendar.DAY_OF_WEEK) != launchCal.get(Calendar.DAY_OF_WEEK) )
			return false;
		// Out of bounds start time today
		return( 
			(  cal.get(Calendar.HOUR_OF_DAY) < startHour ) &&
			( cal.get(Calendar.HOUR_OF_DAY) >= endHour ) );
	}
}
 