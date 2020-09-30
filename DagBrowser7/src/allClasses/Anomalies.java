package allClasses;

import static allClasses.AppLog.theAppLog;
import static allClasses.SystemSettings.NL;

public class Anomalies 
  {

    /* The purpose of this class is to deal with anomalous behavior.
     * It eventually could do all of the following: 
     * * Receive calls reporting various potentially anomalous events.
     * * Decide which events require action, and depending on that decision
     *   it might:
     *   * Log the event.
     *   * Display a message to the user about the event
     *     and wait for a response, which could be:
     *     * Confirmation
     *     * Change of settings to control what events to report in the future.
     *   * Throw an exception which, if the app is running under an IDE,
     *     and the correct Exception breakpoint is set,
     *     could suspend the app and allow the app developer 
     *     to examine the stack to determine the cause of the anomalous event.
     * * Use settings to control whether and how particular event types 
     *   should be reported. and allow user to change the settings.
     * 
     */
  
    public static void displayDialogV( 
        String messageString )
      /* This method displays a dialog box containing messageString.
        This method takes care of switching to the EDT thread, etc.
       */
      {
        theAppLog.info("Anomalies.displayDialogV(..) called," + NL + messageString);

        java.awt.Toolkit.getDefaultToolkit().beep(); // Create audible Beep.

        Dialogger.showModelessDialogV(messageString, "Anomaly Detected");
        }
  
    }
