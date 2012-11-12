package allClasses;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout; // not recommended by Eclipse QuickFix.
import java.awt.Font;
import java.awt.Graphics;
import java.awt.KeyboardFocusManager;
import java.awt.event.*;

import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;


public class DagBrowserPanel
  extends JPanel 
  implements 
    ActionListener, 
    FocusListener,
    TreeSelectionListener   
    //, KeyListener, MouseListener

  /* This class implements a panel which allows a user to browse 
    the Infogora DAG (Directed Acyclic Graph).
    The left main subpanel is the navigation panel
    and displays the graph as a JTree.
    The right main subpanel displays nodes of the graph (DagNode-s)
    in a way appropriate to the type of node.
    */

  { // class DagBrowserPanel. 

    // static variables.
      private static final long serialVersionUID = 1L;

    // instance variables.

      private Timer BlinkerTimer; // Timer that triggers the monitor Blinker.
    
      // data models.
        private RootTreeModel TheRootTreeModel;  // holds all browsable data.
        private TreePath StartTreePath;  // start TreePath to be displayed.

      // displayed subpanel JComponent tree.
            
        private JPanel HTopJPanel; // top horizontal panel for...
          private IJButton HelpIJButton;  // button to activate Help dialog.
          private IJButton ToParentIJButton;  // button to move to Parent folder.
          private JLabel BlinkerJLabel; // window monitor status.
          //private JTextArea DebugJTextArea; // for Debug output info.
          //private JLabel PaintCountJLabel; // a place to display paint count.
          //  int PaintCountI;  // window paint count and...
          
        private JPanel ViewJPanel; // JPanel where desired data is diaplayed.
          private JLabel DirectoryJLabel; // a place to display directory path.

          private JSplitPane TheSplitPane;  // horizontally split content panel
            private JScrollPane TreeJScrollPane;  // left scroller subpanel...
              private RootJTree TheRootJTree;  // ... and tree content.
            private JScrollPane DataJScrollPane;  // right scroller subpanel...
              private JComponent DataJComponent;  // ... and its data content.
              private VHelper DataVHelper;  // ... and its VHelper handle.
              // private JTable DataJComponent;  // was only table before.

          private JLabel InfoJLabel;  // a place to display directory/file info.

      /* ComponentFocusStateMachine.
        This restores the focus to either the left/navigation pane
        or the right/content pain.  */
        private enum FocusPane {   // the normal focus panes.
          NO_PANE,  // neither pane.
          LEFT_PANE,  // left (tree view) pane.
          RIGHT_PANE   // right (content view) pane.
          };
        private FocusPane LastFocusPane=  // The last FocusPane that had focus.
          FocusPane.LEFT_PANE;  // Set to focus right pane initially.
          // was: FocusPane.RIGHT_PANE;  // Set to focus right pane initially.
        private FocusPane DesiredFocusPane=  // The next pane to get focus...
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
          
          This is too long and should be broken up.  ??
          */
        { // DagBrowserPanel()
          { // build and add subpanels of this Panel. ????
            setLayout(new BorderLayout());  // use BorderLayout manager.
            BuildTreeModels(); 
              // this will be used as the source of tree data,
            { // build HTopJPanel containing buttons and other helpful widgets.
            
              HTopJPanel= new JPanel();  // construct it.
              HTopJPanel.setLayout(new WrapLayout());  // set Layout manager.
              HTopJPanel.setAlignmentX(Component.LEFT_ALIGNMENT);  // set alignment.
              ((FlowLayout)HTopJPanel.getLayout()).setHgap(20); // spread components.
              
              { // create and add HelpIJButton.
                HelpIJButton= new IJButton("Help");  // create Help button.
                HTopJPanel.add(HelpIJButton);  // add it to JPanel.
                HelpIJButton.addActionListener(this);
                } // create and add HelpIJButton.
              { // create and add ToParentIJButton.
                ToParentIJButton= new IJButton("To-Parent");  // create button.
                HTopJPanel.add(ToParentIJButton);  // add it to JPanel.
                ToParentIJButton.addActionListener(this);
                } // create and add ToParentIJButton.
              { // create and add BlinkerJLabel.
                BlinkerJLabel= new JLabel("Active");  // for testing.
                BlinkerJLabel.setOpaque(true);  // set opaque for use as blinker.
                BlinkerJLabel.setBackground(Color.WHITE);  // override doesn't work.
                BlinkerJLabel.setFont(new Font("Monospaced",Font.BOLD,16)); // set different font.
                HTopJPanel.add(BlinkerJLabel);
                } // create and add BlinkerJLabel.
              { // create and add Key code debug output areas.
                // HTopJPanel.add( new JLabel("DebugOut:")); // add key code label.
                // HTopJPanel.add(DebugJTextArea= new JTextArea("Debug"));
                // DebugJTextArea.setEditable(false);
                // DebugJTextArea.setRequestFocusEnabled(false);
                // DebugJTextArea.setFocusable(false);
                } // create and add Key code debug output areas.
              // { // create and add PaintCountJLabel debug output area.
              //   PaintCountJLabel= new JLabel("X");  // create place for paint count.
              //   HTopJPanel.add(PaintCountJLabel);  // add it.
              //   } // create and add PaintCountJLabel debug output area.
              } // build HTopJPanel containing buttons and other helpful widgets.
            add(HTopJPanel,BorderLayout.NORTH); // add it as north subpanel
            { // build ViewJPanel for showing file info.
              ViewJPanel= new JPanel();  // construct ViewJPanel.
              ViewJPanel.setLayout(new BorderLayout());  // set BorderLayout manager.
              { // build Current Working DirectoryJLabel.
                DirectoryJLabel= new JLabel();  // create CWD JLabel.
                DirectoryJLabel.setAlignmentX(Component.LEFT_ALIGNMENT);  // align it.
                } // create Current Working DirectoryJLabel.
              ViewJPanel.add(DirectoryJLabel,BorderLayout.NORTH);  // add as north subpanel.
              { // build JSplitPane to be used as content.
                // note that TheRootTreeModel was built earlier.
                { // build the left scroller subpanel.
                  { // build the file system tree panel.
                    TheRootJTree = 
                      new RootJTree( TheRootTreeModel );
                    { // setup handling by listener of various Tree events.
                      TheRootJTree.addTreeSelectionListener(this);
                      TheRootJTree.addFocusListener(this);
                      // TheRootJTree.addKeyListener(this);
                      } // setup handling by listener of various Tree events.
                    } // build the file system tree panel.
                  TreeJScrollPane = new JScrollPane( // construct JScrollPanel to scroll
                    TheRootJTree );  // file system tree.
                  TreeJScrollPane.setMinimumSize( new Dimension( 0, 0 ) );
                  TreeJScrollPane.setPreferredSize( new Dimension( 400, 400 ) );
                  }// build the left scroller subpanel.
                { // build the right scroller subpanel.
                  { // build the scroller content.
                    DataJComponent=   // calculate JPanel from TableModel.
                      TheRootTreeModel.GetDataJComponent(
                        StartTreePath
                        ); // initially displaying root/child.
                    //DataJComponent=   // calculate JComponent from VHelper.
                    //  DataVHelper.GetContentJComponent();
                    DataVHelper= (VHelper)DataJComponent;
                    DataVHelper.addTreeSelectionListener(  // make handler of tree selectionss...
                      this);  // ...be this.
                    } // build the scroller content.
                  DataJScrollPane = new JScrollPane( DataJComponent );  // construct scroller from data panel.
                  DataJScrollPane.setMinimumSize( new Dimension( 0, 0 ) );
                  DataJScrollPane.setPreferredSize( new Dimension( 400, 400 ) );
                  DataJScrollPane.setBackground( Color.white );
                  } // build the right scroller subpanel.
                TheSplitPane = // construct...
                  new JSplitPane(  // ... JSplitPane...
                    JSplitPane.HORIZONTAL_SPLIT,  // ...split horizontally from...
                    TreeJScrollPane,  // ...left scroller subpanel and...
                    DataJScrollPane  // ...right scroller subpanel.
                    );
                TheSplitPane.setContinuousLayout( true );  // enable continuous layoug mode.
                } // build JSplitPane to be used as content.
              ViewJPanel.add(TheSplitPane,BorderLayout.CENTER); // add TheJSplitPane as...
                                                                // ... the center subpanel.
              { // build InfoJLabel for displaying file information.
                InfoJLabel= new JLabel();  // construct it.
                InfoJLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
                InfoJLabel.setText("UNDEFINED");  // report the present info string.
                } // build InfoJLabel for displaying file information.
              ViewJPanel.add(InfoJLabel,BorderLayout.SOUTH);  // add as south subpanel.
              } // build ViewJPanel for showing file info.
            add(ViewJPanel,BorderLayout.CENTER);  // add it as center subpanel.
            } // build and add subpanels of this Panel.
          { // define the content in the above panels.
            TreePath CurrentTreePath=  // Get TreePath of starting node.
              StartTreePath;
            TheRootJTree.  // In the left subpanel...
              SelectNodeV(CurrentTreePath);  // ...select current tree node.
            /* In theory, the above should trigger a series of events which
              will load both the left and right subpanels with 
              coordinated content which is ready for display.  
              */
            } // define the content in the above panels.
          { // build and start BlinkerTimer.
            BlinkerTimer = new Timer(  // construct a timer...
              1000,  // ...set to fire once per second...
              this  // ...with the DagBrowserPanel the tick Listener.
              );
            BlinkerTimer.start();  // start BlinkerTimer so 1-second blinkers will work.
            } // build and start BlinkerTimer.
          LastValidFocusOwnerPanelComponent=   // initialize ID of last component with focus.
            // TheRootJTree.getTree();
            TheRootJTree;  // was DataJComponent;            
          RestoreFocusV(); // make certain focus is set correctly.
          SwingUtilities.invokeLater(new Runnable() {
            @Override  
            public void run() 
              {  
                CommandHelpV();  // give help.
                }  
            });
          } // DagBrowserPanel()

      private void BuildTreeModels()
        /* This grouping method builds the TreeModel
          which will be the source of data to be browsed.
          It also builds the DagInfo cache.
          */
        { // BuildTreeModels()
          StartTreePath= // Calculate and save StartTreePath for browsing...
            //IFile.TreePathStart(); // ... IFiles.
            //new TreePath( new StringObject( "TEST-STRING" ) );
            //FileRoots.TreePathStart(); // ... FileRoots.
            new TreePath( new InfogoraRoot( ) );   // BEING ADDED.

          DagNode DagNodeRoot= // Extract tree root...
            (DagNode)
            StartTreePath.getPathComponent( 0 );  // ...from StartTreePath.

          TheRootTreeModel =  // build TreeModel from tree root Object.
            new RootTreeModel(DagNodeRoot); 

          new DagInfo(  // initialize DagInfo from StartTreePath.
            StartTreePath
            );
            
          } // BuildTreeModels()

    // Listener methods.
  
      /* ActionListener and related methods, for processing ActionEvent-s from 
        buttons and timers.
        */
      
        public void actionPerformed(ActionEvent TheActionEvent) 
          /* This method processes ActionsEvent-s from 
            the Button-s and the BlinkerTimer.
            */
          { // actionPerformed( ActionEvent )
            Object SourceObject= TheActionEvent.getSource();  // get Evemt source.
            if (SourceObject == BlinkerTimer)  // try processing timer event.
              ProcessBlinkerTimerV();
              else // try processing button press.
              { 
                Boolean ButtonDone= true; // assume we will do a button action.
                { // try the various buttons.
                  if (SourceObject == ToParentIJButton)
                    // CommandDummyGoToParentV();  // go to parent folder.
                    TheRootJTree. // delegate to JTree panel and...
                      CommandGoToParentV();  // ...its GoToParent command.
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
          /* This grouping method impliments the Help command.  */
          { // give help.
            JOptionPane.showMessageDialog(
              this, // null, 
              "Use Arrow keys to navigate folders.\n"+
              "      <Right-arrow> moves to child item.\n"+
              "      <Left-arrow> moves to parent item.\n"+
              "      <Down-arrow> moves to next item.\n"+
              "      <Up-arrow> moves to previous item\n"+
              " <Tab> selects next window gadget.\n"+
              " <Enter> manually expands or collapses an item.",
              "Help",
              JOptionPane.INFORMATION_MESSAGE
              );
            } // give help.
        
        private void ProcessBlinkerTimerV()
          /* This grouping method process one tick of the BlinkerTimer.  */
          { // process timer event.
            { // switch blinker color.
              BlinkerJLabel.setOpaque(!BlinkerJLabel.isOpaque());  // reverse opacity.
              BlinkerJLabel.repaint();  // request redisplay of it only.
              } // switch blinker color.
            // PaintCountJLabel.setText("Paints="+PaintCountI);  // store present paint count.
            // PaintCountJLabel.repaint();  // request redisplay of it only.
            } // process timer event.

      /* TreeSelectionListener methods, for when TreeSelectionEvent-s
        happen in either the left or right panel.
        */
        
        public void valueChanged( TreeSelectionEvent TheTreeSelectionEvent ) 
          /* This processes TreeSelectionEvent-s.
            It's job is to coordinate selections in 
            the left and right subpanels.  
            This includes 
              
              Passing appropriate TreePath selection information to
              the major left and right subpanels, 
              
              Maintaining the parent-child relationship between them.
              The left subpanel selection should be the parent of
              the right subpanel selection.
              
            It also updates DirectoryJLabel and InfoJLabel
            appropriate to the selection with focus.
            
            The TreeSelectionEvent from the right subpanel can mean either:
            1. a change of child selection within the subpanel.
            2. complete replacement of what the subpanel is displaying
            It might be better to create a custom Event for 
            right subpanel Viewer-s instead of using TreeSelectionEvent-s. ??
            */
          { // valueChanged( TreeSelectionEvent TheTreeSelectionEvent ) 
            if ( TreeSelectionReentryBlockedB )  // process unless a reentry. 
              { // do nothing because the reentry blocking flag is set.
                } // do nothing because the recursion blocking flag is set.
              else // flag is not set.
              { // process the TreeSelectionEvent.
                TreeSelectionReentryBlockedB= true;  // disallow re-entry.

                TreePath SelectedTreePath=
                  TheTreeSelectionEvent.  // ...the TreeSelectionEvent's...
                    getNewLeadSelectionPath();  // ...one and only TreePath.
                if ( SelectedTreePath != null )  // process only if not null.
                  { // process based on Source.
                    if (TheTreeSelectionEvent.getSource() == DataJComponent)
                      ProcessTreeSelectionFromRightSubpanel(
                        SelectedTreePath,
                        TheTreeSelectionEvent.isAddedPath()
                        );
                    else if (TheTreeSelectionEvent.getSource() == TheRootJTree)
                      ProcessTreeSelectionFromLeftSubpanel( SelectedTreePath );
                    } // process based on Source.
                DisplayPathAndInfoV(SelectedTreePath); // display other info.

                TreeSelectionReentryBlockedB= false;  // now that we are done, allow re-entry.
                } // process the TreeSelectionEvent.
            } // valueChanged( TreeSelectionEvent TheTreeSelectionEvent ) 

        private void ProcessTreeSelectionFromLeftSubpanel
          /* This replaces the content of the right subpanel
            based on the new selection in the left subpanel.
            This is all that can be done in the general case.
            */
          ( TreePath InSelectedTreePath )
          { // ProcessTreeSelectionFromLeftSubpanel(.)
            // System.out.println( 
            //   "TreeSelectionListener DagBrowserPanel.valueChanged(...), from left panel"
            //   );
            ReplaceRightPanelContent( InSelectedTreePath );
            } // ProcessTreeSelectionFromLeftSubpanel(.)
            
        private void ProcessTreeSelectionFromRightSubpanel
          ( TreePath InSelectedTreePath,
            boolean InternalSelectionB
            )
          /* What this does depends on the InternalSelectionB flag.
          
            if true then it simply selects the parent in the left subpanel.
            
            if false then it replaces the right subpanel content
            with a new Viewer appropriate to the new selection TreePath,
            and select that TreePath in the left subpanel.
            */
          { // ProcessTreeSelectionFromRightSubpanel()
            // System.out.println( 
            //   "TreeSelectionListener DagBrowserPanel.valueChanged(...), from right panel"
            //   );
            if // redo right panel if selection is outside it.
              ( ! InternalSelectionB )
              { ReplaceRightPanelContent( InSelectedTreePath );
                TheRootJTree.  // in the right subpanel...
                  SelectNodeV(InSelectedTreePath);
                RestoreFocusV();
                }
              else
              { TreePath SelectedParentTreePath=  // get parent.
                  InSelectedTreePath.getParentPath();
                TheRootJTree.  // in the right subpanel...
                  SelectNodeV(SelectedParentTreePath);  // ...select parent.
                }
            } // ProcessTreeSelectionFromRightSubpanel()
            
        private void ReplaceRightPanelContent( TreePath SelectedTreePath )
          /* This method calculates a new
            JComponent and VHelper appropriate for displaying
            the last element of SelectedTreePath and sets them
            as content of the right subpanel for display.
            */
          { // ReplaceRightPanelContent(.)
            { // build the scroller content.
              DataJComponent=   // calculate JComponent...
                TheRootTreeModel.  // ...by having the TreeModel...
                GetDataJComponent(  // ...generate a JComponent...
                  SelectedTreePath  // appropriate to new selection.
                  );
              DataVHelper= // calculate VHelper version.
                (VHelper)DataJComponent;
              DataVHelper.  // set VHelper's TreeSelectionListener by...
                addTreeSelectionListener(  // adding to its Listener list...
                  this);  // ...a reference to this the main panel
              DataJComponent.addFocusListener(this);  // setup focus restoration.
              } // build the scroller content.
            DataJScrollPane.setViewportView(  // in the DataJScrollPane's viewport...
              DataJComponent);  // ...set the DataJPanel for viewing.
            DataJComponent.repaint();  // make certain it's displayed.
            } // ReplaceRightPanelContent(.)
      
      /* methods of the FocusListener, the FocusStateMachine, and others.  */

        public void focusGained(FocusEvent TheFocusEvent)
          /* This does what needs doing when one of the subpanels gains focus.
            This includes:
            
              Saving the Component getting focus in 
              LastValidFocusOwnerPanelComponent so RestoreFocusV() 
              can restore the focus later after temporary focus-altering user input.
              The two Components that usually have focus in this app are the
              left JTreePanel and the right DataJComponent.
              
              Updating the Path and Info JLabel-s to agree with
              the selection in the new focused subpanel.
              
            */
          { // focusGained(FocusEvent TheFocusEvent)
            LastValidFocusOwnerPanelComponent=  // record last focus owner panel.
              TheFocusEvent.getComponent();
            
            { // record focused component as an enum because it might change.
              if  // left subpanel gained focus.
                (LastValidFocusOwnerPanelComponent == TheRootJTree)
                { 
                  LastFocusPane= FocusPane.LEFT_PANE;  // record left enum ID.
                  DisplayPathAndInfoV(  // display left subpanel's info.
                    TheRootJTree.GetSelectedTreePath()
                    );
                  }
              else if  // right subpanel gained focus.
                (LastValidFocusOwnerPanelComponent == DataJComponent)
                { 
                  LastFocusPane= FocusPane.RIGHT_PANE;  // record right enum ID.
                  DisplayPathAndInfoV(  // display right subpanel's info for...
                    DataVHelper.GetSelectedChildTreePath() // ...selected TreePath.
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
            */
          { // RestoreFocusV()
            /* ??
            System.out.println(  // Debug.
              "RestoreFocusV()"
              );
            */
            
            DesiredFocusPane=  // change state of the FocusStateMachine to...
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
          /* performs one step of the ComponentFocusStateMachine,
            to move the focus one Component closer to desired Component
            in the Component hierarchy.  
            returns true if the state machine is still running, false otherwise.
            */
          { // FocusStepB().
            // System.out.println( "FocusStepB(), 1");
            if (DesiredFocusPane == FocusPane.NO_PANE)  // machine halted.
              return false;  // return indicating machine is halted.
              
            // System.out.println( "FocusStepB(), 2");
            Component FocusOwningComponent=  // get Component owning the focus.
              KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
            // if (FocusOwningComponent == null)  // no component has focus.
            //   return false;  // return indicating machine is halted.
             
            // System.out.println( "FocusStepB(), 3");
            Component NextFocusComponent= null; // assume no more steps.
            
            /* the following complex code might be replace by
              a Component hierarchy scanning loop.  */

            NextFocusComponent= ViewJPanel;  // assume focusing starts at root.
            { // override if needed.
              if (FocusOwningComponent == ViewJPanel)
                NextFocusComponent= TheSplitPane;
              else
                { // decode based on desired panel.
                  if (DesiredFocusPane == FocusPane.LEFT_PANE)
                    { // step focus toward left pane.
                      if (FocusOwningComponent == TheSplitPane)
                        NextFocusComponent= TreeJScrollPane;
                      else if (FocusOwningComponent == TreeJScrollPane)
                        NextFocusComponent= TheRootJTree;
                      // else if (FocusOwningComponent == TheRootJTree)
                      //   NextFocusComponent= TheRootJTree.getTree();
                      else if (FocusOwningComponent == TheRootJTree)
                        NextFocusComponent= null;  // end of sequence.  don't change it.
                      } // step focus toward left pane.
                  if (DesiredFocusPane == FocusPane.RIGHT_PANE) 
                    { // step focus toward right pane.
                      if (FocusOwningComponent == TheSplitPane)
                        NextFocusComponent= DataJScrollPane;
                      else if (FocusOwningComponent == DataJScrollPane)
                        NextFocusComponent= DataJComponent;
                      else if (FocusOwningComponent == DataJComponent)
                        NextFocusComponent= null;  // end of sequence.  don't change it.
                      } // step focus toward right pane.
                  } // decode based on desired panel.
              } // override  if needed.
                
            if (NextFocusComponent != null)  // focus change desired.
              { // change focus.
                /*
                System.out.println( 
                  "FocusStepB(), "
                  + " " + DesiredFocusPane
                  + ComponentInfoString(FocusOwningComponent)
                  + " has focus, "
                  );
                System.out.println( 
                  "  "
                  +ComponentInfoString(NextFocusComponent)
                  + " requests focus."
                  );
                */
                NextFocusComponent.requestFocusInWindow();  // set focus
                } // change focus.
              else  // no focus change desired.
              { // do final focus processing.
                { // now that focus is correct, repaint the two panels.
                  DataJComponent.repaint();  // repaint right data panel.
                  TheRootJTree.repaint();  // repaint left tree panel.
                  } // now that focus is correct, repaint the two panels.
                // System.out.println( "FocusStepB(), focusing complete" );
                DesiredFocusPane= FocusPane.NO_PANE;  // halt state machine.
                } // do final focus processing.

            return  // return an indication of whether ...
              (DesiredFocusPane != FocusPane.NO_PANE);  // ... machine still running.
            } // FocusStepB().

    // miscellaneous methods.

      private void DisplayPathAndInfoV(TreePath InTreePath)
        /* Displays the absolute path and attributes of
          the TreePath InTreePath in the two lines above and below
          the two main subpanels.
          This method is called whenever the TreeSelection changes
          or left-panel/right-panel focus changes.
          */
        { // DisplayPathAndInfoV(TreePath InTreePath)
          if (InTreePath == null)
            { // display null info.
              DirectoryJLabel.setText("NO PATH");
              InfoJLabel.setText("NO INFO");
              } // display null info.
            else
            { // display non-null info.
              DirectoryJLabel.setText(  // in DirectoryJLabel display set...
                TheRootTreeModel.  // ...TreeModel's calculation of...
                GetAbsolutePathString( InTreePath ) // ...absolute path name of InTreePath.
                );
              InfoJLabel.setText(  // set InfoJLabel to be...
                TheRootTreeModel.  // ...TreeModel's calculation of...
                GetInfoString(InTreePath)  // the info string of InTreePath.
                );
              } // display non-null info.
          } // DisplayPathAndInfoV(TreePath InTreePath)
          
      public void paintComponent(Graphics g)
        /* The paintComponent() method draws the current state of the app pane.  
          It isn't called as often as one might think, for example,
          when the app pane is uncovered,
          apparently because the display is restored from a saved image.
          This method exists mostly for debugging.
          */
        {
          // PaintCountI++;  // increment app pane paint count.
          super.paintComponent(g);  // let JPanel do most of the work..
          } // end drawFrame()

    }  // class DagBrowserPanel.

// end of file.
