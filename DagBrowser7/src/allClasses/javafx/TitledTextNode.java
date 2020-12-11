package allClasses.javafx;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;

import javax.swing.tree.TreePath;

import allClasses.DataNode;
import allClasses.DataTreeModel;


///////////// imports for Swing...
// import static allClasses.Globals.appLogger;



public class TitledTextNode extends BorderPane 

  /* This class is used for displaying leaf Nodes that
   * can be displayed as blocks of text.
   * 
   * ///fix Though the name of this class includes "Titled",
   * it does not yet display a title.
   */
  
  {

    //// private DataTreeModel theDataTreeModel;
    
    public TitledTextNode( 
                TreePath theTreePath, 
                DataTreeModel theDataTreeModel, 
                String theString
                )
      /* Constructs a TitledTextNode.
        theTreePath is the TreePath associated with
        the node of the Tree to be displayed.
        The last DataNode in the path is that Node.
        The content text to be displayed is theString.
        theDataTreeModel provides context.
        */
      {
        //// this.theDataTreeModel= theDataTreeModel;

        Label titleLabel= new Label(
          //"TEST-TITLE"
          ((DataNode)(theTreePath.getLastPathComponent())).toString()
          );
        titleLabel.setAlignment(Pos.CENTER);
        setTop(titleLabel); // Adding it to main JPanel.

        TextArea theTextArea= new TextArea(   // Construct JTextArea.
          /// "--------------DEBUG--------------  \n"+
          theString  // Text String to view.
          );
        setCenter(theTextArea);
        }

    }
