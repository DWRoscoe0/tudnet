package allClasses;

import javax.swing.JComponent;
import javax.swing.tree.TreePath;

public 
  //% interface 
  class DataNode {

  /* This //% interface 
    class forms the basis of the classes 
    which represent the DAG (Directed Acyclic Graph). 
    Many of its methods are similar to methods in 
    the DataTreeModel interface.

    ?? Possible new subclasses:

      ?? Maybe create a subclass CachedDataNode.
		    * It maintains an array of all names of all its children,
		      for fast counts and name searches.
		    * It caches the actual child nodes for fast child getChild(..).

    ?? Possible methods to add:

			?? Add getEpiThread() which returns the EpiThread associated with
			  this DataNode, or null if there is none, which is the default.
			  This would standardize and simplify thread starting and stopping.
			   
		  ?? Add JComponent getSummaryJComponent() which returns 
		    a component, such as a Label, which summarizes this DataNode,
		    at least with the name, but possibly also with a summary value,
		    can be used as a part of its parent DataJComponent.
		    See getSummaryString() which returns a String.
	    
    ?? This could be combined with the abstract class AbDataNode.

    ?? Maybe add a field, parentsObject, which contains references to
      DataNodes which parents of this node.
      This would be used by DataTreeModel.translatingToTreePath( .. ).

    */
  
  /* All the DataNode methods ///// implemented here work, 
    but some of them do so by counting and searching.
    They should be used only by subclasses which 
    do not have a lot of children so they will run quickly.
    Otherwise they should be optimized or cached or both.
    
    ?? This could be combined with the interface DataNode.
    */
    
  //% { // AbDataNode

    // Static methods.

		  static DataNode[] emptyListOfDataNodes()
		  	// This method returns an empty DataNode list.
		    { 
		      return new DataNode[]{}; 
		      }

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

      public String toString()
        //Defines a meaningful and useful String representation.
        { 
          return getLineSummaryString( );  // Using the summary string.
          }

    // Other non-static methods.
    
      public int IDCode() { return super.hashCode(); }

  // Methods with equivalents in DataTreeModel, all getters or testers.

    public boolean isLeaf( ) //%; 
    /* Returns true if this is a leaf node,
      which means it can never have children.
      Returns false otherwise.
      */
    //% public boolean isLeaf( ) 
      /* Returns false, because most nodes are not leaves.  */
      { 
        return false; 
        }

    public int getChildCount( ) //%;  
    /* Returns the number of children 
      in this node.  */
    //% public int getChildCount( ) 
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

    public DataNode getChild( int indexI ) //%;  
    /* Returns the child DataNode 
      whose index is indexI if in the range 0 through getChildCount() - 1.
      If IndexI is out of this range then it returns null.
      */
    
    //% public DataNode getChild( int IndexI ) 
      /* This returns null to indicate a default of 0 children.  */
      {
        return null;
        }

    public int getIndexOfChild( Object inChildObject ) //%; 
    /* Returns the index of 
      child childObject.  If childObject is not one of the children 
      of this DataNode then it returns -1.  */
    //% public int getIndexOfChild( Object inChildObject ) 
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

  // Methods which return Strings about the node.

    public String getNameString( )//%;  
    /* Returns the name of the DataNode
      as a String.  This should be an identifying String.
      It will be used as part of a path-name and which
      identifies this DataNode distinctly from its siblings.
      */
    //% public String getNameString( )
      {
    	  return "-NAME-UNDEFINED-";
        }

    public String getValueString( ) //%;  
    /* Returns the value of the DataNode
      as a String.  This is meant to be a very short string that
      might be appended to the name to represent a title. 
      */
    //% public String getValueString( )
      {
    		return "-UNDEFINED-";
    		}

    public String getContentString( ) //% ;  
    /* Returns the content of the DataNode
      as a String.  This is meant to represent 
      potentially large blocks of data, such as the contents of files,
      and often consists of multiple lines.
      */
    //% public String getContentString( )
      {
    		return getValueString( );
    		}

    public String getLineSummaryString( ) //% ;  
    /* Returns a one-line summary of
      the contents of this DataNode as a String, usually the name
      followed maybe by something else.  */
    //% public String getLineSummaryString( )
      /* This method returns a String which is a meaningful summary
        of this DataNode. 
        */
      {
    	  String nameString= getNameString();  // Caching name.
    	  String summaryString= getValueString(); // Initializing summary.
    	  process: {
    	  	if ( summaryString == "-UNDEFINED-" )
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

    public String getInfoString( ) //%;  
    /* Returns additional attributes 
      about this DataNode as a String.  */
    //% public String getInfoString()
      //Returns a String representing information about this object.
      { 
        return getLineSummaryString( );  // Using the summary string.
        }

  // Other methods.

    public JComponent getDataJComponent( 
      TreePath inTreePath, DataTreeModel inDataTreeModel 
      ) //%; 
      /* Returns a JComponent capable of displaying this DataNode
        and using the DataTreeModel InDataTreeModel to provide context.  
        The DataTreeModel might be ignored.
        */
    //% public JComponent getDataJComponent(
	  //%     TreePath inTreePath, DataTreeModel inDataTreeModel 
	  //%   )
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

    public int getIndexOfNamedChild( String inString ) //%;
      /* Returns the index of the child whose name is inString,
        or -1 if this DataNode has no such child. 
        It assumes and uses a fully functional getChild(.) method.  
        */
    //% public int getIndexOfNamedChild( String inString )
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

    public DataNode getNamedChildDataNode( String inNameString ) //%;
      /* Returns the child DataNode whose name is inString.
          If no such child exists then it returns null.
          This method is used for reading from the MetaFile
          and translating a name into a DataNode subclass.
          */
    //% public DataNode getNamedChildDataNode( String inNameString )
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
    
  } // interface DataNode.
