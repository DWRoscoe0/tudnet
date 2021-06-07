package allClasses;

import static allClasses.AppLog.theAppLog;
import static allClasses.SystemSettings.NL;

public class Anomalies

  {

    /* 
     * Anomalies   ///ano
     * 
     * What is the purpose of this class?
     * 
     * The purpose of this class is to contain code 
     * that is useful for dealing with app anomalies.
     * 
     * What is an app anomaly?
     * 
     * An anomaly is a difference from the normal.
     * An app anomaly is behavior that is different from expected app behavior.
     * 
     * How is an anomaly different from a malfunction?
     * 
     * All malfunctions are anomalies, but not all anomalies are malfunctions.
     * 
     * Can you give an example?
     * 
     * For example, if an Internet connection is performing poorly
     * then an Internet app using that connection will perform poorly. 
     * This is an app anomaly.  It is not an app malfunction.
     * The app might actually be functioning perfectly, 
     * doing the best that's possible under the circumstances.
     * 
     * Why is this anomaly-malfunction distinction important?
     * 
     * It's important for 2 reasons:
     * * It helps to distinguish between 
     *   problems caused by something inside the app,
     *   and problems caused by something outside the app.
     * * The number of problems caused by something outside the app
     *   is likely to be more for this app than the average app.
     * 
     * Why would this app have more problems with outside causes?
     * 
     * A major purpose of this app is to be an anti-censorship tool.
     * Powerful interests exist whose business models 
     * depend on hiding information that is damaging to them.  
     * They would not want this app or similar apps to succeed,
     * and might try to to slow this app's development,
     * or sabotage its use after release.
     * 
     * Is this actually happening?  
     * 
     * It's difficult to know for certain, 
     * but the evidence seems to indicate that it is. 
     * 
     * What is the evidence?
     * 
     * Problems have been encountered during the development of this app 
     * for which no cause could be found.
     * Code that deals with anomalies is marked with the string "///ano".
     * Search this app's source code for that string to see examples.
     * A good place to start is the Infogora.main(.) method, 
     * which is this app's entry point.
     * 
     * Why is so much attention being paid to anomalies?
     * 
     * App anomalies can ruin the user's experience of the app
     * and they can ruin the app's usefulness.  
     * They can cause the app to fail in its purpose.
     * 
     * How can one fix a problem if one can't determine the problem's cause?
     * 
     * One can't.
     * 
     * So, how does one deal with app anomalies?
     * 
     * It depends on the anomaly, but a reasonable first step in most cases 
     * is to assume that the anomaly has a cause that can be found,
     * and to use normal program debugging techniques to try to find it.
     * These techniques include:
     * * analyzing log files
     * * program tracing
     * * breakpoints
     * * single stepping 
     * These techniques can be used on both first-party app code,
     * and third-party libraries if the source code is available.
     * With luck the cause of the anomaly will be found and can be fixed with 
     * a simple source code change.
     * 
     * What's the next step if the cause of the problem can't be found?
     * 
     * Again, it depends on the anomaly.
     * 
     * Can you give an example?
     * 
     * One example is network performance anomalies.
     * Sometimes, for unknown reasons, 
     * network packets are intermittently blocked going out, 
     * or blocked coming in, or lost in some other way, 
     * or delayed for long periods.  
     * Any of these problems can cause poor app performance.
     * These problems have happened even when 
     * both end points are on the same LAN, 
     * and when other network apps running at the same time work fine.
     * 
     * How do you deal with that?
     * 
     * The options are limited.
     * There is no way for the app to force packets 
     * though an uncooperative network connection.
     * It can detect lost packets, and retransmit them until they get through,
     * but not without negatively affecting app performance.
     * 
     * Is there anything else that can be done?
     * 
     * Yes, and this applies to not only this anomaly,
     * but to any anomaly that appears to have a cause 
     * that is outside the app's control.
     * The user should be informed when an anomaly is happening, 
     * and that the anomaly is causing the app's poor performance.
     * The specifics of the problem should be provided if possible. 
     * This should be done because:
     * 
     * * It rightfully shifts the blame for the poor performance
     *   away from the app and toward the actual cause.
     *   
     * * Identifying anomalies whenever they happen might make 
     *   whoever is causing them do it less often
     *   to reduce the risk of being exposed.
     *   
     * Is this whole anomaly situation discouraging?
     * 
     * It is in some ways.  In other ways it is motivating.  
     * In any case, development of this app will continue.
     *
     * Doesn't blaming poor app performance on somebody else
     * make you look like cry babies?
     * 
     * To some, it might.  But to state otherwise would be dishonest.
     * And to the people that matter: app developers, users, and
     * those who have been targets of COINTELPRO-like operations;
     * it won't.
     * 
     * What do you say to people who have difficulty believing this?
     * 
     * I would suggest that they do some research, 
     * starting with the following search terms:
     * * communication interception black room
     * * Microsoft AARD code
     * * COINTELPRO
     * * prank software
     * 
     * Are there any other plans for handling anomalies?
     * 
     * See below.
     * 
     * 
     * 
     * 
     * 
     * 
     * 
     * * ///pos Integrate this Anomalies class into AppLog
     *   as an injectable service.
     *   This will mean creating a new AppLog method: anomaly(.),
     *   or simply continue to treat all error(.) calls as anomalies.
     * 
     * * ///pos When an anomaly is reported, give the user the option to
     *   suppress or control in other ways the reporting of 
     *   future instances of the same anomaly.  For example:
     *   * Enable/Disable reporting.
     *   * Time-out threshold for anomalies involving excessive response times.
     *   * Enable/Disable throwing of a DeveloperException which, 
     *     if the app is running under an IDE,
     *     could be used to suspend the app and 
     *     allow the app developer to do some analysis.
     *     This may need to use a modal dialog instead of mode-less.
     *     
     * * ///pos Have an Anomaly Central screen which allows the user to browse
     *     all the known anomalies, and for each anomaly:
     *     * See a summary of recent occurrences. 
     *     * See a history of occurrences.
     *     * Allow the user to adjust trigger settings as described above.
     *     
     * * ///pos Better link anomalies with the app features
     *   whose performance they negatively affect, 
     *   and include the performance effects in anomaly reports.
     *   
     * * ///pos Anomalies are already logged, but maybe they should be
     *   logged in a separate log file, one for anomalies only.
     *     
     */
  
    public static boolean displayDialogAndLogB(String messageString) //////////
      /* This method is equivalent to displayDialogOnlyV(.) plus
       * it logs the message as an INFO entry, to prevent stack overflow.
       */
      {
        theAppLog.info(
            "Anomalies.displayDialogV(..) called," + NL + messageString);
        return displayDialogB(messageString);
        }
  
    public static boolean displayDialogB(String messageString)
      /* This method tries to display a mode-less dialog box 
       * that displays messageString as an anomaly.
       * It also plays a beep sound to get the user's attention.
       * It returns true if the dialog box was displayed, false otherwise.
       */
      {
        java.awt.Toolkit.getDefaultToolkit().beep(); // Create audible Beep.

        boolean successB= // Try reporting via dialog box. 
            Dialogger.showModelessDialogB(messageString, "Anomaly Detected");
        return successB;
        }
  
    public static String displayDialogReturnString(String messageString)
      /* This method tries to display a mode-less dialog box 
       * that displays messageString as an anomaly.
       * It also plays a beep sound to get the user's attention.
       * If it fails to display the dialog box,
       * it tries display the message to the console.
       * It returns true if the dialog box was displayed, false otherwise.
       */
      {
        java.awt.Toolkit.getDefaultToolkit().beep(); // Create audible Beep.

        String resultString= // Try reporting via dialog box. 
            Dialogger.showModelessJavaFXDialogReturnString(
                messageString, "Anomaly Detected");
        return resultString;
        }
  
    }
