package allClasses;

import static allClasses.AppLog.theAppLog;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JOptionPane;

import static allClasses.SystemSettings.NL;

public class Anomalies 
  {

    /* The purpose of this class is to deal with anomalous behavior.
     * It eventually could do all of the following: 
     * * Receives calls reporting various potentially anomalous events.
     * * Decide which events require action, and depending on that decision
     *   it might:
     *   * Log the event.
     *   * Display a message to the user about the event
     *     and wait for a response, which could be:
     *     * Confirmation
     *     * Change of settings to control what events to report in the future.
     *   * Throw an exception which, if the app is running under an IDE,
     *     could suspend the app and allow the app developer 
     *     to examine the stack to determine the cause of the anomalous event.
     * * Use settings to control whether and how particular event types 
     *   should be reported. and allow user to change the settings.
     * 
     */
  
    @SuppressWarnings("unused")
    private boolean displayDialogB(  ////// being adapted. 
        final boolean informDontApproveB, String messageString, File appFile )
      /* This method displays a dialog box containing messageString
        which should be a string about a software update,
        and appFile, which is the file that contained the potential update.
        This method takes care of switching to the EDT thread, etc.
        If informDontApproveB is true, it only informs the user.
        If informDontApproveB is false, it asks for the user's approval
        and returns the approval as the function value.
        
        ///enh Change to allow user to reject update, return response,
        and have caller use that value to skip update.
       */
      {
        java.awt.Toolkit.getDefaultToolkit().beep(); // Beep.
        theAppLog.info("displayUpdateApprovalDialogB(..) begins.");
        final AtomicBoolean resultAtomicBoolean= new AtomicBoolean(true);
        final String outString= 
            messageString
            + NL + "The file that contains the other app is: "
            + appFile.toString()
            + NL + "It's creation time is: "
            + FileOps.dateString(appFile)
            + ( informDontApproveB ? "" : NL + "Do you approve?");
        EDTUtilities.runOrInvokeAndWaitV( // Run following on EDT thread. 
            new Runnable() {
              @Override  
              public void run() {
                if (!informDontApproveB) { // Approving.
                  int answerI= JOptionPane.showConfirmDialog(
                    null, // No parent component. 
                    outString,
                    "Infogora Info",
                    JOptionPane.OK_CANCEL_OPTION
                    );
                  resultAtomicBoolean.set(
                      (answerI == JOptionPane.OK_OPTION) );
                  }
                else // Informing only.
                  JOptionPane.showMessageDialog(
                    null, // No parent component. 
                    outString,
                    "Infogora Info",
                    JOptionPane.INFORMATION_MESSAGE
                    );
                }
              } 
            );
        theAppLog.info(
            "displayUpdateApprovalDialogB(..) ends, value= " 
            + resultAtomicBoolean.get() );
        return resultAtomicBoolean.get();
        }
  
    }
