package allClasses.javafx;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;

import allClasses.DataNode;
import allClasses.DataRoot;
import allClasses.Persistent;

public class Navigation extends EpiStage

  /* This class is, or eventually will be,
   * for navigation of the Infogora DataNode hierarchy.
   * It displays the hierarchy in 1 of 2 ways:
   * * showing the hierarchy as a navigable tree using a TreeView, or 
   * * showing a particular DataNode and possibly some of its descendants.
   * At startup it displays the DataNode that was being displayed at shutdown.
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
    private TreeView<DataNode> theTreeView; 
    private Button theTreeShowItemButton;
    private EpiTreeItem theRootEpiTreeItem;

    // Variables related to the DataNode view Scene.
    private Scene theDataNodeScene;
    private BorderPane theDataNodeContentBorderPane;
      // The center of this will change to display different DataNodes.
    private Button theDataNodeShowTreeButton;
    private TreeStuff theDataNodeTreeStuff; // For location tracking.
 

    // Construction.

    public Navigation( // Constructor.
        JavaFXGUI theJavaFXGUI, 
        DataNode theRootDataNode,
        Persistent thePersistent,
        DataRoot theDataRoot,
        Selections theSelections
        )
      {
        super(theJavaFXGUI);
        this.theRootDataNode= theRootDataNode;
        this.theDataRoot= theDataRoot;
        this.thePersistent= thePersistent;
        this.theSelections= theSelections;
        }
    
    public void initializeAndStartV()
      /* This method initializes and starts the Navigation Stage, then returns.
       * The Stage then alternates between 2 Scenes:
       * * one which displays a TreeView of the hierarchy, and
       * * one which displays a single DataNode and its descendants.
       * Each Scene has a button to switch to the other Scene.
       */
      {
        theTreeShowItemButton= new Button("Show Node");
        theDataNodeShowTreeButton= new Button("Show Tree");
        
        DataNode previouslySelectedDataNode= 
            theSelections.getPreviousSelectedDataNode();
        
        buildTreeSceneV(theTreeShowItemButton, theRootDataNode);
        setTreeSelectionFromDataNodeV(previouslySelectedDataNode);

        buildDataNodeSceneV();
        setDataNodeContentFromDataNodeV(previouslySelectedDataNode);

        setEventHandlersV(); // Okay to do now that above definitions are done.

        setScene(theTreeScene); // Use tree scene as first one displayed.
        /// setScene(theDataNodeScene); // Use item scene as first one displayed.

        finishStateInitAndStartV("Infogora JavaFX Navigation UI");
        }

    private void buildTreeSceneV(
        Button theSwitchButton, DataNode rootDataNode)
      /* This method builds the Scene to be used when
       * the user wants to display DataNodes as a tree. 
       */ 
      {
        theRootEpiTreeItem= new EpiTreeItem(theRootDataNode);
        theRootEpiTreeItem.setExpanded(true);
        theTreeView= new TreeView<DataNode>(theRootEpiTreeItem);
        treeContentBorderPane= new BorderPane();
        treeContentBorderPane.setCenter(theTreeView);
        treeContentBorderPane.setBottom(theSwitchButton);
        theTreeScene= new Scene(treeContentBorderPane);
        EpiScene.setDefaultsV(theTreeScene);
        }

    private void setTreeSelectionFromDataNodeV(DataNode theDataNode) 
      /* If theDataNode is null then this method does nothing.
       * Otherwise it calculates the TreeItem that references theDataNode
       * and causes the TreeView's SelectionModel to select it.
       */
      { 
        if (null != theDataNode) { // Process DataNode if present.
          TreeItem<DataNode> theTreeItem= // Translate to TreeItem. 
              toTreeItem(theDataNode,theRootEpiTreeItem); 
          theTreeView.getSelectionModel().select(theTreeItem); // Select it.
          }
        }

    private TreeItem<DataNode> toTreeItem(
        DataNode targetDataNode, TreeItem<DataNode> rootTreeItem) 
      /* This translates targetDataNode to the TreeItem that references it
       * by searching for the ancestor DataNode referenced by rootTreeItem,
       * then tracing TreeItems back to the target DataNode.
       * This is done recursively to simplify path tracking.  
       * This method returns the target TreeItem or null if translation fails.
       * The returned TreeItem and some of its immediate ancestors
       * might be created if they do not exist when this method is called.
       */
      {
          TreeItem<DataNode> resultTreeItem;
        main: {
          if // Root TreeItem references target DataNode.
            (rootTreeItem.getValue() == targetDataNode)
            { resultTreeItem= rootTreeItem; break main; } // Exit with root.
          TreeItem<DataNode> parentTreeItem= // Recursively translate parent. 
              toTreeItem(targetDataNode.getParentNamedList(), rootTreeItem);
          if (null == parentTreeItem) // Parent translation failed.
            { resultTreeItem= null; break main; } // Indicate failure with null.
          for // Search for target DataNode in translated parent's children.
            ( TreeItem<DataNode> childTreeItem : parentTreeItem.getChildren() )
            {
              if  // Exit with child TreeItem if it references target DataNode.
                (childTreeItem.getValue().equals(targetDataNode))
                { resultTreeItem= childTreeItem; break main; }
              }
          // If here then no child referenced the target DataNode.
          resultTreeItem= null; // Indicate failure with null.
        } // main:
          return resultTreeItem;
        }

    private void buildDataNodeSceneV()
      /* Creates and returns a Scene for displaying a single DataNode.
        As a aide-effect, it also sets theDataNodeContentBorderPane so that
        theDataNodeContentBorderPane.setCenter() may be used later
        to fill in the content.
       */
      {
        theDataNodeContentBorderPane= new BorderPane();
        theDataNodeContentBorderPane.setBottom(theDataNodeShowTreeButton);
        theDataNodeScene= new Scene(theDataNodeContentBorderPane);
        EpiScene.setDefaultsV(theDataNodeScene);
        }
    
    private void setEventHandlersV()
      /* This method registers EventHandlers for various 
       * button activation and key press events.
       * The handlers registered are for the relatively small number of events
       * for which the desired handler behavior is different from 
       * the default behavior provided by handlers in the JavaFX libraries. 
       */
      {
        // Button activations.
        theTreeShowItemButton.setOnAction(
            theActionEvent -> doTreeShowDataNodeButtonActionV() );
        theDataNodeShowTreeButton.setOnAction(
            theActionEvent -> doDataNodeShowTreeButtonActionV() );

        // Key presses, DataNode scene only.
        theDataNodeScene.addEventHandler(
            KeyEvent.KEY_PRESSED, e -> doItemKeyV(e) );
        }

    private void doItemKeyV(javafx.scene.input.KeyEvent theKeyEvent)
      /* This is the event handler for DataNode viewer key press events.  */
      {
        KeyCode keyCodeI = theKeyEvent.getCode(); // Get code of key pressed.
        switch (keyCodeI) {
          case RIGHT:  // right-arrow.
            //// System.out.println("Right-arrow typed.");
            setGuiNodeAndTreeStuffV(
                theDataNodeTreeStuff.moveRightAndMakeTreeStuff());
            break;
          case LEFT:  // left-arrow.
            //// System.out.println("Left-arrow typed.");
            setGuiNodeAndTreeStuffV(
                theDataNodeTreeStuff.moveLeftAndMakeTreeStuff());
            break;
          default: 
            break;
          }
        }
    
    private void doTreeShowDataNodeButtonActionV()
      /* This method sets the Scene to display
       * the DataNode referenced by the TreeItem 
       * presently selected in and displayed by the TreeView.
       * This method is used to cause a switch of Scenes 
       * from the Tree scene to the DataNode Scene.
       */
      {
        TreeItem<DataNode> theTreeItemOfDataNode=
          theTreeView.getSelectionModel().getSelectedItem();
        if (null != theTreeItemOfDataNode) { // Process if a TreeItem selected.
          DataNode theDataNode= theTreeItemOfDataNode.getValue();
          setDataNodeContentFromDataNodeV(theDataNode);
          }
        setScene(theDataNodeScene); // Switch Scene to DataNode Scene.
        }

    private void setGuiNodeAndTreeStuffV(TreeStuff theTreeStuff)
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
        if (null != theDataNode) { // Process DataNode if present.
          theDataNodeTreeStuff= // Make
            theDataNode.makeTreeStuff( // TreeStuff appropriate for DataNode.
              (DataNode)null, // No child selection specified.
              thePersistent,
              theDataRoot,
              theSelections
              );
          Node guiNode= theDataNodeTreeStuff.getGuiNode();
          theDataNodeContentBorderPane.setCenter(guiNode); // Store for display.
          }
        }

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
        DataNode parentOfSelectedDataNode= 
            theDataNodeTreeStuff.getSubjectDataNode();
        TreeItem<DataNode> theTreeItemOfDataNode= 
            toTreeItem(parentOfSelectedDataNode,theRootEpiTreeItem);
        theTreeView.getSelectionModel().select(theTreeItemOfDataNode);
        setScene(theTreeScene); // Switch [back] to tree scene.
        Platform.runLater( () -> theTreeView.requestFocus() );
        }

    }
