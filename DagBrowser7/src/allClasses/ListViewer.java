package allClasses;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

//import javax.swing.DefaultListModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

public class ListViewer

  //extends JList<DagNode>
  extends JList<Object>
  //extends DagNodeViewer 
 
  implements 
    KeyListener, FocusListener, ListSelectionListener, MouseListener,
    VHelper
  
  /* This class provides a simple DagNodeViewer that 
    displays and browses List-s using a JList.
    It was created based on code from DirectoryTableViewer.
    Eventually it might be a good idea to create 
    a static or intermediate base class that handles common operations.
    */
    
  { // ListViewer
  
    // variables.
  
      private static final long serialVersionUID = 1L;

      public ViewHelper aViewHelper;  // helper class ???

      // static variables.
        private final static StringObject StringObjectEmpty= // for place-holders.
          new StringObject("EMPTY");

      // instance variables.

        private boolean UpdateListReentryBlockedB;  // to prevent reentry.
        
        /* Subject-DagNode-related variables.  These are in addition to 
          the ones in superclass DagNodeViewer.  */

          // whole Subject, the DagNode that this class displays.
            
            private TreePath SubjectTreePath;  // TreePath of DagNode displayed.
            private DagNode SubjectDagNode;  // DagNode displayed.
            
          // selection within Subject, the child DagNode that is selected.

            private DagNode SelectedDagNode;  // selected DagNode.
            //private String SelectionNameString; // Name of selected child.
              // This [might?] also function as a List-not-tmpty flag.

    // constructor and related methods.

      public ListViewer( TreePath InTreePath, TreeModel InTreeModel )
        /* Constructs a ListViewer.
          InTreePath is the TreePath associated with
          the node of the Tree to be displayed.
          The last DagNode in the path is that object.
          */
        { // ListViewer(.)
          super(   // do the inherited constructor code.
            //null  // TreePath will be calculated and set later.
            );  // do the inherited constructor code.

          aViewHelper=  // construct helper class instance???
            new ViewHelper( this );
            
          SubjectDagNode=  // Extract and save List DagNode from TreePath.
            (DagNode)InTreePath.getLastPathComponent();
          { // define a temporary selection TreePath to be overridden later.
            DagNode SelectionDagNode=
              new StringObject( "TEMPORARY SELECTION" );
            aViewHelper.SetSelectedChildTreePath( 
              InTreePath.pathByAddingChild( SelectionDagNode )
              );
            } // define a temporary selection TreePath to be overridden later.
          InitializeTheJList( InTreeModel );
          UpdateEverythingForSubject(   // to finish go to the desired element...
            InTreePath  // ...specified by TreePath.
            );
          } // ListViewer(.)

      private void InitializeTheJList( TreeModel InTreeModel )
        /* This grouping method creates and initializes the JList.  */
        { // InitializeTheJList( )
          //TheJList=   // Construct an empty JList.
          //  new JList<DagNode>( );  // It's data will be supplied later.
          { // Set ListModel for the proper type of elements.
            ListModel<Object> AListModel;
            //if ( InTreeModel != null )
            AListModel= new TreeListModel( SubjectDagNode, InTreeModel );
            //  else
            //  AListModel= new DefaultListModel<Object>();
            setModel( AListModel );  // Define its ListModel.
            } // Set ListModel for the proper type of elements.
          setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
          setCellRenderer( TheListCellRenderer );  // set custom rendering.
          { // Add listeners.
            addKeyListener(this);  // listen to process some key events.
            addMouseListener(this);  // listen to process mouse double-click.
            addFocusListener(this);  // listen to repaint on focus events.
            getSelectionModel().  // in its selection model...
              addListSelectionListener(this);  // ...listen to selections.
            } // Add listeners.
          } // InitializeTheJList( )

    // input (setter) methods.  this includes Listeners.
        
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
            int IndexI =   // get index of selected element from the model.
              TheListSelectionModel.getMinSelectionIndex();
            if // Process the selection if...
              ( //...the selection is legal.
                (IndexI >= 0) && 
                (IndexI < SubjectDagNode.getChildCount( ))
                )
              { // Process the selection.
                DagNode NewSelectionDagNode=  // Get selected DagNode...
                  SubjectDagNode.getChild(IndexI);  // ...which is child at IndexI.
                SetSelectionRelatedVariablesFrom( NewSelectionDagNode );
                aViewHelper.NotifyTreeSelectionListenersV(true); // tell others, if any.
                } // Process the selection.
            } // void valueChanged(ListSelectionEvent TheListSelectionEvent)
  
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
                  // System.out.println( "ListViewer.keyPressed(), it's a tab" );
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
            System.out.println("MouseListener ListViewer.mouseClicked(...), ");
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
            // System.out.println( "ListViewer.focusGained()" );
            // TheJTable.repaint();  // bug fix Kluge to display cell in correct color.  
            }
      
        @Override
        public void focusLost(FocusEvent arg0) 
          {
            // System.out.println( "ListViewer.focusLost()" );
            // TheJTable.repaint();  // bug fix Kluge to display cell in correct color.  
            }
      
    // command methods.

      private void CommandGoToParentV() 
        /* Tries to go to and display the parent of this object. */
        { // CommandGoToParentV().
          TreePath ParentTreePath=  // get the parent of selection.
            aViewHelper.GetSelectedChildTreePath().getParentPath();
          TreePath GrandParentTreePath=  // try getting parent of the parent.
            ParentTreePath.getParentPath();
          { // process attempt to get grandparent.
            if (GrandParentTreePath == null)  // there is no grandparent.
              ; // do nothing.  or handle externally?
            else  // there is a parent.
              { // record visit and display parent.
                DagInfo.  // In the visits tree...
                  UpdatePath( // update recent-visit info with...
                    aViewHelper.GetSelectedChildTreePath()  // ...the new selected TreePath.
                    );
                aViewHelper.SetSelectedChildTreePath( GrandParentTreePath );  // kluge so Notify will work.
                aViewHelper.NotifyTreeSelectionListenersV( false );  // let listener handle it.
                } // record visit and display parent.
            } // process attempt to get parent.
          } // CommandGoToParentV().

      private void CommandGoToChildV() 
        /* Tries to go to and displays a presentlly selected child 
          of the present DagNode.  
          */
        { // CommandGoToChildV().
          if  // act only if a child selected.
            //( SubDagNode.getChildCount( ) > 0 )
            //( SelectionNameString != null )
            ( SelectedDagNode != null )
            { // go to and display that child.
              aViewHelper.NotifyTreeSelectionListenersV( false );  // let listener handle it.
              } // go to and display that child.
          } // CommandGoToChildV().
      
    // state updating methods.
    
      private void UpdateEverythingForSubject(TreePath TreePathSubject)
        /* This grouping method updates everything needed to display 
          the List named by TreePathSubject as a JList
          It adjusts the instance variables, 
          the JList ListModel, and the scroller state.
          It also notifies any dependent TreeSelectionListeners.
          */
        { // UpdateEverythingForSubject()
          if ( UpdateListReentryBlockedB )  // process unless this is a reentry. 
            { // do not update, because the reentry blocking flag is set.
              System.out.println( 
                "ListViewer.java UpdateEverythingForSubject(...), "+
                "UpdateListReentryBlockedB==true"
                );
              } // do not update, because the reentry blocking flag is set.
            else // reentry flag is not set.
            { // process the update.
              UpdateListReentryBlockedB= true;  // disallow re-entry.
              
              SetSubjectRelatedVariablesFrom(TreePathSubject);
              UpdateJListStateV();  // Updates JList selection and scroller state.

              UpdateListReentryBlockedB=  // we are done so allow re-entry.
                false;
              } // process the update.
          } // UpdateEverythingForSubject()
          
      private void SetSubjectRelatedVariablesFrom
        (TreePath InSubjectTreePath)
        /* This grouping method calculates values for and stores
          the selection-related instance variables based on the
          TreePath InSubjectTreePath.
          */
        { // SetSubjectRelatedVariablesFrom(InSubjectTreePath()
          SubjectTreePath= InSubjectTreePath;  // Save base TreePath.
          SubjectDagNode=  // store new list DagNode at end of TreePath.
            (DagNode)SubjectTreePath.getLastPathComponent();

          DagNode ChildDagNode=  // Try to get the child...
            DagInfo.  // ...from the visits tree that was the...
              UpdatePathDagNode( // ...most recently visited child...
                InSubjectTreePath  // ...of the List at the end of the TreePath.
                );
          if (ChildDagNode == null)  // if no recent child try first one.
            { // try getting first ChildDagNode.
              if (SubjectDagNode.getChildCount() <= 0)  // there are no children.
                ChildDagNode= StringObjectEmpty;  // use dummy child place-holder.
              else  // there are children.
                ChildDagNode= SubjectDagNode.getChild(0);  // get first ChildDagNode.
              } // get name of first child.
          
          SetSelectionRelatedVariablesFrom( ChildDagNode );
          } // SetSubjectRelatedVariablesFrom(InSubjectTreePath()
          
      private void SetSelectionRelatedVariablesFrom( DagNode ChildDagNode )
        /* This grouping method calculates values for and stores
          the child-related instance variables based on the ChildDagNode.
          It assumes the base variables are set already.
          */
        { // SetSelectionRelatedVariablesFrom( ChildUserObject ).
          SelectedDagNode= ChildDagNode;  // Save selected DagNode.
          TreePath ChildTreePath=  // Calculate selected child TreePath to be...
            SubjectTreePath.  // ...the base TreePath with...
              pathByAddingChild( ChildDagNode );  // ... the child added.
          //SelectionNameString=   // Store name of selected child.
          //  ChildDagNode.GetNameString();
          aViewHelper.SetSelectedChildTreePath( ChildTreePath );  // select new TreePath.
          } // SetSelectionRelatedVariablesFrom( ChildUserObject ).
  
      private void UpdateJListStateV()
        /* This grouping method updates the JList state,
          including its list Model, selection, and Scroller state,
          to match the selection-related instance variables.
          Note, this might trigger a call to 
          internal method ListSelectionListener.valueChanged(),
          which might cause further processing and calls to
          esternal TreeSelectionListeners-s. */
        { // UpdateJListStateV()
          // UpdateJListListModel();  // not needed with TreeModel.
          if  // Update other stuff if...
            ( getModel().getSize() > 0 ) // ... any rows in model.
            { // Update other stuff.
              UpdateJListSelection();  // Note, this might trigger Event-s.
              UpdateJListScrollState();
              } // Update other stuff.
          } // UpdateJListStateV()
  
      /* private void UpdateJListListModel()
        /* This grouping method updates the JList's ListModel from
          the selection-related instance variables.
          */
        /*
        { //UpdateJListListModel()
          DefaultListModel<Object> TheDefaultListModel= 
            (DefaultListModel<Object>)getModel();
          TheDefaultListModel.clear();  // empfy the ListModel.
          for   // Add all the children.
            (int i = 0; i < SubjectDagNode.getChildCount(); i++)  // each child...
            TheDefaultListModel.addElement( SubjectDagNode.getChild( i ) );
          } //UpdateJListListModel()
        */
  
      private void UpdateJListSelection()
        /* This grouping method updates the JList selection state
          from the selection-related instance variables.
          Note, this will trigger a call to 
          internal method ListSelectionListener.valueChanged(),
          which might cause further processing and calls to
          esternal TreeSelectionListeners-s. */
        { // UpdateJListSelection()
          int IndexI= // try to get index of selected child.
            SubjectDagNode.getIndexOfChild( SelectedDagNode );
          if ( IndexI < 0 )  // force index to 0 if child not found.
            IndexI= 0;
          setSelectionInterval( IndexI, IndexI );  // set selection using final resulting index.
          }  // UpdateJListSelection()
  
      private void UpdateJListScrollState()
        /* This grouping method updates the JList scroll state
          from its selection state to make the selection visible.
          */
        { // UpdateJListScrollState()
          ListSelectionModel TheListSelectionModel = // get ListSelectionModel.
            (ListSelectionModel)getSelectionModel();
          //int SelectionIndexI =   // get index of selected element from the model.
          //  TheListSelectionModel.getMinSelectionIndex();
          int SelectionIndexI= // cache selection index.
            //TheJTable.getSelectedRow();
              TheListSelectionModel.getMinSelectionIndex() ;
          //Rectangle SelectionRectangle= // calculate rectangle...
          //  new Rectangle(  //...of selected cell.
          //    TheJTable.getCellRect(SelectionIndexI, 0, true)
          //  );
          ensureIndexIsVisible( // scroll into view...
            SelectionIndexI // ...the current selection.
            );
          //TheJTable.scrollRectToVisible( // scroll into view...
          //  SelectionRectangle); //...the cell's rectangle.
          // TheJTable.scrollRectToVisible( // repeat for Java bug?
          //   SelectionRectangle);
          }  // UpdateJListScrollState()
  
    // rendering methods.  to be added.

    // ViewHelper pass-through methods.

      public TreePath GetSelectedChildTreePath()
        { 
          return aViewHelper.GetSelectedChildTreePath();
          }

      public void addTreeSelectionListener( TreeSelectionListener listener ) 
        {
          aViewHelper.addTreeSelectionListener( listener );
          }
         
      public void SetSelectedChildTreePath(TreePath InSelectedChildTreePath)
        { 
          aViewHelper.SetSelectedChildTreePath( InSelectedChildTreePath );
          }
      
    // nested class stuff.

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
