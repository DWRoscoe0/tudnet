package allClasses;

import javax.swing.JComponent;
import javax.swing.tree.TreePath;

public abstract class AbDataNode 

  implements DataNode
  
  /* All the DataNode methods implemented here work, 
    but some of them do so by counting and searching.
    They should be used only by subclasses which 
    do not have a lot of children and can be evaluated quickly.
    Otherwise they should be optimized or cached or both.
    */
    
  { // AbDataNode

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
              break toReturn;  // Go return initial default value of usable.

          } // toUnusable
            usableB= false;  // Override default usability value.

          } // toReturn
            return usableB;  // Return final calculated value.

          }
      
    // DataNode interface instance methods.
  
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
                  ( inString.equals( childDataNode.getNameString( ) ) )
                  break;  // Exiting while loop.
                }

          return childIndexI;  // Return index as search result.
          }

      public String getNameString( )
        {
      	  return "NAME-UNDEFINED";
          }

      public String getValueString( )
	      {
      		return "VALUE-UNDEFINED";
      		}

      public String getContentString( )
	      {
      		return "CONTENT-UNDEFINED";
      		}

      public String getLineSummaryString( )
        /* This method returns a String which is a meaningful summary
          of this DataNode. 
          */
	      {
      	  String nameString= getNameString();  // Caching name.
      	  String summaryString= getValueString(); // Initializing summary.
      	  process: {
      	  	if ( summaryString == "VALUE-UNDEFINED" )
      	  	  { summaryString= ""; break process; }
	    	    int indexOfNewLineI= summaryString.indexOf("\n");
	      	  if // Trimming extra lines if there are any in value string.
	      	    ( indexOfNewLineI >= 0 )
	      	  	summaryString= // Replacing value string with only its first line. 
	      	  	  summaryString.substring(0,indexOfNewLineI);
	      	  }
      	  if // Combining with name if value not nil or same as name.
      	  	( ( summaryString != "" ) && 
      	  		( ! nameString.equals(summaryString) ) 
      	  		)
	      	  summaryString= // Using
	      	  		nameString // the name,
		        		+ " : " // a separator,
		        		+summaryString // and the trimmed value.
		        		;
      	  	else
  	      	  summaryString= getNameString(); // Using only the name.
      	  return summaryString; 
	        }

      public String getInfoString()
        //Returns a String representing information about this object.
        { 
          return getLineSummaryString( );  // Using the summary string.
          }

      public String toString()
        //Defines a meaningful and useful String representation.
        { 
          return getLineSummaryString( );  // Using the summary string.
          }

      public JComponent getDataJComponent(
          TreePath inTreePath, DataTreeModel inDataTreeModel 
          )
        /* Returns a JComponent which is appropriate for viewing
          its associated DataNode, using context from inDataTreeModel.
          It returns a TextViewer for leaves and 
          a ListViewer for non-leaves.
          The DataNode should be the last element of inTreePath,
          */
        {
          JComponent resultJComponent= null;

          if ( isLeaf( ) ) // Using TitledTextViewer if node is leaf.
            resultJComponent= // Using TitledTextViewer.
              new TitledTextViewer( 
                inTreePath, 
                inDataTreeModel, 
                getContentString()
                );
            else  // Using TitledListViewer if not a leaf.
            resultJComponent= // Using TitledListViewer.
              new TitledListViewer( inTreePath, inDataTreeModel );

          return resultJComponent;  // Returning result from above.
          }

    // Other non-static methods.
    
      public int IDCode() { return super.hashCode(); }

    } // AbDataNode
