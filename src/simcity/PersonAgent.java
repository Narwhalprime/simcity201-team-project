package simcity;

import agent.Agent;
import simcity.gui.trace.AlertLog;
import simcity.gui.trace.AlertTag;
import simcity.interfaces.Bank_Douglass;
import simcity.interfaces.Housing_Douglass;
import simcity.interfaces.Person;

import java.util.*;

import market.Market;
import simcity.test.mock.EventLog;
import simcity.test.mock.LoggedEvent;
import transportation.Transportation;

public class PersonAgent extends Agent implements Person {
	
	// ************************* DATA ***********************************
	
	// Console message handling
	int printCount = 1000;
	
	// Unit testing
	public EventLog log = new EventLog();
	
	// Inherent data of the person
	private String name;
	private boolean isNourished;
	private double moneyOnHand;
	private Map<String, Integer> itemsOnHand;
	private enum PersonType {Normal, Deadbeat, Crook};
	private PersonType myPersonality;
	private enum PreferredCommute {Walk, Bus, Car};
	private PreferredCommute preferredCommute;
	private enum BodyState {Asleep, Active, Tired};
	private BodyState bodyState;
	
	private final static double MONEY_ON_HAND_LIMIT = 80.0;
	private final static int MARKET_PURCHASE_QUANTITY = 5;
	private final static double MONEY_TO_ROB = 300.00;
	private final static int SPAM_RESET = 4;
	
	// Transportation
	private Transportation transportation;
	
	// Location
	private enum LocationState {Home, Transit, Restaurant, Bank, Market};
	private boolean insideHouse = false;
	private LocationState currentLocationState;
	private String currentLocation;
	
	// Other constantly changing, state-based variables related to person
	private double moneyWanted = 0.0;
	private double moneyToDeposit = 0.0;
	private double rentToPay = 0.0;
	private double fareToPay = 0.0;
	private MyObject workplace = null;
	private String targetLocation;
	private enum RestaurantState		{ None, WantToEat, WantToWork }
	private enum MarketState			{ None, WantToBuy, WantToWork };
	private enum BankState				{ None, NeedTransaction, WantToWork };
	private enum TransportationState	{ None, NeedToPayFare };
	private RestaurantState restState = RestaurantState.None;
	private MarketState marketState = MarketState.None;
	private BankState bankState = BankState.None;
	private TransportationState transportationState = TransportationState.None;
	private boolean houseNeedsMaintenance = false;
	private boolean isBankOpen = false;
	private boolean isSunday = false;
	private int spamCounter = 3;
	
	// Wrapper class lists
	private List<MyObject> myObjects = new ArrayList<MyObject>();
	private MyObject currentMyObject = null; // denotes actual location that person is at
	private MyHousing myHome = null;
	private MyBankAccount myPersonalBankAccount = null;
	
	// Food preferences
	String foodPreference;
	boolean preferEatAtHome;
	
	// Synchronization
	public PriorityQueue<Action> actionQueue = new PriorityQueue<Action>();
	public enum PersonEvent {makingDecision, makingDecisionAtRestaurant, onHold, onHoldInTransportation, onHoldInTransportationPayFare, onHoldAtRestaurant,
		onHoldInMarket, onHoldInBank};
	private enum ActionString { becomeHungry, wakeUp, goToSleep, goToWork, payRent, receiveRent, needMaintenance };
	public PersonEvent event = PersonEvent.makingDecision;
	
	// ************************* SETUP ***********************************
	
	// Constructor for CustomerAgent class
	public PersonAgent(String aName, Housing_Douglass h, double startMoney, String foodPreference, boolean preferEatAtHome,
			String relationWithHousing, Transportation t, char commute) {
		super();

		name = aName;
		myPersonality = PersonType.Normal;
		if(aName.equals("Narwhal_Prime")){
			AlertLog.getInstance().logMessage(AlertTag.PERSON, aName, "HACKED TO THIEF");
			myPersonality = PersonType.Crook;
		}
		isNourished = true;
		currentLocation = h.getName();
		moneyOnHand = startMoney;
		targetLocation = currentLocation;
		
		currentLocationState = LocationState.Home;
		switch(commute) {
			case 'W': preferredCommute = PreferredCommute.Walk; break;
			case 'B': preferredCommute = PreferredCommute.Bus; break;
			case 'C': preferredCommute = PreferredCommute.Car; break;
		}
		
		this.foodPreference = foodPreference;
		this.preferEatAtHome = preferEatAtHome;
		
		currentMyObject = addHousing(h, relationWithHousing);
		transportation = t;
		bodyState = BodyState.Asleep;
		itemsOnHand = new HashMap<String, Integer>();
	}

	// accessor/modifier methods
	public String	getName()				{ return name; }
	public boolean	getIsNourished()		{ return isNourished; }
	public double	getMoney()				{ return moneyOnHand; }
	public String	getCurrLocation()		{ return currentLocation; }
	public String	getCurrLocationState()	{ return currentLocationState.name(); }
	public String	getPersonalityType()	{ return myPersonality.name(); }
	public String	getBodyState()			{ return bodyState.toString(); }
	public boolean	isWorking()				{ return workplace != null; }
	
