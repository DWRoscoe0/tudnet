package allClasses;

import java.awt.Component;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
//import java.awt.event.MouseEvent;
//import java.awt.event.MouseListener;

//import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

public class TextViewer 

  //extends DagNodeViewer 
  extends IJTextArea
 
  implements 
    KeyListener, FocusListener, ListSelectionListener, // MouseListener,
    VHelper
  
  /* This class was created quickly from ListViewer 
    to provide a simple DagNodeViewer that 
    displays and browses Text using a JTextArea.
    It's got a lot of unused and useless code,
    but it does display text strings and text files.
    */
    
  { // TextViewer
  
    // variables.
    
      // static variables.
    
        private static final long serialVersionUID = 1L;

        private final static StringObject StringObjectEmpty= // for place-holders.
          new StringObject("EMPTY");

      // instance variables.

        private boolean UpdateReentryBlockedB;  // to prevent method reentry.
        
        /* Subject-DataNode-related variables.  These are in addition to 
          the ones in superclass DagNodeViewer.  */

          // whole Subject, the DataNode that this class displays.
            
            private TreePath SubjectTreePath;  // TreePath of DataNode displayed.
            private DataNode SubjectDataNode;  // DataNode displayed.
            
          // selection within Subject, the child DataNode that is selected.

            public ViewHelper aViewHelper;  // helper class ???

    // constructor and related methods.

      /* public TextViewer( TreePath TreePathIn, String InString )
        { 
          this( TreePathIn, null, InString ); 
          System.out.println( "TextViewer(InTreePath, InString)" );
          }
        */

      public TextViewer
        ( TreePath TreePathIn, TreeModel InTreeModel, String InString )
        /* Constructs a TextViewer.
          TreePathIn is the TreePath associated with
          the node of the Tree to be displayed.
          The last DataNode in the path is that object.
          The contents is InString.
          InTreeModel provides context.
          */
        { // TextViewer(.)
          super(   // do the inherited constructor code.
            InString  // String to view.
            //null  // TreePath will be calculated and set later.
            );  // do the inherited constructor code.
          //TheJTextArea=   // Construct an empty JTextArea.
          //  //new JTextArea( InString );  // It's data is InString.
          //  this;  // Reference self.  ???
          CommonInitialization( TreePathIn );
          } // TextViewer(.)

      /* public TextViewer( TreePath TreePathIn, IFile InIFile )
        { 
          this( TreePathIn, null, InIFile ); 
          System.out.println( "TextViewer(TreePathIn, InIFile)" );
          }
        */

      public TextViewer
        ( TreePath TreePathIn, TreeModel InTreeModel, IFile InIFile )
        /* Constructs a TextViewer.
          TreePathIn is the TreePath associated with
          the node of the Tree to be displayed.
          The last DataNode in the path is that object.
          The contents is InIFile.
          InTreeModel provides context.
          */
        { // TextViewer(.)
          super(   // do the inherited constructor code.
            //null  // TreePath will be calculated and set later.
             InIFile.GetFile()   // File to view.
            );  // do the inherited constructor code.
            
          //TheJTextArea=   // Construct an empty JTextArea.
          //  //new IJTextArea( InIFile );  // It's data is InIFile.
          //  this;  // Reference self.  ???
          CommonInitialization( TreePathIn );
          } // TextViewer(.)

      private void CommonInitialization( TreePath TreePathIn )
        /* This grouping method creates and initializes the JTextArea.  */
        { // CommonInitialization( )

          aViewHelper=  // construct helper class instance.
            new ViewHelper( this ); 

          if ( TreePathIn == null )  // prevent null TreePath.
            TreePathIn = new TreePath( new StringObject( "DUMMY TreePath" ));
          SubjectDataNode=  // Extract and save List DataNode from TreePath.
            (DataNode)TreePathIn.getLastPathComponent();
          { // define a temporary selection TreePath to be overridden later.
            DataNode SelectionDataNode=
              new StringObject( "TEMPORARY SELECTION" );
            SetSelectedChildTreePath( 
              TreePathIn.pathByAddingChild(SelectionDataNode)
              );
            } // define a temporary selection TreePath to be overridden later.
          InitializeTheJTextArea( );
          UpdateEverythingForSubject(   // to finish go to the desired element...
            TreePathIn  // ...specified by TreePath.
            );
          } // CommonInitialization( )

      private void InitializeTheJTextArea( )
        /* This grouping method creates and initializes the JTextArea.  */
        { // InitializeTheJTextArea( )
          /*
          { // Set ListModel for the proper type of elements.
            DefaultListModel<DataNode> ADefaultListModel= 
              new DefaultListModel<DataNode>();
            TheJTextArea.setModel( ADefaultListModel );  // Define its ListModel.
            } // Set ListModel for the proper type of elements.
          TheJTextArea.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
          */
          { // Add listeners.
            addKeyListener(this);  // listen to process some key events.
            // addMouseListener(this);  // listen to process mouse double-click.
            addFocusListener(this);  // listen to repaint on focus events.
            //TheJTextArea.getSelectionModel().  // in its selection model...
            //  addListSelectionListener(this);  // ...listen to selections.
            } // Add listeners.
          } // InitializeTheJTextArea( )

    // input (setter) methods.  this includes Listeners.
        
      /* ListSelectionListener method, for processing ListSelectionEvent-s 
        from the List's SelectionModel.
        */
      
        public void valueChanged(ListSelectionEvent TheListSelectionEvent) 
          /* Processes an [internal or external] ListSelectionEvent
            from TheJTextArea's ListSelectionModel.  It does this by 
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
                (IndexI < SubjectDataNode.getChildCount( ))
                )
              { // Process the selection.
                DataNode NewSelectionDataNode=  // Get selected DataNode...
                  SubjectDataNode.getChild(IndexI);  // ...which is child at IndexI.
                SetSelectionRelatedVariablesFrom( NewSelectionDataNode );
                NotifyTreeSelectionListenersV(true); // tell others, if any.
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
                  // System.out.println( "TextViewer.keyPressed(), it's a tab" );
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
              //else if (KeyCodeI == KeyEvent.VK_RIGHT)  // right-arrow key.
              //  CommandGoToChildV();  // go to child folder.
              //else if (KeyCodeI == KeyEvent.VK_ENTER)  // Enter key.
              //  CommandGoToChildV();  // go to child folder.
              else  // no more keys to check.
                KeyProcessedB= false;  // indicate no key was processed.
              } // try to process the key event.
            if (KeyProcessedB)  // if the key event was processed...
              TheKeyEvent.consume();  // ... prevent more processing of this key.
            } // keyPressed.

        public void keyReleased(KeyEvent TheKeyEvent) { }  // unused part of KeyListener interface.
        
        public void keyTyped(KeyEvent TheKeyEvent) { }  // unused part of KeyListener interface.

      /* MouseListener methods, for user input from mouse.
      
        @Override
        public void mouseClicked(MouseEvent TheMouseEvent) 
          /* Checks for double click on mouse,
            which now means to go to the child folder,
            so is synonymous with the right arrow key.
            */
        /*
          {
            System.out.println("MouseListener TextViewer.mouseClicked(...), ");
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
        
        */
  
      // FocusListener methods, to fix JTable cell-invalidate/repaint bug.

        @Override
        public void focusGained(FocusEvent arg0) 
          {
            // System.out.println( "TextViewer.focusGained()" );
            // TheJTable.repaint();  // bug fix Kluge to display cell in correct color.  
            }
      
        @Override
        public void focusLost(FocusEvent arg0) 
          {
            // System.out.println( "TextViewer.focusLost()" );
            // TheJTable.repaint();  // bug fix Kluge to display cell in correct color.  
            }
      
    // command methods.

      private void CommandGoToParentV() 
        /* Tries to go to and display the parent of this object. */
        { // CommandGoToParentV().
          TreePath ParentTreePath=  // get the parent of selection.
            GetSelectedChildTreePath().getParentPath();
          TreePath GrandParentTreePath=  // try getting parent of the parent.
            ParentTreePath.getParentPath();
          { // process attempt to get grandparent.
            if (GrandParentTreePath == null)  // there is no grandparent.
              ; // do nothing.  or handle externally?
            else  // there is a parent.
              { // record visit and display parent.
                MetaTool.  // In the visits tree...
                  UpdatePath( // update recent-visit info with...
                    GetSelectedChildTreePath()  // ...the new selected TreePath.
                    );
                SetSelectedChildTreePath( GrandParentTreePath );  // kluge so Notify will work.
                NotifyTreeSelectionListenersV( false );  // let listener handle it.
                } // record visit and display parent.
            } // process attempt to get parent.
          } // CommandGoToParentV().

      /* private void CommandGoToChildV() 
        /* Tries to go to and displays a presentlly selected child 
          of the present DataNode.  
          */
        /*
        { // CommandGoToChildV().
          /* can't do this in text, yet.
          if  // act only if a child selected.
            ( SelectedDagNode != null )
            { // go to and display that child.
              NotifyTreeSelectionListenersV( false );  // let listener handle it.
              } // go to and display that child.
          */
        /*
          } // CommandGoToChildV().
        */
      
    // state updating methods.
    
      private void UpdateEverythingForSubject(TreePath TreePathSubject)
        /* This grouping method updates everything needed to display 
          the List named by TreePathSubject as a JTextArea
          It adjusts the instance variables, 
          the JTextArea ListModel, and the scroller state.
          It also notifies any dependent TreeSelectionListeners.
          */
        { // UpdateEverythingForSubject()
          if ( UpdateReentryBlockedB )  // process unless this is a reentry. 
            { // do not update, because the reentry blocking flag is set.
              System.out.println( 
                "TextViewer.java UpdateEverythingForSubject(...), "+
                "UpdateReentryBlockedB==true"
                );
              } // do not update, because the reentry blocking flag is set.
            else // reentry flag is not set.
            { // process the update.
              UpdateReentryBlockedB= true;  // disallow re-entry.
              
              SetSubjectRelatedVariablesFrom(TreePathSubject);
              //UpdateJTextAreaStateV();  // Updates JTextArea selection and scroller state.

              UpdateReentryBlockedB=  // we are done so allow re-entry.
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
          SubjectDataNode=  // store new list DataNode at end of TreePath.
            (DataNode)SubjectTreePath.getLastPathComponent();

          DataNode ChildDataNode=  // Try to get the child...
            MetaTool.  // ...from the visits tree that was the...
              UpdatePathDataNode( // ...most recently visited child...
                InSubjectTreePath  // ...of the List at the end of the TreePath.
                );
          if (ChildDataNode == null)  // if no recent child try first one.
            { // try getting first ChildDagNode.
              if (SubjectDataNode.getChildCount() <= 0)  // there are no children.
                ChildDataNode= StringObjectEmpty;  // use dummy child place-holder.
              else  // there are children.
                ChildDataNode= SubjectDataNode.getChild(0);  // get first ChildDagNode.
              } // get name of first child.
          
          SetSelectionRelatedVariablesFrom( ChildDataNode );
          } // SetSubjectRelatedVariablesFrom(InSubjectTreePath()
          
      private void SetSelectionRelatedVariablesFrom( DataNode ChildDataNode )
        /* This grouping method calculates values for and stores
          the child-related instance variables based on the ChildDagNode.
          It assumes the base variables are set already.
          */
        { // SetSelectionRelatedVariablesFrom( ChildUserObject ).
          //SelectedDagNode= ChildDagNode;  // Save selected DataNode.
          TreePath ChildTreePath=  // Calculate selected child TreePath to be...
            SubjectTreePath.  // ...the base TreePath with...
              pathByAddingChild( ChildDataNode );  // ... the child added.
          //SelectionNameString=   // Store name of selected child.
          //  ChildDagNode.GetNameString();
          SetSelectedChildTreePath( ChildTreePath );  // select new TreePath.
          } // SetSelectionRelatedVariablesFrom( ChildUserObject ).

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

      /*
      private void UpdateJTextAreaStateV()
        /* This grouping method updates the JTextArea state,
          including its list Model, selection, and Scroller state,
          to match the selection-related instance variables.
          Note, this might trigger a call to 
          internal method ListSelectionListener.valueChanged(),
          which might cause further processing and calls to
          esternal TreeSelectionListeners-s. */
      /*
        { // UpdateJTextAreaStateV()
          UpdateJTextAreaListModel();
          if  // Update other stuff if...
            ( TheJTextArea.getModel().getSize() > 0 ) // ... any rows in model.
            { // Update other stuff.
              UpdateJTextAreaSelection();  // Note, this might trigger Event-s.
              UpdateJTextAreaScrollState();
              } // Update other stuff.
          } // UpdateJTextAreaStateV()
      */

      /*
      private void UpdateJTextAreaListModel()
        /* This grouping method updates the JTextArea's ListModel from
          the selection-related instance variables.
          */
      /*
        { //UpdateJTextAreaListModel()
          DefaultListModel<DataNode> TheDefaultListModel= 
            (DefaultListModel<DataNode>)TheJTextArea.getModel();
          TheDefaultListModel.clear();  // empfy the ListModel.
          for   // Add all the children.
            (int i = 0; i < SubjectDataNode.getChildCount(); i++)  // each child...
            TheDefaultListModel.addElement( SubjectDataNode.getChild( i ) );
          } //UpdateJTextAreaListModel()
      */
      /*
      private void UpdateJTextAreaSelection()
        /* This grouping method updates the JTextArea selection state
          from the selection-related instance variables.
          Note, this will trigger a call to 
          internal method ListSelectionListener.valueChanged(),
          which might cause further processing and calls to
          esternal TreeSelectionListeners-s. */
      /*
        { // UpdateJTextAreaSelection()
          int IndexI= // try to get index of selected child.
            SubjectDataNode.getIndexOfChild( SelectedDagNode );
          if ( IndexI < 0 )  // force index to 0 if child not found.
            IndexI= 0;
          TheJTextArea.  // set selection using final resulting index.
            setSelectionInterval( IndexI, IndexI );
          }  // UpdateJTextAreaSelection()
      */
      /*
      private void UpdateJTextAreaScrollState()
        /* This grouping method updates the JTextArea scroll state
          from its selection state to make the selection visible.
          */
      /*
        { // UpdateJTextAreaScrollState()
          ListSelectionModel TheListSelectionModel = // get ListSelectionModel.
            (ListSelectionModel)TheJTextArea.getSelectionModel();
          //int SelectionIndexI =   // get index of selected element from the model.
          //  TheListSelectionModel.getMinSelectionIndex();
          int SelectionIndexI= // cache selection index.
            //TheJTable.getSelectedRow();
              TheListSelectionModel.getMinSelectionIndex() ;
          //TheJTextArea.ensureIndexIsVisible( // scroll into view...
          //  SelectionIndexI // ...the current selection.
          //  );
          }  // UpdateJTextAreaScrollState()
      */
      
    // rendering methods.  to be added.
      
    } // TextViewer
