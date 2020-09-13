package allClasses;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.border.Border;

public class Dialogger extends Object

  /* This class contains code related to the creation of dialogs.
   * At first they will be simple message dialog, then error dialogs.
   * ///enh Eventually the ability to add message setting controls might be added.
   */
   
  {

    public static void showModelessDialogV(
        String theString, String titleTailString)
      /* General-purpose non-modal (mode-less) dialog displayer.
       * It queues the display of the dialog on the EDT (Event Dispatch Thread, 
       * then returns immediately. 
       * It displays titleTailString after the app name in the title bar,
       * and displays theString in the main window,
       * and waits for the user to execute OK before it closes.  
       * */
      {
        java.awt.EventQueue.invokeLater( // Do everything on the EDT.
          new Runnable() {
            @Override
            public void run() { 
              final JFrame theJFrame=  // Make the JFrame.
                  new JFrame( Config.appString + " " + titleTailString );

              JPanel theJPanel= new JPanel();
              Border emptyBorder = BorderFactory.createEmptyBorder(
                  20,20,20,20);
              theJPanel.setBorder( emptyBorder );
              theJPanel.setLayout( 
                  new BoxLayout( theJPanel, BoxLayout.PAGE_AXIS));

              JTextArea theJTextArea= new JTextArea( theString );
              theJTextArea.setBackground( theJFrame.getBackground() );
              theJTextArea.setEditable( false );
              theJTextArea.setFocusable( false );
              theJPanel.add( theJTextArea );
              
              Component spacerComponent= Box.createVerticalStrut( 10 );
              theJPanel.add( spacerComponent );
              
              JButton theJButton= new JButton("OK");
              theJButton.setAlignmentX(Component.CENTER_ALIGNMENT);
              theJButton.addActionListener( 
                  new ActionListener() {
                    public void actionPerformed(ActionEvent inActionEvent) {
                      theJFrame.dispose();
                    }
                  }
                );
              theJPanel.add( theJButton );

              theJFrame.setContentPane( theJPanel );
              
              theJFrame.pack(); // Layout all the content's sub-panels.
              theJFrame.setLocationRelativeTo(null); // Center on screen.
              theJFrame.setVisible(true);  // Make the window visible.
              } 
            } 
          );
        }

    }
