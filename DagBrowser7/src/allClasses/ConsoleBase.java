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

  {
  
    // static variables.

  
    // instance variables.

    // Constructor-injected variables.

    // Locally stored injected dependencies.
    @SuppressWarnings("unused") ////
    private Persistent thePersistent;

    // Other variables.
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
        ScheduledThreadPoolExecutor theScheduledThreadPoolExecutor ////
        )
      {
        super.initializeV(nameString);

        this.thePersistent= thePersistent;
        
        //// appendToDocumentV("ConsoleBase initial content.\n");
        outputStringBuffer.append(
            "ConsoleBase:  initial content by outputStringBuffer.append.\n");
        
        theScheduledThreadPoolExecutor.execute(this); // Start our thread.
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
        }

    public void run() // For our Thread.
      {
        mainThreadLogicV();
        }

    private void mainThreadLogicV() 
      {
        outputStringBuffer.append("ConsoleBase: thread starting.\n");
        mainLoop: while(true) {
         loopBody: {
          if (0 < outputStringBuffer.length()) {
            String outString= outputStringBuffer.substring(0,1);
            //// theAppLog.debug(
            ////     "ConsoleBase.mainThreadLogicV() char='"+outString+"'");
            appendToDocumentV(outString);
            outputStringBuffer.delete(0,1);
            EpiThread.interruptibleSleepB(20);
            break loopBody;
            }
          if (0 < inputStringBuffer.length()) {
            String inString= inputStringBuffer.substring(0,1);
            theAppLog.debug(
              "ConsoleBase.mainThreadLogicV() inString='"+inString+"'");
            inputStringBuffer.delete(0,1);
            outputStringBuffer.append(
                "The character '"+inString+"' was typed.\n");
            break loopBody;
            }
          //// EpiThread.interruptibleSleepB(1000);
          //// appendToDocumentV("ConsoleBase:Looping append.\n");
          LockAndSignal.Input theInput= // Waiting for any new inputs. 
            theLockAndSignal.waitingForNotificationOrInterruptE();
          if // Exiting loop if  thread termination is requested.
            ( theInput == Input.INTERRUPTION )
            break mainLoop;
        } // loopBody: 
        } // mainLoop: 
        }

    public void processInputKeyV(String theKeyString) {
      //// inputQueueOfKeyCode.enqueue(theKeyCode);
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
