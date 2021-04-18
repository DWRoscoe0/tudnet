package allClasses;

import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JTextArea;
import javax.swing.border.Border;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Modality;

public class Dialogger extends Object

  /* This class contains code related to the creation of dialogs.
   * At first they will be simple message dialog, then error dialogs.
   * 
   * ///enh Eventually add the ability to add message setting controls?
   * 
   * ///pla Convert these to use JavaFX libraries instead of Swing libraries.
   */
   
  {

    public static void showModelessDialogV(
        String theString, String titleTailString)
      /* General-purpose non-modal (mode-less) dialog displayer.
       * It presently calls a method to display a Swing dialog.
       * ///pla Change to display a JavaFX dialog.
       */
      {
        //// showModelessSwingDialogV(theString, titleTailString);
        showModelessJavaFXDialogV(theString, titleTailString);
        }

    public static void showModelessJavaFXDialogV(
        String theString, String titleTailString)
      /* General-purpose non-modal (mode-less) dialog displayer.
       * It queues the display of the dialog on the JavaFX Application Thread, 
       * then returns immediately. 
       * The dialog displays titleTailString after the app name in the title bar,
       * and displays theString in the main window,
       * and waits for the user to execute OK before it closes.  
       * */
      {
        Platform.runLater( () -> {
          String featureString= Config.appString + " " + titleTailString;
          Alert theAlert= new Alert(
              AlertType.INFORMATION, 
              featureString+"\n\n"+theString
              );
          theAlert.initModality(Modality.NONE);
          theAlert.setTitle(featureString);
          //// alert.showAndWait();
          theAlert.show();
          } );
        }

    public static void showModelessSwingDialogV( ///elim Delete when reproduced.
        String theString, String titleTailString)
      /* General-purpose non-modal (mode-less) dialog displayer.
       * It queues the display of the dialog on the EDT (Event Dispatch Thread, 
       * then returns immediately. 
       * The dialog displays titleTailString after the app name in the title bar,
       * and displays theString in the main window,
       * and waits for the user to execute OK before it closes.  
       *
        ///enh Don't change JTextArea color during resizing.
        ///enh Don't open multiple instances of the same dialog.
        ///fix Determine why restoreFocusV() doesn't work with this?
       * */
      {
        EventQueue.invokeLater(
          new Runnable() {
            @Override
            public void run() { 
              final JDialog theJDialog= new JDialog(
                (Frame)null, Config.appString + " " + titleTailString, false);
              Border emptyBorder = BorderFactory.createEmptyBorder(
                20,20,20,20);
              theJDialog.getRootPane().setBorder( emptyBorder );
              theJDialog.setLayout( new BoxLayout( 
                  theJDialog.getContentPane(), BoxLayout.PAGE_AXIS ) );

              JTextArea theJTextArea= new JTextArea( theString );
              theJTextArea.setBackground( theJDialog.getBackground() );
              theJTextArea.setEditable( false );
              theJTextArea.setFocusable( false );
              
              Component spacerComponent= Box.createVerticalStrut( 10 );
              
              JButton theJButton= new JButton("OK");
              theJButton.setAlignmentX(Component.CENTER_ALIGNMENT);
              theJButton.addActionListener( 
                  new ActionListener() {
                    public void actionPerformed(ActionEvent inActionEvent) {
                      theJDialog.dispose();
                    }
                  }
                );

              theJDialog.add( theJTextArea );
              theJDialog.add( spacerComponent );
              theJDialog.add( theJButton );
              
              theJDialog.pack(); // Layout all the content's sub-panels.
              theJDialog.setLocationRelativeTo(null); // Center on screen.
              theJDialog.setVisible(true); // Make the window visible.
              } 
            } 
          );
        }

    }
