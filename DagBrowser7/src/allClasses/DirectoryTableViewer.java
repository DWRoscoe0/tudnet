package allClasses;

import java.awt.BorderLayout;
//import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableCellRenderer;
import javax.swing.tree.TreePath;


public class DirectoryTableViewer

  extends JPanel
  
  implements 
    FocusListener, ListSelectionListener, TreeAware
  
  /* This class displays file-system directories as tables.
    They appear in the main app's right side sub-panel as a JTable.

    This class is more complicated than ListViewer because
    more than one cell, every cell in a row,
    is associated with the IFile which is displayed in that row.
    This association is presently maintained by comparing 
    the File/IFile name String-s,
    an array of which is stored in TheDirectoryTableModel,
    with the value of the first column in the rows.

    It might simplify things to make fuller use of the IFile DataNode-s
    interface and the DataTreeModel.  ///enh ??
    Though doing this might occupy more memory.
    It would allow using more common code and reduce complexity.
    
    ///fix?? If this panel has focus, and a selected folder is far from the top,
    it might appear below the bottom of the Scroller 
    if the selection is moved to either a parent or child folder.  

    */
  
  { // DirectoryTableViewer
  
    // Variables, some of them.

      private JTable theJTable;
      
      private DirectoryTableCellRenderer theDirectoryTableCellRenderer= 
        new DirectoryTableCellRenderer(); // for custom node rendering.
      
    // Constructor and related methods.

      public DirectoryTableViewer(
          TreePath inTreePath, 
          DataTreeModel InTreeModel
          )
        /* Constructs a DirectoryTableViewer.
          inTreePath is the TreePath associated with
          the Subject IFile DataNode to be displayed.
          The last IFile DataNode in the TreePath is the Subject.
          It uses InTreeModel for context, but is presently ignored.
          */
        {
          super( );  // Call superclass constructor.
            
          { // Construct and initialize the helper object.
            theTreeHelper= new TreeHelper(  // Construct helper class instance...
              this, 
              InTreeModel.getMetaRoot(),
              inTreePath  // ...with back-reference and path info.
              );  // Note, subject not set yet.
            } // Construct and initialize the helper object.

          setupTheJTable(InTreeModel);  // Initialize JTable state.
          JScrollPane theJScrollPane= // Place the JTextArea in a scroll pane.
              new JScrollPane(theJTable);
          add(theJScrollPane,BorderLayout.CENTER); // Add it to main JPanel.
          }

      private void setupTheJTable(DataTreeModel InTreeModel )
        /* This grouping method initializes the JTable.  */
        { // setupTheJTable( )
          theJTable= new JTable();
          DirectoryTableModel ADirectoryTableModel =  // Construct...
            new DirectoryTableModel(  //...directory table model from...
              (IFile)theTreeHelper.getWholeDataNode(), //...subject IFile...
              InTreeModel  // ...and TreeModel.
              );
          theJTable.setModel( ADirectoryTableModel );  // store TableModel.
          theJTable.setShowHorizontalLines( false );
          theJTable.setShowVerticalLines( false );
          theJTable.setIntercellSpacing( new Dimension( 0, 2 ) );
          theJTable.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
          
          { // limit Type field display width.
            theJTable.getColumn( "Type" ).setMaxWidth( 32 );
            theJTable.getColumn( "Type" ).setMinWidth( 32 );
            } // limit Type field display width.
          
          { // Linking event listeners to event sources.
            theJTable.getSelectionModel().addListSelectionListener(this); // Making...
              // ...this DirectoryTableViewer be a ListSelectionListener...
              // ...for its own ListSelectionEvent-s.
            theTreeHelper.addTreePathListener( // Making...
              theTreePathListener  // ...my special TreePathAdapter be...
              ); //  ...a TreePathListener for TreeHelper TreePathEvent-s.
            }

          UpdateJTableForContentV();
          } // setupTheJTable( )

    // ListSelectionListener interface code.
    
      public void valueChanged(ListSelectionEvent TheListSelectionEvent) 
        /* Processes [internal or external] ListSelectionEvent
          from the DirectoryIJTable's ListSelectionModel.  
          It does this by determining what row item has become selected,
          adjusting any dependent instance variables,
          and firing a TreePathEvent to notify
          any interested TreeSelectionListener-s.
          */
        { // void valueChanged(TheListSelectionEvent)
          ListSelectionModel TheListSelectionModel = // get ListSelectionModel.
            (ListSelectionModel)TheListSelectionEvent.getSource();
          int IndexI =   // get index of selected element from the model.
            TheListSelectionModel.getMinSelectionIndex();
          IFile subjectIFile=  // Cache Subject directory.
            (IFile)theTreeHelper.getWholeDataNode();
          String[] IFileNameStrings =  // Calculate array of child file names.
            subjectIFile.getFile().list();
          if ( IFileNameStrings == null )  // If array is null replace with empty array.
            IFileNameStrings= new String[ 0 ]; // Replace with empty array.
          if // Process the selection if...
            ( //...the selection index is within the legal range.
              (IndexI >= 0) && 
              (IndexI < IFileNameStrings.length)
              )
            { // Process the selection.
              IFile NewSelectionIFile=   // build IFile of selection at IndexI.
                new IFile( subjectIFile, IFileNameStrings[IndexI] );
              //SetSelectionRelatedVariablesFrom( NewSelectionIFile );
              theTreeHelper.setPartDataNodeV( NewSelectionIFile );
                // This will set the TreePaths also.
                // This converts the row selection to a tree selection.
              } // Process the selection.
          repaint();  // ?? kluge: do entire table for selection color.
            // this should repaint only the rows whose selection changed.
          } // void valueChanged(TheListSelectionEvent)
    
    // FocusListener  interface code.

      // The purpose of this is to fix JTable cell-invalidate/repaint bug.

      @Override
      public void focusGained(FocusEvent arg0) 
        {
          // System.out.println( "DirectoryTableViewer.focusGained()" );
          repaint();  // bug fix Kluge to display cell in correct color.  
          }
    
      @Override
      public void focusLost(FocusEvent arg0) 
        {
          // System.out.println( "DirectoryTableViewer.focusLost()" );
          repaint();  // bug fix Kluge to display cell in correct color.  
          }

    // TreeAware and TreePathListener interface code for TreeHelper class.

			private TreeHelper theTreeHelper;  // Reference to helper class.

			public TreeHelper getTreeHelper() { return theTreeHelper; }
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
              /* This TreePathListener method is called by theTreeHelper
                when theTreeHelper accepts a new TreePath.
                This method translates inTreePathEvent TreeHelper tree path 
                into an internal JTable selection.

                ?? It ignores any paths with which it cannot deal.
                */
              {
                TreePath inTreePath=  // Get the TreeHelper's path from...
                  inTreePathEvent.  // ...the TreePathEvent's...
                    getTreePath();  // ...one and only TreePath.

                  selectTableRowV(   // Select row appropriate to...
                    inTreePath  // ...path.
                    );  // Note, this might trigger ListSelectionEvent.

                }
            }
        
    // miscellaneous shared methods and grouping methods.
      
      private void UpdateJTableForContentV()
        /* This grouping method updates the JTable state,
          including its table Model, selection, and Scroller state,
          to match the selection-related instance variables.
          Note, this might trigger a call to 
          internal method ListSelectionListener.valueChanged(),
          which might cause further processing and calls to
          esternal TreeSelectionListeners-s. */
        { // UpdateJTableForContentV()
          //UpdateJTableModel();  // Update the JTable's DataModel.
          if  // Update other stuff if...
            ( theJTable.getModel().getRowCount() > 0 ) // ... any rows in model.
            { // Update other stuff.
              TreePath inTreePath= theTreeHelper.getPartTreePath();
              selectTableRowV(inTreePath);  // Select appropriate row.  
                // Note, this might trigger ListSelectionEvent.
              UpdateJTableScrollState();  // Adjust scroll position.
              } // Update other stuff.
          } // UpdateJTableForContentV()

      private void selectTableRowV(TreePath inTreePath) //??
        /* This helper method selects the row in the JTable 
          associated with inTreePath, if possible.
          It must be a sibling of the present part TreePath.
          Note, changing the JTable selection might trigger a call to 
          internal method ListSelectionListener.valueChanged().
          Otherwise it does nothing.
          */
        { // selectTableRowV()
          toReturn: {
            if ( inTreePath == null )  // Path is null.
              break toReturn;  // Exit without selecting.
            DataNode inDataNode=  // Translate TreePath to DataNode.
              (DataNode)inTreePath.getLastPathComponent();
            if ( inDataNode == null )  // There is no selection.
              break toReturn;  // Exit without selecting.
            if (   // New path not is sibling of old one because...
                ! theTreeHelper.getWholeTreePath( ).  // ...whole path isn't...
                   equals( inTreePath.getParentPath() )  // ...parent of new.
                  )
              break toReturn;  // Exit without selecting.
            int IndexI;  // Allocate index.
            { // Calculate new path's child's index.
              IndexI= // try to get index of selected child.
                theTreeHelper.getWholeDataNode().getIndexOfChild( inDataNode );
              if ( IndexI < 0 )  // force index to 0 if child not found.
                IndexI= 0;
              } // Calculate new path's child's index.
            theJTable.setRowSelectionInterval(  // Selection row as interval...
              IndexI, IndexI  // ...with same start and end row index.
              );
              
          } // toReturn ends.
            return;

          }  // selectTableRowV()

      private void UpdateJTableScrollState()
        /* This grouping method updates the JTable scroll state
          from its selection state to make the selection visible.
          */
        { // UpdateJTableScrollState()
          int SelectionIndexI= // cache selection index.
              theJTable.getSelectedRow();
          Rectangle SelectionRectangle= // calculate rectangle...
            new Rectangle(  //...of selected cell.
                theJTable.getCellRect(SelectionIndexI, 0, true)
            );
          scrollRectToVisible( // scroll into view...
            SelectionRectangle); //...the cell's rectangle.
          }  // UpdateJTableScrollState()

    // rendering methods, for coloring cells based on fucus state.
      
      public TableCellRenderer getCellRenderer(int row, int column)
        /* Returns precalculated Renderer.  
          It is done this way so it's the same for every cell in a row.
          This is okay for now, but might need changing later.
          */
        { return theDirectoryTableCellRenderer; }

      /* public Component prepareRenderer
        (TableCellRenderer renderer, int row, int column)
        // This is to make each cell in the same row be the same color.
        {
          Component c = super.prepareRenderer(renderer, row, column);  // ??

          //  Alternate row color
          if (!isRowSelected(row))
            // c.setBackground(row % 2 == 0 ? getBackground() : Color.LIGHT_GRAY);
            c.setBackground( getBackground() );
          return c;
          }
        */

    } // DirectoryTableViewer
