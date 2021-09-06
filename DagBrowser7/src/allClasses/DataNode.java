package allClasses;

import static allClasses.AppLog.theAppLog;
import static allClasses.SystemSettings.NL;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.tree.TreePath;

import allClasses.AppLog.LogLevel;
import allClasses.epinode.MapEpiNode;
import allClasses.javafx.EpiTreeItem;
import allClasses.javafx.Selections;
import allClasses.javafx.TitledListNode;
import allClasses.javafx.TitledTextNode;
import allClasses.javafx.TreeStuff;
import allClasses.multilink.ElementMultiLink;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public abstract class DataNode //// test 1

  implements ElementMultiLink<DataNode>

  {

	  // Constructors and initialization.

			public DataNode() // Constructor.
			  {}

    /* Much of the code here came from an earlier version of DataNode.
      
      ///fix After the code is moved, some of it will need to be generified.

      ///opt: Remove theMaxLogLevelto reduce node storage, 
        also the methods what use it,
        because this feature hasn't been used for a long time,
        and there are probably better ways to do controlled logging.
      */
    
    /* This class forms the basis of the classes 
      which represent the DAG (Directed Acyclic Graph). 
      All subclasses of this class add non-DAG-essential capabilities.
      Presently it does not support full DAG capabilities,
      because a node can have only one parent.  Eventually this might change.

      When displayed, the DAG is treated as a tree.
      Many of methods in this class are similar to 
      methods in the TreeModel interface.
      DataTreeModel implements this interface.
      Many of its getter methods simply call the DataNode equivalents.
      
      Some of the methods in this class are 
      non working stubs and MUST be overridden.
      Other methods in this class work as is,
      but might be inefficient in nodes with large numbers of children, 
      and SHOULD be overridden. 

      One important method in this class is 
        JComponent getDataJComponent(
          TreePath inTreePath, 
          DataTreeModel inDataTreeModel
          )
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
        This could be a MultiLink object.
  
      */
  
    // Instance variables

      protected DataTreeModel theDataTreeModel; // For reporting DAG changes.
        // This also doubles as a flag for the propagateDownV(..) method.
        // theDataTreeModel != null means down-propagation into this node
        // and all its descendants has already occurred and 
        // need not be done again, except for new added children. 

      /* Tree node change notification system.
       
        This is part of a multiple thread system for 
        aggregating and processing DataNode change notifications 
        from multiple threads and eventually 
        displaying changed nodes to the GUI.
        Communication happens through shared memory consisting of
        variables names "theTreeChange" in the DataNode tree.
        Threads that report and aggregate node changes write
        values other than TreeChange.NONE to the variables and
        propagates these changes toward the root.
        The thread that processes and displays nodes to the GUI
        writes the value TreeChange.NONE to the variables
        and propagate those changes toward the leaves.
        A change to any node means that it and all its ancestors
        should be redisplayed to the GUI.
        This is an example of restricted value multitasking communication. 
        No locks are used.
        
        This system is used for deciding when to repaint
        cells representing DataNodes in JTrees and JLists.
        ///enh Make it work with JavaFX trees also.
        */
      public volatile TreeChange theTreeChange= TreeChange.NONE;
      public enum TreeChange
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
          theTreeChange field should probably be converted into two fields:
          * a field to indicate subtree changes
          * a field to indicate structure changes

          ///org A more scalable solution might be 
          to not propagate general changes 
          which might cause a GUI appearance change,
          but propagate node field change dependencies between nodes,
          then propagate fields locally into the node's GUI appearance.
          This way, invalidations would be limited to only
          attributes in nodes that were actually being displayed. 
          */
        {
          // Minimum values needed for correct operation.

          // Value Name           // Value Description

          NONE,                   // No changes here or in descendants.
                                  // The cell is correct as displayed.
                                  // This subtree may be ignored.
          STRUCTURE_CHANGED,      // This subtree contains major changes. 
                                  // Requires structure change event.  The node 
                                  // and its descendants need redisplay.
          SUBTREE_CHANGED,        // This node and some of its descendants 
                                  // have changed and should be redisplayed.
                                  // Requires child checking and change events.

          ///opt Other values for possible later optimizations.  Unused.
          /*   ///opt
          INSERTED,     // this node or subtree has been inserted
          INSERTED_DESCENDANTS,   // one or more children have been inserted
          CHANGED,

          NODE,         // this node changed
          CHILDREN,     // one or more of this node's children
          REMOVALS,     // one or more children have been removed
          */  ///opt
          }

      private NamedList treeParentNamedList= null; /* Parent node of this node.
        If there is a parent, the parent must be a list, hence NamedList.

        The value in this variable has multiple uses.
        * It is used for up-propagation of change notifications,
          needed for timely updating of displays.
        * It is used in the first phase of translating
          a DataNode to an associated 
          * MapEpiNode or MetaNode in order to access metadata.
          * TreeItem for JavaFX TreeViewer. 

        This variable might be replaced by a list later, possibly a MultiLink,
        when and if DAGs are supported and multiple parents are required.

        ///opt If we eliminate StateList.parentStateList then
        // this would need to be changed to a StateList,
        // unless we allow casting of the variable.
        
        */

      protected LogLevel theMaxLogLevel= AppLog.defaultMaxLogLevel;
        // Used to determine logging from this node and its descendants.
        ///opt: Remove to reduce node storage, because
        // this feature hasn't been used for a long time.

      private static NamedLeaf dummyDataNode=
          NamedLeaf.makeNamedLeaf( "DUMMY-DataNode" );
      
    // Static methods.

      static DataNode[] emptyArrayOfDataNodes()
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
          It is meant to be overridden if it needs 
          to do anything but finalize its children, 
          for example close any files that it has opened.
          Returns the number of nodes finalized, which in this case is 1.
          See NamedList for an override example.
          
          Note, this does not finalize some nodes with 
          children that are created with lazy eveluation,
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
            
          This method simply stores theDataTreeModel.
          It does nothing else because this is a leaf node 
          and leaves have no subtrees, so the propagation ends here.
          List nodes will override this method.  See NamedList.
          
          This method should be called last because 
          theDataTreeModel is the controlling flag for tree traversal.
          */
        {
          this.theDataTreeModel=  theDataTreeModel;
          }

      public void setTreeParentToV( NamedList treeParentNamedList )
        /* This method is called when a DataNode is added to a NamedList
          or one of its subclasses.  
          This method stores treeParentNamedList within this node, 
          because every node has a parent and needs access to its parent.
          */
              {
          this.treeParentNamedList= treeParentNamedList;
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
          if ( getTreeParentNamedList() == null )
            {
              theAppLog.debug("reportChangeOfSelfV(): treeParentNamedList == null!");
              }
            else
            getTreeParentNamedList().reportChangeInChildB( 
                (DataNode)this ///org Temporary until generified. 
                );
          }

      protected boolean tryProcessingMapEpiNodeB(MapEpiNode theMapEpiNode)
        /* This method tries processing a MapEpiNode.
          It is supposed to return true if processing is successful, false otherwise.
          This version does no processing so it returns false.
          Methods which override this method will return true in some cases.
          For an example, see the NamedList class.
          */
        { 
          return false; 
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
                theLogLevel, null, theLogString, theThrowable, consoleB );
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
              theLogLevel, null, theLogString, theThrowable, consoleB );
          }


      // Other instance methods.
    
      public int IDCode() { return super.hashCode(); } /// Unused.
    
      public boolean isRootB()
        /* Returns whether this node is the root node.
         * This default method returns false.
         * The class which defines the root should return true.
         * This method provides an easy and low cost way
         * to determine whether a node is the root node without
         * comparing node references, which would require having
         * access to the known root node.
         */
        { 
          return false; 
          }


    // Instance getter and tester methods with equivalents in DataTreeModel.

      public boolean isLeaf()
        /* This method and any of its overridden version
          returns true if this is a leaf node,
          which means it can never have children.
          It returns false otherwise.
          The method of this class always returns false as the default value.
          */
        { 
          return false; 
          }
  
      public int getChildCount()
        /* This method returns the number of children in this node.
          The method of this class actually scans all the children that are
          visible to the method getChild(..) and counts them.  
          It assumes a functional getChild( IndexI ) method which
          returns null if IndexI is out of range.  
          */
        { // getChildCount()
          int childIndexI= 0;  // Initialize child index.
  
          while  // Process all children...
            ( getChild( childIndexI ) != null )  // ...returned by getChild(.).
            { // process this child.
              childIndexI++;  // increment index.
              } // process this child.
  
          return childIndexI;  // Return ending index as count.
          } // getChildCount()
  
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

      public ObservableList<DataNode> getChildObservableListOfDataNodes()
        /* Returns an ObservableList containing the children of this object.
         */
        { 
          return FXCollections.observableArrayList(getChildListOfDataNodes()); 
          }

      public List<DataNode> getChildListOfDataNodes()
        /* Returns a List containing the children of this object.
         * This version builds a list by adding all children to an empty list.
         * 
         * This method should be overridden if a non-leaf subclass
         * already contains its children in its own list.
         */
        { 
          List<DataNode> theList= // Create empty list. 
              new ArrayList<DataNode>();
          for // Add all the children.
            (
              int childIndexI= 0; 
              childIndexI < getChildCount();
              childIndexI++
              )
            {
              theList.add(getChild(childIndexI));
              } 
          return theList;
          }

      public List<DataNode> getChildLazyListOfDataNodes()
        /* Returns a List containing the children of this object
         * which might contain temporary unevaluated children.
         * It returns a regular evaluated list 
         * if an unevaluated one does not exist.
         * 
         * This version builds and returns an empty list.
         * 
         * This method should be overridden by any class
         * whose children's laziness differs from its superclass.
         */
        { 
          List<DataNode> theList= // Create empty list. 
              new ArrayList<DataNode>();
          return theList; // Return it.
          }


  /* Methods which return Strings containing information about the node.
    These may be overridden by subclasses as needed.
    The meaning of each method should be preserved in the overrides.
    */

    protected String getNodePathString()
      /* Recursively calculates and returns the path to this node.
        The path is a comma-separated list of node names
        from the root of the hierarchy to this node.
        It gathers the path elements by following node parent links. 
        It is used only the DataTreeModel for logging.
       */
      {
        String resultString;
        
        if ( getTreeParentNamedList() == null )
          resultString= getNameString();
        else
          resultString= 
            getTreeParentNamedList().getNodePathString()
            + ", "
            + getNameString(); 

        Nulls.fastFailNullCheckT(resultString);
        return resultString;
        }

    public String getMetaDataString()
      /* Returns meta-data of this DataNode as a String.
        This is typically a sequence of name:value pair attributes 
        and the names of present value-less attributes.
        Meta-data is generally considered to be all associated attributes except 
        content-type attributes, which tend to be large.
        This default method returns the concatenation of the Name attribute and,
        if its value is non-blank, the Summary attribute.
        Other classes might want to override this method to return
        additional attributes.
        */
      { 
        String resultString= "Name=" + getNameString(); // Assume only Name attribute.
        String summaryString= getSummaryString();
        if (! summaryString.isEmpty()) // Append Summary attribute if value isn't blank
          resultString+= " Summary=" + summaryString;

        return resultString; 
        }

    public boolean isDecoratingB()
      /* Returns indication of whether node String decoration is enabled.
        By default it is disabled.
        Subclasses that want decoration should override this method
        with one that returns true.
       */
      {
        return false;
        }

    public String toString()
      /* According the Java documentation, this is supposed to returns 
        a meaningful and useful String representation.
        Typically it returns a possibly very long string which represents 
        the entire object state, including objects referenced in fields,
        So this, in theory, is the most complex of the String-returning methods.
        But the Swing cell-rendering methods use this to get a value suitable
        for display in a cell, including JTree cells.
        So this method is defined to return a limited length string.
        This method returns a decorated cell string.
        */
      { 
        if (isDecoratingB()) // If decoration is enabled
          return decoratedCellString(); // return decorated string
          else // otherwise
          return getCellString(); // return undecorated string.
        }

    public String decoratedCellString()
      /* This method returns String appropriate for display in
        a Component cell which  might include decoration
        with other characters to indicate state information.
        This method returns the default of the undecorated cell String,
        but subclasses could decorate as desired.
        */
      {
        return getCellString();
        }

    public String getCellString()  
    /* Returns an undecorated String representation of this node, 
      suitable for use in rendering the node
      in the cell of a JTree, JList, or other Component.
      This version returns the node name followed by
      the Summary attribute value if it is not blank.
      */
      {
        String resultString= getNameString(); // Assume result will be name only.

        String summaryString= getSummaryString();
        if (! summaryString.isEmpty()) // Append value if there is one. 
          resultString+= " : " + summaryString;

        return resultString; 
        }

    public String getSummaryString()
      /* This method returns Summary attribute value String of the node.
        It is a calculated value that is meant to be combined 
        with the node's name and used for rendering in a Component cell.
        The String might be a value, a child count,
        or some other type of summary information.
        This default method returns the first line of 
        the Content attribute value.
        Other classes might want to override this behavior.
        */
      {
        String resultString= getContentString(); // Assume no content trimming is needed.
        int indexOfNewLineI= resultString.indexOf(NL);
        if // Trim extra lines if there are any in content string.
          ( indexOfNewLineI >= 0 )
          resultString= // Replacing string with only its first line. 
            resultString.substring(0,indexOfNewLineI);
        return resultString;
        }

    public String getContentString()
      /* Returns the content of the DataNode as a String.  
        This is potentially a long String, such as the content of a file, 
        and it might consist of multiple lines.
        This method will be overridden.
        */
      {
        return
            "UNDEFINED-CONTENT-OF-"
            + super.toString(); // Object's toString(), equivalent to: 
              // getClass().getName() + '@' + Integer.toHexString(hashCode())
        }

    public String getNameString()
      /* Returns the name of the DataNode as a String.  
        This should be a unique String among its siblings because
        it will be used to distinguish it from those sibling,
        and as part of tree path-names.
        It need not be unique in of all the DataNodes in the DataNode tree.

        This might not necessarily be exactly what 
        is displayed in JTree or JList cells.
        Object.toString() satisfies these requirements.
        This method will be overridden.
        */
      {
        return
          DataNode.class.getSimpleName()
            + '@' 
            + Integer.toHexString(((Object)this).hashCode());
              // getClass().getName() + '@' + Integer.toHexString(hashCode())
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
        This DataNode, the DataNode to be viewed,
        is the last element of inTreePath,

        This method may be overridden if a more specialized viewer is needed.
        */
      {
        JComponent resultJComponent= null;

        if ( isLeaf() ) // Using TitledTextViewer if node is leaf.
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

    public TreeStuff makeTreeStuff( 
        DataNode selectedDataNode,
        Persistent thePersistent,
        DataRoot theDataRoot,
        EpiTreeItem theRootEpiTreeItem,
        Selections theSelections
        )
      /* This is the JavaFX version of getDataJComponent(.).
       * 
       * This method creates a TreeStuff object for displaying this DataNode.
       * * selectedDataNode is the DataNode to be the selected child data node,
       *   or null if there is to be no selected child DataNode,
       *   or the selection should come from the selection history.
       * * thePersistent provides access to persistent data storage.
       * * theDataRoot provides access to the root of the data hierarchy.
       * * theSelections provides access to the DataNode selection history.
       * 
       * This implementation creates one of 2 default JavaFX Node viewers:
       * * one for displaying leaves as text
       * * one for displaying branches as a list
       * A DataNode subclass may override this method to create viewers
       * customized for viewing that particular subclass. 
       */
      {
        TreeStuff resultTreeStuff;
        if ( isLeaf() ) // Create a text viewer Node if this DataNode is leaf.
          resultTreeStuff= TitledTextNode.makeTreeStuff(
                this,
                selectedDataNode,
                getContentString(),
                thePersistent,
                theDataRoot,
                theRootEpiTreeItem,
                theSelections
                );
          else  // Create a list viewer Node if this DataNode is not a leaf.
          resultTreeStuff= TitledListNode.makeTreeStuff(
                this,
                selectedDataNode,
                theDataRoot,
                theRootEpiTreeItem,
                thePersistent,
                theSelections
                );
        return resultTreeStuff; // Return the view that was created.
        }

    public TreePath getTreePath()
      /* This method returns the TreePath associated with this DataNode.
       * The path always includes at least one element and
       * this DataNode is always the last element of the path.
       * 
       * This method would be simpler if all constructors of TreePath
       * were public.
       * 
       *  ///opt Use cache like 
       *    DataTreeModel.translatingToTreePath( DataNode targetDataNode ).
       */
      { 
        TreePath resultTreePath;
        if ( null == getTreeParentNamedList() ) // If node has no parent then
          resultTreePath= 
            new TreePath(this); // path is only this node.
        else // Node has a parent so recursively
          resultTreePath= // construct path from parent and this node. 
            getTreeParentNamedList().getTreePath().pathByAddingChild(this);
        return resultTreePath;   
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
                ( inString.equals( childDataNode.getNameString() ) )
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

    public NamedList getTreeParentNamedList()
      /* This method returns a reference to this node's tree parent node.
       * 
       * This method has different uses, depending on context.
       * * Sometimes it is used to get the parent DataNode.  
       *   This is the tree sense.
       * * Sometimes it is used to access the viewer of the parent DataNode.
       *   This is the DAG sense.
       * 
       * This method might be replaced by multiple methods later,
       * one for trees and one for DAGs.
       */
      {
        return treeParentNamedList;
        }

    
    // Methods refactored/moved from DataTreeModel.

    /* The following code is used to aggregate changes to the
      TUDNet hierarchy for later and more efficient display to the user.
      These methods do not need to be called on the Event Dispatch Thread (EDT).

      This is in implementation of up-propagation of changes 
      that might affect the the appearance of ancestor nodes.
      This is not a general-purpose up-propagation system.
      It is highly customized for Java, its JTree class, and its GUI.
      */

    public static void signalRemovalV(
        DataNode parentDataNode, 
        int indexI, 
        DataNode childDataNode 
        )
      /* This method signals the removal of a single child DataNode, 
        childDataNode, from theSubjectDataNode at child position indexI.
        ///opt Implement without StructuralChange.
        */
      {
        EDTUtilities.testAndLogIfRunningEDTB();
        signalStructuralChangeInV( // Translate to a structural change. 
            parentDataNode, indexI, childDataNode );
        }

    static void signalStructuralChangeInV(
        DataNode parentDataNode, 
        int indexI, 
        DataNode childDataNode 
        )
      /* This method is used to signal a structural change in 
        theSubjectDataNode.  It is used to deal with insertions and deletions.
        */
      {
        EDTUtilities.testAndLogIfRunningEDTB();
    
        switch // Act based on present flag value.
          ( parentDataNode.theTreeChange ) 
          { 
            case NONE: // Node is unmarked, so must mark it.
            case SUBTREE_CHANGED: // Node marked changed, so must override it.
              parentDataNode.theTreeChange= // Mark node. 
                TreeChange.STRUCTURE_CHANGED;
              signalSubtreeChangeV( // Propagate as ordinary change 
                  parentDataNode.getTreeParentNamedList()); // to parent.
              break;
              
            case STRUCTURE_CHANGED: // Already marked as desired
              ; // so no additional marking is needed.
              break;
            }
        }

    static void signalSubtreeChangeV( 
        DataNode theDataNode
        )
      /* This method signals that a change happened that might affect
        this node or one of its subtrees.
        If it needs to change the update status of theDataNode then
        it will also propagate the change to the node's parent if needed.
        */
      {
        EDTUtilities.testAndLogIfRunningEDTB();
        if ( theDataNode == null ) // No node to update 
          ; // so do nothing.
        else // Mark as changed this node and its unmarked ancestors.
          switch ( theDataNode.theTreeChange ) {
            case NONE: // Node is unmarked, so now it must be.
              theDataNode.theTreeChange= // Mark node changed. 
                TreeChange.SUBTREE_CHANGED;
              signalSubtreeChangeV( // Propagate change to parent, if any. 
                  theDataNode.getTreeParentNamedList() );
              break;
              
            case STRUCTURE_CHANGED: // Already marked for structure change.
            case SUBTREE_CHANGED: // Already marked with node/subtree change.
              ; // So no additional marking is needed.
              break;
            }
        }

    public static void signalInsertionV(
        DataNode parentDataNode, 
        int indexI, 
        DataNode childDataNode 
        )
      /* This method signals the insertion of a single child DataNode, 
        childDataNode, into theSubjectDataNode at child position indexI.
        ///opt Implement without StructuralChange.
        */
      {
        EDTUtilities.testAndLogIfRunningEDTB();
        DataNode.signalStructuralChangeInV( // Translate to a structural change.
            parentDataNode, indexI, childDataNode );
        }

    public static void signalChangeV( DataNode theDataNode )
      /* This method signals the change of a single DataNode, 
        theDataNode.
        */
      {
        EDTUtilities.testAndLogIfRunningEDTB();
        DataNode.signalSubtreeChangeV( theDataNode ); // Convert to a subtree change.
        }

  
    /* The following code is used to display to the user
        previously aggregated changes recorded in 
        theTreeChange-s of DataNodes in the TUDNet hierarchy.

        The displaying is done with the firing of notification events 
        to GUI components such as JTrees and JLists which
        are displaying parts of the TUDNet hierarchy.
        Wone exception, these methods must be called on 
        the Event Dispatch Thread (EDT).
        The one exception is displayTreeModelChangesV(),
        which is the only public method in this group.
        */
    
    public static void displayChangedNodesFromV(
        TreePath parentTreePath, DataNode theDataNode,EpiTreeItem theEpiTreeItem 
        )
      /* If theDataNode has changed, as indicated by theTreeChange,
        then it is displayed and repeats this for any children.
        The TreePath of its parent is parentTreePath.

        theEpiTreeItem is ignored, but will be used for JavaFX TreeView display.
        */
      {
        if ( theDataNode == null ) // Nothing to display. 
          ; // Do nothing.
        else { // Check this subtree.
          theAppLog.trace( "DataTreeModel.displayChangedNodesFromV() "
              + theDataNode.getNodePathString() );
          // Display this node and any updated descendants if it's needed.
          switch ( theDataNode.theTreeChange ) {
            case NONE: // This subtree is unmarked.
              ; // So display nothing.
              break;
            case STRUCTURE_CHANGED: // Structure of this subtree changed.
              displayStructuralChangeV( parentTreePath, theDataNode );
              break;
            case SUBTREE_CHANGED: // Something else in this subtree changed.
              displayChangedSubtreeV( 
                  parentTreePath, theDataNode, theEpiTreeItem );
              break;
            }
          }
      }

    private static void displayStructuralChangeV(
        TreePath parentTreePath, DataNode theDataNode 
        )
      /* This method displays a structural change of
        a subtree rooted at theDataNode.
        The TreePath of its parent is parentTreePath.
    
        ///fix?  It is presently assumed that displaying a structural change
        will be handled by displaying the entire subtree.
        If this is not true then it might be necessary
        to report which nodes have changed also,
        probably after the structure change has been reported.
        It shouldn't do any harm, except maybe take extra time.
        */
      {
        resetChangesInSubtreeV( // Do this now so late changes are recorded.
            theDataNode );
    
        TreePath theTreePath= parentTreePath.pathByAddingChild(theDataNode);
        theDataNode.theDataTreeModel.reportingStructuralChangeB( theTreePath ); // Display by reporting
          // to the listeners.
        }

    static void displayChangedSubtreeV(
      TreePath parentTreePath, DataNode theDataNode, EpiTreeItem theEpiTreeItem 
      )
    /* This method displays a subtree rooted at theDataNode,
      a subtree which is known to have changed,
      It does this by reporting changes to the Java GUI.
      The descendants are displayed recursively first.
      The TreeChange of all the nodes of any subtree displayed is reset. 

      parentTreePath is the TreePath of the subtree's parent 
      and is used to identify the tree node to the Swing GUI.
      */
    {
      theAppLog.trace( "DataTreeModel.displayChangedSubtreeV() "
          + theDataNode.getNodePathString() );
      theDataNode.theTreeChange= // Unmark the subtree root now
          TreeChange.NONE; // to prevent loss of future changes.
    
      TreePath theTreePath= parentTreePath.pathByAddingChild(theDataNode); 
      int childIndexI= 0;  // Initialize child scan index.
      while ( true ) // Recursively display any changed descendants.
        { // Process one descendant subtree.
          DataNode childDataNode=  // Get the child, the descendant root.
             theDataNode.getChild( childIndexI );
          if ( childDataNode == null )  // Null means no more children.
              break;  // so exit while loop.
          DataNode.displayChangedNodesFromV( // Recursively display descendants.
              theTreePath, childDataNode, theEpiTreeItem ); 
          childIndexI++;  // Increment index for processing next child.
          }
      theDataNode.theDataTreeModel.reportingChangeB( // Display root with Swing.
          parentTreePath, theDataNode );
      Platform.runLater(() -> {
        ////// theEpiTreeItem.reportingChangeB( // Display root with JavaFX.
        //////     parentTreePath, theDataNode );
        // force reference change
        //// theEpiTreeItem.setValue(null);
        theEpiTreeItem.setValue(dummyDataNode);
        // theEpiTreeItem.setValue(theDataNode);
        System.out.println(
          "DataNode.displayChangedSubtreeV(.) "+theDataNode+", "+theEpiTreeItem);
        }); 
      }

    static void resetChangesInSubtreeV( DataNode theDataNode )
      /* This method resets the change status of 
        all the nodes in a subtree rooted at theDataNode.
        The node's descendants that need resetting 
        are reset recursively first.
        All nodes that need resetting 
        are assumed to be connected to theDataNode.
        */
      {
        switch ( theDataNode.theTreeChange ) {
          case NONE:
            ; // Do nothing.
            break;
          default: 
            theDataNode.theTreeChange= // Reset root node update status. 
                TreeChange.NONE;
            int childIndexI= 0;  // Initialize child scan index.
            while ( true ) // Recursively reset appropriate descendants.
              { // Process one child subtree.
                DataNode childDataNode=  // Get the child.
                   (DataNode)theDataNode.getChild(childIndexI);
                if ( childDataNode == null )  // Null means no more children.
                    break;  // so exit while loop.
                resetChangesInSubtreeV( // Recursively reset child subtree. 
                    childDataNode ); 
                childIndexI++;  // Increment index for processing next child.
                }
            break;
          }
        }

    }
