package allClasses;

import java.util.Enumeration;
import java.util.Vector;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;

import static allClasses.AppLog.theAppLog;
import static allClasses.SystemSettings.NL;


public abstract class TreeModelSupport 

  {
    /* This class acts as a base class for implementors of TreeModel.
     * It is based on the SwingX library file TreeModelSupport.java.
     * It provides fields and methods which every TreeModel needs for:
     * * managing a TreeModelListener list and
     * * firing TreeModelEvent-s to call those TreeModelListeners.
     * 
     * The TreeModelListeners process changes to 
     * the content or structure of a tree being displayed.
     * Compare this with TreePathListener,
     * which processes changes to the selection
     * within a tree that is being displayed.
     */


    // Variables.

    private Vector<TreeModelListener> vectorOfTreeModelListener =
      new Vector<TreeModelListener>();   // Listener storage.

    private int maxListenersI= 0; // 7; // Used to trigger logging.


    // Methods that add, remove, and log TreeModelListener.

    public void addTreeModelListener( TreeModelListener theTreeModelListener )
      /* ///fix Note, I suspect I have a Listener leak, 
        because I had to increase the vector size limit several times
        to prevent error messages.
        When I made the limit variable, maxListenersI,
        increment each time it was exceeded,
        and Alogging only when it was increased, I noticed:
        * It always seems to become 5 when the app starts.
        * It increments only when the cursor in the JTree window changes,
          though it doesn't happen all the time.
          It might not if the change is to an adjacent child node.
        */
      {
        // appLogger.debug(
        //  ssssddd"TreeModelSupport.addTreeModelListener(..) begins with "
        //    +theTreeModelListener);
        if ( theTreeModelListener != null && 
            !vectorOfTreeModelListener.contains( theTreeModelListener ) )
          { 
            vectorOfTreeModelListener.addElement( theTreeModelListener );
            if ( vectorOfTreeModelListener.size() > maxListenersI) {
              // theAppLog.debug(
              //   "TreeModelSupport.addTreeModelListener(..), maxListenersI: "+
              //   vectorOfTreeModelListener.size()+ NL + "  "+
              //   theTreeModelListener.getClass().getName() + "@" +
              //   Integer.toHexString(
              //     System.identityHashCode(theTreeModelListener))
              //   );
              maxListenersI++; // Increment maximum listeners so far.
              }
            }
          else
          theAppLog.debug(
            "TreeModelSupport.addTreeModelListener(..) "
            + "rejecting duplicate listener "
            + NL + "  "+theTreeModelListener
            );
        // appLogger.debug(
        //    "TreeModelSupport.addTreeModelListener(..) ends with "
        //    +theTreeModelListener);
        }

    public void removeTreeModelListener(TreeModelListener theTreeModelListener) 
      {
        // appLogger.debug(
        //     "TreeModelSupport.removeTreeModelListener(..) begins with "
        //    +theTreeModelListener);
        if ( theTreeModelListener != null )
          {
            if (vectorOfTreeModelListener.removeElement( theTreeModelListener ))
              //appLogger.info(
              //    "TreeModelSupport.removeTreeModelListener(..), listeners: "+
              //    vectorOfTreeModelListener.size()+ NL + "  "+
              //    theTreeModelListener.getClass().getName() + "@" +
              //    Integer.toHexString(
              //        System.identityHashCode(theTreeModelListener)
              //        )
              //    )
              ;
              else
              theAppLog.info(
                  "TreeModelSupport.removeTreeModelListener(..), "
                  +"not registered:" + NL + "   "+
                  theTreeModelListener.getClass().getName() + "@" +
                  Integer.toHexString(
                      System.identityHashCode(theTreeModelListener)
                      )
                  );
            }
        // appLogger.debug(
        //     "TreeModelSupport.removeTreeModelListener(..) ends with "
        //    +theTreeModelListener);
        }

    public void logListenersV()
      // Logs the number of registered listeners.
      {
        theAppLog.info(
            "TreeModelSupport.logListenersV(), listeners: "+
            vectorOfTreeModelListener.size()
            );
      }


    /* Methods that fire TreeModelEvents.
     * If these methods call listener methods in Swing components
     * then they must be called on the Event Dispatch Thread (EDT). 
     */

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
                "in TreeModelSupport.fireTreeNodesChanged(...) "
                +"calling listener.treeNodesChanged( theTreeModelEvent )"
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
            //appLogger.info(
            //    "TreeModelSupport.fireTreeNodesInserted(..) to:" + NL 
            //    + theTreeModelListener
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
