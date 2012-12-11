package allClasses;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.SwingUtilities;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;

// import java.awt.*;

// import javax.swing.JPanel;


public class RootJTree 

  extends IJTree

  implements KeyListener, TreeSelectionListener

  /* This class is used for the content in the left subpanel.
    the only field is an IJTree.  why not just use IJTree?? */

  {

    // variables.
      private static final long serialVersionUID = 1L;
      // private IJTree TheIJTree;
      private RootTreeModel TheRootTreeModel;
      private TreePath SelectedTreePath;  /* TreePath of node selected in tree.
        this should be synchronized with the one in the JTree,
        and therefore might be redundant.  ??
        */
      private Object SelectedObject;  // Object of node selected in tree.  ??

    // constructors.

      public RootJTree( RootTreeModel InputTreeModel ) 
        { // constructor.
          super( InputTreeModel );  // Construct the superclass.
          
          TheRootTreeModel=   // save a local copy of the TreeModel.
            InputTreeModel;
          //TheIJTree.setLargeModel( true );        
          /*
          { // customize the tree cell rendering.
            DefaultTreeCellRenderer TheDefaultTreeCellRenderer = 
                new DefaultTreeCellRenderer();
            TheDefaultTreeCellRenderer.setBackgroundSelectionColor(Color.CYAN);
            setCellRenderer(TheDefaultTreeCellRenderer);
            } // customize the tree cell rendering.
          */
  
          collapseRow( 0 );  // collapse root which should be at row 0.
          setRootVisible( true );  // show the root.
          setShowsRootHandles( true );
          putClientProperty( "IJTree.lineStyle", "Angled" );

          addTreeSelectionListener(this);  // listen for tree selections.
          addKeyListener(this);  // listen for key presses.
          
          // setLayout( new BorderLayout() );
          // add( TheIJTree /*, BorderLayout.CENTER */ );

          } // constructor.

    // input methods.

      // KeyListener methods, for some user input from keyboard.
      
        @Override
        public void keyPressed(KeyEvent TheKeyEvent) 
        { // keyPressed.
          int KeyCodeI = TheKeyEvent.getKeyCode();  // cache key pressed.
          
          boolean KeyProcessedB= true;  // assume the key event will be processed here. 
          { // try to process the key event.
            if      (KeyCodeI == KeyEvent.VK_ENTER)  // Enter key.
              CommandExpandOrCollapseV();  // expand or collapse node.
            else if (KeyCodeI == KeyEvent.VK_LEFT)  // left-arrow key.
              CommandGoToParentV();  // go to parent folder.
            else if (KeyCodeI == KeyEvent.VK_RIGHT)  // right-arrow key.
              CommandGoToChildV();  // go to child folder.
            else
              KeyProcessedB= false;
            } // try to process the key event.
          if (KeyProcessedB)  // if the key event was processed...
            TheKeyEvent.consume();  // ... prevent further processing of it.
                  
          } // keyPressed.

        @Override
        public void keyReleased(KeyEvent TheKeyEvent) { }  // unused part of KeyListener interface.
        
        @Override
        public void keyTyped(KeyEvent TheKeyEvent) { }  // unused part of KeyListener interface.
        
      // TreeSelectionListener methods, for when JTree selection changes.
        
        public void valueChanged( TreeSelectionEvent TheTreeSelectionEvent ) 
          /* This processes TreeSelectionEvent-s from the JTree superclass.
            It records information about the selection.
            It checks for and performs any needed
            automatic expanding or collapsing of JTree nodes.
            It records position and other information in the DagInfo tree.
            */
          { // valueChanged( TreeSelectionEvent TheTreeSelectionEvent )
            final TreePath FinalNewTreePath=   // Get the destination TreePath which is...
              TheTreeSelectionEvent.  // ...the TreeSelectionEvent's...
              getNewLeadSelectionPath();  // ...one and only TreePath.
            if ( FinalNewTreePath == null ) // process TreePath based on null-hood.
              ;  // ignore null TreePath.
              else
              { // process non-null selection TreePath.
                TreePath OldTreePath= 
                  SelectedTreePath;  // Get old TreePath.
                if ( OldTreePath == null )  // Don't let OldTreePath be null...
                  OldTreePath= FinalNewTreePath;  // ...simulating null selection change.
                final TreePath FinalOldTreePath= OldTreePath;  // for Runnable().
                SelectedTreePath=  // store the selected TreePath from...
                  FinalNewTreePath; // which was calculated previously.
                SelectedObject=  // store the last component as selected Object.
                  SelectedTreePath.getLastPathComponent();

                // SwingUtilities.invokeLater(.) can't be used because
                // node is already expanded.
                
                //boolean wasCollapsedB= isCollapsed( OldTreePath );
                if ( isVisible( SelectedTreePath ) )  // if new path is visible...
                  {
                    scrollPathToVisible( SelectedTreePath );  // ...make it visible.
                    MyPaintImmediately( );  // show progress.
                    //System.out.print( " RootJTree.valueChanged( ).scrollPathToVisible()");
                    }
                
                CollapseAndExpand( // Collapse and expand nodes along path...
                  FinalOldTreePath, // ...from the previous selection...
                  FinalNewTreePath  // ...to the new selection.
                  ) ;
                } // process non-null selection TreePath.
            } // valueChanged( TreeSelectionEvent TheTreeSelectionEvent )
        
        private void CollapseAndExpand
          ( TreePath StartTreePath, TreePath StopTreePath) 
          /* This processes a recent change in Selection TreePath,
            a change from StartTreePath to StopTreePath.
            
            It does this by calling CollapseAndExpandRaw(.)
            but wraps this call in code to insulate the app
            from any JTree node selections that might cause.
            */
          { // CollapseAndExpand(.)
            TreeSelectionListener[] TreeSelectionListeners= // get listeners.
              getTreeSelectionListeners();
            for // disable all TreeSelectionListener by removing them.
              ( TreeSelectionListener ATreeSelectionListener : 
                TreeSelectionListeners 
                )
              removeTreeSelectionListener( ATreeSelectionListener );

            CollapseAndExpandRaw( // Collapse and expand along path...
              StartTreePath, // ...from the previous selection...
              StopTreePath  // ...to the new selection.
              ) ;

            SelectNodeV(  // reselect...
              StopTreePath   // ...original selection TreePath because...
              );  // ...CollapseAndExpandRaw() might have changed it.
            for // re-enable all TreeSelectionListener by adding them back.
              ( TreeSelectionListener ATreeSelectionListener : 
                TreeSelectionListeners 
                )
              addTreeSelectionListener( ATreeSelectionListener );
            } // CollapseAndExpand(.)

        private void CollapseAndExpandRaw
          ( TreePath StartTreePath, TreePath StopTreePath) 
          /* This processes a change in JTree Selection TreePath
            from StartTreePath to StopTreePath.
            It auto-collapses auto-expanded nodes up
            from the StartTreePath node to the common ancestor node,
            and it auto-expands nodes down
            from the common ancestor to the StopTreePath.
            It records DagNode visit information along the way.
            This method must be wrapped by CollapseAndExpandRaw(.) 
            because collapsing or expanding apparently causes selections.
            */
          { // CollapseAndExpandRaw(.)
            DagInfo.UpdatePath( StartTreePath );  // record visit information.
            TreePath CommonAncestorTreePath= // do the up part.
              CollapseAndExpandUpTreePath( StartTreePath, StopTreePath );
            { // expand downward if needed.
              if // scan node is a descendent of (same as) stop node.
                ( StopTreePath.isDescendant( CommonAncestorTreePath ) )
              	TreeExpansion.SetAutoExpanded(  // clear auto-expanded status of...
                  StopTreePath, false  // ...of stop node.
                  );
              else /// scan node is NOT a descendent of (same as) stop node.
                CollapseAndExpandDown(  // expand down to stop node.
                  CommonAncestorTreePath, 
                  StopTreePath 
                  );
              } // expand downward if needed.
            final TreePath AutoExpandTreePath= // destination auto-expandabe?
            	TreeExpansion.FollowAutoExpandToTreePath( StopTreePath );
            if ( AutoExpandTreePath != null )  // if so then...
              SwingUtilities.invokeLater(new Runnable() { // ...queue event...
                @Override  
                public void run() 
                  {  
                    //Misc.DbgOut( "RootJTree.CollapseAndExpandRaw( ).reselection");
                    setSelectionPath(   // ...to autoexpand by selecting...
                      AutoExpandTreePath );  // ...last node in expand path.
                    }  
                });
            } // CollapseAndExpandRaw(.)

        private TreePath CollapseAndExpandUpTreePath
          ( TreePath StartTreePath, TreePath StopTreePath) 
          /* This method processes only the upward, toward the root, 
            change in Selection TreePath from StartTreePath to StopTreePath.
            It auto-collapses reached nodes if needed.
            It stops when it reaches a node which is an ancestor of 
            the node named by StopTreePath.
            It returns the TreePath of this ancestor.
            */
          { // CollapseAndExpandUpTreePath(.)
            TreePath ScanTreePath= StartTreePath;  // prepare up-scan.
            while  // process tree positions up to the common ancestor.
              ( ! ScanTreePath.isDescendant( StopTreePath ) )
              { // move and process one position toward root.
                ScanTreePath=   // Move one position toward root.
                  ScanTreePath.getParentPath();
                if // auto-collapse this node if...
                  ( ( // ... it was auto-expanded. 
                  		TreeExpansion.GetAutoExpandedB( ScanTreePath ) 
                      ) &&  // ...and...
                    ( // ...not the top of an inverted-V.
                      ! ScanTreePath.isDescendant( StopTreePath ) ||
                      ScanTreePath.equals( StopTreePath )
                      )  
                    )
                  { // auto-collapse. 
                    collapsePath( ScanTreePath );  // collapse.
                    // Notice that the the AutoExpandedB status is not cleared.
                    //setSelectionPath( ScanTreePath );  // select node
                      // temporarilly also for visibility.
                    scrollPathToVisible( ScanTreePath );  // to show progress.
                    MyPaintAfterDelay( );  // show progress.
                    //System.out.print( "RootJTree.CollapseAndExpandUpTreePath(.)");
                    } // auto-collapse.
                } // move and process one position toward root.
            return ScanTreePath;  // return the final common ancestor TreePath.
            } // CollapseAndExpandUpTreePath(.)

        private void CollapseAndExpandDown
          ( TreePath StartTreePath, TreePath StopTreePath ) 
          /* This recursive method processes a change in Selection TreePath
            from StartTreePath to StopTreePath.
            It assumes StopTreePath is a descendant of StartTreePath.
            It records DagNode visit information.
            It does auto-expansion of nodes if needed 
            on the way to StopTreePath.
            */
          { // CollapseAndExpandDown(.)
            TreePath StopsParentTreePath= StopTreePath.getParentPath();
            boolean atTopLevelB=  // Determine whether recursion needed.
              ( StopsParentTreePath.equals( StartTreePath ) );
            if  // recursively process nodes farther from StopTreePath.
              ( ! atTopLevelB ) // there are such nodes.
              CollapseAndExpandDown(  // recursively process them.
                StartTreePath, StopsParentTreePath
                );
            if // auto-expand Parent node if it is presently collapsed. 
              ( isCollapsed( StopsParentTreePath ) )
              { // auto-expand. 
                expandPath( StopsParentTreePath );  // expand node.
                //setSelectionPath( StopsParentTreePath );  // select node
                  // temporarilly also for visibility.
                scrollPathToVisible( StopsParentTreePath );  // to show progress.
                TreeExpansion.SetAutoExpanded(  // set auto-expanded status.
                  StopsParentTreePath, true
                  );
                MyPaintAfterDelay( );  // show progress.
                //System.out.print( "RootJTree.CollapseAndExpandDown(.)");
                } // auto-expand.
            } // CollapseAndExpandDown(.)

    // output methods.
    
      public TreePath GetSelectedTreePath() 
        {
          return SelectedTreePath;
          }

    // other methods.

      public void CommandGoToChildV() 
        /* Goes to and displays the most recently visited child 
          of the present node.  If none of the children
          have been visited before then it goes to the first child.
          */
        { // CommandGoToChildV().
          Object ChildObject=  // try to get child...
            DagInfo.  // ...from the visits tree...
              UpdatePathDagNode( // ...most recently visited...
                SelectedTreePath 
                );  // ...of the tree node at end of selected TreePath.
              
          if (ChildObject == null)  // if no recent child try first one.
            { // try getting first ChildObject.
              if (TheRootTreeModel.getChildCount(SelectedObject) <= 0)  // there are no children.
                ChildObject= null;  // keep ChildObject null.
              else  // there are children.
                ChildObject= TheRootTreeModel.getChild(SelectedObject,0);  // get first ChildObject.
              } // get name of first child.

          if // act based on whether a usable Child found.
            (ChildObject!=null) // one was found.
            { // adjust visits tree and display directory.
              TreePath ChildTreePath=  // child path is...
                SelectedTreePath.   // ...old selected with...
                  pathByAddingChild(  // ...addition of...
                  ChildObject    // ...the found child object.
                  );
              SelectNodeV(  // select (and display)...
                ChildTreePath  // ...the new child path.
                );
              } // adjust visits tree and display directory.
          } // CommandGoToChildV().
  
      public void CommandGoToParentV() 
        /* Goes to and displays the parent of the present tree node. */
        { // CommandGoToParentV().
          TreePath ParentTreePath=  // try getting get parent TreePath.
          	SelectedTreePath.getParentPath();
          if (ParentTreePath != null)  // there is a parent.
            { // record visit then select and display parent node.
              // Object ParentObject=   // was not Parent!  fixed.
              //   ParentTreePath.getLastPathComponent();
              SelectNodeV(ParentTreePath);  // select (and display) the parent directory.
              } // record visit then select and display parent node.
          } // CommandGoToParentV().
  
      public void CommandGoDownV() 
        /* Goes to and displays the tree node in row below the present one. */
        { // CommandGoDownV().
          int RowI= getLeadSelectionRow( );  // Get # of selected row.
          TreePath NextTreePath=  // Convert next row to next TreePath.
          	getPathForRow( RowI + 1 );
          if (NextTreePath != null)  // Select that path if it exists.
            { // select and display the node.
              SelectNodeV(NextTreePath);  // select (and display) the path.
              } // select and display the node.
          } // CommandGoDownV().
  
      public void CommandGoUpV() 
        /* Goes to and displays the tree node in row above the present one. */
        { // CommandGoUpV().
          int RowI= getLeadSelectionRow( );  // Get # of selected row.
          TreePath NextTreePath=  // Convert next row to next TreePath.
          	getPathForRow( RowI - 1 );
          if (NextTreePath != null)  // Select that path if it exists.
            { // select and display the node.
              SelectNodeV(NextTreePath);  // select (and display) the path.
              } // select and display the node.
          } // CommandGoUpV().

      private void CommandExpandOrCollapseV()
        {
          int SelectedRowI=   // determine which row is selected.
            getLeadSelectionRow();
          if  // expand or collapse it, whichever makes sense.
            (isExpanded(SelectedRowI))
              { 
                collapseRow(SelectedRowI);  // collapse present node.
                TreeExpansion.SetAutoExpanded(  // force auto-expanded status...
                  getLeadSelectionPath() , // ...for selected path...
                  false  // ... to be off.
                  );
                }
            else
              expandRow(SelectedRowI);
          }

        public void SelectNodeV(TreePath InSelectionTreePath) 
          /* Programatically selects InSelectionTreePath in the JTree.
            This method is usually called as a result of a keyboard command.
            This will trigger any TreeSelectionListener valueChanged() methods,
            including one in this class, which will cache selection values.
            It also scrolls the newly selected TreePath into view in the JTree.
            */
          { // SelectNodeV()
            setSelectionPath(InSelectionTreePath);  // select in JTree.
              // This will trigger TreeSelectionListener activity.
            scrollPathToVisible(InSelectionTreePath);  // make it visible.
            } // SelectNodeV()

        private void MyPaintAfterDelay( )
          /* This, along with MyPaintImmediately(), is used to
            display a change in a Component without having to wait
            for a normal Paint, which happens only just before
            the app is ready to process the next user input.
            */
          { // MyPaintAfterDelay( )
            try {
              Thread.sleep( 50 );  // Pause for 0.2 second.
              } 
            catch(InterruptedException ex) {
              Thread.currentThread().interrupt();
              }
            MyPaintImmediately( );  // show progress.
            }// MyPaintAfterDelay( )

        private void MyPaintImmediately( )
          { // MyPaintImmediately( )
            //Misc.DbgOut( "RootJTree.MyPaintImmediately( ).");
            paintImmediately( getBounds( ) );  // show progress.
            } // MyPaintImmediately( )


    }
