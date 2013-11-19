package bank;

import bank.gui.BankCustomerGui;
import bank.gui.BankGui;
import bank.gui.Account;
import bank.interfaces.BankCustomer;
import bank.interfaces.Teller;
import bank.interfaces.Bank;
import agent.Agent;

import java.util.*;



/**
 * bank customer agent.
 */
public class BankCustomerAgent extends Agent implements BankCustomer {
	private String name;
	private double balance = 25.00;
	private double change;
	
	private BankGui bankGui;
	private BankCustomerGui personGui;
	
	// agent correspondents
	private Bank bank = null;
	private Teller teller = null;

	public enum State
	{deciding, openingAccount, depositing, withdrawing, requestingLoan, leaving, idle};
	State state = State.idle;

	public enum AnimState{go, walking, idle};
	AnimState animState = AnimState.idle;
	
	int accountNum;
	double requestAmt;	

	/**
	 * Constructor for CustomerAgent class
	 *
	 * @param name name of the customer
	 * @param gui  reference to the customergui so the customer can send it messages
	 */
	public BankCustomerAgent(String name, int an, Bank b, BankGui bg){
		super();
		this.name = name;
		accountNum = an;
		bank = b;
		bankGui = bg;
		state = State.idle;
	}

	public String getCustomerName() {
		return name;
	}
	// Messages
	
	public void	msgRequestNewAccount(double ra){
		requestAmt = ra;
		state = State.openingAccount;
		stateChanged();
	}
	public void	msgRequestDeposit(double ra){
		requestAmt = ra;
		state = State.depositing;
		stateChanged();	
	}
	public void	msgRequestWithdraw(double ra){
		requestAmt = ra;
		state = State.withdrawing;
		stateChanged();
	}
	public void	msgRequestLoan(double ra){
		requestAmt = ra;
		state = State.requestingLoan;
		stateChanged();
	}

	public void msgGoToTeller(Teller t){
		teller = t;
		animState = AnimState.go; 
		stateChanged();
	}
	public void msgAccountOpened(int an){
		balance += change;
		print("ACCOUNT OPENED "+balance);
		accountNum = an;
		state = State.leaving;
		stateChanged();
	}
	public void msgMoneyDeposited(){
		balance += change;
		print("MONEY DEPOSITED "+ balance);
		state = State.leaving;
		stateChanged();
	}
	public void msgMoneyWithdrawn(double amtWithdrawn){
		balance += change;
		print("MONEY WITHDRAWN "+ balance);
		state = State.leaving;
		change = amtWithdrawn;
		stateChanged();
	}
	public void msgLoanDecision(boolean status){
		print("LOAN "+status);
		
	}
	
	public void msgAnimationFinishedGoToTeller(){
		print("AT TELLER");
		animState = AnimState.idle;
		stateChanged();
	}
	
	public void msgAnimationFinishedLeavingBank(){
		animState = AnimState.idle;
		stateChanged();
	}

	/**
	 * Scheduler.  Determine what action is called for, and do it.
	 */
	protected boolean pickAndExecuteAnAction() {
		//	CustomerAgent is a finite state machine
		
	    if(animState == AnimState.go){
			goToTeller();
			return true;
		}
	    else if(animState == AnimState.idle){
		
		   if(state == State.openingAccount){
			   openAccount();
			   return true;
		    }
		   else if(state == State.depositing){
			   depositCash();
			   return true;
		    }
		   else if(state == State.withdrawing){
			   withdrawCash();
			   return true;
		    }
		   else if(state == State.leaving){
			   leaveBank();
			   return true;
		   }
		 /*  else if(state == State.left){
			   leftBank();
			   return true;
		   }*/
	    }
		return false;
	}

	// Actions
	
	private void goToTeller(){
		state = State.idle;
		personGui.DoGoToTeller(teller.getGui().getBaseX(), teller.getGui().getBaseY());
	    bankGui.updateInfoPanel(this);
	}
	
	private void openAccount(){
		teller.msgOpenAccount(this, balance*.5);
		change = -balance*.5;
		state = State.idle;
	}
	private void depositCash(){
		teller.msgDepositCash(accountNum, requestAmt);
		//change = -5.00;
		state = State.idle;
	}
	private void withdrawCash(){
		teller.msgWithdrawCash(accountNum, requestAmt);
		//change = 5.00;
		state = State.idle;
	}
	private void leaveBank(){
		teller.msgLeavingBank();
		state = State.idle;
		personGui.DoLeaveBank();
	}
	
	private void leftBank(){
		state = State.idle;
	    bankGui.updateInfoPanel(this);
	}

	// Accessors, etc.

	public String getName() {
		return name;
	}
	
	public void setBalance(double b) {
		balance = b;
	}

	public String toString() {
		return "customer " + getName();
	}
	
	public void setBank(Bank b) {
		bank = b;
	}
	
	public Bank getBank() {
		return bank;
	}
	
	public int getAccountNum(){
		return accountNum;
	}
	
	public void setTeller(Teller t) {
		teller = t;
	}
	
	public void setGui(BankCustomerGui g) {
		personGui = g;
	}

	public BankCustomerGui getGui() {
		return personGui;
	}
}

