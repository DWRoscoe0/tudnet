package allClasses;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.JButton;


public class IJButton  
  extends JButton
  implements FocusListener, ActionListener

  /* This class changes JButton functionality as follows:
  
    * It customizes the display of buttons to make focus more identifiable. 
    
    * It (will) allow interrogating and clearing whether
      a button has been clicked.
    
    ?? Find and use icon image files of up, down, left, and right arrows.
    */
  {
    // Variables.

      private static final long serialVersionUID = 1L;
      
      private boolean buttonClickedB= false;  // Button-clicked flag.

    // Constructors.

      public IJButton(String NameString)
        // Constructor.
        { 
          super(NameString); 
          super.addActionListener(this);
          addFocusListener(this);  // make this be FocusListener.
          }
  
      // ActionListener methods. 

        public void addActionListener( ActionListener inActionListener )
          /* This method exists so that this object 
            will always be the last user ActionListener added so that 
            it will be the first user ActionListener to be executed so that
            it can change state before other ActionListeners interrogate it.
            */
          { 
            super.removeActionListener(this);
            super.addActionListener( inActionListener );
            super.addActionListener(this);
            }

        public void actionPerformed(ActionEvent inActionEvent) 
          /* This method processes ActionsEvent-s from the button.
            All it does is record that the button has been clicked.
            */
          {
            buttonClickedB= true;  // Record in flag that button was clicked.
            }

        public boolean getAndResetActionV( boolean resetB ) 
          /* This method returns the value of the button-clicked flag.
            If resetB == true then it resets the flag for next time. 
            The value returned is the value before the reset.
            This method can be called from 
            a user code ActionListener because Listeners are called 
            in the order in which they were added.
            Since this one is added by the constructor,
            it is added before any other user code Listener.
            */
          {
            boolean returnB= buttonClickedB;  // Save flag value for return.
            if ( resetB )   // Reset button-clicked flag if desired.
              buttonClickedB= false;
            return returnB;  // Return previously saved value.
            }

    // FocusListener methods, for changing the appearance of focused button.

      public void focusGained(FocusEvent TheFocusEvent)
        { 
          setBackground(Color.GREEN);  // use GREEN as background color when focused.
          repaint();
          }

      public void focusLost(FocusEvent TheFocusEvent)
        { 
          setBackground(null);  // use default (parent) background color when not focused.
          repaint();
          }
  
    }
