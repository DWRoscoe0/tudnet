package allClasses;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout; // not recommended by Eclipse QuickFix.
import java.awt.Font;
import java.awt.Graphics;
import java.awt.KeyboardFocusManager;  // See note about this below.
import java.awt.event.*;

import javax.swing.event.TreeSelectionEvent;
//import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;
import javax.swing.AbstractAction;
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
    FocusListener,
    //TreeSelectionListener,
    TreePathListener

  /* This class implements a panel which allows a user to browse 
    the Infogora DAG (Directed Acyclic Graph) as a Tree.
    The left main sub-panel is the navigation panel
    and displays the graph using a JTree.
    The right main sub-panel displays nodes of the graph (DataNode-s).
    The JComponent used there depends on the type of data.
    */

  /* ??? marks things to do below.  Here are those items summarized:
    * Factor DagBrowserPanel() and other methods so eachffits on a screen.
    * FocusStepB() simplification using scanning loop.
    * CommandHelpV() replacement with a general PromptingHelp system.
    */

  { // class DagBrowserPanel. 

    // static variables.
      //private static final long serialVersionUID = 1L;

    // instance variables.

      private Timer BlinkerTimer; // Timer that triggers the monitor Blinker.
    
      // data models.
        private DataTreeModel TheDataTreeModel;  // holds all browsable data.
        private TreePath StartTreePath;  // start TreePath to be displayed.

      // displayed sub-panel JComponent tree.
            
        private JPanel HTopJPanel; // top horizontal panel for...
          private IJButton HelpIJButton;  // button to activate Help dialog.
          private IJButton DownArrowIJButton;  // button to move down.
          private IJButton UpArrowIJButton;  // button to move up.
          private IJButton LeftArrowIJButton;  // button to move to parent.
          private IJButton RightArrowIJButton;  // button to move to child.
          private JLabel BlinkerJLabel; // window monitor status.
          
        private JPanel ViewJPanel; // JPanel where desired data is diaplayed.
          private JLabel DirectoryJLabel; // a place to display directory path.

          private JSplitPane TheSplitPane;  // horizontally split content panel
            private JScrollPane TreeJScrollPane;  // left scroller sub-panel...
              private RootJTree theRootJTree;  // ... and tree content.
            private JScrollPane DataJScrollPane;  // right scroller sub-panel...
              private JComponent DataJComponent;  // ... and its data content.
              private TreeAware DataTreeAware;  // ... and its TreeAware alias.

          private JLabel InfoJLabel;  // a place to display directory/file info.

      /* ComponentFocusStateMachine.
        This state machine restores the focus to either 
        the left/navigation JTree pane or the right/content pane.  
        This restoration is done becaue most of the time 
        the focus is in one of those two places while the user browses, 
        and the user will want to move the cursor back into
        one of these panes if it ever goes out.
        The reason the restoration is done stepping
        only one Component at a time in the component hierarchy
        is because Component.requestFocusInWindow() did not seem
        to be able to reliably move the focus more than one Component.
        */
        private enum FocusPane {   // the normal focus panes.
          NO_PANE,  // neither pane.
          LEFT_PANE,  // left (tree view) pane.
          RIGHT_PANE   // right (content view) pane.
          };
        private FocusPane LastFocusPane=  // The last FocusPane that had focus.
          FocusPane.LEFT_PANE;  // Set to focus right pane initially.
          // was: FocusPane.RIGHT_PANE;  // Set to focus right pane initially.
        private FocusPane desiredFocusPane=  // The next pane to get focus...
          FocusPane.NO_PANE;  // ... is initially undefined.
          /* The meanings are as follows:
            if NO_PANE then the ComponentFocusStateMachine is not running.
            if LEFT_PANE or RIGHT_PANE then the ComponentFocusStateMachine 
            is active and trying to set the focus to that pane Component.  */
        // private int FocusStepCountI=0; // Debug.
        private Component LastValidFocusOwnerPanelComponent= null;
          /* if non-null then a focus-altering command is underway and 
            the value is the penel Component with focus immediately before 
            the command began.  */
        
      private Boolean TreeSelectionReentryBlockedB=   // for preventing entry...
        false;  // ...to TreeSelectionListener.

    // constructor and related methods.
    
      public DagBrowserPanel()
        /* This constructor creates the DagBrowserPanel.
          This includes creating all the components, 
          defining their appearances, and
          connecting the various event listeners.
          It also starts a BlinkerTimer used to indicate 
          that the program is running.
          */
        { // DagBrowserPanel()
          buildDataModelsAndGraphsV();  // This is where the data is.

          setLayout(new BorderLayout());  // use BorderLayout manager.
          { // Build and add sub-panels of this Panel.
            buildAndAddHTopJPanelV();  // Contains control components.
            buildAndAddViewJPanelV();  // Contains data components.
            } // Build and add sub-panels of this Panel.
          { // Define the content in the above panels.
            TreePath CurrentTreePath=  // Get TreePath of starting node.
              StartTreePath;
            theRootJTree.  // In the left sub-panel...
              selectNodeV(CurrentTreePath);  // ...select current tree node.
              // This should trigger a series of events which
              // loads all the data-dependent sub-panel components
              // and get them ready for display.  
            } // Define the content in the above panels.

          miscellaneousInitializationV();  // Odds and end.

          //appLogger.info("DagBrowserPanel constructor End.(");  // ???
          } // DagBrowserPanel()

      private void buildAndAddHTopJPanelV()
        /* This method builds the HTopPanel with all its buttons
          and the BlinkerJLabel and adds it to the main panel.
          */
        { // build HTopJPanel containing buttons and other helpful widgets.
        
          HTopJPanel= new JPanel();  // construct it.
          HTopJPanel.setLayout(new WrapLayout());  // set Layout manager.
          HTopJPanel.setAlignmentX(Component.LEFT_ALIGNMENT);  // set alignment.
          ((FlowLayout)HTopJPanel.getLayout()).setHgap(20); // spread components.

          buildAndAddIJButtonsV();

          { // create and add BlinkerJLabel.
            BlinkerJLabel= new JLabel("Active");  // for testing.
            BlinkerJLabel.setOpaque(true);  // set opaque for use as blinker.
            BlinkerJLabel.setBackground(Color.WHITE);  // override doesn't work.
            BlinkerJLabel.setFont(new Font("Monospaced",Font.BOLD,16)); // set different font.
            HTopJPanel.add(BlinkerJLabel);
            } // create and add BlinkerJLabel.

          add(HTopJPanel,BorderLayout.NORTH); // add it as north sub-panel
          } // build HTopJPanel containing buttons and other helpful widgets.

      private void buildAndAddIJButtonsV()
        /* This method creates and initializes
          all the IJButtons for the HTopJPanel.
          */
        {
          HelpIJButton= buildAndAddIJButton(
            "Help", "Display what the buttons and some keys do."
            );
          DownArrowIJButton= buildAndAddIJButton(
            "v", "Move to next item."
            );
          UpArrowIJButton= buildAndAddIJButton(
            "^", "Move to previous item."
            );
          LeftArrowIJButton= buildAndAddIJButton(
            "<", "Move to parent item."
            );
          RightArrowIJButton= buildAndAddIJButton(
            ">", "Move to first or last-visited child item."
            );
          }

      private IJButton buildAndAddIJButton
        (String inLabelString, String inTipString)
        /* This method creates an IJButton with label inLabelString
          and tool tip inTipString, adds it to the HTopJPanel,
          makes this class the ActionListener for ButtonEvent-s,
          and returns the button as a value.
          */
        {
          IJButton theIJButton=  // create JButton with...
            new IJButton( inLabelString );  // ...this label.
          HTopJPanel.add( theIJButton );  // Add it to HTopJPanel.
          theIJButton.setToolTipText( inTipString ); // Add tip text.
          theIJButton.addActionListener( this );  // Make this ActionListener.
          return theIJButton;  // Return button for saving.
          }

      private void buildAndAddViewJPanelV()
        /* This composition method builds the ViewJPanel with its 
          major left and right sub-panels, and 2 smaller
          path and status line sub-panels.
          */
        {
          { // build ViewJPanel.
            ViewJPanel= new JPanel();  // construct ViewJPanel.
            ViewJPanel.setLayout(new BorderLayout());  // set layout manager.
            { // Build and add Current Working DirectoryJLabel.
              DirectoryJLabel= new JLabel();  // create CWD JLabel.
              DirectoryJLabel.setAlignmentX(Component.LEFT_ALIGNMENT);  // align it.
              ViewJPanel.add(DirectoryJLabel,BorderLayout.NORTH);  // add as north sub-panel.
              } // Build and add Current Working DirectoryJLabel.
            buildAndAddJSplitPane();  // Contains left and right sub-panels.
              // It is added to the center sub-panel.
            { // Build and add InfoJLabel for displaying file information.
              InfoJLabel= new JLabel();  // construct it.
              InfoJLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
              InfoJLabel.setText("UNDEFINED");  // report the present info string.
              ViewJPanel.add(InfoJLabel,BorderLayout.SOUTH);  // add as south sub-panel.
              } // Build and add InfoJLabel for displaying file information.
            add(ViewJPanel,BorderLayout.CENTER);  // add it as center sub-panel.
            } // build ViewJPanel.
          }

      private void buildAndAddJSplitPane()
        /* This composition method builds and adds the 
          JSplitPane for the main left and right sub-panels.
          It is added in the center of the ViewJPanel
          */
        {
          buildLeftJScrollPaneV();  // Build the navigation sub-panel.
          buildRightJScrollPaneV();  // Build the content sub-panel.
          TheSplitPane = // construct...
            new JSplitPane(  // ... JSplitPane...
              JSplitPane.HORIZONTAL_SPLIT,  // ...split horizontally into...
              TreeJScrollPane,  // ...left scroller sub-panel and...
              DataJScrollPane  // ...right scroller sub-panel.
              );
          TheSplitPane.setContinuousLayout( true );  // Enable continuous layoug mode.
          //TheSplitPane.setDividerLocation( 0.25 );  // Set the position of split.
          TheSplitPane.setResizeWeight( 0.25 );  // Handle extra space
          ViewJPanel.add(TheSplitPane,BorderLayout.CENTER); // add TheJSplitPane as...
          }

      private void buildLeftJScrollPaneV()
        /* This composition method builds the left JScrollPane
          which contains the RootJTree which is used as
          a navigation pane.
          */
        {
          TreeJScrollPane = new JScrollPane( );  // construct JScrollPane.
          TreeJScrollPane.setMinimumSize( new Dimension( 0, 0 ) );
          TreeJScrollPane.setPreferredSize( new Dimension( 400, 400 ) );
          { // Build the JTree view for the JScrollPane.
            theRootJTree= new RootJTree(  // Construct the JTree with...
              TheDataTreeModel,  // ...this model for tree data and...
              TreeJScrollPane   // ...JScrollPane for view-ability tests.
              );  // Note that TheDataTreeModel was built earlier.

            { // setup handling by listener of various Tree events.
              //theRootJTree.addTreeSelectionListener(this);
              theRootJTree.addTreePathListener(this);
              theRootJTree.addFocusListener(this);
              // theRootJTree.addKeyListener(this);
              } // setup handling by listener of various Tree events.
            } // Build the JTree view for the JScrollPane.
          TreeJScrollPane.setViewportView(  // Set client to be scrolled...
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
          { // build the scroller content.
            DataJComponent=   // calculate JPanel from TableModel.
              TheDataTreeModel.GetDataJComponent(
                StartTreePath // initially displaying root/child.
                );  // Note that TheDataTreeModel was built earlier.
            DataTreeAware= (TreeAware)DataJComponent;  // Define alias...
            DataTreeAware.addTreePathListener(  // ...and Listener.
              this
              );
            } // build the scroller content.
            
          // Maybe move JScrollPane into ViewHelpter or DataJComponent??
          DataJScrollPane = new JScrollPane( DataJComponent );  // construct scroller from data panel.
          DataJScrollPane.setMinimumSize( new Dimension( 0, 0 ) );
          DataJScrollPane.setPreferredSize( new Dimension( 400, 400 ) );
          DataJScrollPane.setBackground( Color.white );
          }

      private void miscellaneousInitializationV()
        /* This composition method does initialization of
          miscellaneous systems such as the Timer,
          the focus state machine, and an experimental key binder.
          It also queues the display of the Help dialogue. 
          */
        {

          { // build and start BlinkerTimer.
            BlinkerTimer = new Timer(  // construct a timer...
              1000,  // ...set to fire once per second...
              this  // ...with the DagBrowserPanel the tick Listener.
              );
            BlinkerTimer.start();  // start BlinkerTimer so 1-second blinkers will work.
            } // build and start BlinkerTimer.
          LastValidFocusOwnerPanelComponent=   // initialize ID of last component with focus.
            // theRootJTree.getTree();
            theRootJTree;  // was DataJComponent;            
          RestoreFocusV(); // make certain focus is set correctly.

          { // Create key mapping.  Experimental and incomplete.
            bindKeys();  
            bindActions();
            } // Create key mapping.  Experimental and incomplete.

          SwingUtilities.invokeLater( new Runnable() { // queue giving Help.
            @Override  
            public void run() { CommandHelpV(); }  
            } );
          
          }

      private void buildDataModelsAndGraphsV()
        /* This composition method builds the TreeModel
          which will be the source of Infogora DAG to be browsed.
          It also builds the MetaTool cache DAG which
          stores information about the Infogora graph.
          */
        {
          StartTreePath= // Initialize StartTreePath for browsing...
            Selection.buildAttributeTreePath( );  // ...saved selection state.
          TheDataTreeModel =  // Initialize DataTreeModel for JTree using...
            //new DataTreeModel(RootDataNode);  // ...DataNode root.
            new DataTreeModel(   // ...DataNode root parent.
              DataRoot.getParentOfRootDataNode() 
              );
          }

    // Listener methods and their helpers.
  
      /* ActionListener and related methods, for processing ActionEvent-s from 
        buttons and timers.
        */
      
        public void actionPerformed(ActionEvent TheActionEvent) 
          /* This method processes ActionsEvent-s from 
            the Button-s and the BlinkerTimer.
            
            ??? This should be changed so that if the right sub-panel has focus,
            then the triggered commands will happen in the right sub-panel.
            Presently they always happen in the left sub-panel RootJTree.
            */
          { // actionPerformed( ActionEvent )
            Object SourceObject= TheActionEvent.getSource();  // get Evemt source.
            if (SourceObject == BlinkerTimer)  // try processing timer event.
              ProcessBlinkerTimerV();
              else // try processing button press.
              { 
                Boolean ButtonDone= true; // assume we will do a button action.
                { // try the various buttons.
                  if (SourceObject == LeftArrowIJButton)
                    theRootJTree. // delegate to JTree panel and...
                      commandGoToParentV();  // ...its GoToParent command.
                  else if (SourceObject == RightArrowIJButton)
                    theRootJTree. // delegate to JTree panel and...
                      commandGoToChildV();  // ...its GoToChild command.
                  else if (SourceObject == DownArrowIJButton)
                    theRootJTree. // delegate to JTree panel and...
                      commandGoDownV();  // ...its GoDown command.
                  else if (SourceObject == UpArrowIJButton)
                    theRootJTree. // delegate to JTree panel and...
                      commandGoUpV();  // ...its GoUp command.
                  else if (SourceObject == HelpIJButton)
                    CommandHelpV();  // give help.
                  else
                    ButtonDone= false; // indicate no button action done.
                  } // try the various buttons.
                if (ButtonDone)  // restore focus to JTable if button was processed.
                  RestoreFocusV(); // change focus from button to what is was.
                }
            } // actionPerformed( ActionEvent )

        private void CommandHelpV()
          /* This composition method impliments the Help command.  */
          { // give help.
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
            } // give help.
        
        private void ProcessBlinkerTimerV()
          /* This composition method process one tick of the BlinkerTimer.  */
          { // process timer event.
            { // switch blinker color.
              BlinkerJLabel.setOpaque(!BlinkerJLabel.isOpaque());  // reverse opacity.
              BlinkerJLabel.repaint();  // request redisplay of it only.
              } // switch blinker color.

            AppInstanceManager.tryExitForChainToUpdateFromNewerArgAppV();
            } // process timer event.

      /* TreePathListener methods, for when TreeSelectionEvent-s
        happen in either the left or right panel.
        This was based on TreeSelectionListener code.
        It still uses TreeSelectionEvent-s for passing TreePath info.
        */
        
        public void partTreeChangedV( TreeSelectionEvent theTreeSelectionEvent )
/*
          // Temporary bridge method. ???
          { valueChanged( theTreeSelectionEvent ); }
          
        public void valueChanged( TreeSelectionEvent theTreeSelectionEvent ) 
*/
          /* This processes TreeSelectionEvent-s.
            It's job is to coordinate selections in 
            the left and right sub-panels.  
            This includes 

              Passing appropriate TreePath selection information to
              the major left and right sub-panels, 

              Maintaining the parent-child relationship between them.
              The selection, if it exists, in the right sub-panel,
              should be a child of the selection in the left JTree sub-panel.
              The right sub-panel should display a descendent
              in the selection of the left sub-panel.

            It also updates DirectoryJLabel and InfoJLabel
            appropriate to the selection in the sub-panel with focus.

            When a TreeSelectionEvent delivers a new TreePath
            it might or might not require that a new JComponent
            be used in the right sub-panel.  
            It is required if 
            1. The JComponent in the right sub-panel
              determines that it can not display the new TreePath.
            2. The new TreePath is not a descendant of
              the TreePath currently displayed by 
              the JComponent in the right sub-panel
              
            Most the work of this method id done by alueChangedProcessorV(..)
            if it determined that valueChanged(..) was not re-entered.
            */
          { // valueChanged( TreeSelectionEvent theTreeSelectionEvent ) 
            if ( TreeSelectionReentryBlockedB ) // process unless a re-entry.
              { // do nothing because the re-entry blocking flag is set.
                appLogger.info(
                  "DagBrowserPanel.valueChanged(..), re-entry blocked."
                  );  // ???
                } // do nothing because the recursion blocking flag is set.
              else // flag is not set.
              { // process the TreeSelectionEvent.
                TreeSelectionReentryBlockedB= true;  // disallow re-entry.

                valueChangedProcessorV( // Have this do all the work.
                  theTreeSelectionEvent 
                  );

                TreeSelectionReentryBlockedB= false;  // now that we are done, allow re-entry.
                } // process the TreeSelectionEvent.
            } // valueChanged( TreeSelectionEvent theTreeSelectionEvent ) 

        private void valueChangedProcessorV
          ( TreeSelectionEvent theTreeSelectionEvent )
          /* This composition method does the work of valueChanged(..)
            if valueChanged(..) determines that it was not re-entered.
            */
          {
            TreePath selectedTreePath=  // Get...
              theTreeSelectionEvent.  // ...the TreeSelectionEvent's...
                getNewLeadSelectionPath();  // ...one and only TreePath.
            Component sourceComponent=  // Also get its source Component.
              (Component)theTreeSelectionEvent.getSource();
            //appLogger.info(
            //  "DagBrowserPanel.valueChangedProcessorV("
            //  +selectedTreePath.getLastPathComponent()
            //  +")."
            //  );  // ???
            if ( selectedTreePath != null )  // process only if not null.
              { // process based on Source Component.
                if (  // Source is right sub-pannel,...
                    (sourceComponent == DataJComponent) ||  // ... equal to or...
                    DataJComponent.isAncestorOf(  // ...an ancestor of it.
                      sourceComponent)
                      )
                  ProcessSelectionFromRightSubpanel(
                    selectedTreePath,
                    theTreeSelectionEvent.isAddedPath()
                    );
                else if  // Source is left sub-pannel,...
                  (sourceComponent == theRootJTree) // ...equal to RootJTree.
                  ProcessSelectionFromLeftSubpanel( selectedTreePath );
                } // process based on Source Component.
            DisplayPathAndInfoV( ); // display other info.
            Misc.dbgEventDone(); // Debug.
            //appLogger.info("DagBrowserPanel.valueChangedProcessorV(..) End.");  // ???
            }

        private void ProcessSelectionFromLeftSubpanel( TreePath inTreePath )
          /* This always replaces the content of the right sub-panel
            based on inTreePath.

            ??? Change to possibly reuse the present JComponent
            if the right sub-panel JComponent says that it can handle it.
            */
          { // ProcessSelectionFromLeftSubpanel(.)
            //appLogger.info("DagBrowserPanel.ProcessSelectionFromLeftSubpanel(..).");  // 

            // Maybe let RightPanel decide whether to do this??
            ReplaceRightPanelContent( inTreePath );
            } // ProcessSelectionFromLeftSubpanel(.)
            
        private void ProcessSelectionFromRightSubpanel( 
            TreePath inTreePath, boolean internalSelectionB
            )
          /* What this does depends on the internalSelectionB flag.
          
            if true then it simply selects the parent in the left sub-panel.
            
            if false then it replaces the right sub-panel content
            with a new Viewer appropriate to the new selection TreePath,
            and select that TreePath in the left sub-panel.
            */
          { // ProcessSelectionFromRightSubpanel()
            //appLogger.info(    
            //  "DagBrowserPanel.ProcessSelectionFromRightSubpanel(..), "
            //  +"internalSelectionB="+internalSelectionB
            //  );  // ???
            if ( ! internalSelectionB ) // Selection is outside right panel.
              { // Replace the right panel.
                ReplaceRightPanelContent( inTreePath );  // Replace right panel.
                theRootJTree.selectNodeV(  // Select its path in in the left.
                  inTreePath
                  );
                RestoreFocusV();  // Restore right panel's focus.
                } // Replace the right panel.
              else   // Selection is not outside right panel.
              { // Adjust left panel selection to match.
                TreePath SelectedParentTreePath=  // Get path of...
                  inTreePath.getParentPath();  // ...right selection's parent.
                theRootJTree.  // In the left sub-panel...
                  selectNodeV(SelectedParentTreePath);  // ...select that path.
                } // Adjust left panel selection to match.
            } // ProcessSelectionFromRightSubpanel()
            
        private void ReplaceRightPanelContent( TreePath selectedTreePath )
          /* This method calculates a new
            JComponent and TreeAware appropriate for displaying
            the last element of selectedTreePath and sets them
            as content of the right sub-panel for display.
            */
          { // ReplaceRightPanelContent(.)
            //appLogger.info("DagBrowserPanel.ReplaceRightPanelContent().");  // ???
            { // build the scroller content.
              DataJComponent=   // Calculate new JComponent...
                TheDataTreeModel.  // ...by having the TreeModel...
                GetDataJComponent(  // ...generate a JComponent...
                  selectedTreePath  // appropriate to new selection.
                  );
              DataTreeAware= // Calculate TreeAware alias.
                (TreeAware)DataJComponent;
              DataTreeAware.  // set TreeAware's TreeSelectionListener by...
                addTreePathListener(  // adding to its Listener list...
                  this);  // ...a reference to this the main panel
              DataJComponent.addFocusListener(this);  // setup focus restoration.
              } // build the scroller content.
            DataJScrollPane.setViewportView(  // in the DataJScrollPane's viewport...
              DataJComponent);  // ...set the DataJPanel for viewing.
            DataJComponent.repaint();  // make certain it's displayed.
            } // ReplaceRightPanelContent(.)
      
      /* methods of the FocusListener, the FocusStateMachine, and others.  */

        public void focusGained(FocusEvent TheFocusEvent)
          /* This does what needs doing when 
            one of the sub-panels gains focus.  This includes:
            
              Saving the Component getting focus in 
              LastValidFocusOwnerPanelComponent so RestoreFocusV() 
              can restore the focus later after 
              temporary focus-altering user input.
              The two Components that usually have focus in this app are the
              left theRootJTree and the right DataJComponent.
              
              Updating the Path and Info JLabel-s to agree with
              the selection in the new focused sub-panel.
              
            */
          { // focusGained(FocusEvent TheFocusEvent)
            LastValidFocusOwnerPanelComponent=  // record last focus owner panel.
              TheFocusEvent.getComponent();
            
            { // record focused component as an enum because it might change.
              if  // left sub-panel gained focus.
                (LastValidFocusOwnerPanelComponent == theRootJTree)
                { 
                  LastFocusPane= FocusPane.LEFT_PANE;  // record left enum ID.
                  DisplayPathAndInfoV(  // display left sub-panel's info.
                    theRootJTree.getSelectedTreePath()
                    );
                  }
              else if  // right sub-panel gained focus.
                (LastValidFocusOwnerPanelComponent == DataJComponent)
                { 
                  LastFocusPane= FocusPane.RIGHT_PANE;  // record right enum ID.
                  DisplayPathAndInfoV(  // display right sub-panel's info for...
                    DataTreeAware.getPartTreePath() // ...selected TreePath.
                    );
                  }
              else 
                LastFocusPane= FocusPane.NO_PANE;  // record enum ID.
              } // record focused component as an enum because it might change.

            /* ??
            System.out.println(
              "focusGained(...) by"
              + " " + LastFocusPane
              + ComponentInfoString(TheFocusEvent.getComponent()));
            System.out.println(
              "  component losing focus is"
              + ComponentInfoString(TheFocusEvent.getOppositeComponent())
              );
            */
            
            } // focusGained(FocusEvent TheFocusEvent)

        public void focusLost(FocusEvent TheFocusEvent)
          /* This method does nothing, 
            but it must be here as part of the FocusListener interface.
            */
          { 
            /* ??
            System.out.println(
              "focusLost(...) by "
              + ComponentInfoString(TheFocusEvent.getComponent()));
            System.out.println(
              "  component gaining focus is"
              + ComponentInfoString(TheFocusEvent.getOppositeComponent())
              );
            */
            }

        public void RestoreFocusV()
          /* Restores the state of the app focus to one of two states, 
            previously saved by the FocusListener methods, either:
              * the left tree panel has focus, or 
              * the right table panel has focus.
              
            ??? Replace complicated stepper by simple switch
            if Java bug that prevented setting focus is fixed.
            */
          { // RestoreFocusV()
            /* ??
            System.out.println(  // Debug.
              "RestoreFocusV()"
              );
            */
            
            desiredFocusPane=  // change state of the FocusStateMachine to...
              LastFocusPane;  // ... pane that last had focus.
            FocusStepperV();  // start the FocusStateMachine.
            } // RestoreFocusV()

        private void FocusStepperV()
          /* Steps the FocusStateMachine until desired focus is achieved.'
            The first version did nothing and let the Blinker timer step things.
            This version starts a fast self-stepper.
            */
          { // FocusStepperV()
          
            /* 
            System.out.println( 
              "FocusStepperV() " + (++FocusStepCountI)
              );
            */
            
            if (FocusStepB())  //* step the FocusStateMachine and if still running...
              { // queue another step.
                SwingUtilities.invokeLater(new Runnable() {                   
                  @Override  
                  public void run() 
                    {  
                      /* ??
                      System.out.println( 
                        "restarting the FocusStateMachine."
                        );
                      */
                      FocusStepperV();  // restart the FocusStateMachine.
                      }  
                  });
                } // queue another step.
            
            } // FocusStepperV()

        private Boolean FocusStepB()
          /* This method performs one step of the ComponentFocusStateMachine,
            to move the focus one Component closer to desired Component
            in the Component hierarchy.  
            It returns true if the state machine is still running, 
            false otherwise.

            ??? This could be rewritten to simplify and shorten  by replacing
            all the Component-specific code by code which 
            scans Components upward in the hierarchy from the 
            Component which should have focus to
            the Component which does have focus
            and then requesting focus in the previous one scanned
            one level down.
            */
          { // FocusStepB().
            if (desiredFocusPane == FocusPane.NO_PANE)  // machine halted.
              return false;  // return indicating machine is halted.
            Component focusedComponent=  // get Component owning the focus.
              KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
            Component nextFocusComponent= null; // assume no more steps.
            
            /* the following complex code might be replace by
              a Component hierarchy scanning loop.  */
            nextFocusComponent=  // assume focusing starts at...
              ViewJPanel;   // ... the root Component.
            { // override if needed.
              if (focusedComponent == ViewJPanel)
                nextFocusComponent= TheSplitPane;
              else
                { // decode based on desired panel.
                  if (desiredFocusPane == FocusPane.LEFT_PANE)
                    { // step focus toward left pane.
                      if (focusedComponent == TheSplitPane)
                        nextFocusComponent= TreeJScrollPane;
                      else if (focusedComponent == TreeJScrollPane)
                        nextFocusComponent= theRootJTree;
                      else if (focusedComponent == theRootJTree)
                        nextFocusComponent= null;  // end of sequence.  don't change it.
                      } // step focus toward left pane.
                  if (desiredFocusPane == FocusPane.RIGHT_PANE) 
                    { // step focus toward right pane.
                      if (focusedComponent == TheSplitPane)
                        nextFocusComponent= DataJScrollPane;
                      else if (focusedComponent == DataJScrollPane)
                        nextFocusComponent= DataJComponent;
                      else if (focusedComponent == DataJComponent)
                        nextFocusComponent= null;  // end of sequence.  don't change it.
                      } // step focus toward right pane.
                  } // decode based on desired panel.
              } // override  if needed.
                
            if (nextFocusComponent != null)  // focus change desired.
              { // change focus.
                /*
                System.out.println( 
                  "FocusStepB(), "
                  + " " + desiredFocusPane
                  + ComponentInfoString(focusedComponent)
                  + " has focus, "
                  );
                System.out.println( 
                  "  "
                  +ComponentInfoString(nextFocusComponent)
                  + " requests focus."
                  );
                */
                nextFocusComponent.requestFocusInWindow();  // set focus
                } // change focus.
              else  // no focus change desired.
              { // do final focus processing.
                { // now that focus is correct, repaint the two panels.
                  DataJComponent.repaint();  // repaint right data panel.
                  theRootJTree.repaint();  // repaint left tree panel.
                  } // now that focus is correct, repaint the two panels.
                desiredFocusPane= FocusPane.NO_PANE;  // halt state machine.
                } // do final focus processing.
            return  // return an indication of whether ...
              (desiredFocusPane != FocusPane.NO_PANE);  // ... machine still running.
            } // FocusStepB().

    // Key and Action bindings (KeyboardFocusManager ).
    
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

        private void DisplayPathAndInfoV()
          /* This method Updates DirectoryJLabel and InfoJLabel, 
            which appear as two lines above and below the two main sub-panels.
            What it displays depends on which of the two major sub-panel
            has focus and what, if anything, is selected in that panel.
            This method is called whenever something changes
            that might effect these fields.
            */
          {
            TreePath theTreePath= null;  // TreePath to be displayed.
            Component focusedComponent=  // Get Component with focus.
              KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();

            if ( focusedComponent == theRootJTree ) // Left sub-panel.
            {
              //appLogger.info("DagBrowserPanel.DisplayPathAndInfoV() left sub-panel.");  // ???
              theTreePath= theRootJTree.getSelectedTreePath();
              } 
            else if ( focusedComponent == DataJComponent ) // Right sub-panel.
              { // Calculate right sub-panel TreePath.
                //appLogger.info("DagBrowserPanel.DisplayPathAndInfoV() right sub-panel.");  // ???
                theTreePath= DataTreeAware.getPartTreePath();
                if ( theTreePath == null ) // There is no selection TreePath.
                  theTreePath=  // Use subject TreePath instead.
                    DataTreeAware.getWholeTreePath();
                } // Calculate right sub-panel TreePath.
            else // Some other component has the focus.
              {
                //appLogger.info("DagBrowserPanel.DisplayPathAndInfoV() NEITHER sub-panel.");  // ???
                }

            DisplayPathAndInfoV( theTreePath ); // Display chosen TreePath.
            }

        private void DisplayPathAndInfoV(TreePath inTreePath)
          /* This method Updates DirectoryJLabel and InfoJLabel, 
            which appear as two lines above and below the two main sub-panels.
            It displays the string representation of inTreePath in 
            DirectoryJLabel and various subject-dependent info in InfoJLabel.
            This method is called whenever the TreeSelection changes
            or left-panel/right-panel focus changes.
            */
          { // DisplayPathAndInfoV(TreePath inTreePath)
            if (inTreePath == null) // No path was provided.
              { // display null info.
                appLogger.info("DagBrowserPanel.DisplayPathAndInfoV( null )");  // ???
                DirectoryJLabel.setText("NO PATH");
                InfoJLabel.setText("NO INFO AVAILABLE");
                } // display null info.
              else  // A path was provided.
              { // display non-null info.
                DirectoryJLabel.setText(  // in DirectoryJLabel display set...
                  TheDataTreeModel.  // ...DataTreeModel's calculation of...
                    GetAbsolutePathString(  // ...String representation of...
                      inTreePath  // ...of inTreePath.
                    )
                  );
                InfoJLabel.setText(  // set InfoJLabel to be...
                  TheDataTreeModel.  // ...DataTreeModel's calculation of...
                    GetInfoString(inTreePath)  // the info string of inTreePath.
                  );
                } // display non-null info.
            } // DisplayPathAndInfoV(TreePath inTreePath)
          
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