	public boolean hasJob() {
		MyObject[] myObjectsArray = getObjects();
		for(int i = 0; i < myObjectsArray.length; i++) {
			if(myObjectsArray[i] instanceof MyRestaurant) {
				MyRestaurant temp = (MyRestaurant)myObjectsArray[i];
				if(temp.workSession != 0)
					return true;
			}
			else if(myObjectsArray[i] instanceof MyMarket) {
				MyMarket temp = (MyMarket)myObjectsArray[i];
				if(temp.workSession != 0)
					return true;
			}
			else if(myObjectsArray[i] instanceof MyBank) {
				MyBank temp = (MyBank)myObjectsArray[i];
				if(temp.workSession != 0)
					return true;
			}
		}
		return false;
	}

	public void		setIsNourished(boolean full)	{ isNourished = full; }
	public void		setMoney(double money)			{ moneyOnHand = money; }
	
	public void	setPersonality(String type) {
		if(type.equals("Normal"))
			myPersonality = PersonType.Normal;
		else if(type.equals("Deadbeat"))
			myPersonality = PersonType.Deadbeat;
		else if(type.equals("Crook"))
			myPersonality = PersonType.Crook;
	}

	public void	setFoodPreference(String type, boolean atHome) {
		foodPreference = type;
		preferEatAtHome = atHome;
	}
	
	public MyHousing addHousing(Housing_Douglass h, String personType) {
		MyHousing tempMyHousing = new MyHousing(h, h.getName(), personType);
		if(personType.equals("Renter") || personType.equals("OwnerResident"))
			myHome = tempMyHousing; 
		myObjects.add(tempMyHousing);
		return tempMyHousing;
	}
	
	public void	addBank(Bank_Douglass b, String personType, int workSession) {
		MyBank tempMyBank = new MyBank(b, b.getBankName(), personType, workSession);
		myObjects.add(tempMyBank);
	}
	
	public void	addRestaurant(Restaurant r, String personType, int workSession) {
		MyRestaurant tempMyRestaurant = new MyRestaurant(r, r.getRestaurantName(), r.getType(), personType, r.getMenu().menuItems, workSession);
		myObjects.add(tempMyRestaurant);
	}
	
	public void	addMarket(Market m, String personType, int workSession) {
		MyMarket tempMyMarket = new MyMarket(m, m.getName(), personType, workSession);
		myObjects.add(tempMyMarket);
	}
	
	public String toString() {
		return "Person " + getName();
	}
	
	// ************************* MESSAGES *********************************** 

	// from main class
	public void msgWakeUp() {
		log.add(new LoggedEvent("Must wake up"));
		print("Must wake up");
		actionQueue.add(new Action(ActionString.wakeUp, 0, 0));
		event = PersonEvent.makingDecision;
		stateChanged();
	}
	
	public void msgGoToSleep() {
		log.add(new LoggedEvent("Must go to sleep"));
		print("Must go to sleep");
		actionQueue.add(new Action(ActionString.goToSleep, 1, 0));
		stateChanged();
	}
	
	public void msgSetHungry() {
		log.add(new LoggedEvent("I'm hungry now"));
		print("I'm hungry now");
		if(isNourished) {
			actionQueue.add(new Action(ActionString.becomeHungry, 3, 1));
			stateChanged();
		}
	}
	
	public void msgSetBanksOpen(boolean open) {
		log.add(new LoggedEvent("Banks open? " + open));
		isBankOpen = open;
		stateChanged();
	}
	
	public void msgIsSunday(boolean sunday) {
		if(sunday) {
			isSunday = true;
			stateChanged();
		}
	}

	public void msgGoToWork(int i) {
		log.add(new LoggedEvent("Going to work: work period #" + i));
		print("Going to work: work period #" + i);
		actionQueue.add(new Action(ActionString.goToWork, 2, i));
		stateChanged();
	}
	
	public void msgStopWork(double amount) {
		log.add(new LoggedEvent("Stopping work; got paid " + amount));
		print("Stopping work; got paid " + amount);
		moneyOnHand += amount; 
		if(workplace instanceof MyRestaurant)
			restState = RestaurantState.None;
		if(workplace instanceof MyBank)
			bankState = BankState.None;
		if(workplace instanceof MyMarket)
			marketState = MarketState.None;
		workplace = null;
		event = PersonEvent.makingDecision;
		stateChanged();
	}
	
	public void msgSwitchRole(String role, String location) {
		
		MyObject[] myObjectsArray = getObjects();
		for(int i = 0; i < myObjectsArray.length; i++) {
			if(myObjectsArray[i] instanceof MyRestaurant) {
				MyRestaurant temp = (MyRestaurant)myObjectsArray[i];
				if(location.equals(temp.name)) {
					Restaurant theRestaurant = (Restaurant)temp.restaurant;
					myObjects.remove(i);
					if(role.equals("Customer"))
						addRestaurant(theRestaurant, "Customer", 0);
					else
						addRestaurant(theRestaurant, role, 2);
				}
			}
			else if(myObjectsArray[i] instanceof MyMarket) {
				MyMarket temp = (MyMarket)myObjectsArray[i];
				if(location.equals(temp.name)) {
					Market theMarket = (Market)temp.theMarket;
					myObjects.remove(i);
					if(role.equals("Customer"))
						addMarket(theMarket, "Customer", 0);
					else
						addMarket(theMarket, role, 2);
				}
			}
			else if(myObjectsArray[i] instanceof MyBank) {
				MyBank temp = (MyBank)myObjectsArray[i];
				if(location.equals(temp.name)) {
					Bank_Douglass theBank = (Bank_Douglass)temp.bank;
					myObjects.remove(i);
					if(role.equals("Customer"))
						addBank(theBank, "Customer", 0);
					else
						addBank(theBank, role, 2);
				}
			}
		}
		AlertLog.getInstance().logMessage(AlertTag.PERSON, name, "My role is now: " + role);
		print("My role is now: " + role);
		/*
		if(tempObject instanceof MyHousing)
			currentLocationState = LocationState.Bank;
		else if(tempObject instanceof MyRestaurant)
			currentLocationState = LocationState.Restaurant;
		else if(tempObject instanceof MyMarket)
			currentLocationState = LocationState.Market;
		return;
		*/
	}
	
