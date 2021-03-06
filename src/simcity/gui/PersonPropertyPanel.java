package simcity.gui;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;

import simcity.gui.trace.AlertLog;
import simcity.gui.trace.AlertTag;

public class PersonPropertyPanel extends JPanel implements ActionListener {
	
	private final static double START_MONEY = 20;
	
	SimCityGui gui;

	JPanel settings = new JPanel();
	
	JTextField nameField;
	JComboBox housingList;
	JSpinner moneySpinner;
	JComboBox foodPreferenceList;
	JComboBox personalityList;
	JCheckBox preferAtHomeCheckBox;
	JComboBox transportationList;
	JComboBox workplaceList;
	JComboBox workplaceRoleList;
	JComboBox mickeyMarketShiftList;
	
	JButton addPersonButton = new JButton("Create person");
	
	public PersonPropertyPanel(SimCityGui gui) {
		this.gui = gui;
		updateGui();
	}

	public void updateGui(){
	    clear();
		settings.removeAll();
		settings.setLayout(new GridLayout(11, 2));
        Dimension panelDim = new Dimension(354,50);  
        
		JLabel label = new JLabel("Name");
		settings.add(label);
		
        nameField = new JTextField();
        settings.add(nameField);
        
        label = new JLabel("Housing");
		settings.add(label);
        
        housingList = new JComboBox(SimCityGui.simCityPanel.getAllHousing());
        settings.add(housingList);
        
        label = new JLabel("Starting money");
		settings.add(label);
        
        SpinnerModel startMoneySpinner = new SpinnerNumberModel(START_MONEY, 0, 100, 1);
        moneySpinner = new JSpinner(startMoneySpinner);
        settings.add(moneySpinner);
        
        label = new JLabel("Food preference");
		settings.add(label);
        
        String[] foodPreferenceArray = {"Italian", "Mexican", "Southern", "American", "German"};
        foodPreferenceList = new JComboBox(foodPreferenceArray);
        settings.add(foodPreferenceList);
        
        label = new JLabel("Personality type");
		settings.add(label);
        
        String[] personalityArray = {"Normal", "Deadbeat", "Crook"};
        personalityList = new JComboBox(personalityArray);
        settings.add(personalityList);
        
        label = new JLabel("Start eating at home?");
		settings.add(label);
		
		preferAtHomeCheckBox = new JCheckBox();
		settings.add(preferAtHomeCheckBox);
        
		label = new JLabel("Transportation type");
		settings.add(label);
        
        String[] transportationArray = {"Walk", "Bus", "Car"};
        transportationList = new JComboBox(transportationArray);
        settings.add(transportationList);
        
        label = new JLabel("Workplace");
		settings.add(label);
		
        String[] workplaceArray = {"Mickey's Market", "Minnie's Market", "Rancho Del Zocalo", "Village Haus", "Carnation Cafe", "Blue Bayou", "Pizza Port", "Pirate Bank", "Buccaneer Bank"};
        workplaceList = new JComboBox(workplaceArray);
        settings.add(workplaceList);
        
        label = new JLabel("Workplace Role");
		settings.add(label);
		
        String[] workplaceRoleArray = {"Cashier", "Cook", "Customer", "Host", "Manager", "Waiter", "Worker"};
        workplaceRoleList = new JComboBox(workplaceRoleArray);
        settings.add(workplaceRoleList);
        
        label = new JLabel("Workplace Shift");
		settings.add(label);
        
        String[] shiftArray = {"0", "1", "2"};
        mickeyMarketShiftList = new JComboBox(shiftArray);
        settings.add(mickeyMarketShiftList);
		
        addPersonButton.addActionListener(this);
        settings.add(addPersonButton);
		add(settings);
	    setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
	}
	
	public void clear(){
		
		//settings.revalidate();
		//settings.repaint();
	}
	
	public void actionPerformed(ActionEvent e) {
		if(e.getSource() == addPersonButton) {
			String personName = nameField.getText();
			gui.simCityPanel.addPerson(personName, (String)housingList.getSelectedItem(), 
					(Double)moneySpinner.getValue(), (String)foodPreferenceList.getSelectedItem(),
					preferAtHomeCheckBox.isSelected(), ((String)transportationList.getSelectedItem()).charAt(0),
					(String)personalityList.getSelectedItem(), (String)workplaceList.getSelectedItem(), (String)workplaceRoleList.getSelectedItem(),
					Integer.parseInt((String)mickeyMarketShiftList.getSelectedItem()));
		}
	}
}
