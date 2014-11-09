package allClasses;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
//import javax.swing.event.TreeSelectionEvent;
//import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

//import static allClasses.Globals.*;  // appLogger;

public class ListViewer

  extends JList<Object>
 
  implements 
    ListSelectionListener
    , FocusListener
    , TreeAware
    /// , TreePathListener
  
  /* This class provides a simple DagNodeViewer that 
    displays and browses List-s using a JList.
    It was created based on code from DirectoryTableViewer.
    Eventually it might be a good idea to create 
    a static or intermediate base class that handles common operations.

    ??? It appears that JList<Object> implements many keyboard commands,
    so these have been removed from KeyListener.keyPressed(KeyEvent).
    */
    
  { // ListViewer
  
    // variables, most of them.
  
      private static final long serialVersionUID = 1L;

    // constructor and related methods.

      public ListViewer( TreePath inTreePath, TreeModel inTreeModel )
        /* Constructs a ListViewer.
          inTreePath is the TreePath associated with
          the node of the Tree to be displayed.
          The last DataNode in the path is that node.
          */
        { // ListViewer(.)
          super();   // Call constructor inherited from JList<Object>.
            // TreePath will be calculated and set later.

          { // Prepare the helper object.
            aTreeHelper=  // Construct helper class instance.
                new TreeHelper( this, inTreePath );  // Note, subject not set yet.
            } // Prepare the helper object.

          InitializeTheJList( inTreeModel );
          } // ListViewer(.)

      private void InitializeTheJList( TreeModel inTreeModel )
        /* This grouping method creates and initializes the JList.  */
        { // InitializeTheJList( )
          { // Set ListModel for the proper type of elements.
            ListModel<Object> aListModel;
            aListModel= new TreeListModel( 
              aTreeHelper.getWholeDataNode( ),
              inTreeModel 
              );
            setModel( aListModel );  // Define its ListModel.
            } // Set ListModel for the proper type of elements.
          setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
          setCellRenderer( TheListCellRenderer );  // set custom rendering.
          { // Set the user input event listeners.
            aTreeHelper.addTreePathListener(   // Listen for tree paths.
              /// this
              theTreePathListener
              );
            addKeyListener(aTreeHelper);  // TreeHelper does KeyEvent-s.
            addMouseListener(aTreeHelper);  // TreeHelper does MouseEvent-s.
            addFocusListener(this);  // listen to repaint on focus events.???
            getSelectionModel().  // This does ListSelectionEvent-s.
              addListSelectionListener(this);
            } // Set the user input event listeners.
          setJListSelection();
          setJListScrollState();
          } // InitializeTheJList( )

      private void setJListSelection()
        /* This grouping method updates the JList selection state
          from the selection-related instance variables.
          Note, this will trigger a call to 
          internal method ListSelectionListener.valueChanged(),
          which might cause further processing and calls to
          esternal TreeSelectionListeners-s. */
        { // setJListSelection()
          int IndexI= // try to get index of selected child.
            aTreeHelper.getWholeDataNode( ).getIndexOfChild( 
              aTreeHelper.getPartDataNode() 
              );
          if ( IndexI < 0 )  // force index to 0 if child not found.
            IndexI= 0;
          setSelectionInterval( IndexI, IndexI );  // set selection using final resulting index.
          }  // setJListSelection()

      /* ???
      private void selectRowV(TreePath inTreePath) //??? being adapted.
        /* This helper method selects the row in the JTable 
          associated with inTreePath, if possible.
          It must be a sibling of the present part TreePath.
          Note, changing the JTable selection might trigger a call to 
          internal method ListSelectionListener.valueChanged().
          Otherwise it does nothing.
          */
      /* ???
        { // selectTableRowV()
          toReturn: {
            if ( inTreePath == null )  // Path is null.
              break toReturn;  // Exit without selecting.
            DataNode inDataNode=  // Translate TreePath to DataNode.
              (DataNode)inTreePath.getLastPathComponent();
            if ( inDataNode == null )  // There is no selection.
              break toReturn;  // Exit without selecting.
            if (   // New path not is sibling of old one because...
                ! aTreeHelper.getWholeTreePath( ).  // ...whole path isn't...
                   equals( inTreePath.getParentPath() )  // ...parent of new.
                  )
              break toReturn;  // Exit without selecting.
            int IndexI;  // Allocate index.
            { // Calculate new path's child's index.
              IndexI= // try to get index of selected child.
                aTreeHelper.getWholeDataNode().getIndexOfChild( inDataNode );
              //if ( IndexI < 0 )  // force index to 0 if child not found.
              //  IndexI= 0;
              } // Calculate new path's child's index.
            //setSelectionInterval(  // Selection row as interval...
            //  IndexI, IndexI  // ...with same start and end row index.
            //  );
            selectRowV(IndexI); //??? being adapted.
          } // toReturn ends.
            return;

          }  // selectTableRowV()
      */

      private void selectRowV(int IndexI) //??? being adapted.
        {
          if ( IndexI < 0 )  // force index to 0 if child not found.
            IndexI= 0;
          setSelectionInterval(  // Selection row as interval...
            IndexI, IndexI  // ...with same start and end row index.
            );
          }

      private void setJListScrollState()
        /* This method sets the JList scroll state
          from its selection state to make certain that
          the selection is visible.
          */
        { // setJListScrollState()
          ListSelectionModel TheListSelectionModel = // Get selection model.
            (ListSelectionModel)getSelectionModel();
          int SelectionIndexI= // Get index of row selected.
            TheListSelectionModel.getMinSelectionIndex() ;
          ensureIndexIsVisible( // Scroll into view the row...
            SelectionIndexI // ...with that index.
            );
          }  // setJListScrollState()

    // Input (setter) methods.  this includes Listeners.
        
      /* ListSelectionListener method, for processing ListSelectionEvent-s 
        from the List's SelectionModel.
        */
      
        public void valueChanged(ListSelectionEvent TheListSelectionEvent) 
          /* Processes an [internal or external] ListSelectionEvent
            from the ListSelectionModel.  It does this by 
            determining what element was selected,
            adjusting dependent instance variables, and 
            firing a TreePathEvent to notify TreeSelectionListener-s.
            */
          { // void valueChanged(ListSelectionEvent TheListSelectionEvent)
            ListSelectionModel TheListSelectionModel = // get ListSelectionModel.
              (ListSelectionModel)TheListSelectionEvent.getSource();
            int IndexI =   // Get index of selected element from the model.
              TheListSelectionModel.getMinSelectionIndex();
            if // Process the selection if...
              ( //...the selection index is legal.
                (IndexI >= 0) && 
                (IndexI < aTreeHelper.getWholeDataNode( ).getChildCount( ))
                )
              { // Process the selection.
                DataNode NewSelectionDataNode=  // Get selected DataNode...
                  aTreeHelper.getWholeDataNode( ).
                    getChild(IndexI);  // ...which is child at IndexI.
                aTreeHelper.setPartDataNodeV( NewSelectionDataNode );
                  // This will set the TreePaths also.
                  // This converts the row selection to a tree selection.
                } // Process the selection.
            } // void valueChanged(ListSelectionEvent TheListSelectionEvent)
      
      // FocusListener methods  , to fix JTable cell-invalidate/repaint bug.

        @Override
        public void focusGained(FocusEvent arg0) 
          {
            // System.out.println( "DirectoryTableViewer.focusGained()" );
            setJListScrollState();
            repaint();  // bug fix Kluge to display cell in correct color.  
            }
      
        @Override
        public void focusLost(FocusEvent arg0) 
          {
            // System.out.println( "DirectoryTableViewer.focusLost()" );
            setJListScrollState();
            repaint();  // bug fix Kluge to display cell in correct color.  
            }

    // interface TreeAware code for TreeHelper access.

			private TreeHelper aTreeHelper;  // helper class ???

			public TreeHelper getTreeHelper() { return aTreeHelper; }

      /* TreePathListener code, for when TreePathEvent-s
        happen in either the left or right panel.
        This was based on TreeSelectionListener code.
        For a while it used TreeSelectionEvent-s for 
        passing TreePath data.
        */

        private TreePathListener theTreePathListener= 
          new MyTreePathListener();

        private class MyTreePathListener 
          extends TreePathAdapter
          {
            public void setPartTreePathV( TreePathEvent inTreePathEvent )
              /* This TreePathListener method translates 
                inTreePathEvent TreeHelper tree path into 
                an internal JList selection.
                It ignores any paths with which it cannot deal.
                */
              {
                //TreePath inTreePath=  // Get the TreeHelper's path from...
                //  inTreeSelectionEvent.  // ...the TreeSelectionEvent's...
                //    getNewLeadSelectionPath();  // ...one and only TreePath.

                selectRowV(   // Select row appropriate to...
                  //inTreePath  // ...path.
                  aTreeHelper.getPartIndexI()
                  );  // Note, this might trigger ListSelectionEvent.
                }
            }

      /* ???
      public TreePath getWholeTreePath()
        { 
          return aTreeHelper.getWholeTreePath();
          }

      public TreePath getPartTreePath()
        { 
          return aTreeHelper.getPartTreePath();
          }

      public void addTreePathListener( TreePathListener listener ) 
        {
          aTreeHelper.addTreePathListener( listener );
          }
      */

    // List cell rendering.

      private ListCellRenderer TheListCellRenderer=
        new ListCellRenderer(); // for custom cell rendering.
    
      public static class ListCellRenderer
        extends DefaultListCellRenderer
        /* This small helper class extends the method 
          getTableCellRendererComponent() which is 
          used for rendering JList cells.
          The main purpose of this is to give the selected cell
          a different color when the Component has focus.
          */
        { // class ListCellRenderer
          private static final long serialVersionUID = 1L;

          public Component getListCellRendererComponent
            ( JList<?> list,
              Object value,
              int index,
              boolean isSelected,
              boolean hasFocus
              )
            {
              Component RenderComponent=  // Getting the superclass version.
                super.getListCellRendererComponent(
                  list, value, index, isSelected, hasFocus 
                  );
              { // Making color adjustments based on various state.
                if ( ! isSelected )  // cell not selected.
                  RenderComponent.setBackground(list.getBackground());
                else if ( ! list.isFocusOwner() )  // selected but not focused.
                  RenderComponent.setBackground( list.getSelectionBackground() );
                else  // both selected and focused.
                  RenderComponent.setBackground( Color.GREEN ); // be distinctive.
                }
              // RenderComponent.setBackground(Color.RED);  // ???
              return RenderComponent;
              }

        } // class ListCellRenderer    

    } // ListViewer
