package allClasses;

import static allClasses.Globals.appLogger;
import allClasses.AppLog.LogLevel;

import java.awt.Color;

import javax.swing.JComponent;
import javax.swing.tree.TreePath;

public class DataNode 
  {
	
	  /* This class forms the basis of the classes 
	    which represent the DAG (Directed Acyclic Graph). 
	    Many of its methods are similar to methods in 
	    the DataTreeModel interface.
	  
	    All these methods work, but some of them do so by counting and searching.
	    They should be used only by subclasses which 
	    do not have a lot of children so they will run quickly.
	    For nodes with many children,
	    this method should be optimized or cached or both.
	   
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
	
	    ?? Maybe add a field, parentsObject, which contains references to
	      DataNodes which parents of this node.
	      This would be used by DataTreeModel.translatingToTreePath( .. ).
	
	    */
	
	  // Instance variables
	
			protected NamedList parentNamedList= null; // My parent node. 
			protected LogLevel theMaxLogLevel= AppLog.defaultMaxLogLevel;
			
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
      
    // Instance methods.

  	  protected void propagateIntoSubtreeV( DataTreeModel theDataTreeModel )
  	    /* This method is called when a DataNode is added to a NamedList
  	      or one of its subclasses.  This method ignores theDataTreeModel, 
  	      because this is a leaf node and leaves it don't need it.
  	      So the propigation ends here.
	        List nodes that do need it will override this method.
	        See NamedList.
  	      */
  		  {
  		  	}

  	  protected void setParentToV( NamedList parentNamedList )
  	    /* This method is called when a DataNode is added to a NamedList
  	      or one of its subclasses.  This method:
  	      * Stores parentNamedList, because every node has a parent
  	        and needs access to it.
  	      */
  		  {
  	  	  this.parentNamedList= parentNamedList;
  		  	}

      protected void reportChangeOfSelfV()
        /* This method reports a change of this node.
          It does this by calling a parent method,
          because it is more convenient for a parent to
          notify the TreeModel about changes in its children
          that for the children to do it.
         */
      	{
      	  if ( parentNamedList == null )
      	  	{
	      	  	appLogger.debug(
	      	  	"reportChangeOfSelfV(): parentNamedList == null!");
      	  	  /// Eventually replace variable or link it to DAG.
	      	  	}
      	  	else
      			parentNamedList.reportChangeInChildV( this );
      		}

      // Customized logging methods.

  	  protected void propagateIntoSubtreeV( LogLevel theMaxLogLevel )
  	    /* This and its subclass overrides are used to propagate
  	      maximum LogLevel values into subtrees for 
  	      customized DataNode logging.
  	      */
  		  {
  	  	  this.theMaxLogLevel= theMaxLogLevel;
  		  	}

  	  protected boolean logB( LogLevel theLogLevel )
  	  	{
  	  		return ( theLogLevel.compareTo( theMaxLogLevel ) <= 0 ) ;
  	  		}

  	  protected boolean logB( LogLevel theLogLevel, String theLogString )
  	  	{
  		  	return logB( theLogLevel, theLogString, null, false );
  	  		}

  	  protected void logV( LogLevel theLogLevel, String theLogString )
  	  	{
  		  	logV( theLogLevel, theLogString, null, false );
  	  		}

  	  protected boolean logB( 
  	  		LogLevel theLogLevel, 
  	  		String theLogString, 
  	  		Throwable theThrowable, 
  	  		boolean consoleB )
  	    /* This method logs if theLogLevel is 
  	      lower than or equal to maxLogLevel.
  	      It returns true if it logged, false otherwise.
  	     	*/
  	    {
  	  		boolean loggingB= logB(theLogLevel);
  	  		if ( loggingB )
  		      appLogger.logV( 
  		      		theLogLevel, theLogString, theThrowable, consoleB );
  	  		return loggingB;
  	  	  }

  	  protected void logV( 
  	  		LogLevel theLogLevel, 
  	  		String theLogString, 
  	  		Throwable theThrowable, 
  	  		boolean consoleB )
  	    /* This method logs unconditionally. */
  	    {
		      appLogger.logV( 
		      		theLogLevel, theLogString, theThrowable, consoleB );
  	  	  }

  	  
  	  // Other instance methods.
  	  
      public String toString()
        // Returns a meaningful and useful String representation.
        { 
          return getLineSummaryString( );  // Using the summary string.
          }
    
      public int IDCode() { return super.hashCode(); }
      
    // Instance getter and tester methods with equivalents in DataTreeModel.

	    public boolean isLeaf( )
		    /* This method and any of its overridden version
		      returns true if this is a leaf node,
		      which means it can never have children.
		      It returns false otherwise.
		      The method of this class always returns false as the default value.
		      */
	      { 
	        return false; 
	        }
	
	    public int getChildCount( )
		    /* This method returns the number of children in this node.
	        The method of this class actually scans all the children that are
	        visible to the method getChild(..) and counts them.  
	        It assumes a functional getChild( IndexI ) method which
	        returns null if IndexI is out of range.  
	        */
	      { // getChildCount( )
	        int childIndexI= 0;  // Initialize child index.
	
	        while  // Process all children...
	          ( getChild( childIndexI ) != null )  // ...returned by getChild(.).
	          { // process this child.
	            childIndexI++;  // increment index.
	            } // process this child.
	
	        return childIndexI;  // Return ending index as count.
	        } // getChildCount( )
	
	    public DataNode getChild( int indexI )
		    /* This method returns the child DataNode 
		      whose index is indexI if in the range 0 through getChildCount() - 1.
		      If IndexI is out in this range then it returns null.
	        The method of this class returns null as the default value.  
	        */
	      {
	        return null;
	        }
	
	    public int getIndexOfChild( Object inChildObject )
		    /* Returns the index of child childObject.  
		      If childObject is not one of this node's children then it returns -1.
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

    public String getNameString( )
	    /* Returns the name of the DataNode as a String.  
	      his should be a unique (identifying) String.
	      It will be used as part of a path-name and which
	      identifies this DataNode distinctly from its siblings.
	      */
      {
    	  return "-NAME-UNDEFINED-";
        }

    public String getValueString( )
	    /* Returns the value of the D+ataNode as a String.  
	      This is meant to be a very short string that
	      might be appended to the name to represent a title. 
	      */
      {
    		return "-UNDEFINED-";
    		}

    public String getContentString( )
	    /* Returns the content of the DataNode as a String.  
	      This is meant to represent potentially large blocks of data, 
	      such as the contents of files, 
	      and it might consist of multiple lines.
	      */
      {
    		return getValueString( );
    		}

    public String getLineSummaryString( )  
    /* Returns a one-line summary of
      the contents of this DataNode as a String, 
      usually the name followed maybe by something else.  
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

    public String getInfoString( )
    	// Returns additional attributes about this DataNode as a String.
      { 
        return getLineSummaryString( );  // Using the summary string.
        }

  // Other methods.

    public JComponent getDataJComponent( 
	      TreePath inTreePath, DataTreeModel inDataTreeModel 
	      ) 
      /* Returns a JComponent capable of displaying this DataNode
        and using the DataTreeModel InDataTreeModel to provide context.  
        The DataTreeModel might be ignored.
        This default method returns a TextViewer for leaves and 
        a ListViewer for non-leaves.
        The DataNode to be viewed is the last element of inTreePath,
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

    public int getIndexOfNamedChild( String inString )
      /* Returns the index of the child whose name is inString,
        or -1 if this DataNode has no such child. 
        This method works by doing a search of the node's children.
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

    public DataNode getNamedChildDataNode( String inNameString )
      /* Returns the child DataNode whose name is inString.
          If no such child exists then it returns null.
          This method is used for reading from the MetaFile
          and translating a name into a DataNode subclass.
          */
      { 
        int childIndexI= // Translate name to index.
          getIndexOfNamedChild( inNameString );

        DataNode childDataNode= // Translate index to DataNode.
          getChild( childIndexI );

        return childDataNode;  // Return DataNode.
        }

    Color getBackgroundColor( Color defaultBackgroundColor )
      /* This method returns the background color 
        which should be used to display this DataNode.
        The default is input parameter defaultBackgroundColor,
        but this method may be overridden to return 
        any class-dependent or data-dependent color desired.
       */
	    {
	    	return defaultBackgroundColor;
	    	}

  } // interface DataNode.
