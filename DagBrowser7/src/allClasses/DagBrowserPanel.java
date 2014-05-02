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
    * Factor DagBrowserPanel() and other methods so each fits on a screen.
    * FocusStepB() simplification using scanning loop.
    * CommandHelpV() replacement with a general PromptingHelp system.
    */

  { // class DagBrowserPanel. 

    // instance variables.

      private Timer blinkerTimer; // Timer that triggers the monitor Blinker.
    
      // data models.
        private DataTreeModel theDataTreeModel;  // holds all browsable data.
        private TreePath startTreePath;  // start TreePath to be displayed.

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
          private FocusPane lastFocusPane=  // The last FocusPane that had focus.
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
          It also starts a blinkerTimer used to indicate 
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
              startTreePath;
            theRootJTree  // In the left sub-panel JTree's...
              .getTreeHelper()  // ...TreeHelper...
              .setPartTreePathV(  // ...select...
                CurrentTreePath  // ...current tree node.
                );
                // This should trigger a series of events which
                // load all the data-dependent sub-panel components
                // and get them ready for display.  
            } // Define the content in the above panels.

          miscellaneousInitializationV();  // Odds and end.

          //appLogger.info("DagBrowserPanel constructor End.(");
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
              theDataTreeModel,  // ...this model for tree data and...
              TreeJScrollPane   // ...JScrollPane for view-ability tests.
              );  // Note that theDataTreeModel was built earlier.

            { // setup handling by listener of various Tree events.
              //theRootJTree.addTreeSelectionListener(this);
              theRootJTree.getTreeHelper().addTreePathListener(this);
              theRootJTree.addFocusListener(this);
              // theRootJTree.addKeyListener(this);
              } // setup handling by listener of various Tree events.
            } // Build the JTree view for the JScrollPane.
          TreeJScrollPane.setViewportView(  // Set client to be scrolled...
            theRootJTree  // ...to be JTree .
            );
          }

      private void buildRightJScrollPaneV()  // ???? needs factoring.
        /* This composition method builds the right JScrollPane
          which contains whatever JComponent is appropriate for
          displaying the item selected in 
          the left JScrollPane navigation pane.
          */
        {
          { // build the scroller content.
            DataJComponent=   // calculate JPanel from TableModel.
              theDataTreeModel.GetDataJComponent(
                startTreePath // initially displaying root/child.
                );  // Note that theDataTreeModel was built earlier.
            DataTreeAware= (TreeAware)DataJComponent;  // Define alias...
            DataTreeAware.getTreeHelper().addTreePathListener(  // ...and Listener.
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

          { // build and start blinkerTimer.
            blinkerTimer = new Timer(  // construct a timer...
              1000,  // ...set to fire once per second...
              this  // ...with the DagBrowserPanel the tick Listener.
              );
            blinkerTimer.start();  // start blinkerTimer so 1-second blinkers will work.
            } // build and start blinkerTimer.
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
          startTreePath= // Initialize startTreePath for browsing...
            Selection.buildAttributeTreePath( );  // ...saved selection state.
          theDataTreeModel =  // Initialize DataTreeModel for JTree using...
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
            the Button-s and the blinkerTimer.
            
            ??? This should be changed so that if the right sub-panel has focus,
            then the triggered commands will happen in the right sub-panel.
            Presently they always happen in the left sub-panel RootJTree.
            
            Also the button panel should be made its own class
            and use aTreeHelper for the commands.
            */
          { // actionPerformed( ActionEvent )
            Object SourceObject= TheActionEvent.getSource();  // get Evemt source.
            if (SourceObject == blinkerTimer)  // try processing timer event.
              ProcessblinkerTimerV();
              else // try processing button press.
              { 
                TreeAware theTreeAware=  // Panel attached to buttons...
                  getFocusedTreeAware();  // ...is the one last focused.
                Boolean ButtonDone= true; // Assume a button action will happen.
                { // Try the various buttons and execute JTree commands.
                  if (SourceObject == LeftArrowIJButton)
                    theTreeAware.getTreeHelper().
                      commandGoToParentB(true);
                  else if (SourceObject == RightArrowIJButton)
                    theTreeAware.getTreeHelper().
                      commandGoToChildB(true);
                  else if (SourceObject == DownArrowIJButton)
                    theTreeAware.getTreeHelper().
                      commandGoDownB(true);
                  else if (SourceObject == UpArrowIJButton)
                    theTreeAware.getTreeHelper().
                      commandGoUpB(true);
                  else if (SourceObject == HelpIJButton)
                    CommandHelpV();  // give help.
                  else
                    ButtonDone= false; // indicate no button action done.
                  } // Try the various buttons and execute JTree commands.
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
        
        private void ProcessblinkerTimerV()
          /* This composition method process one tick of the blinkerTimer.  */
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
          /* This processes TreeSelectionEvent-s.
            It's job is to coordinate selections in 
            the left and right sub-panels.  
            It is very source-panel-dependent.  This includes 

              Passing appropriate TreePath selection information to
              the major left and right sub-panels, 

              Maintaining the correct relationship between 
              what is displayed and selected in the 2 main sub-panels.
              Generally the node being displayed by the right sub-panel
              is the selection of the left sub-panel.

            It also updates DirectoryJLabel and InfoJLabel
            appropriate to the selection in the sub-panel with focus.

            When a TreeSelectionEvent delivers a new TreePath
            it might or might not require that a new JComponent
            be used in the right sub-panel.  
            It is required if :
            1. The JComponent in the right sub-panel
              determines that it can not display the new TreePath.
            2. The new TreePath is not a descendant of
              the TreePath currently displayed by 
              the JComponent in the right sub-panel

            Most the work of this method id done by partTreeChangedProcessorV(..),
            but this called only if it determined that 
            valueChanged(..) was not re-entered.
            */
          {
            if ( TreeSelectionReentryBlockedB ) // Process unless a re-entry.
              { // do nothing because the re-entry blocking flag is set.
                appLogger.info(
                  "DagBrowserPanel.valueChanged(..), re-entry blocked."
                  );
                } // do nothing because the recursion blocking flag is set.
              else // flag is not  set.
              { // process the TreeSelectionEvent.
                TreeSelectionReentryBlockedB= true;  // Disable re-entry.

                partTreeChangedProcessorV( // Have this do all the work.
                  theTreeSelectionEvent 
                  );

                TreeSelectionReentryBlockedB= false;  // Enable re-entry.
                } // process the TreeSelectionEvent.
            }

        private void partTreeChangedProcessorV
          ( TreeSelectionEvent theTreeSelectionEvent )
          /* This composition method does the work of partTreeChangedV(..).
            See that method for more information.
            */
          {
            TreePath selectedTreePath=  // Get...
              theTreeSelectionEvent.  // ...the TreeSelectionEvent's...
                getNewLeadSelectionPath();  // ...one and only TreePath.
            Component sourceComponent=  // Also get its source Component.
              (Component)theTreeSelectionEvent.getSource();
            if ( selectedTreePath != null )  // process only if not null.
              { // Process non-null path.
                { // process based on Source Component.
                  if (  // Source is right sub-pannel,...
                      (sourceComponent == DataJComponent) ||  // ... equal to or...
                      DataJComponent.isAncestorOf(  // ...an ancestor of it.
                        sourceComponent)
                        )
                    ProcessSelectionFromRightSubpanel(
                      selectedTreePath
                      );
                  else if  // Source is left sub-pannel,...
                    (sourceComponent == theRootJTree) // ...equal to RootJTree.
                    ProcessSelectionFromLeftSubpanel( selectedTreePath );
                  } // process based on Source Component.
                RecordPathSelectionV( );
                } // Process non-null path.
            DisplayPathAndInfoV( ); // Update the display of other info.
            Misc.dbgEventDone(); // Debug.
            }

        private void RecordPathSelectionV( )
          /* This records the selection from the always replaces the content of the right sub-panel
            based on inTreePath.
            */
          {
            TreeAware theTreeAware= getFocusedTreeAware();
            TreePath theTreePath= 
              theTreeAware.getTreeHelper().getPartTreePath();
            Selection.set( theTreePath );  // Record as a selection.
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

        private TreePath oldPanelTreePath; // Previous right-panel TreePath.
          // ??? DataRoot.getParentOfRootTreePath( );  // An impossible value.

        private void ProcessSelectionFromRightSubpanel( TreePath inTreePath )
          /* What this does depends on inTreePath.
          
            If the new inTreePath from the right panel 
            is a sibling of the old TreePath then it does nothing 
            [or maybe selects the parent in the left sub-panel).
            
            If the new inTreePath is not a sibling of the old TreePath then 
            it replaces the right sub-panel content
            with a new Viewer appropriate to the new selection TreePath,
            and selects the appropriate TreePath in the left sub-panel.
            */
          { // ProcessSelectionFromRightSubpanel()
            boolean siblingsB=  // Is new path a sibling of the old path?
              inTreePath.getParentPath().equals(oldPanelTreePath);
            if ( siblingsB ) // New and old Part paths have same parent.
              ; // Nothing needs to be done.
              else // New and old selections do NOT have same parent.
              { // Replace right panel and update things.
                TreePath panelTreePath= inTreePath;
                panelTreePath= panelTreePath.getParentPath();
                ReplaceRightPanelContent(   // Replace right panel.
                  panelTreePath
                  );
                RestoreFocusV();  // Restore right panel's focus.
                theRootJTree  // In the left sub-panel JTree's...
                  .getTreeHelper()  // ...TreeHelper...
                  .setPartTreePathV(  // ...select...
                    panelTreePath  // ...appropriate path.
                    );
                } // Replace right panel and update things.
            } // ProcessSelectionFromRightSubpanel()

        private void ReplaceRightPanelContent( TreePath inTreePath )
          /* This method calculates a new
            JComponent and TreeAware appropriate for displaying
            the last element of inTreePath and sets them
            as content of the right sub-panel for display.
            */
          { // ReplaceRightPanelContent(.)
            //appLogger.info("DagBrowserPanel.ReplaceRightPanelContent().");
            { // build the scroller content.
              DataJComponent=   // Calculate new JComponent...
                theDataTreeModel.  // ...by having the TreeModel...
                GetDataJComponent(  // ...generate a JComponent...
                  inTreePath  // appropriate to new selection.
                  );
              DataTreeAware= // Calculate TreeAware alias.
                (TreeAware)DataJComponent;
              DataTreeAware.getTreeHelper().  // set TreeAware's TreeSelectionListener by...
                addTreePathListener(  // adding to its Listener list...
                  this);  // ...a reference to this the main panel
              DataJComponent.addFocusListener(this);  // setup focus restoration.
              } // build the scroller content.
            DataJScrollPane.setViewportView(  // in the DataJScrollPane's viewport...
              DataJComponent);  // ...set the DataJPanel for viewing.
            DataJComponent.repaint();  // make certain it's displayed.
            oldPanelTreePath= inTreePath;  // Save path for compares later.
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
                  lastFocusPane= FocusPane.LEFT_PANE;  // record left enum ID.
                  DisplayPathAndInfoV(  // display left sub-panel's info.
                    theRootJTree.getSelectedTreePath()
                    );
                  }
              else if  // right sub-panel gained focus.
                (LastValidFocusOwnerPanelComponent == DataJComponent)
                { 
                  lastFocusPane= FocusPane.RIGHT_PANE;  // record right enum ID.
                  DisplayPathAndInfoV(  // display right sub-panel's info for...
                    DataTreeAware.getTreeHelper().getPartTreePath() // ...selected TreePath.
                    );
                  }
              else 
                lastFocusPane= FocusPane.NO_PANE;  // record enum ID.
              } // record focused component as an enum because it might change.

            /* ??
            System.out.println(
              "focusGained(...) by"
              + " " + lastFocusPane
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
              lastFocusPane;  // ... pane that last had focus.
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
            one level down.  This could make use of getFocusedTreeAware().
            */
          { // FocusStepB().
            if (desiredFocusPane == FocusPane.NO_PANE)  // machine halted.
              return false;  // return indicating machine is halted.
            Component focusedComponent=  // get Component owning the focus.
              KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
            Component nextFocusComponent= null; // assume no more steps.
            
            /* The following complex code might be replace by
              a Component hierarchy scanning loop ???  */
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
            return  // return an indication of whether machine still running.
              (desiredFocusPane != FocusPane.NO_PANE); 
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

        public TreeAware getFocusedTreeAware()
          /* This method returns a reference to the TreeAware JComponent
            which last had focus and will probably have it again if
            it was taken away by a button click or dialog box activation.
            It is called for restoring focus and for recording 
            final correct Selections in the MetaNode tree.
            */
          {
            TreeAware resultTreeAware;
            switch ( lastFocusPane ) { // Calculate from last focused pane.
              case RIGHT_PANE:
                resultTreeAware= (TreeAware)DataJComponent; break;
              case LEFT_PANE:
              case NO_PANE:
              default:
                resultTreeAware= theRootJTree; break;
              } // Calculate from last focused pane.
            return resultTreeAware;
            }

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
                //appLogger.info("DagBrowserPanel.DisplayPathAndInfoV() left sub-panel.");
                theTreePath= theRootJTree.getSelectedTreePath();
                } 
            else if ( focusedComponent == DataJComponent ) // Right sub-panel.
              { // Calculate right sub-panel TreePath.
                //appLogger.info("DagBrowserPanel.DisplayPathAndInfoV() right sub-panel.");
                theTreePath= DataTreeAware.getTreeHelper().getPartTreePath();
                if ( theTreePath == null ) // There is no selection TreePath.
                  theTreePath=  // Use subject TreePath instead.
                    DataTreeAware.getTreeHelper().getWholeTreePath();
                } // Calculate right sub-panel TreePath.
            else // Some other component has the focus.
              {
                //appLogger.info("DagBrowserPanel.DisplayPathAndInfoV() NEITHER sub-panel.");
                }

            if ( theTreePath != null )  // Display only if not null.
              DisplayPathAndInfoV( theTreePath ); // Display chosen TreePath.
            }

        private void DisplayPathAndInfoV(TreePath inTreePath)
          /* This method Updates DirectoryJLabel and InfoJLabel, 
            which appear as two lines above and below the two main sub-panels.
            It displays the string representation of inTreePath 
            in DirectoryJLabel, and various attributes of 
            the final DataNode of that path in InfoJLabel.
            This method is called whenever the TreeSelection changes
            or left-panel/right-panel focus changes.
            But it doesn't display information on an ErrorDataNode.
            If there are any ErrorDataNode-s at the end of the TreePath
            then it removes them first.
            This means that more than one path could display the same way.
            */
          { // DisplayPathAndInfoV(TreePath inTreePath)
            if (inTreePath == null) // No path was provided.
              { // display null info.
                appLogger.info("DagBrowserPanel.DisplayPathAndInfoV( null )");
                DirectoryJLabel.setText("NO PATH");
                InfoJLabel.setText("NO INFO AVAILABLE");
                } // display null info.
              else  // A path was provided.
              { // display non-null info.
                while // Strip all error nodes from tail of TreePath.
                  ( inTreePath.getLastPathComponent() 
                    == ErrorDataNode.getSingletonErrorDataNode() 
                    )  // Last elemenr is the ErrorDataNode.
                  inTreePath= inTreePath.getParentPath();  // Strip the node.
                DirectoryJLabel.setText(  // in DirectoryJLabel display set...
                  theDataTreeModel.  // ...DataTreeModel's calculation of...
                    GetAbsolutePathString(  // ...String representation of...
                      inTreePath  // ...of inTreePath.
                    )
                  );
                InfoJLabel.setText(  // set InfoJLabel to be...
                  theDataTreeModel.  // ...DataTreeModel's calculation of...
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
