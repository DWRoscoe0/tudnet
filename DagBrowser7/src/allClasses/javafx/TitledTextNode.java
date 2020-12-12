package allClasses.javafx;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;

import javax.swing.tree.TreePath;

import allClasses.DataNode;
import allClasses.DataTreeModel;

// import static allClasses.Globals.appLogger;



public class TitledTextNode extends BorderPane 

  /* This class is used for displaying leaf Nodes that
   * can be displayed as blocks of text.
   * 
   * ///fix Though the name of this class includes "Titled",
   * it does not yet display a title.
   */
  
  {

    public TitledTextNode( 
                TreePath theTreePath, 
                DataTreeModel theDataTreeModel, ///opt 
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
        Label titleLabel= new Label(
          //"TEST-TITLE"
          ((DataNode)(theTreePath.getLastPathComponent())).toString()
          );
        setTop(titleLabel); // Adding it to main JPanel.
        BorderPane.setAlignment(titleLabel,Pos.CENTER);

        TextArea theTextArea= new TextArea(   // Construct JTextArea.
          /// "--------------DEBUG--------------  \n"+
          theString  // Text String to view.
          );
        /// theTextArea.getCaret().setVisible(true); // Make viewer cursor visible.
        theTextArea.setWrapText(true); // Make all visible.
        /// theTextArea.setWrapStyleWord(true); // Make it pretty.
        setCenter(theTextArea);
        }

    }
