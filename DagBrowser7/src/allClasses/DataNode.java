package allClasses;

import allClasses.AppLog.LogLevel;

import java.awt.Color;

import javax.swing.JComponent;
import javax.swing.tree.TreePath;

import static allClasses.AppLog.theAppLog;
import static allClasses.SystemSettings.NL;


public class DataNode 
  {

	  /* This class forms the basis of the classes 
	    which represent the DAG (Directed Acyclic Graph). 
	    All subclasses of this class add non-DAG-essential capabilities.
	    Presently it does not support full DAG capabilities,
	    because a node can have only one parent.  Eventually this might change.

      When displayed, the DAG is treated as a tree.
      Many of methods in this class are similar to methods in the TreeModel interface.
      DataTreeModel implements this interface.
      Many of its getter methods simply call the DataNode equivalents.
	    
	    Some of the methods in this class are 
	    non working stubs and MUST be overridden.
	    Other methods in this class work as is,
	    but might be inefficient in nodes with large numbers of children, 
	    and SHOULD be overridden. 

      One important method in this class is 
        JComponent getDataJComponent(TreePath inTreePath, DataTreeModel inDataTreeModel).
      Its purpose is to return a JComponent that can 
      display and possibly manipulate an instance of this DataNode.
      This method in this class returns JComponents that
      display the node either as a simple list or as a block of text.
      More complicated DataNodes should override this default method
      with one that returns a more capable JComponent.   

	    ///enh Possible methods to add:
	
				?? Add getEpiThread() which returns the EpiThread associated with
				  this DataNode, or null if there is none, which is the default.
				  This would standardize and simplify thread starting and stopping.
				   
			  ?? Add JComponent getSummaryJComponent() which returns 
			    a component, such as a Label, which summarizes this DataNode,
			    at least with the name, but possibly also with a summary value,
			    can be used as a part of its parent DataJComponent.
			    See getSummaryString() which returns a String.
			    This might be able to be temporary, like a Render component.
	
	    ?? Maybe add a field, parentsObject, which contains references to
	      DataNodes which are parents of this node.
	      This would be used by DataTreeModel.translatingToTreePath( .. ).
	
	    */
	
	  // Instance variables

	    /* Variables for a hybrid computing cache using
	      lazy evaluation by up-propagation of 
	      properties DataNode in the DataNode hierarchy.
	      Changes in a DataNode can affect the properties,
	      including the displayed appearance, of ancestor DataNodes.
	      They are used mainly for deciding when to repaint
	      cells representing DataNodes in JTrees, JLists, and JLabels.
	     	*/
			public ChangeFlag theChangeFlag= ChangeFlag.NONE;
		  public enum ChangeFlag  
		    /* These constants are used to delay and manage the firing of 
		      node appearance change notification events to GUI components.
		      It is based on the theory that if the appearance of a node changes,
		      then it might affect the appearance of the node's ancestors,
		      and the GUI display of those nodes should happen soon.

		      Besides NONE and SUBTREE_CHANGED, which would be sufficient for
		      node value changes in a tree with unchanging size and shape, 
		      there is also STRUCTURE_CHANGED,
		      which helps in dealing with changes in a tree's branching structure.
		      STRUCTURE_CHANGED is interpreted as a need to reevaluate
		      and display the entire subtree that contains it.
		      
		      ///org To better match the way caches are invalidated,
		      theChaneFlag field should probably be converted into two fields:
		      * a field to indicate subtree changes
		      * a field to indicate structure changes
		      SUBTREE_CHANGED and STRUCTURE_CHANGED 
		      are associated with value invalidation.
		      NONE is associated with value invalidation.
		      
		      ///org A more scalable solution would be 
		      to not propagate general changes 
		      which might cause a GUI appearance change,
		      but propagate node field change dependencies between nodes,
		      then propagate fields locally into the node's GUI appearance.
		      This way, invalidations would be limited to only
		      attributes in nodes that were actually being displayed. 
		      */
			  {
			  	// Minimum needed for correct operation.
			  	NONE, 									// No changes here or in descendants.
			  													// The cell is correct as displayed.
					                        // This subtree may be ignored.
			  	STRUCTURE_CHANGED,			// This subtree contains major changes. 
																	// Requires structure change event.  The node 
			  													// and its descendants need redisplay.
			  	SUBTREE_CHANGED,				// This node and some of its descendants 
			  													// have changed and should be redisplayed.
			  													// Requires child checking and change events.

			  	///opt Other values for possible later optimizations.  Unused.
			  	/*   ///opt
			  	INSERTED,			// this node or subtree has been inserted
			  	INSERTED_DESCENDANTS,		// one or more children have been inserted
			  	CHANGED,

			  	NODE,					// this node changed
			  	CHILDREN,			// one or more of this node's children
			  	REMOVALS,			// one or more children have been removed
			  	*/  ///opt
					}

			protected NamedList parentNamedList= null; // My parent node.
			  // If there is a parent, it must be a list.
        ///opt If we eliminate StateList.parentStateList then
        // this would need to be changed to a StateList,
        // unless we allow casting of the variable.

			protected LogLevel theMaxLogLevel= AppLog.defaultMaxLogLevel;
			  // Used to determine logging from this node.

    // Static methods.

		  static DataNode[] emptyListOfDataNodes()
		  	// This method returns a new empty DataNode list.
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

  	  protected int finalizeDataNodesI()
  	    /* This method is called to finalize the non-State aspects of
  	      the subtree rooted at this DataNode.
  	      It is meant to be overridden if it needs to do anything but
  	      finalize its children, for example close files that it has open.
  	      Returns the number of nodes finalized, which in this case is 1.
          See NamedList for an override example.
          
          Note, this does not finalize some nodes with virtual children,
          for example Infinitree and IFile, which do not extend NamedList.
  	      */
  		  {
  	      //theAppLog.debug("DataNode.finalizeDataNodesV() called.");
  	      // This base class has nothing to do.
  	      return 1; // Return a total of 1 node for just ourself.
  		  	}

      protected void propagateIntoSubtreeV( DataTreeModel theDataTreeModel )
        /* This method is called to propagate theDataTreeModel
          into the nodes which needed it when 
          a DataNode is added to a NamedList or one of its subclasses.
            
          This method ignores theDataTreeModel and does nothing 
          because this is a leaf node and leaves have no subtrees,
          so the propagation ends here.
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
        /* This method reports a change of this node 
          which might affect it appearance.
          It does this by calling a parent method,
          because it is more convenient for a parent to
          notify the TreeModel about changes in its children
          than for the children to do it.
         */
      	{
      	  if ( parentNamedList == null )
      	  	{
	      	  	theAppLog.debug("reportChangeOfSelfV(): parentNamedList == null!");
      	  	  /// Eventually replace variable or link it to DAG.
	      	  	}
      	  	else
      			parentNamedList.reportChangeInChildB( this );
      		}

      
      /* Customized logging methods.
        These methods, along with this.theMaxLogLevel and class AppLog,
        are used to control what is logged from this class and its subclasses.
        this.theMaxLogLevel works in a way similar to AppLog.maxLogLevel,
        to determine which log statements are executed.
        this.theMaxLogLevel is usually changed at the same time as
        theMaxLogLevel of child DataNodes.
        The method propagateIntoSubtreeB(..) makes 
        the control of logging of entire subtrees possible.  
       */

  	  protected boolean propagateIntoSubtreeB( LogLevel theMaxLogLevel )
		    /* This method propagates theMaxLogLevel into this node.
		      It acts only if the present level is different from theMaxLogLevel.
			   	Subclasses with descendants should propagate into those also.
			   	
			   	Unlike propagateIntoSubtreeV( DataTreeModel ),
			   	this method is called only when changes to the default logging
			   	are needed for debugging purposes.
			   	This method can be called whenever it is determined that
			   	special logging would be helpful.
			   	
			   	///opt The checking whether a change is needed
			   	  is probably not needed, and should eventually be eliminated.
			   	  Meanwhile, it shouldn't cause any harm. 
			   	*/
			  {
		  	  boolean changeNeededB= ( this.theMaxLogLevel != theMaxLogLevel ); 
			  	if // Make change only if new level limit is different.
		  	    ( changeNeededB )
				  	{
			  			this.theMaxLogLevel= theMaxLogLevel;
				  		}
			  	return changeNeededB;
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
  		      theAppLog.logV( 
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
		      theAppLog.logV( 
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

	  protected String getNodePathString()
  	  /* Recursively calculates and returns 
  	    a comma-separated list of node names
  	    from the root of the hierarchy to this state.
  	   */
  	  {
  		  String resultString;
  		  
  		  if ( parentNamedList == null )
  		  	resultString= getNameString();
  		  else
  		    resultString= 
  		    	parentNamedList.getNodePathString()
  		  		+ ", "
  		  		+ getNameString(); 

  		  Nulls.fastFailNullCheckT(resultString);
  		  return resultString;
  	  	}
	  	
    public String getNameString( )
	    /* Returns the name of the DataNode as a String.  
	      This should be a unique String among its siblings.
	      It will be used as part of a path-name and which
	      identifies this DataNode distinctly from all other tree nodes.
	      */
      {
    	  return "-UNDEFINED-NAME-"; // Base class has no name.
        }

    public String getValueString( )
	    /* Returns the value of the DataNode as a String.  
	      This is meant to be a very short summary string that
	      might be appended to the name to represent a title. 
	      */
      {
    		return "-UNDEFINED-VALUE-"; // Base class has no value.
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

    public String getLineSummaryString()  
    /* Returns a one-line summary of
      the contents of this DataNode as a String.
      The line consists of the name of the node,
      and sometimes followed by something else,
      such as a child count, or some other type of value summary information.
      This is not meant to be a unique identifier.  
      */
      {
    	  String nameString= processedNameString();
    	  String valueString= processedValueString();
    	  
    	  // Combine name with value.
    	  String resultString;
    	  if // Combining with name if value not nil or same as name.
    	  	( ( valueString != "" ) && 
    	  		( ! nameString.equals(valueString) ) 
    	  		)
      	  resultString= // Using
      	  		nameString // the name,
	        		+ " : " // a separator,
	        		+valueString // and the value.
	        		;
    	  	else
      	  resultString= nameString; // Using only the name.
    	  
    	  return resultString; 
        }

	  public String processedNameString()
	    /* This method returns a name String which is might have been decorated
	      with other characters, but still viewable as a name.
	      This method should not be used to get a unique identifying name,
	      because the decorations could change.
	      getNameString() should be used for that.
	      This version returns the ordinary name,
	      but subclasses could decorate the name as desired.
	     	*/
  		{
		  	return getNameString();
		  	}

	  private String processedValueString()
	    // This method returns a value String which is okay to display in a cell.
	  	{
	  		String valueString= getValueString(); // Initializing process value.
	  		process: {
			  	if // Convert undefined to blank.
			  	  ( valueString == "-UNDEFINED-VALUE-" )
			  	  { valueString= ""; break process; }
			  	int indexOfNewLineI= valueString.indexOf(NL);
		  	  if // Trimming extra lines if there are any in value string.
		  	    ( indexOfNewLineI >= 0 )
		  	  	valueString= // Replacing value string with only its first line. 
		  	  	  valueString.substring(0,indexOfNewLineI);
		  	  } // process:
			  return valueString;
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
      /* Returns a JComponent capable of displaying this DataNode.
        It may use the DataTreeModel inDataTreeModel to provide context.  
        This base class method returns useful defaults:
        * a TextViewer for leaves and 
        * a ListViewer for non-leaves.
        The DataNode to be viewed is the last element of inTreePath,

        This method may be overridden if a more specialized viewer is needed.
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
        but this method may be overridden to return any other color 
        which is a function of class, state, or other data.
       */
	    {
	    	return defaultBackgroundColor;
	    	}

  }
