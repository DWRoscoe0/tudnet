package allClasses;


public class Anomalies

  {

    /* Anomalies Class  ///ano Anomalies documentation and code.
     * 
     * What is the purpose of this Java class?
     * 
     * The purpose of this class is to contain documentation and 
     * some of the code that is useful for dealing with app anomalies.
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
     * 1 It helps to distinguish between 
     *   a problem caused by something inside the app,
     *   and a problem caused by something outside the app.
     * 2 The number of problems caused by something outside the app
     *   is likely to be greater for this app than most other apps.
     * 
     * Why would this app have more problems with outside causes?
     * 
     * A major purpose of this app is to be an anti-censorship tool.
     * Powerful interests exist whose business models and fortunes 
     * depend on hiding information that is damaging to them.  
     * Because of this, they would not want this app or similar apps to succeed,
     * and might try to slow this app's development,
     * or sabotage its use after release.
     * 
     * Is this actually happening?
     * 
     * There is evidence indicating that it is happening. 
     * 
     * What is the evidence?
     * 
     * Anomalies were encountered during the development of this app 
     * for which no fix-able cause could be found.
     * Some of these anomalies are marked in this app's source code 
     * with the string "///ano".  Search this app's source code 
     * for that string to learn about these anomalies.
     * A good place to start is the file TUDNet.java.
     * It contains the main(.) method which is this app's entry point.
     * 
     * Why is so much attention being paid to anomalies?
     * 
     * Severe app anomalies can ruin 
     * the user's experience of the app and the app's usefulness.  
     * They can cause the app to fail in its purpose.
     * 
     * How can one fix a problem if one can't find a fix-able cause?
     * 
     * One can't.
     * 
     * So, how does one deal with that situation?
     * 
     * It depends on the anomaly.
     * 
     * Can you give an example?
     * 
     * One example is network performance anomalies.
     * Sometimes, for unknown reasons, the network packets 
     * that carry data  between apps connected to the Internet, 
     * don't get through.
     * Packets might be blocked going out, or blocked coming in, 
     * or lost in some other way, or delayed for long periods.  
     * Any of these problems can cause poor performance of a network app.
     * 
     * Aren't these network problems normal.
     * Aren't network apps supposed to be able to deal with them?
     * 
     * No and No.
     * Network apps are designed to deal with occasional lost packets.
     * For example, if an app determines that 
     * an incoming data packet is overdue, 
     * it might request that the packet be sent again, repeatedly if necessary, 
     * until the packet is finally received.
     * 
     * So what's the problem?
     * 
     * The problem is that this works well only when a few packets are lost.
     * It will not work well if the anomaly is severe, 
     * when many packets are lost.
     * 
     * Has a severe version of this anomaly happened to the TUDNet app?
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
     * In the case of this example, one could do the following:
     * 1 Never give up.  Continue to request retransmission until either 
     *   the data packet is received or 
     *   the user cancels the data transfer.
     *   Unless the blockage is complete and permanent, 
     *   the data will eventually get through.
     * 2 Use protocols that are stateful at a high level,
     *   to enable them to transfer, track, and acknowledge
     *   complex subsets of partially transfered data.
     * 3 Use protocols that can use alternative networks, if available,   
     *   to transfer data, including the ultimate backup network, sneakernet.
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
     * Yes.
     * 
     * 
     * Here are some of those plans.
     * 
     * To review, a major purpose of this app is to be an anti-censorship tool.
     * Powerful interests exist whose business models and fortunes 
     * depend on hiding information that is damaging to them.  
     * Because of this, they would not want this app or similar apps to succeed,
     * and might try to to slow this app's development, 
     * or sabotage its use after release.
     * A way for them to do those things
     * is with the intentional creation of app anomalies 
     * 
     * Most unintentional app anomalies are 
     * malfunctions caused by programming mistakes, in other words, bugs, 
     * either in the app or in systems with which it interacts, 
     * for example the operating system.
     * Though there are exceptions, such as bugs involving race conditions,
     * most bugs are reproducible, and their causes can eventually be found. 
     * Most programmers know how to find and fix bugs.
     *
     * Intentional anomalies are very different.
     * They are malfunctions caused by a malicious actor, and not mistakes.
     * They are generally not reproducible because the malicious actor
     * can turn an anomaly on and off at will.
     * 
     * Another complication is that many anomalies are performance anomalies.
     * Things work, and work correctly, but they don't go as fast 
     * as they should.  For example:
     * * A network reply packet takes longer than it should to be received,
     *   or isn't received at all.
     * * Writing a block of data to a file takes longer than it should. 
     * Some performance anomalies can be converted to pass-fail anomalies
     * by placing a maximum limit on how long an operation takes,
     * but it is sometimes difficult to decide what the limit should be.
     * 
     * Here are some of the guidelines being used for 
     * dealing with the anomalies described above.
     * 
     * * Don't assume that an anomaly is a bug.
     *   If normal debugging techniques do not seem to be working,
     *   assume the anomaly is intentional and act accordingly.
     * 
     *   Create and use reusable code to make dealing with anomalies 
     *   easier and less costly.
     *
     * * Don't wait for new anomalies to appear.
     *   Preemptively add anomaly detection code.  For example:
     *   * Add code to handle exceptions that normally would not occur.
     *   * Wrap API calls with code that measures and detects 
     *     excessive API execution time.
     *   
     * * Log operations that could behave anomalously.
     *   It is acceptable and preferable to log 
     *   summaries of pass-fail and performance data,
     *   instead of producing a log entry for each individual operation.
     *
     * * Notify the user when an anomaly is happening,
     *   but don't make the notification more annoying than the anomaly.
     *   
     * * Provide a way for the user to adjust the settings which control 
     *   how anomalies are detected, logged, and reported by notification.
     *
     * * Recover from anomalies when possible.  Recovery could mean:
     *   * Retrying an operation that seems to have failed.
     *   * Performing an operation that failed by trying a different method. 
     *   * Restarting a thread that seems to have unexpectedly 
     *     terminated, blocked, or failed in some other way;
     *     or starting a new thread to replace the failed one.
     *   * Restarting the app's process.
     *   * Restarting the user device.
     *   
     * * There should be a color-coded Anomaly-Button that is visible 
     *   on all UI screens that indicates the overall anomaly state of the app.
     *   For example:
     *   * Green indicates that the app is performing normally.
     *   * Yellow indicates that the app is working 
     *     but performance is suffering.
     *   * Red indicates the app is severely disabled.
     *
     * * When the Anomaly-Button is activated,
     *   it should open a screen that displays
     *   a list of entries, one entry for each known app anomaly.
     *   Each entry, when activated, should:
     *   * Display a description of the anomaly, for example:
     *       "This anomaly happens when OutputStream.write(byte[]) method
     *        takes too long to execute."
     *   * Display whether the anomaly is occurring now.
     *   * Display at least a summary of recent occurrences of the anomaly.
     *   * Display the limits that was exceeded that triggered the anomaly.
     *   * Allow the user to view and edit the setting for the anomaly.
     * 
     * 
     * Possible specific future work:
     * 
     * * ///pos Integrate this Anomalies class into AppLog
     *   as an injectable service.
     *   This will mean creating a new AppLog method: anomaly(.),
     *   or simply continue to treat all error(.) calls as anomalies.
     * 
     * * ///pos When the user is notified of the occurrence of an anomaly, 
     *   give the user the option to suppress or control in other ways 
     *   the reporting of future instances of the same anomaly.  For example:
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

    public static String displayDialogReturnString(
        String messageString,
        boolean alertWithSoundB)
      /* This method tries to display a mode-less dialog box 
       * that displays messageString as an anomaly.
       * It also plays a beep sound to get the user's attention.
       * If it fails to display the dialog box,
       * it tries display the message to the console.
       * It returns true if the dialog box was displayed, false otherwise.
       */
      {
        String resultString= // Try reporting via dialog box. 
            Anomalies.displayDialogReturnString(
                null, messageString, alertWithSoundB);
        return resultString;
        }

    public static String testAndDisplayDialogReturnString(
        String nameString,
        String stateString,
        long maximumTimeMsL,
        long timeMsL)
      /* ///ano This method tests for and reports excessive-time Anomalies.
       * It tests whether the time timeMsL exceeds maximumTimeMsL 
       * for the time parameter with name nameString.
       * If the limit is exceeded then 
       * it displays or updates a mode-less dialog box 
       * which reports this as an Anomaly.
       */
      {
        String resultString= null;
        
        boolean limitExceededB= timeMsL > maximumTimeMsL;
        if (limitExceededB) // If excessive time used for dispatch, report it.
          {
            String summaryIDLineString= "Excessive time used for " + nameString;
            
            String detailsString= 
                "Operation: " + nameString
                + "\nTime used: " + timeMsL + "ms"
                + "\nTime limit: " + maximumTimeMsL + "ms"
                + "\nStatus: " + stateString;
            resultString= Anomalies.displayDialogReturnString(
                summaryIDLineString, detailsString, true);
            }
        return resultString; 
        }

    public static String displayDialogReturnString(
        String summaryIDLineString, 
        String detailsString, 
        boolean alertWithSoundB)
      /* This method tries to display a mode-less dialog box. 
       * It is similar to
       *   Dialogger.showModelessJavaFXDialogReturnString(
             String summaryIDLineString, String detailsString)
       * but with the following differences to indicate an anomaly:
       * * If summaryIDLineString is null, "Uncategorized Anomaly" is used. 
       * * If summaryIDLineString is NOT null,
       *   then "Anomaly Detected" is prepended to detailsString. 
       */
      {
        /// java.awt.Toolkit.getDefaultToolkit().beep(); // Create audible Beep.
        ///   ///ano The above line causes debug tracing to fail!

        if (null == summaryIDLineString) 
          summaryIDLineString= "Uncategorized Anomaly";
        else
          detailsString= "Anomaly Detected:\n\n" + detailsString; 

        String resultString= // Try reporting via dialog box. 
            Dialogger.showModelessJavaFXDialogReturnString(
                summaryIDLineString, detailsString, alertWithSoundB);

        return resultString;
        }

    }
