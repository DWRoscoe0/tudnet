package allClasses.javafx;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TreeItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;

import static allClasses.AppLog.theAppLog;
import static allClasses.SystemSettings.NL;

import allClasses.DataNode;
import allClasses.DataRoot;
import allClasses.Dialogger;
import allClasses.EpiThread;
import allClasses.Persistent;
import allClasses.Shutdowner;
import allClasses.epinode.MapEpiNode;

public class Navigation extends EpiStage

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

  {

    // Injected dependencies.
    private final DataNode theRootDataNode;
    private final Persistent thePersistent;
    private DataRoot theDataRoot;
    private Selections theSelections;

    // Variables related to the tree view Scene.
    private Scene theTreeScene;
    private BorderPane treeContentBorderPane;
    private Button theTreeShowItemButton;
    private Label tickerLabel= new Label(" label-");
    private TreeStuff treeTreeStuff;

    // Variables related to the DataNode view Scene.
    private Scene theDataNodeScene;
    private BorderPane theDataNodeContentBorderPane;
      // The center of this will change to display different DataNodes.
    private Button theDataNodeShowTreeButton;
    private TreeStuff theDataNodeTreeStuff; // For location tracking.

    // Other variables.
    private EpiTreeItem theRootEpiTreeItem;
    private MapEpiNode persistentMapEpiNode; // Root of Persistent data.

    private FlowPane savedBottomFlowPane; ////////// debug
        
    // Construction.

    public Navigation( // Constructor.
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
        theRootEpiTreeItem= new EpiTreeItem(theRootDataNode);
        theRootEpiTreeItem.setExpanded(true); // Show 1st and 2nd levels.

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
        (new Thread(() -> { // Create thread that updates tickerLabel.
          int tickInteger=0;
          while(true) {
            EpiThread.interruptibleSleepB(1000); // Sleep for 1 second.
            tickInteger++;
            final int finalTickI= tickInteger;
            Platform.runLater(() -> { // Use JavaFX Application Thread.
              tickerLabel.setText(" tick-"+finalTickI); // Update ticker.
              }); 
            }
          })).start();;

        buildDataNodeSceneV();
        buildTreeSceneV();

        displayTreeOrDataNodeV();

        finishStateInitAndStartV("TUDNet JavaFX Navigation UI");
        
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
        /*  ////
        DataNode parentOfSelectedDataNode= 
            theDataNodeTreeStuff.getSubjectDataNode();
        setTreeContentFromDataNodeV(parentOfSelectedDataNode);//
        setScene(theTreeScene); // Switch [back] to tree scene.
        */  ////
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
        /*  ////
        TreeItem<DataNode> selectedTreeItemOfDataNode=
          treeTreeStuff.toTreeItem(
              treeTreeStuff.getSelectionDataNode());
        if (null != selectedTreeItemOfDataNode) { // Set selection if present.
          DataNode theDataNode= selectedTreeItemOfDataNode.getValue();
          setDataNodeContentFromDataNodeV(theDataNode);
          }
        setScene(theDataNodeScene); // Switch Scene to DataNode Scene.
        */  ////
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
        theAppLog.debug( ////
            "Navigation.displayTreeOrDataNodeV() end");
        }


    // Tree scene methods.

    private void buildTreeSceneV()
      /* This method builds the Scene to be used when
       * the user wants to display DataNodes as a tree. 
       */ 
      {
        treeContentBorderPane= new BorderPane();
        FlowPane bottomFlowPane= new FlowPane();
        bottomFlowPane.getChildren().add(new Label("Tree-Begin "));
        bottomFlowPane.getChildren().add(theTreeShowItemButton);
        bottomFlowPane.getChildren().add(tickerLabel);
        bottomFlowPane.getChildren().add(new Label(" Tree-End"));
        savedBottomFlowPane= bottomFlowPane;
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
        treeTreeStuff= TitledTreeNode.makeTreeStuff(
                (DataNode)null,
                selectedDataNode,
                theDataRoot,
                theRootEpiTreeItem,
                thePersistent,
                theSelections
                );
        Node guiNode= treeTreeStuff.getGuiNode();
        treeContentBorderPane.setCenter(guiNode);
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
        savedBottomFlowPane= bottomFlowPane;
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
          theDataNodeTreeStuff= // Make
            theDataNode.makeTreeStuff( // TreeStuff appropriate for DataNode.
              (DataNode)null, // No child selection specified.
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

    }
