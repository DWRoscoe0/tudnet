package allClasses;


import java.awt.event.KeyEvent;
import javax.swing.JComponent;
import javax.swing.tree.TreePath;

import allClasses.javafx.ConsoleNode;
import allClasses.javafx.EpiTreeItem;
import allClasses.javafx.Selections;
import allClasses.javafx.TreeStuff;

import static allClasses.AppLog.theAppLog;


public class ConsoleBase

  extends NamedDataNode

  {
    /* 
     * ///opt This class is being deprecated.
     * 
     *  It was meant to be part of a way of adding new command features.
     *  A new feature could be created by instantiating this DataNode class
     *  with the name of the Viewer class implementing the feature,
     *  and then implementing the Viewer class.
     *  This class uses Java reflection to calculate 
     *  the appropriate Viewer class from a name string.
     *  
     *  It was later decided that it is better to put 
     *  the intelligence and state of the command feature in a new DataNode,
     *  and display it with one of a small set of simple Viewer JComponents
     *  which display lists of items from the DataNode.
     *  
     */

    // Locally stored injected dependencies.
    //// private String viewerClassString;
    @SuppressWarnings("unused") ////
    private Persistent thePersistent;
  
    public ConsoleBase( // constructor
        String nameString, // Node name.
        //// String viewerClassString, // Name of viewer class for this node.
        Persistent thePersistent
        )
      {
        super.initializeV(nameString);
        
        //// this.viewerClassString= viewerClassString; 
        this.thePersistent= thePersistent;
        }
    
    public String getSummaryString()
      {
        return "";
        }

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
                "ConsoleBase stub, work-in-progress.");
          return theJComponent;
        }
    
    // variables.
    
      // static variables.

      // instance variables.
  
        // Constructor-injected variables.
        //// private Persistent thePersistent;
        
    // Constructors and constructor-related methods.

      public enum State
        {
          INITIAL_GREETING,
          AWAIT_DEVICE_INSERTION
          }

      State theState= State.INITIAL_GREETING;
      State nextState= null;
      
      @SuppressWarnings("unused") ////
      private void cycleStateMachineV()
        {
          while (true) {
            switch (theState) {
              case INITIAL_GREETING: initialGreetingV(); break;
              case AWAIT_DEVICE_INSERTION: awaitDeviceInsertionV(); break;
              }
            if (null == nextState) break;  // Exit loop and return.
            theState= nextState;
            nextState= null;
            }
        }
      
      private void initialGreetingV()
        {
          append(
              "\nTo begin building a volume with installation files,"+
              "\nplease insert the device to use into a USB port."+
              "\nIf you have already inserted one then please "+
              "\nremove it and insert it again.");
          nextState= State.AWAIT_DEVICE_INSERTION;
          }

      private void awaitDeviceInsertionV()
        {
          append(
              "\n\nThis is where we wait.");
          }

      private void append(String theString) {}

      @SuppressWarnings("unused") ////
      private void processKeyPressedV(KeyEvent theKeyEvent)
        //// Thread safety might be a problem.  
        //// Only insertString(.) is thread safe.
        {
          //// if(theKeyEvent.getKeyCode() == KeyEvent.VK_ENTER){
          theAppLog.debug( "ConsoleBase.processKeyPressedV(.) called.");
          //// theKeyEvent.consume(); // Prevent further processing.
          //// ioIJTextArea.append("\nA key was pressed.\n");
          }

      @SuppressWarnings("unused") ////
      private void putCursorAtEndDocumentV()
        {
          ////
          }

    // rendering methods.  to be added ??

    // TreeAware interface code for TreeHelper access.

      public TreeHelper theTreeHelper;

      public TreeHelper getTreeHelper() { return theTreeHelper; }

    // Other / TreeStuff methods.
      

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
        TreeStuff resultTreeStuff= ConsoleNode.makeTreeStuff(
                this,
                selectedDataNode,
                getContentString(),
                thePersistent,
                theDataRoot,
                theRootEpiTreeItem,
                theSelections
                );
        return resultTreeStuff; // Return the view that was created.
        }

    }
