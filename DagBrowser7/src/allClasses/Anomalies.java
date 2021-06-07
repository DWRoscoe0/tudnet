package allClasses;


public class Anomalies

  {

    /* 
     * Anomalies Class  ///ano
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
     * How is an app anomaly different from an app malfunction?
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
     * Because of this, they would not want this app or similar apps to succeed,
     * and might try to to slow this app's development,
     * or sabotage its use after release.
     * 
     * Is this actually happening?  
     * 
     * It's difficult to know for certain, but 
     * there is evidence that indicates that it is happening. 
     * 
     * What is the evidence?
     * 
     * Anomalies were encountered during the development of this app 
     * for which no cause could be found.
     * Source code that deals with these anomalies 
     * is marked with the string "///ano".
     * Search this app's source code for that string to see examples.
     * A good place to start is the Infogora.main(.) method, 
     * which is this app's entry point.
     * 
     * Why is so much attention being paid to anomalies?
     * 
     * App anomalies can ruin the user's experience of the app 
     * and the app's usefulness.  
     * Anomalies can cause the app to fail in its purpose.
     * 
     * How can one fix a problem if one can't determine the problem's cause?
     * 
     * One can't.
     * 
     * So, how does one deal with app anomalies?
     * 
     * A reasonable first step is to assume that 
     * the anomaly has a cause that can be found,
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
     * It depends on the anomaly.
     * 
     * Can you give an example?
     * 
     * One example is network performance anomalies.
     * Sometimes, for unknown reasons, the network packets 
     * that carry information between devices on the Internet, 
     * don't get through.
     * Packets might be intermittently blocked going out, 
     * or blocked coming in, or lost in some other way, 
     * or delayed for long periods.  
     * Any of these problems can cause poor network app performance.
     * 
     * Aren't these problems normal.
     * Aren't network apps supposed to be able to deal with them?
     * 
     * Yes and yes.
     * Network apps are designed to deal with the occasional lost packet.
     * An app might do this by detecting that a packet is overdue, 
     * and causing it to be resent, repeatedly if necessary, 
     * until the packet is finally received.
     * 
     * So what's the problem?
     * 
     * The problem is that this works well only when a few packets are lost.
     * It will not work well if the anomaly is severe, 
     * when most of the packets are lost.
     * 
     * Has a severe version of this anomaly happened to this app?
     * 
     * Yes.
     * 
     * Often?
     * 
     * Yes.
     * 
     * How do you deal with severe anomalies?
     * 
     * Again, it depends on the anomaly.
     * In this case, one might do the following actions:
     * * Use protocols that are stateful at a high level
     *   to enable them to transfer, track, and acknowledge
     *   complex subsets of data.
     * * Use protocols that can use multiple or alternative networks, 
     *   including sneakernet, to transfer the data.
     * * Never give up.  Continue to send the data until either 
     *   the data is received at its destination
     *   or the user cancels the data transfer. 
     * 
     * Is there anything that can be done 
     * to deal with severe anomalies in general?
     * 
     * Yes.  The user should be informed when a severe anomaly 
     * is causing the app to perform poorly.
     * The specifics of the problem should be provided if possible. 
     * This should be done because:
     * 
     * * Doing so rightfully shifts the blame for the app's poor performance
     *   away from the app and toward the actual cause.
     *   
     * * If an anomaly is being caused by a malicious agent,
     *   identifying the anomaly to the user whenever it happens 
     *   might cause the agent to do it less often.
     *   
     * Is this whole anomaly situation discouraging?
     * 
     * It is, in some ways.  In other ways it is motivating.  
     * In any case, development of this app will continue.
     *
     * Doesn't blaming poor app performance on anomalies,
     * possibly caused by unidentified malicious agents,
     * make you seem like complainers?
     * 
     * To some it might, but to do otherwise would be dishonest.
     * To the people that matter, such as: 
     * serious users of the app, app developers, and 
     * people who have been targets of COINTELPRO-like operations;
     * it won't.
     * 
     * What do you say to people who have difficulty believing this?
     * 
     * They might have less difficulty believing after they do some research, 
     * starting with the following recommended search terms:
     * * communication interception black room
     * * Microsoft AARD code
     * * COINTELPRO
     * * prank software
     * 
     * Are there any other plans for handling anomalies?
     * 
     * Yes, but they are beyond the scope of this Q&A document.
     * 
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
