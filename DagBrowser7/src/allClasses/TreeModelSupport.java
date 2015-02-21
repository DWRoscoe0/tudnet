package allClasses;

import java.util.Enumeration;
import java.util.Vector;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;

import static allClasses.Globals.*;  // appLogger;

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
    private Vector<TreeModelListener> vectorOfTreeModelListener =
      new Vector<TreeModelListener>();   // Listener storage.

    public void addTreeModelListener( TreeModelListener theTreeModelListener )
      /* The number of listeners shouldn't go above 4.
       * If it goes any higher then a debug message is logged.
       */
      {
        if ( 
        		  theTreeModelListener != null && 
        		  !vectorOfTreeModelListener.contains( theTreeModelListener ) 
        		  )
          vectorOfTreeModelListener.addElement( theTreeModelListener );

        if ( vectorOfTreeModelListener.size() > 4)
	    		appLogger.debug(
	      			"TreeModelSupport.addTreeModelListener(..),\n  vector now has "+
	      		    vectorOfTreeModelListener.size()+
	      		    " listeners."
	            );
        }

    public void removeTreeModelListener(TreeModelListener theTreeModelListener) 
      {
        if ( theTreeModelListener != null )
          vectorOfTreeModelListener.removeElement( theTreeModelListener );

        //appLogger.debug(
        //		"TreeModelSupport.removeTreeModelListener(..),\n  vector now has "+
        //	    vectorOfTreeModelListener.size()+
        //	    " listeners."
        //    );
        }

    public void fireTreeNodesChanged( TreeModelEvent theTreeModelEvent ) 
      {
        /* 
        System.out.println( 
          "in TreeModelSupport.fireTreeNodesChanged(...)"
          );
        */
          
        Enumeration<TreeModelListener> listeners = 
        	vectorOfTreeModelListener.elements();
        while ( listeners.hasMoreElements() ) 
          {
            TreeModelListener listener = 
            	(TreeModelListener)listeners.nextElement();
            /* 
            System.out.println( 
                "in TreeModelSupport.fireTreeNodesChanged(...) calling listener.treeNodesChanged( theTreeModelEvent )"
                );
            */
            listener.treeNodesChanged( theTreeModelEvent );
            }
      }

    public void fireTreeNodesInserted( TreeModelEvent theTreeModelEvent ) 
      {
        Enumeration<TreeModelListener> listeners = 
        	vectorOfTreeModelListener.elements();
        while ( listeners.hasMoreElements() ) 
          {
            TreeModelListener theTreeModelListener = 
            	(TreeModelListener)listeners.nextElement();
            theTreeModelListener.treeNodesInserted( theTreeModelEvent );
            }
        }

    public void fireTreeNodesRemoved( TreeModelEvent theTreeModelEvent ) 
      {
        Enumeration<TreeModelListener> listeners = 
        	vectorOfTreeModelListener.elements();
        while ( listeners.hasMoreElements() ) 
          {
            TreeModelListener theTreeModelListener = 
            	(TreeModelListener)listeners.nextElement();
            theTreeModelListener.treeNodesRemoved( theTreeModelEvent );
            }
        }

    public void fireTreeStructureChanged( TreeModelEvent theTreeModelEvent ) 
      {
        Enumeration<TreeModelListener> listeners = 
        	vectorOfTreeModelListener.elements();
        while ( listeners.hasMoreElements() ) 
          {
            TreeModelListener theTreeModelListener = 
            	(TreeModelListener)listeners.nextElement();
            theTreeModelListener.treeStructureChanged( theTreeModelEvent );
            }
        }

  }
