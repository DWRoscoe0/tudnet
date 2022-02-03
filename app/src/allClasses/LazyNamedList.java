package allClasses;

import java.util.List;

public class LazyNamedList 

  extends NamedList

  {
  
    /* This class adds to its superclass the ability 
     * to do lazy evaluation of its children.
     */


    // Variables.

    private boolean childrenEvaluatedB= false; /* If this is true then 
      it means that all child NamedLeafs have been evaluated 
      and converted to either an IFile or an IDirectory.  */


    // Methods.

    public List<DataNode> getChildrenAsListOfDataNodes()
      /* This method returns the list of evaluated children.
       * It does evaluation of the entire list if it hasn't been done before.
       * It evaluates every child by getting each one.
       * 
       * This method is called by:
       * * DataNode.getChildObservableListOfDataNodes()
       * * ObservableList<TreeItem<DataNode>> EpiTreeItem.getChildren()
       *   
       * This method was added for use with JavaFX.
       */
    {
      if (! childrenEvaluatedB) // Evaluate entire child list if not done yet.
        { // Evaluate entire child list 
          for // Evaluate each child.
            (
              int childIndexI= 0; 
              childIndexI < getChildCount();
              childIndexI++
              )
            getChild(childIndexI); // Get the child to evaluate it.
          childrenEvaluatedB= true; // Mark entire list evaluated.
          }
      return childMultiLinkOfDataNodes.getListOfEs(); // Return the list.
      }

    public DataNode getChild( int childI ) 
      /* This method tries to return the evaluated child 
       * that is associated with index childI.
       * If childI is out of range then this method returns null.
       * If childI is in range then this method
       * examines the associated child in the child lazy evaluation cache.
       * If the child has not been evaluated yet then
       * it is evaluated and the result stored back into the cache.
       * Finally a reference to the evaluated child is returned.
       */
      { 
        DataNode resultChildDataNode;

        goReturn: {
          if  // Exit with null if child index is out of bounds.
            ( childI < 0 || childI >= childMultiLinkOfDataNodes.getCountI())
            { resultChildDataNode= null; break goReturn; } // exit with null.
          DataNode cachedChildDataNode= // Retrieve child from cache. 
              childMultiLinkOfDataNodes.getE(childI);
          resultChildDataNode= evaluateDataNode(cachedChildDataNode);
          if // Exit with evaluated child if it is the same as cached child.
            (cachedChildDataNode == resultChildDataNode) // 
            break goReturn;
          { // Replace the place-holder child with its evaluation.
            resultChildDataNode.propagateTreeModelIntoSubtreeV( 
                theDataTreeModel );
            resultChildDataNode.setTreeParentToV( // Set child's parent link
                this ); // to reference this DataNode.
            childMultiLinkOfDataNodes.setE( // Replace in cache the old child
                childI, // which at the specified index
                resultChildDataNode // with the new evaluated child.
                );
            }
          } // goReturn:

        return resultChildDataNode;
        }

    public DataNode evaluateDataNode(DataNode childDataNode)
      /* This method returns childDataNode evaluated 
       * in the context of this DataNode, which [is or] will be its parent.
       * If childDataNode has already been evaluated
       * then it returns childDataNode.
       * 
       * This method does not read any children from the cache.
       * This method does not write a new evaluated child to the cache.
       * This method does not link a new child to its parent-to-be or
       * unlink the old child from its present parent.
       * This method does not propagate LogLevel or TreeLevel from the parent.
       * These things must be done by the caller.
       * 
       * This version of the method returns childDataNode.
       */
      {
        return childDataNode;
        }

    }
