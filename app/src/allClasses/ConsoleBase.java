package allClasses;


import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import javax.swing.JComponent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.tree.TreePath;

import allClasses.LockAndSignal.Input;
import allClasses.javafx.ConsoleNode;
import allClasses.javafx.EpiTreeItem;
import allClasses.javafx.Selections;
import allClasses.javafx.TreeStuff;

import static allClasses.AppLog.theAppLog;


public class ConsoleBase
  extends NamedDataNode
  implements Runnable

  {

    /* This class is the base class for DataNodes that use 
     * a simple console-like user interface.
     * It and its subclasses work with the ConsoleNode class
     * to provide this behavior.
     * It provides keyboard input and text display output.
     * The text output is intentionally slowed for 
     * readability and comprehension.
     */

    // instance variables.

    // Constructor-injected variables.
      ScheduledThreadPoolExecutor theScheduledThreadPoolExecutor;

    // Other variables.
    private boolean threadIsRunningB= false;
    private StringBuffer inputStringBuffer= new StringBuffer();
    private StringBuffer outputStringBuffer= new StringBuffer();
    protected LockAndSignal theLockAndSignal= new LockAndSignal();
      // processInputKeyV(.) uses this for notification.
      // Subclasses may add notifications for additional inputs.
    protected PlainDocument thePlainDocument= new PlainDocument(); /* 
      This is a document that is used to simulate a console-like display.
      It is not part of a TextArea or any other part of a user-interface,
      so there are no EDT or JAT thread restrictions on its use.
      There are methods for getting, setting, 
      and listening for changes in its content.
       */

    public ConsoleBase( // constructor
        String nameString, // Node name.
        Persistent thePersistent,
        ScheduledThreadPoolExecutor theScheduledThreadPoolExecutor
        )
      {
        /// theAppLog.debug(
        ///   myToString()+"ConsoleBase.ConsoleBase(.) begins, nameString='"+nameString+"'");
        super.setNameV(nameString);
        
        this.theScheduledThreadPoolExecutor= theScheduledThreadPoolExecutor;

        /// theAppLog.debug(
        ///   myToString()+"ConsoleBase.ConsoleBase(.) ends, nameString='"+nameString+"'");
        }

    private synchronized void startThreadIfNeededV()
      /* This method starts our thread if it hasn't started yet.  */
      {
        if (! threadIsRunningB) { // If our thread is not running
          theScheduledThreadPoolExecutor.execute(this); // start our thread
          threadIsRunningB|= true; // and record same.
          }
        }

    protected String myToString()
      // Method to simulate Object.toString().
      {
        return getClass().getName() + '@' + Integer.toHexString(hashCode())+":";
        }

    public String getSummaryString()
      {
        return "";
        }

    public void run()
      // This method makes this class a Runnable to be run by our Thread.
      {
        /// theAppLog.debug(myToString()+"ConsoleBase.run() begins.");
        mainThreadLogicV();
        /// theAppLog.debug(myToString()+"ConsoleBase.run() ends.");
        }

    protected void mainThreadLogicV()
      /* This method contains the code run by our thread.
        It is a simple loop that reads and echoes keys.
        It should be overridden by subclasses to provide other behaviors.
        */
      {
        queueOutputV("ConsoleBase echo test.\n\n");
        while(true) {
          queueOutputV("Press key to echo: ");
          String inString= promptSlowlyAndGetKeyString(); 
          if (null == inString) // Exit if termination requested.
            break;
          if ("\r".equals(inString)) // If needed replace Enter with
            inString= "\n"; // standard newline.
          queueOutputV("\nThe character '"+inString+"' was typed.\n");
          } 
        }


    // General ProgressReport code.  This might eventually move to ConsoleBase.

    private boolean progressReportsEnabledB= true;
    private final long msPerReportMsL= 100; // Trigger limit.
    private int progressReportHeadOffsetI= -1; /* -1 means report inactive. */
    private int progressReportMaximumLengthI= -1;
    private long progressReportNextTimeMsL;
    private ScheduledFuture<?> outputFuture;
    private Supplier<String> emptyProgressReportSupplierOfString= 
      new Supplier<String>() {
        public String get(){ return "!"; } 
        };
    private Supplier<String> progressReportSupplierOfString= 
      new Supplier<String>() {
        public String get(){ return "PROGRESS REPORT STUB!"; } 
        };


    protected synchronized void progressReportResetV()
      /* This method resets the progress report system
       * in preparation for producing empty progress reports.
       */
      {
        if (null != outputFuture) {
          outputFuture.cancel(true);
          outputFuture= null;
          }
        progressReportNextTimeMsL= // Set to do first report immediately.
            getTimeMsL();
        progressReportHeadOffsetI= thePlainDocument.getLength();
        progressReportMaximumLengthI= 0;
        this.progressReportSupplierOfString= 
            emptyProgressReportSupplierOfString;
        }

    protected synchronized void progressReportSetV(
        Supplier<String> newProgressReportSupplierOfString)
      /* This method sets the progress report system
       * in preparation for a new progress report.
       * newProgressReportSupplierOfString is the Supplier of 
       * the progress report String.
       */
      {
        progressReportResetV(); // Do the common stuff.
        this.progressReportSupplierOfString= // Override empty report
            newProgressReportSupplierOfString; // with this.
        outputFuture= theScheduledThreadPoolExecutor.scheduleAtFixedRate(
          new Runnable() { 
              public void run() {
                if (progressReportsEnabledB)
                  progressReportUpdateV();
                } 
              },
          1000, /// 1 second delay.
          1000, /// 1 second period.
          TimeUnit.MILLISECONDS
          );
        }

    protected synchronized void progressReportUpdateMaybeV()
      /* This method updates the progress report, maybe.
       * It depends on how much time has passed since the previous report.
       * If enough time has passed then it updates, otherwise it does nothing.
       */
      {
        long presentTimeMsL= getTimeMsL(); // Measure the time.
        if // Produce progress report if time remaining in period reached 0.
          (0 <= (presentTimeMsL-progressReportNextTimeMsL))
          { // Produce progress report and calculate when to do next one.
            progressReportNextTimeMsL= // Calculate time of next report.
                presentTimeMsL 
                + theLockAndSignal.periodCorrectedDelayMsL(
                    progressReportNextTimeMsL, msPerReportMsL);
            progressReportUpdateV();
            }
      }

    protected synchronized void progressReportUpdateV()
      /* This method unconditionally updates the progress report 
       * to the display by writing it to the thePlainDocument.
       * It gets the content from progressReportSupplierOfString.
       * It outputs slowly any part of the report 
       * that extends past the existing document,
       * thereby extending the document for next time.
       */
      { 
        String newTailString= progressReportSupplierOfString.get();
        int newTailLengthI= newTailString.length();
        if  // Document not being extended.
          (newTailLengthI <= progressReportMaximumLengthI)
          replaceDocumentTailAt1With2V( // Do simple and complete replacement.
              progressReportHeadOffsetI,
              newTailString
              );
          else // We will have a new maximum document length.
          { // Fast replace tail and slow output remainder.
            replaceDocumentTailAt1With2V( // Replace common part.
              progressReportHeadOffsetI, 
              newTailString.substring(0,progressReportMaximumLengthI));
            appendSlowlyV( // Append remainder slowly.
                newTailString.substring(progressReportMaximumLengthI));
            progressReportMaximumLengthI= newTailLengthI; // Update new maximum length.
          }
        }

    
    // Utility code begins.

    protected String testInterruptionGetConfirmation1ReturnResultString(
        String confirmationQuestionString,String resultDescriptionString)
      {
        progressReportsEnabledB= false;
        String returnString= null; // Assume no interruption.
      toReturn: {
        if  // Exit if no interruption key pressed.
          (null == tryToGetFromQueueKeyString())
          break toReturn;
        if // Exit if the interruption is not confirmed.
          (! getConfirmationKeyPressB("\n"+confirmationQuestionString))
          break toReturn;
        returnString= resultDescriptionString; // Override return value.
      } // toReturn:
        progressReportsEnabledB= true;
        return returnString;
      }

    protected boolean getConfirmationKeyPressB(
        String confirmationQuestionString)
      {
        boolean confirmedB= false;
        String responseString= promptSlowlyAndGetKeyString(
          "\n"
          + confirmationQuestionString
          + " [y/n] "
          );
        queueAndDisplayOutputSlowV(responseString); // Echo response.
        responseString= responseString.toLowerCase();
        if ("y".equals(responseString))
          confirmedB= true;
        return confirmedB;
        }

    protected void appendWithPromptSlowlyAndWaitForKeyV(String theString)
      /* This method appends slowly to the document any queued output text
       * followed by theString, followed by a prompt to press Enter.
       * Next it flushes keyboard input and waits for 
       * an Enter-terminated String or a thread termination request.
       * Then it returns.
       */
      {
        queueOutputV(
            theString
            + "\n\nPress the Enter key to continue: "
            );
        promptSlowlyAndGetKeyString();
        }

    protected String promptSlowlyAndGetKeyString(String promptString)
      /* This method appends slowly to the document any queued output text
       * followed by promptString.
       * Next it flushes keyboard input and waits for
       * an Enter-terminated key String or a thread termination request.
       * It returns the entered String, 
       * or null if thread termination was requested.
       */
      { 
        queueOutputV(promptString);
        return promptSlowlyAndGetKeyString();
        }

    protected String promptSlowlyAndGetKeyString()
      /* This method appends slowly to the document any queued output text.
       * Next it flushes keyboard input and waits for
       * an Enter-terminated key String or a thread termination request.
       * It returns the entered String, 
       * or null if thread termination was requested.
       */
      {
        promptSlowlyV();
        return getKeyString();
        }

    protected void promptSlowlyV(String promptString)
      /* This method appends promptString to the text queue.
       * Next it appends slowly to the document any queued output text.
       * Next it flushes the keyboard input queue in preparation for
       * possible new keyboard input.
       */
      {
        queueOutputV(promptString);
        promptSlowlyV();
        }

    protected void promptSlowlyV()
      /* This method appends slowly to the document any queued output text.
       * Next it flushes the keyboard input queue in preparation for
       * possible new keyboard input.
       */
      {
        appendQueuedOutputSlowV();
        flushKeysV();
        }

    protected void appendSlowlyV(String theString)
      /* This method appends slowly to the document any queued output text
       * followed by promptString.
       */
      {
        queueOutputV(theString);
        appendQueuedOutputSlowV();
        }

    protected void flushKeysV()
      /* This method flushes the keyboard input queue.
       * This is done to prevent processing any keyboard keys
       * that were input before the app was ready to accept them.
       * This is to reduce the risk of executing a command that destroys data. 
       */
      {
        String keyString;
        while (true) { // Keep getting keys until the key queue is empty.
          keyString= tryToGetFromQueueKeyString(); // Try getting a key.
          if (null == keyString) break; // Exit if null meaning queue empty.
          theAppLog.debug("ConsoleBase.flushKeysV(): flushed <"
            + AppLog.glyphifyString(keyString)
            + ">");
          }
        }

    protected void queueAndDisplayOutputSlowV(String theString)
      /* This method appends theString to the output queue.
       * Next it appends slowly to the document any queued output text,
       * which includes anything previously queued plus theString.
       * Next it returns.
       */
      {
        queueOutputV(theString);
        appendQueuedOutputSlowV();
        }
      
    protected void queueOutputV(String theString)
      /* This method adds theString to the output queue.
       */
      {
        outputStringBuffer.append(theString);
        }

    protected String getKeyString()
      /* This method waits for and returns
       * the next key to appear in the input queue.
       * It returns the key as a String,
       * or returns null if thread termination is requested first.
       * It will wait until one of those input types happens.
       */
      {
        String keyString;
        while (true) {
          keyString= tryToGetFromQueueKeyString();
          if (null != keyString) {
            theAppLog.debug("ConsoleBase.getKeyString(): <"
              + AppLog.glyphifyString(keyString) + ">");
            break; // Exit if got key.
            }
          LockAndSignal.Input theInput= // Waiting for any new inputs. 
            theLockAndSignal.waitingForNotificationOrInterruptE();
          if // Exiting loop with null if thread termination is requested.
            (Input.INTERRUPTION == theInput)
            break;
          }
        return keyString;
        }

    protected String tryToGetFromQueueKeyString()
      /* This method tries to extract the next key as a String
       * from the keyboard input queue.  It always returns immediately.
       * If there is no key available then it returns null.
       */
      {
        String inString= testGetFromQueueKeyString();
        if (null != inString) { // If got key, delete it from queue.
          inputStringBuffer.delete(0,1);
          }
        /// theAppLog.debug(myToString()+"ConsoleBase.mainThreadLogicV() "
        ///     + "processing from inputStringBuffer \""+inString+"\"");
        return inString;
        }

    protected String testGetFromQueueKeyString()
      /* This method tests whether it is possible 
       * to return a key from the keyboard input queue.
       * It returns the next key as a String if there is one, 
       * otherwise it returns null.
       * All control characters are translated to \n.
       * It does not remove the key from the queue.
       */
      {
        String inString= null;
        /// theAppLog.debug("ConsoleBase.testGetFromQueueKeyString() buffer='"
        ///  +AppLog.glyphifyString(inputStringBuffer.toString())+"'");
        if (0 < inputStringBuffer.length()) {
          inString= inputStringBuffer.substring(0,1);
          char C= inString.charAt(0);
          if (Character.isISOControl(C)) { // Translate all control characters
            inString= "\n"; // to standard newline character.
            }
          }
        return inString;
        }

    protected void appendQueuedOutputSlowV()
      /* This method appends slowly to the document any queued output text.
       * It does this by removing each character from the output queue, 
       * one character at a time, appending each to the end of theDocument,
       * with a small delay after each character.
       * These delays makes it clearer to the user that output is occurring,
       * and makes it easier to follow the output,
       * if the document is being displayed to a user interface.
       * This method doesn't stop until the outputStringBuffer is empty.
       */
      {
        while (0 < outputStringBuffer.length()) {
          int charactersToOutputI= 1;
          String outString= outputStringBuffer.substring(0,charactersToOutputI);
          /// theAppLog.debug(myToString()+"ConsoleBase.mainThreadLogicV() "
          ///     + "processing from outputStringBuffer \""+outString+"\"");
          appendToDocumentV(outString);
          outputStringBuffer.delete(0,charactersToOutputI);
          EpiThread.interruptibleSleepB(2);
          }
        }

    private void appendToDocumentV(String theString)
      // Convenience method that appends to the document and handles exceptions.
      {
        replaceInDocumentV(
          thePlainDocument.getLength(),
           0,
           theString
           );
      }

    protected void replaceDocumentTailAt1With2V(
        int tailOffsetI,String newTailString)
      /* This method replaces the tail end of the document.
       * This is used mainly for when the tail of the document
       * is being used as an update-able text display area.  
       */
      {
        replaceInDocumentV( // Replace the text in the document
           tailOffsetI, // from here
           thePlainDocument.getLength()-tailOffsetI, // to the document end 
           newTailString // with this new text.
           );
        }

    private void replaceInDocumentV(
           int oldTextOffsetI,
           int oldTextLengthI,
           String newTextString
           )
      /* This method replaces a piece of the document by a new  piece
       * and handles exceptions.
       */
      {
        try {
          /*  ///dbg
          logPlainDocumentStateV("before.replace");
          theAppLog.debug(
              "ConsoleBase.replaceInDocumentV(.) at " + oldTextOffsetI
              + " " + oldTextLengthI
              + " old chars:\n'" + AppLog.glyphifyString(
                  getTextFromDocumentString(oldTextOffsetI,oldTextLengthI)) 
              + "' with " + newTextString.length()
              + " new chars:\n'" + AppLog.glyphifyString(newTextString)
              + "'"
              );
          */  ///dbg
          thePlainDocument.replace( // Replace in document the old text
            oldTextOffsetI, // that starts here
            oldTextLengthI, // and is this long
            newTextString, // with this new text
            SimpleAttributeSet.EMPTY // with no special attributes.
            );
          ///dbg logPlainDocumentStateV("after..replace");
        } catch (BadLocationException theBadLocationException) {
          theAppLog.warning(
            "ConsoleBase.replaceInDocumentV(.) " + theBadLocationException);
        }
      }

    /*  ///dbg  Some methods I was using to debug.

    private void logPlainDocumentStateV(String contextString)
      {
        int lengthI= thePlainDocument.getLength();
        theAppLog.debug(
            "ConsoleBase.logPlainDocumentStateV(.) " + contextString 
            + " " + lengthI
            + " chars:\n'"
              + AppLog.glyphifyString(
                  getTextFromDocumentString(0, lengthI)) 
              + "'"
            );
        }

    private String getTextFromDocumentString(int offsetI,int lengthI)
      // This method gets some document text and and handles exceptions.
      {
        String resultString;
        try {
         resultString= thePlainDocument.getText(offsetI,lengthI);
        } catch (BadLocationException e) {
          resultString= "ConsoleBase.getTextFromDocumentString(.) failed, "+e;
          theAppLog.warning(resultString);
          e.printStackTrace();
        }
      return resultString;
      }

    */  ///dbg

    public void addDocumentListener(DocumentListener listener)
      /* This method simply forwards to thePlainDocument. 
       * thePlainDocument will be the actual event source.  
       * */
      { 
        thePlainDocument.addDocumentListener(listener); 
        startThreadIfNeededV();
        }

    public void processInputKeyV(String theKeyString) 
      /* This method processes key input from the user.
       * It is meant to be called from a JavaFX Node handling KeyEvent's.
       * Key input is in theKeyString.  
       * It is normally one character but can be more.
       * The key or keys are appended to the key input queue
       * and appropriate notifications are made.
       */
      {
        inputStringBuffer.append(theKeyString);

        theLockAndSignal.notifyingV();
        startThreadIfNeededV();
        }


    // Swing and JavaFX helper code.

    @Override
    public JComponent getDataJComponent( 
        TreePath inTreePath, DataTreeModel inDataTreeModel 
        ) 
      /* This is a mostly non-functional stub.
       * It exists so that something will be displayed 
       * if this DataNode is displayed in the old Java Swing UI.
       */
      {
          JComponent theJComponent;
          
          theJComponent= 
            new TitledTextViewer( 
                inTreePath, 
                inDataTreeModel,
                "Not implemented in the Java Swing user interface.\n" +
                "For this feature, use the new JavaFX user interface instead.");
          return theJComponent;
        }

    @Override
    public TreeStuff makeTreeStuff( 
        DataNode selectedDataNode,
        Persistent thePersistent,
        DataRoot theDataRoot,
        EpiTreeItem theRootEpiTreeItem,
        Selections theSelections
        )
      /* This method creates a TreeStuff appropriate for 
       * use in interacting with this DataNode subclass object
       * using the JavaFX GUI.
       * That object will contain a reference to an appropriate 
       * ConsoleNode JavaFX UI Node subclass object.
       */
      {
        TreeStuff resultTreeStuff= null;
        try {
          resultTreeStuff= // Delegate to ConsoleNode.
            ConsoleNode.makeTreeStuff(
              this,
              selectedDataNode,
              thePlainDocument.getText(0,thePlainDocument.getLength()),
              thePersistent,
              theDataRoot,
              theRootEpiTreeItem,
              theSelections
              );
        } catch (BadLocationException theBadLocationException) {
          theAppLog.warning(
            "ConsoleBase.makeTreeStuff(.) " + theBadLocationException);
        }
        return resultTreeStuff; // Return the view that was created.
        }


    protected long getTimeMsL()
      { 
        return System.currentTimeMillis();
        // return System.nanoTime()/1000;
        }

    }
