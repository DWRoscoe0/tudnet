package allClasses;

import javax.swing.JComponent;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

public abstract class AbDataNode 

  implements DataNode
  
  /* This class started as an abstract class.
    It implemented only part of the DataNode interface.
    Over time I was provided implementations for
    all of the interface methods, and then some.
    So now this class is now abstract in name only,
    and I could probably remove the abstract keyword. ???

    The DataNode methods will work, but some of them
    do so by counting and searching.
    They should be used only by subclasses which 
    do not have a lot of children and can be evaluated quickly.
    Otherwise they should be optimized or cached or both.
    */
    
  {
    // Static methods.

      static boolean isUsableB( DataNode inDataNode )
        /* This method returns true if inDataNode is usable,
          which means it is not null and not an UnknownDataNode.
          */
        { 
          boolean usableB= true;  // Assume the node is usable.

          toReturn: { // block beginning.
          toUnusable: { // block beginning.
          
            if ( inDataNode == null )  // There is no DataNode reference.
              break toUnusable; // Go return value for unusable.
            if // Node class is not an UnknownDataNode.
              ( ! UnknownDataNode.isOneB( inDataNode ) )
              // ( ! ( inDataNode instanceof UnknownDataNode ) )
              break toReturn;  // Go return initial default value of usable.

          } // toUnusable
            usableB= false;  // Override default useability value.

          } // toReturn
            return usableB;  // Return final calculated value.

          }
      
    // DataNode interface methods.
  
      public boolean isLeaf( ) 
        /* Returns false, because most nodes are not leaves.  */
        { 
          return false; 
          }
  
      public DataNode getChild( int IndexI ) 
        /* This returns null to indicate a default of 0 children.  */
        {
          return null;
          }

      public int getChildCount( ) 
        /* This method actually scans all the children that are
          visible to the method getChild(..) and counts them.  
          It assumes a functional getChild( IndexI ) method which
          returns null if IndexI is out of range.  */
        { // getChildCount( )
          int childIndexI= 0;  // Initialize child index.

          while  // Process all children...
            ( getChild( childIndexI ) != null )  // ...returned by getChild(.).
            { // process this child.
              childIndexI++;  // increment index.
              } // process this child.

          return childIndexI;  // Return ending index as count.
          } // getChildCount( )

      public int getIndexOfChild( Object inChildObject ) 
        /* Returns the index of the child inchildObject,
          or -1 if it is not one of this node's children. 
          It assumes a functional getChild(.) method.  
          */
        { // getIndexOfChild(.)
          int childIndexI= 0;  // Initialize child search index.

          while ( true ) // Search for child.
            { // Check one child.
              Object childObject=  // get the child.
                 getChild( childIndexI );

              if ( childObject == null )  // null means no more children.
                { // Exit with failure.
                  childIndexI= -1;  // Set index to indicate failure.
                  break;  // Exit while loop.
                  } // Exit with failure.

              if ( inChildObject.equals( childObject ) )  // Found child.
                break;  // Exit while loop.

              childIndexI++;  // Increment index to check next child.
              } // Check one child.

          return childIndexI;  // Return index as search result.
          } // getIndexOfChild(.)

      public DataNode getNamedChildDataNode( String inNameString )
        /* Returns the child DataNode whose name is inNameString.
          If no such child exists, then it returns null.
          */
        { 
          int childIndexI= // Translate name to index.
            getIndexOfNamedChild( inNameString );

          DataNode childDataNode= // Translate index to DataNode.
            getChild( childIndexI );

          return childDataNode;  // Return DataNode.
          }

      public int getIndexOfNamedChild( String inString )
        /* Returns the index of the child whose name is inString,
          or -1 if this node's has no such child or inString is null.
          It does this by doing a search of the node's children.
          It assumes a functional getChild(..) method.  
          */
        {
          int childIndexI;  // Storage for search index and return result.

          if ( inString == null )  // Handling null child name.
            childIndexI= -1;  // Indicating no matching child.
            else  // Handling non-null child name.
            for ( childIndexI=0; ; childIndexI++ ) // Searching for child.
              { // Checking one child.
                DataNode childDataNode=  // Getting the child.
                   getChild( childIndexI );
                if ( childDataNode == null )  // Handling end of children.
                  { // Exiting with search failure.
                    childIndexI= -1;  // Setting index to indicate failure.
                    break;  // Exiting loop.
                    }
                if  // Handling child with matching name.
                  ( inString.equals( childDataNode.GetNameString( ) ) )
                  break;  // Exiting while loop.
                }

          return childIndexI;  // Return index as search result.
          }

      public String GetInfoString()
        /* Returns a String representing information about this object. */
        { 
          return GetNameString( );  // Use the name string.
          }

      public String GetHeadString()
        /* Returns a String representing this node excluding any children. */
        { 
          return GetNameString( );  // Use the name string.
          }

      public String GetNameString( )
        /* Returns String representing name of this Object.  */
        {
          return toString();  // Return default String representation.
          }
    
      public JComponent GetDataJComponent
        ( TreePath inTreePath, TreeModel inTreeModel )
        /* Returns a JComponent which is appropriate for viewing
          and possibly changing its associated DataNode, 
          using context from inTreeModel.
          It returns a TextViewer for leaves and 
          a ListViewer for non-leaves.
          The DataNode is defined by inTreePath,
          */
        { // GetDataJComponent()
          JComponent resultJComponent= null;  // For result.

          if ( isLeaf( ) )  // This DataNode is a leaf.
            resultJComponent= // Set result to be a TextViewer JComponent.
              new TextViewer( 
                inTreePath, 
                inTreeModel, 
                "Leaf Object: "+GetHeadString() 
                );
            else  // This DataNode is NOT a leaf.
            resultJComponent= // Set result for exploring a List.
              new ListViewer( inTreePath, inTreeModel );

          return resultJComponent;  // Return the result from above.
          } // GetDataJComponent()

    // Other non-static methods.
    
      public int IDCode() { return super.hashCode(); }

    }