	// from Housing
	public void msgDoneEntering() {
		log.add(new LoggedEvent("msgDoneEntering() called"));
		event = PersonEvent.makingDecision;
		stateChanged();
	}
	
	public void msgRentIsDue(double amount) {
		if(myHome.occupantType.equals("Renter")) {
			log.add(new LoggedEvent("msgRentisDue() called"));
			actionQueue.add(new Action(ActionString.payRent, 1, amount));
			stateChanged();
		}
	}
	
	public void msgHereIsRent(double amount) {
		log.add(new LoggedEvent("msgHereIsRent() called"));
		actionQueue.add(new Action(ActionString.receiveRent, 1, amount));
		stateChanged();
	}
	
	public void msgNeedMaintenance() {
		System.out.println("msgNeedMaintenance() called");
		log.add(new LoggedEvent("msgNeedMaintenance() called"));
		actionQueue.add(new Action(ActionString.needMaintenance, 1, 0));
		stateChanged();
	}
	
	public void msgFinishedMaintenance() {
		log.add(new LoggedEvent("msgFinishedMaintenance() called"));
		event = PersonEvent.makingDecision;
		houseNeedsMaintenance = false;
		stateChanged();
	}
	
	public void msgFoodDone(boolean doneEating) {
		log.add(new LoggedEvent("msgFoodDone() called"));
		if(doneEating) {
			isNourished = true;
			preferEatAtHome = !preferEatAtHome;
		}
		else {
			marketState = MarketState.WantToBuy;
		}
		event = PersonEvent.makingDecision;
		stateChanged();
	}
	
	public void msgDoneLeaving() {
		log.add(new LoggedEvent("msgDoneLeaving() called"));
		insideHouse = false;
		event = PersonEvent.makingDecision;
		stateChanged();
	}
	
	// from Transportation
	public void msgReachedDestination(String destination) {
		print("Received msgReachedDestination: destination = " + destination);
		log.add(new LoggedEvent("Received msgReachedDestination: destination = " + destination));
		currentLocation = destination;
		mapLocationToEnum(currentLocation);
		updateCurrentMyObject(currentLocation);
		event = PersonEvent.makingDecision;
		stateChanged();
	}
	
	public void msgCrash(boolean atHome) {
		if(atHome) {
			print("CRASHING, returning home");
			currentLocation = myHome.name;
			mapLocationToEnum(currentLocation);
			updateCurrentMyObject(currentLocation);
			event = PersonEvent.makingDecision;
			stateChanged();
		}
	}
	
	public void msgPayFare(double fare) {
		log.add(new LoggedEvent("msgPayFare() called; fare = " + fare));
		fareToPay += fare;
		transportationState = TransportationState.NeedToPayFare;
		event = PersonEvent.onHoldInTransportationPayFare;
		stateChanged();
	}

	// from Bank
	public void msgLeftBank(Bank_Douglass theBank, int accountNumber, double change, double loanAmount, int loanTime) {
		log.add(new LoggedEvent("Leaving bank"));
		if(myPersonality != PersonType.Crook){
			if(myPersonalBankAccount == null) {
				myPersonalBankAccount = new MyBankAccount(accountNumber, "Personal", theBank, change, loanAmount, loanTime);
			}
			if(change > 0)
				moneyWanted -= change;
			else
				moneyToDeposit = -change;
			myPersonalBankAccount.amount += -change;
			myPersonalBankAccount.loanNeeded = loanAmount;
			myPersonalBankAccount.loanTime = loanTime;
		}
		else {
			myPersonality = PersonType.Normal;
		}
		moneyOnHand += change;
		event = PersonEvent.makingDecision;
		bankState = BankState.None;
		AlertLog.getInstance().logMessage(AlertTag.PERSON, name, "Just left the bank: change, loanAmount, loanTime: "
				+ change + ", " + loanAmount + ", " + loanTime); 
		stateChanged();
	}
	
	// from Restaurant
	public void msgDoneEating(boolean success, double newMoneyOnHand) {
		log.add(new LoggedEvent("Received msgDoneEating()"));
		if(success) {
			isNourished = true;
			moneyOnHand = newMoneyOnHand;
		}
		preferEatAtHome = !preferEatAtHome;
		event = PersonEvent.makingDecision;
		stateChanged();
	}
	
	// from Market
	public void msgHereIsOrder(String order, int quantity) {
		log.add(new LoggedEvent("Received msgHereIsOrder from Market"));
		marketState = MarketState.None;
		event = PersonEvent.makingDecision;
		if(itemsOnHand.get(order) == null)
			itemsOnHand.put(order, quantity);
		else
			itemsOnHand.put(order, itemsOnHand.get(order) + quantity);
		stateChanged();
	}
	
