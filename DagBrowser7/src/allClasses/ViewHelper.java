package allClasses;

import java.util.Enumeration;
import java.util.Vector;

import javax.swing.JComponent;
import javax.swing.tree.TreePath;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;

public class ViewHelper

  /* This class is being used to move code which is now in 
    base class DagNodeViewer into what will be the derived classes.
    */

  { // class ViewHelper

    private JComponent OwningJComponent;  /* JComponent being helped.
      This is needed mainly as source of TreeSelectionEvent-s.  */
    
    /* Location/Selection-related variables.
      These should be redesigned to support both
      the TreePath of the entire component,
      and the TreePath of the selected item within that component.
      */
      
      private TreePath SelectedChildTreePath; /* TreePath of the selected item.
        It it one of possibly multiple items being displayed by 
        the JComponent being helped.
        The parent of this TreePath represents the entire set being displayed.
        This assumes that there is only one sublevel,
        so all items have the same parent.
        This restriction might be removed later.
        */

    // constructor.
    
      ViewHelper( JComponent InOwningJComponent )
        {
          OwningJComponent= InOwningJComponent;
          }
    
    // TreeSelectionListener variables and methods.  

      /* Though the class TreeSelectionEvent is used by the following methods,
        neither this class or the class being helped by this class
        is a TreeSelectionModel.
        TreeSelectionEvent is being used simply because 
        it is a convenient way to pass a TreePath and a boolean.
        */
      
      private Vector<TreeSelectionListener> ListenerVector =   // Listeners.
        new Vector<TreeSelectionListener>();

      public void addTreeSelectionListener( TreeSelectionListener listener ) 
        {
          if ( listener != null && !ListenerVector.contains( listener ) )
            ListenerVector.addElement( listener );
          }

      public void removeTreeSelectionListener( TreeSelectionListener listener ) 
        {
          if ( listener != null )
            ListenerVector.removeElement( listener );
          }

      public void NotifyTreeSelectionListenersV( boolean InternalB )
        /* Notifies any TreeSelectionListeners of the JComponent being helped
          that the current selection has changed.
          For efficienty it should be called only when
          the selection actually has changed.
          InternalB has the following meanings:
            true: the selection is within the same DataNode as the previous 
              selection, so its display can be handled by 
              the same DagNodeViewer JComponent being helped.
            false: the selection within the same DataNode as the previous 
              selection, so it must be handled by a new DagNodeViewer JComponent.
          */
        { // NotifyTreeSelectionListenersV.
          TreePath ChildTreePath=   // get the selected TreePath.
            GetSelectedChildTreePath();
          TreeSelectionEvent TheTreeSelectionEvent= //construct...
            new TreeSelectionEvent (  // ...a TreeSelectionEvent from...
              OwningJComponent,  // ...the JComponent being helped...
              ChildTreePath,  // ...with a change of ChildTreePath...
              InternalB,  // ...being added as a selection path...
              ChildTreePath,  // ...and also being the new lead selection path...
              ChildTreePath  // ...and the old lead selection path.
              ); 
          fireValueChanged(  // fire value changed with...
            TheTreeSelectionEvent  // ...newly constructed event.
            );
          } // NotifyTreeSelectionListenersV.

      private void fireValueChanged( TreeSelectionEvent e ) 
        {
          Enumeration<TreeSelectionListener> listeners = 
            ListenerVector.elements();
          while ( listeners.hasMoreElements() ) 
            {
              TreeSelectionListener listener = 
                (TreeSelectionListener)listeners.nextElement();
              listener.valueChanged( e );
              }
        }

    // input methods.
          
      protected void SetSelectedChildTreePath(TreePath InSelectedChildTreePath)
        /* This method stores the TreePath representing the selected position. 
          But it does not notify any TreeSelectionListener about it.
          That must be done by NotifyTreeSelectionListenersV(.)
          or fireValueChanged(.).
          
          It might be a good idea to rewrite existing code so that
          selection is always the last thing done,
          and combine this method with NotifyTreeSelectionListenersV(.).
          But I'm not certain that is possible for all calls.
          */
        { 
          SelectedChildTreePath= InSelectedChildTreePath;  // store value.
          }

    // output methods.
          
      protected TreePath GetSelectedChildTreePath()
        // This method returns the TreePath representing the selected position.
        { 
          return SelectedChildTreePath;  // return TreePath of selected item.
          }

    } // class ViewHelper
