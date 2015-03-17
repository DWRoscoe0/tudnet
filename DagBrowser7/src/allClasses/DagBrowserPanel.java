package allClasses;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout; // not recommended by Eclipse QuickFix.
import java.awt.Font;
import java.awt.Graphics;
import java.awt.KeyboardFocusManager;  // See note about this below.
import java.awt.event.*;

import javax.swing.border.EtchedBorder;
//import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.TreePath;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;  // a Timer for GUIs.
//import java.util.Timer  // a more general Timer.



import static allClasses.Globals.*;  // appLogger;

public class DagBrowserPanel

  extends JPanel

  implements 
    ActionListener, 
    FocusListener
    // ,TreePathListener

  /* This class implements a JPanel which allows a user to browse 
    the Infogora DAG (Directed Acyclic Graph) as a Tree.
    The left main sub-panel is the navigation panel
    and displays the DAG as an outline using a JTree.
    The right main sub-panel displays an individual node  
    of the DAG (DataNode-s) using different 
    JComponents appropriate to the type of node.
    
    This class acts as both an Observer and a Mediator.
    As a Java Listener it observes the left and right sub-panels 
    and several buttons looking for input from them.
    It then appropriately adjusts the states of the other objects
    in a coordinated manner.

    ??? marks things to do in the code below.  
    Here are those things summarized:
    * ??? Re-factor so each method fits on a screen.
    * ??? Integrate commandHelpV() general PromptingHelp button scanner.
    * ??? Put code in JComponent building blocks sub-classes, such as:
      / Existing classes to display a Tree as
        a List, Directory, Text, as well as new classes such as
      * An enclosing JPanel, for DagBrowserPanel at least.
      * JLabels for displaying a Tree path and Tree node status.
    */

  { // class DagBrowserPanel. 

    // Injected dependency storage variables.

      AppInstanceManager theAppInstanceManager;  // For update checking.
      DataTreeModel theDataTreeModel;  // holds all browsable data.
      DataRoot theDataRoot;  // The stuff to display.
      MetaRoot theMetaRoot;  // How to display it.

    // Other instance variables.

      private Timer activityTimer; // Timer that triggers the monitor activity.
        // This timer is not started.  Using theTimerThread instead.
      private TimerThread theTimerThread = new TimerThread();
    
      // data models.
        private TreePath startTreePath;  // start TreePath to be displayed.

      // displayed sub-panel JComponent tree.
            
        private JPanel hTopJPanel; // horizontal top panel for...
          private IJButton helpIJButton;  // button to activate Help dialog.
          private IJButton downArrowIJButton;  // button to move down.
          private IJButton upArrowIJButton;  // button to move up.
          private IJButton leftArrowIJButton;  // button to move to parent.
          private IJButton rightArrowIJButton;  // button to move to child.
          private JLabel activityJLabel; // window monitor status.
          
        private JPanel viewJPanel; // JPanel where desired data is diaplayed.
          private JLabel directoryJLabel; // a place to display directory path.

          private JSplitPane theSplitPane;  // horizontally split content panel
            private JScrollPane treeJScrollPane;  // left scroller sub-panel...
              private RootJTree theRootJTree;  // ... and tree content.
            private JScrollPane dataJScrollPane;  // right scroller sub-panel...
              private JComponent dataJComponent;  // ... and its data content.
              private TreeAware dataTreeAware;  // ... and its TreeAware alias.

          private JLabel infoJLabel;  // a place to display directory/file info.

        /* Component focus control.
          
          This is the code to control what component has focus
          and the behavior that.  
          This includes the ComponentFocusStateMachine.

          The focus is normally set to either 
          the left/navigation JTree pane or the right/content pane.
          This must be restored when a button is pushed because
          that takes the focus away from whatever component had focus.  
          The focus restoration is done by stepping
          only one Component at a time in the component hierarchy
          because Java's Component.requestFocusInWindow() did not seem
          to be able to reliably move the focus more than one Component.
          */

          private enum FocusPane { // IDs of panes that can be focused.
            NO_PANE,  // neither pane.
            LEFT_PANE,  // left (tree navigation) pane.
            RIGHT_PANE   // right (content) pane.
            };
          private Component lastGainedFocusComponent= null;
            // Last main panel Component which gained focus.
          private FocusPane lastGainedFocusPane= // FocusPane equivalent.
          		FocusPane.LEFT_PANE;
          private FocusPane targetFocusPane= // State machine target pane
              FocusPane.NO_PANE;  // is initially undefined.
              /* The meanings are as follows:
                NO_PANE: the state-machine is halted.
                LEFT_PANE or RIGHT_PANE: the state machine 
                	is active and trying to reach the target focus pane.
                */

    // Constructor and related methods.
    
      public DagBrowserPanel( 
          AppInstanceManager theAppInstanceManager,
          DataTreeModel theDataTreeModel,
          DataRoot theDataRoot,
          MetaRoot theMetaRoot
)
        /* This constructor creates the DagBrowserPanel.
          This includes creating all the components, 
          defining their appearances, and
          connecting the various event listeners.
          It also starts a activityTimer used to indicate 
          that the program is running and for update checks.
          */
        {
          this.theAppInstanceManager= theAppInstanceManager;
          this.theDataTreeModel= theDataTreeModel;
          this.theDataRoot= theDataRoot;
          this.theMetaRoot= theMetaRoot;
          }

      public void initializeV()
        /* This method does initialization, exclucing dependency injection,
          which is done by the oonstructor.
          It builds the HTopPanel with all its buttons
          and the activityJLabel and adds it to the main panel.
          */
        {
          startTreePath= // Initialize startTreePath for browsing with...
            theMetaRoot.buildAttributeTreePath( );  // ...selection state.

          setOpaque( true ); // ???
          setLayout(new BorderLayout());  // use BorderLayout manager.
          { // Build and add sub-panels of this Panel.
            buildAndAddHTopJPanelV();  // Contains control components.
            buildAndAddViewJPanelV();  // Contains data components.
            } // Build and add sub-panels of this Panel.
          { // Define the content in the above panels.
            TreePath currentTreePath=  // Get TreePath of starting node.
              startTreePath;  // Is this needed ???
            theRootJTree  // In the left sub-panel JTree's...
              .getTreeHelper()  // ...TreeHelper...
              .setPartTreePathB(  // ...select...
                currentTreePath  // ...current tree node.
                );
                // This should trigger a series of events which
                // load all the data-dependent sub-panel components
                // and get them ready for display.  
            } // Define the content in the above panels.

          miscellaneousInitializationV();  // Odds and end.

          //appLogger.info("DagBrowserPanel constructor End.(");
          }

      private void buildAndAddHTopJPanelV()
        /* This method builds the HTopPanel with all its buttons
          and the activityJLabel and adds it to the main panel.
          */
        { // build hTopJPanel containing buttons and other helpful widgets.
        
          hTopJPanel= new JPanel();  // construct it.
          hTopJPanel.setLayout(new WrapLayout());  // set Layout manager.
          hTopJPanel.setAlignmentX(Component.LEFT_ALIGNMENT);  // set alignment.
          ((FlowLayout)hTopJPanel.getLayout()).setHgap(20); // spread components.

          buildAndAddIJButtonsV();

          { // create and add activityJLabel.
            activityJLabel= new JLabel("Active");  // for testing.
            activityJLabel.setOpaque(true);  // set opaque for use as activity.
            activityJLabel.setBackground(Color.WHITE);  // override doesn't work.
            activityJLabel.setFont(new Font("Monospaced",Font.BOLD,16)); // set different font.
            hTopJPanel.add(activityJLabel);
            } // create and add activityJLabel.

          add(hTopJPanel,BorderLayout.NORTH); // add it as north sub-panel
          } // build hTopJPanel containing buttons and other helpful widgets.

      private void buildAndAddIJButtonsV()
        /* This method creates and initializes
          all the IJButtons for the hTopJPanel.
          */
        {
          helpIJButton= buildAndAddIJButton(
            "Help", "Display what the buttons and some keys do."
            );
          downArrowIJButton= buildAndAddIJButton(
            "v", "Move to next item."
            );
          upArrowIJButton= buildAndAddIJButton(
            "^", "Move to previous item."
            );
          leftArrowIJButton= buildAndAddIJButton(
            "<", "Move to parent item."
            );
          rightArrowIJButton= buildAndAddIJButton(
            ">", "Move to first or last-visited child item."
            );
          }

      private IJButton buildAndAddIJButton
        (String inLabelString, String inTipString)
        /* This method creates an IJButton with label inLabelString
          and tool tip inTipString, adds it to the hTopJPanel,
          makes this class the ActionListener for ButtonEvent-s,
          and returns the button as a value.
          */
        {
          IJButton theIJButton=  // create JButton with...
            new IJButton( inLabelString );  // ...this label.
          hTopJPanel.add( theIJButton );  // Add it to hTopJPanel.
          theIJButton.setToolTipText( inTipString ); // Add tip text.
          theIJButton.addActionListener( this );  // Make this ActionListener.
          return theIJButton;  // Return button for saving.
          }

      private void buildAndAddViewJPanelV()
        /* This composition method builds the viewJPanel with its 
          major left and right sub-panels, and 2 smaller
          path and status line sub-panels.
          */
        {
          { // build viewJPanel.
            viewJPanel= new JPanel();  // construct viewJPanel.
            viewJPanel.setLayout(new BorderLayout());  // set layout manager.
            { // Build and add Current Working directoryJLabel.
              directoryJLabel= new JLabel();  // create CWD JLabel.
              directoryJLabel.setAlignmentX(Component.LEFT_ALIGNMENT);  // align it.
              directoryJLabel.setBorder(
                BorderFactory.createEtchedBorder(EtchedBorder.RAISED)
                );
              // ??? Use left or center elipsis, not right, when truncating.
              viewJPanel.add(directoryJLabel,BorderLayout.NORTH);  // add as north sub-panel.
              } // Build and add Current Working directoryJLabel.
            buildAndAddJSplitPane();  // Contains left and right sub-panels.
              // It is added to the center sub-panel.
            { // Build and add infoJLabel for displaying file information.
              infoJLabel= new JLabel();  // construct it.
              infoJLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
              infoJLabel.setOpaque( true ); // ???
              infoJLabel.setText("UNDEFINED");  // report the present info string.
              viewJPanel.add(infoJLabel,BorderLayout.SOUTH);  // add as south sub-panel.
              } // Build and add infoJLabel for displaying file information.
            add(viewJPanel,BorderLayout.CENTER);  // add it as center sub-panel.
            } // build viewJPanel.
          }

      private void buildAndAddJSplitPane()
        /* This composition method builds and adds the 
          JSplitPane for the main left and right sub-panels.
          It is added in the center of the viewJPanel
          */
        {
          buildLeftJScrollPaneV();  // Build the navigation sub-panel.
          buildRightJScrollPaneV();  // Build the content sub-panel.
          theSplitPane = // construct...
            new JSplitPane(  // ... JSplitPane...
              JSplitPane.HORIZONTAL_SPLIT,  // ...split horizontally into...
              treeJScrollPane,  // ...left scroller sub-panel and...
              dataJScrollPane  // ...right scroller sub-panel.
              );
          theSplitPane.setContinuousLayout( true );  // Enable continuous layoug mode.
          theSplitPane.setDividerLocation( 0.25 );  // Set the position of split.
          theSplitPane.setResizeWeight( 0.25 );  // Handle extra space
          viewJPanel.add(theSplitPane,BorderLayout.CENTER); // add TheJSplitPane as...
          }

      private void buildLeftJScrollPaneV()
        /* This composition method builds the left JScrollPane
          which contains the RootJTree which is used as
          a navigation pane.
          */
        {
          treeJScrollPane = new JScrollPane( );  // construct JScrollPane.
          treeJScrollPane.setMinimumSize( new Dimension( 0, 0 ) );
          treeJScrollPane.setPreferredSize( new Dimension( 400, 400 ) );
          treeJScrollPane.getViewport().setOpaque( true );
          treeJScrollPane.getViewport().setBackground( Color.GREEN );
          { // Build the JTree view for the JScrollPane.
            theRootJTree= new RootJTree(  // Construct the JTree with...
              theDataTreeModel,  // ...this model for tree data and...
              treeJScrollPane,   // ...JScrollPane for view-ability tests,
              theMetaRoot  // ...and MetaRoot for doing Selections.
              );  // Note that theDataTreeModel was built earlier.

            { // setup handling by listener of various Tree events.
              theRootJTree.getTreeHelper().addTreePathListener(
                theTreePathListener
                );
              theRootJTree.getTreeHelper().addFocusListener(this);
              } // setup handling by listener of various Tree events.
            } // Build the JTree view for the JScrollPane.
          treeJScrollPane.setViewportView(  // Set client to be scrolled...
            theRootJTree  // ...to be JTree .
            );
          }

      private void buildRightJScrollPaneV()
        /* This composition method builds the right JScrollPane
          which contains whatever JComponent is appropriate for
          displaying the item selected in 
          the left JScrollPane navigation pane.
          */
        {
          // Maybe move JScrollPane into ViewHelpter or dataJComponent??
          dataJScrollPane =   // Construct JScrollPane with null content.
            new JScrollPane( null );
          dataJScrollPane.setMinimumSize( new Dimension( 0, 0 ) );
          dataJScrollPane.setPreferredSize( new Dimension( 400, 400 ) );
          //dataJScrollPane.setBackground( Color.white );
          //dataJScrollPane.setOpaque( false );
          dataJScrollPane.getViewport().setOpaque( true );
          dataJScrollPane.getViewport().setBackground( Color.GRAY );

          replaceRightPanelContent(  // Replace null JScrollPane content...
            startTreePath  // ...with content based on startTreePath.
            );
          }

      private void miscellaneousInitializationV()
        /* This composition method does initialization of
          miscellaneous systems such as the Timer,
          the focus state machine, and an experimental key binder.
          It also queues the display of the Help dialogue. 
          */
        {

          { // build and start activityTimer.
            activityTimer = new Timer(  // construct a timer...
              1000,  // ...set to fire once per second...
              this  // ...with the DagBrowserPanel the tick Listener.
              );
            activityTimer.start();  // start activityTimer so 1-second activitys will work.
            } // build and start activityTimer.
          theTimerThread.start();  // Start TimerThread.
          lastGainedFocusComponent= // Initialize record of 
            theRootJTree;  // last component with focus.
          //restoreFocusV(); // make certain focus is set correctly.

          { // Create key mapping.  Experimental and incomplete.
            bindKeys();  
            bindActions();
            } // Create key mapping.  Experimental and incomplete.

          SwingUtilities.invokeLater( // queuing final initialization.
            new Runnable() {
              @Override  
              public void run() { 
                displayPathAndInfoV(  // display left sub-panel's info.
                  theRootJTree.getSelectedTreePath()
                  );
                commandHelpV();  // Displaying Help dialog.
                buttonEnableScanV( );  // Updating button graying.
                restoreFocusV(); // change focus from button to what is was.
                } 
              } 
            );
          
          }

    // Listener methods and their helper methods.
  
      /* ActionListener and related methods, for processing ActionEvent-s from 
        buttons and timers.
        */

        public void actionPerformed(ActionEvent inActionEvent) 
          /* This method processes ActionsEvent-s from 
            the Button-s and the activityTimer.

            ??? The button panel could be made its own class
            which uses aTreeHelper for the commands.

            */
          { // actionPerformed( ActionEvent )
            Object sourceObject=   // get Evemt source.
              inActionEvent.getSource();
            if (sourceObject == activityTimer)  // try processing timer event.
              theTimerThread.interrupt();
              // processTimerTickV();
              else // try processing button press.
              { 
                Boolean buttonDoneB= true; // Assume a button action will happen.
                { // Try the various buttons and execute JTree commands.
                  if ( buttonCommandScanB( sourceObject ) )
                    ; // Nothing else.  Command was executed.
                  else if (sourceObject == helpIJButton)
                    commandHelpV();  // give help.
                  else
                    buttonDoneB= false; // indicate no button action done.
                  } // Try the various buttons and execute JTree commands.
                if (buttonDoneB)  // restore focus to JTable if button was processed.
                  {
                    buttonEnableScanV( );
                    restoreFocusV(); // change focus from button to what is was.
                    }
                }
            } // actionPerformed( ActionEvent )

        private boolean buttonCommandScanB( Object sourceObject )
          /* This composition method processes the navigation buttons
            trying to executed an associate command.
            sourceObject is the JComponent that triggered an ActionEvent.
            It stops trying to execute at the first doable button found,
            so it executes a maximum of one button command per call.
            It returns true if a button command was executed.
            */
          {
            TreeAware focusedTreeAware=  // Panel attached to buttons...
              getFocusedTreeAware();  // ...is the one last focused.
            TreeHelper cachedTreeHelper= focusedTreeAware.getTreeHelper();

            boolean doB= true; // Set to do commands, not tests.
            boolean buttonDoneB= true; // Assume a button action will happen.

            if (sourceObject == leftArrowIJButton)
              cachedTreeHelper.commandGoToParentB(doB);
            else if (sourceObject == rightArrowIJButton)
              cachedTreeHelper.commandGoToChildB(doB);
            else if (sourceObject == downArrowIJButton)
              cachedTreeHelper.commandGoToNextB(doB);
            else if (sourceObject == upArrowIJButton)
              cachedTreeHelper.commandGoToPreviousB(doB);
            else
              buttonDoneB= false; // indicate no button action done.
              
            return buttonDoneB;  // Return whether button action occurred.
            }

        private void buttonEnableScanV( )
          /* This composition method processes the navigation buttons
            enabling or disabling each one based on 
            which commands are actually executable according to
            the TreeHelper of the focused Component.
            */
          {
            TreeAware focusedTreeAware=  // Panel attached to buttons...
              getFocusedTreeAware();  // ...is the one last focused.
            TreeHelper cachedTreeHelper= focusedTreeAware.getTreeHelper();

            leftArrowIJButton.setEnabled( 
              cachedTreeHelper.commandGoToParentB( false )
              );
            rightArrowIJButton.setEnabled( 
              cachedTreeHelper.commandGoToChildB( false )
              );
            downArrowIJButton.setEnabled( 
              cachedTreeHelper.commandGoToNextB( false )
              );
            upArrowIJButton.setEnabled( 
              cachedTreeHelper.commandGoToPreviousB( false )
              );
            }

        private void commandHelpV()
          /* This composition method impliments the Help command.  */
          {
            JOptionPane.showMessageDialog(
              this, // null, 
              "Use Arrows, in keys or buttons, to navigate folders.\n"+
              "      <Right-arrow> moves to child item.\n"+
              "      <Left-arrow> moves to parent item.\n"+
              "      <Down-arrow> moves to next item.\n"+
              "      <Up-arrow> moves to previous item\n"+
              " <Tab> key selects next window gadget.\n"+
              " <Enter> key manually expands or collapses an item.",
              "Help",
              JOptionPane.INFORMATION_MESSAGE
              );
            }
        
        private void processTimerTickV()
          /* This composition method processes one tick of the activityTimer.  
            Because it accesses UI components,
            it should be called from the UI/AWT Thread only.
            Use SwingUtilities.invokeLater(..) if needed.
            It does non-UI stuff also.
            */
          {
            { // switch activity color.
              if ( activityJLabel.isOpaque() )  //Beep maybe.
                {
                  //java.awt.Toolkit.getDefaultToolkit().beep();
                  //increment HearBeat. ???
                  }
              activityJLabel.  // Reverse activity JLabel opacity.
                setOpaque(!activityJLabel.isOpaque());
              activityJLabel.repaint();  // Request redisplay of it only.
              } // switch activity color.

            theAppInstanceManager. // Call so AppInstanceManager.getAppInstanceManager(). can poll things.
              tryExitForChainToUpdateFromNewerArgAppV();
            }

      /* TimerThread.
        This a replacement for the activityTimer,
        though I might keep it around for a while.
        */
        
        class TimerThread extends Thread
          {
            public void run()
              {
                setName("SwingTimer");  // Make debugging easy.
                while (true)
                  {
                    // Misc.snoozeV(2000);  // Do nothing for 2 second.
                    try {  // Try sleeping the requested time.
                      Thread.sleep(2000);
                      }
                    catch (InterruptedException e) { // If interrupted...
                      // Do nothing except leave sleep cut short.
                      }
                    SwingUtilities.invokeLater( // Queue tick processor.
                      new Runnable() {
                        @Override  
                        public void run() { processTimerTickV(); }  
                        } 
                      );
                    }
                }
            }

      /* TreePathListener code.

        /* This code is for when TreePathEvent-s happen in either the 
          left or right panel.  This was based on TreeSelectionListener code.
          For a while it used TreeSelectionEvent-s for 
          passing TreePath data.
          */

        private TreePathListener theTreePathListener= 
          new MyTreePathListener();

        private class MyTreePathListener 
          extends TreePathAdapter
          {
            public boolean testPartTreePathB(TreePathEvent theTreePathEvent)
              /* This method tests whether the TreePath in theTreePathEvent 
                is legal in the current display context.  
                It is implemented only by coordinators, such as this class, 
                which coordinates the TreePath-s displayed by 
                the left and right sub-panels.
                Presently the selected node in the left sub-panel is always
                the parent of path of the part selected in the right sub-panel.
                */
              { 
                boolean legalB;
                goReturn: {
                  TreePath inTreePath=  // Getting...
                    theTreePathEvent.  // ...the TreePathEvent's...
                      getTreePath();  // ...one and only TreePath.

                  if ( ! theDataRoot.isLegalB(inTreePath) )  // Handling illegal path.
                    { legalB= false; break goReturn; } // Exiting, not legal.
                  
                  Component sourceComponent=  // Getting event's source Component.
                    (Component)theTreePathEvent.getSource();
                  
                  if  // Handling source that is not right sub-panel,
                    ( ! ancestorOfB( dataJComponent, sourceComponent) )
                    { legalB= true; break goReturn; } // Exiting, legal.
                      
                  TreePath parentTreePath=  // Getting parent of path.
                    inTreePath.getParentPath();

                  if  // Handling illegal parent path.
                    ( ! theDataRoot.isLegalB(parentTreePath) ) 
                    { legalB= false; break goReturn; } // Exiting, not legal.

                  legalB= true;  // Setting legal because path passed all tests.
                  }
                return legalB;
                }

            public void setPartTreePathV( TreePathEvent theTreePathEvent )
              /* This TreePathListener method processes TreePathEvent-s 
                from TreeAware JComponents, 
                which are JComponents with TreeHelper code
                in the left and right sub-panels.
                It coordinates selections in the left and right sub-panels.  
                The left panel is a navigation pane containing a RootJTree.
                The right panel is a JComponent apprpriate for
                displaying whatever node is highlighted in the left sub-panel.
                It maintains the correct relationship between 
                what is displayed and selected in the 2 main sub-panels.
                It also updates directoryJLabel and infoJLabel with information 
                about the selection in the sub-panel with focus.

                When a TreePathEvent delivers a new TreePath
                it might or might not require that a new JComponent
                be created and placed in the right sub-panel 
                to display the node identified by that TreePath.  
                
                This method might be re-entered.
                This can  happen because DagBrowser
                listens to the sub-panels for
                TreePathEvents indicating new selections, and
                sends TreePaths to the sub-panels,
                which might themselves cause new selections.
                To prevent infinite recursion, re-entry must be detected
                and aborted somewhere.  There are several options:
                * Detect it in TreeHelper and abort it there.
                  This is also being done in 
                  TreeHelper.setPartTreePathB(inTreePath,doB),
                  by aborting processing if the TreePath would not change.
                * Detect it here in DagBrowserPanel and abort it.  
                  This is presently being done.  
                  This was the original solution.  Maybe eliminate this ???

                ??? Have a separate Listener class for each sub-panel.  
                This would eliminate decoding code, but
                might complicate re-entry detection.
                */
              {
                TreePath selectedTreePath=  // Get...
                  theTreePathEvent.  // ...the TreePathEvent's...
                    getTreePath();  // ...one and only TreePath.
                Component sourceComponent=  // Also get its source Component.
                  (Component)theTreePathEvent.getSource();
                if ( selectedTreePath != null )  // process only if not null.
                  { // Process non-null path.
                    { // process based on Source Component.
                      if (  // Source is right sub-pannel,...
                          ancestorOfB( dataJComponent, sourceComponent)
                          )
                        processSelectionFromRightSubpanel(selectedTreePath);
                      else if  // Source is left sub-pannel,...
                        ( ancestorOfB( theRootJTree, sourceComponent) )
                        processSelectionFromLeftSubpanel( selectedTreePath );
                      } // process based on Source Component.
                    recordPartPathSelectionV( );
                    } // Process non-null path.
                displayPathAndInfoV( ); // Update the display of other info.
                buttonEnableScanV( );
                Misc.dbgEventDone(); // Debug.
                }
            }

        private void recordPartPathSelectionV( )
          /* This records the part TreePath selection from 
            whichever main TreeAware sub-panel has focus.
            This TreePath may be considered to represent
            the active selection.
            */
          {
            TreeAware focusedTreeAware= // Get TreeAware JComponent with focus.
              getFocusedTreeAware();
            TreePath theTreePath=  // Get TreePath from its TreeHelper.
              focusedTreeAware.getTreeHelper().getPartTreePath();
            theMetaRoot.set( theTreePath );  // Record TreePath as selection.
            }

        private void processSelectionFromLeftSubpanel( TreePath inTreePath )
          /* This always replaces the content of the right sub-panel
            based on inTreePath.

            ??? Change to possibly reuse the present JComponent
            if the right sub-panel JComponent says that it can handle it.
            */
          { // processSelectionFromLeftSubpanel(.)
            //appLogger.info("DagBrowserPanel.processSelectionFromLeftSubpanel(..).");  // 

            replaceRightPanelContent( inTreePath );
            } // processSelectionFromLeftSubpanel(.)

        private TreePath oldPanelTreePath; // Previous right-panel TreePath.

        private void processSelectionFromRightSubpanel( TreePath inTreePath )
          /* What this does depends on inTreePath.
          
            If the new inTreePath from the right panel 
            is a sibling of the old TreePath then it does nothing 
            [or maybe selects the parent in the left sub-panel).
            
            If the new inTreePath is not a sibling of the old TreePath then 
            it replaces the right sub-panel content
            with a new Viewer appropriate to the new selection TreePath,
            and selects the appropriate TreePath in the left sub-panel.
            */
          { // processSelectionFromRightSubpanel()
            boolean siblingsB=  // Is new path a sibling of the old path?
              inTreePath.getParentPath().equals(oldPanelTreePath);
            if ( siblingsB ) // New and old Part paths have same parent.
              ; // Nothing needs to be done.
              else // New and old selections do NOT have same parent.
              { // Replace right panel and update things.
                TreePath panelTreePath= inTreePath;
                panelTreePath= panelTreePath.getParentPath();
                replaceRightPanelContent(   // Replace right panel.
                  panelTreePath
                  );
                restoreFocusV();  // Restore right panel's focus.
                theRootJTree  // In the left sub-panel JTree's...
                  .getTreeHelper()  // ...TreeHelper...
                  .setPartTreePathB(  // ...select...
                    panelTreePath  // ...appropriate path.
                    );
                } // Replace right panel and update things.
            } // processSelectionFromRightSubpanel()

        private void replaceRightPanelContent( TreePath inTreePath )
          /* This method calculates a new JComponent and its equivalent 
            TreeAware which are appropriate for displaying
            the last DataNode element of inTreePath and sets to be
            the content of the right sub-panel JScrollPane for display.

            It also does registration and unregistration 
            of the JComponent as a TreeModelListener 
            to prevent TreeModelListener leakage.
            */
          { // replaceRightPanelContent(.)
        	  TreeAware oldTreeAware= dataTreeAware;  // Save old content.
      	    { // Initialize new scroller content.
              dataJComponent=   // Calculate new JComponent...
                theDataTreeModel.  // ...by having the TreeModel...
                getDataJComponent(  // ...generate a JComponent...
                  inTreePath  // appropriate to new selection.
                  );
              dataTreeAware= // Calculate TreeAware alias.
                (TreeAware)dataJComponent;
              dataTreeAware.getTreeHelper(). // To the panel's TreeHelper's
                addTreePathListener(  // TreeSelectionListener list add
                  theTreePathListener // this panel's TreePathListener.
                  );
              dataTreeAware.getTreeHelper().addFocusListener(this);
            	dataTreeAware.getTreeHelper(). // In the panel's TreeHelper
                setDataTreeModel(theDataTreeModel); // set the DataTreeModel.
                  // This makes the TreeHelper be a TreeModelListener.
              } // Initialize new scroller content.
      	    { // Changing scroller content. 
	            dataJScrollPane.setViewportView(  // in the dataJScrollPane's viewport...
	              dataJComponent);  // ...set the DataJPanel for viewing.
	            dataJScrollPane.getViewport().setOpaque( true );
	            dataJScrollPane.getViewport().setBackground( Color.GRAY );
	            dataJComponent.repaint();  // make certain it's displayed.
	            oldPanelTreePath= inTreePath;  // Save path for compares later.
      	      }  // Change scroller content.
            if // Finalizing old scroller content...
              ( oldTreeAware != null ) // ...if it exists.
  	        	{ // Finalizing old scroller content.
                { // Unsetting the DataTreeModel in the TreeHelper.
                  oldTreeAware.getTreeHelper().setDataTreeModel( null );
                  } // This is to prevent TreeModelListener leakage.
	          		} // Finalize old scroller content.
            } // replaceRightPanelContent(.)

      // methods of the FocusListener, the FocusStateMachine, and others.

        /* Maybe make this its own class?

          WARNING: Focus code is presently difficult to debug because
          using Eclipse's breakpoints and stepping affects the
          focus state of the app.  
          It might be necessary to debug using logging.
          But at least these focus changes no longer interfere with
          debugging of non-focus code.
          */

        public void focusLost(FocusEvent theFocusEvent)
          /* This FocusListener method does nothing, 
            but it must be here as part of the FocusListener interface.
            */
          { 
            /* ??
            System.out.println(
              "focusLost(...) by "
              + ComponentInfoString(theFocusEvent.getComponent()));
            System.out.println(
              "  component gaining focus is"
              + ComponentInfoString(theFocusEvent.getOppositeComponent())
              );
            */
            }

        public void focusGained(FocusEvent theFocusEvent)
          /* This FocusListener method does what needs doing when 
            one of the main sub-panels gains focus.  This includes:

              Saving the Component getting focus in 
              lastGainedFocusComponent so restoreFocusV() 
              can restore the focus later after 
              temporary focus-altering user input.
              The two Components that usually have focus in this app are 
              the left theRootJTree and the right dataJComponent.

              Updating the Path and Info JLabel-s to agree with
              the selection in the new focused sub-panel.

							Updating the button enable states based on what
							movements are possible in the panel with focus. 
            */
          { // focusGained(FocusEvent theFocusEvent)
        		FocusPane previousFocusPane= lastGainedFocusPane;
            lastGainedFocusComponent= // Recording component with focus.
              theFocusEvent.getComponent();
            lastGainedFocusPane= // Translate owning component to enum.
              ancestorFocusPane( lastGainedFocusComponent );
        		if // Updating things only if focus owner changed.
        		    // Done to make debugging with Eclipse window easier.
        		  ( previousFocusPane != lastGainedFocusPane )
	        		{ // Updating things.
		            switch // Updating path and info based on focus owner. 
		              ( lastGainedFocusPane ) 
			            {
				            case RIGHT_PANE:
				              displayPathAndInfoV(
				                  dataTreeAware.getTreeHelper().getPartTreePath()
				                  );
				              break;
				            case LEFT_PANE:
				              displayPathAndInfoV(
				                  theRootJTree.getSelectedTreePath()
				                  );
				              break;
				            case NO_PANE:
				            default:
				            	; // Displaying nothing.
			            	}
		            buttonEnableScanV(); // Adjusting which buttons enabled.
  	        		}

            /* ??
            System.out.println(
              "focusGained(...) by"
              + " " + lastGainedFocusPane
              + ComponentInfoString(theFocusEvent.getComponent()));
            System.out.println(
              "  component losing focus is"
              + ComponentInfoString(theFocusEvent.getOppositeComponent())
              );
            */
            
            } // focusGained(FocusEvent theFocusEvent)

        private FocusPane ancestorFocusPane( Component inComponent )
          /* This method returns the FocusPanel ID of the major component panel,
            either dataJComponent or theRootJTree, 
            which is ancestor of inComponent, or
            FocusPane.NO_PANE if neither major panel is its ancestor.
            */
        	{
        	  FocusPane resultFocusPane;
          	if // inComponent is in right sub-panel (dataJComponent).
              ( ancestorOfB( dataJComponent, inComponent ) )
          		resultFocusPane= FocusPane.RIGHT_PANE;  // record right enum ID.
            else if // inComponent is in left sub-panel (theRootJTree).
              ( ancestorOfB( theRootJTree, inComponent ) )
            	resultFocusPane= FocusPane.LEFT_PANE;  // record left enum ID.
            else 
              { 
	            	resultFocusPane= FocusPane.NO_PANE;  // record no pane enum ID.
	                appLogger.info("NO_PANE gained focus.");
                }
        	  return resultFocusPane; // Return determined panel ID.
        	  }
        
        private boolean ancestorOfB(
            Component theComponent,Component theOtherComponent)
          /* This returns true if theComponent contains, that is, 
            is an ancestor of, theOtherComponent.
            A component is considered to be an ancestor of itself.
            This method was created because of 
            a bug in Component.isAncestorOf(..) making
            an extra equality test necessary. 
            */
          {
            // Doing caste because of Java's weird [J]Component hierarchy.
            Container theContainer= (Container)theComponent;
            Container theOtherContainer= (Container)theOtherComponent;

            boolean resultB= true;  // Assuming contained.

            goReturn: {  // Overriding if actually not contained.
              if ( theContainer == theOtherContainer )
                break goReturn;
              if ( theContainer.isAncestorOf( theOtherContainer ) )
                break goReturn;
              resultB= false;  // Overriding because both tests failed.
              }

            return resultB;
            }

        public void restoreFocusV()
          /* Restores the state of the app focus to one of two states, 
            previously saved by the FocusListener methods, either:
              * the left tree navigation panel has focus, or 
              * the right content panel has focus.
              
            ??? Replace complicated stepper by simple switch
            if Java bug that prevented setting focus is fixed.
            */
          { // restoreFocusV()
            //appLogger.debug("restoreFocusV(), ");

            targetFocusPane=  // change state of the FocusStateMachine to...
              lastGainedFocusPane;  // ... pane that last had focus.

            focusStepperV();  // start the FocusStateMachine stepper.
            
            } // restoreFocusV()

        private void focusStepperV()
          /* Steps the FocusStateMachine until desired focus is achieved.
            The first version separated steps using
            the 1-second activity timer for debugging.
            This version uses a no-delay fast stepper.
            */
          { // focusStepperV()
            
            if // Recursively stepping the FocusStateMachine until done.
              (focusStepB()) // Stepping and testing whether more needed
              { // Queuing more needed stepping.
                SwingUtilities.invokeLater(new Runnable() {                   
                  @Override  
                  public void run() 
                    {  
                      /* ??
                      System.out.println( 
                        "restarting the FocusStateMachine."
                        );
                      */
                      focusStepperV(); // Continuing to step.
                      }  
                  });
                }
            
            } // focusStepperV()

        private Boolean focusStepB()
          /* This method performs one step of the ComponentFocusStateMachine,
            to move the focus one Component closer to desired Component
            in the Component hierarchy.  
            It returns true if more steps of the state machine are needed, 
            false otherwise.

            ??? Simplify the focus step code with a tree scanning loop.
            This could be rewritten to simplify and shorten  by replacing
            all the Component-specific code by code which 
            scans Components upward in the hierarchy from the 
            Component which should have focus to
            the Component which does have focus
            and then requesting focus in the previous one scanned
            one level down.  This could make use of getFocusedTreeAware().
            */
          { // focusStepB().
            Component nextFocusComponent= null; // assume machine is halted.
 
            goReturn: {
              if (targetFocusPane == FocusPane.NO_PANE)  // machine halted.
                break goReturn;  // Exiting.
              Component focusedComponent=  // get Component owning the focus.
                KeyboardFocusManager.
                  getCurrentKeyboardFocusManager().getFocusOwner();
              if (focusedComponent == null)  // Handling when no focus.
                break goReturn;  // Exiting.
              //appLogger.debug("focusStepB(), "
              //  +Misc.componentInfoString(focusedComponent)
              //  + " is focused."
              //  );
              
              /* The following complex code might be replaced by
                a Component hierarchy scanning loop,
                or scanning using Component.transferFocus() ???  */

              nextFocusComponent=  // assume focusing starts at...
                viewJPanel;   // ... the root Component.
              { // override if needed.
                if (focusedComponent == viewJPanel)
                  nextFocusComponent= theSplitPane;
                else
                  { // decode based on desired panel.
                    if (targetFocusPane == FocusPane.LEFT_PANE)
                      { // step focus toward left pane.
                        if (focusedComponent == theSplitPane)
                          nextFocusComponent= treeJScrollPane;
                        else if (focusedComponent == treeJScrollPane)
                          nextFocusComponent= theRootJTree;
                        else if (focusedComponent == theRootJTree)
                          nextFocusComponent= null;  // Halting machine.
                        } // step focus toward left pane.
                    if (targetFocusPane == FocusPane.RIGHT_PANE) 
                      { // step focus toward right pane.
                        if (focusedComponent == theSplitPane)
                          nextFocusComponent= dataJScrollPane;
                        else if (focusedComponent == dataJScrollPane)
                          nextFocusComponent= dataJComponent;
                        else if (focusedComponent == dataJComponent)
                          nextFocusComponent= null;  // Halting machine.
                        } // step focus toward right pane.
                    } // decode based on desired panel.
                } // override  if needed.
                  
              if (nextFocusComponent != null)  // focus change desired.
                { // change focus.
                  //appLogger.debug("focusStepB(), "
                  //  +Misc.componentInfoString(nextFocusComponent)
                  //  + " requests focus."
                  //  );
                  nextFocusComponent.requestFocusInWindow();  // set focus
                  } // change focus.
                else  // no focus change desired.
                { // do final focus processing.
                  { // now that focus is correct, repaint the two panels.
                    dataJComponent.repaint();  // repaint right data panel.
                    theRootJTree.repaint();  // repaint left tree panel.
                    } // now that focus is correct, repaint the two panels.
                  targetFocusPane= FocusPane.NO_PANE;  // halt state machine.
                  } // do final focus processing.
              }
            return  // Returning whether there will be more steps.
              (nextFocusComponent != null);
              // runningB; // return whether machine still running.
              
            } // focusStepB().

        public TreeAware getFocusedTreeAware()
          /* This method returns a reference to the JComponent,
            casted to a TreeAware, which last had focus 
            and will probably have it again if
            it was taken away by a button click or dialog box activation.
            It is called for restoring focus and for recording 
            final correct Selections in the MetaNode tree.
            */
          {
            TreeAware resultTreeAware;
            switch ( lastGainedFocusPane ) { // Calculate from last focused pane.
              case RIGHT_PANE:
                resultTreeAware= (TreeAware)dataJComponent; break;
              case LEFT_PANE:
              case NO_PANE:
              default:
                resultTreeAware= theRootJTree; break;
              } // Calculate from last focused pane.
            return resultTreeAware;
            }

    // Key and Action bindings (KeyboardFocusManager ).  Experimental/Unused???
    
      /* Although the way Java handles Keyboard Focus has been improved,
        I still find it difficult to use.
        There are some default Key and Action bindings which
        cause Tab and Shift-Tab to move the forward and backward
        through a default cycle.
        But to use these bindings in a PromptingHelp system
        it might be necessary to replace or override these.
        
        The following is the beginning of code to do Java KeyBindings.
        It doesn't do much now because of 
        the above mentioned default bindings,
        but they don't do any harm either.
        */

      private void bindKeys()
        /* This method binds keys decoded by this component 
          to Action name Strings. 
          Presently it does only (Tab) and (Shift-Tab).  
          */
        {
          getInputMap().put(
            KeyStroke.getKeyStroke(
              KeyEvent.VK_TAB, 0
              )
            , "component forward"
            );
          getInputMap().put(
            KeyStroke.getKeyStroke(
              KeyEvent.VK_TAB, InputEvent.SHIFT_DOWN_MASK
              )
            , "component backward"
            );
          }
    
      private void bindActions()
        /* This method binds Action name Strings to Actions subclasses.
          Presently it does only (Tab) and (Shift-Tab).  
          */
        {
          getActionMap().put(
            "component forward"
            , new ComponentForwardAction( )
            );
          }

      class ComponentForwardAction extends AbstractAction 
        {
          public ComponentForwardAction() {}

          public void actionPerformed(ActionEvent e) 
            {
              Component focusedComponent=  // get focused Component.
                KeyboardFocusManager.
                getCurrentKeyboardFocusManager().getFocusOwner();
              //focusedComponent.transferFocus();  // Move focus forward.
              appLogger.info(
                "ComponentForwardAction(): "+focusedComponent
                );
              }
          }

    // miscellaneous methods.

      private void displayPathAndInfoV()
        /* This method Updates directoryJLabel and infoJLabel, 
          which appear as two lines above and below the two main sub-panels.
          What it displays depends on which of the two major sub-panels
          has focus and what, if anything, is selected in that panel.
          This method is called whenever something changes
          that might effect these fields.
          
          Maybe simplify this by using getFocusedTreeAware()???
          */
        {
          TreePath theTreePath= null;  // TreePath to be displayed.
          Component focusedComponent=  // Get Component with focus.
            KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();

          if  // Left sub-panel.
            ( ancestorOfB( theRootJTree, focusedComponent) )
            {
              //appLogger.info("DagBrowserPanel.displayPathAndInfoV() left sub-panel.");
              theTreePath= theRootJTree.getSelectedTreePath();
              } 
          else if  // Right sub-panel.
            ( ancestorOfB( dataJComponent, focusedComponent) )
            { // Calculate right sub-panel TreePath.
              //appLogger.info("DagBrowserPanel.displayPathAndInfoV() right sub-panel.");
              theTreePath= dataTreeAware.getTreeHelper().getPartTreePath();
              if ( theTreePath == null ) // There is no selection TreePath.
                theTreePath=  // Use subject TreePath instead.
                  dataTreeAware.getTreeHelper().getWholeTreePath();
              } // Calculate right sub-panel TreePath.
          else // Some other component has the focus.
            {
              //appLogger.info("DagBrowserPanel.displayPathAndInfoV() NEITHER sub-panel.");
              }

          displayPathAndInfoV( theTreePath ); // Display chosen TreePath.
          }

      private void displayPathAndInfoV(TreePath inTreePath)
        /* This method Updates directoryJLabel and infoJLabel, 
          which appear as two lines above and below the two main sub-panels.
          It displays the string representation of inTreePath 
          in directoryJLabel, and various attributes of 
          the final DataNode of that path in infoJLabel.
          This method is called whenever the TreeSelection changes
          or left-panel/right-panel focus changes.
          But it doesn't display information on an UnknownDataNode.
          If there are any UnknownDataNode-s at the end of the TreePath
          then it removes them first.
          This means that more than one path could display the same way.
          */
        { // displayPathAndInfoV(TreePath inTreePath)
          if (inTreePath == null) // No path was provided.
            { // display null info.
              //appLogger.info("DagBrowserPanel.displayPathAndInfoV( null )");
              directoryJLabel.setText("NO PATH");
              infoJLabel.setText("NO INFORMATION AVAILABLE");
              } // display null info.
            else  // A path was provided.
            { // display non-null info.
              while // Strip all error nodes from tail of TreePath.
                ( UnknownDataNode.isOneB( inTreePath.getLastPathComponent() ))
                inTreePath= inTreePath.getParentPath();  // Strip the node.
              directoryJLabel.setText(  // in directoryJLabel display set...
                theDataTreeModel.  // ...DataTreeModel's calculation of...
                  getAbsolutePathString(  // ...String representation of...
                    inTreePath  // ...of inTreePath.
                  )
                );
              infoJLabel.setText(  // set infoJLabel to be...
                theDataTreeModel.  // ...DataTreeModel's calculation of...
                  getInfoString(inTreePath)  // the info string of inTreePath.
                );
              } // display non-null info.
          } // displayPathAndInfoV(TreePath inTreePath)
        
      public void paintComponent(Graphics g)
        /* The paintComponent() method draws the current state of the app pane.  
          It isn't called as often as one might think, for example,
          when the app pane is uncovered,
          apparently because the display is restored from a saved image.
          This method exists mostly for debugging.
          */
        {
          super.paintComponent(g);  // let JPanel do most of the work..
          }

    }  // class DagBrowserPanel.

// end of file.
