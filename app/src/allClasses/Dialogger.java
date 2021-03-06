package allClasses;

import static allClasses.AppLog.theAppLog;

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

import allClasses.javafx.JavaFXGUI;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.Region;
import javafx.stage.Modality;

public class Dialogger extends Object

  /* This class contains code related to the creation of dialogs.
   * 
   * It can create dialogs for both Swing and JavaFX,
   * though Swing dialogs are being deprecated.
   * 
   * The JavaFX method may be called at any time,
   * but if the JavaFX runtime has not been started,
   * no output will be produced.
   * 
   * ///enh Eventually add the ability to add message setting controls
   * which deal with messages which happen a lot, by:
   * * suppressing particular types of messages or 
   * * reducing anomaly detection sensitivity.
   * 
   * ///enh Add ability to return user responses in the case of error dialogs,
   * if that becomes useful.
   * 
   * ///enh Use setAlwaysOnTop(boolean value) of the dialog's Stage
   * for important error or anomaly dialogs so they will always show.
   * Early anomaly dialogs are being obscured by the JavaFX Help dialog. 
   * 
   */
   
  {

    public static String showModelessJavaFXDialogReturnString(
        String summaryIDLineString, 
        String detailsString,
        boolean alertWithSoundB)
      /* This method tries to display a dialog window.
        This method is equivalent to 
          showModelessJavaFXDialogReturnString(
            titleString, summaryIDLineString, detailsString)
        with titleString == appNameString + summaryIDLineString.
        See that method for details.
        */
      {
        String titleString= // Make title out of app name and summary line.
            Config.appString + ": " + summaryIDLineString;
        return showModelessJavaFXDialogReturnString(
          titleString, summaryIDLineString, detailsString, alertWithSoundB);
        }

    private static String showModelessJavaFXDialogReturnString(
        String titleString, 
        String summaryIDLineString, 
        String detailsString,
        boolean alertWithSoundB)
      /* This method tries to run a Runnable on the JavaFX Application Thread 
       * to do the following:
       * * Display a dialog window. 
       * * Display the titleString in the title bar.
       *   This might be meaningful for only large-screen OSs.
       * * Display the summaryIDLineString in the header area.
       * * Display the detailsString in the content area at the bottom.
       * The method returns immediately after queuing the job,
       * but the dialog waits for the user to execute OK before it disappears.
       * It returns null if the job is successfully submitted,
       * a String describing the failure if it fails.  
       * 
       * ///ano WindowOpensOffScreen.  It happened after I added
       *  JavaFXGUI.setDefaultStyle(theAlert.getDialogPane());
       *  
       * ///enh Format detailsString to indent the continuation of 
       *   wrapped lines so line/paragraph boundaries stand out. 
       */
      {
        String resultString= null;
        try {
          Platform.runLater( () -> {
            Alert theAlert= JavaFXGUI.testForAlert(summaryIDLineString);
            if (null == theAlert) // If Alert not cached, make new one. 
              { // Make new Alert dialog.
                if (alertWithSoundB) // Signal new dialog if sound desired.
                  java.awt.Toolkit.getDefaultToolkit().beep(); // Audible Beep.
                   ///ano The above line causes debug tracing to fail!
                theAlert= new Alert(AlertType.INFORMATION);
                theAlert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
                theAlert.getDialogPane().setMinWidth(600); ///ano Kludge fixes:
                  // * getDialogPane().setMinWidth(Region.USE_PREF_SIZE) fails.
                  // * Title bar mostly off-screen.
                JavaFXGUI.setDefaultStyleV(theAlert.getDialogPane());
                theAlert.initModality(Modality.NONE);
    
                theAlert.setTitle(titleString);
                theAlert.setHeaderText(summaryIDLineString);
    
                JavaFXGUI.recordOpenWindowV( // Record showing of new Alert.
                    summaryIDLineString, null, theAlert);
                theAppLog.debug(
                  "Dialogger.showModelessJavaFXDialogReturnString(.) "
                  + "new Alert:\n" + summaryIDLineString + "\n" 
                  + detailsString);
                } // Make new Alert dialog.
            theAlert.setContentText(detailsString); // Customize with content.
            if (! theAlert.isShowing()) 
              theAlert.show();
            } );
          }
        catch (IllegalStateException theIllegalStateException) {
          resultString= "JavaFX-Dialog-Failed with " + theIllegalStateException;
          theAppLog.debug(
            "Dialogger.showModelessJavaFXDialogReturnString(.), during"
            + "\n  " + summaryIDLineString + "\n  " + detailsString + "\n  "
            + resultString);
          }
        return resultString;
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
