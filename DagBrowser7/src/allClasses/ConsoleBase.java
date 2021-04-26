package allClasses;


import java.util.concurrent.ScheduledThreadPoolExecutor;

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

  /* This class is the DataNode basis for features that use 
   * a simple console-like user interface.
   * It and its subclasses work with the ConsoleNode class
   * to provide this behavior.
   * It provides keyboard input and text display output.
   * The text output is intentionally slowed for 
   * readability and comprehension.
   */

  {
  
    // static variables.

  
    // instance variables.

    // Constructor-injected variables.
      ScheduledThreadPoolExecutor theScheduledThreadPoolExecutor;

    // Other variables.
    private boolean threadRunningB= false;
    private StringBuffer inputStringBuffer= new StringBuffer();
    private StringBuffer outputStringBuffer= new StringBuffer();
    protected LockAndSignal theLockAndSignal= new LockAndSignal();
      // processInputKeyV(.) uses this for notification.
      // Subclasses may add notifications for additional inputs.
    protected PlainDocument thePlainDocument= new PlainDocument();
      // Internal document store.
      // Unlike thePlainDocument in TextStream2,
      // this has no EDT (Event Dispatch Thread) restrictions,
      // because it is not used directly by any GUI TextArea.

    public ConsoleBase( // constructor
        String nameString, // Node name.
        Persistent thePersistent,
        ScheduledThreadPoolExecutor theScheduledThreadPoolExecutor
        )
      {
        /// theAppLog.debug(
        ///   myToString()+"ConsoleBase.ConsoleBase(.) begins, nameString='"+nameString+"'");
        super.initializeV(nameString);
        
        this.theScheduledThreadPoolExecutor= theScheduledThreadPoolExecutor;

        /// theAppLog.debug(
        ///   myToString()+"ConsoleBase.ConsoleBase(.) ends, nameString='"+nameString+"'");
        }

    private synchronized void startThreadIfNeededV()
      {
        if (! threadRunningB) { // If our thread is not running
          theScheduledThreadPoolExecutor.execute(this); // start our thread
          threadRunningB|= true; // and record same.
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

    public void run() // For our Thread.
      {
        /// theAppLog.debug(myToString()+"ConsoleBase.run() begins.");
        mainThreadLogicV();
        /// theAppLog.debug(myToString()+"ConsoleBase.run() ends.");
        }

    protected void mainThreadLogicV()
      // This should be overridden by subclasses. 
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

    protected void reportWithPromptSlowlyAndWaitForKeyV(String reportString)
      /* This method outputs slowly any queued output text
       * followed by reportString, followed by a prompt to press Enter.
       * Then it waits for any key press or thread termination request
       * and returns.
       */
      {
        queueOutputV(
            "\n\n"
            + reportString
            + "\nPress Enter key to continue: "
            );
        promptSlowlyAndGetKeyString();
        }

    protected String promptSlowlyAndGetKeyString(String promptString)
      /* This method outputs slowly any queued output text
       * followed by prompt string,
       * then waits for and returns a key string, 
       * or null if thread termination is requested.
       */
      { 
        queueOutputV(promptString);
        return promptSlowlyAndGetKeyString();
        }

    protected String promptSlowlyAndGetKeyString()
      /* This method outputs slowly any queued output text,
       * then waits for and returns a key string, 
       * or returns null if thread termination is requested first.
       */
      {
        promptSlowlyV();
        return getKeyString(); // Wait for and return a new key.
        }

    protected void promptSlowlyV()
      /* This method outputs slowly any queued output text,
       * then flushes the keyboard input queue in preparation for
       * possible keyboard input.
       */
      {
        displayQueuedOutputSlowV();
        flushKeysV();
        }

    protected void outputSlowlyV(String theString)
      /* This method outputs theString slowly along with 
       * anything else previously queued.
       */
      {
        queueOutputV(theString);
        displayQueuedOutputSlowV();
        }

    protected void flushKeysV()
      /* This method flushes the keyboard input queue.
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
      {
        queueOutputV(theString);
        displayQueuedOutputSlowV();
        }
      
    protected void queueOutputV(String theString)
      /* Use this method to add content to the outputStringBuffer.
       * The content is always appended.
       */
      {
        outputStringBuffer.append(theString);
        }

    protected String getKeyString()
      /* This method waits for and returns the next key to appear in the queue.
       * It returns this key as a String,
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
       * from the keyboard input queue, or null if there is none.
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
      /* This method test whether there it is possible 
       * to extract the next key as a String
       * from the keyboard input queue.
       * It returns the key if so, or null if there is none.
       * All control characters are translated to \n.
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

    protected void displayQueuedOutputSlowV()
      /* This method outputs the content of the outputStringBuffer slowly,
       * one character at a time, appending each to the end of theDocument,
       * with several milliseconds between them.
       * This makes it clear that output is occurring,
       * and makes it easier to follow the output.
       * It doesn't stop until the outputStringBuffer is empty.
       */
      {
        while (0 < outputStringBuffer.length()) {
          int charactersToOutputI= outputStringBuffer.length();
          String outString= outputStringBuffer.substring(0,charactersToOutputI);
          /// theAppLog.debug(myToString()+"ConsoleBase.mainThreadLogicV() "
          ///     + "processing from outputStringBuffer \""+outString+"\"");
          appendToDocumentV(outString);
          outputStringBuffer.delete(0,charactersToOutputI);
          EpiThread.interruptibleSleepB(5);
          }
        }

    private void appendToDocumentV(String theString)
      // Convenience method that appends and handles exceptions.
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
       * is being used as an update-able display area.  
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
            newTextString, // by this new text
            SimpleAttributeSet.EMPTY // with no special attributes.
            );
          ///dbg logPlainDocumentStateV("after..replace");
        } catch (BadLocationException e) {
          theAppLog.warning(
              "ConsoleBase.replaceInDocumentV(.) failed, "+e);
          e.printStackTrace();
        }
      }

    /*  ///dbg
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
      // This method get some document text and and handles exceptions.
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
      /* This method simply forwards to thePlainDocument. */
      { 
        thePlainDocument.addDocumentListener(listener); 
        startThreadIfNeededV();
        }

    public void processInputKeyV(String theKeyString) {
      inputStringBuffer.append(theKeyString);

      theLockAndSignal.notifyingV();

      startThreadMaybeV();
      }

    private synchronized void startThreadMaybeV()
      {
        }

    
    // Swing, TreeAware, and TreeHelper code.

    public TreeHelper theTreeHelper;

    public TreeHelper getTreeHelper() { return theTreeHelper; }

    public JComponent getDataJComponent( 
        TreePath inTreePath, DataTreeModel inDataTreeModel 
        ) 
      /* Returns a JComponent which should be a viewer capable of displaying 
       * this DataNode and executing the command associated with it.
       * The DataNode to be viewed should be 
       * the last element of inTreePath,
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

    public TreeStuff makeTreeStuff( 
        DataNode selectedDataNode,
        Persistent thePersistent,
        DataRoot theDataRoot,
        EpiTreeItem theRootEpiTreeItem,
        Selections theSelections
        )
      /* This is the JavaFX version of getDataJComponent(.).
       * See DataNode for base version.
       * 
       * This method creates a ConsoleViewer Node object 
       * for displaying this DataNode.
       */
      {
        TreeStuff resultTreeStuff= null;
        try {
          resultTreeStuff = ConsoleNode.makeTreeStuff(
                  this,
                  selectedDataNode,
                  thePlainDocument.getText(0,thePlainDocument.getLength()),
                  thePersistent,
                  theDataRoot,
                  theRootEpiTreeItem,
                  theSelections
                  );
        } catch (BadLocationException e) {
          ////// TODO Auto-generated catch block
          e.printStackTrace();
        }
        return resultTreeStuff; // Return the view that was created.
        }

    }
