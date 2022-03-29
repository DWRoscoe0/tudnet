package allClasses;

import javax.swing.JComponent;
import javax.swing.tree.TreePath;

public class EmptyBranchTreeHelper extends TreeHelper {
  
  /* This class is identical to TreeHelper except
    commandGoToChildB(..) reports not-doable in the case of
    a branch node with no children.
    */

  EmptyBranchTreeHelper(  // Constructor.
      JComponent inOwningJComponent, 
      MetaRoot theMetaRoot,
      TreePath inTreePath
      )
    {
      super(inOwningJComponent, theMetaRoot, inTreePath);
      }

  public boolean commandGoToChildB( boolean doB )
    /* This is like the TreeHelper version but reports false if
      the node is a branch but has no children.
      */
    {
      boolean doableB= false;  // Assume command is not doable.

      toReturn: {
        if (getPartDataNode().isLeaf())  // It is undoable leaf case.
          break toReturn;  // Exit with default not doable result.
        if   // Superclass reports no other doable case.
          (!super.commandGoToChildB(false))
          break toReturn;  // Exit with default not doable result.
        doableB= true;  // Override result to indicate command doable.

        if (! doB)  // Command execution is not desired.
          break toReturn; // So exit with doability result.
        
        // Command execution begins.
        super.commandGoToChildB(true);  // Have superclass execute command.

      } // toReturn end.
        return doableB;  // Return whether command is/was doable.

      }

  } // MyTreeHelper
