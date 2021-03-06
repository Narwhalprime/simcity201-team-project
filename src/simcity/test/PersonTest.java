package simcity.test;

import java.util.*;

import simcity.PersonAgent;
import simcity.Restaurant;
import simcity.test.mock.*;
import junit.framework.*;

/**
 * Unit test code for Person.
 */
public class PersonTest extends TestCase
{
	// instantiated in the setUp() method
	PersonAgent person;
	PersonAgent dummyHost, dummyWaiter, dummyCook, dummyCashier;
	PersonAgent renter, owner;
	MockHousing_Douglass mockHousing1, mockHousing2;
	MockBank_Douglass mockBank;
	MockRestaurant_Douglass mockRestaurant;
	MockTransportation_Douglass mockTransportation;
	MockMarket_Douglass mockMarket;
	
	// custom test variables
	
	public void setUp() throws Exception {
		super.setUp();
		
		Map<String, Double> menu = new HashMap<String, Double>();
		menu.put("Beef with brocolli", 10.00);
		menu.put("Orange chicken", 13.00);
		menu.put("Hot and sour soup", 8.00);
		
		mockHousing1 = new MockHousing_Douglass("Mock House 1");
		mockHousing2 = new MockHousing_Douglass("Mock House 2");
		
		mockBank = new MockBank_Douglass("Mock Bank 1");
		mockRestaurant = new MockRestaurant_Douglass("Mock Restaurant 1", "Chinese", menu);
		mockTransportation = new MockTransportation_Douglass("Mock Transportation");

		dummyHost = new PersonAgent("Dummy Host", mockHousing2, 0, "Italian", false, "OwnerResident", mockTransportation, 'W');
		dummyWaiter = new PersonAgent("Dummy Waiter", mockHousing2, 0, "Italian", false, "Renter", mockTransportation, 'W');
		dummyCook = new PersonAgent("Dummy Cook", mockHousing2, 0, "Italian", false, "Renter", mockTransportation, 'W');
		dummyCashier = new PersonAgent("Dummy Cashier", mockHousing2, 0, "Italian", false, "Renter", mockTransportation, 'W');
		mockRestaurant.personAs(dummyHost, "Host", "Dummy Host", 0);
		mockRestaurant.personAs(dummyWaiter, "Waiter", "Dummy Waiter", 0);
		mockRestaurant.personAs(dummyCook, "Coook", "Dummy Cook", 0);
		mockRestaurant.personAs(dummyCashier, "Cashier", "Dummy Cashier", 0);
		
		person = new PersonAgent("Narwhal Prime", mockHousing1, 0, "Chinese", false, "OwnerResident", mockTransportation, 'W');
		
		renter = new PersonAgent("Renter", mockHousing2, 50, "Chinese", false, "OwnerResident", mockTransportation, 'W');
		owner = new PersonAgent("Owner", mockHousing2, 50, "Chinese", false, "Resident", mockTransportation, 'W');
	}	
	
