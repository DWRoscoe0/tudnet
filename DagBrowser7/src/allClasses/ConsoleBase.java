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

/// import static allClasses.AppLog.theAppLog;


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
    private LockAndSignal theLockAndSignal= new LockAndSignal();
    private PlainDocument thePlainDocument= new PlainDocument();
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
        
        outputStringBuffer.append(
            "ConsoleBase:  initial content by outputStringBuffer.append.\n");

        //// startThreadV();

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

    private void appendToDocumentV(String theString)
      // Convenience method that appends and handles exceptions.
      {
        try { // Insert initial content.
          thePlainDocument.insertString( // Insert into document
              // 0,"ConsoleBase initial content.\n",SimpleAttributeSet.EMPTY);
              thePlainDocument.getLength(), // at its end
              theString, // the given string
              SimpleAttributeSet.EMPTY // with no special attributes.
              );
        } catch (BadLocationException e) {
          ////// TODO Auto-generated catch block
          e.printStackTrace();
        }
      }

    public void addDocumentListener(DocumentListener listener)
      /* This method simply forwards to thePlainDocument. */
      { 
        thePlainDocument.addDocumentListener(listener); 
        startThreadIfNeededV();
        }

    public void run() // For our Thread.
      {
        /// theAppLog.debug(myToString()+"ConsoleBase.run() begins.");
        mainThreadLogicV();
        /// theAppLog.debug(myToString()+"ConsoleBase.run() ends.");
        }

    private void mainThreadLogicV()
      {
        outputStringBuffer.append("ConsoleBase: mainThreadLogicV() begins.\n");
        mainLoop: while(true) {
         loopBody: {
          if (0 < outputStringBuffer.length()) {
            String outString= outputStringBuffer.substring(0,1);
            /// theAppLog.debug(myToString()+"ConsoleBase.mainThreadLogicV() "
            ///     + "processing from outputStringBuffer \""+outString+"\"");
            appendToDocumentV(outString);
            outputStringBuffer.delete(0,1);
            EpiThread.interruptibleSleepB(20);
            break loopBody;
            }
          if (0 < inputStringBuffer.length()) {
            String inString= inputStringBuffer.substring(0,1);
            /// theAppLog.debug(myToString()+"ConsoleBase.mainThreadLogicV() "
            ///     + "processing from inputStringBuffer \""+inString+"\"");
            inputStringBuffer.delete(0,1);
            outputStringBuffer.append(
                "The character '"+inString+"' was typed.\n");
            break loopBody;
            }
          LockAndSignal.Input theInput= // Waiting for any new inputs. 
            theLockAndSignal.waitingForNotificationOrInterruptE();
          if // Exiting loop if  thread termination is requested.
            ( theInput == Input.INTERRUPTION )
            break mainLoop;
        } // loopBody: 
        } // mainLoop: 
        outputStringBuffer.append("ConsoleBase: mainThreadLogicV() ends.\n");
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
      /* Returns a JComponent of type whose name is //////////doc
       * which should be a viewer capable of displaying 
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
                "ConsoleBase stub, JavaFX work in-progress.");
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
