package allClasses;


import java.awt.Color;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import javax.swing.JTextArea;


public class IJTextArea

  extends JTextArea

  implements FocusListener

  {
    /* This class adds a few features to JTextArea.

      ?? Use StringBuilder to make long files load and display faster.
      The append(..) method is quite slow.
      */

    // constructors.

      public IJTextArea()
        {
          this( "" );
          }

      public IJTextArea( String StringIn )
        {
          super( StringIn );  // Construct superclass with text StringIn.
          
          commonInitializationV();
          }

      private void commonInitializationV()
        {
          setEditable(false);  // allow user to read only.
          addFocusListener(this);
          setBackground(null);  // revert to default gray color.
          setLineWrap(true);
          setWrapStyleWord(true);
          setCaretPosition(0);  // Put caret at beginning of text.
          }
      
    // FocusListener methods, to change color when focused.

      // @Override
      public void focusGained(FocusEvent arg0) 
        {
          setBackground(Color.white);
          }
    
      @Override
      public void focusLost(FocusEvent arg0) 
        {
          setBackground(null);  // revert to default color.
          }

    }
