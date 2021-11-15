package allClasses;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import javax.swing.JComponent;
import javax.swing.tree.TreePath;
import static allClasses.AppLog.theAppLog;

public class CommandNode

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
    private String viewerClassString;
    private Persistent thePersistent;
  
    public CommandNode( // constructor
        String nameString, // Node name.
        String viewerClassString, // Name of viewer class for this node.
        Persistent thePersistent
        )
      {
        super.setNameV(nameString);
        
        this.viewerClassString= viewerClassString; 
        this.thePersistent= thePersistent;
        }
    
    public String getSummaryString()
      {
        return "";
        }

    public JComponent getDataJComponent( 
        TreePath inTreePath, DataTreeModel inDataTreeModel 
        ) 
      /* Returns a JComponent of type whose name is viewerClassString
       * which should be a viewer capable of displaying 
       * this DataNode and executing the command associated with it.
       * The DataNode to be viewed should be 
       * the last element of inTreePath,
       */
      {
          Object componentObject= null;
          Class<?> theClass= null;
          Constructor<?> theConstructor= null;
          Exception theException= null;
          JComponent theJComponent;
          
        toProcessResults: {
          try { 
                String getNameString= getClass().getName();
                theAppLog.debug("CommandNode.getDataJComponent(.) class:"
                    +getNameString);
                theClass= Class.forName(viewerClassString);
            } catch (ClassNotFoundException e) {
              theException= e; break toProcessResults;
            }
          try {
              theConstructor= theClass.getConstructor(
                  TreePath.class, DataTreeModel.class, Persistent.class);
            } catch (NoSuchMethodException | SecurityException e1) {
              theException= e1; break toProcessResults;
            }
          try {
              componentObject= theConstructor.newInstance(
                  inTreePath, inDataTreeModel, thePersistent);
            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e) {
              theException= e; break toProcessResults;
            }
        } // toProcessResults:
          if (null != theException) { // Deal with any exception.
              theAppLog.exception("CommandNode.getDataJComponent(.)", 
                  theException);
              theJComponent= 
                new TitledTextViewer( inTreePath, inDataTreeModel,
                  "CommandNode.getDataJComponent(.): "
                      +theException.toString());
              }
            else {
              theJComponent= (JComponent)componentObject;
              }
           
          return theJComponent;
        }

    } 
