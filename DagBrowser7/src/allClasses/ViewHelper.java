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
import javax.swing.event.TreeSelectionListener;

import static allClasses.Globals.*;  // appLogger;

public class ViewHelper
  implements KeyListener, MouseListener

  /* This class is being used to hold code which usefull for
    right panel JComponents, 
    but because of Java's lack of multiple was moved here
    and is now referenced by composition,
    usually with a variable named aViewHelper.
    
    ??? It might make more sense to call this class something else,
    such as TreeViewHelper, TreeHelper, or RightPanelHelper.

    Originally code in this class came mostly from base class DagNodeViewer.
    That code was mainly about notifying Listeners about 
    TreeSelectionEvents from a JComponent, 
    but the code has since expanded.
    */

  { // class ViewHelper

    private JComponent owningJComponent;  /* JComponent being helped.
      This is the JComponent to which other parts of the system will refer.
      That JComponent has a reference to this object also,
      so this linkage might be refered to as mutual-composition.  
      */

    // constructor.
    
      ViewHelper( JComponent inOwningJComponent )
        {
          owningJComponent= inOwningJComponent;
          }
 
    /* User input Listeners, for Keyboard and Mouse.  */

      /* KeyListener methods, for 
        overriding normal Tab key processing
        and providing command key processing.
        Normally the Tab key moves the selection from table cell to cell.
        The modification causes Tab to move keyboard focus out of the table
        to the next Component.  (Shift-Tab) moves it in the opposite direction.
        
        ??? Maybe replace with a more compact KeyAdapter implimentation.
        */
      
        public void keyPressed(KeyEvent inKeyEvent) 
          /* Processes KeyEvent-s.  
            The keys processed and consumed include:
              Tab and Shift-Tab for focus transferring.
              Right-Arrow and Enter keys to go to child.
              Left-Arrow keys to go to parent.
            */
          { // keyPressed.
            int KeyCodeI = inKeyEvent.getKeyCode();  // cache key pressed.
            appLogger.info("ViewHelper.keyPressed("+KeyCodeI+") begin.");
            boolean KeyProcessedB= true;  // assume the key event will be processed here. 
            { // try to process the key event.
              // /* Tab decoded else by JList.
              if (KeyCodeI == KeyEvent.VK_TAB)  // Tab key.
                { // process Tab key.
                  appLogger.info("ViewHelper.keyPressed("+KeyCodeI+"), it's a tab" );
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
              appLogger.info("ViewHelper.keyPressed("+KeyCodeI+") key consumed.");
              }
            appLogger.info("ViewHelper.keyPressed("+KeyCodeI+") end.");  // ???
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
        
    // Command methods ??? being moved.

      public void commandGoToParentV() 
        /* Tries to go to and display the parent of this object. */
        { // commandGoToParentV().
          appLogger.info("ViewHelper.CommandGoToParentV() begin.");
          //TreePath subjectTreePath=  // get the parent of selection.
          //  GetSelectedChildTreePath().getParentPath();
          TreePath grandParentTreePath=  // try getting parent of the parent.
            subjectTreePath.getParentPath();
          { // process attempt to get grandparent.
            if (grandParentTreePath == null)  // there is no grandparent.
              ; // do nothing.  or handle externally?
            else  // there is a parent.
              { // record visit and display parent.
                TreePath childTreePath= getSelectionTreePath();
                if ( childTreePath != null )
                  Selection.  // In the visits tree...
                    set( // record...
                      childTreePath  // ...the new selected TreePath.
                      );
                setSelectionTreePathV(    // kluge so Notify will work.
                  grandParentTreePath 
                  );
                notifyTreeSelectionListenersV(   // let listener handle it.
                  false  // Tell it to use new JComponent.
                  );
                } // record visit and display parent.
            } // process attempt to get parent.
          appLogger.info("ViewHelper.CommandGoToParentV() end.");
          } // commandGoToParentV().

      public void commandGoToChildV() 
        /* Tries to go to and displays a presentlly selected child 
          of the present DataNode.  
          */
        { // commandGoToChildV().
          appLogger.info("ViewHelper.commandGoToChildV() begin.");
          if  // act only if there is a child selected.
            ( selectionDataNode != null )
            { // go to and display that child.
              notifyTreeSelectionListenersV(   // let listener handle it.
                false  // Use new JComponent.
                );
              } // go to and display that child.
          appLogger.info("ViewHelper.commandGoToChildV() end.");
          } // commandGoToChildV().
    
    /* Subject and Selection code.
      ??? This is being redesigned to support both
      the TreePath of the entire item       
      and the TreePath of the selected item within that component.
      The two  are related.  Changing can change the others.
      ??? They might also eventually automaticly trigger
      TreeSelectionEvents.

      ??? It will eventually support viewing a subject
      which contains no children and therefore no selection.
      */

      /* Subject code.  The subject is the thing being viewed.  */

        private TreePath subjectTreePath;  /* TreePath of node displayed.  
          This variable is never null.
          */

        private DataNode subjectDataNode;  /* DataNode of subject. ???
          This is the last element of the subjectTreePath.
          This variable is never null.
          */

        protected void setSubjectTreePathV( TreePath inTreePath )
          /* This method stores the TreePath representing the node
            being displayed.
            It should be used only there is no selected subnode.
            selectionTreePath will be set to null;  
            */
          { 
            subjectTreePath= inTreePath;  // Store subject TreePath.
            subjectDataNode=  // Store last element for easy access.
              (DataNode)subjectTreePath.getLastPathComponent();
            
            { // Set selection assuming there is none.
              selectionTreePath= null;
              selectionDataNode= null;
              } // Set selection assuming there is none.
            }

        protected void setSubjectTreePathWithAutoSelectV(TreePath inTreePath)
          /* This method calculates sets the Subject from inTreePath
            and sets the Selection also to the most recently
            visited child, if there is one.
            */
          { // setSubjectTreePathWithAutoSelectV(inTreePath)
            setSubjectTreePathV( inTreePath );  // Save subject TreePath.
              // This will [temporarilly] set the selection TreePath to null.
            DataNode childDataNode=  // Try to get the child...
              Selection.  // ...from the visits tree that was the...
                setAndReturnDataNode( // ...most recently visited child...
                  inTreePath  // ...of the List at the end of the TreePath.
                  );
            if (childDataNode == null)  // if no recent child try first one.
              { // try getting first ChildDagNode.
                DataNode subjectDataNode=  // Get DataNode at end of TreePath.
                  (DataNode)inTreePath.getLastPathComponent();
                if   // There are no children.
                  (subjectDataNode.getChildCount() <= 0)
                    ;  // Do nothing.
                    //childDataNode= StringObjectEmpty;  // use dummy child place-holder.
                  else  // There are children.
                    childDataNode=   // get first ChildDagNode.
                      subjectDataNode.getChild(0);
                } // get name of first child.
            if (childDataNode != null)  // There is a selectable child.
              setSelectionTreePathV(   // Set selection TreePath to be...
                inTreePath.  // ...subject TreePath with...
                  pathByAddingChild( childDataNode ) // ... child added.
                );
            } // setSubjectTreePathWithAutoSelectV(inTreePath()

        protected TreePath getSubjectTreePath( )
          /* This method returns the TreePath representing the node
            being displayed.
            */
          { 
            return subjectTreePath;  // Return it.
            }

        protected DataNode getSubjectDataNode()
          /* This method returns the DataNode of the subject.  */
          { 
            return subjectDataNode;  // return TreePath of selected item.
            }

      /* Selection code.  The selection is a single child of interest.
        It it one of possibly multiple subject child items being displayed.
        */
        
        private TreePath selectionTreePath; /* TreePath to selected item.  
          The parent of this TreePath is subjectTreePath
          which represents the entire set being displayed.
          This assumes that there is only one sub-level displayed,
          so all items displayed have the same parent.
          This restriction might be removed later if
          multiple levels are displayed simultaneously.
          This variable is null if the subject contains 
          no selectable children.
          */

        private DataNode selectionDataNode; /* DataNode of selected item. ???
          This is the last element of the selectionTreePath.
          This variable is null if the JComponent is displaying 
          a node with no selectable children.
          */

        protected void setSelectionTreePathV( TreePath inTreePath )
          /* This method stores inTreePath as 
            the TreePath of the selected position. 
            But it does not notify any TreeSelectionListener about it.
            That must be done by NotifyTreeSelectionListenersV(.)
            or fireValueChanged(.).

            ??? It might be a good idea to rewrite existing code so that
            selection is always the last thing done,
            and combine this method with NotifyTreeSelectionListenersV(.).
            But I'm not certain this is possible for all callers.
            */
          { 
            setSubjectTreePathV(  // Set appropriate subject TreePath to be...
              inTreePath.getParentPath()  // ...parent of selection TreePath.
              );  // This will set the subject DataNode also.

            selectionTreePath= inTreePath;  // Store selection TreePath.
            selectionDataNode=  // Store last element for easy access.
              (DataNode)selectionTreePath.getLastPathComponent();
            }

        protected TreePath getSelectionTreePath()
          /* This method returns the TreePath representing 
            the selected position.
            */
          { 
            return selectionTreePath;  // return TreePath of selected item.
            }

        protected void setSelectionDataNodeV( DataNode inDataNode )
          /* This method sets a new selected DataNode to inDataNode.
            It updates other variables appropriately.
            It assumes that the subject container is unchanged.
            */
          {
            TreePath childTreePath= // Calculate selected child TreePath to be...
              subjectTreePath.  // ...the base TreePath with...
                pathByAddingChild( inDataNode );  // ... the child added.
            setSelectionTreePathV(  childTreePath );  // select new TreePath.
            }

        protected DataNode getSelectionDataNode()
          /* This method returns the DataNode of the selected position.  */
          { 
            return selectionDataNode;  // return TreePath of selected item.
            }
    
    // TreeSelectionListener code (setting and using).

      /* Though the class TreeSelectionEvent is used by the following methods,
        neither this class or the class being helped by this class
        is a TreeSelectionModel, the usual source of TreeSelectionEvent-s.
        TreeSelectionEvent is being used simply because 
        it is a convenient way to pass a TreePath and a boolean.
        */
      
      private Vector<TreeSelectionListener> listenerVector =   // Listeners.
        new Vector<TreeSelectionListener>();

      public void addTreeSelectionListener( TreeSelectionListener listener ) 
        {
          if ( listener != null && !listenerVector.contains( listener ) )
            listenerVector.addElement( listener );
          }

      public void removeTreeSelectionListener(TreeSelectionListener listener)
        {
          if ( listener != null )
            listenerVector.removeElement( listener );
          }

      public void notifyTreeSelectionListenersV( boolean internalB )
        /* Notifies any TreeSelectionListeners of the JComponent being helped
          that the associated current selection has changed.
          For efficiency it should be called only when
          the selection actually has changed.
          internalB has the following meanings:
            true: the new selection is within the same DataNode 
              as the previous selection, so its display can be handled by 
              the same DagNodeViewer JComponent being helped.
            false: the new selection is NOT within the same DataNode 
              as the previous selection, so it must be handled by 
              a new DagNodeViewer JComponent.
          */
        { // NotifyTreeSelectionListenersV.
          TreePath childTreePath=   // get the selected TreePath.
            getSelectionTreePath();
          TreeSelectionEvent TheTreeSelectionEvent= //construct...
            new TreeSelectionEvent (  // ...a TreeSelectionEvent from...
              owningJComponent,  // ...the source JComponent being helped...
              childTreePath,  // ...with a change of childTreePath...
              internalB,  // ...being added as a selection path...
              childTreePath,  // ...and also being the new lead selection path...
              childTreePath  // ...and the old lead selection path.
              ); 
          fireValueChanged(  // fire value changed with...
            TheTreeSelectionEvent  // ...newly constructed event.
            );
          } // NotifyTreeSelectionListenersV.

      private void fireValueChanged( TreeSelectionEvent e ) 
        {
          Enumeration<TreeSelectionListener> listeners = 
            listenerVector.elements();
          while ( listeners.hasMoreElements() ) 
            {
              TreeSelectionListener listener = 
                (TreeSelectionListener)listeners.nextElement();
              listener.valueChanged( e );
              }
        }

    } // class ViewHelper
