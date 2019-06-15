package allClasses;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.Rectangle;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.JScrollPane;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;

import static allClasses.Globals.*;  // appLogger;

public class RootJTree 

  extends IJTree

  implements 
    KeyListener, 
    TreeSelectionListener, 
    TreeExpansionListener, 
    TreeAware,
    TreeModelListener
    //, TreePathListener
  
  /* This class is used for the content in the left JTree subpanel.
    
    Possible changes/fixes ??
    
    *!If (Down-Arrow) causes the collapse of a large subtree,
      the final selection is not scrolled into Viewport.
      * It seems to be calling the correct routines, including
        paintImmediately() and scrollPathToVisible(..).
      * The correct path is selected, but it is positioned
        above the top of the Viewport.
      ! expandPath(..) is called AFTER subselectionsEndV()(..)!
        Why is that.

    */

  {
		static final String ExpandedAttributeString= "Expanded"; 
		static final String autoExpandedAttributeString= "AutoExpanded"; 

    // Variables.
      private static final long serialVersionUID = 1L;

      // Injected dependencies.
      private JScrollPane theJScrollPane;  // Associated JScrollPane.
      private MetaRoot theMetaRoot;

      private TreePath savedTreePath;  // Previous selection.  This is...
        // ...for use as first argument to doSubselectionsV(..)

      private TreeHelper aTreeHelper;  // For doing TreePath things.

    public RootJTree( // Constructor.
        DataTreeModel inTreeModel, 
        JScrollPane inJScrollPane,
        MetaRoot theMetaRoot
        ) 
      /* This constructs a RootJTree.
        inTreeModel is the TreeModel from which it gets it tree data.
        inJScrollPane is the JScrollPane of which it is a client.
        inJScrollPane is needed to determine whether a particular tree node
        is visible in the JScrollPane's Viewport.
        */
      { // Constructor.
        super( inTreeModel );  // Construct the superclass.
        theJScrollPane= inJScrollPane;  // Save the JScrollPane.
        this.theMetaRoot= theMetaRoot;

        aTreeHelper=  // Construct extended TreeHelper class instance...
          new MyTreeHelper(  // ...from this nested class.
            this, 
            null,   // Note, TreePath is not set yet.
            theMetaRoot
            );

        //theIJTree.setLargeModel( true );        
        /*
        { // customize the tree cell rendering.
          DefaultTreeCellRenderer TheDefaultTreeCellRenderer = 
              new DefaultTreeCellRenderer();
          TheDefaultTreeCellRenderer.setBackgroundSelectionColor(Color.CYAN);
          setCellRenderer(TheDefaultTreeCellRenderer);
          } // customize the tree cell rendering.
        */

        expandRow( 0 );  // Expand root which should be at row 0.
        setRootVisible( false );  // Do not show the pseudo-parent of root.
        setShowsRootHandles( true );
        putClientProperty( "IJTree.lineStyle", "Angled" );

        getModel().addTreeModelListener( // This listening for TreeModel events.
        		this
        		);
        addTreeSelectionListener(this); // This listening for tree selections.
        addTreeExpansionListener(this); // This listening for expansion events.
        addKeyListener(this);  // This listening for key events.
        aTreeHelper.addTreePathListener( // Customized listening for tree paths.
          new MyTreePathListener()
          );
        addFocusListener(aTreeHelper); // TreeHelper listening for focus events.
        addMouseListener(aTreeHelper); // TreeHelper listening for MouseEvent-s.

        } // Constructor.

    public void initializeV( TreePath selectedTreePath )
      /* This method initializes the state of the JTree,
        or at least the part that is visible in the JTree pane.
        Most of this is based on the state of the MetaNodes.
        
        Things to be initialized:
        = Expansion state.  This comes from the MetaNodes.
        * Selection state.  This comes from selectedTreePath.
        */
      {
    		TreePath subtreeTreePath=  // Get path to
      		theMetaRoot.getTheDataRoot().
      		  getParentOfRootTreePath( );  // to parent of root.
        MetaNode subtreeMetaNode=  // Get parent of root MetaNode.
          theMetaRoot.getParentOfRootMetaNode( );
        expandSubtreeV( // Expanding all nodes that need expanding.
        		subtreeTreePath, subtreeMetaNode 
        		);

        getTreeHelper()  // In TreeHelper...
	        .setPartTreePathB(  // ...select...
	          selectedTreePath  // ...current tree node.
	          );
		      // This JTree selection should trigger a series of events which
		      // load all the data-dependent sub-panel components
		      // and get them ready for display.  
        }

    public void expandSubtreeV( TreePath subTreePath, MetaNode subtreeMetaNode )
      /* This method expands all the nodes in a subtree 
        named by subTreePath using information in subtreeMetaNode.
        */
      {
        KeyMetaPiteratorOfMetaNode // Creating iterator which does the search.  
		      childKeyMetaPiteratorOfMetaNode= 
		      		subtreeMetaNode.makeKeyMetaPiteratorOfMetaNode( 
		      				ExpandedAttributeString
		      				);
        scanner: while (true) { // Scan all nodes with attribute.
          MetaNode childMetaNode= // Test for a child with attribute key.
          		childKeyMetaPiteratorOfMetaNode.getE();
          if  // No more children with the desired attribute.
            ( childMetaNode == null)
            break scanner;  // Exit Processor.
          DataNode childDataNode= // Get associated DataNode.
            childMetaNode.getDataNode();
          if // DataNode is an UnknownDataNode.
          	( ! DataNode.isUsableB( childDataNode ) )
            break scanner;  // Exit Processor.
          TreePath childTreePath=  // Add DataNode to TreePath.
          		subTreePath.pathByAddingChild( childDataNode );
          expandSubtreeChildV( childTreePath, childMetaNode );
          childKeyMetaPiteratorOfMetaNode.nextE(); // Advance Piterator to
            // next child with desired attribute key.
          }
        }

    private void expandSubtreeChildV( TreePath childTreePath, MetaNode childMetaNode )
    /* This is a helper method for expandSubtreeV(..).
      It handles the expansion of the node identified by childTreePath and
      whose Meta data is in childMetaNode.
      It handles the fact that expanding a node expands all of its ancestors. 
      This is because expanding a node is defined to make a node,
      its immediate children, and all its ancestors, viewable.
      */
    {
    	boolean expandedB= // Saving attribute of whether this node is expanded. 
        BooleanAttributeMetaTool.getNodeAttributeB(
      		childMetaNode, ExpandedAttributeString
      		);  // Saving because recursive expand might change the attribute.
	    expandSubtreeV( // Recursively expand child's children if needed.
	    		childTreePath, childMetaNode 
	    		);
	    if // Setting child expansion state according to saved attribute value.
	    	( expandedB ) 
	    	expandPath(childTreePath); // Expanding child node because
	        // all JTree nodes are initially collapsed.
  	    else
	      collapsePath(childTreePath); // Collapsing child node because
	        // the above recursive expansion might have expanded this child.
	    }
    
    /* TreeHelper code, including extension MyTreeHelper 
      and TreePathListener.
      */

    	class MyTreeHelper extends EmptyBranchTreeHelper {
      
        /* This class overrides some TreeHelper methods
          which need to be different for RootJTree.
          */

        MyTreeHelper(  // Constructor.
            JComponent inOwningJComponent, 
            TreePath inTreePath,
            MetaRoot theMetaRoot
            )
          {
            super(inOwningJComponent, theMetaRoot, inTreePath);
            }

        public boolean commandGoToChildB( boolean doB )
          /* Tries to go to an appropriate child if doB is true.
            It returns true if the command is/was doable, false otherwise.
            It tests first for the leaf case, 
            which Tree panels can not handle.
            If it's not a leaf then it lets the superclass method handle it.
            */
          {
              boolean doableB= false;  // Assume command is not doable.

            toReturn: {
              if (getPartDataNode().isLeaf())  // It is undoable leaf case.
                break toReturn;  // Exit with default not doable result.
              if   // Superclass reports no other doable case.
                (!super.commandGoToChildB(false))
                break toReturn;  // Exit with default not doable result.
              doableB= true;  // Override result to indicate command doable.

              if (! doB)  // Command execution is not desired.
                break toReturn; // So exit with doability result.
              
              // Command execution begins.
              super.commandGoToChildB(true);  // Have superclass execute command.

            } // toReturn end.
              return doableB;  // Return whether command is/was doable.

            }
        
        public boolean commandGoToPreviousOrNextB( boolean doB, int incrementI )
          /* This is a helper method for commandGoDownB(..) and commandGoUpB(..).
            If incrementI == +1 it tries to go to the next node.
            If incrementI == -1 it tries to go to the previous node.
            */
          { 
            boolean doableB= false;  // Assume command not doable.
            int rowI= getLeadSelectionRow( );  // Get # of selected row.
            TreePath nextTreePath=  // Convert next row to next TreePath.
              getPathForRow( rowI + incrementI );
            if ( nextTreePath != null )
              doableB= true;  // Indicate command is doable.
            if // Select that path doable and command desired.
              ( doableB && doB )
              setPathV(nextTreePath);  // Select the path.
            return doableB;  // Return doability result.
            }

        @Override
        public void mouseClicked(MouseEvent inMouseEvent) 
          /* This MouseListener method checks for double click on mouse,
            which now means to expand or collapse the present node,
            so is synonymous with the Enter key.
            JTree already does this and it can not be overridden
            so this only notifies TreePathLlistener about
            a possible change which could effect button enabling.
            */
          {
        		if (inMouseEvent.getClickCount() >= 2)
              {
                //commandExpandOrCollapseV();  // Already done by JTree.
            	  //inMouseEvent.consume();  // This not needed either.

                aTreeHelper.notifyListenersAboutChangeV( );
                }
            }

        } // MyTreeHelper

      public TreeHelper getTreeHelper() 
        /* Method to allow access by other classes to 
          the TreeHelper code by returning the TreeHelper.
          */
        { return aTreeHelper; }

      private class MyTreePathListener 
        extends TreePathAdapter
        {
          public void setPartTreePathV( TreePathEvent inTreePathEvent )
            /* This TreePathListener method translates 
              inTreePathEvent TreeHelper tree paths into 
              internal JTree selections.
              It ignores any paths with which it cannot deal.
              */
            {
              TreePath inTreePath=  // Get the TreeHelper's path from...
                inTreePathEvent.  // ...the TreePathEvent's...
                  getTreePath();  // ...one and only TreePath.

              toReturn:{
                if (inTreePath == null)  // null path.
                  break toReturn;  // Ignore it.

                if // Last node is an UnknownDataNode
                  ( UnknownDataNode.isOneB( inTreePath.getLastPathComponent() ) )
                  break toReturn;  // Ignore it.
                if (   // Parent of root.
                    inTreePath 
                    == theMetaRoot.getTheDataRoot().getParentOfRootTreePath( )
                    )
                  break toReturn;  // Ignore it.

                // Execution begins on fully checked path.
                setSelectionPath(inTreePath);  // Select path in the JTree.
              	} // toReturn:
              }
          }

      private void setPathV(TreePath inTreePath )
        /* This method is simply shorthand for
          aTreeHelper.setPartTreePathV(..).
          */
        {
          aTreeHelper.setPartTreePathB( // In TreeHelper set path to...
            inTreePath  // ...desired TreePath.
            );
          }
    
      public TreePath getSelectedTreePath()
        /* This method returns the selection TreePath.
          There should be only one, because multiselection is disabled.
          */
        {
          return getSelectionPath();  // Get path directly from JTree.
          }

    // Tree Command methods.  Other ones were moved to TreeHelper.

      private void commandExpandOrCollapseV() // Move to MyTreeHelper??
        /* This command method toggles whether 
          the presently selected node/row is expanded or collapsed.  
          This is for manual expansions and collapses, 
          not auto expansions and collapses,
          so it makes certain that the AutoExpanded attribute
          for the selected node is false.
          */
        {
          TreePath selectedTreePath= getLeadSelectionPath();
          if  // Expanding or collapsing it, whichever makes sense.
            (isExpanded( selectedTreePath ))  // Node is expanded now.
          	collapsePath(selectedTreePath);  // Collapsing it.
            else  // Node is collapsed now.
            expandPath(selectedTreePath);  // Expanding the row.
          setAutoExpandedV(  // Forcing auto-expanded status...
          		selectedTreePath , // ...for selected path...
              false  // ... to be off.
              );
          aTreeHelper.notifyListenersAboutChangeV( );
          }

    /* Listener methods and their helpers.  
      Listeners respond to state changes or externals.
      */

      /* TreeModelListener methods. Most do nothing.
        Only treeNodesInserted(..) does anything.
        It checks whether an insertion
        */

		    public void treeStructureChanged(TreeModelEvent theTreeModelEvent) { }
		
		    public void treeNodesRemoved(TreeModelEvent theTreeModelEvent) { }
		
		    public void treeNodesInserted(TreeModelEvent theTreeModelEvent)
		      /* This method checks whether an inserted node was selected before,
		        and is a descendant of the presently selected node.
		        If it is then it is selected.
		        The purpose of this is to automatically select and display a node
		        which was selected and being displayed when the app shutdown.
		        */
		      {
		    	  processingEvent: {
			    		//appLogger.debug("RootJTree.treeNodesInserted(..)");
          	  TreePath parentTreePath= theTreeModelEvent.getTreePath();
			    		if ( // Ignoring event if event's parent path isn't selected path. 
			    	      	!aTreeHelper.getPartTreePath().equals(parentTreePath)
			    	      	)
			    	  	break processingEvent; // Ignoring.
		          for 
			          ( Object childObject: theTreeModelEvent.getChildren() )
		          	{ // Selecting child if is meets requirements.
		          	  DataNode childDataNode= (DataNode)childObject;
		          	  TreePath childTreePath= 
		          	  		parentTreePath.pathByAddingChild(childDataNode);
		          	  PathAttributeMetaTool childPathAttributeMetaTool=
		          	    theMetaRoot.makePathAttributeMetaTool( 
		          	    		childTreePath, MetaRoot.selectionAttributeString 
		                  );
		          	  String valueString= // Getting selection path attribute value. 
		          	  		(String)childPathAttributeMetaTool.get();
		          	  if ( "IS".equals(valueString) )
		                getTreeHelper()  // In TreeHelper...
		        	        .setPartTreePathB(  // ...select...
		        	          childTreePath  // ...child tree node.
		        	          );
		          		}
		    			}
		      	}
		
		    public void treeNodesChanged(TreeModelEvent theTreeModelEvent) { }

      /* TreeSelectionListener method  and its numerous exclusive helpers.
        There is a lot of code here.
        The purpose of a lot of it is to do
        automatic expansion, collapsing, and selection of nodes,
        and to show the user what is happening. 
        */
      
        public void valueChanged( TreeSelectionEvent theTreeSelectionEvent ) 
          /* This TreeSelectionListener method processes 
            TreeSelectionEvent-s from the JTree superclass.  
            After performing a validity check it does:
            * Subselection processing such as automatic expanding, 
              collapsing, and selecting of JTree nodes.
            * Trivial translating of the JTree path to the TreeHelper path.
            */
          {
            dbgV("RootJTree.valueChanged(..) Begin");
            final TreePath finalNewTreePath=  // Get selection TreePath...
              theTreeSelectionEvent.  // ...which is the event's...
                getNewLeadSelectionPath();  // ...one and only TreePath.
            dbgV("RootJTree.valueChanged(..) finalNewTreePath",finalNewTreePath);
            if // Process based on whether selection path is null.
              ( finalNewTreePath == null ) // Selection path is null.
              ;  // So do nothing.
              else  // Selection path is not null.
              { // Process non-null JTree path.
                setupAndDoSubselectionsV(  // Doing subselections to get to...
                  finalNewTreePath  // ...the new TreePath.
                  );
                aTreeHelper.setPartTreePathB(  // Informing TreeHelper...
                  finalNewTreePath  // ...of JTree selection, a trivial case.
                  );
                } // Process non-null JTree path.
            dbgV("RootJTree.valueChanged(..) End");
            Misc.dbgEventDone(); // for Debug.
            }

        private void setupAndDoSubselectionsV(TreePath finalNewTreePath)
          /* This sets up and then does all subselections
            associated with the new TreePath finalNewTreePath.
            It records the TreePath of the final selection for use in
            the next round of subselections.
            It records position and other information in the MetaTool tree.
            */
          { 
            TreePath oldTreePath= // Set old TreePath to...
              savedTreePath;  // ...TreePath of last selection made.
            if ( oldTreePath == null )  // If oldTreePath is null...
              oldTreePath= finalNewTreePath;  // ...simulate no change.
            final TreePath finalOldTreePath= oldTreePath;  // for Runnable().
            savedTreePath=  // Save the new selected TreePath...
              finalNewTreePath; // which was calculated previously.
            subselectionsBeginV(   // Mark beginning of subselection...
              finalOldTreePath, // ...from the previous selection...
              finalNewTreePath  // ...to the new selection...
              );
            // At this point a new selection may be triggered.
            doSubselectionsV( // Collapse and expand nodes along path...
              finalOldTreePath, // ...from the previous selection...
              finalNewTreePath  // ...to the new selection...
              ) ;  // ...and maybe trigger an auto-expansion selection.
            theMetaRoot.set(finalNewTreePath); // Record final selection position.
            subselectionsEndV();  // Mark end of windows changes.
            }

        private void doSubselectionsV
          ( TreePath startTreePath, TreePath stopTreePath) 
          /* This processes a recent change in Selection TreePath,
            a change from startTreePath to stopTreePath.

            It does this by calling doSubselectionsRawV(.)
            with all the TreeSelectionListeners temporarilly disabled,
            because doSubselectionsRawV(..) makes a lot of selection changes
            for cosmetic reasons, and those listeners are interested in only
            the TreePath of the final selection.
            */
          { // doSubselectionsV(.)
            TreeSelectionListener[] TreeSelectionListeners= // Get listeners.
              getTreeSelectionListeners();
            for // disable all TreeSelectionListener-s by removing them.
              ( TreeSelectionListener ATreeSelectionListener : 
                TreeSelectionListeners 
                )
              removeTreeSelectionListener( ATreeSelectionListener );

            doSubselectionsRawV( // Collapse and expand along path...
              startTreePath, // ...from the previous selection...
              stopTreePath  // ...to the new selection.
              ) ;

            for // re-enable all TreeSelectionListener by adding them back.
              ( TreeSelectionListener ATreeSelectionListener : 
                TreeSelectionListeners 
                )
              addTreeSelectionListener( ATreeSelectionListener );
            } // doSubselectionsV(.)

        private void doSubselectionsRawV
          ( TreePath startTreePath, TreePath stopTreePath) 
          /* This processes a change in JTree Selection TreePath
            from startTreePath to stopTreePath.
            It auto-collapses auto-expanded nodes up
            from the startTreePath node to the common ancestor node,
            and it auto-expands nodes down from the common ancestor node 
            down to the node at stopTreePath.
            It records DataNode visit information along the way
            in the static class Selection.

            This method must be wrapped by doSubselectionsV(.) 
            to disable Tree-SelectionListener-s because 
            they have no interest in the many intermediate selections
            which happen during the execution of this method.
            */
          {
            dbgV("RootJTree.doSubselectionsRawV(..) startTreePath",startTreePath);
            dbgV("RootJTree.doSubselectionsRawV(..) stopTreePath",stopTreePath);
            setSelectionPath(startTreePath);  // Reselect start node for animation.
            TreePath CommonAncestorTreePath= // Do the up part.
              collapseAndExpandUpTreePath( startTreePath, stopTreePath );
            { // Expand downward if needed.
              if // Common node is a descendant of (the same as) stop node.
                ( stopTreePath.isDescendant( CommonAncestorTreePath ) )
                { // Set auto-expanded attribute of stop node to false.
                  dbgV("RootJTree.doSubselectionsRawV(..) at stop node");
                  setAutoExpandedV(  // Set auto-expanded attribute...
                    stopTreePath, false  // ...of stop node to false.
                    );
                  } // Set auto-expanded attribute of stop node to false.
              else // Common node is NOT a descendant of (same as) stop node.
              { // Expand downward.
                dbgV("RootJTree.doSubselectionsRawV(..) calling collapseAndExpandDownV(..)");
                dbgV("RootJTree.doSubselectionsRawV(..) CommonAncestorTreePath",CommonAncestorTreePath);
                dbgV("RootJTree.doSubselectionsRawV(..) stopTreePath",stopTreePath);
                collapseAndExpandDownV(  // Expand down...
                  CommonAncestorTreePath,  // ...from the common anestor...
                  stopTreePath  // ...to the stop node.
                  );
                } // Expand downward.
              } // Expand downward if needed.
            // We are at the stop node.  Now trigger possible auto-expansion there.
            trySelectingToAutoExpandV( stopTreePath );
            }

        private void trySelectingToAutoExpandV( TreePath stopTreePath )
          /* This method tries to trigger a new selection to auto-expand
            the node at stopTreePath.
            It follows the trail of most recently visited descendants
            which have their "AutoExpanded" attribute set.
            If there are no nodes in the trail then it simply returns.
            If there is at least one node in the trail,
            not including the first one,
            it queues a selection in the JTree of the last node in the trail.
            This is the first node encountered without the attribute set.
            The expansion will be handled later by that selection of that node.
            The expansion is done by this 2nd selection so that
            all the TreeSelectionListener-s are called
            for the present selection before being called again for the new one.
            */
          {
            final TreePath TrailEndTreePath= // Calculate end of trail of...
              getAutoExpandedTreePath( // ...expandable nodes...
                stopTreePath  // ...starting at stopTreePath.
                );
            if ( TrailEndTreePath != null )  // If there is an expansion trail...
              { // Trigger the trail's expansion.
                paintSelectionIfChangedV( );  // Display present selection...
                animationDelayDoMaybeV( );  // and delay now for visual smoothness.
                SwingUtilities.invokeLater(new Runnable() { // Queue GUI event...
                  @Override  
                  public void run() 
                    {  
                      setSelectionPath(   // ...to autoexpand by selecting...
                        TrailEndTreePath );  // ...last node of expansion trail.
                      }  
                  });
                } // Trigger the trail's expansion.
            }

        private TreePath collapseAndExpandUpTreePath
          ( TreePath startTreePath, TreePath stopTreePath) 
          /* This method processes only the upward, toward the root, 
            change in Selection TreePath from startTreePath to stopTreePath.
            It auto-collapses reached nodes if needed.
            It displays each selected node also as it goes if desired.
            It stops when it reaches a node which is an ancestor of 
            the node named by stopTreePath.
            It returns the TreePath of this common ancestor.
            */
          { // collapseAndExpandUpTreePath(.)
            dbgV("RootJTree.collapseAndExpandUpTreePath(..) startTreePath", startTreePath);
            dbgV("RootJTree.collapseAndExpandUpTreePath(..) stopTreePath", stopTreePath);
            TreePath scanTreePath= startTreePath;  // Prepare up-scan.
            while  // Process tree positions up to the common ancestor.
              ( ! scanTreePath.isDescendant( stopTreePath ) )
              { // Move and maybe collapse one position toward root.
                scanTreePath=   // Move one position toward root.
                  scanTreePath.getParentPath();
                dbgV("RootJTree.collapseAndExpandUpTreePath(..) selecct",scanTreePath);
                subselectV( scanTreePath );
                if // Auto-collapse this node if...
                  ( ( // ... it was auto-expanded. 
                      getAutoExpandedB( scanTreePath )
                      ) &&  // ...and...
                    ( // ...not the top of an inverted-V.
                      ! scanTreePath.isDescendant( stopTreePath ) 
                      || scanTreePath.equals( stopTreePath )
                      )  
                    )
                  { // Collapse.
                    dbgV(
                      "RootJTree.collapseAndExpandUpTreePath(..) collapse",
                      scanTreePath
                      );
                    collapseV( scanTreePath );
                    // Notice that the AutoExpandedB attribute is not cleared.
                    } // Collapse.
                } // Move and maybe collapse one position toward root.
            dbgV("RootJTree.collapseAndExpandUpTreePath(..) common");
            return scanTreePath;  // return the final common ancestor TreePath.
            } // collapseAndExpandUpTreePath(.)

        private void collapseAndExpandDownV
          ( TreePath startTreePath, TreePath stopTreePath ) 
          /* This recursive method processes a change in Selection TreePath
            from startTreePath to stopTreePath.
            It assumes stopTreePath is a descendant of startTreePath.
            It records DataNode visit information.
            It does auto-expansion of nodes if needed 
            on the way to stopTreePath.
            It displays each selected node also as it goes if desired.
            It recursively calls itself to process the lower nodes. 
            */
          { // collapseAndExpandDownV(.)
            TreePath stopParentTreePath= stopTreePath.getParentPath();
            boolean atTopLevelB=  // Determining whether recursion needed.
              ( stopParentTreePath.equals( startTreePath ) );
            if  // Recursively processing lower nodes first.
              ( ! atTopLevelB ) // There are such nodes.
              collapseAndExpandDownV(  // Recursively processing them.
                startTreePath, stopParentTreePath
                );
            dbgV("RootJTree.collapseAndExpandDownV(..) stopParentTreePath",stopParentTreePath);
            dbgV("RootJTree.collapseAndExpandDownV(..) isCollapsed(..): "+isCollapsed(stopParentTreePath));
            if // Auto-expanding Parent node if it is presently collapsed. 
              ( ! isExpanded( stopParentTreePath ) )
              {
                dbgV("RootJTree.collapseAndExpandDownV(..) expanding");
                subselectV( stopParentTreePath );
                expandV( stopParentTreePath );
                setAutoExpandedV(  // Set auto-expanded status.
                  stopParentTreePath, true
                  );
                }
            dbgV("RootJTree.collapseAndExpandDownV(..) stopTreePath",stopTreePath);
            subselectV( stopTreePath );
            } // collapseAndExpandDownV(.)

        // The following 4 methods came from old TreeExpansion class.

        private BooleanAttributeMetaTool 
        newAutoExpandedBooleanAttributeMetaTool( TreePath InTreePath )
          /* This method returns a BooleanAttributeMetaTool 
            that's ready to use for accessing the "AutoExpanded" attribute 
            in the MetaNode associated with InTreePath.  
            */
          { 
            return theMetaRoot.makeBooleanAttributeMetaTool(
              InTreePath, autoExpandedAttributeString 
              ); 
            }

        public void setAutoExpandedV( 
            TreePath InTreePath, boolean inAutoExpandedB 
            )
          /* This method stores inAutoExpandedB
            as the boolean value of the AutoExpanded attribute
            of the MetaNode associated with InTreePath.
            */
          { 
            BooleanAttributeMetaTool theBooleanAttributeMetaTool=
              newAutoExpandedBooleanAttributeMetaTool( InTreePath );
            theBooleanAttributeMetaTool.putAttributeB( 
              inAutoExpandedB 
              );
            }

        public boolean getAutoExpandedB( TreePath InTreePath )
          /* This method returns the boolean value of 
            the AutoExpanded attribute
            of the MetaNode associated with InTreePath.
            */
          { 
            BooleanAttributeMetaTool theBooleanAttributeMetaTool=
              newAutoExpandedBooleanAttributeMetaTool( InTreePath );
            return theBooleanAttributeMetaTool.getAttributeB( );
            }

        public TreePath getAutoExpandedTreePath( 
            TreePath startTreePath 
            )
          /* This method tries to follow a chain of 
            the most recently selected and AutoExpanded MetaNodes
            starting with the MetaNode associated with startTreePath
            and moving away from the root.
            It returns:
              The TreePath associated with the first MetaNode 
              without the AutoExpanded attribute set, or 
              
              Null if there were no AutoExpanded MetaNodes at all.
            */
          {
            TreePath scanTreePath=  // Initialize TreePath scanner to be...
              startTreePath;  // ...the start TreePath.
            BooleanAttributeMetaTool scanBooleanAttributeMetaTool=
              newAutoExpandedBooleanAttributeMetaTool( startTreePath );
            while (true) // Follow chain of nodes with AutoExpanded attribute set.
              { // Try to process one node.
                if  // Exit loop if AutoExpanded attribute of MetaNode not set.
                  ( ! scanBooleanAttributeMetaTool.getAttributeB( ) )
                  break;  // Exit loop.  We're past the last AutoExpanded node.
                MetaNode childMetaNode=  // Get recently selected child MetaNode.
                	theMetaRoot.getLastSelectedChildMetaNode(
                    scanBooleanAttributeMetaTool.getMetaNode()
                    );
                if ( childMetaNode == null ) // Exiting loop if no such child.
                  break;  // Exit loop.  Meta data is corrupted.
                if // Exiting loop if child MetaNode contains UnknownDataNode. 
                  ( childMetaNode.eliminateAndTestForUnknownDataNodeB(
                		scanBooleanAttributeMetaTool
                		) )
                	break;
                Object childOfDataNode=  // Get associated child DataNode.
                  childMetaNode.getDataNode();
                // Setup next possible iteration,
                scanTreePath=  // create scanTreePath of next node...
                  scanTreePath.pathByAddingChild( // ...by adding to old path...
                    childOfDataNode);  // ...the child DataNode.
                scanBooleanAttributeMetaTool.syncV( // Sync the MetaTool with...
                  scanTreePath );  // ...the new scanTreePath.
                } // Try to process one node.
            if  // Adjust result for special case of not moving at all.
              ( scanTreePath == startTreePath ) // If we haven't moved...
              scanTreePath=  null;  // ...replace scanTreePath with null.
            return scanTreePath;  // Return final scanTreePath as result.
            }

      // ExpansionListener methods and its helper.
      
        @Override
        public void treeExpanded(TreeExpansionEvent e) {
        	setTreeExpansionV( e, true );
        	}

        @Override
        public void treeCollapsed(TreeExpansionEvent e) {
        	setTreeExpansionV( e, false );
        	}

        private void setTreeExpansionV(
        		TreeExpansionEvent theTreeExpansionEvent, boolean valueB
        		)
          /* This helper method stores valueB
            as the boolean value of the Expanded attribute
            of the MetaNode associated with InTreePath.
            */
          { 
	          TreePath theTreePath= theTreeExpansionEvent.getPath();
            BooleanAttributeMetaTool theBooleanAttributeMetaTool=
          		theMetaRoot.makeBooleanAttributeMetaTool(
            		theTreePath, "Expanded" 
                ); 
            theBooleanAttributeMetaTool.putAttributeB( 
            	valueB
              );
            }

      // KeyListener methods.
      
        @Override
        public void keyPressed(KeyEvent TheKeyEvent) 
	        /* This KeyListener method is used to intercept and to do 
	          non-standard processing of some keys.
	          The keys are those which would normally cause
	          the expanding and collapsing of tree nodes.
	          They are intercepted here in order to implement 
	          auto-expand and auto-collapse.
	          */
	        { // keyPressed.
	          int KeyCodeI = TheKeyEvent.getKeyCode();  // cache key pressed.
	          
	          boolean KeyProcessedB= // Assume the key will be processed here. 
	            true;
	          { // try to process the key event.
	            if      (KeyCodeI == KeyEvent.VK_ENTER)  // Enter key.
	              commandExpandOrCollapseV();  // expand or collapse node.
	            else if (KeyCodeI == KeyEvent.VK_LEFT)  // left-arrow key.
	              aTreeHelper.commandGoToParentB(true);  // go to parent folder.
	            else if (KeyCodeI == KeyEvent.VK_RIGHT)  // right-arrow key.
	              aTreeHelper.commandGoToChildB(true);  // go to child folder.
	            //else if (KeyCodeI == KeyEvent.VK_X)  // X-key. For debugging.
	            //  Misc.dbgEventDone(); // Debug.
	            else
	              KeyProcessedB= false;
	            } // try to process the key event.
	          if // Post process key,... 
	            (KeyProcessedB)  // ...if it was processed earlier.
	            { // Post process key.
	              TheKeyEvent.consume();  // ...prevent more processing of it.
	              Misc.dbgEventDone(); // Debug.
	              } // Post process key.
	          } // keyPressed.

        @Override
        public void keyReleased(KeyEvent TheKeyEvent) { }  // Does nothing.
        
        @Override
        public void keyTyped(KeyEvent TheKeyEvent) { }  // Does nothing.

    /* Display/paint code. */

      private void paintImmediately( )
        /* This method displays the present state of
          a displayable Component without having to wait
          for a normal Paint, which happens only just before
          the app is ready to process the next user input.
          It is used to display changes to a Component that
          are not the immediate result of user input.
          */
        { // paintImmediately( )
          paintImmediately(  // Paint immediately...
            getBounds( )  // ...the entire Component.
            );
          } // paintImmediately( )

    // Subselection (expand/collapse, show) code.

      /* This code receives inputs regarding JTree node selections, 
        expansions, and collapses, as well as 
        output option "show" flag values,
        and decides when to output the state of the JTree.
        Subselections are program-generated selections that happen
        in the processing of normal user-generated selections,
        when all TreeSelectionListener-s are disabled.
        */

      private TreePath subselectionTreePath= null;  // Previous selection.

      private boolean subselectionShowAllB= false;  // Show selection paths.

      private void subselectionsBeginV
        ( TreePath startTreePath, TreePath stopTreePath) 
        /* This method is called to begin a possible sequence of
          JTree subselections in the processing of 
          a selection change from startTreePath to stopTreePath.
          */
        {
          subselectionTreePath= startTreePath;  // Save previous selection.
          if ( isVisible( stopTreePath) )
            paintSelectionIfChangedV( );  // Paint the initial selection.
          }

      private void subselectionsEndV()
        /* This method is called to end a sequence of JTree sub-selections. */
        {
          paintSelectionIfChangedV(   // Display any pending selection...
            true  // ...and scroll into Viewport.
            );
          animationDelaySetRequestV( false );  // Disable the final delay.
          dbgV("RootJTree.subselectionsEndV()(..)",getSelectionPath());
          scrollPathToVisible( getSelectionPath() );
          paintImmediately();
          }

      private void subselectV( TreePath inTreePath )
        /* This method is called to change, and possibly show, now or later,
          the JTree node selection. 
          */
        { 
          setSelectionPath( inTreePath );  // Actually Select the JTree node.
          if ( subselectionShowAllB )  // If showing everything
            paintSelectionIfChangedV(   // paint selection in Viewport only.
              false 
              );
          }

      private void expandV( TreePath inTreePath )
        {
          paintSelectionIfChangedV(   // Paint selection in Viewport only.
            false 
            );
          expandPath( inTreePath );  // Expand node.
          paintV( inTreePath, false ); // Paint in Viewport only.
          }

      private void collapseV( TreePath inTreePath )
        {
          paintSelectionIfChangedV(   // Paint selection in Viewport only.
            false 
            );
          collapsePath( inTreePath );  // Collapse node.
          paintV( inTreePath, false ); // Paint in Viewport only.
          }

      private void paintSelectionIfChangedV( )
        /* This method is called to update the JTree display to
          show a previously saved selection, if needed.
          */
        {
          paintSelectionIfChangedV( true );
          }

      private void paintSelectionIfChangedV( boolean ScrollingIntoViewOkayB )
        /* This method is called to update the JTree display to
          show a previously saved selection, if needed.

          If ScrollingIntoViewOkayB is true then a changed selection
          will be scrolled into the Viewport if necessary.
          If ScrollingIntoViewOkayB is false then a changed selection
          will be painted only if it already in the Viewport.
          */
        {
          TreePath presentTreePath= getSelectionPath();  // Get present selection.
          if  // Selection has not changed since last saved.
            ( subselectionTreePath.equals(presentTreePath) )
            ;  // So do nothing.
            else // Selection has changed.
            { // Display the new selection.
              paintV( presentTreePath, ScrollingIntoViewOkayB );
              subselectionTreePath= presentTreePath;  // Update selection copy.
              } // Display the new selection.
          }

      private void paintV( TreePath inTreePath, boolean ScrollingIntoViewOkayB )
        /* This method paints the display but only if 
          the tree node identified by inTreePath is
          visible in theJScrollPane.

          If ScrollingIntoViewOkayB is true then the node at inTreePath
          will be scrolled into the Viewport if necessary.
          If ScrollingIntoViewOkayB is false then the node at inTreePath
          will be painted only if it already in the Viewport.
          */
        {
          if   // Paint conditionally.
            ( ScrollingIntoViewOkayB  // The node may be outside of Viewport...
              || inViewportB( inTreePath )   // ...or the node is inside.
              )
            paintAfterScrollingV( inTreePath );  // ...paint.
          }

      private void paintAfterScrollingV( TreePath inTreePath )
        /* This method scrolls the node at inTreePath into view,
          paints the display to show that node and any other changes,
          and requests an animation delay to follow so that
          the user can see what has changed.
          */
        {
          scrollPathToVisible(  // Scroll node completely into view.
            inTreePath 
            );
          animationDelayDoMaybeV( );  // Config if requested earlier.
          paintImmediately( );  // Display present JTree state.
          animationDelaySetRequestV( true );  // Request new paint delay.
          }

      private boolean inViewportB( TreePath inTreePath )
        /* This method tests whether the node identified by inTreePath
          is in the Viewport of the JTree's JScrollPane.
          This is used to reduce the amount of painting needed
          to keep the user informed about what's going on
          to approximately one screenfull of output.
          */
        {
          boolean resultB= false;  // Assume result is false.

          toReturn: {

            if ( ! isVisible( inTreePath ) )  // JTree node is hidden.
              break toReturn;  ;  // Return default of false.

            Rectangle nodeRectangle=  // Calculate JTree node rectangle.
                getPathBounds( inTreePath ); // Might be null.
            if ( nodeRectangle == null )
              break toReturn;  ;  // Return default of false.
            Rectangle viewportRectangle=  // Calculate scroller rectangle.
              theJScrollPane.getViewport().getViewRect();
            if  // The two rectangles do not intersect. 
              ( ! nodeRectangle.intersects (viewportRectangle) )
              break toReturn;  ;  // Return default of false.
              
            resultB= true;  // Override result with true.

          } // toReturn:
            return resultB;

          }

    // Animation Config manager.

      /* This manages the creation of short delays to be used between
        window paints to animate complex changes to the window state.
        */

      private int animationDelayI= 50;  // != 0 means milliseconds to delay.

      private boolean animationDelayRequestedB= false;  // Request flag.

      private void animationDelaySetRequestV( boolean inB )
        /* This sets or resets an animation delay request by storing a flag.  */
        { 
          animationDelayRequestedB= inB;  // Store new value.
          }

      private void animationDelayDoMaybeV( )
        /* This delays thread execution if a delay has been requested,
          and then clears the request so there won't be another delay
          unless and until another delay has been requested.
          */
        { 
          if ( animationDelayRequestedB )
            { 
              animationDelayV( );
              animationDelaySetRequestV( false );
              }
          }

      private void animationDelayV( )
        /* This method sleeps for a standard animation interval.
          It is used for delays in multistep display changes.
          */
        {
          if ( animationDelayI != 0 )  // Config if desired.
            EpiThread.interruptibleSleepB( animationDelayI );
          }

    // Debugging logging code.  Much of this might eventually be deleted.

      public void collapsePath( TreePath inTreePath )
        /* This method is for debugging.  */
        {
          dbgV("RootJTree.collapsePath(..)",inTreePath);
          super.collapsePath( inTreePath );
          }

      public void expandPath( TreePath inTreePath )
        /* This method is for debugging.  */
        {
          dbgV("RootJTree.expandPath(..)",inTreePath);
          super.expandPath( inTreePath );
          }

      private boolean logB= false;  // false;  // false for no debug logging.

      private void dbgV( String inString, TreePath inTreePath )
        /* This method logs inString as the name of the caller,
          and the name of the last element of inTreePath.
          This is used for debugging.  
          */
        {
          if ( logB )

            appLogger.info( 
              inString 
              + ": "
              + inTreePath.getLastPathComponent()
              );
          }

      private void dbgV( String inString )
        /* This method logs inString as the name of the caller,
          and the name of the last element of the present selection path.
          This is used for debugging.  
          */
        {
          dbgV( 
            inString +  " SELECTION"
            , getSelectionPath()
            );
          }

    }
