package transportation;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.*;

import simcity.gui.SimCityGui;
import simcity.gui.trace.AlertLog;
import simcity.gui.trace.AlertTag;
import transportation.Agents.TransportationController;
import transportation.GUIs.Gui;
import transportation.GUIs.WalkerGui;

public class TransportationPanel extends JPanel implements ActionListener, MouseListener, MouseMotionListener{
	private final int WINDOWX = 400;
	private final int WINDOWY = 330;
	private final int scrollSpeed = 4;
	private final int bufferZones = 4;
	private final int buffer = 15;

	private Image img;
	SimCityGui gui;
	private Transportation controller;
	boolean day = false;
	
	MouseEvent previousPosition;
	Point offset;

	private List<Gui> guis = Collections.synchronizedList(new ArrayList<Gui>());
	Timer timer;

	class BuildingFinder {
		int xLeft, xRight, yTop, yBottom;
		String name;

		BuildingFinder(int xLeft, int yTop, int xRight, int yBottom, String name) {
			this.xLeft = xLeft;
			this.xRight = xRight;
			this.yTop = yTop;
			this.yBottom = yBottom;
			this.name = name;
		}
	}

	List<BuildingFinder> buildings;
	
	public void changeDay() {
		System.out.println("yay changeDay is called");
		if(day) {
			img = Toolkit.getDefaultToolkit().getImage("res/simcitynight.jpg");
//			if (!gui.poppedOut) img = img.getScaledInstance(400, 330, Image.SCALE_DEFAULT);
			day = false;
			updateUI();
		}
		else {
			img = Toolkit.getDefaultToolkit().getImage("res/simcityLarge.jpg");
//			if (!gui.poppedOut) img = img.getScaledInstance(400, 330, Image.SCALE_DEFAULT);
			day = true;
			updateUI();
		}
			
	}
	
	public TransportationPanel(SimCityGui gui) {
		offset = new Point(0,0);
		setSize(WINDOWX, WINDOWY);
		setVisible(true);

		this.gui = gui;

		img = Toolkit.getDefaultToolkit().getImage("res/simcitynight.jpg");	
//		if (!gui.poppedOut) img = img.getScaledInstance(400, 330, Image.SCALE_DEFAULT);
		
		timer = new Timer(20, this );
		timer.start();

		controller = new TransportationController(this);

		addMouseListener(this);
		addMouseMotionListener(this);

		buildings = new ArrayList<BuildingFinder>();
		
		buildings.add(new BuildingFinder(650,0,700,50,"Main St Apartments #1"));
		buildings.add(new BuildingFinder(0,100,50,150,"Main St Apartments #2"));
		buildings.add(new BuildingFinder(0,425,50,475,"Main St Apartments #3"));
		buildings.add(new BuildingFinder(300,450,350,500,"Main St Apartments #4"));
		buildings.add(new BuildingFinder(0,525,50,575,"Main St Apartments #5"));
		buildings.add(new BuildingFinder(800,550,850,600,"Main St Apartments #6"));
		buildings.add(new BuildingFinder(800,650,850,700,"Main St Apartments #7"));
		buildings.add(new BuildingFinder(50,700,100,750,"Main St Apartments #8"));
		buildings.add(new BuildingFinder(200,700,250,750,"Main St Apartments #9"));
		buildings.add(new BuildingFinder(500,700,550,750,"Main St Apartments #10"));
		buildings.add(new BuildingFinder(650,700,700,750,"Main St Apartments #11"));
		
		
		buildings.add(new BuildingFinder(500,225,600,300,"Pirate Bank"));
		buildings.add(new BuildingFinder(600,450,650,500,"Buccaneer Bank"));
		buildings.add(new BuildingFinder(375,0,450,50,"Mickey's Market"));
		buildings.add(new BuildingFinder(500,450,550,525,"Minnie's Market"));
		
		buildings.add(new BuildingFinder(550,0,600,50,"Rancho Del Zocalo"));
		buildings.add(new BuildingFinder(300,700,350,750,"Blue Bayou"));
		buildings.add(new BuildingFinder(800,200,850,250,"Village Haus"));
		buildings.add(new BuildingFinder(200, 450, 250, 500,"Pizza Port"));
		buildings.add(new BuildingFinder(550,500,600,550,"Carnation Cafe"));
		
		buildings.add(new BuildingFinder(800,75, 850, 150,"Haunted Mansion"));
		buildings.add(new BuildingFinder(150,0, 225, 50,"Tiki Hut"));
		buildings.add(new BuildingFinder(0,275, 50, 350,"Rabbit Hole"));
		buildings.add(new BuildingFinder(0,625, 50, 700,"Cinderella Castle"));
		buildings.add(new BuildingFinder(200,500, 275, 550,"Space Mountain"));
		buildings.add(new BuildingFinder(800,425, 850, 500,"Pirate's Suite"));
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		repaint();
	}