	// ************************* SCHEDULER ***********************************
	
	public boolean pickAndExecuteAnAction() {
		if(printCount > 0) {
			printCount--;
			print("Calling PersonAgent's scheduler: currentStateLocation = " + currentLocationState
					+ "; event = " + event);
		}
		
		if(transportationState == TransportationState.NeedToPayFare) {
			payFare();
			transportationState = TransportationState.None;
			return true;
		}
		
		// action queue for urgent actions
		if((event == PersonEvent.makingDecision || event == PersonEvent.onHold)
				&& actionQueue.size() > 0) {
			Action theAction = actionQueue.poll();
			switch(theAction.action) {
				case becomeHungry:
					isNourished = false; break;
				case wakeUp:
					bodyState = BodyState.Active; break;
				case goToSleep:
					bodyState = BodyState.Tired; break;
				case payRent:
					rentToPay += theAction.amount; break;
				case receiveRent:
					AlertLog.getInstance().logMessage(AlertTag.PERSON, name, "Received rent: " + theAction.amount);
					moneyOnHand += theAction.amount; break;
				case needMaintenance: 
					houseNeedsMaintenance = true; break;
				case goToWork:
					checkGoingToWork((int)theAction.amount); break;
			}
			event = PersonEvent.makingDecision;
			print("returning true from action queue; popped: " + theAction.action + "; size: " + actionQueue.size());
			return true;
		}
		
		// if no urgent actions and not asleep, proceed with normal decision rules
		if(event == PersonEvent.makingDecision && bodyState != BodyState.Asleep) {
			
			if(currentLocationState == LocationState.Home) { // at home
				if(!insideHouse) { // if not inside house (i.e., at the doorstep), enter it
					// I want to be home and I'm not working now
					if(currentLocation.equals(targetLocation) && workplace == null) {
						enterHouse();
						insideHouse = true;
						event = PersonEvent.onHold;
					}
					else {
						// I have to leave
						if(workplace != null)
							targetLocation = workplace.name;
						goToTransportation();
						event = PersonEvent.onHoldInTransportation;
					}
					log.add(new LoggedEvent("Returning true because !insideHouse"));
					print("Returning true because !insideHouse was true");
					return true;
				}
				
				// I have rent to pay
				print("checking rentToPay > 0");
				if(rentToPay > 0) {
					// Ready to go to bank
					if(bankState == BankState.NeedTransaction) {
						leaveHouse();
						event = PersonEvent.onHold;
						return true;
					}
					else {
						// Ready to pay rent
						if(moneyOnHand >= rentToPay) {
							payRent(rentToPay);
							return true;
						}
						else {
							// Have to go to bank
							getRentMoneyFromBank();
							if(isBankOpen)
								return true;
						}
					}
				}
				
				// I have to go to work
				if(workplace != null) {
					leaveHouse();
					event = PersonEvent.onHold;
				}
				
				// I need to do maintenance
				if(houseNeedsMaintenance) {
					doMaintenance();
					event = PersonEvent.onHold;
				}
				
				// I'm hungry
				if(!isNourished) {
					log.add(new LoggedEvent("!isNourished"));
					if(currentLocation.equals(targetLocation)) {
						print("Deciding to eat");
						// I want to eat at home
						if(preferEatAtHome) {
							if(marketState == MarketState.WantToBuy) {
								hungryToMarket();
								if(isBankOpen)
									return true;
							}
							else {
								// I have no food in the fridge
								prepareToCookAtHome();
								event = PersonEvent.onHold;
								return true;
							}
						}
						else {
							hungryToRestaurant();
							if(isBankOpen)
								return true;
						}
					}
					else {
						leaveHouse();
						event = PersonEvent.onHold;
						return true;
					}
					log.add(new LoggedEvent("returning true because !isNourished"));
				}
				
				// I am at home and I have surplus money
				if(currentLocation.equals(targetLocation)) {
					if(isBankOpen && moneyOnHand > MONEY_ON_HAND_LIMIT) {
						haveMoneyToDeposit();
						print("returning true because haveMoneyToDeposit()");
						return true;
					}
				}
				else {
					// I am leaving the house
					leaveHouse();
					event = PersonEvent.onHold;
					print("returning true because isNourished");
					return true;
				}
				
				// I am tired
				if(bodyState == BodyState.Tired) {
					goToSleep();
					bodyState = BodyState.Asleep;
					event = PersonEvent.onHold;
				}
			}
			
			if(currentLocationState == LocationState.Bank) { // at bank
				if(bankState != BankState.WantToWork && !isBankOpen)
					bankState = BankState.None;
				switch(bankState) {
					case NeedTransaction: // Has business at the bank
						if(myPersonalBankAccount == null && myPersonality == PersonType.Crook){
							event = PersonEvent.onHoldInBank;
							tryToRobBank();
						}
						else if(myPersonalBankAccount == null) {
							event = PersonEvent.onHoldInBank;
							requestNewAccount();
						}
						else if(moneyWanted > 0) {
							event = PersonEvent.onHoldInBank;
							requestWithdrawal();
						}
						else if(moneyToDeposit > 0) {
							event = PersonEvent.onHoldInBank;
							requestDeposit();
						}
						break;
					case None: // Done at bank, time to transition
						if(!currentLocation.equals(targetLocation)) {
							goToTransportation();
							event = PersonEvent.onHoldInTransportation;
						}
						else {
							if(!isNourished && !preferEatAtHome) {
								hungryToRestaurant();
							}
							else {
								goHome();
								event = PersonEvent.onHoldInTransportation;
							}
						}
						break;
					case WantToWork:
						goToWorkAtBank();
						event = PersonEvent.onHoldInBank;
						break;
				}
				return true;
			}
			
			if(currentLocationState == LocationState.Restaurant) { // at restaurant
				if( (!isNourished && !preferEatAtHome && restState == RestaurantState.WantToEat)
						|| ((restState == RestaurantState.WantToWork) && currentLocation.equals(workplace.name)) ) {
					enterRestaurant();
					event = PersonEvent.onHoldAtRestaurant;
				}
				else {
					if(workplace != null) {
						targetLocation = workplace.name;
						goToTransportation();
					}
					else
						goHome();
					event = PersonEvent.onHoldInTransportation;
				}
				print("Restaurant: setting on hold");
				return true;
			}
			
			if(currentLocationState == LocationState.Market) { // at market
				
				if(!isNourished && !preferEatAtHome) {
					hungryToRestaurant();
				}
				switch(marketState) {
					case None:
						goHome();
						event = PersonEvent.onHoldInTransportation;
						break;
					case WantToBuy:
						enterMarket();
						event = PersonEvent.onHoldInMarket;
						break;
					case WantToWork:
						goToWorkInMarket();
						event = PersonEvent.onHoldInMarket;
						break;
				}
				print("Market: event onHold set");
				return true;
			}
		}
		
		print("Nothing to do for now: isNourished = " + isNourished
				+ "; currentLocationState = " + currentLocationState.toString()
				+ "; bodyState = " + bodyState
				+ "; personEvent = " + event);
		return false;
	}

