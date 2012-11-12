package allClasses;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeSelectionListener;
//import javax.swing.event.TreeSelectionEvent;
import javax.swing.table.TableCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;


public class DirectoryTableViewer

  //extends DagNodeViewer
  extends JTable
  
  implements 
    KeyListener, FocusListener, ListSelectionListener, MouseListener,
    VHelper
  
  /* This DagNodeViewer class displays filesystem directories as tables.
    They appear in the main app's right side subpanel as a JTable.
    
    This class is more complicated than ListViewer because
    more than one cell, every cell in a row,
    is associated with the IFile which is displayed in that row.
    This association is presently maintained by comparing 
    the File/IFile name String-s,
    an array of which is stored in TheDirectoryTableModel,
    with the value of the first column in the rows.
    
    It might simplify things to make fuller use of the IFile DagNode-s
    interface and the RootTreeModel.  ??
    Though doing this might occupy more memory.
    It would allow using more common code and reduce complexity.
    
    It might be worthwhile to create a generic JTableViewer,
    which gets all its data from an augmented TableModel which
    also understands TreePath-s.  ??
    */ 
  
  { // DirectoryTableViewer
  
    // Variables.
  
      private static final long serialVersionUID = 1L;

			public ViewHelper aViewHelper;  // helper class ???

      // static variables.

        //private static final long serialVersionUID = 1L;
        
        private static IFile IFileDummy=   // selection place-holder.
          new IFile("DUMMY");

      // instance variables.
      
        //private JTable TheJTable;  // The JTable in which Subject is displayed.
        
        private DirectoryTableCellRenderer TheDirectoryTableCellRenderer= 
          new DirectoryTableCellRenderer(); // for custom node rendering.
          
        /* Subject-DagNode-related variables.  These are in addition to 
          the ones in superclass DagNodeViewer.  */
          
          // whole Subject, the directory that this class displays.
            private IFile SubjectIFile;  // as an IFile reference.
            private DagNode SubjectDagNode;  // DagNode equivalent.
            private TreePath SubjectTreePath;  // its TreePath.
            
          // selection within Subject, the directory entry that is selected.
            private DagNode SelectionDagNode;  // as a DagNode.
            //private String SelectionNameString; // as a Name String.
              // Also indicates table/directory is tmpty if null.

          private boolean UpdateTableReentryBlockedB;  // to prevent method reentry.
      
    // Constructor methods.

      /* public DirectoryTableViewer
        ( TreePath InTreePath
          // , int ForceErrorI
          )
        /* Constructs a DirectoryTableViewer.
          InTreePath is the TreePath associated with
          the Subject IFile DagNode to be displayed.
          The last IFile DagNode in the TreePath is the Subject.
          */
        /* 
        { // constructor.
          this( InTreePath, null );
          System.out.println( "DirectoryTableViewer(InTreePath)" );
          } // constructor.
        */

      public DirectoryTableViewer
        ( TreePath InTreePath,
          TreeModel InTreeModel
          )
        /* Constructs a DirectoryTableViewer.
          InTreePath is the TreePath associated with
          the Subject IFile DagNode to be displayed.
          The last IFile DagNode in the TreePath is the Subject.
          It uses InTreeModel for context, but is presently ignored.
          */
        { // constructor.
          super( );
            
          aViewHelper= new ViewHelper( this );  // construct helper class instance???

          SubjectIFile=  // Extract Subject directory to be displayed.
            (IFile)InTreePath.getLastPathComponent();
          SubjectTreePath=  // save the TreePath of Subject directory.
            InTreePath;
                      
          { // Define a temporary selection TreePath.
            // It will be overridden later by a more appropriate one.
          	aViewHelper.SetSelectedChildTreePath(   // not needed??
              InTreePath.pathByAddingChild( IFileDummy )
              );
            } // define a temporary selection TreePath.

          DirectoryTableModel ADirectoryTableModel =  // construct table model...
            new DirectoryTableModel( SubjectIFile,   //...from IFile...
              InTreeModel );  // ...and TreeModel.
          setModel( ADirectoryTableModel );  // store TableModel.
          SetupTheJTable( );  // Initialize JTable state.

          UpdateTableFor(   // to finish go to the desired directory...
            InTreePath  // ...specified by TreePath.
            );
          } // constructor.

      private void SetupTheJTable( )
        /* This grouping method initializes TheJTable.  */
        { // SetupTheJTable( )
                      
          setShowHorizontalLines( false );
          setShowVerticalLines( false );
          setIntercellSpacing( new Dimension( 0, 2 ) );
          setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
          
          // DirectoryIJTable.getColumn( "Type" ).setCellRenderer( new DirectoryTableCellRenderer() );
          // getColumn( "Type" ).setCellRenderer( new DirectoryTableCellRenderer() );  // no longer rneeded??
          { // limit Type field display width.
            getColumn( "Type" ).setMaxWidth( 32 );
            getColumn( "Type" ).setMinWidth( 32 );
            } // limit Type field display width.
          
          { // add listeners.
            addKeyListener(this);  // listen to process some key events.
            addMouseListener(this);  // listen to process mouse double-click.
            addFocusListener(this);  // listen to repaint on focus events.
            getSelectionModel().  // in its selection model...
              addListSelectionListener(this);  // ...listen to selections.
            } // add listeners.

          } // SetupTheJTable( )

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
            String[] IFileNameStrings =  // calculate list of child file names.
              SubjectIFile.GetFile().list();
            if // Process the selection if...
              ( //...the selection index is within the legal range.
                (IndexI >= 0) && 
                (IndexI < IFileNameStrings.length)
                )
              { // Process the selection.
                IFile NewSelectionIFile=   // build IFile of selection at IndexI.
                  new IFile( SubjectIFile, IFileNameStrings[IndexI] );
                SetSelectionRelatedVariablesFrom( NewSelectionIFile );
                aViewHelper.NotifyTreeSelectionListenersV(true); // tell others, if any.
                } // Process the selection.
            repaint();  // ??? kluge: do entire table for selection color.
              // this should repaint only the rows whose selection changed.
            } // void valueChanged(TheListSelectionEvent)
  
      /* KeyListener methods, for 
        overriding normal Tab key processing
        and providing command key processing.
        Normally the Tab key moves the selection from table cell to cell.
        The modification causes Tab to move keyboard focus out of the table
        to the next Component.  (Shift-Tab) moves it in the opposite direction.
        */
      
        public void keyPressed(KeyEvent TheKeyEvent) 
          /* Processes KeyEvent-s.  
            The keys processed and consued include:
              Tab and Shift-Tab for focus transfering.
              Right-Arrow and Enter keys to go to child.
              Left-Arrow keys to go to parent.
            */
          { // keyPressed.
            int KeyCodeI = TheKeyEvent.getKeyCode();  // cache key pressed.
            boolean KeyProcessedB= true;  // assume the key event will be processed here. 
            { // try to process the key event.
              if (KeyCodeI == KeyEvent.VK_TAB)  // Tab key.
                { // process Tab key.
                  // System.out.println( "DirectoryTableViewer.keyPressed(), it's a tab" );
                  Component SourceComponent= (Component)TheKeyEvent.getSource();
                  int shift = // Determine (Shift) key state.
                    TheKeyEvent.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK;
                  if (shift == 0) // (Shift) not down.
                    SourceComponent.transferFocus();  // Move focus to next component.
                    else   // (Shift) is down.
                    SourceComponent.transferFocusBackward();  // Move focus to previous component.
                  } // process Tab key.
              else if (KeyCodeI == KeyEvent.VK_LEFT)  // left-arrow key.
                CommandGoToParentV();  // go to parent folder.
              else if (KeyCodeI == KeyEvent.VK_RIGHT)  // right-arrow key.
                CommandGoToChildV();  // go to child folder.
              else if (KeyCodeI == KeyEvent.VK_ENTER)  // Enter key.
                CommandGoToChildV();  // go to child folder.
              else  // no more keys to check.
                KeyProcessedB= false;  // indicate no key was processed.
              } // try to process the key event.
            if (KeyProcessedB)  // if the key event was processed...
              TheKeyEvent.consume();  // ... prevent more processing of this key.
            } // keyPressed.

        public void keyReleased(KeyEvent TheKeyEvent) { }  // unused part of KeyListener interface.
        
        public void keyTyped(KeyEvent TheKeyEvent) { }  // unused part of KeyListener interface.
      
      // MouseListener methods, for user input from mouse.
      
        @Override
        public void mouseClicked(MouseEvent TheMouseEvent) 
          /* Checks for double click on mouse,
            which now means to go to the child folder,
            so is synonymous with the right arrow key.
            */
          {
            System.out.println("MouseListener DirectoryTableViewer.mouseClicked(...), ");
            if (TheMouseEvent.getClickCount() >= 2)
              CommandGoToChildV();  // go to child folder.
            }
            
        @Override
        public void mouseEntered(MouseEvent arg0) { }  // unused part of MouseListener interface.
        
        @Override
        public void mouseExited(MouseEvent arg0) { }  // unused part of MouseListener interface.
        
        @Override
        public void mousePressed(MouseEvent arg0) { }  // unused part of MouseListener interface.
        
        @Override
        public void mouseReleased(MouseEvent arg0) { }  // unused part of MouseListener interface.
      
      // FocusListener methods, to fix JTable cell-invalidate/repaint bug.

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
        
    // command methods.
    
      private void CommandGoToParentV() 
        /* Tries to go to and displays the parent of the present directory. 
          There are 3 possibilities:
          1.  There is a parent IFile directory.  This can be handled by 
            displaying it in this DirectoryTableViewer.
          2.  There is no parent IFile, but there is a parent that is something else.
            This can be handled by passing a pseudo-selection
            of that parent to any TreeSelectiooListener-s.
          3.  There is no parent of any kind.  This will happen only in
            stand-alone mode.  It is handled by doing nothing.
          */
        { // CommandGoToParentV().
          TreePath DirectoryTreePath=  // get the directory of selection.
            aViewHelper.GetSelectedChildTreePath().getParentPath();
          TreePath ParentTreePath=  // try getting parent of the directory.
            DirectoryTreePath.getParentPath();
          { // process attempt to get parent.
            if (ParentTreePath == null)  // there is no parent.
              ; // do nothing.  or handle externally?
            else  // there is a parent directory.
              { // record visit and display parent directory.
                DagInfo.  // In the visits tree...
                  UpdateTreeWith( // update recent-visit info with...
                    aViewHelper.GetSelectedChildTreePath()  // ...the new selected TreePath.
                    );
                //UpdateTableFor(ParentTreePath);  // select (and display) the parent directory.
                aViewHelper.SetSelectedChildTreePath( ParentTreePath );  // kluge so Notify will work.
                aViewHelper.NotifyTreeSelectionListenersV( false );  // slow listener delegation. ??
                } // record visit and display parent directory.
            } // process attempt to get parent.
          } // CommandGoToParentV().

      private void CommandGoToChildV() 
        /* Tries to go to and displays a presentlly selected child 
          of the present directory.  There are 3 possibilities:
          1.  The selected child is a directory IFile.  This can be 
            handled by displaying it in this DirectoryTableViewer.
          2.  The selected child is a data IFile.  
            This could be handled by passing a pseudo-selection
            of that child to the TreeSelectiooListener-s,
            which will should handle it with an IJTextArea
            in a DagNodeViewer.
          3.  There is no selected child.  
            This will happen when a directory is empty.
            It is handled by doing nothing.
          */
        { // CommandGoToChildV().
          if  // act only if a child file is selected.
            ( SelectionDagNode != null )
            { // go to and display that child.
              File ChildFile=  // reference child object as file.
                new File(
                  SubjectIFile.GetFile(), 
                  SelectionDagNode.GetNameString( )  // was SelectionNameString
                  );
              if ( ChildFile.isDirectory() ) 
                UpdateTableFor(  // select (and display) the child...
                    aViewHelper.GetSelectedChildTreePath()  // ...which is present selection.
                  );
              else if ( ChildFile.isFile() ) 
                { // handle unable to display.
                  aViewHelper.NotifyTreeSelectionListenersV( false );  // let listener
                    // handle it.
                  } // handle unable to display.
              } // go to and display that child.
          } // CommandGoToChildV().
        
    // miscellaneous shared methods and grouping methods.
    
      private void UpdateTableFor(TreePath TreePathNewDirectory)
        /* This method updates everything needed to display as a table
          the directory named by TreePathNewDirectory.
          It adjusts the instance variables, the JTable TableModel,
          and the scroller state.
          It also notifies any connected TreeSelectionListeners.
          */
        { // UpdateTableFor()
          if ( UpdateTableReentryBlockedB )  // process unless this is a reentry. 
            { // do not update, because the reentry blocking flag is set.
              System.out.println( 
                "DirectoryTableViewer.java UpdateTableFor(...), "+
                "UpdateTableReentryBlockedB==true"
                );
              } // do not update, because the reentry blocking flag is set.
            else // reentry flag is not set.
            { // process the update.
              UpdateTableReentryBlockedB= true;  // disallow re-entry.
              
              SetSubjectRelatedVariablesFrom(TreePathNewDirectory);
              UpdateJTableStateV();

              UpdateTableReentryBlockedB=  // we are done so allow re-entry.
                false;
              } // process the update.
          } // UpdateTableFor()
          
      private void SetSubjectRelatedVariablesFrom
        (TreePath InSubjectTreePath)
        /* This grouping method calculates values for and stores
          the Subject-DagNode-related instance variables from the
          directory TreePath InSubjectTreePath.
          Much of it is concerned with determining which child
          should be selected when the directory is displayed.
          */
        { // SetSubjectRelatedVariablesFrom(.).
          SubjectTreePath= InSubjectTreePath; // save Subject TreePath.
          SubjectIFile=  // store as Subject directory IFile DagNode...
            (IFile)InSubjectTreePath.  // ...the direcoty TreePath's...
              getLastPathComponent();  // ...last DagNode.
          SubjectDagNode= SubjectIFile;  // copy to alias.
          DagNode SelectedDagNode=  // try to get child...
            DagInfo.  // ...from the visits tree which is...
              UpdateTreeAndGetRecentChildDagNodeAt( // ...most recently visited...
                InSubjectTreePath
                );  // ...of the tree node at end of selected TreePath.
          if ( SelectedDagNode == null )  // if no recent child get first one.
            { // try getting first child.
              if (SubjectIFile.getChildCount() <= 0)  // there are no children.
                SelectedDagNode= IFileDummy;  // use dummy child place-holder.
              else  // there are children.
                SelectedDagNode= SubjectIFile.getChild(0);  // get first one.
              } // try getting first child.
          SetSelectionRelatedVariablesFrom(  // set selection-related variables...
            SelectedDagNode  // ...from the resulting SelectedDagNode.
            );
          } // SetSubjectRelatedVariablesFrom(.)

      private void SetSelectionRelatedVariablesFrom( DagNode SelectedDagNode )
          /* This grouping method calculates values for and stores the 
            selection-related instance variables based on the SelectedDagNode.
            It assumes the non-selection Subject variables are set already.
            */
        { // SetSelectionRelatedVariablesFrom( ChildUserObject ).
          SelectionDagNode= SelectedDagNode;  // Save selection DagNode.
          TreePath ChildTreePath=  // Calculate selected child TreePath to be...
            SubjectTreePath.  // ...the base TreePath with...
              pathByAddingChild( SelectedDagNode );  // ... the child added.
              // maybe add Dummy if null?
          //SelectionNameString=   // Store name of selected child.
          //  SelectedDagNode.GetNameString();
          aViewHelper.SetSelectedChildTreePath( ChildTreePath );  // select new TreePath.
          } // SetSelectionRelatedVariablesFrom( ChildUserObject ).

      private void UpdateJTableStateV()
        /* This grouping method updates the JTable state,
          including its table Model, selection, and Scroller state,
          to match the selection-related instance variables.
          Note, this might trigger a call to 
          internal method ListSelectionListener.valueChanged(),
          which might cause further processing and calls to
          esternal TreeSelectionListeners-s. */
        { // UpdateJTableStateV()
          UpdateJTableModel();  // Update the JTable's DataModel.
          if  // Update other stuff if...
            ( getModel().getRowCount() > 0 ) // ... any rows in model.
            { // Update other stuff.
              UpdateJTableSelection();  // Select appropriate row.  
                // Note, this might trigger ListSelectionEvent.
              UpdateJTableScrollState();  // Adjust scroll position.
              } // Update other stuff.
          } // UpdateJTableStateV()

      private void UpdateJTableModel()
        /* This grouping method updates the JTable's TableModel from
          the selection-related instance variables.
          Maybe the FakeDirectory code belongs in the DirectoryTableModel??
          */
        { //UpdateJTableModel()
          String[] DirectoryArrayString=  // read directory as Strings.
            SubjectIFile.GetFile().list(); 
          DirectoryTableModel TheDirectoryTableModel= // cache TableModel.
            (DirectoryTableModel)getModel();
            
          if (DirectoryArrayString != null)  // A proper file list was returned.
            TheDirectoryTableModel.  // in the TableModel...
              setDirectory(SubjectIFile);  // ...store directory.
          else  // IFile.list() failed to return directory file name list.
            { // create and store fake file list for display and navigation.
              System.out.println( // report lack of fake directory.
                "DirectoryTableViewer.UpdateJTableModel() needs Fake" 
                );
              /*
                String [] FakeDirectoryStrings=  // create a fake array...
                  {};  //...ontaining zero elements initially.
                if  // override if we previously...
                  (SelectionNameString != null)  //...visited a child.
                  { // create one-element file list containing child.
                    FakeDirectoryStrings= // create new array...
                      new String [1];  //... with 1 element.
                    FakeDirectoryStrings[0]=  // store in it the...
                      SelectionNameString;  //...last visited child.
                    } // create one-element file list containing child.
                TheDirectoryTableModel.  //... of the JTable...
                  setChildNames(FakeDirectoryStrings); //...store fake.
                */
              } // create and store fake file list for display and navigation.
          } //UpdateJTableModel()

      private void UpdateJTableSelection()
        /* This grouping method updates the JTable selection state
          from the selection-related instance variables.
          Note, changing the JTable selection might trigger a call to 
          internal method ListSelectionListener.valueChanged(),
          which might cause further processing and calls to
          esternal TreeSelectionListeners-s. */
        { // UpdateJTableSelection()
          int IndexI= // try to get index of selected child.
            SubjectDagNode.getIndexOfChild( SelectionDagNode );
          if ( IndexI < 0 )  // force index to 0 if child not found.
            IndexI= 0;
          setRowSelectionInterval( IndexI, IndexI ); // set selection using final resulting index.
          }  // UpdateJTableSelection()

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
          // scrollRectToVisible( // repeat for Java bug?
          //   SelectionRectangle);
          }  // UpdateJTableScrollState()

    // rendering methods, for coloring cells based on fucus state.
      
      public TableCellRenderer getCellRenderer(int row, int column)
      /* Returns precalculated Renderer.  
        It is done this way so it's the same for every cell in a row.
        This is okay for now, but might need changing later.
        */
      { return TheDirectoryTableCellRenderer; }

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

    // ViewHelper pass-through methods.

      public TreePath GetSelectedChildTreePath()
        { 
          return aViewHelper.GetSelectedChildTreePath();
          }

      public void NotifyTreeSelectionListenersV( boolean InternalB )
        { // NotifyTreeSelectionListenersV.
          aViewHelper.NotifyTreeSelectionListenersV( InternalB );
          } // NotifyTreeSelectionListenersV.

      public void addTreeSelectionListener( TreeSelectionListener listener ) 
        {
          aViewHelper.addTreeSelectionListener( listener );
          }
         
      public void SetSelectedChildTreePath(TreePath InSelectedChildTreePath)
        { 
          aViewHelper.SetSelectedChildTreePath( InSelectedChildTreePath );
          }

    } // DirectoryTableViewer
