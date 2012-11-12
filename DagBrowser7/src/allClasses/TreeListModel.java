package allClasses;

import javax.swing.AbstractListModel;
import javax.swing.tree.TreeModel;

public class TreeListModel

  extends AbstractListModel<Object> 

  /* This class impliments a ListModel which gets its data from
    a node Object in the context of a TreeModel.
    
    ??? It will need the ability to Listen to the TreeModel for changes,
    respond to only the relevant changes,
    and fire Event-s to its own Listener-s.
    Wrap Listener-s in weak-references to prevent memory leaks.
    */

    { // class TreeListModel

    private static final long serialVersionUID = 1L;
    
    private Object TheObject;  //source of data with tree.
    private TreeModel TheTreeModel;  // tree containing the object.

    TreeListModel( Object InObject, TreeModel InTreeModel )
      /* This constructor records InObject which will provide the data,
        and InTreeModel which will provide context.
        */
      {
        TheObject= InObject;
        TheTreeModel= InTreeModel;
        }
    
    @Override
    public Object getElementAt(int IndexI) 
      {
        return TheTreeModel.getChild( TheObject, IndexI );
        }

    @Override
    public int getSize() 
      {
        return TheTreeModel.getChildCount( TheObject ); 
        }

    } // class TreeListModel
