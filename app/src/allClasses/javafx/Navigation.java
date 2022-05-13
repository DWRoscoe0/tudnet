package allClasses.javafx;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;


import static allClasses.AppLog.theAppLog;
import static allClasses.SystemSettings.NL;

import allClasses.DataNode;
import allClasses.DataRoot;
import allClasses.Dialogger;
import allClasses.EpiThread;
import allClasses.NamedLeaf;
import allClasses.NamedList;
import allClasses.Persistent;
import allClasses.Shutdowner;
import allClasses.epinode.MapEpiNode;

public class Navigation extends EpiStage

  {

    /* This class is, or eventually will be,
     * for navigation of the TUDNet DataNode hierarchy.
     * It displays the hierarchy in 1 of 2 ways:
     * * showing the hierarchy as a navigable tree using a TreeView, or 
     * * showing a particular DataNode and possibly some of its descendants,
     *   often but not always as a simple list of text lines.
     * At startup it displays the DataNode that was being displayed at shutdown.
     * 
     * ///ano Sometimes at startup this window fails to display.
     *   This has happened when trying to display an empty directory as a node.
     *   Switching to tree display mode before startup enabled display,
     *   and then the window could be switched back and forth between modes.
     *   After that, it could start in either mode and display correctly.
     *
     *
     * ///fix To prevent Listener leak, in Finalization,
     *   remove ConsoleNode Document change listener from ConsoleBase 
     *
     * ///enh Learn about Insets, Outsets, and Border and use
     * to make better looking labels.
     *
     */


    // Injected dependency variables.
    private final DataNode theRootDataNode;
    private final Persistent thePersistent;
    private DataRoot theDataRoot;
    private Selections theSelections;


    // Code for construction.

    public Navigation( // Simple object constructor.
        JavaFXGUI theJavaFXGUI, 
        DataNode theRootDataNode,
        Persistent thePersistent,
        DataRoot theDataRoot,
        Selections theSelections,
        Shutdowner theShutdowner
        )
      {
        super(theShutdowner);
        this.theRootDataNode= theRootDataNode;
        this.theDataRoot= theDataRoot;
        this.thePersistent= thePersistent;
        this.theSelections= theSelections;
        }


    // Code for top-level initialization.

    private MapEpiNode rootMapEpiNode; // Root of Persistent data.
    private MapEpiNode settingsMapEpiNode; // Persistent Settings subtree.

    public void initializeAndShowV()
      /* After the Navigation Stage is constructed, 
       * this method should be called.
       * This method initializes and shows the Navigation Stage.
       * 
       * One of 2 views of the DataNode hierarchy will showing at exit:
       * * one which displays a TreeView of the DataNode hierarchy, and
       * * one which displays a single DataNode of the DataNode hierarchy.
       * There is also a button to switch to the other content.
       *
       * ///opt Simplify by using theDataRoot.getRootEpiTreeItem().
       */
      {
        rootMapEpiNode= // Cache Persistent root MapEpiNode. 
            thePersistent.getRootMapEpiNode();
        settingsMapEpiNode= // Cache Persistent Settings subtree.
            rootMapEpiNode.getMapEpiNode("Settings");
        setOnCloseRequest( (theWindowEvent) -> {
              theAppLog.info(
                "Navigation.initializeAndShowV().setOnCloseRequest(.) handler"
                + "\n   ======== REQUESTING APP SHUTDOWN =========");
              theShutdowner.requestAppShutdownV();
              });

        theBorderPane= new BorderPane(); // Make the one BorderPane.
        theBorderPane.addEventFilter(
            KeyEvent.KEY_PRESSED,
            (theKeyEvent) -> theAppLog.appendToFileV(
                NL+"[begin-key:"+theKeyEvent.getCode()+"]")
            );
        theBorderPane.addEventHandler(
            KeyEvent.KEY_PRESSED,
            (theKeyEvent) -> theAppLog.appendToFileV(
                "[end-key:"+theKeyEvent.getCode()+"]")
            );

        theEpiScene= new EpiScene(theBorderPane);
        setScene(theEpiScene); // Use tree scene as first one displayed.
        
        rebuildSceneContentV();

        setCommonSettingsV("TUDNet JavaFX Navigation UI");
        show(); // Make UI show this stage.
        JavaFXGUI.recordOpenWindowV(null,this,null); // Record showing.

        showCommandHelpV(); // Display help in a separate dialog window.
        theAppLog.info(
          "Navigation.initializeAndShowV() Ends"
          + "\n   ======== DISPLAYING APP UI =========");
        }


    // Code related to choosing what type of scene to display.

    private void rebuildSceneContentV()
      /* Depending on the contents of Persistent storage,
       * this methods rebuilds the Nodes to display 
       * the tree or a single DataNode.
       * Which one is controlled from Persistent storage.
       */
      {
        createUIComponentsV();
        DataNode previouslySelectedDataNode= 
            theSelections.getPreviousSelectedDataNode();
        boolean displayTreeB= rootMapEpiNode.isTrueB("DisplayAsTree");
        if (displayTreeB)
          { buildTreePartsV(previouslySelectedDataNode); }
          else
          { buildDataNodePartsV(previouslySelectedDataNode); }
        theBorderPane.setBottom(theBottomFlowPane);
        JavaFXGUI.setDefaultStyleV(theBorderPane);
        }


    // Code related to the DataNode view Scene.

    private void buildDataNodePartsV(
        DataNode previouslySelectedDataNode)
      /* This method builds the DataNode dependent part of the UI.
       * If previouslySelectedDataNode is null then this method does nothing.
       *
       * Otherwise it calculates a new TreeStuff from the DataNode,
       * and stores a GUI Node appropriate for displaying theDataNode
       * at the center of the content BorderPane of the DataNode Scene 
       * so that the DataNode will be displayed.
       */
      {
        rebuildWithButtonTheBottomFlowPaneV(theDataNodeShowTreeButton);
        DataNode theDataNode= previouslySelectedDataNode;
        if (null != theDataNode) { // Process DataNode if present.
          DataNode selectionDataNode= null;
          DataNode subjectDataNode= theDataNode;
          if (! theDataNode.isRootB()) { // Move to parent DataNode if present.
            selectionDataNode= subjectDataNode;
            subjectDataNode= theDataNode.getTreeParentNamedList();
            }
          theTreeStuff= // Make TreeStuff appropriate for DataNode.
            subjectDataNode.makeTreeStuff(
              selectionDataNode,
              thePersistent,
              theDataRoot,
              theRootEpiTreeItem,
              theSelections
              );
          updateFromDataNodeTreeStuffV();
          }
        }
    
    private void updateFromDataNodeTreeStuffV()
      /*
       * In this method, the code
       *   /// guiNode.addEventFilter(
       *   ///   KeyEvent.KEY_PRESSED,
       *   ///   (theKeyEvent) -> handleDataNodeKeyPressV(theKeyEvent)
       *   ///   );
       * was discovered to add multiple copies of the handler,
       * wasting resources.  It was replaced with
       *   // guiNode.setOnKeyPressed(
       *   //   (theKeyEvent) -> handleDataNodeKeyPressV(theKeyEvent));
       * sets only one copy.
       */
      {
        Node guiNode= theTreeStuff.getGuiNode();
        guiNode.addEventFilter(
            KeyEvent.KEY_PRESSED,
            (theKeyEvent) -> handleDataNodeKeyPressV(theKeyEvent)
            );
        theAppLog.appendToFileV(
            NL+"[ufdnst Adding KEY_PRESSED filter to:"+guiNode+"]");
        /// theAppLog.debug("Navigation.setDataNodeContentFromDataNodeV() "
        ///   + "guiNode= "+guiNode);
        theBorderPane.setCenter(guiNode); // Store in graph for display.
        }


    // Code related to the tree view Scene, being converted to MultiPurpose.
    // Some of these variables might be convertable to local variables.

    // These are some of the main UI Nodes, showing nesting.
    private EpiScene theEpiScene;
      private BorderPane theBorderPane; // Root of Nodes.
        private EpiTreeItem theRootEpiTreeItem;
        private FlowPane theBottomFlowPane;

    private TreeStuff theTreeStuff; // Helper TreeStuff.

    private void buildTreePartsV(DataNode previouslySelectedDataNode)
      {
        rebuildWithButtonTheBottomFlowPaneV(theTreeShowItemButton);
        setTreeContentFromDataNodeV(previouslySelectedDataNode);
        }

    private void setTreeContentFromDataNodeV(DataNode selectedDataNode)
      /* If theDataNode is null then this method does nothing.
       * Otherwise it calculates a new TreeStuff from theDataNode,
       * and stores a GUI Node for displaying theDataNode as a tree
       * at the center of the content BorderPane of the DataNode Scene 
       * so that theDataNode will be displayed.
       */
      {
        theRootEpiTreeItem= new EpiTreeItem(theRootDataNode);
        theRootEpiTreeItem.setExpanded(true); // Show only 1st and 2nd levels.
        theTreeStuff= TitledTreeNode.makeTreeStuff(
                (DataNode)null,
                selectedDataNode,
                theDataRoot,
                theRootEpiTreeItem,
                thePersistent,
                theSelections
                );
        Node guiNode= theTreeStuff.getGuiNode();
        VBox theVBox= new VBox();
        theVBox.getChildren().add(guiNode); // Add production TreeView.
        theBorderPane.setCenter(theVBox); // Store for display.
        }





    private void createUIComponentsV()
      {
        // Create the UI components.
        buildButtonsV();
        buildOnceElapsedLabelV();
        buildOnceFrameLabelViewV();
        createTestTreeViewV();
        }

    private void rebuildWithButtonTheBottomFlowPaneV(
        Button theChangeFormatButton)
      /* This method builds the theBottomFlowPane to used as
       * the bottom section of theMultiPurposeBorderPane.
       */
      {
        theBottomFlowPane= new FlowPane();
        theBottomFlowPane.getChildren().add(theChangeFormatButton);
        theBottomFlowPane.getChildren().add(theElapsedLabel);
        if (settingsMapEpiNode.isTrueB("FrameCounterEnable"))
          theBottomFlowPane.getChildren().add(frameLabel);
        }


    // Code related to the bottom FlowPane Buttons.
    
    private Button theTreeShowItemButton;
    private Button theDataNodeShowTreeButton;

    private void buildButtonsV()
      /* This method builds the buttons.
       * 
       * ///ano Based on log entry time-stamps in log file,
       * it takes almost 1/2 second to show the new window content
       * after this method exits.
       * Maybe it will be faster after being changed 
       * to change Node instead of entire Scene?
       */
      {
        theTreeShowItemButton= new Button("Show Node");
        theTreeShowItemButton.setOnAction( theActionEvent -> {
          rootMapEpiNode.putTrueOrRemoveB( // Set in Persistent storage
              "DisplayAsTree", false); // value to display as DataNode.
          rebuildSceneContentV();
          });

        theDataNodeShowTreeButton= new Button("Show Tree");
        theDataNodeShowTreeButton.setOnAction( theActionEvent -> { 
          rootMapEpiNode.putTrueOrRemoveB( // Set in Persistent storage
              "DisplayAsTree", true); // value to display as tree.
          rebuildSceneContentV();
          });

        }


    // Code related to the Elapsed-Time Label display.

    private static boolean displayElapsedB= true; // Enable at first. ///
    private static Label theElapsedLabel= null;
    private static int theElapsedI=0; ///


    private void buildOnceElapsedLabelV()
      {
        if (null != theElapsedLabel) return; // Exit if built.

        theElapsedLabel= new Label("ELAPSED");
        Runnable tickerRunnable= // Create Runnable that updates ticker Label.
          (() -> {
            theElapsedI=0;
            while(true) {
              EpiThread.interruptibleSleepB(1000); // Sleep for 1 second.
              if (! displayElapsedB) continue; // End loop if ticker not enabled.
              //  tickerB= false; // Disable later ticks.
              theElapsedI++;
              theAppLog.appendToFileV(NL+"[t]");
              final int finalElapsedI= theElapsedI;
              Platform.runLater(
                () -> { // Use JavaFX Application Thread.
                  theElapsedLabel.setText( // Update elapsed time.
                      " Elapsed:"+finalElapsedI+"s ");

                  /// Kludge.
                  // The following was an unsuccessful attempt to cause
                  // the window and TreeView to periodically 
                  // re-layout and update by changing the window size slightly.
                  // It changed the size, but didn't cause the update.
                  // getScene().getWindow().setWidth(
                  //    getScene().getWindow().getWidth() + 0.001);
                  // getScene().setWidth(
                  //     getScene().getWindow().getWidth() + 0.001);
                  });
              }
            });
        Thread tickerThread= new Thread(tickerRunnable);
        tickerThread.setDaemon( true ); // Make thread this daemon type. 
        tickerThread.start();
        }


    // Code related to the Frame Label display.

    private Label frameLabel= null;
    private AnimationTimer frameAnimationTimer= null; ///


    private void buildOnceFrameLabelViewV()
      {
        if (null != frameLabel) return; // Exit if built.

        frameLabel= new Label(" FRAME#");
        frameAnimationTimer = new AnimationTimer() {
          int frameI=0;
          @Override
          public void handle(long now) {
            frameLabel.setText(" FRAME#"+frameI++); // Update frame number.
            }
          };
        frameAnimationTimer.start();
        }


    /* ///tmp Temporary dynamic TreeView troubleshooting code.
     * This was added to try to figure out why the 
     * main TreeView used for displaying the DataNode tree isn't updating.  
     */

    private int countI= 0;
    private TreeItem<DataNode> middleChildTreeItem;
    @SuppressWarnings("unused")
    private TreeView<DataNode> testTreeView;
    /// if (settingsMapEpiNode.isTrueB("TestTreeEnable"))
    ///   theVBox.getChildren().add(testTreeView); // Add test TreeView. 


    public void createTestTreeViewV() 
      {
        EpiTreeItem rootEpiTreeItem = 
            new EpiTreeItem(
                new NamedList("TestTreeRoot"));
        rootEpiTreeItem.setExpanded(true);
        for (int i = 1; i < 4; i++) {
            EpiTreeItem childEpiTreeItem= 
                new EpiTreeItem (
                    NamedLeaf.makeNamedLeaf("TestChild" + i));
            rootEpiTreeItem.getChildren().add(childEpiTreeItem);
        }
        middleChildTreeItem= // Get and cache middle child.
            rootEpiTreeItem.getChildren().get(1);
        testTreeView= new TreeView<DataNode> (rootEpiTreeItem);        
        Runnable updateRunnable= // Create Runnable that updates middle child.
          (() -> {
            countI=0;
            while(true) { // Repeatedly change child and update UI.
              EpiThread.interruptibleSleepB(1000); // Sleep for 1 second.
              Platform.runLater(() -> {

                /// theAppLog.debug("Navigation.createTestTreeViewV() updating TreeView.");
                // Change DataNode contents, but not DataNode in TreeItem.
                DataNode child2DataNode= middleChildTreeItem.getValue();
                ((NamedLeaf)child2DataNode).setNameV("NEW-VALUE-"+countI++);

                // Trigger UI update by cycling TreeItem value reference.
                /// System.out.print("[tti]"); /// Debug.
                DataNode savedDataNode= middleChildTreeItem.getValue();
                middleChildTreeItem.setValue(null); // Clear reference.
                middleChildTreeItem.setValue(savedDataNode); // Restore it.
                });
              }
            });
        Thread updateThread= new Thread(updateRunnable);
        updateThread.setDaemon( true ); // Make thread \daemon type. 
        updateThread.start();
        }




    // Miscellaneous methods:
  

    private void handleDataNodeKeyPressV(KeyEvent theKeyEvent)
      ///org relative to Selections and TreeStuff.
      {
        theAppLog.appendToFileV("[dnkp]");
        KeyCode keyCodeI = theKeyEvent.getCode(); // Get code of key pressed.
        TreeStuff newTreeStuff= null;
        switch (keyCodeI) {
          case RIGHT:  // right-arrow.
            newTreeStuff= theTreeStuff.moveRightAndMakeTreeStuff();
            break;
          case LEFT:  // left-arrow.
            newTreeStuff= theTreeStuff.moveLeftAndMakeTreeStuff();
            break;
          default: break;
          }
        if (null != newTreeStuff) { // Change content if new TreeStuff present.
          theAppLog.appendToFileV(
              NL+"[dnnts:"+newTreeStuff.getSubjectDataNode()
              +";"+newTreeStuff.getSelectionDataNode()+"]");
          theTreeStuff= newTreeStuff; // Save new TreeStuff.
          updateFromDataNodeTreeStuffV();
          }
        }


    public void showCommandHelpV()
      /* This method implements the Help command.
        It does this by displaying a mode-less dialog.
        */
      { // queueCommandHelpV()
        String helpString=
          "This is a work-in-progress." + NL
          + NL
          + "Some of the following commands were copied from "
          + "the Swing UI Help dialog." + NL
          + "They might not all work." + NL
          + NL
          + "Use Arrow keys to navigate folders." + NL
          + "      <Right-arrow> moves to child item." + NL
          + "      <Left-arrow> moves to parent item." + NL
          + "      <Down-arrow> moves to next item." + NL
          + "      <Up-arrow> moves to previous item" + NL
          + "(Show Tree) and (Show Node) buttons alternate between views."
          ;

        Dialogger.showModelessJavaFXDialogReturnString(
            "JavaFX UI Help", helpString);
        } // queueCommandHelpV()

    }
