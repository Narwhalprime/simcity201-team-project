package market.gui;

import housing.ResidentAgent;

import javax.swing.*;

import simcity.PersonAgent;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Line2D;
import java.util.List;
import java.util.ArrayList;

public class MarketAnimationPanel extends JPanel implements ActionListener {

	private int lastTableX = 0;
	private int lastTableY = 0;
	private int tableWidth = this.getWidth()/9;
	private int tableHeight = this.getHeight()/7;
	
    private Image bufferImage;
    private Dimension bufferSize;
    
    private List<Gui> guis = new ArrayList<Gui>();
    Timer timer = new Timer(5, this);
    Graphics2D g2;
    
    public MarketAnimationPanel() {
    	this.setSize(400, 360);
        setVisible(true);
        
        bufferSize = this.getSize();
                
    	timer.start();	
    }

	public void actionPerformed(ActionEvent e) {
		updatePosition();
		repaint();  //Will have paintComponent called
	}	

    public void paintComponent(Graphics g) {
        g2 = (Graphics2D)g;
        
    	//Clear the screen by painting a rectangle the size of the frame
        g2.setColor(getBackground());
        g2.fillRect(0, 0, this.getWidth(), this.getHeight());
        Image backgroundImage = Toolkit.getDefaultToolkit().getImage("res/mickeysmarket.png/");
        g2.drawImage(backgroundImage, 0, 0, 400, 330, null);

        for(Gui gui : guis) {
            if (gui.isPresent()) {
                gui.draw(g2);
            }
        }
    }
    
    public void updatePosition() {
        for(Gui gui : guis) {
            if (gui.isPresent()) {
                gui.updatePosition();
            }
        }
    }
    
    public void addGui(WorkerGui gui) {
    	guis.add(gui);
    }
    
    public void addGui(CashierGui gui) {
    	guis.add(gui);
    }
    
    public void addGui(CustomerGui gui) {
    	System.out.println("added gui");
    	guis.add(gui);
    }
   
}
