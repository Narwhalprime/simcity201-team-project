package transportation.Agents;

import java.util.List;
import java.util.Random;
import java.util.concurrent.Semaphore;

import astar.astar.AStarNode;
import astar.astar.Position;
import simcity.interfaces.Person;
import transportation.GUIs.WalkerGui;
import transportation.Objects.*;

public class WalkerAgent extends MobileAgent{

	Person walker;
	Position currentPosition;
	Position endPosition;
	TransportationController master;
	WalkerGui gui;
	boolean arrived;
	WalkerTraversal aStar;
	
	Semaphore animSem;
	BusStop beginBusStop, endBusStop;
	String building;

	public WalkerAgent(Person walker, Position currentPosition, Position endPosition, TransportationController master, WalkerTraversal aStar) {
		this.walker = walker;
		this.currentPosition = currentPosition;
		this.endPosition = endPosition;
		this.master = master;
		arrived = false;
		this.aStar = aStar;
		
		animSem = new Semaphore(0, true);
		beginBusStop = null;
		endBusStop = null;
		building = null;
	}
	
	public WalkerAgent(Person person, Position currentPosition, Position endPosition, TransportationController master, WalkerTraversal aStar, BusStop beginBusStop, BusStop endBusStop, String building) {
		this.walker = person;
		this.currentPosition = currentPosition;
		this.endPosition = endPosition;
		this.master = master;
		arrived = false;
		this.aStar = aStar;
		
		animSem = new Semaphore(0, true);
		this.beginBusStop = beginBusStop;
		this.endBusStop= endBusStop;
		this.building = building;
	}
	
	public void msgHalfway() {//Releases semaphore at halfway point to prevent sprites from colliding majorly
		if(master.getGrid()[currentPosition.getX()][currentPosition.getY()].availablePermits() == 0) {
			master.getGrid()[currentPosition.getX()][currentPosition.getY()].release();
			
		}
		//System.out.println("Releasing " + currentPosition.toString());
		//System.out.println(String.valueOf(master.getGrid()[currentPosition.getX()][currentPosition.getY()].availablePermits()));
	}

	public void msgDestination() {
		animSem.release();
	}

	//Remember to release semaphores to tiles when despawning
	@Override
	protected boolean pickAndExecuteAnAction() {
		if(beginBusStop != null) {
			goToPosition(beginBusStop.getAssociatedTile(), null);
		}
		if(!arrived) {
			goToPosition(endPosition, null);
		}
		if(arrived) {
			tauntAndLeave();
		}
		return false;
	}

	public void goToPosition(Position goal, Position ignore) {
		AStarNode aStarNode = (AStarNode)aStar.generalSearch(currentPosition, goal, ignore);
		List<Position> path = aStarNode.getPath();
		Boolean firstStep   = true;
		Boolean gotPermit   = true;

		for (Position tmpPath: path) {
			//The first node in the path is the current node. So skip it.
			if (firstStep) {
				firstStep   = false;
				continue;
			}

			//Try and get lock for the next step.
			int attempts    = 1;
			
			MovementTile.MovementType temp = master.getGrid()[tmpPath.getX()][tmpPath.getY()].getMovementType();
			while(temp == MovementTile.MovementType.TRAFFICCROSSROAD || temp == MovementTile.MovementType.TRAFFICCROSSNONE) {
				if(temp == MovementTile.MovementType.TRAFFICCROSSROAD || temp == MovementTile.MovementType.TRAFFICCROSSNONE)
					break;
				try { Thread.sleep(1000); }
				catch (Exception e){}
			}
			gotPermit       = new Position(tmpPath.getX(), tmpPath.getY()).moveInto(aStar.getGrid());
			
			Random random = new Random();
			int attemptsToMake = random.nextInt(3) + 3;
			//Did not get lock. Lets make n attempts.
			while (!gotPermit && attempts < attemptsToMake) {
				//System.out.println("[Gaut] " + guiWaiter.getName() + " got NO permit for " + tmpPath.toString() + " on attempt " + attempts);
				gui.setStill();
				//Wait for 1sec and try again to get lock.
				try { Thread.sleep(1000); }
				catch (Exception e){}
				
				while(temp == MovementTile.MovementType.TRAFFICCROSSROAD || temp == MovementTile.MovementType.TRAFFICCROSSNONE) {
					if(temp == MovementTile.MovementType.TRAFFICCROSSROAD || temp == MovementTile.MovementType.TRAFFICCROSSNONE)
						break;
					try { Thread.sleep(1000); }
					catch (Exception e){}
				}
				gotPermit   = new Position(tmpPath.getX(), tmpPath.getY()).moveInto(aStar.getGrid());
				attempts ++;
			}

			//Did not get lock after trying n attempts. So recalculating path.            
			if (!gotPermit) {
				//System.out.println("[Gaut] " + guiWaiter.getName() + " No Luck even after " + attempts + " attempts! Lets recalculate");
				goToPosition(goal, tmpPath);
				break;
			}

			//Got the required lock. Lets move.
			//System.out.println("[Gaut] " + guiWaiter.getName() + " got permit for " + tmpPath.toString());
			//currentPosition.release(aStar.getGrid());
			gui.setMoving();
			gui.setDestination(tmpPath.getX(), tmpPath.getY());
			//System.out.println("DESTINATION: " + tmpPath.toString());
			try {
				animSem.acquire();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			currentPosition = new Position(tmpPath.getX(), tmpPath.getY ());
		}
		
		arrived = true;
	}

	public void tauntAndLeave() {
		gui.doTauntAndLeave();
		try {
			animSem.acquire();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if(beginBusStop == null) {
			master.msgArrivedAtDestination(walker);
		}
		else
			beginBusStop.addRider(walker, endBusStop, building);
		if(master.grid[currentPosition.getX()][currentPosition.getY()].availablePermits() == 0) {
			master.grid[currentPosition.getX()][currentPosition.getY()].release();
			//System.out.println("Releasing " + currentPosition.toString());
		}
		gui.setIgnore();
		stopThread();
	}

	public void setGui (WalkerGui gui) {
		this.gui = gui;
	}

	@Override
	public String getType() {
		// TODO Auto-generated method stub
		return "walker";
	}

}
