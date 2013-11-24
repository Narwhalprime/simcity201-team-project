package simcity.gui;

import restaurant_rancho.gui.RestaurantRancho;
import simcity.PersonAgent;
import simcity.interfaces.Transportation_Douglass;
import simcity.test.mock.MockTransportation_Douglass;
import housing.Housing;

import javax.swing.*;

import java.awt.*;
import java.util.*;
import java.util.Timer;

public class SimCityPanel extends JPanel{
	
	Timer timer;
	
	SimCityGui gui = null;
	RestaurantRancho restRancho;
	 	 
	ArrayList<PersonAgent> people = new ArrayList<PersonAgent>();
	ArrayList<Housing> housings = new ArrayList<Housing>();
	Transportation_Douglass transportation = new MockTransportation_Douglass("Mock Transportation");
	
	private JPanel group = new JPanel();
	 
	public SimCityPanel(SimCityGui gui) {
		
		this.gui = gui;
		restRancho = gui.restRancho;
		
		Housing firstHousing = new Housing(gui, "Haunted Mansion");
		String foodPreferenceMexican = "Mexican";
	 
		// All PersonAgents are instantiated here. Upon instantiation, we must pass
		// all pointers to all things (restaurants, markets, housings, banks) to the person as follows:
		PersonAgent firstHackedPerson = new PersonAgent("Narwhal Prime", firstHousing, foodPreferenceMexican, "OwnerResident", transportation);
		firstHousing.setOwner(firstHackedPerson);
		firstHousing.addRenter(firstHackedPerson);
		firstHackedPerson.addRestaurant(restRancho, "Customer");
		people.add(firstHackedPerson);
		
		// Alternatively, you can call the next line as a hack (in place of the previous three lines)
		//		 firstHousing.setOwner();
		 
		firstHackedPerson.startThread();
	
		/* timing */
	    setLayout(new GridLayout());
	    timer = new Timer();
	    timer.scheduleAtFixedRate(new TimerTask() {
			public void run() {
				incrementNumTicks();
				handleTick();
			}
		}, 0, TICK_DELAY);
	}
	
	public void handleTick() {
		long currTicks = getNumTicks();
		if(currTicks % 10 == 0)
			System.out.println("Timer has ticked: # ticks = " + currTicks);
		
		for(int i = 0; i < people.size(); i++) {
			final PersonAgent person = people.get(i);
			
			// hunger signals
			if(currTicks == MORNING || currTicks == NOON || currTicks == EVENING) {
				timer.schedule(new TimerTask() {
					public void run() {
						System.out.println("Setting person to be hungry");
						person.msgSetHunger(false);
					}
				}, (int)(Math.random() * EAT_DELAY_MAX * TICK_DELAY));
			}
			
			
			// body state signals
			if(currTicks == START_OF_DAY) {
				person.msgWakeUp();
			}
			if(currTicks == END_OF_DAY) {
				person.msgGoToSleep();
			}
		}
	}
	
	/* all time-related variables and methods */
	public enum DayOfTheWeek { Sunday, Monday, Tuesday, Wednesday, Thursday, Friday, Saturday };
	public DayOfTheWeek currentDay = DayOfTheWeek.Friday;
	public long numTicks = 0;
	
	/* Time intervals */
	private static final int TICK_DELAY = 125; // every quarter second = one clock tick
	
	// these are start times for each of the day's phases
	private static final long START_OF_DAY = 1;
	private static final long MORNING = 30;
	private static final long NOON = 150;
	private static final long EVENING = 270;
	private static final long END_OF_DAY = 390;
	
	// for setting random delay for eating
	private static final int EAT_DELAY_MAX = 10;
	
	public String getCurrentDay() {
		return currentDay.toString();
	}

	public void setNextDay() {
		switch(currentDay) {
			case Sunday:
				currentDay = DayOfTheWeek.Monday; break;
			case Monday:
				currentDay = DayOfTheWeek.Tuesday; break;
			case Tuesday:
				currentDay = DayOfTheWeek.Wednesday; break;
			case Wednesday:
				currentDay = DayOfTheWeek.Thursday; break;
			case Thursday:
				currentDay = DayOfTheWeek.Friday; break;
			case Friday:
				currentDay = DayOfTheWeek.Saturday; break;
			case Saturday:
				currentDay = DayOfTheWeek.Sunday; break;
		}
	}
	
	public long getNumTicks() {
		return numTicks;
	}
	
	public void incrementNumTicks() {
		numTicks++;
		if(numTicks > END_OF_DAY) {
			numTicks = 0;
			setNextDay();
			timer.cancel(); // TODO test that this stops entire simulation
		}
	}
}
