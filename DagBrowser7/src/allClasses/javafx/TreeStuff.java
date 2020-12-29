package allClasses.javafx;

import allClasses.DataNode;
import javafx.scene.Node;

public class TreeStuff 
  /* This class stores information about 
   * a location in the hierarchy for JavaFX Node viewers.
   * It stores both the location of the Node,
   * and the location of the selection, if any, within the Node.
   * It should be updated by the selection model of
   * the Node's associated viewer.
   * It may be interrogated for location information.
   * Location can be expressed by either
   * a TreePath or the DataNode of interest that terminates it. 
   * A TreePath can be calculated from a DataNode
   * by following the links to parent DataNodes.
   */

  {
    private Node theGuiNode= null;
      // This should be the JavaFX Node used to display the DataNode. 
    private DataNode subjectDataNode= null;
      // This is the whole DataNode being displayed.
      // It should be the parent of the selected DataNode, if any.
    private DataNode selectedChildDataNode= null;
      // This should be the selected child DataNode of the subject DataNode.
      // This may be null if there is not selection.
      ///org Maybe bind this to viewer instead of assigning it.


    public void initializeV(Node theNode)
      { 
        this.theGuiNode= theNode;
        }


    public DataNode getSelectedDataNode()
      {
        return selectedChildDataNode;
        }
    
    public void setSelectedDataNodeV(DataNode theDataNode)
      {
        selectedChildDataNode= theDataNode;
        }


    public Node getGuiNode()
      /* Returns the JavaFX GUI Node being used to display the DataNode. */
      {
        return theGuiNode;
        }

    }