	public void paintComponent(Graphics g) {
		Graphics2D g2 = (Graphics2D)g;

		
		//offset changing
		if(previousPosition != null) {
			//Move Camera Up
			for(int i = 0; i < bufferZones; i++) {
				if(previousPosition.getY() >= 0 && previousPosition.getY() <= buffer * (bufferZones-i))
					offset.y -= scrollSpeed;

				//Move Camera Down
				if(previousPosition.getY() >= getSize().height - buffer * (bufferZones-i) && previousPosition.getY() <= getSize().height)
					offset.y += scrollSpeed;

				//Move Camera Left
				if(previousPosition.getX() >= 0 && previousPosition.getX() <= buffer * (bufferZones-i))
					offset.x -= scrollSpeed;

				//Move Camera Right
				if(previousPosition.getX() >= getSize().height - buffer * (bufferZones-i) && previousPosition.getY() <= getSize().height)
					offset.x += scrollSpeed;
			}
		}

		//offset clamping
		if(offset.x < 0) {
			offset.x = 0;
		}
		
		if(offset.x > 850 - getSize().width) {
			offset.x = 850 - getSize().width;
		}
		
		if(offset.y > 750 - getSize().height) {
			offset.y = 750 - getSize().height;
		}
		if(offset.y < 0) {
			offset.y = 0;
		}
		
		g2.drawImage(img, (int)-offset.getX(), (int)-offset.getY(), null);

		synchronized (guis) {
			for(Gui gui : guis) {
				/*
				if(gui instanceof WalkerGui) {
					WalkerGui temp = (WalkerGui)gui;
					//System.out.println(temp.agent.getPerson().getName());
					if(temp.agent.getPerson().getName().equals("CafeCashier1")) {
						System.out.println("CAFECASHIER IS HERE: " + String.valueOf(guis.indexOf(gui)));
					}
				}
				System.out.println("ERROR IS HERE: " + String.valueOf(guis.indexOf(gui)));
				if(guis.indexOf(gui) == 46)
					System.exit(0);
				System.out.println(gui.returnType());
				*/
				if (gui.isPresent()) {
					gui.updatePosition();
				}
			}
		}
		synchronized (guis) {
			for(Gui gui : guis) {
				if (gui.isPresent()) {
					gui.draw(g2, offset);
				}
			}
		}
	}

	public void addGui (Gui gui) {
		guis.add(gui);
		gui.setPanel(this);
	}

	public void pauseAnim() {
		timer.stop();
	}

	public void unpauseAnim() {
		timer.start();
	}

	@Override
	public void mouseClicked(MouseEvent me) {
		String name = findBuilding((int)(me.getX() + offset.getX()), (int)(me.getY() + offset.getY()));
		AlertLog.getInstance().logInfo(AlertTag.TRANSPORTATION, "MOUSE CLICKED", name + ": " + String.valueOf(me.getX() + offset.getX()) + " " + String.valueOf(me.getY() + offset.getY()));
		System.out.println(name + ": " + String.valueOf(me.getX() + offset.getX()) + " " + String.valueOf(me.getY() + offset.getY()));
		if(name != null) {
			gui.showPanel(name);
			gui.closePopOut();
		}
	}

	private String findBuilding(int x, int y) {
		//for(BuildingFinder b : buildings) {
		for(int i = 0; i < buildings.size(); i++) {
			BuildingFinder b = buildings.get(i);
			if(x >= b.xLeft && x < b.xRight && y >= b.yTop && y < b.yBottom)
				return b.name;
		}
		return null;
	}

	@Override
	public void mouseEntered(MouseEvent me) {
		// TODO Auto-generated method stub
	}

	@Override
	public void mouseExited(MouseEvent me) {
		previousPosition = null;

	}

	@Override
	public void mousePressed(MouseEvent me) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseReleased(MouseEvent me) {
		// TODO Auto-generated method stub

	}

	public Transportation getTransportation() {
		return controller;
	}

	@Override
	public void mouseDragged(MouseEvent e) {
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		previousPosition = e;
	}
}
