package simcity;

import agent.Agent;

import java.util.*;
import java.util.concurrent.Semaphore;

import simcity.interfaces.Restaurant;
import simcity.interfaces.Transportation;
import simcity.test.mock.EventLog;

// Priority of coding classes: Person, Housing, Transportation, Bank, Restaurant, Market 

public class PersonAgent extends Agent {
	
	// ************************* DATA ***********************************
	
	// Unit testing
	public EventLog log = new EventLog();
	
	// Inherent data - simple variables
	private String name;
	private int nourishmentLevel;
	private double moneyOnHand;
	private enum PersonType {Normal, Wealthy, Deadbeat, Crook};
	private PersonType myPersonality;
	private enum PreferredCommute {Walk, Bus, Car};
	private PreferredCommute preferredCommute;
	
	// Transportation
	private Transportation transportation;
	
	// Location
	private enum LocationState {Home, Transit, Restaurant, Bank, Market};
	private LocationState currentLocationState;
	private String currentLocation;
	
	// Variables with intention to update
	private double moneyWanted;
	private LocationState targetLocationState;
	private String targetLocation;
	
	// Wrapper class lists
	private List<MyHousing> myHousings = new ArrayList<MyHousing>();
	private List<MyRestaurant> myRestaurants = new ArrayList<MyRestaurant>();
	private List<MyBank> myBanks = new ArrayList<MyBank>();
	private List<MyBankAccount> myBankAccounts = new ArrayList<MyBankAccount>();
	
	private MyHousing myHouse = null;
	private MyBankAccount myPersonalBankAccount = null;
	
	// Food
	String foodPreference;
	boolean preferEatAtHome;
	
	// Synchronization
	Semaphore readyForNextAction = new Semaphore(0, true);
	
	// ************************* SETUP ***********************************
	
	// Constructor for CustomerAgent class
	public PersonAgent(String aName, Transportation t) {
		super();
		name = aName;
		myPersonality = PersonType.Normal;
		currentLocationState = LocationState.Home;
		targetLocationState = LocationState.Home;
		preferredCommute = PreferredCommute.Walk;
		transportation = t;
	}

	// get/set methods
	
	public String	getName()				{ return name; }
	public int		getNourishmentLevel()	{ return nourishmentLevel; }
	public double	getMoney()				{ return moneyOnHand; }

	public void		setNourishmentLevel(int level)	{ nourishmentLevel = level; }
	public void		setMoney(int money)				{ moneyOnHand = money; }
	

	public void	setFoodPreference(String type, boolean atHome) {
		foodPreference = type;
		preferEatAtHome = atHome;
	}
	
	public void	addRestaurant(String name, String restaurantType, String personType, Map<String, Double> menu) {
		MyRestaurant newMyRestaurant = new MyRestaurant(name, restaurantType, personType, menu);
		myRestaurants.add(newMyRestaurant);
	}
	public void	addBankAccount(String accountName, String accountType, String bankName, int accountNumber) {
		MyBankAccount newMyBankAccount = new MyBankAccount(accountName, accountType, bankName, accountNumber);
		myBankAccounts.add(newMyBankAccount);
	}
	
	public String toString() {
		return "Person " + getName();
	}
	
	// ************************* MESSAGES ***********************************

	// from Transportation
	public void msgReachedDestination(String destination) {
		currentLocation = destination;
		//TODO Map destination to appropriate enum location state
		stateChanged();
	}
	
	// from Bank
	public void msgWithdrawalSuccessful(double amount) {
		
		stateChanged();
	}
	
	// from Restaurant
	public void msgDoneEating() {
		
		stateChanged();
	}
	
	// ************************* SCHEDULER ***********************************
	
	public boolean pickAndExecuteAnAction() {
		
		if(currentLocationState == LocationState.Home) {
			if(nourishmentLevel <= 0) {
				if(preferEatAtHome) {
					// TODO if person prefers eating at home
					return true;
				}
				else {
					MyRestaurant targetRestaurant = chooseRestaurant();
					Map<String, Double> theMenu = targetRestaurant.menu;
					// TODO: get the minimum food cost; this is a hack
					double lowestPrice = 100;
					if(moneyOnHand < lowestPrice) {
						moneyWanted = lowestPrice - moneyOnHand;
						// TODO: bank name hacked
						targetLocation = "Mock Bank 1";
						goToX();
						return true;
					}
					targetLocation = targetRestaurant.restaurantName;
					goToX();
					return true;
				}
			}
		}
		
		if(currentLocationState == LocationState.Bank) {
			// TODO Person scheduler while in Bank
		}

		if(currentLocationState == LocationState.Restaurant) {
			// TODO Person scheduler while in Restaurant
		}
		
		return false;
	}

	// ************************* ACTIONS ***********************************
	
	private void goToX() {
		transportation.msgGoTo(currentLocation, targetLocation, this, preferredCommute.name());
	}
	
	/*
	private void GoToRestaurant() {
		Do("Going to restaurant");
		customerGui.DoDisplayOrder("" + money);
		customerGui.DoGoToWaitingArea();
		host.msgIWantFood(this);
	}
	
	private void ReadingMenu() {
		Do("Reading menu and deciding order...");
		
		timer.schedule(new TimerTask() {
			public void run() {
				print("Done deciding");
				event = AgentEvent.orderDecided;
				stateChanged();
			}
		},
		orderTime * Constants.SECOND); //how long to wait before running task
	}
	*/
	
	// ************************* UTILITIES ***********************************
	
	private void mapLocationToEnum(String location) {
		MyBankAccount[] myBankAccountsArray = (MyBankAccount[])myBankAccounts.toArray(new MyBankAccount[myBankAccounts.size()]);
		for(int i = 0; i < myBankAccountsArray.length; i++) {
			MyBankAccount tempMBA = myBankAccountsArray[i];
			if(location.equals(tempMBA.bankName)) {
				
			}
		}
		for(int i = 0; i < myHousings.size(); i++) {
			
		}
	}
	
	private MyRestaurant chooseRestaurant() {
		// TODO: hack
		return myRestaurants.get(0);
	}
	
	// ************************* WRAPPER CLASSES ***********************************
	
	private class MyHousing {
		
		String housingName, housingType, occupantType;
		Map<String, Integer> inventory = new HashMap<String, Integer>();
		
		public MyHousing(String housingName, String housingType, String occupantType) {
			this.housingName = housingName; 
			this.housingType = housingType;  
			this.occupantType = occupantType;
		}
	}
	
	private class MyBankAccount {
		// Bank theBank;
		String accountName, accountType, bankName;
		int accountNumber;
		double amount = 0, loanNeeded = 0;
		
		public MyBankAccount(String accountName, String accountType, String bankName, int accountNumber) {
			this.accountName = accountName; 
			this.accountType = accountType;
			this.bankName = bankName;
			this.accountNumber = accountNumber;
		}
	}
	
	private class MyRestaurant {
		Restaurant theRestaurant;
		String restaurantName, restaurantType, personType;
		Map<String, Double> menu = new HashMap<String, Double>();
		
		public MyRestaurant(String restaurantName, String restaurantType, String personType, Map<String, Double> map) {
			// TODO initialize
		}
	}
	
	private class MyBank {
		String bankName;
		
		public MyBank(String name) {
			bankName = name;
		}
	}
}
