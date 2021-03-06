package restaurant_haus;

import agent_haus.Agent;
import restaurant_haus.gui.RestaurantHaus;
import restaurant_haus.gui.WaiterGui;
import restaurant_haus.interfaces.Cashier;
import restaurant_haus.interfaces.Customer;
import restaurant_haus.interfaces.Waiter;
import simcity.PersonAgent;
import simcity.interfaces.Market_Douglass;
import simcity.interfaces.Person;

import java.util.*;
import java.util.concurrent.Semaphore;

/**
 * Restaurant Host Agent
 */
//We only have 2 types of agents in this prototype. A customer and an agent that
//does all the rest. Rather than calling the other agent a waiter, we called him
//the HostAgent. A Host is the manager of a restaurant who sees that all
//is proceeded as he wishes.
public class CashierAgent extends Agent implements Cashier{

	private String name;
	public double money;
	public Person person;
	boolean shiftDone = false;
	RestaurantHaus rest;
	double wage;
	public boolean isWorking;


	Menu m;
	public class Check {
		Waiter w;
		public Customer c;
		String choice;
		public double price;
		public double payment = 0.00f;
		public CheckState s;

		public Check(Waiter w, String choice, Customer c) {
			this.w = w;
			this.choice = choice;
			this.c = c;
			s = CheckState.Requested;
			price = m.checkPrice(choice);
		}
	}

	public List<Check> checks = Collections.synchronizedList(new ArrayList<Check>());//Changed to public for testing
	public enum CheckState {Requested, WithWaiter, Payed};

	public class Bill {
		Market_Douglass market;
		public double bill;
		public BillState state;

		Bill(Market_Douglass market, double bill) {
			this.market = market;
			this.bill = bill;
			state = BillState.Outstanding;
		}
	}

	public List<Bill> bills = Collections.synchronizedList(new ArrayList<Bill>());
	public enum BillState {Outstanding, PayASAP, Payed};

	public CashierAgent(String name, RestaurantHaus rest, double money) {
		super();
		this.name = name;
		this.rest = rest;
		this.money = money;
	}

	public void setPerson(Person p) {
		person = p;
	}

	public String getName() {
		return name;
	}

	// Messages
	
	public void msgShiftDone(double w) {
		print("got msg shift done");
		shiftDone = true;
		wage = w;
		stateChanged();
	
	}

	public void msgNeedCheck (Waiter w, String choice, Customer c) {
		checks.add(new Check(w, choice, c));
		stateChanged();
		//Add new order to list of orders
	}

	public void msgHereIsPayment(Customer customer, double payment) {
		money += payment;
		synchronized(checks) {
			for(Check check : checks) {
				if(check.c == customer) {
					check.s = CheckState.Payed;
					check.payment = payment;
				}
			}
		}
		stateChanged();
	}
	
	

	public void msgYourBillIs(Market_Douglass market, double bill) {
		bills.add(new Bill(market, bill));
		stateChanged();
	}

	/**
	 * Scheduler.  Determine what action is called for, and do it.
	 */
	public boolean pickAndExecuteAnAction() {//Made public for testing
		if(isPaused) {
			try {
				pauseSem.acquire();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		synchronized(bills) {
			for(Bill bill : bills) {
				if(bill.state == BillState.Outstanding) {
					FulfillBill(bill);
					return true;
				}
			}
		}

		synchronized(checks) {
			for(Check check : checks) {
				if(check.s == CheckState.Requested) {
					SendCheck(check);
					return true;
				}
			}
		}

		synchronized(checks) {
			for(Check check : checks) {
				if(check.s == CheckState.Payed) {
					TakeMoney(check);
					return true;
				}
			}
		}

		return false;
		//we have tried all our rules and found
		//nothing to do. So return false to main loop of abstract agent
		//and wait.
	}

	// Actions
	
	private void leaveWork() {
		person.msgStopWork(wage);
		isWorking = false; 
	}
	private void SendCheck(Check check) {
		print(check.c.getName() + " owes $" + String.valueOf(check.price) + ".");
		check.w.msgHereIsCheck(check.c, check.price);
		check.s = CheckState.WithWaiter;
	}

	private void TakeMoney(Check check) {
		if(check.payment == check.price) {
			print("Please come again!");
			//check.c.msgYouMayLeave();
		}
		else {
			print("Give me what you have and get out!");
			//check.c.msgYouMayLeave();
		}
		checks.remove(check);
	}

	private void FulfillBill(Bill bill) {
		if(money >= bill.bill) {
			bill.market.msgHereIsPayment(rest, bill.bill);
			money -= bill.bill;
			money = (double)((int)(money*100))/100;
			print("Here's the payment for the order.");
			bills.remove(bill);
		}
		else {
			print("I can't pay everything right now. I'll pay as soon as I can. Here's all I have.");
			bill.market.msgHereIsPayment(rest, money);
			bill.bill -= money;
			money = 0.00d;
			bills.remove(bill);
		}
	}

	//utilities

	public void setMenu(Menu m) {
		this.m = m;
	}
	public void subtract(double amount) {
		money -= amount;
	}
}