	// ************************* ACTIONS ***********************************

	private void checkGoingToWork(int workPeriod) {
		print("Is it time for me to work? Time period: " + workPeriod);
		AlertLog.getInstance().logMessage(AlertTag.PERSON, name, "Is it time for me to work? Time period: " + workPeriod);
		
		MyRestaurant workplaceRestaurant = findWorkplaceRestaurant(workPeriod);
		if(workplaceRestaurant != null) {
			restState = RestaurantState.WantToWork;
			print("I am going to work at restaurant");
			AlertLog.getInstance().logMessage(AlertTag.PERSON, name, "I am going to work at restaurant");
			targetLocation = workplaceRestaurant.name;
			this.workplace = workplaceRestaurant;
			return;
		}
		MyBank workplaceBank = findWorkplaceBank(workPeriod);
		if(workplaceBank != null && !isSunday) {
			bankState = BankState.WantToWork;
			print("I am going to work at bank");
			AlertLog.getInstance().logMessage(AlertTag.PERSON, name, "I am going to work at bank");
			targetLocation = workplaceBank.name;
			this.workplace = workplaceBank;
			return;
		}
		MyMarket workplaceMarket = findWorkplaceMarket(workPeriod);
		if(workplaceMarket != null) {
			marketState = MarketState.WantToWork;
			print("I am going to work at market");
			AlertLog.getInstance().logMessage(AlertTag.PERSON, name, "I am going to work at market");
			targetLocation = workplaceMarket.name;
			this.workplace = workplaceMarket;
			return;
		}
	}

	//House actions
	private void enterHouse() {
		print("Entering house, adding items");
		AlertLog.getInstance().logMessage(AlertTag.PERSON, name, "Entering house, adding items");
		Map<String, Integer> copyOfItems = new HashMap<String, Integer>();
		
		Set<String> keySet = itemsOnHand.keySet();
		String[] keyArray = keySet.toArray(new String[keySet.size()]);
		for(int i = 0; i < keyArray.length; i++) {
			Integer aQuantity = itemsOnHand.get(keyArray[i]);
			if(aQuantity > 0)
				copyOfItems.put(keyArray[i], aQuantity);
			itemsOnHand.put(keyArray[i], 0);
		}
		myHome.housing.msgIAmHome(this, copyOfItems);
	}
	
	private void prepareToCookAtHome() {
		print("I'm hungry and I want to cook at home");
		AlertLog.getInstance().logMessage(AlertTag.PERSON, name, "I'm hungry and I want to cook at home");
		myHome.housing.msgPrepareToCookAtHome(this, foodPreference);
	}
	
	private void getRentMoneyFromBank() {
		print("I need rent money");
		log.add(new LoggedEvent("Need to pay rent; not enough money"));
		AlertLog.getInstance().logMessage(AlertTag.PERSON, name, "I need rent money");
		MyBank targetBank = chooseBank();
		if(targetBank != null) {
			moneyWanted += rentToPay;
			bankState = BankState.NeedTransaction;
			targetLocation = targetBank.name;
		}
		else
			isBankOpen = false;
	}
	
