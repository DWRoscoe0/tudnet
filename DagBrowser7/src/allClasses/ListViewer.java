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
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

//import static allClasses.Globals.*;  // appLogger;

public class ListViewer

  extends JList<Object>
 
  implements 
    ListSelectionListener
    , FocusListener
    , VHelper
  
  /* This class provides a simple DagNodeViewer that 
    displays and browses List-s using a JList.
    It was created based on code from DirectoryTableViewer.
    Eventually it might be a good idea to create 
    a static or intermediate base class that handles common operations.

    ??? It appears that JList<Object> implements many keyboard commands,
    so these have been removed from KeyListener.keyPressed(KeyEvent).
    */
    
  { // ListViewer
  
    // variables.
  
      private static final long serialVersionUID = 1L;

      private ViewHelper aViewHelper;  // Mutual composition helper class ???

    // constructor and related methods.

      public ListViewer( TreePath inTreePath, TreeModel inTreeModel )
        /* Constructs a ListViewer.
          inTreePath is the TreePath associated with
          the node of the Tree to be displayed.
          The last DataNode in the path is that object.
          */
        { // ListViewer(.)
          super();   // Call constructor inherited from JList<Object>.
            // TreePath will be calculated and set later.

          { // Prepare the helper object.
            aViewHelper=  // Construct helper class instance.
              new ViewHelper( this );  // Note, subject not set yet.
            aViewHelper.setSubjectTreePathWithAutoSelectV(  // Set subject.
              inTreePath
              );
            } // Prepare the helper object.

          InitializeTheJList( inTreeModel );
          } // ListViewer(.)

      private void InitializeTheJList( TreeModel inTreeModel )
        /* This grouping method creates and initializes the JList.  */
        { // InitializeTheJList( )
          { // Set ListModel for the proper type of elements.
            ListModel<Object> AListModel;
            AListModel= new TreeListModel( 
              //subjectDataNode, 
              aViewHelper.getSubjectDataNode( ),
              inTreeModel 
              );
            setModel( AListModel );  // Define its ListModel.
            } // Set ListModel for the proper type of elements.
          setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
          setCellRenderer( TheListCellRenderer );  // set custom rendering.
          { // Set the user input event listeners.
            addKeyListener(aViewHelper);  // ViewHelper does KeyEvent-s.
            addMouseListener(aViewHelper);  // ViewHelper does MouseEvent-s.
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
            aViewHelper.getSubjectDataNode( ).getIndexOfChild( 
              aViewHelper.getSelectionDataNode() 
              );
          if ( IndexI < 0 )  // force index to 0 if child not found.
            IndexI= 0;
          setSelectionInterval( IndexI, IndexI );  // set selection using final resulting index.
          }  // setJListSelection()
  
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
            firing a TreeSelectionEvent to notify TreeSelectionListener-s.
            */
          { // void valueChanged(ListSelectionEvent TheListSelectionEvent)
            ListSelectionModel TheListSelectionModel = // get ListSelectionModel.
              (ListSelectionModel)TheListSelectionEvent.getSource();
            int IndexI =   // Get index of selected element from the model.
              TheListSelectionModel.getMinSelectionIndex();
            if // Process the selection if...
              ( //...the selection index is legal.
                (IndexI >= 0) && 
                (IndexI < aViewHelper.getSubjectDataNode( ).getChildCount( ))
                )
              { // Process the selection.
                DataNode NewSelectionDataNode=  // Get selected DataNode...
                  aViewHelper.getSubjectDataNode( ).
                    getChild(IndexI);  // ...which is child at IndexI.
                aViewHelper.setSelectionDataNodeV( NewSelectionDataNode );
                  // This will set the TreePaths also.
                aViewHelper.notifyTreeSelectionListenersV(true);
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
  
    // interface ViewHelper pass-through methods.

      public TreePath getSubjectTreePath()
        { 
          return aViewHelper.getSubjectTreePath();
          }

      public TreePath getSelectionTreePath()
        { 
          return aViewHelper.getSelectionTreePath();
          }

      public void addTreeSelectionListener( TreeSelectionListener listener ) 
        {
          aViewHelper.addTreeSelectionListener( listener );
          }

      // Nested class stuff for List cell rendering.

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

              Component RenderComponent=  // get the superclass RenderCompoent.
                super.getListCellRendererComponent(
                  list, value, index, isSelected, hasFocus );
              { // make color adjustments to RenderComponent.
                if ( ! isSelected )  // cell not selected.
                  RenderComponent.setBackground(list.getBackground());
                else if ( ! list.isFocusOwner() )  // selected but not focused.
                  RenderComponent.setBackground( list.getSelectionBackground() );
                else  // both selected and focused.
                  RenderComponent.setBackground( Color.GREEN ); // be distinctive.
                } // make color adjustments to RenderComponent.
              // RenderComponent.setBackground(Color.RED);  // ???
              return RenderComponent;
              }
          } // class ListCellRenderer    

    } // ListViewer
