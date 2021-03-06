package restaurant_bayou;

import agent_bayou.Agent;
import restaurant_bayou.gui.CashierGui;
import restaurant_bayou.gui.CookGui;
import restaurant_bayou.gui.RestaurantBayou;
import restaurant_bayou.gui.WaiterGui;
import simcity.PersonAgent;
import simcity.RestMenu;
import simcity.interfaces.Person;

import java.util.*;
import java.util.concurrent.Semaphore;

/**
 * Restaurant Host Agent
 */

//A Host is the manager of a restaurant who sees that all
//is proceeded as he wishes.

public class HostAgent extends Agent {
	
	static final int NTABLES = 3;
	static final int NWAITERS = 0;
	public List<CustomerAgent> customers =  Collections.synchronizedList(new ArrayList<CustomerAgent>());
	public List<CustomerAgent> waitingCustomers =  Collections.synchronizedList(new ArrayList<CustomerAgent>());
	public List<CustomerAgent> unseatedCustomers =  Collections.synchronizedList(new ArrayList<CustomerAgent>());
	public List<WaiterAgent> waiters =  Collections.synchronizedList(new ArrayList<WaiterAgent>());
	public RestMenu menu = new RestMenu();
	public Collection<Table> tables;
	public CashierGui cashierGui = null;
	public CookGui cookGui = null;
	public CookAgent cook;
	public CashierAgent cashier;
	private String name;
	private Person person;
	boolean shiftDone = false;
	RestaurantBayou restaurant;
	public boolean isWorking = true;
	double wage;
	
	public HostAgent(String name, RestaurantBayou rest) {
		super();

		this.name = name;
		this.restaurant = rest;

		tables = new ArrayList<Table>(NTABLES);
		for (int ix = 1; ix <= NTABLES; ix++) {
			tables.add(new Table(ix));
		}

		waiters = new ArrayList<WaiterAgent>(NWAITERS);
		for (int ix = 1; ix <= NWAITERS; ix++) {
			waiters.add(new WaiterAgentNorm("le waiter "+ix, restaurant));
		}

		
		
	}

	public String getName() {
		return name;
	}
	
	public void setPerson(Person p) {
		person = p;
	}

	public List getCustomers() {
		return customers;
	}

	public Collection getTables() {
		return tables;
	}
	
	public List getWaiters() {
		return waiters;
	}
	
	public void msgShiftDone(double w) {
		print("got msg shift done");
		shiftDone = true;
		isWorking = false;
		wage = w;
		stateChanged();
	}
		
	public void msgIAmHere(CustomerAgent cust) {
		print("bayou host got message i am here");
		waitingCustomers.add(cust);
		for (CustomerAgent c: customers) {
			if (c == cust) {
				stateChanged();
				return;
			}
		}
		customers.add(cust);
		stateChanged();
	}
	
	public void msgTableIsFree(Table t) {
		t.setUnoccupied();
		stateChanged();
	}
	
	public void msgIAmReady() {
		stateChanged();
	}
	
	public void msgWantToGoOnBreak(WaiterAgent w) {
		if (approveBreak(w)) {
			Do("Break approved!");
			w.msgOKToGoOnBreak(true);
		} else {
			Do("Break not approved.");
			w.msgOKToGoOnBreak(false);
		}
		stateChanged();
	}
	
	public void msgBackFromBreak(WaiterAgent w) {
		stateChanged();
	}

	/**
	 * Scheduler.  Determine what action is called for, and do it.
	 */
	protected boolean pickAndExecuteAnAction() {
		/* Think of this next rule as:
            Does there exist a table, customer and waiter,
            such that table is unoccupied, customer is waiting, and waiter is ready.
            If so, tell waiter to seat customer at table.
		 */
		if (waitingCustomers.size() == 0 && shiftDone == true) {if (person!=null) person.msgStopWork(10); print("host going home");} 

		synchronized(waitingCustomers){
			for (CustomerAgent c: waitingCustomers) {
				Collections.sort(waiters, new Comparator<WaiterAgent>() {
				    public int compare(WaiterAgent w1, WaiterAgent w2) {
				        return w1.getCustomers() - w2.getCustomers();
				    }});
				synchronized(waiters){
					for (WaiterAgent w: waiters) {
						if (w.isReady()) {
							synchronized(tables){
								for (Table t: tables) {
									if (!t.isOccupied()) {
										t.setOccupant(c);
										w.msgSeatCustomer(c, menu);
										waitingCustomers.remove(c);
										return true;
									}
								}
							}
						}
					}
				}
			}
		}
		if (shiftDone == true && waitingCustomers.size()==0) {leaveWork();} 
		return false;
		//we have tried all our rules and found
		//nothing to do. So return false to main loop of abstract agent
		//and wait.
	}

	// Actions
	
	private void leaveWork() {
		if (person!=null) person.msgStopWork(wage);
		for (WaiterAgent w : waiters) {
			w.msgShiftDone(true, wage);
		}	
	}
	
	public void addWaiter(WaiterAgent w) {
		//WaiterAgent w = new WaiterAgent("W"+(int)(waiters.size()+1), this);
		waiters.add(w);
		stateChanged();
		//return w;
	}
	
	public void addMarket(MarketAgent mkt) {
		//MarketAgent mkt = new MarketAgent("M"+(int)(cook.numMarkets()+1), menu);
		cook.addMarket(mkt);
		//return mkt;
	}
	
	public boolean approveBreak(WaiterAgent w) {
		if (!w.isReady()) return false;
		for (WaiterAgent wa: waiters) {
			if (wa != w && !wa.isOnBreak()) return true;
		}
		return false;
	}
		
	public void setGui(CashierGui gui) {
		cashierGui = gui;
	}
	
	public void setCashier(CashierAgent c) {
		cashier = c;
	}
	
	public void setGui(CookGui gui) {
		cookGui = gui;
	}

	public CashierGui getGui() {
		return cashierGui;
	}
	
	public boolean isFull() {
		for (Table t: tables) {
			if (!t.isOccupied()) {
				return false;
			}
		}
		return true;
	}

	public class Table {
		CustomerAgent occupiedBy;
		int num;

		Table(int number) {
			this.num = number;
		}

		void setOccupant(CustomerAgent cust) {
			occupiedBy = cust;
		}
		
		void setUnoccupied() {
			occupiedBy = null;
		}

		CustomerAgent getOccupant() {
			return occupiedBy;
		}

		boolean isOccupied() {
			return occupiedBy != null;
		}
		
	}
	
	public class Menu {
		Hashtable<String, Double> menuItems = new Hashtable<String, Double>();
		List<String> menuNames = new ArrayList<String>();
		public void add(String name, double cost) {
			menuItems.put(name, cost);
			menuNames.add(name);
		}
		public double getCost(String name) {
			return menuItems.get(name);
		}
	}
	
	public int howManyWaiters() {
		return waiters.size();
	}
}

