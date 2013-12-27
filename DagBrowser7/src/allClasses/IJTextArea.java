package allClasses;


import java.awt.Color;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.File; // added manually.

import javax.swing.JTextArea;


public class IJTextArea

  extends JTextArea

  implements FocusListener

  {
    private static final long serialVersionUID = 1L;

    // constructors.
      
      public IJTextArea( String StringIn )
        {
          super( StringIn );  // Construct superclass with text StringIn.
          
          setEditable(false);  // allow user to read only.
          addFocusListener(this);

          }
      
      public IJTextArea(File InFile)
        {
          super( );  // Construct superclass without any content yet.
          
          setEditable(false);  // allow user to read only.
          addFocusListener(this);

          { // Read in file to JTextArea.
            String LineString;  // temporary storage for file lines.
            try {
              /*
              BufferedReader TheBufferedReader = 
                  new BufferedReader(new InputStreamReader(
                      new ProgressMonitorInputStream(
                        TheJTextArea, // parentComponent???
                        "Reading " + InFile,
                        new FileInputStream(InFile))));
              */
              FileInputStream TheFileInputStream = new FileInputStream(InFile);
              @SuppressWarnings("resource")
              BufferedReader TheBufferedReader = 
                new BufferedReader(new InputStreamReader(TheFileInputStream));
              
              while ((LineString = TheBufferedReader.readLine()) != null) {
                  append(LineString + "\n");
                  }
              }
            catch (Exception ReaderException){
              // System.out.println("error reading file! " + ReaderException);
            	append("\nError reading file!\n\n" + ReaderException + "\n");
              setBackground(Color.PINK);  // Indicate error with color.
              } // Read in file to JTextArea.
            }
          }
      
    // FocusListener methods, to change color when focused.

      @Override
      public void focusGained(FocusEvent arg0) 
        {
          setBackground(Color.GREEN);
          }
    
      @Override
      public void focusLost(FocusEvent arg0) 
        {
          setBackground(null);  // revert to default color.
          }

    }
