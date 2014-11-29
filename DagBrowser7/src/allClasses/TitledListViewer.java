package allClasses;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.BorderFactory;
//import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
//import javax.swing.event.TreeSelectionEvent;
//import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

//import static allClasses.Globals.*;  // appLogger;

public class TitledListViewer // adapted from TitledListViewer.

  extends JPanel
  ///extends JList<Object>
 
  implements 
    ListSelectionListener
    , FocusListener
    , TreeAware
    /// , TreePathListener
  
  /* This class was developed from, and is intended to replace, 
    the ListViewer class.
    It was developed and tested without disrupting other code by 
    temporarilly replacing single ListViewer references to 
    TitledListViewer references for the duration of the test.

    This class provides a simple titled DagNodeViewer that 
    displays and browses List-s using a JList.
    It was created based on code from DirectoryTableViewer.
    Eventually it might be a good idea to create 
    a static or intermediate base class that handles common operations.

    ??? It appears that JList<Object> implements many keyboard commands,
    so these have been removed from KeyListener.keyPressed(KeyEvent).
    */
    
  { // TitledListViewer
  
    // variables, most of them.
  
      /// private static final long serialVersionUID = 1L;

      JLabel titleJLabel;  // Label with the title.

      JList<Object> theJList;  // Component with the content.

      Color backgroundColor;  ////???

    // constructor and related methods.

      public TitledListViewer( TreePath inTreePath, TreeModel inTreeModel )
        /* Constructs a TitledListViewer.
          inTreePath is the TreePath associated with
          the node of the Tree to be displayed.
          The last DataNode in the path is that object.
          */
        { // TitledListViewer(.)
          super();   // Call constructor inherited from JPanel.

          { // Prepare the helper object.
            aTreeHelper=  // Construct helper class instance.
                new TreeHelper( this, inTreePath );  // Note, subject not set yet.
            } // Prepare the helper object.

          backgroundColor= getBackground();  // Saving background for later use.
          setLayout( new BorderLayout() );
          //setLayout( new BoxLayout( this, BoxLayout.Y_AXIS ) );
          
          titleJLabel= new JLabel(
            //"TEST-TITLE"
            aTreeHelper.getWholeDataNode().GetNameString( )
            );
          //titleJLabel.setBackground( Color.RED );
          titleJLabel.setOpaque( true );
          Font labelFont= titleJLabel.getFont();
          titleJLabel.setFont( labelFont.deriveFont( labelFont.getSize() * 1.5f) );
          //titleJLabel.setAlignmentX( Component.CENTER_ALIGNMENT );
          titleJLabel.setHorizontalAlignment( SwingConstants.CENTER );
          Border raisedetched = BorderFactory.createEtchedBorder(EtchedBorder.RAISED);
          titleJLabel.setBorder(raisedetched);
          add(titleJLabel,BorderLayout.NORTH); // Adding it to main JPanel.
          
          theJList= new JList<Object>();  // Construct JList.
          add(theJList,BorderLayout.CENTER); // Adding it to main JPanel.
          //add(theJList); // Adding it to main JPanel.

          InitializeTheJList( inTreeModel );
          } // TitledListViewer(.)

      public void setPreferredSize( Dimension inDimension ) /// ???
        // Do this so theJList will be full width.
        {
          super.setPreferredSize( inDimension );
          theJList.setPreferredSize( inDimension );
          }
      
      private void InitializeTheJList( TreeModel inTreeModel )
        /* This grouping method creates and initializes the JList.  */
        { // InitializeTheJList( )
          { // Set ListModel for the proper type of elements.
            ListModel<Object> AListModel= new TreeListModel( 
              aTreeHelper.getWholeDataNode( ),
              inTreeModel 
              );
            theJList.setModel( AListModel );  // Define its ListModel.
            } // Set ListModel for the proper type of elements.
          theJList.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
          theJList.setCellRenderer(   // Setting custom rendering.
            TheListCellRenderer 
            );
          { // Set the user input event listeners.
            /// aTreeHelper.addTreePathListener(this);  // Listen for tree paths.
            aTreeHelper.addTreePathListener(   // Listen for tree paths.
              /// this
              theTreePathListener
              );
            theJList.addKeyListener(aTreeHelper);  // TreeHelper does KeyEvent-s.
            theJList.addMouseListener(aTreeHelper);  // TreeHelper does MouseEvent-s.
            theJList.addFocusListener(this);  // Old FocusListener.
            theJList.addFocusListener(aTreeHelper);  // New FocusListener.
            addFocusListener(this);  // Old FocusListener.  for autofocus ???
            theJList.getSelectionModel().  // This does ListSelectionEvent-s.
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
          theJList.setSelectionInterval( // Set selection from index.
            IndexI, IndexI 
            );
          }  // setJListSelection()

      private void selectRowV(int IndexI) //??? being adapted.
        {
          if ( IndexI < 0 )  // force index to 0 if child not found.
            IndexI= 0;
          theJList.setSelectionInterval(  // Selection row as interval...
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
            (ListSelectionModel)theJList.getSelectionModel();
          int SelectionIndexI= // Get index of row selected.
            TheListSelectionModel.getMinSelectionIndex() ;
          theJList.ensureIndexIsVisible( // Scroll into view the row...
            SelectionIndexI // ...with that index.
            );
          }  // setJListScrollState()

    // Input (setter) methods.  This includes Listeners.

      /* ListSelectionListener method, for processing ListSelectionEvent-s 
        from the List's SelectionModel.
        */

        public void valueChanged(ListSelectionEvent TheListSelectionEvent) 
          /* Processes an [internal or external] ListSelectionEvent
            from the ListSelectionModel.  It does this by 
            determining what element was selected,
            adjusting dependent instance variables, and 
            firing a TreePathEvent to notify TreeSelection]-s.
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
                DataNode newSelectionDataNode=  // Get selected DataNode...
                  aTreeHelper.getWholeDataNode( ).
                    getChild(IndexI);  // ...which is child at IndexI.
                aTreeHelper.setPartDataNodeV( newSelectionDataNode );
                  // This will set the TreePaths also.
                  // This converts the row selection to a tree selection.
                ///appLogger.info( "valueChanged(ListSelectionEvent TheListSelectionEvent)");
              } // Process the selection.
            } // void valueChanged(ListSelectionEvent TheListSelectionEvent)
      
      // FocusListener methods  , to fix JTable cell-invalidate/repaint bug.

        @Override
        public void focusGained(FocusEvent arg0) 
          {
            ///appLogger.debug( "TitledListViewer.focusGained()" );

            theJList.requestFocusInWindow();  // Autofocus the JList.

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

			public TreeHelper aTreeHelper;  // helper class ???

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

    // List cell rendering.

      private ListCellRenderer TheListCellRenderer=
        new ListCellRenderer(); // for custom cell rendering.
    
      public class ListCellRenderer
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
                  //RenderComponent.setBackground(list.getBackground());
                    // This returns white!
                  RenderComponent.setBackground( backgroundColor );
                  //RenderComponent.setBackground( Color.YELLOW );
                else if ( ! list.isFocusOwner() )  // selected but not focused.
                  RenderComponent.setBackground( list.getSelectionBackground() );
                else  // both selected and focused.
                  RenderComponent.setBackground( Color.GREEN ); // be distinctive.
                  //RenderComponent.setBackground( Color.PINK); // for developing.
                }
              return RenderComponent;
              }

        } // class ListCellRenderer    

    } // TitledListViewer
