package restaurant_pizza;

import agent_pizza.Agent;
import restaurant_pizza.gui.WaiterGui;
import restaurant_pizza.interfaces.Cashier;
import restaurant_pizza.interfaces.Customer;
import restaurant_pizza.interfaces.Waiter;
import simcity.PersonAgent;
import simcity.interfaces.Person;
import simcity.RestMenu;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.concurrent.Semaphore;

public class WaiterAgent_Normal extends WaiterAgent implements Waiter {
	
	public WaiterAgent_Normal(String name) {
		super(name);
		initializeMenu();
		this.name = name;
	}

	protected void goToCook(MyCustomer mc) {
		int tableNum = mc.table;
		String order = mc.order;
		print("Calling action goToCook");
		DoGoToCook();
		try {
			atTable.acquire();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		cook.msgNewOrder(this, tableNum, order);
		DoGoToHomePosition();
		try {
			atTable.acquire();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}