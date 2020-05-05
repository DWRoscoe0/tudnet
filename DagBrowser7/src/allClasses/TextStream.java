package allClasses;

import static allClasses.AppLog.theAppLog;

import javax.swing.JComponent;
import javax.swing.tree.TreePath;

public class TextStream

  extends DataNode

  /* This class extends DataNode to represent text streams.
    */
  
  {
  
    // Variables.
      private String valueString;
      private UnicasterManager theUnicasterManager;
      private Persistent thePersistent;
      private ConnectionManager theConnectionManager;

    // Constructors.

      TextStream( 
          String inString, 
          UnicasterManager theUnicasterManager,
          Persistent thePersistent,
          ConnectionManager theConnectionManager
          )
        // Constructs a TextStream with a name inString.
        { 
          theAppLog.debug("TextStream.TextStream(.) called.");
          valueString= inString;
          this.theUnicasterManager= theUnicasterManager;
          this.thePersistent= thePersistent;
          this.theConnectionManager= theConnectionManager;
          }

    // theFile pass-through methods.

    // A subset of delegated DataTreeModel methods.

      public boolean isLeaf( )
        {
          return true;
          }

      // Methods which return Strings about the node.

      public String getNameString( )
        {
          return "Text-Stream";
          }
          
    // other interface DataNode methods.

      public String getContentString()
        /* This method produces the value which is used by
          TitledTextViewer to display the contents of a file.
          */
        {
          return valueString;
          }
      
      public JComponent getDataJComponent( 
          TreePath inTreePath, 
          DataTreeModel inDataTreeModel 
          )
        {
          theAppLog.debug("TextStream.getDataJComponent(.) called.");
          JComponent resultJComponent= 
            new TextStreamViewer( 
              inTreePath, 
              inDataTreeModel,
              theUnicasterManager,
              thePersistent,
              theConnectionManager
              );
          return resultJComponent;  // return the final result.
          }
          
    // other methods.

      public String toString( ) { return getNameString( ); }
        /* it appears that JList uses toString() but
          JTree uses something else (getName()?).
          */

    }
