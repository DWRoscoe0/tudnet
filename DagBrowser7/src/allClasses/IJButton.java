package allClasses;
import java.awt.Color;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.JButton;


public class IJButton  
  extends JButton
  implements FocusListener
  /* This class was created to customize the display of buttons.
   * This is to make the holder of focus more identifiable. 
   */
  {
    // Constructors.

    private static final long serialVersionUID = 1L;

	public IJButton(String NameString)
        // Constructor.
        { 
          super(NameString); 
          this.addFocusListener(this);  // make this be FocusListener.
          }
          
    // FocusListener methods, for any component focus changes, and related methods.

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