	// TEST #1a
	// Person: leave house, walk to bank, withdraw money, walk to restaurant,
	// run restaurant scenario (successfully eat and pay), return home
	public void testNormative_HomeBankRestaurantHome() {
		
		// setup
		person.setMoney(5);
		person.setFoodPreference("Chinese", false);
		person.setIsNourished(false);
		person.addBank(mockBank, "Customer", 0);
		person.addHousing(mockHousing1, "OwnerResident"); // TODO: There are three types; OwnerResident, Owner, Renter
		person.addRestaurant(mockRestaurant, "Customer", 0);
		
		// step 1 pre-conditions
		assertEquals("Person: 5 dollars at start",
				5.00, person.getMoney());
		assertFalse("Person: not nourished at start", 
				person.getIsNourished());
		assertEquals("Restaurant: 3 food items", 
				3, mockRestaurant.getMenu().menuItems.size());
		
		// step 1: person wants to go to restaurant, needs money first
			// step 1a: person tells transportation that he wants to go to restaurant
			// step 1b: transportation sends person in transit
			// step 1c: person arrives at bank
		person.msgWakeUp();
		assertTrue("Call scheduler, wake up, scheduler returns true",
				person.pickAndExecuteAnAction());
		
		// step 1 post-conditions and step 2 pre-conditions
		assertEquals("Person: 1 event logs",
				1, person.log.size());
		assertTrue("Contains log: Must wake up",
				person.log.containsString("Must wake up"));
		assertTrue("Call scheduler, enter restaurant, scheduler returns true",
				person.pickAndExecuteAnAction());
		
		person.msgDoneEntering();
		assertEquals("Person: 3 event logs",
				3, person.log.size());
		assertTrue("Contains log: Returning true because !insideHouse",
				person.log.containsString("Returning true because !insideHouse"));
		assertTrue("Contains log: msgDoneEntering() called",
				person.log.containsString("msgDoneEntering() called"));
		assertTrue("Person: event = makingDecision", 
				person.event.toString().equals("makingDecision"));
		
		person.msgSetBanksOpen(true);
		assertTrue("Call scheduler, deciding hunger, scheduler returns true",
				person.pickAndExecuteAnAction());
		assertTrue("Call scheduler, query restaurants, not enough money, scheduler returns true",
				person.pickAndExecuteAnAction());
		assertTrue("Contains log: want to go to restaurant but not enough money",
				person.log.containsString("!isNourished"));
		assertTrue("Contains log: want to go to restaurant but not enough money",
				person.log.containsString("Want to eat at restaurant; not enough money"));		
		
		person.msgDoneLeaving(); // leaving house to go get food
		assertEquals("Person: 8 event logs",
				8, person.log.size());
		assertTrue("Person: event = makingDecision", 
				person.event.toString().equals("makingDecision"));
		
		assertTrue("Call scheduler, set target destination to bank, scheduler returns true",
				person.pickAndExecuteAnAction());
		assertTrue("Contains log: describes going from house to bank",
				person.log.containsString("Going from Mock House 1 to Mock Bank 1"));
		
		assertFalse("Call scheduler, in transit, on hold, scheduler returns false",
				person.pickAndExecuteAnAction());
		assertEquals("Mock House 1", person.getCurrLocation());
		
		//mockTransportation.msgWantToGo(person.getCurrLocation(), "Mock Bank 1", person, "method", "Edgar");
		long startTime = System.currentTimeMillis();
		while(System.currentTimeMillis() - startTime < 200);

		// step 2: person is at bank, withdraws money from account
			// step 2a: person requests withdrawal from bank, blocks
			// TODO: step 2b: bank checks if withdrawal is valid
			// step 2b: after brief delay, bank messages that withdrawal approved
			// step 2c: person receives money, gets released
		
		assertEquals("Mock Bank 1", person.getCurrLocation());
		assertEquals("Transportation: 2 event log",
				2, mockTransportation.log.size());
		assertTrue("", person.log.containsString("Received msgReachedDestination: destination = Mock Bank 1"));
		
		assertTrue("Call scheduler, enter bank to create account, scheduler returns true",
				person.pickAndExecuteAnAction());
		
		assertTrue("Person: creates account",
				person.log.containsString("Creating account"));
		
		assertTrue("Call scheduler, now realizes need to withdraw money, scheduler returns true",
				person.pickAndExecuteAnAction());
		assertTrue("Call scheduler, the actual withdraw money action, scheduler returns true",
				person.pickAndExecuteAnAction());

		// step 2 post-conditions and step 3 pre-conditions
		
		assertEquals("Person: currentLocation = Mock Bank 1",
				"Mock Bank 1", person.getCurrLocation());
		assertEquals("Person: currentLocationState = Bank",
				"Bank", person.getCurrLocationState());
		assertTrue("Person: event = makingDecision", 
				person.event.toString().equals("makingDecision"));
		
		assertEquals("Person: 16 event logs",
				16, person.log.size());
		
		assertEquals("Person: has 8 dollars", 
				8.00, person.getMoney());
		assertTrue("Bank log: Received msgRequestWithdrawal(): amount = 3.0",
				mockBank.log.containsString("Received msgRequestWithdrawal(): amount = 3.0"));
		
		assertEquals("Bank: 1 event log",
				1, mockBank.log.size());
		
		assertEquals("Person: currentLocation = Mock Bank 1",
				"Mock Bank 1", person.getCurrLocation());
		assertEquals("Person: currentLocationState = Bank",
				"Bank", person.getCurrLocationState());
		
		// step 3a: person now goes to restaurant
		assertTrue("Call scheduler, want to go to restaurant, scheduler returns true",
				person.pickAndExecuteAnAction());
		assertTrue("Call scheduler, go to restaurant, scheduler returns true",
				person.pickAndExecuteAnAction());
		
		startTime = System.currentTimeMillis();
		while(System.currentTimeMillis() - startTime < 200);
		
		assertEquals("Person: currentLocation = Mock Restaurant 1",
				"Mock Restaurant 1", person.getCurrLocation());
		assertEquals("Person: currentLocationState = Restaurant",
				"Restaurant", person.getCurrLocationState());
		assertTrue("Person: event = makingDecision", 
				person.event.toString().equals("makingDecision"));
		assertTrue("Person: currently hungry",
				!person.getIsNourished());
		
		// step 3b: go eat
		assertTrue("Call scheduler, enter restaurant, scheduler returns true",
				person.pickAndExecuteAnAction());
		startTime = System.currentTimeMillis();
		while(System.currentTimeMillis() - startTime < 800);
		assertTrue("Person: currently full",
				person.getIsNourished());
		
		// step 3 post-conditions and step 4 pre-conditions
		
		// step 4: going home
		
		assertTrue("Call scheduler, time to go home",
				person.pickAndExecuteAnAction());
		startTime = System.currentTimeMillis();
		while(System.currentTimeMillis() - startTime < 200);
		
		assertEquals("Person: currentLocation = Mock House 1",
				"Mock House 1", person.getCurrLocation());
		assertEquals("Person: currentLocationState = Home",
				"Home", person.getCurrLocationState());
		assertTrue("Person: event = makingDecision", 
				person.event.toString().equals("makingDecision"));
		
		// step 4 post-conditions
		assertEquals("Person: has 0 dollars", 
				0.00, person.getMoney());
	}
	
