package allClasses;

import java.awt.Component;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Enumeration;
import java.util.Vector;

import javax.swing.JComponent;
import javax.swing.tree.TreePath;
import javax.swing.event.TreeSelectionEvent;
//import javax.swing.event.TreeSelectionListener;


import static allClasses.Globals.*;  // appLogger;

public class TreeHelper
  implements KeyListener, MouseListener

  /* This class is being used to hold code which 
    is usefull for right panel interface TreeAware JComponents, 
    but because of Java's lack of multiple inheritance 
    was moved here and is now referenced by composition,
    usually with a variable named aTreeHelper.
    
    Concepts and their names:

    * Whole (formerly Subject): 
      The tree node displayed by this Component.

    * Part (formerly Selection within Subject): 
      Highlighted tree node within the Whole.  

    * Selection: The thing selected in this Component.
      This is the standard Java meaning.
      It is not a node in the tree,
      though it might associated with a node in the tree.
      
    * TreeSelectionEvent: The class used to pass locations of
      Wholes and Parts within the Infogora DAG.
      It should probably be its own class,
      but for now we're using the Java library class.
    
    ??? This class will probably be changed to 
    not force the Whole to be direct parent of the Part.  
    It would remain constant through the lift of the object.
    This will allow it to be used for more components, 
    including JTree with Whole being the tree root.
    
    ??? Breakout an interface of public methods for documentation purposes.

    ??? Name this class something else, such as TreeLogic, 
    or to eliminate it by integrating it into 
    an TreeAware JComponent subclass or abstract.

    */

  { // class TreeHelper

    private JComponent owningJComponent;  /* JComponent being helped.
      This is the JComponent to which other parts of the system will refer.
      That JComponent has a reference to this object also,
      so this linkage might be refered to as mutual-composition.  
      */

    // Constructors.
    
      TreeHelper( JComponent inOwningJComponent, TreePath inWholeTreePath  )
        /* Constructs a TreeHelper.
          inWholeTreePath identifies the root of the Whole subtree to display.
          The Part TreePath is auto-selected if possible.
          */
        {
          owningJComponent= inOwningJComponent;
          setWholeWithPartAutoSelectV( inWholeTreePath );
          }
    
      /*
      TreeHelper(   // ??? not used yet.  For when Part is not Whole child.
          JComponent inOwningJComponent, 
          TreePath inWholeTreePath,
          TreePath inPartTreePath
          )
        /* Constructs a TreeHelper.
          inWholeTreePath identifies the root of the Whole subtree to display.
          inPartTreePath identifies the part of the Whole to highlight.
          */
      /*
        {
          owningJComponent= inOwningJComponent;
          setWholeV( inWholeTreePath );
          //setPartV( inPartTreePath ); ???
          }
      */

    /* Keyboard and Mouse Listener methods.  */

      /* KeyListener methods, for 
        overriding normal Tab key processing
        and providing command key processing.

        Normally the Tab key moves the selection within a Table
        from cell to cell.  The modification causes Tab 
        to move keyboard focus out of the table to the next Component.  
        (Shift-Tab) moves it in the opposite direction.
        
        ??? Maybe replace with a more compact KeyAdapter implimentation.
        */
      
        public void keyPressed(KeyEvent inKeyEvent) 
          /* Processes KeyEvent-s.  
            The keys processed and consumed include:
              * Tab and Shift-Tab for focus transferring.
              * Right-Arrow and Enter keys to go to child.
              * Left-Arrow keys to go to parent.
            */
          { // keyPressed.
            int KeyCodeI = inKeyEvent.getKeyCode();  // cache key pressed.
            boolean KeyProcessedB= true;  // assume the key event will be processed here. 
            { // try to process the key event.
              // /* Tab decoded else by JList.
              if (KeyCodeI == KeyEvent.VK_TAB)  // Tab key.
                { // process Tab key.
                  Component SourceComponent= 
                    (Component)inKeyEvent.getSource();
                  int shift = // Determine (Shift) key state.
                    inKeyEvent.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK;
                  if (shift == 0) // (Shift) not down.
                    SourceComponent.transferFocus();  // Move focus to next component.
                    else   // (Shift) is down.
                    SourceComponent.transferFocusBackward();  // Move focus to previous component.
                  } // process Tab key.
              else 
              // Tab decoded else by JList.
              // */
              if (KeyCodeI == KeyEvent.VK_LEFT)  // left-arrow key.
                commandGoToParentB(true);  // go to parent folder.
              else if (KeyCodeI == KeyEvent.VK_RIGHT)  // right-arrow key.
                commandGoToChildB(true);  // go to child folder.
              else if (KeyCodeI == KeyEvent.VK_ENTER)  // Enter key.
                commandGoToChildB(true);  // go to child folder.
              else  // no more keys to check.
                KeyProcessedB= false;  // indicate no key was processed.
              } // try to process the key event.
            if (KeyProcessedB)  // if the key event was processed...
            {
              inKeyEvent.consume();  // ... prevent more processing of this key.
              }
            } // keyPressed.

        public void keyReleased(KeyEvent inKeyEvent) { }  // unused part of KeyListener interface.
        
        public void keyTyped(KeyEvent inKeyEvent) { }  // unused part of KeyListener interface.

      /* MouseListener methods, for user input from mouse.
        Because this is a helper class, putting the MouseListener methds
        in a MouseAdapter subclass would not be helpful.
        */

        @Override
        public void mouseClicked(MouseEvent inMouseEvent) 
          /* Checks for double click on mouse,
            which now means to go to the child folder,
            so is synonymous with the right arrow key.
            */
          {
            if (inMouseEvent.getClickCount() >= 2)
              commandGoToChildB(true);  // go to child folder.
            }
            
        @Override
        public void mouseEntered(MouseEvent arg0) { }  // unused part of MouseListener interface.
        
        @Override
        public void mouseExited(MouseEvent arg0) { }  // unused part of MouseListener interface.
        
        @Override
        public void mousePressed(MouseEvent arg0) { }  // unused part of MouseListener interface.
        
        @Override
        public void mouseReleased(MouseEvent arg0) { }  // unused part of MouseListener interface.
        
    /* Command methods.

      ??? Mr maybe impliment the Command pattern.
      In Java the Command pattern is implimented with the Action interface
      which extends the ActionListener interface.
      This might do everything I eventually want to do.
      */

      public boolean commandGoToParentB( boolean doB  ) 
        /* Tries to go to the parent node.
          First it checks whether moving is doable,
          that there is enough valid TreePath to move toward the parent.
          If there is and doB is true then it removes one element from 
          the present Part path and notifies the TreePath listeners.
          In any case it returns whether moving was doable.
          */
        {
          boolean doableB= false;  // Set result assuming command isn't doable.

          toReturn: {
            // Exit if not enough path valid for movement toward parent.
              TreePath a0TreePath= getPartTreePath();
              if (a0TreePath == null) break toReturn;
              TreePath a1TreePath= a0TreePath.getParentPath();
              if (a1TreePath == null) break toReturn;
              TreePath a2TreePath= a1TreePath.getParentPath();
              if (a2TreePath == null) break toReturn;
              //TreePath a3TreePath= a2TreePath.getParentPath();
              //if (a3TreePath == null) break toReturn;

            doableB= true;  // Override result to indicate command is doable.

            if (! doB)  // Command execution is not desired.
              break toReturn; // So exit now with calculated result.

            // Execute the command to move to parent.
              notifyListenersAboutPartV(  // Notify listener that...
                a1TreePath  // ...parent of part path should be new part path.
                );  // ??? Use ignoring-select instead?

          } // toReturn
            return doableB;  // Return doable result.

          }

      public boolean commandGoToChildB( boolean doB )
        /* Tries to go to an appropriate child if doB is true.
          It returns true if the command is/was doable, false otherwise.
          To facilitate the viewing of leaves, though they have no children,
          it returns true for them and executes the command by 
          adding an ErrorDataNode to the end of the present Part path.  
          ??? This method is too long.  Break it up.
          */
        {
            boolean doableB= false;  // Assume command is not doable.
          toReturn: {
            if ( getPartDataNode() == null )  // No part path.
              break toReturn; // So exit with not doable.
            if // Part is itself an ErrorDataNode.
              ( ErrorDataNode.isOneB( getPartDataNode() ) )
              break toReturn; // So exit with not doable.
            DataNode childDataNode= null;  // Storage for findingChild result.
          findingChild: {
            childDataNode=  // Try to find best child of part node.
              findBestChildDataNode( getPartTreePath() );
            if (childDataNode != null) break findingChild;  // Exit if found.
            if (!getPartDataNode().isLeaf())  // It is an empty non-leaf.
              break toReturn; // So exit with not doable.
            childDataNode=  // Use a place holder child for entering leaf.
              ErrorDataNode.newErrorDataNode();
          } // findingChild end.
            doableB= true;  // Override result to indicate command doable.

            if (! doB)  // Command execution is not desired.
              break toReturn; // So exit with result.

            // Command execution begins.
            notifyListenersAboutPartV(  // Set new part path to be...
              getPartTreePath().pathByAddingChild( // ...present part plus...
                childDataNode  // ...whatever child was found.
                )
              );  // Using setPartTreePathV(..) here doesn't work.

          } // toReturn end.
            return doableB;  // Return whether command is/was doable.
          }

      public boolean commandGoToNextB( boolean doB )
        /* Tries to go to the next node if doB is true.
          It returns true if the command is/was doable, false otherwise.
          */
        { return commandGoToPreviousOrNextB( doB, +1 ); }
      
      public boolean commandGoToPreviousB( boolean doB )
        /* Tries to the previous node if doB is true.
          It returns true if the command is/was doable, false otherwise.
          */
        { return commandGoToPreviousOrNextB( doB, -1 ); }
      
      public boolean commandGoToPreviousOrNextB( boolean doB, int incrementI )
        /* This is a helper method.  It is called by
          commandGoDownB(..) and commandGoUpB(..).
          If incrementI == +1 it tries to go to the next node.
          If incrementI == -1 it tries to go to the previous node.
          */
        { 
            boolean doableB= false;  

          toReturn: {
            TreePath a0TreePath= getPartTreePath();
            if (a0TreePath == null) break toReturn;  // Check base path.
            TreePath a1TreePath= a0TreePath.getParentPath();
            if (a1TreePath == null) break toReturn;  // Check parent path.
            DataNode a0DataNode= (DataNode)a0TreePath.getLastPathComponent();
            if // Check that base node is not an ErrorDataNode.
                ( ErrorDataNode.isOneB( a0DataNode ) )
              break toReturn;
            DataNode a1DataNode= (DataNode)a1TreePath.getLastPathComponent();
            int a0IndexI= // Get present index.
               a1DataNode.getIndexOfChild( a0DataNode );
            a0IndexI+= incrementI;  // Increment index to go up or down one.
            DataNode a0NewDataNode= a1DataNode.getChild(a0IndexI);
            if (a0NewDataNode == null) break toReturn;  // Check new node.
            
            doableB= true;  // Indicate that command is doable.
            if (! doB)  // Command execution is not desired.
              break toReturn; // So exit with result.
            
            // Command execution begins.
            //notifyListenersAboutPartV(  // Set new part path to be...
            //  a1TreePath.pathByAddingChild( // ...present parent plus...
            //    a0NewDataNode  // ...the new child.
            //    )
            //  );  // Using setPartTreePathV(..) here doesn't work.
            setPartDataNodeV(a0NewDataNode);  // Move to sibling.

          } // toReturn
            return doableB;
            
          }
          
    /* Whole and Part node code.  */

      /* Whole code.  The Whole is the larger tree node being viewed.
        This was previously called the Subject.
        It tends to be the immediate parent of the Part,
        but this might change when viewsr are able to display
        more than one level.  

        After initialization, Whole is never changed!
        */

        private TreePath theWholeTreePath;  /* TreePath of node displayed.  
          This variable is never null.
          */

        private DataNode theWholeDataNode;  /* DataNode of Whole.
          This is the last element of the theWholeTreePath.
          This variable is never null.
          */

        private void setWholeV( TreePath inTreePath )
          /* This method stores inTreePath as the TreePath of the Whole.
            ? It also notifies any TreePathListener about it.
            It should be used only when there is no accessible Part.
            thePartTreePath is set to null;  
            Actually now it is called only by the constructors.
            To prevent infinite recursion it acts only if
            inTreePath is different from the present theWholeTreePath.
            */
          { 
            if  // This is a different theWholeTreePath.
              ( ! inTreePath.equals( theWholeTreePath ) )

              { // Update TreePath variables using the new value.

                { // Store Whole Part variables.
                  theWholeTreePath= inTreePath;  // Store Whole TreePath.
                  theWholeDataNode=  // Store last element for easy access.
                    (DataNode)theWholeTreePath.getLastPathComponent();
                  } // Store Whole Part variables. 

                { // Set Part variables to non-null sentinel.
                  thePartDataNode= ErrorDataNode.newErrorDataNode();
                  thePartTreePath= theWholeTreePath.pathByAddingChild( 
                    thePartDataNode 
                    );
                  } // Set Part variables to non-null sentinel
                  
                } // Update TreePath variables using the new value.
            }

        private void setWholeWithPartAutoSelectV(TreePath inTreePath)
          /* This method sets the Whole variables from inTreePath
            and sets the Part also to the most recently
            visited child of the Whole, if there is one.
            */
          {
            if (inTreePath != null)  // TreePath is not null.
              { // Set things from inTreePath.
                setWholeV( inTreePath );  // Save Whole TreePath no
                DataNode childDataNode=  // Calculate best child.
                  findBestChildDataNode(inTreePath);
                if (childDataNode != null)  // There is an accessible child.
                  setPartDataNodeV( childDataNode );  // Store it.
                } // Set things from inTreePath.
            }

        public TreePath getWholeTreePath( )
          /* This method returns the TreePath representing the node
            being displayed.
            */
          { 
            return theWholeTreePath;  // Return it.
            }

        public DataNode getWholeDataNode()
          /* This method returns the DataNode of the Whole.  */
          { 
            return theWholeDataNode;  // return DataNode of Part.
            }

      /* Part code.  The Part is the highlighted tree node within the Whole.
        It it one of possibly multiple child nodes being displayed.
        This was previously called the Selection,
        but was changed to avoid confusion with selection within
        the JComponent being used to represent the Whole.
        The part variables should probably never remain null.
        thePartDataNode might be the ErrorDataNode,
        which means that the whole is a leaf,
        which can be displayed but not as a collection of children.
        */

        private TreePath thePartTreePath; /* TreePath of Part.  
          The parent of this TreePath is theWholeTreePath
          which represents the entire set being displayed.
          This assumes that there is only one sub-level displayed,
          so all items displayed have the same parent.
          This restriction might be removed later if
          multiple levels are displayed simultaneously.
          This variable is null if the Whole contains 
          no highlighted child.
          */

        private DataNode thePartDataNode; /* DataNode of Part.
          This is the last element of the thePartTreePath.
          This variable is null if the JComponent is displaying 
          a node with no highlighted child.
          */

        public void setPartTreePathV( TreePath inTreePath )
          /* This method stores inTreePath as the TreePath of the Part. 
            It also notifies any TreePathListener about it.
            To prevent infinite recursion it acts only if
            inTreePath is different from the present thePartTreePath.
            
            This used to set Whole to be its parent, but no longer does.
            */
          { 
            if  // This is a different thePartTreePath.
              ( ! inTreePath.equals( thePartTreePath ) )
              { // Update TreePath variables using the new value.
                thePartTreePath= inTreePath;  // Store Part TreePath.
                thePartDataNode=  // Store last element for easy access.
                  (DataNode)thePartTreePath.getLastPathComponent();
                notifyListenersAboutPartV( inTreePath );
                } // Update TreePath variables using the new value.
            }

        protected TreePath getPartTreePath()
          /* This method returns the TreePath representing the Part. */
          { 
            return thePartTreePath;  // return TreePath of Part.
            }

        protected void setPartDataNodeV( DataNode inDataNode )
          /* This method sets the Part DataNode to be inDataNode.
            It updates other variables appropriately.
            But it assumes that the Whole is unchanged.
            It also notifies any TreePathListener about it.
            */
          {
            TreePath childTreePath= // Calculate child TreePath to be...
              //theWholeTreePath  // ...the base TreePath with...
              thePartTreePath  // ...present path's...
                .getParentPath()  // ...parent with...
                .pathByAddingChild( inDataNode );  // ...new child added.
            setPartTreePathV(  childTreePath );  // Set Part TreePath.
            }

        protected DataNode getPartDataNode()
          /* This method returns the DataNode of the Part.  */
          { 
            return thePartDataNode;  // return DataNode of Part.
            }

    /* TreePathListener code for adding, removing, and triggering.

      Though the class TreeSelectionEvent is used by the following methods,
      neither this class nor the class being helped by this class
      is a TreeSelectionModel, the usual source of TreeSelectionEvent-s.
      TreeSelectionEvent is being used simply because 
      it is a convenient way to pass:
      * a TreePath, which represents a change of location within a tree
      It was also being used to pass a boolean, interpreted as follows:
        * false: the location is of the Whole.
        * true: the location is of the Part.
      but this is no longer true.
      */
      
      private Vector<TreePathListener> listenerVector =   // Listeners.
        new Vector<TreePathListener>();

      public void addTreePathListener( TreePathListener listener ) 
        {
          if ( listener != null && !listenerVector.contains( listener ) )
            listenerVector.addElement( listener );
          }

      public void removeTreePathListener(TreePathListener listener)
        {
          if ( listener != null )
            listenerVector.removeElement( listener );
          }

      private void notifyListenersAboutPartV( TreePath inTreePath )
        {
          Selection.set( inTreePath );  // Record as a selection.
          notifyTreePathListenersV(  // Notify the listeners about...
            true,  // ... an internal/Part change...
            inTreePath  // ...to the Part path.
            );
          }

      private void notifyTreePathListenersV( 
          boolean internalB, TreePath inTreePath 
          )
        /* Notifies any TreePathListeners of the JComponent being helped
          of a tree location change of either the Whole or the Part.
          inTreePath is the TreePath of the new location.
          internalB has the following meanings:
            true: the new location is of the Part.
              The new Part is within the same Whole as the old Part.
              The viewer JComponent for viewing it doesn't need to be changed.
            false: the new location is of the Whole.
              It must be handle by a new viewer JComponent.
              In this case, -this- object will be replaced.
          */
        {
          TreeSelectionEvent TheTreeSelectionEvent= //construct...
            new TreeSelectionEvent (  // ...a TreeSelectionEvent from...
              owningJComponent,  // ...the source JComponent being helped...
              inTreePath,  // ...with a change of inTreePath...
              internalB,  // ...being added as a selection path...
              inTreePath,  // ...and also being the new lead selection path...
              inTreePath  // ...and the old lead selection path.
              ); 
          firePartTreeChangedV(  // fire value changed with...
            TheTreeSelectionEvent  // ...newly constructed event.
            );
          }

      private void firePartTreeChangedV( TreeSelectionEvent e ) 
        {
          Enumeration<TreePathListener> listeners = 
            listenerVector.elements();
          while ( listeners.hasMoreElements() ) 
            {
              TreePathListener listener = 
                (TreePathListener)listeners.nextElement();
              try {  // ??? kluge.
                listener.partTreeChangedV( e );
                } 
              catch(ClassCastException ex) {
                appLogger.info( ""+ex );
                }
              }
        }

    // Utility methods.

      public void notifyListenersAboutChangeV( )
        /* This method is used to notify listeners that something other than
          the node TreePath, such as the display of other nodes, has changed.  
          It is communicated by sending a TreePath of null. 
          Presently it used to communicate when a node 
          is expanded or collapsed.
          */
        {
          notifyTreePathListenersV(  // Notify the listeners about...
            true,  // ... an internal/Part change...
            null // ...to the Part path.
            );
          }

      private DataNode findBestChildDataNode(TreePath inTreePath)
        /* This method tries to find and return the best child DataNode 
          of the node identified by inTreePath.  The best is:
            * The most recently visited child, if there is one.
            * The first child, if there is one.
          Otherwise it returns null.
          
          ??? Maybe instead of null it should return ErrorDataNode?
          */
        {
          DataNode childDataNode=  // Try to get the child...
            Selection.  // ...from the visits tree that was the...
              setAndReturnDataNode( // ...most recently visited child...
                inTreePath  // ...of the List at the end of the TreePath.
                );
          if (childDataNode == null)  // if no recent child try first one.
            { // try getting first ChildDataNode.
              DataNode theDataNode=  // Get DataNode at end of TreePath.
                (DataNode)inTreePath.getLastPathComponent();
              if   // There are no children.
                (theDataNode.getChildCount() <= 0)
                  ;  // Do nothing.
                else  // There are children.
                  childDataNode=   // get first ChildDataNode.
                    theDataNode.getChild(0);
              } // get name of first child.
          return childDataNode;  // Return final result.
          }

    } // class TreeHelper