	private void hungryToMarket() {
		print("I'm hungry and I want to buy food at market and cook at home");
		AlertLog.getInstance().logMessage(AlertTag.PERSON, name, "I'm hungry and I want to buy food at market and cook at home");
		MyMarket targetMarket = chooseMarket();
		if(targetMarket != null) {
			spamCounter = SPAM_RESET;
			double price = targetMarket.theMarket.getPrice(foodPreference);
			if(moneyOnHand < price) {
				log.add(new LoggedEvent("Want to buy food at market; not enough money"));
				MyBank targetBank = chooseBank();
				if(targetBank != null) {
					targetLocation = targetBank.name;
					moneyWanted = price - moneyOnHand;
					bankState = BankState.NeedTransaction;
				}
				else
					isBankOpen = false;
			}
			else {
				print("I have enough money to buy food from market");
				AlertLog.getInstance().logMessage(AlertTag.PERSON, name, "I have enough money to buy food from market");
				targetLocation = targetMarket.name;
			}
		}
		else {
			preferEatAtHome = !preferEatAtHome;
			spamCounter--;
			if(spamCounter == 0) {
				spamCounter = SPAM_RESET;
				isNourished = true; // hack to prevent decision paralysis
			}
		}
	}
	
	private void haveMoneyToDeposit() {
		print("I have excess money to deposit");
		AlertLog.getInstance().logMessage(AlertTag.PERSON, name, "I have excess money to deposit");
		log.add(new LoggedEvent("I have excess money to deposit"));
		MyBank targetBank = chooseBank();
		if(targetBank != null) {
			moneyToDeposit = moneyOnHand - MONEY_ON_HAND_LIMIT;
			bankState = BankState.NeedTransaction;
			targetLocation = targetBank.name;
		}
		else
			isBankOpen = false;
	}

	private void doMaintenance() {
		print("Performing maintenance");
		AlertLog.getInstance().logMessage(AlertTag.PERSON, name, "Performing maintenance");
		myHome.housing.msgDoMaintenance(this);
	}
	
	private void payRent(double amount) {
		print("Paying rent");
		AlertLog.getInstance().logMessage(AlertTag.PERSON, name, "Paying rent: " + rentToPay);
		moneyOnHand -= amount;
		rentToPay = 0;
		myHome.housing.msgHereIsRent(this, amount);
	}
	
	private void leaveHouse() {
		print("Leaving house");
		AlertLog.getInstance().logMessage(AlertTag.PERSON, name, "Leaving house");
		myHome.housing.msgIAmLeaving(this);
	}
	
	private void goToSleep() {
		print("Going to sleep");
		AlertLog.getInstance().logMessage(AlertTag.PERSON, name, "Going to sleep");
		myHome.housing.msgGoToBed(this);
	}
	
	//Restaurant actions
	private void hungryToRestaurant() {
		print("I'm hungry and I want to eat at restaurant");
		AlertLog.getInstance().logMessage(AlertTag.PERSON, name, "I'm hungry and I want to eat at restaurant");
		MyRestaurant targetRestaurant = chooseRestaurant();
		
		if(targetRestaurant == null)
			preferEatAtHome = !preferEatAtHome;
		else {
			Map<String, Double> theMenu = targetRestaurant.menu;
			double lowestPrice = getLowestPrice(theMenu);
			if(moneyOnHand < lowestPrice) {
				log.add(new LoggedEvent("Want to eat at restaurant; not enough money"));
				MyBank targetBank = chooseBank();
				if(targetBank != null) {
					moneyWanted = lowestPrice - moneyOnHand;
					bankState = BankState.NeedTransaction;
					targetLocation = targetBank.name;
				}
				else
					isBankOpen = false;
			}
			else {
				print("I have enough money to buy from restaurant");
				log.add(new LoggedEvent("I have enough money to buy from restaurant"));
				restState = RestaurantState.WantToEat;
				targetLocation = targetRestaurant.name;
			}
		}
	}
	
	private void enterRestaurant() {
		print("Entering restaurant");
		AlertLog.getInstance().logMessage(AlertTag.PERSON, name, "Entering restaurant");
		MyRestaurant myRest = (MyRestaurant)currentMyObject;
		if (myRest.personType == "Waiter" ) print("going to restaurant as waiter " + " i am " + this.getName());
		myRest.restaurant.personAs(this, myRest.personType, name, moneyOnHand);
	}
	
	//Transportation actions
	private void goToTransportation() {
		print("Going from " + currentLocation + " to " + targetLocation);
		log.add(new LoggedEvent("Going from " + currentLocation + " to " + targetLocation));
		AlertLog.getInstance().logMessage(AlertTag.PERSON, name, "Going from " + currentLocation + " to " + targetLocation);
		transportation.msgWantToGo(currentLocation, targetLocation, this, preferredCommute.name(), "Edgar");
		currentLocationState = LocationState.Transit;
	}
	
	private void goHome() {
		print("Going home");
		AlertLog.getInstance().logMessage(AlertTag.PERSON, name, "Going home");
		log.add(new LoggedEvent("Going home"));
		targetLocation = myHome.name;
		goToTransportation();
	}
	
	private void payFare() {
		print("Paying fare");
		AlertLog.getInstance().logMessage(AlertTag.PERSON, name, "Paying fare");
		transportation.msgPayFare(this, (float)fareToPay);
	}
	
	//Bank actions
	private void tryToRobBank() {
		print("Trying to rob bank");
		MyBank myBank = (MyBank)currentMyObject;
		log.add(new LoggedEvent("Going to rob the bank"));
		AlertLog.getInstance().logMessage(AlertTag.PERSON, name, "Going to rob the bank");
		myBank.bank.msgThief(this, MONEY_TO_ROB, true); // pointer to myself, money, present
	}
	
