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

import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.tree.TreePath;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
//import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;


//import allClasses.LockAndSignal.Input;
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
    and several buttons looking for input from them, 
    It then appropriately adjusts the states of the other objects
    to maintain the correct relationships between them.
    
    It's 3 main jobs are:
    * Constructing and initializing itself.
      This is done on the Event Dispatch Thread.
    * Processing various inputs.
    * Maintaining  state relationships.
    
    Inputs include:
    * New TreePath's of objects being displayed.
    * Component focus changes.
    * Object selections within container Components.
    * Button activations.
    * Keyboard input, indirectly handled with translation into
      other events by JComponent TreeHelpers or Java library code.

    State and outputs which are adjusted and coordinated include:
    * Which major sub-panel Component contains the Component with focus.
    * What container JComponent is displayed in the right sub-panel.
    * Which Objects within sub-panel container JComponents are selected.
    * Display of the TreePath to the node of interest.
    * Display of a line of information about the node of interest.
    * Arrow buttons, whether enabled (not grayed) or disabled (grayed).

    Arrow buttons, whether enabled or not, is based on whether
    the direction indicated by the button's arrow is possible in
    the selection in the sub-panel container JComponent which has the focus.
    The container Component's TreeHelper is queried for this information.

    ?? marks things to do in the code below.  
    Here are those things summarized:
    * ?? Re-factor so each method fits on a screen.
    * ?? Remove unneeded repaint() calls.
    */

  { // class DagBrowserPanel. 

    // Injected dependency storage variables.

      AppInstanceManager theAppInstanceManager;  // For update checking.
      DataTreeModel theDataTreeModel;  // holds all browsable data.
      DataRoot theDataRoot;  // The stuff to display.
      MetaRoot theMetaRoot;  // How to display it.
      BackgroundEventQueue theBackgroundEventQueue;

    // Other instance variables.

      private LockAndSignal activityLockAndSignal=
      		new LockAndSignal();
      private TimerThread theTimerThread = // 2-second backup timer. 
      		new TimerThread("ActivityTimer");
    
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
          
        private JPanel viewJPanel; // JPanel where desired data is displayed.
          private JLabel directoryJLabel; // a place to display directory path.

          private JSplitPane theSplitPane;  // horizontally split content panel
            private JScrollPane treeJScrollPane;  // left scroller sub-panel...
              private RootJTree theRootJTree;  // ... and tree content.
            private JScrollPane dataJScrollPane;  // right scroller sub-panel...
              private JComponent dataJComponent;  // ... and its data content.
              private TreeAware dataTreeAware;  // ... and its TreeAware alias.

          private JLabel infoJLabel;  // a place to display directory/file info.

        /* Component focus control.

          This is the code to control what component has focus.

          The component with focus is usually either 
          the left/navigation JTree pane or the right/content pane.
          This pane is called the MasterPane.
          
          Selections in the MasterPane control what is displayed by 
          other components, including the pane on the opposite side.

          The focus of the MasterPane must be restored after 
          a button is clicked because the button 
          temporarily takes the focus away from the MasterPane that had it.  

          This code includes the ComponentFocusStateMachine, being deprecated.
          */

          private enum MasterPane { // Sub-panels which could have focus.
            NEITHER_PANE,  // neither pane.
            LEFT_PANE,  // left (tree navigation) pane.
            RIGHT_PANE   // right (content) pane.
            };
          private MasterPane theMasterPane= // Last sub-panel with focus.
          		MasterPane.LEFT_PANE;
          void setPaneV(MasterPane aMasterPane) // Setter for above.
            /* There is no getter.  This setter was added to aid debugging
              to help find where theMasterPane was being set to RIGHT_PANE.
              */
	          {
	          	theMasterPane= aMasterPane;
	          	}

    // Constructor and related methods.

      public DagBrowserPanel( // Constructor.
          AppInstanceManager theAppInstanceManager,
          DataTreeModel theDataTreeModel,
          DataRoot theDataRoot,
          MetaRoot theMetaRoot,
          BackgroundEventQueue theBackgroundEventQueue
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
          this.theBackgroundEventQueue= theBackgroundEventQueue;
          }

      public void initializeV()
        /* This method does initialization, excluding dependency injection,
          which is done by the constructor.
          It builds the HTopPanel with all its buttons
          and the activityJLabel and adds it to the main panel.
          */
        {
          startTreePath= // Initialize startTreePath for browsing with...
            theMetaRoot.buildAttributeTreePath( );  // ...selection state.
          theDataTreeModel.cachePathInMapV( startTreePath );

          setOpaque( true ); // ??
          setLayout(new BorderLayout());  // use BorderLayout manager.
          { // Build and add sub-panels of this Panel.
            buildAndAddHTopJPanelV();  // Contains control components.
            buildAndAddViewJPanelV();  // Contains data components.
            } // Build and add sub-panels of this Panel.
          { // Define the content in the above panels.
          	theRootJTree.initializeV( startTreePath );
	            // Initializing the RootJTree should trigger a series of events 
	            // which load all the data-dependent sub-panel components
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
            // WrapLayout handles multiple rows better than FlowLayout.
            // It has something to do with handling of PreferredSize.
          hTopJPanel.setAlignmentX(Component.LEFT_ALIGNMENT);  // set alignment.
          ((FlowLayout)hTopJPanel.getLayout()).setHgap(20); // spread components.

          buildAndAddIJButtonsV();

          { // create and add activityJLabel.
            activityJLabel= new JLabel("Activity");  // for testing.
            activityJLabel.setOpaque(true);  // set opaque for use as activity.
            activityJLabel.setFont( // set different font.
            		new Font("Monospaced",Font.BOLD,16)
            		);
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
              // ?? Use left or center elipsis, not right, when truncating.
              viewJPanel.add(directoryJLabel,BorderLayout.NORTH);  // add as north sub-panel.
              } // Build and add Current Working directoryJLabel.
            buildAndAddJSplitPane();  // Contains left and right sub-panels.
              // It is added to the center sub-panel.
            { // Build and add infoJLabel for displaying file information.
              infoJLabel= new JLabel();  // construct it.
              infoJLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
              infoJLabel.setOpaque( true ); // ??
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

          replaceRightPanelContentWithV(  // Replace null JScrollPane content...
            startTreePath  // ...with content based on startTreePath.
            );
          }

      private void miscellaneousInitializationV()
        /* This composition method does initialization of
          miscellaneous systems such as the Timer,
          the focus state machine, and an experimental key binder.
          It also queues the display of the Help dialogue. 
          */
        { // miscellaneousInitializationV()
          theTimerThread.setDaemon(true); // ?? temporary so I needn't terminate it.
          theTimerThread.start();  // Start backup TimerThread.

          { // Create key mapping initialization.  Experimental and incomplete.
            bindKeys();  
            bindActions();
            } // Create key mapping.  Experimental and incomplete.

          displayPathAndInfoV(  // display left sub-panel's info.
              theRootJTree.getSelectedTreePath()
              );
          buttonEnableScanV( );  // Updating button graying.
          queueCommandHelpV();  // Queuing display of Help dialog.
          } // miscellaneousInitializationV()

    // Listener methods and their helper methods.
  
      /* ActionListener and related methods, for processing ActionEvent-s from 
        buttons and timers.
        */

        public void actionPerformed(ActionEvent inActionEvent) 
          /* This method processes ActionsEvent-s from 
            the Button-s and the activityTimer.

            ?? The button panel could be made its own class
            which uses aTreeHelper for the commands.

            */
          { // actionPerformed( ActionEvent )
            Object sourceObject=   // get Evemt source.
              inActionEvent.getSource();
            { 
              Boolean buttonDoneB= true; // Assume a button action will happen.
              { // Try the various buttons and execute JTree commands.
                if ( buttonCommandScanB( sourceObject ) )
                  ; // Nothing else.  Button command was executed.
                else if (sourceObject == helpIJButton)
                  queueCommandHelpV();  // give help.
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
		          buttonEnableScanV( focusedTreeAware );
		          }
        
          private void buttonEnableScanV( TreeAware theTreeAware )
            /* This composition method processes the navigation buttons
              enabling or disabling each one based on 
              which commands are actually executable 
              according to the TreeHelper of theTreeAware.
              */
            {
          	  if ( theTreeAware != null ) { // Acting if non-null.
		            TreeHelper cachedTreeHelper= theTreeAware.getTreeHelper();
		
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
	            }

        private void queueCommandHelpV()
          /* This composition method implements the Help command JFrame.
            It does this by queuing a Runnable for executing.
            
            The queuing is done on the EDT on theBackgroundEventQueue.
            theBackgroundEventQueue was used so the main JFrame 
            could be setup completely, but might no longer be necessary.
            Using invokeLater(..) might now be sufficient.
              
            The Help command itself uses a JTextArea in a JFrame.
            
            ?? Improve:
              ? Determine why restoreFocusV() doesn't work with this.
              ? Don't open multiple help JFrames if Help button pressed 
                multiple times.
            */
          { // queueCommandHelpV()
	          theBackgroundEventQueue.setBackgroundRunnableV(
	              new Runnable() {
	                @Override
	                public void run() { 
	                  String helpString= 
	                    	"Use Arrows, in keys or buttons, to navigate folders.\n"+
	                      "      <Right-arrow> moves to child item.\n"+
	                      "      <Left-arrow> moves to parent item.\n"+
	                      "      <Down-arrow> moves to next item.\n"+
	                      "      <Up-arrow> moves to previous item\n"+
	                      " <Tab> key selects next window gadget.\n"+
	                      " <Enter> key manually expands or collapses an item.";
	                    
                    final JFrame theJFrame =  // Make the Help JFrame.
                        new JFrame( 
                          AppName.getAppNameString()
                          +" Help"
                          );
                    
                    JPanel theJPanel= new JPanel();
                    Border emptyBorder = BorderFactory.createEmptyBorder(
                    		20,20,20,20
                    		);
                    theJPanel.setBorder( emptyBorder );
                    theJPanel.setLayout( 
                    		new BoxLayout( theJPanel, BoxLayout.PAGE_AXIS) 
                    		);
                    
                    JTextArea theJTextArea= new JTextArea( helpString );
                    theJTextArea.setBackground( theJFrame.getBackground() );
                    theJTextArea.setEditable( false );
                    theJTextArea.setFocusable( false );
                    theJPanel.add( theJTextArea );
                    
                    Component spacerComponent= Box.createVerticalStrut( 10 );
                    theJPanel.add( spacerComponent );
                    
                    JButton theJButton= new JButton("OK");
                    theJButton.setAlignmentX(Component.CENTER_ALIGNMENT);
                    theJButton.addActionListener( 
                    		new ActionListener() {
                    			public void actionPerformed(ActionEvent inActionEvent) {
                    				theJFrame.dispose();
                    			}
                    			}
                    		);
                    theJPanel.add( theJButton );

                    theJFrame.setContentPane( theJPanel );
                    
                    theJFrame.pack();  // Layout all the content's sub-panels.
                    theJFrame.setLocationRelativeTo(null);  // Center JFrame on screen.
                    theJFrame.setVisible(true);  // Make the window visible.
	                  } 
	                } 
	              );
            } // queueCommandHelpV()

      class TimerThread extends Thread

	      /* This class does does some tasks once every second:
	        * Thread wake delay measurement and display.
	        * EDT delay measurement and display.
	        * Software update file check.

          In the past it called java.awt.Toolkit.getDefaultToolkit().beep().

	        ?? Make into an EpiThread and start and stop in the normal way.
	        */

        {
      		long periodicTargetTimeMsL; // Target times for waits.
      		  // Making variable volatile doesn't make any difference.
      		boolean colorToggleB= false; // For gadget color.

      		TimerThread( String nameString )
        		{
        			super( nameString );
        		  }
      		
          public void run()
            {
              appLogger.info("begun.");
              periodicTargetTimeMsL= System.currentTimeMillis();
        			while (! EpiThread.exitingB() )
                {
              		//appLogger.info("in loop.");
              		doDelayMeasurementsV();
                  theAppInstanceManager. // Executing updater if present.
                    tryUpdateFromNewerFileInstanceV();
                  }
              appLogger.info("ending.");
              }

      		private void doDelayMeasurementsV()
      		  /* This methods now does little more than toggle the color
      		    of the Activity Label.
      		    
      		    Earlier this method measured and displayed two delays,
      		    but this has since been moved to SystemsMonitor.  They were:
			        * Thread wake delay, the time between the desired end of a wait
			          and the actual end of that wait.
			        * EDT delay, the time it takes between when 
			          SwingUtilities.invokeLater(..) is called and
			          the time the Runnable's run() method is executing.

      		    */
        		{
      			  final long periodMsL= Config.activityBlinkerPeriod1000MsL;
    				  long shiftInTimeMsL= // Calculating any needed time shift.
    				    activityLockAndSignal.periodCorrectedShiftMsL(
    				    		periodicTargetTimeMsL, periodMsL
    				    		);
    				  periodicTargetTimeMsL+= shiftInTimeMsL; // Adjusting target time for shift.
    	  			activityLockAndSignal.waitingForInterruptOrIntervalOrNotificationE(
  	  					///activityLockAndSignal.timeOutForMsL( 
    	  					periodicTargetTimeMsL, periodMsL ///)
      					); // Waiting for next mark.
    	  			periodicTargetTimeMsL+= periodMsL; // Advancing target.
		        	DataTreeModel.invokeAndWaitV( // Executing on EDT...
	              new Runnable() {
	                @Override  
	                public void run() { 
	                	activityJLabel.setText( "Active" );
                  	Color activityColor= (colorToggleB^=true) 
                  			? Color.WHITE
                  		  : getBackground();
                    activityJLabel.setBackground(activityColor);
                    }  
	                } 
	              );
        			}
      		
          } // class TimerThread

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
          /* This listener class handles TreePathEvents generated by
            the TreeHelper of either the left sub-panel Component or
            the right sub-panel Component.  
            The handling is MasterPane dependent.
            */
          {
            public boolean testPartTreePathB(TreePathEvent theTreePathEvent)
              /* This method tests whether the TreePath in theTreePathEvent 
                is legal in the current component display context.  
                It is implemented only by coordinators, 
                such as DagBrowserPanel, 
                which coordinates the TreePath-s displayed by 
                the left and right sub-panels.
                Presently the selected node in the left sub-panel is always
                the parent of the part selected in the right sub-panel.
                */
              { 
                boolean legalB;
                goReturn: {
                  TreePath inTreePath=  // Getting...
                    theTreePathEvent.  // ...the TreePathEvent's...
                      getTreePath();  // ...one and only TreePath.

                  if  // Handling illegal path and exiting.
                    ( ! theDataRoot.isLegalB(inTreePath) )
                    { legalB= false; break goReturn; } // Exiting, not legal.
                  
                  Component sourceComponent=  // Getting event's source.
                    (Component)theTreePathEvent.getSource();
                  
                  if  // Handling source in left sub-panel,
                    ( ancestorOfB( theRootJTree, sourceComponent) )
                    { legalB= true; break goReturn; } // Exiting, legal.
                      // No further checking is needed.

                  // Source must be right sub-panel.  Must check parent path.
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
              /* This TreePathListener method processes a TreePathEvent 
                from a TreeAware JComponent, 
                which is a JComponents with TreeHelper code.
                The TreePath identifies a selection in that component.
                The component is either the left or right sub-panel.
                Adjustments are made to other components,
                including the opposite sub-panel if needed.

                The left sub-panel is a navigation pane containing a RootJTree.
                The right sub-panel is a JComponent appropriate for
                displaying whatever node is selected in the left sub-panel.

                When the TreePath in the left sub-panel changes, 
                it might require that a new JComponent
                be created and placed in the right sub-panel 
                to display the node identified by the TreePath.  
                
                The directoryJLabel and infoJLabel are updated with information 
                based on any new selection in the MasterPane.

                This method might be re-entered.
                This can  happen because when 
                a TreePath selection happens in one sub-panel,
                a different but related path must be set 
                in the other sub-panel.
                Because this can happen in both directions,
                it would cause infinite recursion if not prevented somehow.
                Recursion is prevented by returning immediately if
                a new TreePath value is equal to the old value. 
                This returning was being done in two places:
                * TreeHelper.setPartTreePathB(inTreePath,doB).
                * DagBrowserPanel.  Maybe eliminate this ??

                ?? Have a separate Listener class for each sub-panel.  
                This would eliminate decoding code, but
                might complicate re-entry detection.
                */
              { 
                TreePath selectedTreePath=  // Get...
                  theTreePathEvent.  // ...the TreePathEvent's...
                    getTreePath();  // ...one and only TreePath.
                Component sourceComponent=  // Also get its source Component.
                  (Component)theTreePathEvent.getSource();
                if ( selectedTreePath != null )  // Processing if path not null.
                  { // Processing non-null path.
                    { // processing	 based on Source Component.
                      if (  // Source is right sub-panel,...
                          ancestorOfB( dataJComponent, sourceComponent)
                          )
                        processSelectionFromRightSubpanel(selectedTreePath);
                      else if  // Source is left sub-panel,...
                        ( ancestorOfB( theRootJTree, sourceComponent) )
                        processSelectionFromLeftSubpanel( selectedTreePath );
                      } // process based on Source Component.
                    } // Process non-null path.
                updateMiscellaneousV();
                Misc.dbgEventDone(); // Debug.
                }

            } // MyTreePathListener

        private void processSelectionFromLeftSubpanel( TreePath inTreePath )
          /* This always replaces the content of the right sub-panel
            based on inTreePath.

            ?? Change to possibly reuse the present JComponent
            if the right sub-panel JComponent says that it can handle it.
            */
          { // processSelectionFromLeftSubpanel(.)
            //appLogger.info("DagBrowserPanel.processSelectionFromLeftSubpanel(..).");  // 

            replaceRightPanelContentWithV( inTreePath );
            } // processSelectionFromLeftSubpanel(.)

        private TreePath oldParentTreePath; // Previous right-panel TreePath.

        private void processSelectionFromRightSubpanel( 
        		TreePath selectedTreePath 
        		)
          /* This method adjusts the left and right sub-panels, if needed.
            It depends on inTreePath.

            If the new inTreePath from the right panel 
            is a sibling of the old TreePath then it does nothing.
            It assumes that the right subpanel has or will adjust
            its selection appropriately.
            
            If the new inTreePath is not a sibling of the old TreePath then
            it does the following: 
	          * It replaces the right sub-panel content
	            with a new Viewer appropriate to the new selection TreePath.
	          * It selects the appropriate TreePath in the left sub-panel.
	            Presently the left sub-panel TreePath is always
	            the parent of the right sub-panel TreePath.
            */
          { // processSelectionFromRightSubpanel()
            boolean siblingsB=  // Calculating whether paths are siblings.
              selectedTreePath.getParentPath().equals(oldParentTreePath);
            if ( siblingsB ) // New and old Part paths have same parent.
              ; // Nothing needs to be done.
              else // New and old selections do NOT have same parent.
              { // Replace right panel and update things.
  	            TreePath parentTreePath= selectedTreePath.getParentPath();
  	            oldParentTreePath=   // Save path for compares later.
  	            		parentTreePath;
                replaceRightPanelContentWithV( parentTreePath );
                theRootJTree  // In the left sub-panel JTree's...
                  .getTreeHelper()  // ...TreeHelper...
                  .setPartTreePathB(  // ...select...
                    parentTreePath  // ...parent path.
                    );
                restoreFocusV();  // Restore right panel's focus.
                } // Replace right panel and update things.
            } // processSelectionFromRightSubpanel()

        private void replaceRightPanelContentWithV( TreePath inTreePath )
          /* This method calculates a new JComponent and its equivalent 
            TreeAware which are appropriate for displaying
            the last DataNode element of inTreePath.  
            Then it replaces in the right sub-panel JScrollPane 
            the old JComponent and TreeAware alias with the new ones.

            During this process it also does 
            registration and unregistration of objects as TreeModelListeners 
            to prevent TreeModelListener leakage.
            */
          { // replaceRightPanelContent(.)
        	  TreeAware oldTreeAware=  // Saving  (alias of) present JComponent. 
        	  		dataTreeAware;

            dataJComponent=   // Calculating new JComponent...
              theDataTreeModel.  // ...by having the TreeModel build it.
                getDataJComponent( inTreePath );
            dataTreeAware= // Calculating its new TreeAware alias.
              (TreeAware)dataJComponent;

            dataTreeAware. // Initializing the new JComponent by calling
              getTreeHelper().initializeHelperV( // its helper's initializer.
            		theTreePathListener, // using this panel's TreePathListener.
            		this, // as a FocusListener.
            		theDataTreeModel
                );

      	    { // Replacing scroller content with new JComponent. ?? factor? 
	            dataJScrollPane.setViewportView(  // in the dataJScrollPane's viewport...
	              dataJComponent  // ...set the DataJPanel for viewing.
	              );
	            dataJScrollPane.getViewport().setOpaque( true );
	            dataJScrollPane.getViewport().setBackground( Color.GRAY );
	            dataJComponent.repaint();  // make certain it's displayed.
      	      }

            if // Finalizing old scroller content
              ( oldTreeAware != null ) // if it exists, using
            	oldTreeAware.getTreeHelper().finalizeHelperV(); // its TreeHelper.
                // This is done to prevent Listener leaks.
            } // replaceRightPanelContent(.)


        // Key and Action bindings (KeyboardFocusManager ).  Experimental/Unused??
        
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

          private void updateMiscellaneousV( )
            /* This method updates everything except the 2 main sub-panels.
              It should be called when:
              * Focus changes.
              * Selection changes.
              */
	          {
          		TreeAware theTreeAware= null;
	            switch // Updating path and info based on focus owner. 
	              ( theMasterPane ) 
		            {
			            case RIGHT_PANE: 
			            	theTreeAware= dataTreeAware; 
			              break;
			            case LEFT_PANE: 
			            	theTreeAware= theRootJTree;
			              break;
			            case NEITHER_PANE:
			            default: 
			            	theTreeAware= null;
			            	; // Displaying nothing.
		            	}
	            if ( theTreeAware != null ) { // Updating unless null.
		            buttonEnableScanV( theTreeAware );
		            TreePath theTreePath= 
		            		theTreeAware.getTreeHelper().getPartTreePath() ;
			          displayPathAndInfoV( theTreePath );
	          		theMetaRoot.set( theTreePath ); // Record as global selection.
	            	}
		          }

          private void displayPathAndInfoV(TreePath inTreePath)
            /* This method Updates directoryJLabel and infoJLabel, 
              which appear as two lines above and below the two main sub-panels.
              It displays the string representation of inTreePath 
              in directoryJLabel, and various attributes of 
              the final DataNode of that path in infoJLabel.
              This method is meant to be called whenever:
              * the TreeSelection changes
              * or left-panel/right-panel focus changes.
              But it doesn't display information on an UnknownDataNode.
              If there are any UnknownDataNode-s at the end of the TreePath
              then it removes them first.
              This means that more than one path could display the same way.
              */
            { // displayPathAndInfoV(TreePath inTreePath)
              if (inTreePath == null) // Handling no path provided.
                { // display null info.
                  //appLogger.info("DagBrowserPanel.displayPathAndInfoV( null )");
                  directoryJLabel.setText("NO PATH");
                  infoJLabel.setText("NO INFORMATION AVAILABLE");
                  } // display null info.
                else  // Handling path provided.
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
            /* The paintComponent() method draws 
              the current state of the app pane.  
              It isn't called as often as one might think, for example,
              when the app pane is uncovered,
              apparently because the display is restored from a saved image.
              This method now exists mostly for debugging.
              */
            {
              super.paintComponent(g);  // let JPanel do most of the work..
              }


      /* Focus management code.
        This includes methods of the FocusListener, 
        the FocusStateMachine (being deprecated), and others.

				This code does the following:
				* It updates various state and outputs when 
				  either the left or right sub-panels gains focus.
				* It records which of either the left or right sub-panels
				  last had focus.  This is so that focus can be restored if
				  the loss of that focus was temporary.

        WARNING: Focus code is presently difficult to debug because
        using Eclipse's breakpoints and stepping affects the
        focus state of the app.  
        It might be necessary to debug using logging.
        But at least these focus changes no longer interfere with
        debugging of non-focus code.
        
        ?? Maybe put some of these methods in a separate class.
        */

      public void focusLost(FocusEvent theFocusEvent)
        /* This FocusListener method does nothing, 
          but it must be here as part of the FocusListener interface.
          */
        { 
      		//appLogger.debug(
		      //  "DagBrowserPanel.focusLost(...),"
	        //  +"\n  lost by "
	        // + Misc.componentInfoString(theFocusEvent.getComponent())
		      // +", gained by "
		      //   + Misc.componentInfoString(theFocusEvent.getOppositeComponent())
		      // );
          }

      public void focusGained(FocusEvent theFocusEvent)
        /* This FocusListener method does what needs doing when 
          a JComponent in one of the main sub-panels gains focus.  
          It is probably called from the TreeHelper of 
          the sub-panels JComponent.  What needs doing includes:

          * Saving the identity of the Component getting focus in 
            theMasterPane so restoreFocusV() 
            can restore its focus later after 
            any temporary focus-altering user input occurs.
            The two Components that usually have focus, in this app are 
            the theRootJTree in the left sub-panel and 
            the dataJComponent in the right sub-panel.

          * Updating the Path and Info JLabel-s to agree with
            any new selection in the possibly new theMasterPane.

				  * Updating the button enable states based on what
						movements are possible in the new sub-panel with focus.

          */
        { // focusGained(FocusEvent theFocusEvent)
	    		//appLogger.debug(
		      //    "DagBrowserPanel.focusGained(...) begin"
		      //		+"\n  gained by "
		      //   + Misc.componentInfoString(theFocusEvent.getComponent())
		      //   +", lost by "
		      //     + Misc.componentInfoString(theFocusEvent.getOppositeComponent())
		      //   );
      		MasterPane previousMasterPane= theMasterPane;
          setPaneV( // Translate Component with focus to MasterPane ID.
            ancestorMasterPane( theFocusEvent.getComponent() )
            );
      		if // Updating only if sub-panel with focused subcomponent changed.
      		    // Done to make debugging with Eclipse window easier?
      		  ( previousMasterPane != theMasterPane )
      			updateMiscellaneousV();
          } // focusGained(FocusEvent theFocusEvent)

      private MasterPane ancestorMasterPane( Component inComponent )
        /* This method returns the MasterPane ID of 
          the major component panel, either dataJComponent or theRootJTree, 
          which is ancestor of inComponent, or
          MasterPane.NEITHER_PANE if neither major panel is its ancestor.
          */
      	{
      	  MasterPane resultMasterPane;
        	if // inComponent is in right sub-panel (dataJComponent).
            ( ancestorOfB( dataJComponent, inComponent ) )
        		resultMasterPane= MasterPane.RIGHT_PANE;  // record right enum ID.
          else if // inComponent is in left sub-panel (theRootJTree).
            ( ancestorOfB( theRootJTree, inComponent ) )
          	resultMasterPane= MasterPane.LEFT_PANE;  // record left enum ID.
          else 
            { 
            	resultMasterPane= MasterPane.NEITHER_PANE;  // record no pane enum ID.
                appLogger.info("NEITHER_PANE gained focus.");
              }
      	  return resultMasterPane; // Return determined panel ID.
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
          depending on theMasterPane: 
            * MasterPane.LEFT_PANE: the left tree navigation pane gets focus. 
            * MasterPane.RIGHTPANE: the right content pane gets focus.

          This method no longer needs the old focusStepperV() method.
          */
        { // restoreFocusV()
      	  Component theComponent= null;
      		switch (theMasterPane) { // Translating MastPane ID to Component.
	      		case LEFT_PANE:
	      			theComponent= theRootJTree; break;
	      		case RIGHT_PANE: 
	      			theComponent= dataJComponent; break;
	      	  default:
	      	  	; // Nothing.
      			}
      		Misc.requestFocusV(theComponent);
          } // restoreFocusV()


      private TreeAware getFocusedTreeAware()
        /* This method returns a reference to the JComponent,
          casted to a TreeAware, which last had focus, 
          and will probably have it again if
          it was taken away by a button click or dialog box activation.
          Its only callers now are methods which enable or execute
          command buttons based on the TreePath associated
          with the TreeWare/JComponent which last had focus.
          It always returns a valid TreeAware, never a null.
          */
        {
          TreeAware resultTreeAware;
          switch ( theMasterPane ) { // Calculate from last focused pane.
            case RIGHT_PANE:
              resultTreeAware= (TreeAware)dataJComponent; break;
            case LEFT_PANE:
            case NEITHER_PANE:
            default:
              resultTreeAware= theRootJTree; break;
            } // Calculate from last focused pane.
          return resultTreeAware;
          }
      

  }  // class DagBrowserPanel.

// end of file.
