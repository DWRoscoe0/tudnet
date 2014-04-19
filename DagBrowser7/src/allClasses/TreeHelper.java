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

//import static allClasses.Globals.*;  // appLogger;

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

    * Selection: The thing selected this Component.
      This is the standard Java meaning.
      It is not a node in the tree,
      though it is associated with a node in the tree.
      
    * TreeSelectionEvent: The class used to pass locations of
      Wholes and Parts within the Infogora DAG.
      It should probably be its own class,
      but for now we're using the Java library class.

    ??? It might make more sense to call this class something else,
    such as TreeViewHelper, TreeHelper, or RightPanelHelper,
    or to eliminate it by integrating it into 
    an interface TreeAware JComponent subclass.

    */

  { // class TreeHelper

    private JComponent owningJComponent;  /* JComponent being helped.
      This is the JComponent to which other parts of the system will refer.
      That JComponent has a reference to this object also,
      so this linkage might be refered to as mutual-composition.  
      */

    // Constructor.
    
      TreeHelper( JComponent inOwningJComponent, TreePath inTreePath  )
        {
          owningJComponent= inOwningJComponent;
          setWholeWithPartAutoSelectV(inTreePath);
          }

    /* User input Listeners, for Keyboard and Mouse.  */

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
                commandGoToParentV();  // go to parent folder.
              else if (KeyCodeI == KeyEvent.VK_RIGHT)  // right-arrow key.
                commandGoToChildV();  // go to child folder.
              else if (KeyCodeI == KeyEvent.VK_ENTER)  // Enter key.
                commandGoToChildV();  // go to child folder.
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

        ??? Maybe replace with a more compact MouseAdapter implimentation.
        */
      
        @Override
        public void mouseClicked(MouseEvent inMouseEvent) 
          /* Checks for double click on mouse,
            which now means to go to the child folder,
            so is synonymous with the right arrow key.
            */
          {
            if (inMouseEvent.getClickCount() >= 2)
              commandGoToChildV();  // go to child folder.
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
      ??? Maybe these methods should be integrated with the ones in IJTree.
      Maybe IJTree should eventually be integrated with JComponent subclasses
      so that DagBrowserPanel can manipulate those all viewer components,
      including IJTree, with its navigation buttons.
      */

      public void commandGoToParentV() 
        /* Tries to make the parent of present Whole be the new Whole.  */
        {
          toReturn: {
            TreePath parentTreePath=  // Try getting parent of Whole.
              wholeTreePath.getParentPath();
            if (parentTreePath == null)  // There is no parent.
              break toReturn; // So do nothing.
            TreePath grandparentTreePath=  // Try getting parent of parent.
              parentTreePath.getParentPath();
            if (grandparentTreePath == null)  // There is no parent of parent.
              break toReturn; // So do nothing.
            TreePath childTreePath= getPartTreePath();
            if ( childTreePath != null )  // There is an active child so...
              Selection.  // ...in the visits tree...
                set( // record...
                  childTreePath  // ...that child.
                  );
            notifyListenersWithNewWholeV(  // Have listener switch to parent.
              parentTreePath
              );
            } // toReturn
          return;
          }

      public void commandGoToChildV() 
        /* Tries to make the present Part be the new Whole.  */
        {
          if  // act only if there is a child selected.
            ( getPartDataNode() != null )
            { // go to and display that child.
              notifyListenersWithNewWholeV(  // Have listener switch to child.
                getPartTreePath()
                );
              } // go to and display that child.
          }
    
    /* Whole and Part node code.  */

      /* Whole code.  The Whole is the larger tree node being viewed.
        This was previously called the Subject.
        Interestingly, after initialization, 
        Whole is never changed!
        */

        private TreePath wholeTreePath;  /* TreePath of node displayed.  
          This variable is never null.
          */

        private DataNode wholeDataNode;  /* DataNode of Whole.
          This is the last element of the wholeTreePath.
          This variable is never null.
          */

        private void setWholeV( TreePath inTreePath )
          /* This method stores inTreePath as the TreePath of the Whole.
            ? It also notifies any TreePathListener about it.
            It should be used only when there is no accessible Part.
            partTreePath will be set to null;  
            To prevent infinite recursion it acts only if
            inTreePath is different from the present wholeTreePath.
            */
          { 
            if  // This is a different wholeTreePath.
              ( ! inTreePath.equals( wholeTreePath ) )

              { // Update TreePath variables using the new value.

                { // Store Whole Part variables.
                  wholeTreePath= inTreePath;  // Store Whole TreePath.
                  wholeDataNode=  // Store last element for easy access.
                    (DataNode)wholeTreePath.getLastPathComponent();
                  } // Store Whole Part variables. 

                { // Set Part variables to none, for now.
                  partTreePath= null;
                  partDataNode= null;
                  } // Set Part variables to none, for now.
                  
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
                DataNode childDataNode=  // Try to get the child...
                  Selection.  // ...from the visits tree that was the...
                    setAndReturnDataNode( // ...most recently visited child...
                      inTreePath  // ...of the List at the end of the TreePath.
                      );
                if (childDataNode == null)  // if no recent child try first one.
                  { // try getting first ChildDagNode.
                    DataNode theDataNode=  // Get DataNode at end of TreePath.
                      (DataNode)inTreePath.getLastPathComponent();
                    if   // There are no children.
                      (theDataNode.getChildCount() <= 0)
                        ;  // Do nothing.
                      else  // There are children.
                        childDataNode=   // get first ChildDagNode.
                          theDataNode.getChild(0);
                    } // get name of first child.
                if (childDataNode != null)  // There is an accessible child.
                  setPartTreePathV(   // Set Part TreePath to be...
                    inTreePath.  // ...Whole TreePath with...
                      pathByAddingChild( childDataNode ) // ... child added.
                    );  // This will set the Whole also.
                  else  // There is no accessible child.
                  setWholeV( inTreePath );  // Save Whole TreePath only.
                } // Set things from inTreePath.
            }

        protected TreePath getWholeTreePath( )
          /* This method returns the TreePath representing the node
            being displayed.
            */
          { 
            return wholeTreePath;  // Return it.
            }

        protected DataNode getWholeDataNode()
          /* This method returns the DataNode of the Whole.  */
          { 
            return wholeDataNode;  // return DataNode of Part.
            }

      /* Part code.  The Part is the highlighted tree node within the Whole.
        It it one of possibly multiple child nodes being displayed.
        This was previously called the Selection,
        but was changed to avoid confusion with selection within
        the JComponent being used to represent the Whole.
        */

        private TreePath partTreePath; /* TreePath of Part.  
          The parent of this TreePath is wholeTreePath
          which represents the entire set being displayed.
          This assumes that there is only one sub-level displayed,
          so all items displayed have the same parent.
          This restriction might be removed later if
          multiple levels are displayed simultaneously.
          This variable is null if the Whole contains 
          no highlighted child.
          */

        private DataNode partDataNode; /* DataNode of Part.
          This is the last element of the partTreePath.
          This variable is null if the JComponent is displaying 
          a node with no highlighted child.
          */

        public void setPartTreePathV( TreePath inTreePath )
          /* This method stores inTreePath as the TreePath of the Part. 
            It also notifies any TreePathListener about it.
            To prevent infinite recursion it acts only if
            inTreePath is different from the present partTreePath.
            */
          { 
            if  // This is a different partTreePath.
              ( ! inTreePath.equals( partTreePath ) )
              { // Update TreePath variables using the new value.
                setWholeV(  // Set appropriate Whole TreePath to be...
                  inTreePath.getParentPath()  // ...parent of Part TreePath.
                  );  // This will set the Whole DataNode also.

                partTreePath= inTreePath;  // Store Part TreePath.
                partDataNode=  // Store last element for easy access.
                  (DataNode)partTreePath.getLastPathComponent();
                notifyListenersAboutPartV( inTreePath );
                } // Update TreePath variables using the new value.
            }

        protected TreePath getPartTreePath()
          /* This method returns the TreePath representing the Part. */
          { 
            return partTreePath;  // return TreePath of Part.
            }

        protected void setPartDataNodeV( DataNode inDataNode )
          /* This method sets the Part DataNode to be inDataNode.
            It updates other variables appropriately.
            But it assumes that the Whole is unchanged.
            It also notifies any TreePathListener about it.
            */
          {
            TreePath childTreePath= // Calculate child TreePath to be...
              wholeTreePath.  // ...the base TreePath with...
                pathByAddingChild( inDataNode );  // ... the child added.
            setPartTreePathV(  childTreePath );  // Set Part TreePath.
            }

        protected DataNode getPartDataNode()
          /* This method returns the DataNode of the Part.  */
          { 
            return partDataNode;  // return DataNode of Part.
            }
    
    // TreePathListener code (setting and using).

      /* Though the class TreeSelectionEvent is used by the following methods,
        neither this class or the class being helped by this class
        is a TreeSelectionModel, the usual source of TreeSelectionEvent-s.
        TreeSelectionEvent is being used simply because 
        it is a convenient way to pass:
        * a TreePath, which represents a change of location within a tree
        * a boolean, interpreted as follows:
          * false: the location is of the Whole.
          * true: the location is of the Part.
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

      public void notifyListenersAboutPartV( TreePath inTreePath )
        {
          notifyTreePathListenersV(  // Notify the listeners about...
            true,  // ... an internal/Part change...
            inTreePath  // ...to the Part path.
            );
          }

      private void notifyListenersWithNewWholeV( TreePath inTreePath )
        /* This method is named what it is instead of
          notifyListenersAboutNewWholeV( .. ) because the Whole
          never changes for the life of this component.
          When this method executes it causes this object's
          dereference and replacement by the Listener.
          */
        {
          notifyTreePathListenersV(  // Notify the listeners about...
            false,  // ... an external/Whole change...
            inTreePath  // ...to this path.
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
          fireValueChanged(  // fire value changed with...
            TheTreeSelectionEvent  // ...newly constructed event.
            );
          }

      private void fireValueChanged( TreeSelectionEvent e ) 
        {
          Enumeration<TreePathListener> listeners = 
            listenerVector.elements();
          while ( listeners.hasMoreElements() ) 
            {
              TreePathListener listener = 
                (TreePathListener)listeners.nextElement();
              listener.partTreeChangedV( e );
              }
        }

    } // class TreeHelper