	private void requestNewAccount() {
		print("Request new account");
		AlertLog.getInstance().logMessage(AlertTag.PERSON, name, "Request new account");
		MyBank myBank = (MyBank)currentMyObject;
		log.add(new LoggedEvent("Creating account"));
		AlertLog.getInstance().logMessage(AlertTag.PERSON, name, "Creating account");
		myBank.bank.msgRequestAccount(this, moneyToDeposit, true); // pointer to myself, money, present
	}
	
	private void requestWithdrawal() {
		print("Request withdrawal");
		AlertLog.getInstance().logMessage(AlertTag.PERSON, name, "Request withdrawal");
		MyBank myBank = (MyBank)currentMyObject;
		log.add(new LoggedEvent("Want to withdraw " + moneyWanted + " from " + myBank.name));
		myBank.bank.msgRequestWithdrawal(this, myPersonalBankAccount.accountNumber, moneyWanted, true);
	}
	
	private void requestDeposit() {
		print("Request deposit");
		AlertLog.getInstance().logMessage(AlertTag.PERSON, name, "Request deposit");
		MyBank myBank = (MyBank)currentMyObject;
		log.add(new LoggedEvent("Want to deposit " + moneyToDeposit + " from " + myBank.name));
		myBank.bank.msgRequestDeposit(this, myPersonalBankAccount.accountNumber, moneyToDeposit, true);
	}
	
	private void goToWorkAtBank() {
		print("Working at bank");
		AlertLog.getInstance().logMessage(AlertTag.PERSON, name, "Working at bank");
		MyBank myBank = (MyBank)currentMyObject;
		myBank.bank.addPerson(this);
	}
	
	//Market actions
	private void enterMarket() {
		print("Entering market");
		AlertLog.getInstance().logMessage(AlertTag.PERSON, name, "Entering market");
		MyMarket myMarket = (MyMarket)currentMyObject;
		myMarket.theMarket.personAs(this, name, moneyOnHand, foodPreference, MARKET_PURCHASE_QUANTITY);
	}
	
	private void goToWorkInMarket() {
		print("Working at market");
		AlertLog.getInstance().logMessage(AlertTag.PERSON, name, "Working at market");
		MyMarket myMarket = (MyMarket)currentMyObject;
		myMarket.theMarket.addPerson(this, myMarket.personType, name);
	}
	
	// ************************* UTILITIES ***********************************
	
	private void mapLocationToEnum(String location) {
		MyObject[] myObjectsArray = getObjects();
		for(int i = 0; i < myObjectsArray.length; i++) {
			MyObject tempObject = myObjectsArray[i];
			if(location.equals(tempObject.name)) {
				if(tempObject instanceof MyHousing)
					currentLocationState = LocationState.Home;
				else if(tempObject instanceof MyBank)
					currentLocationState = LocationState.Bank;
				else if(tempObject instanceof MyRestaurant)
					currentLocationState = LocationState.Restaurant;
				else if(tempObject instanceof MyMarket)
					currentLocationState = LocationState.Market;
				else
					currentLocationState = LocationState.Transit;
				return;
			}
		}
	}
	
	// updates the currentMyObject variable, which denotes what place the person is currently at
	private void updateCurrentMyObject(String location) {
		MyObject[] myObjectsArray = getObjects();
		for(int i = 0; i < myObjectsArray.length; i++) {
			MyObject tempObject = myObjectsArray[i];
			if(location.equals(tempObject.name)) {
				currentMyObject = tempObject;
				return;
			}
		}
	}

	private MyObject mapLocationToWorkplace(String location) {
		MyObject[] myObjectsArray = getObjects();
		for(int i = 0; i < myObjectsArray.length; i++) {
			MyObject tempObject = myObjectsArray[i];
			if(location.equals(tempObject.name)) {
				return tempObject;
			}
		}
		return null;
	}

	private double getLowestPrice(Map<String, Double> theMenu) {
		Collection<Double> prices = theMenu.values();
		Double[] temp = new Double[1];
		Double[] pricesArray = prices.toArray(temp);
		Arrays.sort(pricesArray);
		return pricesArray[0].doubleValue();
	}
	
	private MyRestaurant chooseRestaurant() {
		/*
		 	Follow these rules in order below:
		 	
		 	Are you a deadbeat? Ignore "affordability" conditions below
		 	
		 	Is that restaurant closed? Choose next restaurant
		 	Is this preferred restaurant and is affordable? Overrides any other restaurant, return immediately
		 	Cannot afford to buy cheapest item? If banks are open plan to go to bank, if banks not open choose next restaurant 
			All other criteria are satisfied? Choose the restaurant for now, look at other restaurants, return if no more
		*/
		MyObject[] myObjectsArray = getObjects();
		MyRestaurant chosenRestaurant = null;
		for(int i = 0; i < myObjectsArray.length; i++) {
			if(myObjectsArray[i] instanceof MyRestaurant) {
				MyRestaurant tempRest = (MyRestaurant)myObjectsArray[i];
				
				boolean isOpen = tempRest.restaurant.isOpen();
				boolean isPreferred = tempRest.restaurantType.equals(foodPreference);
				boolean isAffordable = (myPersonality == PersonType.Deadbeat)
						|| (getLowestPrice(tempRest.restaurant.getMenu().menuItems) <= moneyOnHand);

				if(isOpen) {
					if(isPreferred && isAffordable)
						return tempRest;
					if(isAffordable || (!isAffordable && isBankOpen))
						chosenRestaurant = tempRest;
				}
			}
		}
		return chosenRestaurant;
	}
	