	// Test 1b
	// Similar to previous test with the following variations:
	// The item purchased is more expensive, and person will have more mon
	// Transportation method of the person is now car instead of walking
	public void testNormative_HomeBankRestaurantHome2() {
		
		// setup
		person = new PersonAgent("Narwhal Prime", mockHousing1, 0, "Chinese", false, "OwnerResident", mockTransportation, 'C');
		person.setMoney(5);
		person.setFoodPreference("Chinese", false);
		person.setIsNourished(false);
		person.addBank(mockBank, "Customer", 0);
		person.addHousing(mockHousing1, "OwnerResident"); // TODO: There are three types; OwnerResident, Owner, Renter
		person.addRestaurant(mockRestaurant, "Customer", 0);
		
		// step 1 pre-conditions
		assertEquals("Person: 5 dollars at start",
				5.00, person.getMoney());
		assertFalse("Person: not nourished at start", 
				person.getIsNourished());
		assertEquals("Restaurant: 3 food items", 
				3, mockRestaurant.getMenu().menuItems.size());
		
		// step 1: person wants to go to restaurant, needs money first
			// step 1a: person tells transportation that he wants to go to restaurant
			// step 1b: transportation sends person in transit
			// step 1c: person arrives at bank
		person.msgWakeUp();
		assertTrue("Call scheduler, wake up, scheduler returns true",
				person.pickAndExecuteAnAction());
		
		// step 1 post-conditions and step 2 pre-conditions
		assertEquals("Person: 1 event logs",
				1, person.log.size());
		assertTrue("Contains log: Must wake up",
				person.log.containsString("Must wake up"));
		assertTrue("Call scheduler, enter restaurant, scheduler returns true",
				person.pickAndExecuteAnAction());
		
		person.msgDoneEntering();
		assertEquals("Person: 3 event logs",
				3, person.log.size());
		assertTrue("Contains log: Returning true because !insideHouse",
				person.log.containsString("Returning true because !insideHouse"));
		assertTrue("Contains log: msgDoneEntering() called",
				person.log.containsString("msgDoneEntering() called"));
		assertTrue("Person: event = makingDecision", 
				person.event.toString().equals("makingDecision"));
		
		person.msgSetBanksOpen(true);
		assertTrue("Call scheduler, deciding hunger, scheduler returns true",
				person.pickAndExecuteAnAction());
		assertTrue("Call scheduler, query restaurants, not enough money, scheduler returns true",
				person.pickAndExecuteAnAction());
		assertTrue("Contains log: want to go to restaurant but not enough money",
				person.log.containsString("!isNourished"));
		assertTrue("Contains log: want to go to restaurant but not enough money",
				person.log.containsString("Want to eat at restaurant; not enough money"));		
		
		person.msgDoneLeaving(); // leaving house to go get food
		assertEquals("Person: 8 event logs",
				8, person.log.size());
		assertTrue("Person: event = makingDecision", 
				person.event.toString().equals("makingDecision"));
		
		assertTrue("Call scheduler, set target destination to bank, scheduler returns true",
				person.pickAndExecuteAnAction());
		assertTrue("Contains log: describes going from house to bank",
				person.log.containsString("Going from Mock House 1 to Mock Bank 1"));
		
		assertFalse("Call scheduler, in transit, on hold, scheduler returns false",
				person.pickAndExecuteAnAction());
		assertEquals("Mock House 1", person.getCurrLocation());
		
		//mockTransportation.msgWantToGo(person.getCurrLocation(), "Mock Bank 1", person, "method", "Edgar");
		long startTime = System.currentTimeMillis();
		while(System.currentTimeMillis() - startTime < 200);

		// step 2: person is at bank, withdraws money from account
			// step 2a: person requests withdrawal from bank, blocks
			// TODO: step 2b: bank checks if withdrawal is valid
			// step 2b: after brief delay, bank messages that withdrawal approved
			// step 2c: person receives money, gets released
		
		assertEquals("Mock Bank 1", person.getCurrLocation());
		assertEquals("Transportation: 2 event logs",
				2, mockTransportation.log.size());
		assertTrue("", person.log.containsString("Received msgReachedDestination: destination = Mock Bank 1"));
		
		assertTrue("Call scheduler, enter bank to create account, scheduler returns true",
				person.pickAndExecuteAnAction());
		
		assertTrue("Person: creates account",
				person.log.containsString("Creating account"));
		
		assertTrue("Call scheduler, withdraw money, scheduler returns true",
				person.pickAndExecuteAnAction());
		assertTrue("Call scheduler, actual withdraw money action, scheduler returns true",
				person.pickAndExecuteAnAction());

		// step 2 post-conditions and step 3 pre-conditions
		
		assertEquals("Personis currently at Mock Bank 1",
				"Mock Bank 1", person.getCurrLocation());
		assertEquals("Person: currentLocationState = Bank",
				"Bank", person.getCurrLocationState());
		assertTrue("Person: event = makingDecision", 
				person.event.toString().equals("makingDecision"));
		
		assertEquals("Person: 16 event logs",
				16, person.log.size());
		
		assertEquals("Person: has 8 dollars", 
				8.00, person.getMoney());
		assertTrue("Bank log: Received msgRequestWithdrawal() of 3.0",
				mockBank.log.containsString("Received msgRequestWithdrawal(): amount = 3.0"));
		
		assertEquals("Bank: 1 event log",
				1, mockBank.log.size());
		
		assertEquals("Person: currentLocation = Mock Bank 1",
				"Mock Bank 1", person.getCurrLocation());
		assertEquals("Person: currentLocationState = Bank",
				"Bank", person.getCurrLocationState());
		
		// step 3a: person now goes to restaurant
		person.setMoney(person.getMoney() + 3);
		assertTrue("Call scheduler, want to go to restaurant, scheduler returns true",
				person.pickAndExecuteAnAction());
		assertTrue("Call scheduler, go to restaurant, scheduler returns true",
				person.pickAndExecuteAnAction());
		
		startTime = System.currentTimeMillis();
		while(System.currentTimeMillis() - startTime < 200);
		
		assertEquals("Person: currentLocation = Mock Restaurant 1",
				"Mock Restaurant 1", person.getCurrLocation());
		assertEquals("Person: currentLocationState = Restaurant",
				"Restaurant", person.getCurrLocationState());
		assertTrue("Person: event = makingDecision", 
				person.event.toString().equals("makingDecision"));
		assertTrue("Person: currently hungry",
				!person.getIsNourished());
		
		// step 3b: go eat
		assertTrue("Call scheduler, enter restaurant, scheduler returns true",
				person.pickAndExecuteAnAction());
		startTime = System.currentTimeMillis();
		while(System.currentTimeMillis() - startTime < 800);
		assertTrue("Person: currently full",
				person.getIsNourished());
		
		// step 3 post-conditions and step 4 pre-conditions
		
		// step 4: going home
		
		assertTrue("Call scheduler, time to go home",
				person.pickAndExecuteAnAction());
		startTime = System.currentTimeMillis();
		while(System.currentTimeMillis() - startTime < 200);
		
		assertEquals("Person: currentLocation = Mock House 1",
				"Mock House 1", person.getCurrLocation());
		assertEquals("Person: currentLocationState = Home",
				"Home", person.getCurrLocationState());
		assertTrue("Person: event = makingDecision", 
				person.event.toString().equals("makingDecision"));
		
		// step 4 post-conditions
		assertEquals("Person: has 1 dollars", 
				1.00, person.getMoney());
	}

