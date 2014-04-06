package allClasses;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.SwingUtilities;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;

// import java.awt.*;

// import javax.swing.JPanel;

import static allClasses.Globals.*;  // appLogger;

public class RootJTree 

  extends IJTree

  implements KeyListener, TreeSelectionListener

  /* This class is used for the content in the left subpanel.
    the only field is an IJTree.  why not just use IJTree?? */

  {

    // Variables.
      private static final long serialVersionUID = 1L;
      // private IJTree theIJTree;
      private DataTreeModel theDataTreeModel;
      private TreePath savedTreePath;  // Previous selection.  This is...
        // ...for use as first argument to changeSelectionV(..)

    // Constructors.

      public RootJTree( DataTreeModel InputTreeModel ) 
        { // constructor.
          super( InputTreeModel );  // Construct the superclass.
          
          theDataTreeModel=   // save a local copy of the TreeModel.
            InputTreeModel;
          //theIJTree.setLargeModel( true );        
          /*
          { // customize the tree cell rendering.
            DefaultTreeCellRenderer TheDefaultTreeCellRenderer = 
                new DefaultTreeCellRenderer();
            TheDefaultTreeCellRenderer.setBackgroundSelectionColor(Color.CYAN);
            setCellRenderer(TheDefaultTreeCellRenderer);
            } // customize the tree cell rendering.
          */
  
          //expandPath( ..);  // Expand node.
          //collapseRow( 0 );  // collapse root which should be at row 0.
          expandRow( 0 );  // Expand root which should be at row 0, for descendents.
          setRootVisible( false );  // Do not show the pseudo-parent of root.
          setShowsRootHandles( true );
          putClientProperty( "IJTree.lineStyle", "Angled" );

          addTreeSelectionListener(this);  // listen for tree selections.
          addKeyListener(this);  // listen for key presses.
          
          // setLayout( new BorderLayout() );
          // add( theIJTree /*, BorderLayout.CENTER */ );

          } // constructor.

    // Input methods.

      public void selectNodeV(TreePath inSelectionTreePath ) 
        /* This method programmatically selects the node
          identified by inSelectionTreePath in the JTree.
          This method can be called as a result of a keyboard command
          or a TreeSelectionEvent in the right sub-panel.
          The selection will trigger any 
          TreeSelectionListener valueChanged() methods, 
          including the one in this class, 
          which will cause further processing.

          This could be replaced by setSelectionPath(TreePath)
          since that is all that is done now.

          ??? If the node is viewable, meaning not collapsed,
          then it also scrolls the node into view 
          in the JTree and paints it.
          */
        {
          setSelectionPath(inSelectionTreePath);  // Select in JTree.
            // This will trigger TreeSelectionListener activity.

          /*
          if  // if path is viewable then show it.
            ( isVisible( inSelectionTreePath ) )
            {
              scrollPathToVisible( inSelectionTreePath ); // Scroll it.
              paintImmediately( );  // Paint it to display.
              }
          */
          }

    // Output methods.  These return state information to the caller.
    
      public TreePath getSelectedTreePath()
        /* This method returns the selection TreePath.
          There should be only one, because multiselection is disabled.
          */
        {
          //return savedTreePath;
          return getSelectionPath();  // Get path directly from JTree.
          }

    /* Listener methods and their helpers.  
      Listeners respond to state changes or externals.
      */

      /* TreeSelectionListener methods  and their exclusive helpers.
        These listen for JTree selection changes.
        */
        
        public void valueChanged( TreeSelectionEvent TheTreeSelectionEvent ) 
          /* This method processes TreeSelectionEvent-s from the JTree superclass.
            It checks for and performs any needed
            automatic expanding, collapsing, and selecting of JTree nodes.
            It records position and other information in the MetaTool tree.
            It records the TreePath of the final selection for use in
            automatically collapsing and expanding the next time
            this method is called.

            ???? This method no longer does any scrolling or repainting
            to display the newly selected node because
            it was affecting the expansion state.
            */
          { // valueChanged( TreeSelectionEvent TheTreeSelectionEvent )
            logV("RootJTree.valueChanged(..) Begin");
            //Do not scroll for painting because that changes expansion state.
            final TreePath FinalNewTreePath=  // Get selection TreePath...
              TheTreeSelectionEvent.  // ...which is the event's...
                getNewLeadSelectionPath();  // ...one and only TreePath.
            logV("RootJTree.valueChanged(..) FinalNewTreePath",FinalNewTreePath);
            if // Process based on whether selection path is null.
              ( FinalNewTreePath == null ) // Selection path is null.
              ;  // So do nothing.
              else  // Selection path is not null.
              { // Process non-null selection TreePath.
                changesBeginV();  // Mark beginning of windows changes.
                TreePath OldTreePath= // Set old TreePath to...
                  savedTreePath;  // ...TreePath of last selection made.
                if ( OldTreePath == null )  // If OldTreePath is null...
                  OldTreePath= FinalNewTreePath;  // ...simulate no change.
                final TreePath FinalOldTreePath= OldTreePath;  // for Runnable().
                savedTreePath=  // Save the new selected TreePath...
                  FinalNewTreePath; // which was calculated previously.
                // At this point a new selection may be triggered.
                
                changeSelectionV( // Collapse and expand nodes along path...
                  FinalOldTreePath, // ...from the previous selection...
                  FinalNewTreePath  // ...to the new selection...
                  ) ;  // ...and maybe trigger an auto-expansion selection.
                Selection.set(FinalNewTreePath); // Record final selection position.
                changesEndV();  // Mark end of windows changes.
                } // Process non-null selection TreePath.

            logV("RootJTree.valueChanged(..) End");
            Misc.dbgEventDone(); // ??? Debug.
            } // valueChanged( TreeSelectionEvent TheTreeSelectionEvent )
        
        private void changeSelectionV
          ( TreePath startTreePath, TreePath stopTreePath) 
          /* This processes a recent change in Selection TreePath,
            a change from startTreePath to stopTreePath.
            
            It does this by calling changeSelectionRawV(.)
            with all the TreeSelectionListeners temporarilly disabled,
            because changeSelectionRawV(..) makes a lot of selection changes
            for cosmetic reasons, and those listeners are interested in only
            the TreePath of the final selection.

            ??? Maybe it is only this class which is not interested,
            not all Listeners???
            */
          { // changeSelectionV(.)
            TreeSelectionListener[] TreeSelectionListeners= // Get listeners.
              getTreeSelectionListeners();
            for // disable all TreeSelectionListener-s by removing them.
              ( TreeSelectionListener ATreeSelectionListener : 
                TreeSelectionListeners 
                )
              removeTreeSelectionListener( ATreeSelectionListener );

            changeSelectionRawV( // Collapse and expand along path...
              startTreePath, // ...from the previous selection...
              stopTreePath  // ...to the new selection.
              ) ;

            for // re-enable all TreeSelectionListener by adding them back.
              ( TreeSelectionListener ATreeSelectionListener : 
                TreeSelectionListeners 
                )
              addTreeSelectionListener( ATreeSelectionListener );
            } // changeSelectionV(.)

        private void changeSelectionRawV
          ( TreePath startTreePath, TreePath stopTreePath) 
          /* This processes a change in JTree Selection TreePath
            from startTreePath to stopTreePath.
            It auto-collapses auto-expanded nodes up
            from the startTreePath node to the common ancestor node,
            and it auto-expands nodes down from the common ancestor node 
            down to the node at stopTreePath.
            It records DataNode visit information along the way
            in the static class Selection.

            This method must be wrapped by changeSelectionV(.) 
            to disable TreeSelectionListener-s because 
            they have no interest in the many intermediate selections
            which happen during the execution of this method.
            */
          {
            logV("RootJTree.changeSelectionRawV(..) startTreePath",startTreePath);
            logV("RootJTree.changeSelectionRawV(..) stopTreePath",stopTreePath);
            setSelectionPath(startTreePath);  // Reselect start node for animation.
            TreePath CommonAncestorTreePath= // Do the up part.
              collapseAndExpandUpTreePath( startTreePath, stopTreePath );
            { // expand downward if needed.
              if // Common node is a descendent of (the same as) stop node.
                ( stopTreePath.isDescendant( CommonAncestorTreePath ) )
                {
                  logV("RootJTree.changeSelectionRawV(..) at stop node");
                  TreeExpansion.SetAutoExpanded(  // Set auto-expanded...
                    stopTreePath, false  // ...of stop node to false.
                    );
                  }
              else // scan node is NOT a descendent of (same as) stop node.
              {
                logV("RootJTree.changeSelectionRawV(..) calling collapseAndExpandDownV(..)");
                logV("RootJTree.changeSelectionRawV(..) CommonAncestorTreePath",CommonAncestorTreePath);
                logV("RootJTree.changeSelectionRawV(..) stopTreePath",stopTreePath);
                collapseAndExpandDownV(  // expand down to stop node.
                  CommonAncestorTreePath, 
                  stopTreePath 
                  );
                }
              } // expand downward if needed.
            // We are at the stop node.  Now we do possible auto-expansion.
            final TreePath AutoExpandTreePath= // Is stop node auto-expandable?
            	TreeExpansion.FollowAutoExpandToTreePath( stopTreePath );
            if ( AutoExpandTreePath != null )  // If yes then...
              SwingUtilities.invokeLater(new Runnable() { // ...queue event...
                @Override  
                public void run() 
                  {  
                    setSelectionPath(   // ...to autoexpand by selecting...
                      AutoExpandTreePath );  // ...last node of expand path.
                    }  
                });
            }

        private TreePath collapseAndExpandUpTreePath
          ( TreePath startTreePath, TreePath stopTreePath) 
          /* This method processes only the upward, toward the root, 
            change in Selection TreePath from startTreePath to stopTreePath.
            It auto-collapses reached nodes if needed.
            It displays each node as it goes.
            It stops when it reaches a node which is an ancestor of 
            the node named by stopTreePath.
            It returns the TreePath of this common ancestor.
            */
          { // collapseAndExpandUpTreePath(.)
            logV("RootJTree.collapseAndExpandUpTreePath(..) startTreePath", startTreePath);
            logV("RootJTree.collapseAndExpandUpTreePath(..) stopTreePath", stopTreePath);
            TreePath ScanTreePath= startTreePath;  // Prepare up-scan.
            while  // Process tree positions up to the common ancestor.
              ( ! ScanTreePath.isDescendant( stopTreePath ) )
              { // Move and maybe collapse one position toward root.
                ScanTreePath=   // Move one position toward root.
                  ScanTreePath.getParentPath();
                if // Auto-collapse this node if...
                  ( ( // ... it was auto-expanded. 
                  		TreeExpansion.GetAutoExpandedB( ScanTreePath ) 
                      ) &&  // ...and...
                    ( // ...not the top of an inverted-V.
                      ! ScanTreePath.isDescendant( stopTreePath ) 
                      || ScanTreePath.equals( stopTreePath )
                      )  
                    )
                  { // Collapse and move.
                    logV(
                      "RootJTree.collapseAndExpandUpTreePath(..) collapse",
                      ScanTreePath
                      );
                    setSelectionPath( ScanTreePath ); // Select scan node.
                    showChangeV( ScanTreePath );
                    collapsePath( ScanTreePath );  // Collapse node.
                    showChangeV( ScanTreePath );
                    //changeBySelectingV( ScanTreePath );
                    //changeByCollapsingV( ScanTreePath );
                    // Notice that the the AutoExpandedB status is not cleared.
                    } // Collapse and move.
                  else
                  { // Move only.
                    logV("RootJTree.collapseAndExpandUpTreePath(..) out",ScanTreePath);
                    setSelectionPath( ScanTreePath ); // Select scan node.
                    showMoveV( ScanTreePath );
                    } // Move only.
                } // Move and maybe collapse one position toward root.
            logV("RootJTree.collapseAndExpandUpTreePath(..) common");
            return ScanTreePath;  // return the final common ancestor TreePath.
            } // collapseAndExpandUpTreePath(.)

        private void collapseAndExpandDownV
          ( TreePath startTreePath, TreePath stopTreePath ) 
          /* This recursive method processes a change in Selection TreePath
            from startTreePath to stopTreePath.
            It assumes stopTreePath is a descendant of startTreePath.
            It records DataNode visit information.
            It does auto-expansion of nodes if needed 
            on the way to stopTreePath.
            */
          { // collapseAndExpandDownV(.)
            TreePath stopParentTreePath= stopTreePath.getParentPath();
            boolean atTopLevelB=  // Determine whether recursion needed.
              ( stopParentTreePath.equals( startTreePath ) );
            if  // Recursively process nodes farther from stopTreePath first.
              ( ! atTopLevelB ) // There are such nodes.
              collapseAndExpandDownV(  // Recursively process them.
                startTreePath, stopParentTreePath
                );
            logV("RootJTree.collapseAndExpandDownV(..) stopParentTreePath",stopParentTreePath);
            logV("RootJTree.collapseAndExpandDownV(..) isCollapsed(..): "+isCollapsed(stopParentTreePath));
            if // Auto-expand Parent node if it is presently collapsed. 
              ( ! isExpanded( stopParentTreePath ) )
              { // Auto-expand and move.
                logV("RootJTree.collapseAndExpandDownV(..) expanding");
                setSelectionPath( stopParentTreePath ); // Select parent node.
                showChangeV( stopParentTreePath );
                expandPath( stopParentTreePath );  // Expand node.
                TreeExpansion.SetAutoExpanded(  // Set auto-expanded status.
                  stopParentTreePath, true
                  );
                showChangeV( stopTreePath );
                setSelectionPath( stopTreePath );  // Select the stop node.
                showChangeV( stopTreePath );
                } // Auto-expand and move.
              else
              { // Move only.
                logV("RootJTree.collapseAndExpandDownV(..) stopTreePath",stopTreePath);
                setSelectionPath( stopTreePath );  // Select the stop node.
                showMoveV( stopTreePath );
                }// Move only.
            } // collapseAndExpandDownV(.)

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
                commandGoToParentV();  // go to parent folder.
              else if (KeyCodeI == KeyEvent.VK_RIGHT)  // right-arrow key.
                commandGoToChildV();  // go to child folder.
              //else if (KeyCodeI == KeyEvent.VK_X)  // X-key.  ??? For debugging.
              //  Misc.dbgEventDone(); // ??? Debug.
              else
                KeyProcessedB= false;
              } // try to process the key event.
            if // Post process key... 
              (KeyProcessedB)  // ...if it was processed earlier.
              { // Post process key.
                TheKeyEvent.consume();  // ... prevent further processing of it.
                Misc.dbgEventDone(); // ??? Debug.
                } // Post process key.
            } // keyPressed.

        @Override
        public void keyReleased(KeyEvent TheKeyEvent) { }  // unused part of KeyListener interface.
        
        @Override
        public void keyTyped(KeyEvent TheKeyEvent) { }  // unused part of KeyListener interface.

    // Command methods.

      public void commandGoToChildV() 
        /* Goes to and displays the most recently visited child 
          of the present node.  If none of the children
          have been visited before then it goes to the first child.
          If there are no children then it ??? should do nothing.
          */
        { // CommandGoToChildV().
          Object childObject=  // try to get child...
          	Selection.  // ...from the visits tree...
          	  setAndReturnDataNode( // ...most recently visited...
                //savedTreePath 
                getSelectionPath()
                );  // ...of the tree node at end of selected TreePath.
              
          if (childObject == null)  // if no recent child try first one.
            { // try getting first childObject.
              Object selectedObject= 
                // savedTreePath.getLastPathComponent();
                getLastSelectedPathComponent();
              if (theDataTreeModel.getChildCount(selectedObject) <= 0)  // there are no children.
                childObject= null;  // keep childObject null.
              else  // there are children.
                childObject= theDataTreeModel.getChild(selectedObject,0);  // get first childObject.
              } // get name of first child.

          if // act based on whether a usable Child found.
            (childObject!=null) // one was found.
            { // adjust visits tree and select the child.
              TreePath childTreePath=  // child path is...
                //savedTreePath.   // ...old selected with...
                getSelectionPath().   // ...old selected with...
                  pathByAddingChild(  // ...addition of...
                  childObject    // ...the found child object.
                  );
              selectNodeV(  // select (and display)...
                childTreePath  // ...the new child path.
                );
              } // adjust visits tree and select the child.
          } // CommandGoToChildV().
  
      public void commandGoToParentV() 
        /* Goes to and displays the parent of the present tree node. */
        { // CommandGoToParentV().
          TreePath parentTreePath=  // try getting get parent TreePath.
          	//savedTreePath.getParentPath();
            getSelectionPath().getParentPath();
          if (parentTreePath != null)  // there is a parent.
            { // record visit then select and display parent node.
              selectNodeV(parentTreePath);  // select (and display) the parent directory.
              //setSelectionPath(parentTreePath); 
              } // record visit then select and display parent node.
          } // CommandGoToParentV().
  
      public void commandGoDownV() 
        /* Goes to and displays the tree node in row below the present one. */
        { // commandGoDownV().
          int RowI= getLeadSelectionRow( );  // Get # of selected row.
          TreePath NextTreePath=  // Convert next row to next TreePath.
          	getPathForRow( RowI + 1 );
          if (NextTreePath != null)  // Select that path if it exists.
            { // select and display the node.
              selectNodeV(NextTreePath);  // select (and display) the path.
              } // select and display the node.
          } // commandGoDownV().
  
      public void commandGoUpV() 
        /* Goes to and displays the tree node in row above the present one. */
        { // CommandGoUpV().
          int RowI= getLeadSelectionRow( );  // Get # of selected row.
          TreePath NextTreePath=  // Convert next row to next TreePath.
          	getPathForRow( RowI - 1 );
          if (NextTreePath != null)  // Select that path if it exists.
            { // select and display the node.
              selectNodeV(NextTreePath);  // select (and display) the path.
              } // select and display the node.
          } // CommandGoUpV().

      private void commandExpandOrCollapseV()
        /* This command method toggles when the presently selected row
          is expanded or collapsed.  It also makes certain that
          auto-expand is disabled.
          */
        {
          int SelectedRowI=   // determine which row is selected.
            getLeadSelectionRow();
          //logV("RootJTree.commandExpandOrCollapseV(..) ); ???
          if  // expand or collapse it, whichever makes sense.
            //(isExpanded(SelectedRowI))  // Row is expanded now.
            (isExpanded(getPathForRow(SelectedRowI)))  // Row is expanded now.
            { // Collapse the row and disable auto-expansion.
              collapseRow(SelectedRowI);  // collapse present node.
              TreeExpansion.SetAutoExpanded(  // force auto-expanded status...
                getLeadSelectionPath() , // ...for selected path...
                false  // ... to be off.
                );
              } // Collapse the row and disable auto-expansion.
            else  // Row is collapsed now.
              expandRow(SelectedRowI);  // Expand the row.
          }

    /* Display/paint code.

      ??? This code, or at least the code which calls it,  needs work.
      _ I need something to better control when to insert an animationDelay
        so there will be 1 and only one delay between significant display changes.
        Options are:
        * A way to detect when the display needs repainting.
        * A flag which can be set when paint is called.
      / To make things simpler I should probably completely separate
        the show code from the selection code.
      _ Maybe insert a delay if a selection triggers an auto-expand selection.

      */

      private void paintImmediately( )
        /* This method displays the present state of
          a displayable Component without having to wait
          for a normal Paint, which happens only just before
          the app is ready to process the next user input.
          It is used to display changes to a Component that
          are not the immediate result of user input.
          */
        { // paintImmediately( )
          paintImmediately(  // Paint present Component contents.
            getBounds( ) 
            );
          } // paintImmediately( )

    /* showChange code.  This is being replaced by Change Manager.  */

      private boolean showChangeB= true;  // true means show expansion changes.
      private boolean showMoveB= false;  // true means show scan moves.
      private boolean showAllB= false;  // true means show everything for debugging.
      
      /* The above default combination of parameters shows 
        all auto-expansions and collapses, and the associated movements,
        but not movements without auto-expansions and collapses.
        This prevents scanning to common ancestors,
        which would be annoying when the selection moves
        from one sibling to another.
        */

      private void showChangeV( TreePath inTreePath )
        {
          if ( showChangeB || showMoveB || showAllB )
            showV( inTreePath );
          }

      private void showMoveV( TreePath inTreePath )
        {
          if ( showMoveB || showAllB )  // Show state if showing moves is desired.
            showV( inTreePath );
          }

      private void showV( TreePath inTreePath )
        /* This method unconditionallly displays one step of a 
          possiblly long and complex selection operation
          involving possible automatic collapsing and expanding of tree nodes.
          It does not make any JTree state changes,
          except possibly to scroll the node named by inTreePath into view.
          */
        {
          scrollPathToVisible( inTreePath ); // Scroll into view.
          paintImmediately( );  // Display present JTree state.
          animationDelayV( );  // Keep it on screen for a while,
          }

    /* Change Manager.
      This code receives inputs regarding JTree node selections, expansions, and
      collapses, as well as output option "show" flag values,
      and decides when to change and output the state of the JTree.
      */

      private TreePath changeSelectionTreePath= null;  // Present selection.

      private boolean changeShowAllB= false;

      private void changesBeginV() 
        /* This method is called to begin a possible sequence of
          JTree display changes.
          */
        {
          changeSelectionTreePath= getSelectionPath();  // Save initial selection.
          }

      private void changesEndV()
        /* This method is called to end a sequence of JTree display changes. */
        {}

      private void changeBySelectingV( TreePath inTreePath )
        /* This method is called to change, and possibly show, now or later,
          the JTree node selection. 
          */
        { 
          changeSelectionTreePath= inTreePath;  // Save new selection.
          if ( changeShowAllB )  // If showing everything...
            changeSelectionUpdateV( );  // ...show new selection immediately.
          }

      private void changeSelectionUpdateV( )
        /* This method is called to update the JTree to
          a previously saved selection, if needed.
          */
        {
          if  // Selection does not need changing.
            ( changeSelectionTreePath.equals(getSelectionPath()) )
            ;  // So do nothing.
            else  // Selection needs changing.
            { // Change it and show it.
              animationDelayRequestDoMaybeV( );  // Delay if requested earlier.
              setSelectionPath( changeSelectionTreePath );  // Select the node.
              scrollPathToVisible( changeSelectionTreePath ); // Move it into view.
              paintImmediately( );  // Display present JTree state.
              animationDelaySetRequestV( true );  // Request new delay.
              } // Change it and show it.
          }

      private void changeByExpandingV( TreePath inTreePath )
        {
          animationDelayRequestDoMaybeV( );  // Delay if requested earlier.
          expandPath( inTreePath );  // Collapse node.
          scrollPathToVisible( inTreePath ); // Move it into view.
          paintImmediately( );  // Display present JTree state.
          animationDelaySetRequestV( true );  // Request new delay.
          }

      private void changeByCollapsingV( TreePath inTreePath )
        {
          animationDelayRequestDoMaybeV( );  // Delay if requested earlier.
          collapsePath( inTreePath );  // Collapse node.
          scrollPathToVisible( inTreePath ); // Move it into view.
          paintImmediately( );  // Display present JTree state.
          animationDelaySetRequestV( true );  // Request new delay.
          }

    /* Animatiion Delay manager.
      This manages the creations of short delays to be used between
      window paints to animate complex changes to the window state.
      */

      private int animationDelayI= 1000;  // != 0 means milliseconds to delay.

      private boolean animationDelayRequestedB= false;  // Request flag.

      private void animationDelaySetRequestV( boolean inB )
        /* This records a delay request by setting or clearing a flag.  */
        { animationDelayRequestedB= inB; }

      private void animationDelayRequestDoMaybeV( )
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
          if ( animationDelayI != 0 )  // Delay if desired.
            mySleepV( animationDelayI );  // Delay next output appropriately.
          }

      private void mySleepV( int msI )
        /* This method sleep for msI milliseconds.  */
        {
          try {
            Thread.sleep( msI );
            } 
          catch(InterruptedException ex) {
            Thread.currentThread().interrupt();
            }
          }

    // Debugging logging code.  Much of this might eventually be deleted.

      private boolean logB= false;  // false for no debug logging.

      private void logV( String inString, TreePath inTreePath )
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

      private void logV( String inString )
        /* This method logs inString as the name of the caller,
          and the name of the last element of the present selection path.
          This is used for debugging.  
          */
        {
          logV( 
            inString +  " SELECTION"
            , getSelectionPath()
            );
          }

      public void collapsePath( TreePath inTreePath )
        /* This method is for debugging.  */
        {
          logV("RootJTree.collapsePath(..)",inTreePath);
          super.collapsePath( inTreePath );
          }

      public void expandPath( TreePath inTreePath )
        /* This method is for debugging.  */
        {
          logV("RootJTree.expandPath(..)",inTreePath);
          super.expandPath( inTreePath );
          }

    }
