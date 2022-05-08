package allClasses.javafx;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;

import allClasses.DataNode;
import allClasses.DataRoot;
import allClasses.Persistent;

// import static allClasses.Globals.appLogger;



public class TitledTextNode extends BorderPane 

  /* This class is used for displaying leaf Nodes that
   * can be displayed as blocks of text.
   * 
   * ///fix Though the name of this class includes "Titled",
   * it does not yet display a title.
   */
  
  {

    @SuppressWarnings("unused") ///
    private TreeStuff theTreeStuff;

    public static TreeStuff makeTreeStuff(
                DataNode subjectDataNode,
                DataNode selectedDataNode,
                String theString,
                Persistent thePersistent,
                DataRoot theDataRoot,
                EpiTreeItem theRootEpiTreeItem,
                Selections theSelections
                )
    { 
      TreeStuff theTreeStuff= TreeStuff.makeTreeStuff(
          subjectDataNode,
          selectedDataNode,
          thePersistent,
          theDataRoot,
          theRootEpiTreeItem,
          theSelections
          );
      TitledTextNode theTitledTextNode= new TitledTextNode( 
        subjectDataNode,
        theString,
        theTreeStuff
        );
      theTreeStuff.initializeGUINodeV(theTitledTextNode);
      return theTreeStuff;
      }
    
    public TitledTextNode(
        DataNode subjectDataNode, String theString, TreeStuff theTreeStuff )
      /* Constructs a TitledTextNode.
        subjectDataNode is the node of the Tree to be displayed.
        The last DataNode in the path is that Node.
        The content text to be displayed is theString.
        theDataTreeModel provides context.
        */
      {
        this.theTreeStuff= theTreeStuff;
        Label titleLabel= new Label(
          //"TEST-TITLE"
          subjectDataNode.toString()
          );
        setTop(titleLabel); // Adding it to main JPanel.
        BorderPane.setAlignment(titleLabel,Pos.CENTER);

        TextArea theTextArea= new TextArea(   // Construct TextArea.
          theString  // Text String to view.
          );
        theTextArea.setWrapText(true); // Make all visible.
        setCenter(theTextArea);
        Platform.runLater( () -> theTextArea.requestFocus() );
        }

    }