	// TEST #2
	// Person: start with surplus money, go to bank, ROB THE BANK, deposit money, done
	public void testNormative_SurplusMoney() {
		// setup
		person.setMoney(500);
		person.setIsNourished(true);
		person.addBank(mockBank, "Customer", 0);
		person.addHousing(mockHousing1, "OwnerResident");
		
		assertEquals("Person: 500 dollars at start",
				500.00, person.getMoney());
		assertTrue("Person: nourished at start", 
				person.getIsNourished());
		
		// step 1: wake up
		person.msgWakeUp();
		assertTrue("Call scheduler, wake up, scheduler returns true",
				person.pickAndExecuteAnAction());
		
		assertEquals("Person: 1 event logs",
				1, person.log.size());
		assertTrue("Contains log: Must wake up",
				person.log.containsString("Must wake up"));
		assertTrue("Call scheduler, enter restaurant, scheduler returns true",
				person.pickAndExecuteAnAction());
		
		// step 2: transportation

		person.msgDoneEntering();
		assertEquals("Person: 3 event logs",
				3, person.log.size());
		assertTrue("Contains log: Returning true because !insideHouse",
				person.log.containsString("Returning true because !insideHouse"));
		assertTrue("Contains log: msgDoneEntering() called",
				person.log.containsString("msgDoneEntering() called"));
		assertTrue("Person: event = makingDecision", 
				person.event.toString().equals("makingDecision"));
		
		person.msgSetBanksOpen(true);
		assertTrue("Call scheduler, deciding hunger, scheduler returns true",
				person.pickAndExecuteAnAction());
		assertTrue("Call scheduler, query restaurants, have enough money, scheduler returns true",
				person.pickAndExecuteAnAction());
		
		assertTrue("Contains log: I have excess money to deposit",
				person.log.containsString("I have excess money to deposit"));
		
		person.msgDoneLeaving();
		assertEquals("Person: 6 event logs",
				6, person.log.size());
		assertTrue("Person: event = makingDecision", 
				person.event.toString().equals("makingDecision"));
		
		assertTrue("Call scheduler, set target destination to bank, scheduler returns true",
				person.pickAndExecuteAnAction());
		assertTrue("Contains log: describes going from house to bank",
				person.log.containsString("Going from Mock House 1 to Mock Bank 1"));
		
		assertFalse("Call scheduler, in transit, on hold, scheduler returns false",
				person.pickAndExecuteAnAction());
		assertEquals("Mock House 1", person.getCurrLocation());
		
		long startTime = System.currentTimeMillis();
		while(System.currentTimeMillis() - startTime < 200);

		// step 2: person is at bank, deposits money into account
			// step 2a: person requests account from bank, blocks
			// step 2b: after brief delay, bank messages that deposit amd new account are approved
			// step 2c: person gets updated and released
		
		assertEquals("Mock Bank 1", person.getCurrLocation());
		assertEquals("Transportation: 2 event log",
				2, mockTransportation.log.size());
		assertTrue("", person.log.containsString("Received msgReachedDestination: destination = Mock Bank 1"));
		
		assertTrue("Call scheduler, enter bank to create account, scheduler returns true",
				person.pickAndExecuteAnAction());
		assertTrue("Person: creates account",
				person.log.containsString("Creating account"));
		
		assertTrue("Call scheduler, now realizes need to go to bank, scheduler returns true",
				person.pickAndExecuteAnAction());
		assertFalse("Call scheduler, should be right outside his home, scheduler returns false",
				person.pickAndExecuteAnAction());
		assertEquals("Person: has 920 dollars",
				920.0, person.getMoney());

		// step 2 post-conditions and step 3 pre-conditions
		
		assertEquals("Person: currentLocation = Mock Bank 1",
				"Mock Bank 1", person.getCurrLocation());
		assertEquals("Person: currentLocationState = Transit",
				"Transit", person.getCurrLocationState());
		assertTrue("Person: event = onHoldInTransportation", 
				person.event.toString().equals("onHoldInTransportation"));
		
		assertEquals("Person: 13 event logs",
				13, person.log.size());
	}
	
	// TEST #3
	// Person: perform maintenance, return to original default position, done
	public void testNormative_HomeMaintenance() {
		person = new PersonAgent("Narwhal Prime", mockHousing1, 0, "Chinese", false, "OwnerResident", mockTransportation, 'C');
		person.setMoney(5);
		person.setFoodPreference("Chinese", false);
		person.setIsNourished(false);
		person.addBank(mockBank, "Customer", 0);
		person.addHousing(mockHousing1, "OwnerResident"); // TODO: There are three types; OwnerResident, Owner, Renter
		person.addRestaurant(mockRestaurant, "Customer", 0);
		
		// step 1 pre-conditions
		assertEquals("Person: 5 dollars at start",
				5.00, person.getMoney());
		assertFalse("Person: not nourished at start", 
				person.getIsNourished());
		assertEquals("Restaurant: 3 food items", 
				3, mockRestaurant.getMenu().menuItems.size());
		
		// step 1: perform maintenance
		person.msgNeedMaintenance();
		
		assertTrue("Contains log: msgNeedMaintenance() called",
				person.log.containsString("msgNeedMaintenance() called"));
	}
}
