package transportation.GUIs;
import java.awt.Graphics2D;

import AnimationTools.AnimationModule;
import simcity.gui.*;
import transportation.Agents.TruckAgent;

public class TruckGui implements Gui{
	
	private float xPos, yPos, xDestination, yDestination, xLast, yLast, speed;
	private AnimationModule animModule;
	boolean reachedHalfway, reachedDestination;
	
	
	TruckAgent agent;
	boolean isPresent = true;
	
	public TruckGui(float xPos, float yPos, TruckAgent agent) {
		this.xPos = xPos * 25 -3;
		this.yPos = yPos * 25;
		this.xDestination = xPos * 25 -3;
		this.yDestination = yPos * 25;
		this.xLast = xPos * 25 - 3;
		this.yLast = yPos * 25;
		speed = 1.00f;
		this.agent = agent;		
		reachedHalfway = true;
		reachedDestination = true;
		
		animModule = new AnimationModule("Pelipper", "IDLE", 10);
	}
	
	public void updatePosition() {
		if(Math.abs(xDestination - xPos) <= speed)
			xPos = xDestination;
		else if(xPos < xDestination) {
			xPos += speed;
			animModule.changeAnimation("Right");
		}
		else if(xPos > xDestination) {
			xPos -= speed;
			animModule.changeAnimation("Left");
		}
		
		if(Math.abs(yDestination - yPos) <= speed)
			yPos = yDestination;
		if(yPos < yDestination) {
			yPos += speed;
			animModule.changeAnimation("Down");
		}
		else if(yPos > yDestination) {
			yPos -= speed;
			animModule.changeAnimation("Up");
		}
		
		if(Math.abs(xLast - xPos) >= 15 || Math.abs(yLast - yPos) >= 15 && !reachedHalfway) {
			agent.msgHalfway();
			reachedHalfway = true;
		}
		
		if(xPos == xDestination && yPos == yDestination && !reachedDestination) {
			reachedDestination = true;
//			System.out.println("Reached Destination");
			agent.msgDestination();
		}
		
		if(animModule.getAnimation().equals("Delivery") && animModule.getLastFrame())
			agent.msgDestination();
	}
	
	public void doIdle() {
		animModule.changeAnimation("Idle", 30);
	}
	
	public void doDeliveryDance() {
		animModule.changeAnimation("Deliver", 10);
	}
	
	public void draw(Graphics2D g) {
		animModule.updateAnimation();
		g.drawImage(animModule.getImage(), (int)xPos, (int)yPos, null);
	}

	public void setDestination (float xDestination, float yDestination) {
		xLast = xPos;
		yLast = yPos;
		this.xDestination = xDestination * 25 - 3;
		this.yDestination = yDestination * 25;
		reachedHalfway = false;
//		System.out.println("resetting Destination");
		reachedDestination = false;
	}
	
	public void setIgnore() {
		isPresent = false;
	}
	
	public boolean isPresent() {
		return isPresent;
	}
}
