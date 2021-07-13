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
     * doing the best that's possible under difficult circumstances.
     * 
     * Why is this malfunction-anomaly distinction important?
     * 
     * It's important for 2 reasons:
     * * It helps to distinguish between 
     *   a problem caused by something inside the app,
     *   and a problem caused by something outside the app.
     * * The number of problems caused by something outside the app
     *   is likely to be more for this app than the average app.
     * 
     * Why would this app have more problems with outside causes?
     * 
     * A major purpose of this app is to be an anti-censorship tool.
     * Powerful interests exist whose business models and fortunes 
     * depend on hiding information that is damaging to them.  
     * Because of this, they would not want this app or similar apps to succeed,
     * and might try to to slow this app's development,
     * or sabotage its use after release.
     * 
     * Is this actually happening?  
     * 
     * It's difficult to be certain, but 
     * there is evidence that indicates that it is happening. 
     * 
     * What is the evidence?
     * 
     * Anomalies were encountered during the development of this app 
     * for which no cause could be found.
     * Some of the source code that deals with these anomalies 
     * is marked with the string "///ano".
     * Search this app's source code for that string to see examples.
     * A good place to start is the Infogora.main(.) method, 
     * which is this app's entry point.
     * 
     * Why is so much attention being paid to anomalies?
     * 
     * Severe app anomalies can ruin 
     * the user's experience of the app and the app's usefulness.  
     * They can cause the app to fail in its purpose.
     * 
     * How can one fix a problem if one can't determine the problem's cause?
     * 
     * One can't.
     * 
     * So, how does one deal with app anomalies?
     * 
     * A reasonable first step is to assume that 
     * the anomaly is a malfunction with an internal cause, 
     * and to try to find the cause using normal program debugging techniques.
     * These techniques include:
     * * analyzing log files
     * * program tracing
     * * breakpoints
     * * single stepping 
     * These techniques can be used on both first-party app code,
     * and third-party libraries if their source code is available.
     * With luck the cause of the problem will be found and can be fixed with 
     * a simple change to the app's source code.
     * 
     * What if an internal cause can't be found?  What's the next step?
     * 
     * It depends on the anomaly.
     * 
     * Can you give an example?
     * 
     * One example is network performance anomalies.
     * Sometimes, for unknown reasons, the network packets 
     * that carry information between apps connected to the Internet, 
     * don't get through.
     * Packets might be blocked going out, or blocked coming in, 
     * or lost in some other way, or delayed for long periods.  
     * Any of these problems can cause poor performance of a network app.
     * 
     * Aren't these network problems normal.
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
     * when many packets are lost.
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
     * In this case, one could do the following:
     * 1 Never give up.  Continue to send data until either 
     *   the data is received at its destination
     *   or the user cancels the data transfer request.
     *   Unless the blockage is complete and permanent, 
     *   the data will eventually get through.
     * 2 Use protocols that are stateful at a high level,
     *   to enable them to transfer, track, and acknowledge
     *   complex subsets of partially transfered data.
     * 3 Use protocols that can use multiple networks, if available,   
     *   to transfer data, including the ultimate backup network sneakernet.
     *    
     * Is there anything that can be done 
     * to deal with severe anomalies in general?
     * 
     * Yes.  The user should always be informed whenever a severe anomaly 
     * is causing the app to perform poorly.
     * The specifics of the anomaly should be provided if possible. 
     * 
     * Why do this?
     *
     * Two reasons:
     * 1 Doing so rightfully shifts the blame for the app's poor performance
     *   away from the app and toward the actual external cause.
     * 2 If the anomaly is being caused by a malicious actor,
     *   identifying the anomaly to the user whenever it happens 
     *   might discourage the actor doing it often.
     *   
     * Is this whole anomaly situation discouraging?
     * 
     * It is, in some ways.  In other ways it is motivating.  
     * In any case, development of this app will continue.
     *
     * Doesn't blaming poor app performance on anomalies,
     * possibly caused by unidentified malicious actors,
     * make you seem like complainers?
     * 
     * To some it might seem that way, but to the people that matter,
     * such as app users, app developers, 
     * and COINTELPRO-style operations targets, it won't.
     * 
     * What do you say to people who have difficulty believing this?
     * 
     * They might have less difficulty believing after they do some research, 
     * starting with the following recommended search terms:
     * 
     * * communication interception black room
     * * Microsoft AARD code
     * * COINTELPRO
     * * prank software
     * 
     * Are there other plans for handling anomalies?
     * 
     * Yes, but they are beyond the scope of this Q&A document.
     * 
     * 
     * 
     * 
     * 
     * 
     * Possible future work:
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

  
    /* Methods which display dialog boxes reporting anomalies.
     * These methods are presently called only by AppLog methods
     * which treat warnings, errors, and exceptions as report-able anomalies.
     */
  
    /*  ////
    public static boolean displayDialogB(String messageString)
      /* This method tries to display a mode-less dialog box 
       * that displays messageString as an anomaly.
       * It also plays a beep sound to get the user's attention.
       * It returns true if the dialog box was displayed, false otherwise.
       */
    /*  ////
      {
        java.awt.Toolkit.getDefaultToolkit().beep(); // Create audible Beep.

        boolean successB= // Try reporting via dialog box. 
            Dialogger.showModelessDialogB(messageString, "Anomaly Detected");
        return successB;
        }
    */  ////
  
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
            Anomalies.displayDialogReturnString(null, messageString);
        return resultString;
        }
  
    public static String displayDialogReturnString(
        String summaryIDLineString, String detailsString)
      /* This method tries to display a mode-less dialog box. 
       * It is similar to
       *   Dialogger.showModelessJavaFXDialogReturnString(
             String summaryIDLineString, String detailsString)
       * but with the following differences to indicate an anomaly:
       * * If summaryIDLineString is null, "Anomaly Detected" is used for it. 
       * * If summaryIDLineString is NOT null, 
       *   then "Anomaly Detected" is prepended to detailsString. 
       */
      {
        java.awt.Toolkit.getDefaultToolkit().beep(); // Create audible Beep.
        final String anomalyString= "Anomaly Detected";

        if (null == summaryIDLineString) 
          summaryIDLineString= anomalyString;
        else
          detailsString= anomalyString + "\n" + detailsString; 

        String resultString= // Try reporting via dialog box. 
            Dialogger.showModelessJavaFXDialogReturnString(
                "Anomaly Detected", detailsString);

        return resultString;
        }
  
    }
