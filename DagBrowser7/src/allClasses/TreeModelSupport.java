package allClasses;

// import java.util.*;
import java.util.Enumeration;
import java.util.Vector;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;

public abstract class TreeModelSupport 
  /* This class acts as a base class for 
     classes which implement the TreeModel interface.
     It provides fields and methods which every TreeModel needs,
     but which can be the same for every TreeModel.
     These are the fields and methods for 
     managing a TreeModelListener list
     and for firing TreeModelEvent-s and
     sending them to the TreeModelListener-s.
     
     Combine this with AbstractTreeModel ???

     */
  {
    private Vector<TreeModelListener> ListenerVector =   // ListenerVector to store listeners.
      new Vector<TreeModelListener>();

    public void addTreeModelListener( TreeModelListener listener ) 
      {
        /* 
        System.out.println( 
          "in TreeModelSupport.addTreeModelListener(...)"
          );
        */
      
        if ( listener != null && !ListenerVector.contains( listener ) )
          ListenerVector.addElement( listener );
        }

    public void removeTreeModelListener( TreeModelListener listener ) 
      {
        if ( listener != null )
          ListenerVector.removeElement( listener );
        }

    public void fireTreeNodesChanged( TreeModelEvent e ) 
      {
        /* 
        System.out.println( 
          "in TreeModelSupport.fireTreeNodesChanged(...)"
          );
        */
          
        Enumeration<TreeModelListener> listeners = ListenerVector.elements();
        while ( listeners.hasMoreElements() ) 
          {
            TreeModelListener listener = (TreeModelListener)listeners.nextElement();
            /* 
            System.out.println( 
                "in TreeModelSupport.fireTreeNodesChanged(...) calling listener.treeNodesChanged( e )"
                );
            */
            listener.treeNodesChanged( e );
            }
      }

    public void fireTreeNodesInserted( TreeModelEvent e ) 
      {
        Enumeration<TreeModelListener> listeners = ListenerVector.elements();
        while ( listeners.hasMoreElements() ) 
          {
            TreeModelListener listener = (TreeModelListener)listeners.nextElement();
            listener.treeNodesInserted( e );
            }
        }

    public void fireTreeNodesRemoved( TreeModelEvent e ) 
      {
        Enumeration<TreeModelListener> listeners = ListenerVector.elements();
        while ( listeners.hasMoreElements() ) 
          {
            TreeModelListener listener = (TreeModelListener)listeners.nextElement();
            listener.treeNodesRemoved( e );
            }
        }

    public void fireTreeStructureChanged( TreeModelEvent e ) 
      {
        Enumeration<TreeModelListener> listeners = ListenerVector.elements();
        while ( listeners.hasMoreElements() ) 
          {
            TreeModelListener listener = (TreeModelListener)listeners.nextElement();
            listener.treeStructureChanged( e );
            }
        }

  }
