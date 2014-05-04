package allClasses;

//import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
//import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.table.TableCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

//import static allClasses.Globals.*;  // appLogger;


public class DirectoryTableViewer

  extends JTable
  
  implements 
    FocusListener, ListSelectionListener, TreeAware, TreePathListener
  
  /* This class displays filesystem directories as tables.
    They appear in the main app's right side subpanel as a JTable.
    
    This class is more complicated than ListViewer because
    more than one cell, every cell in a row,
    is associated with the IFile which is displayed in that row.
    This association is presently maintained by comparing 
    the File/IFile name String-s,
    an array of which is stored in TheDirectoryTableModel,
    with the value of the first column in the rows.
    
    It might simplify things to make fuller use of the IFile DataNode-s
    interface and the DataTreeModel.  ??
    Though doing this might occupy more memory.
    It would allow using more common code and reduce complexity.
    
    It might be worthwhile to create a generic JTableViewer,
    which gets all its data from an augmented TableModel which
    also understands TreePath-s.  ??
    
    ??? If this panel has focus, and a selected folder is far from the top,
    it might appear below the botton of the Scroller 
    if the selection is moved to either a parent or child folder.  
    */ 

  /* ??? marks things to do below.  Here are those items summarized:
    * ??? Rewrite so that calls to Combining 
      SetSelectedChildTreePath( TreePath ) and 
      NotifyTreeSelectionListenersV( boolean ) can be combined.
    */
  
  { // DirectoryTableViewer
  
    // Variables, some of them.
  
      private static final long serialVersionUID = 1L;

      private DirectoryTableCellRenderer theDirectoryTableCellRenderer= 
        new DirectoryTableCellRenderer(); // for custom node rendering.
      
    // Constructor methods.

      public DirectoryTableViewer( 
          TreePath inTreePath, TreeModel InTreeModel
          )
        /* Constructs a DirectoryTableViewer.
          inTreePath is the TreePath associated with
          the Subject IFile DataNode to be displayed.
          The last IFile DataNode in the TreePath is the Subject.
          It uses InTreeModel for context, but is presently ignored.
          */
        { // constructor.
          super( );  // Call superclass constructor.
            
          { // Construct and initialize the helper object.
            aTreeHelper= new TreeHelper(  // Construct helper class instance...
              this, inTreePath  // ...with back-referene and path info.
              );  // Note, subject not set yet.
            } // Construct and initialize the helper object.

          DirectoryTableModel ADirectoryTableModel =  // Construct...
            new DirectoryTableModel(  //...directory table model from...
              (IFile)aTreeHelper.getWholeDataNode(), //...subject IFile...
              InTreeModel  // ...and TreeModel.
              );
          setModel( ADirectoryTableModel );  // store TableModel.

          setupTheJTable( );  // Initialize JTable state.
          } // constructor.

      private void setupTheJTable( )
        /* This grouping method initializes the JTable.  */
        { // setupTheJTable( )

          setShowHorizontalLines( false );
          setShowVerticalLines( false );
          setIntercellSpacing( new Dimension( 0, 2 ) );
          setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
          
          { // limit Type field display width.
            getColumn( "Type" ).setMaxWidth( 32 );
            getColumn( "Type" ).setMinWidth( 32 );
            } // limit Type field display width.
          
          { // add listeners.
            aTreeHelper.addTreePathListener(this);  // Listen for tree paths.
            addKeyListener(aTreeHelper);  // listen to process some key events.
            addMouseListener(aTreeHelper);  // listen to process mouse double-click.
            addFocusListener(this);  // listen to repaint on focus events.
            getSelectionModel().  // in its selection model...
              addListSelectionListener(this);  // ...listen to selections.
            } // add listeners.

          UpdateJTableForContentV();
          } // setupTheJTable( )

    // Listener interface methods.
        
      /* ListSelectionListener method, for processing
        ListSelectionEvent-s from the DirectoryIJTable's SelectionModel.
        */
      
        public void valueChanged(ListSelectionEvent TheListSelectionEvent) 
          /* Processes [internal or external] ListSelectionEvent
            from the ListSelectionModel.  It does this by 
            determining what row item has become selected,
            adjusting any dependent instance variables,
            and firing a TreeSelectionEvent to notify
            any interested TreeSelectionListener-s.
            */
          { // void valueChanged(TheListSelectionEvent)
            ListSelectionModel TheListSelectionModel = // get ListSelectionModel.
              (ListSelectionModel)TheListSelectionEvent.getSource();
            int IndexI =   // get index of selected element from the model.
              TheListSelectionModel.getMinSelectionIndex();
            IFile subjectIFile=  // Cache Subject directory.
              (IFile)aTreeHelper.getWholeDataNode();
            String[] IFileNameStrings =  // Calculate array of child file names.
              subjectIFile.GetFile().list();
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
                aTreeHelper.setPartDataNodeV( NewSelectionIFile );
	                // This will set the TreePaths also.
	                // This converts the row selection to a tree selection.
                } // Process the selection.
            repaint();  // ??? kluge: do entire table for selection color.
              // this should repaint only the rows whose selection changed.
            } // void valueChanged(TheListSelectionEvent)
      
      // FocusListener methods  , to fix JTable cell-invalidate/repaint bug.

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
            ( getModel().getRowCount() > 0 ) // ... any rows in model.
            { // Update other stuff.
              TreePath inTreePath= aTreeHelper.getPartTreePath();
              selectTableRowV(inTreePath);  // Select appropriate row.  
                // Note, this might trigger ListSelectionEvent.
              UpdateJTableScrollState();  // Adjust scroll position.
              } // Update other stuff.
          } // UpdateJTableForContentV()

      private void selectTableRowV(TreePath inTreePath) //???
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
                ! aTreeHelper.getWholeTreePath( ).  // ...whole path isn't...
                   equals( inTreePath.getParentPath() )  // ...parent of new.
                  )
              break toReturn;  // Exit without selecting.
            int IndexI;  // Allocate index.
            { // Calculate new path's child's index.
              IndexI= // try to get index of selected child.
                aTreeHelper.getWholeDataNode().getIndexOfChild( inDataNode );
              if ( IndexI < 0 )  // force index to 0 if child not found.
                IndexI= 0;
              } // Calculate new path's child's index.
            setRowSelectionInterval(  // Selection row as interval...
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
            getSelectedRow();
          Rectangle SelectionRectangle= // calculate rectangle...
            new Rectangle(  //...of selected cell.
              getCellRect(SelectionIndexI, 0, true)
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

    // interface TreeAware code for TreeHelper access and TreePathListener.

			public TreeHelper aTreeHelper;  // helper class ???

			public TreeHelper getTreeHelper() { return aTreeHelper; }

      public void partTreeChangedV( TreeSelectionEvent inTreeSelectionEvent )
        /* This TreePathListener method translates 
          inTreeSelectionEvent TreeHelper tree path into 
          an internal JTable selection.
          It ignores any paths with which it cannot deal.
          */
        {
          TreePath inTreePath=  // Get the TreeHelper's path from...
            inTreeSelectionEvent.  // ...the TreeSelectionEvent's...
              getNewLeadSelectionPath();  // ...one and only TreePath.

            selectTableRowV(   // Select row appropriate to...
              inTreePath  // ...path.
              );  // Note, this might trigger ListSelectionEvent.

          }

    } // DirectoryTableViewer
