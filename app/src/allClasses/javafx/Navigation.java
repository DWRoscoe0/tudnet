package allClasses.javafx;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
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
     */

    // Injected dependencies.
    private final DataNode theRootDataNode;
    private final Persistent thePersistent;
    private DataRoot theDataRoot;
    private Selections theSelections;

    // Variables related to the tree view Scene.
    private Scene theTreeScene;
    private BorderPane treeContentBorderPane;
    private Button theTreeShowItemButton;
    private static boolean tickerB= true; // Enable at first. ///
    private static Label tickerLabel= new Label(" label-"); ///
    private static int tickInteger=0; ///
    private Label frameLabel= new Label(" frame#"); ///
    private AnimationTimer frameAnimationTimer= null; ///
    private TreeStuff theTreeTreeStuff;

    // Variables related to the DataNode view Scene.
    private Scene theDataNodeScene;
    private BorderPane theDataNodeContentBorderPane;
      // The center of this will change to display different DataNodes.
    private Button theDataNodeShowTreeButton;
    private TreeStuff theDataNodeTreeStuff; // For location tracking.

    // Other variables.
    private EpiTreeItem theRootEpiTreeItem;
    private MapEpiNode persistentMapEpiNode; // Root of Persistent data.
    private MapEpiNode settingsMapEpiNode;


    // Construction and initialization.

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
        
        persistentMapEpiNode= thePersistent.getRootMapEpiNode();
        }

    public void initializeAndStartV()
      /* This method initializes and starts the Navigation Stage, then returns.
       * The Stage then alternates between 2 Scenes:
       * * one which displays a TreeView of the hierarchy, and
       * * one which displays a single DataNode and its children.
       * Each Scene has a button to switch to the other Scene.
       * 
       * ///opt Simplify by using theDataRoot.getRootEpiTreeItem().
       */
      {
        settingsMapEpiNode= // Cache Settings.
            thePersistent.getRootMapEpiNode().getMapEpiNode("Settings");

        theRootEpiTreeItem= new EpiTreeItem(theRootDataNode);
        theRootEpiTreeItem.setExpanded(true); // Show only 1st and 2nd levels.

        theTreeShowItemButton= new Button("Show Node");
        theTreeShowItemButton.setOnAction(
            theActionEvent -> doTreeShowDataNodeButtonActionV() );

        theDataNodeShowTreeButton= new Button("Show Tree");
        theDataNodeShowTreeButton.setOnAction(
            theActionEvent -> doDataNodeShowTreeButtonActionV() );

        setOnCloseRequest( (theWindowEvent) -> {
              theAppLog.info(
                "Navigation.initializeAndStartV().setOnCloseRequest(.) handler"
                + "\n   ======== REQUESTING APP SHUTDOWN =========");
              theShutdowner.requestAppShutdownV();
              });

        tickerLabel= new Label(" TICKER");
        Runnable tickerRunnable= // Create Runnable that updates tickerLabel.
          (() -> {
            tickInteger=0;
            while(true) {
              EpiThread.interruptibleSleepB(1000); // Sleep for 1 second.
              if (! tickerB) continue; // End loop if ticker not enabled.
              //  tickerB= false; // Disable later ticks.
              tickInteger++;
              theAppLog.appendToFileV("[t]");
              final int finalTickI= tickInteger;
              Platform.runLater(
                () -> { // Use JavaFX Application Thread.
                  tickerLabel.setText(" tick-"+finalTickI); // Update ticker.

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

        createTestTreeViewV();

        frameLabel= new Label(" FRAME#");

        buildDataNodeSceneV();
        buildTreeSceneV();

        displayTreeOrDataNodeV();

        finishStateInitAndStartV("TUDNet JavaFX Navigation UI");

        frameAnimationTimer = new AnimationTimer() {
          int frameI=0;
          @Override
          public void handle(long now) {
            frameLabel.setText(" FRAME#"+frameI++); // Update frame number.
            }
          };
        frameAnimationTimer.start();
        
        showCommandHelpV();
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
        
    private void doDataNodeShowTreeButtonActionV()
      /* 
       * This method is used to cause a switch of Scenes 
       * from the DataNode Scene to the tree Scene.
       * It gets the subject DataNode that 
       * is being displayed by the DataNode viewer,
       * sets that as the selection in the TreeView,
       * then sets the Scene to be the TreeView Scene.
       */
      {
        /// System.out.println("doDataNodeShowTreeButtonActionV() called.");
        persistentMapEpiNode.putTrueOrRemoveB( // Set in Persistent storage
            "DisplayAsTree", true); // value to display as tree.
        displayTreeOrDataNodeV();
        }

    private void doTreeShowDataNodeButtonActionV()
      /* This method sets the Scene to display
       * the DataNode referenced by the TreeItem 
       * presently selected in and displayed by the TreeView.
       * This method is used to cause a switch of Scenes 
       * from the Tree scene to the DataNode Scene.
       */
      {
        persistentMapEpiNode.putTrueOrRemoveB( // Set in Persistent storage
            "DisplayAsTree", false); // value to display as DataNode.
        displayTreeOrDataNodeV();
        }

    private void displayTreeOrDataNodeV()
      /* Depending on the contents of Persistent storage,
       * this methods displays the tree or it displays a DataNode.
       * The selection is controlled from Persistent storage.
       */
      {
        DataNode previouslySelectedDataNode= 
            theSelections.getPreviousSelectedDataNode();
        boolean displayTreeB= persistentMapEpiNode.isTrueB("DisplayAsTree");
        theAppLog.debug(
            "Navigation.displayTreeOrDataNodeV() tree= "+displayTreeB);
        if (displayTreeB)
          { 
            buildTreeSceneV(); // Rebuild to resolve tickerLabel.
            setTreeContentFromDataNodeV(previouslySelectedDataNode);
            setScene(theTreeScene); // Use tree scene as first one displayed.
            }
          else
          { 
            buildDataNodeSceneV(); // Rebuild to resolve tickerLabel.
            setDataNodeContentFromDataNodeV(previouslySelectedDataNode);
            setScene(theDataNodeScene); // Use item scene as first one displayed.
            }
        theAppLog.debug(
            "Navigation.displayTreeOrDataNodeV() end");
        }


    // Tree scene methods.

    private void buildTreeSceneV()
      /* This method builds the Scene to be used when
       * the user wants to display DataNodes as a tree. 
       */ 
      {
        FlowPane bottomFlowPane= new FlowPane();
        bottomFlowPane.getChildren().add(new Label("Tree-Begin "));
        bottomFlowPane.getChildren().add(theTreeShowItemButton);
        bottomFlowPane.getChildren().add(tickerLabel);
        if (settingsMapEpiNode.isTrueB("FrameCounterEnable"))
          bottomFlowPane.getChildren().add(frameLabel);
        bottomFlowPane.getChildren().add(new Label(" Tree-End"));
        
        treeContentBorderPane= new BorderPane();
        treeContentBorderPane.setBottom(bottomFlowPane);
        treeContentBorderPane.setCenter( // This will be replaced later.
            new TextArea("UNDEFINED")); 
        theTreeScene= new Scene(treeContentBorderPane);
        EpiScene.setDefaultsV(theTreeScene);
        }

    private void setTreeContentFromDataNodeV(DataNode selectedDataNode)
      /* If theDataNode is null then this method does nothing.
       * Otherwise it calculates a new TreeStuff from theDataNode,
       * and stores a GUI Node for displaying theDataNode as a tree
       * at the center of the content BorderPane of the DataNode Scene 
       * so that theDataNode will be displayed.
       */
      {
        theTreeTreeStuff= TitledTreeNode.makeTreeStuff(
                (DataNode)null,
                selectedDataNode,
                theDataRoot,
                theRootEpiTreeItem,
                thePersistent,
                theSelections
                );
        Node guiNode= theTreeTreeStuff.getGuiNode();
        VBox theVBox= new VBox();
        if (settingsMapEpiNode.isTrueB("TestTreeEnable"))
          theVBox.getChildren().add(testTreeView); // Add test TreeView. 
        theVBox.getChildren().add(guiNode); // Add production TreeView.
        treeContentBorderPane.setCenter(theVBox); // Store for display.
        }
         


    // DataNode scene methods.

    private void buildDataNodeSceneV()
      /* Creates and returns a Scene for displaying a single DataNode.
        As a aide-effect, it also sets theDataNodeContentBorderPane so that
        theDataNodeContentBorderPane.setCenter() may be used later
        to fill in the content.
       */
      {
        theDataNodeContentBorderPane= new BorderPane();
        FlowPane bottomFlowPane= new FlowPane();
        bottomFlowPane.getChildren().add(new Label("Item-Begin "));
        bottomFlowPane.getChildren().add(theDataNodeShowTreeButton);
        bottomFlowPane.getChildren().add(tickerLabel);
        bottomFlowPane.getChildren().add(new Label(" Item-End"));
        theDataNodeContentBorderPane.setBottom(bottomFlowPane);
        theDataNodeContentBorderPane.addEventFilter( // or addEventHandler(
          KeyEvent.KEY_PRESSED, 
          (theKeyEvent) -> handleDataNodeKeyPressV(theKeyEvent)
          );
        theDataNodeScene= new Scene(theDataNodeContentBorderPane);
        EpiScene.setDefaultsV(theDataNodeScene);
        }

    private void handleDataNodeKeyPressV(KeyEvent theKeyEvent)
      ///org relative to Selections and TreeStuff.
      {
        KeyCode keyCodeI = theKeyEvent.getCode(); // Get code of key pressed.
        switch (keyCodeI) {
          case RIGHT:  // right-arrow.
            /// System.out.println("Right-arrow typed.");
            setDataNodeGuiNodeAndTreeStuffV(
                theDataNodeTreeStuff.moveRightAndMakeTreeStuff());
            break;
          case LEFT:  // left-arrow.
            /// System.out.println("Left-arrow typed.");
            setDataNodeGuiNodeAndTreeStuffV(
                theDataNodeTreeStuff.moveLeftAndMakeTreeStuff());
            break;
          default: 
            break;
          }
        }

    private void setDataNodeGuiNodeAndTreeStuffV(TreeStuff theTreeStuff)
      /* If theTreeStuff is null then this method does nothing.
       * Otherwise it saves the TreeStuff for reference later,
       * and gets the JavaFX GUI Node for theTreeStuff
       * that has been calculated for displaying the DataNode
       * and stores in the appropriate location in the Node tree
       * so it can be displayed.
       */
      {
        if (null != theTreeStuff) { // Process TreeStuff if present.
          theDataNodeTreeStuff= theTreeStuff; // Save TreeStuff.
          Node guiNode= theDataNodeTreeStuff.getGuiNode();
          theDataNodeContentBorderPane.setCenter( // Store for display
              guiNode); // the Node gotten from TreeStuff.
          }
        }

    private void setDataNodeContentFromDataNodeV(DataNode theDataNode)
      /* If theDataNode is null then this method does nothing.
       * Otherwise it calculates a new TreeStuff from theDataNode,
       * and stores a GUI Node appropriate for displaying theDataNode
       * at the center of the content BorderPane of the DataNode Scene 
       * so that theDataNode will be displayed.
       */
      {
        theAppLog.debug("Navigation.setDataNodeContentFromDataNodeV() "
            + "theDataNode= "+theDataNode);
        if (null != theDataNode) { // Process DataNode if present.
          DataNode selectionDataNode= null;
          DataNode subjectDataNode= theDataNode;
          if (! theDataNode.isRootB()) { // Move to parent DataNode if present.
            selectionDataNode= subjectDataNode;
            subjectDataNode= theDataNode.getTreeParentNamedList();
            }
          theDataNodeTreeStuff= // Make TreeStuff appropriate for DataNode.
            subjectDataNode.makeTreeStuff(
              selectionDataNode,
              thePersistent,
              theDataRoot,
              theRootEpiTreeItem,
              theSelections
              );
          Node guiNode= theDataNodeTreeStuff.getGuiNode();
          theAppLog.debug("Navigation.setDataNodeContentFromDataNodeV() "
            + "guiNode= "+guiNode);
          theDataNodeContentBorderPane.setCenter(guiNode); // Store for display.
          }
        }

    
    /// Temporary dynamic TreeView troubleshooting code. 
    private int countI= 0;
    private TreeItem<DataNode> middleChildTreeItem;
    private TreeView<DataNode> testTreeView;

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
                System.out.print("[tti]"); /// Debug.
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

    }


class IndirectString extends Object {
  
  String theString;
  
  public IndirectString(String theString) {
    this.theString= theString;
    }
  
  public void setV(String theString) {
    this.theString= theString;
    }
  
  public String toString() {return theString.toString(); }
  }
    
