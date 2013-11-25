package bank;

import agent.Agent;
import bank.interfaces.Manager;
import bank.interfaces.Teller;
import bank.interfaces.Person;
import bank.interfaces.BankCustomer;
import bank.gui.Bank;
import bank.gui.BankCustomerGui;
import simcity.gui.SimCityGui;

import java.util.*;

/**
 * bank Host Agent
 */
//We only have 2 types of agents in this prototype. A customer and an agent that
//does all the rest. Rather than calling the other agent a waiter, we called him
//the HostAgent. A Host is the manager of a bank who sees that all
//is proceeded as he wishes.
public class ManagerAgent extends Agent implements Manager {
	   public class WaitingCustomer {
		 BankCustomer bankCustomer;
		 State state;
		 Action action;
		 private int accountNum;
		 private double requestAmt;
		 public WaitingCustomer(BankCustomer bc){
			 bankCustomer = bc;
			 state = State.waiting;
		 }
		 
		 public void setAccountNum(int accNum){
			 accountNum = accNum;
		 }
		 public int getAccountNum(){
			 return accountNum;
		 }
		 public void setRequestAmt(double ra){
			 requestAmt = ra;
		 }
		 public double getRequestAmt(){
			 return requestAmt;
		 }		 
	   }

		List<WaitingCustomer> waitingCustomers = Collections.synchronizedList(new ArrayList<WaitingCustomer>());;
		enum State{entered, waiting, leaving, busy, idle};
		enum Action{newAccount, deposit, withdraw};

		class MyTeller {
			Teller teller;
			TellerState state;
			public MyTeller(Teller t){
				teller = t;
				state = TellerState.idle;
			}
		}
		enum TellerState{idle, busy};

		public List<MyTeller> tellers = Collections.synchronizedList(new ArrayList<MyTeller>());
		
		private Bank bank;
		
		private SimCityGui simCityGui;

		private String name;


	public ManagerAgent(String name, Bank b, SimCityGui bg) {
		super();
		bank = b;
		simCityGui = bg;
		this.name = name;
	}

	public String getMaitreDName() {
		return name;
	}

	public String getName() {
		return name;
	}

	
	// Messages
	
	public void msgTellerFree(Teller teller, BankCustomer bankCustomer){
		for(MyTeller t : tellers){
			if(t.teller == teller){
				t.state = TellerState.idle; break;
			}
		}
		for(WaitingCustomer wc : waitingCustomers){
			if(wc.bankCustomer == bankCustomer){
				wc.state = State.leaving; break;
			}
		}
		stateChanged();
	}
	
	public void msgCustomerHere(BankCustomer bca){
		boolean found = false;
		for(WaitingCustomer wc : waitingCustomers){
			if(wc.bankCustomer == bca){
		       wc.state = State.entered;
		       found = true; break;
		    }
		}
		if(found == false){
		    waitingCustomers.add(new WaitingCustomer(bca));
		}
	}

	public void msgRequestAccount(BankCustomer bc, double amount){
		//print(person.getName());
		for(WaitingCustomer wc : waitingCustomers){
			if(wc.bankCustomer == bc){
				wc.setAccountNum(-1);
				wc.setRequestAmt(amount);
				wc.action = Action.newAccount;
				break;
			}
		}
		stateChanged();
	}
	
	public void msgRequestDeposit(BankCustomer bc, int accountNumber, double amount){
		WaitingCustomer waitingCustomer = null;
		for(WaitingCustomer wc : waitingCustomers){
			if(wc.bankCustomer == bc){
				waitingCustomer = wc;
				wc.setAccountNum(accountNumber);
				wc.setRequestAmt(amount);
				break;
			}
		}
		waitingCustomer.action = Action.deposit;
		stateChanged();
	}
	
	public void msgRequestWithdrawal(BankCustomer bc, int accountNumber, double amount){
		for(WaitingCustomer wc : waitingCustomers){
			if(wc.bankCustomer == bc){
				wc.setAccountNum(accountNumber);
				wc.setRequestAmt(amount);
				wc.action = Action.withdraw;
				break;
			}
		}
		  stateChanged();
	}
	
	public void msgRequestLoan(int accountNumber, double amount, Person person){
		
	}

	/**
	 * Scheduler.  Determine what action is called for, and do it.
	 */
	protected boolean pickAndExecuteAnAction() {
		/* Think of this next rule as:
            Does there exist a table and customer,
            so that table is unoccupied and customer is waiting.
            If so seat him at the table.
		 */
		//print("HERE");
		for(WaitingCustomer wc : waitingCustomers){
			if(wc.state == State.waiting){
				for(MyTeller mt : tellers){
					if(mt.state == TellerState.idle){
						assignTeller(mt, wc);
						return true;
					}
				}
			}
		}
		for(WaitingCustomer wc : waitingCustomers){
			if(wc.state == State.entered){
				for(MyTeller mt : tellers){
					if(mt.state == TellerState.idle){
						assignTeller(mt, wc);
						return true;
					}
				}
				tellerBusy(wc);
				return true;
			}
		}
		
		for(WaitingCustomer wc : waitingCustomers){
			if(wc.state == State.leaving){
				updatePersonInfo(wc);
				return true;
			}
		}
		
		return false;
		//we have tried all our rules and found
		//nothing to do. So return false to main loop of abstract agent
		//and wait.
	}

	// Actions
	private void assignTeller(MyTeller mt, WaitingCustomer wc){	
		wc.bankCustomer.msgGoToTeller(mt.teller);
		print(wc.toString() + " " + wc.action);
		if(wc.action == Action.newAccount){
			wc.bankCustomer.msgRequestNewAccount(wc.requestAmt);
		}
		else if(wc.action == Action.deposit){
			wc.bankCustomer.msgRequestDeposit(wc.requestAmt);
		}
		else if(wc.action == Action.withdraw){
			print("RA: "+wc.requestAmt);
			wc.bankCustomer.msgRequestWithdraw(wc.requestAmt);
		}

		//wc.person.msgGoToTeller(mt.teller);
		wc.state = State.busy;
		mt.teller.msgNewCustomer(wc.bankCustomer);
		mt.state = TellerState.busy;
	}
	
	private void tellerBusy(WaitingCustomer wc){
		//wc.person.msgWait();
		wc.state = State.waiting;
	}
	
	private void updatePersonInfo(WaitingCustomer wc){
		BankCustomer bc = wc.bankCustomer;
		print(bc.getAccountNum() + " $" + bc.getBalance());
		wc.state = State.idle;
		bank.msgLeave(wc.bankCustomer, bc.getAccountNum(), bc.getBalance(), bc.getLoanAmount(), bc.getLoanTime());
	}
	
	
	public void addTeller(TellerAgent t){
		MyTeller mt = new MyTeller(t);
		tellers.add(mt); //?
		//tellers.add(new MyTeller(t));
		stateChanged();
	}
	
	public SimCityGui getGui(){
	  return simCityGui;
	}
	
}