	private MyBank chooseBank() {
		ArrayList<MyBank> bankList = new ArrayList<MyBank>();
		MyObject[] myObjectsArray = getObjects();
		for(int i = 0; i < myObjectsArray.length; i++)
			if(myObjectsArray[i] instanceof MyBank) {
				MyBank tempMyBank = (MyBank)myObjectsArray[i];
				if(myPersonalBankAccount != null && myPersonalBankAccount.bank.equals(tempMyBank.bank) && tempMyBank.bank.isOpen())
					return tempMyBank;
				if(tempMyBank.bank.isOpen())
					bankList.add((MyBank)myObjectsArray[i]);
			}
		if(bankList.size() == 0)
			return null;
		int randomInd = getRandomInt(0, bankList.size());
		return bankList.get(randomInd);
	}

	private MyMarket chooseMarket() {
		MyObject[] myObjectsArray = getObjects();
		for(int i = 0; i < myObjectsArray.length; i++)
			if(myObjectsArray[i] instanceof MyMarket){
				MyMarket temp = (MyMarket)myObjectsArray[i];
				if(temp.theMarket.isOpen())
					return temp;
			}
		return null;
	}
	
	private MyRestaurant findWorkplaceRestaurant(int session) {
		MyObject[] myObjectsArray = getObjects();
		for(int i = 0; i < myObjectsArray.length; i++)
			if(myObjectsArray[i] instanceof MyRestaurant) {
				MyRestaurant tempRest = (MyRestaurant)myObjectsArray[i];
				if(tempRest.workSession == session) {
					return tempRest;
				}
			}
		return null;
	}
	
	private MyBank findWorkplaceBank(int session) {
		MyObject[] myObjectsArray = getObjects();
		for(int i = 0; i < myObjectsArray.length; i++)
			if(myObjectsArray[i] instanceof MyBank) {
				MyBank temp = (MyBank)myObjectsArray[i];
				if(temp.workSession == session) {
					return temp;
				}
			}
		return null;
	}
	
	private MyMarket findWorkplaceMarket(int session) {
		MyObject[] myObjectsArray = getObjects();
		for(int i = 0; i < myObjectsArray.length; i++)
			if(myObjectsArray[i] instanceof MyMarket) {
				MyMarket temp = (MyMarket)myObjectsArray[i];
				if(temp.workSession == session) {
					return temp;
				}
			}
		return null;
	}
	
	private MyObject[] getObjects() {
		return (MyObject[])myObjects.toArray(new MyObject[myObjects.size()]);
	}
	
	private int getRandomInt(int min, int max) {
		return (int)((Math.random() * (double)max)+ (double)min);
	}
	
	// ************************* WRAPPER CLASSES ***********************************
	
	private class MyObject {
		String name;
	}
	
	private class MyHousing extends MyObject {
		Housing_Douglass housing;
		String occupantType;
		Map<String, Integer> inventory = new HashMap<String, Integer>();
		
		public MyHousing(Housing_Douglass h, String housingName, String occupantType) {
			housing = h;
			this.name = housingName;
			this.occupantType = occupantType;
		}
	}
	
	private class MyBankAccount extends MyObject {
		MyBank theBank;
		String accountType;
		Bank_Douglass bank;
		int accountNumber;
		double amount = 0, loanNeeded = 0;
		int loanTime;
		
		public MyBankAccount(int accountNumber, String accountType, Bank_Douglass bank, double amount, double loanNeeded, int loanTime) {
			this.name = "Account " + accountNumber;
			this.accountType = accountType;
			this.bank = bank;
			this.accountNumber = accountNumber;
			this.amount = amount;
			this.loanNeeded = loanNeeded;
			this.loanTime = loanTime;
		}
	}
	// Customers, Waiters, Host, Cook, Cashier
	private class MyRestaurant extends MyObject {
		Restaurant restaurant;
		String restaurantType, personType;
		Map<String, Double> menu = new HashMap<String, Double>();
		int workSession; // 0 = not working; 1 = at work state 1; 2 = at work state 2
		
		public MyRestaurant(Restaurant r, String restaurantName, String restaurantType, String personType, Hashtable<String, Double> menu, int workSession) {
			restaurant = r;
			this.name = restaurantName;
			this.restaurantType = restaurantType;
			this.personType = personType;
			this.menu = menu;
			this.workSession = workSession;
		}
	}
	
	private class MyBank extends MyObject {
		
		Bank_Douglass bank;
		String personType;
		int workSession;
		public MyBank(Bank_Douglass b, String name, String type, int workSession) {
			bank = b;
			this.name = name;
			personType = type;
			this.workSession = workSession;
		}
	}
	
	private class MyMarket extends MyObject {
		Market theMarket;
		String personType;
		int workSession;
		public MyMarket(Market m, String name, String type, int workSession) {
			theMarket = m;
			this.name = name;
			personType = type;
			this.workSession = workSession;
		}
	}
	
	// 0 = emergency (highest priority)
	// 1 = urgent
	// 2 = necessary
	// 3 = not really needed at the moment
	private class Action implements Comparable {
		ActionString action;
		int priority;
		double amount;
		public Action(ActionString a, int p, double d) {
			action = a;
			priority = p;
			amount = d;
		}
		@Override
		public int compareTo(Object arg) {
			Action other = (Action)arg;
			return priority - other.priority;
		}
	}
}
