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
     
     Combine this with AbstractTreeModel ??

     */
  {
    private Vector<TreeModelListener> vectorOfTreeModelListener =
      new Vector<TreeModelListener>();   // Listener storage.

    public void addTreeModelListener( TreeModelListener theTreeModelListener )
      /* //// Note, I have had to increase the vector size limit several times.
        I suspect I have a Listener leak, and some type of command mode
        is causing it, but I haven't figured out what that is yet.
        */
      {
        if ( 
        		  theTreeModelListener != null && 
        		  !vectorOfTreeModelListener.contains( theTreeModelListener ) 
        		  )
	        { 
	        	vectorOfTreeModelListener.addElement( theTreeModelListener );
		        if ( vectorOfTreeModelListener.size() > 12)
			    		appLogger.error(
			      			"TreeModelSupport.addTreeModelListener(..), listeners: "+
		      		    vectorOfTreeModelListener.size()+ "\n  "+
		      		    theTreeModelListener.getClass().getName() + "@" +
		      		    Integer.toHexString(
		      		    		System.identityHashCode(theTreeModelListener)
		      		    		)
			            );
  	        }
        	else
	    		appLogger.error(
      			"TreeModelSupport.addTreeModelListener(..) rejecting duplicate listener "+
      		    "\n  "+theTreeModelListener
            );
        }

    public void removeTreeModelListener(TreeModelListener theTreeModelListener) 
      {
        if ( theTreeModelListener != null )
	        {
	          if (vectorOfTreeModelListener.removeElement( theTreeModelListener ))
		  	      //appLogger.debug(
	          	//		"TreeModelSupport.removeTreeModelListener(..), listeners: "+
	          	//    vectorOfTreeModelListener.size()+ "\n  "+
	          	//    theTreeModelListener.getClass().getName() + "@" +
	          	//    Integer.toHexString(
	          	//    		System.identityHashCode(theTreeModelListener)
	          	//    		)
	          	//    )
	          	;
  	          else
		  	      appLogger.debug(
		  	      		"TreeModelSupport.removeTreeModelListener(..), not registered:\n   "+
		      		    theTreeModelListener.getClass().getName() + "@" +
		      		    Integer.toHexString(
		      		    		System.identityHashCode(theTreeModelListener)
		      		    		)
			            );
	          }
        }

    public void logListenersV()
      // Logs the number of registered listeners.
    	{
	      appLogger.info(
	      		"TreeModelSupport.logListenersV(), listeners: "+
	  		    vectorOfTreeModelListener.size()
	  		    );
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
  	    		//appLogger.debug(
            //		"TreeModelSupport.fireTreeNodesInserted(..) to:\n"+theTreeModelListener
            //    );
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
