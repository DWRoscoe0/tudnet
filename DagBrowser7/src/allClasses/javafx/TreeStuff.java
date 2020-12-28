package allClasses.javafx;

import allClasses.DataNode;
import javafx.scene.Node;

public class TreeStuff 
  /* This class manages movements in the hierarchy
   * for JavaFX Node viewers.
   * It stores both the location of the Node,
   * and the location of the selection, if any, within the Node.
   * It should be updated by the selection model of
   * the Node's associated viewer.
   * It may be interrogated for location other objects.
   * Location can be expressed by either
   * a TreePath or the path's terminal DataNode. 
   * A TreePath can be calculated from a DataNode
   * by following the links to parent DataNodes.
   */

  {
    /// private DataNode theDataNode= null;
      // This should always be the parent of selected child DataNode.
    public DataNode selectedDataNode= null;
      // This should be the selected child of the parent.
      ///org Maybe bind this to viewer instead of assigning it.
    private Node theNode= null;
      // This should be the referenced value of theNode
      // Associated JavaFX Node. 

    public void setNode(Node theNode)
      { 
        this.theNode= theNode;
        }

    public Node getNode()
      {
        return theNode;
        }

    }
