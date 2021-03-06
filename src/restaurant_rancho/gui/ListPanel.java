package restaurant_rancho.gui;

import restaurant_rancho.CustomerAgent;
import restaurant_rancho.HostAgent;

import javax.swing.*;

import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.ArrayList;

/**
 * Subpanel of restaurantPanel.
 * This holds the scroll panes for the customers and, later, for waiters
 */
public class ListPanel extends JPanel implements ActionListener {

    public JScrollPane pane =
            new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                    JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    private JPanel view = new JPanel();
    private JPanel nameAdd = new JPanel();
    private List<JButton> list = new ArrayList<JButton>();
    private JButton addPersonB = new JButton("Add");
    private JTextField nameField = new JTextField("", 10);
    private JCheckBox hungry = new JCheckBox("Hungry?");
    private Object currentPerson;
    
    private RestaurantRancho restPanel;
    private String type;

    /**
     * Constructor for ListPanel.  Sets up all the gui
     *
     * @param rp   reference to the restaurant panel
     * @param type indicates if this is for customers or waiters
     */
    public ListPanel(RestaurantRancho rp, String type) {
        restPanel = rp;
        this.type = type;

        
        setLayout(new BoxLayout((Container) this, BoxLayout.Y_AXIS));
        add(new JLabel("<html><pre> <u>" + type + "</u><br></pre></html>"));
        
        if (type == "Customers") {
        hungry.setVisible(true);
        hungry.addActionListener(this);
        nameAdd.add(hungry);
        }
        
        nameAdd.setLayout(new FlowLayout());
        nameAdd.add(nameField);
        //nameAdd.add(hungry);
        addPersonB.addActionListener(this);
        nameAdd.add(addPersonB);
        
        add(nameAdd);

        view.setLayout(new BoxLayout((Container) view, BoxLayout.Y_AXIS));
        pane.setViewportView(view);
        
        Dimension listDim = new Dimension(RestaurantRanchoGui.WINDOWX/4, (int) RestaurantRanchoGui.WINDOWY/5);
        pane.setPreferredSize(listDim);
        pane.setMinimumSize(listDim);
        pane.setMaximumSize(listDim);
        add(pane);
    }

  
    /**
     * Method from the ActionListener interface.
     * Handles the event of the add button being pressed
     */
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == addPersonB) {
        	addPerson(nameField.getText(), hungry.isSelected());

        }
    }
    /*
        else {

        	for (JButton temp:list){
                if (e.getSource() == temp)
                    restPanel.showInfo(type, temp.getText());
            }
        }
    }
    */
    

    /**
     * If the add button is pressed, this function creates
     * a spot for it in the scroll pane, and tells the restaurant panel
     * to add a new person.
     *
     * @param name name of new person
     */
    public void addPerson(String name, boolean hungry) {
        if (name != null) {
            JButton button = new JButton(name);
            button.setBackground(Color.white);

            Dimension paneSize = pane.getSize();
            Dimension buttonSize = new Dimension(paneSize.width - 20,
                    (int) (paneSize.height / 5));
            button.setPreferredSize(buttonSize);
            button.setMinimumSize(buttonSize);
            button.setMaximumSize(buttonSize);
            button.addActionListener(this);
            list.add(button);
            view.add(button);
            //restPanel.addPerson(type, name, hungry);//puts customer on list
           // restPanel.showInfo(type, name);//puts hungry button on panel
            validate();
        }
    }
}
