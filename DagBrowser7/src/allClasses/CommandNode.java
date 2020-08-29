package allClasses;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import javax.swing.JComponent;
import javax.swing.tree.TreePath;
import static allClasses.AppLog.theAppLog;

public class CommandNode

  extends NamedDataNode

  {
    /* This class is meant to be a leaf node, with a name,
     * and a particular viewer for the execution of 
     * a particular command defined by type CV. 
     */

    // Locally stored injected dependency.
    private String viewerClassString;
  
    public CommandNode( // constructor
        String nameString, // Node name.
        String viewerClassString // Name of viewer class for this node.
        )
      {
        super.initializeV(nameString);
        
        this.viewerClassString= viewerClassString; 
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
          JComponent theJComponent= null;
          theJComponent= ////// 
              new InstallerBuilder(inTreePath, inDataTreeModel);
        toProcessResults: {
          try { 
                String getNameString= getClass().getName();
                theAppLog.debug("CommandNode.getDataJComponent(.) class:"
                    +getNameString);
                //// theClass= Class.forName(getNameString);
                theClass= Class.forName(viewerClassString);
                //// Class t = Class.forName("java.lang.Thread"); 
                //// theClass= Class.forName("java.lang.String");
            } catch (ClassNotFoundException e) {
              theException= e; break toProcessResults;
            }
          try {
              theConstructor= theClass.getConstructor(
                  TreePath.class, DataTreeModel.class);
            } catch (NoSuchMethodException | SecurityException e1) {
              theException= e1; break toProcessResults;
            }
          try {
              componentObject= 
                  theConstructor.newInstance(inTreePath, inDataTreeModel);
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
              //// Using this results in NullPointerException.
              //// InstallerBuilder needs customization as planned.
            theJComponent= new TitledTextViewer( 
              inTreePath, inDataTreeModel,
                "CommandNode.getDataJComponent(.): test Component");
            }
         
        return theJComponent;
        }

    } 
