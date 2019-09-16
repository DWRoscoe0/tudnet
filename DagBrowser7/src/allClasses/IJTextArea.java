package allClasses;


import java.awt.Color;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.File; // added manually.

import javax.swing.JTextArea;
import static allClasses.Globals.NL;


public class IJTextArea

  extends JTextArea

  implements FocusListener

  {
    /* This class adds a few features to JTextArea.

      ?? Use StringBuilder to make long files load and display faster.
      The append(..) method is quite slow.
      */

    private static final long serialVersionUID = 1L;
    private Color backgroundColor= null;  // null means default.

    // constructors.
      
      public IJTextArea( String StringIn )
        {
          super( StringIn );  // Construct superclass with text StringIn.
          
          commonInitializationV();
          }

      public IJTextArea(File InFile)
        {
          super( );  // Construct superclass without any content yet.

          { // Read in file to JTextArea.
            String LineString;  // temporary storage for file lines.
            try {
              FileInputStream TheFileInputStream = new FileInputStream(InFile);
              @SuppressWarnings("resource")
              BufferedReader TheBufferedReader = 
                new BufferedReader(new InputStreamReader(TheFileInputStream));
              
              while ((LineString = TheBufferedReader.readLine()) != null) {
                  append(LineString + NL);
                  }
              }
            catch (Exception ReaderException){
              // System.out.println("error reading file! " + ReaderException);
            	append(NL + "Error reading file!" + NL + NL + ReaderException + NL);
              backgroundColor= Color.PINK;  // Overriding color for error.
              }
            } // Read in file to JTextArea.

          commonInitializationV();
          }

      private void commonInitializationV()
        {
          setEditable(false);  // allow user to read only.
          addFocusListener(this);
          setBackground(backgroundColor);  // revert to default color.
          // setLineWrap(true);  This doesn't work well.
          setCaretPosition(0);  // Put caret at beginning of text.
          }
      
    // FocusListener methods, to change color when focused.

      @Override
      public void focusGained(FocusEvent arg0) 
        {
          setBackground(UIColor.activeColor);
          }
    
      @Override
      public void focusLost(FocusEvent arg0) 
        {
          setBackground(backgroundColor);  // revert to default color.
          }

    }
