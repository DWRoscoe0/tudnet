package allClasses;

// import static allClasses.Globals.appLogger;

//import static allClasses.Globals.appLogger;

//import static allClasses.Globals.appLogger;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreePath;


public class TitledListViewer

  extends JPanel
  
  implements ListSelectionListener, FocusListener, TreeAware, TreeModelListener
  
  /* This class was developed from, and is intended to replace, 
    the ListViewer class.
    It was developed and tested without disrupting other code by 
    temporarily replacing single ListViewer references to 
    TitledListViewer references for the duration of the test.

    This class provides a simple titled DagNodeViewer that 
    displays and browses List-s using a JList.
    It was created based on code from DirectoryTableViewer.

    ///enh Eventually it might be a good idea to create 
    a static or intermediate base class that handles common operations.
    */
    
  { // class TitledListViewer

    // interface TreeAware support code for TreeHelper access.
  
      public TreeHelper theTreeHelper;
  
      public TreeHelper getTreeHelper() { return theTreeHelper; }
    
      class MyTreeHelper  // Customized TreeHelper subclass.
    
        extends TreeHelper 
    
        {
          MyTreeHelper(  // Constructor.
              JComponent inOwningJComponent, 
              MetaRoot theMetaRoot,
              TreePath inTreePath
              )
            {
              super(inOwningJComponent, theMetaRoot, inTreePath);
              }
    
          public void initializeHelperV( 
              TreePathListener theTreePathListener,
              FocusListener theFocusListener,
              DataTreeModel theDataTreeModel
              )
            {
              // appLogger.debug("TitledListViewer.MyTreeHelper.initializeHelperV(.) begins");
  
              initializeComponentV(theDataTreeModel);
  
              super.initializeHelperV( // Use base TreeHelper to link the common listeners.
                  theTreePathListener,
                  theFocusListener,
                  theDataTreeModel
                  );
  
              theJList.addKeyListener(theTreeHelper);
              theJList.addMouseListener(theTreeHelper);
              theJList.addFocusListener(theTreeHelper);
              theJList.addFocusListener((TitledListViewer)owningJComponent); // For kludgy Java bug fix?
              owningJComponent.addFocusListener(
                  (FocusListener)owningJComponent);
                  //// For kludgy Java bug fix.

              // appLogger.debug("TitledListViewer.MyTreeHelper.initializeHelperV(.) ends.");
              }
  
          /*  ////
          public void initializeHelpeeV( DataTreeModel theDataTreeModel) // Useful code.
            /* This method initializes the owning JComponent/TreeAware, 
              aka Helpee.  It is called by TreeHelper.
              Final initialization is followed by non-final initialization.
              */
            /*  ////
            { // initializeHelpeeV(..).
              { // Final initialization.
                cachedJListBackgroundColor= getBackground();  // Saving background for later use.
                setLayout( new BorderLayout() );
                
                titleJLabelInitializationV(); // Initializing titleJLabel.
      
                theJListInitializationV( theDataTreeModel );
                
                // Final listener registrations.
                theTreeHelper.addTreePathListener(   // Listen for TreePath changes.
                    theTreePathListener
                    );
                theJList.addKeyListener(theTreeHelper);
                theJList.addMouseListener(theTreeHelper);
                theJList.addFocusListener(theTreeHelper);
                theJList.addFocusListener(this);  // For kludgy Java bug fix.
                addFocusListener(this);  // For kludgy Java bug fix.
                theJList.getSelectionModel().
                  addListSelectionListener(this);
                }
             */  ////
  
          public void setDataTreeModelV(DataTreeModel newDataTreeModel)
            /* This method does what its base class version does, and something else.
            
              It also makes the present ListModel be a Listener of
              newDataTreeModel so the JList component can use
              the children of the DataNode being viewed as its data source.
              */
            { 
              // appLogger.debug("TitledListViewer.setDataTreeModel(.) begins with "+theDataTreeModel);
  
              super.setDataTreeModelV( newDataTreeModel ); // Let superclass do its work.
  
              theTreeListModel.setDataTreeModelV( // Link JList to tree by linking 
                  newDataTreeModel ); // to the TreeModel.
              
              // appLogger.debug("TitledListViewer.setDataTreeModel(.) ends with "+theDataTreeModel);
              }
    
          } // MyTreeHelper

    // constructor, initialization and finalization methods.

      public TitledListViewer(  // Constructor.
          TreePath inTreePath,
          DataTreeModel theDataTreeModel
          )
        /* Constructs a TitledListViewer.
          inTreePath is the TreePath associated with
          the node of the Tree to be displayed.
          The last DataNode in the path is that object.
          */
        { // TitledListViewer(.)
          super();   // Constructing the superclass JPanel.

          { // Prepare the helper object.
            theTreeHelper=  // Construct helper object to be
              new MyTreeHelper(  // an instance of my customized TreeHelper.
                this, theDataTreeModel.getMetaRoot(), inTreePath 
                );  // Note, subject not set yet.
            
            // Code moved to MyTreeHelper.initializeV().
            } // Prepare the helper object.

          } // TitledListViewer(.)

      public void buildAndAddComponentsV()
        /* This method does part of the initialization of this TreeAware JComponent,
          basically the UI structure of it.
          */
        {
          cachedJListBackgroundColor= Color.WHITE;

          setLayout( new BorderLayout() );

          buildAndAddJLabelV(); // Initializing titleJLabel.

          buildAndAddJListV();
          }
      
      private void initializeComponentV(DataTreeModel theDataTreeModel)
        /* This method does most of the JComponent initialization.
          It is called by the MyTreeHelper.initializeV(..).
          The little initialization that remains is done by TreeHelper.
          */
        {
          buildAndAddComponentsV();
         
          buildAndLinkDataModelsV(theDataTreeModel);

          theJList.getSelectionModel().addListSelectionListener(this);

          ////// THESE v-2-THINGS-^ WHEN REVERSED CAUSES EXCEPTION!
  
          // Non-final settings.
          setTitleTextV();
          setJListSelection();
          setJListScrollState();
  
        }
  
      private void buildAndLinkDataModelsV(DataTreeModel theDataTreeModel)
        /* This method links the data models to the JList.  */
        {
          theTreeListModel= // Create ListModel using tree coordinates. 
            new TreeListModel(
              theTreeHelper.getWholeDataNode( ),
              theTreeHelper.getWholeTreePath( )
              );
  
          theJList.setModel( theTreeListModel ); // Link ListModel to the JList.
          
          theTreeListModel.setDataTreeModelV( // Link TreeModel to ListModel. 
              theDataTreeModel );
          }

      public void buildAndAddJLabelV()
	      {
		      titleJLabel= new JLabel();
		      titleJLabel.setOpaque( true );
		      Font labelFont= titleJLabel.getFont();
		      titleJLabel.setFont( labelFont.deriveFont( labelFont.getSize() * 1.5f) );
		      //titleJLabel.setAlignmentX( Component.CENTER_ALIGNMENT );
		      titleJLabel.setHorizontalAlignment( SwingConstants.CENTER );
		      Border raisedetched = BorderFactory.createEtchedBorder(EtchedBorder.RAISED);
		      titleJLabel.setBorder(raisedetched);
		      add(titleJLabel,BorderLayout.NORTH); // Adding it to main JPanel.
	      	}

      private void buildAndAddJListV()
        {
      		theJList= new JList<DataNode>();  // Construct JList.
          theJList.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
          theJList.setCellRenderer(   // Setting custom rendering.
            theTitledListCellRenderer 
            );
          add(theJList,BorderLayout.CENTER); // Adding it to main JPanel.
          }

	    private void setTitleTextV() // Sets text in titleJLabel.
	      /* This method is called during initialization and DataNode change
	        to update the displays title text.
	       	*/
		   	{
	        titleJLabel.setText(
	        		theTreeHelper.getWholeDataNode().getLineSummaryString( )
	        		);
		   	  }


	    /* ??
      public void setPreferredSize( Dimension inDimension )
        // Use this so theJList will be full width.
        {
          super.setPreferredSize( inDimension );
          theJList.setPreferredSize( inDimension );
          }
      ?? */


      private void setJListSelection()
        /* This grouping method updates the JList selection state
          from the selection-related instance variables.
          Note, this will trigger a call to 
          internal method ListSelectionListener.valueChanged(),
          which might cause further processing and calls to
          external TreeSelectionListeners-s. */
        { // setJListSelection()
          int IndexI= // try to get index of selected child.
            theTreeHelper.getWholeDataNode( ).getIndexOfChild( 
              theTreeHelper.getPartDataNode() 
              );
          if ( IndexI < 0 )  // force index to 0 if child not found.
            IndexI= 0;
          theJList.setSelectionInterval( // Set selection from index.
            IndexI, IndexI 
            );
          }  // setJListSelection()
      

      private void selectRowV(int IndexI)
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
          ListSelectionModel theListSelectionModel = // Get selection model.
            (ListSelectionModel)theJList.getSelectionModel();
          int SelectionIndexI= // Get index of row selected.
            theListSelectionModel.getMinSelectionIndex() ;
          theJList.ensureIndexIsVisible( // Scroll into view the row...
            SelectionIndexI // ...with that index.
            );
          }  // setJListScrollState()

    /* TreeModelListener methods. 
      Most do nothing but are required by the TreeModelListener interface.
      Only treeNodesChanged(..) does anything.
      It updates the displayed title if the TreePath is of this List.
      Changes to the List itself are handled by the ListModel,
      which is itself also a TreeModelListener.
      */

	    public void treeStructureChanged(TreeModelEvent theTreeModelEvent)
	      { 
	    		//appLogger.debug("TitledListViewer.treeStructureChanged()" + NL + "  "+theTreeModelEvent);
		    	}

	    public void treeNodesRemoved(TreeModelEvent theTreeModelEvent) 
	      { 
	    		//appLogger.debug("TitledListViewer.treeNodesRemoved()" + NL + "  "+theTreeModelEvent);
	      	}
	
	    public void treeNodesInserted(TreeModelEvent theTreeModelEvent) 
	      { 
	    		//appLogger.debug("TitledListViewer.treeNodesInserted()" + NL + "  "+theTreeModelEvent);
	      	}
	
	    public void treeNodesChanged(TreeModelEvent theTreeModelEvent) 
	      { 
	        //appLogger.debug("TitledListViewer.treeNodesChanged()" + NL + "  "+theTreeModelEvent);
	    	  if ( // Ignoring event if parent TreePath doesn't match our List's. 
    	      	!theTreeHelper.getWholeTreePath().getParentPath().equals(
    	      			theTreeModelEvent.getTreePath()
    	      			)
    	      	)
	    	  	; // Ignoring.
    	  	else
    	  		for // Updating title text if our List matches a child. 
    	  		  (Object childObject: theTreeModelEvent.getChildren()) 
    	  			if ( childObject == theTreeHelper.getWholeDataNode())
  				    	setTitleTextV(); // Updating title text.
	      	}


    /* ListSelectionListener interface method, 
      for processing ListSelectionEvent-s from the List's SelectionModel.
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
              (IndexI < theTreeHelper.getWholeDataNode( ).getChildCount( ))
              )
            { // Process the selection.
              DataNode newSelectionDataNode=  // Get selected DataNode...
                theTreeHelper.getWholeDataNode( ).
                  getChild(IndexI);  // ...which is child at IndexI.
              theTreeHelper.setPartDataNodeV( newSelectionDataNode );
            } // Process the selection.
          } // void valueChanged(ListSelectionEvent TheListSelectionEvent)

    
    /* FocusListener interface methods.
      This was created initially to fix JTable cell-invalidate/repaint bug.
      It was later expanded to do nested focusing.
			*/

      @Override
      public void focusGained(FocusEvent arg0)
        /* This method makes certain that the JList content pane
          has the focus when this TitledListViewer gains focus.
          It also fixed a repaint bug.
         */
        {
          setJListScrollState();
    			Misc.requestFocusAndLogV(theJList);
          repaint();  // bug fix Kluge to display cell in correct color.  
          }
    
      @Override
      public void focusLost(FocusEvent arg0) 
        {
      		//appLogger.debug("TitledListViewer.focusLost() adjusting JList.");
          setJListScrollState();
          repaint();  // bug fix Kluge to display cell in correct color.  
          }

    private class MyTreePathListener

      extends TreePathAdapter

      /* TreePathListener code, for when TreePathEvent-s
        happen in either the left or right panel.
        This was based on TreeSelectionListener code.
        For a while it used TreeSelectionEvent-s for 
        passing TreePath data.
        */
      
      { // MyTreePathListener
        public void setPartTreePathV( TreePathEvent inTreePathEvent )
          /* This TreePathListener method translates 
            inTreePathEvent TreeHelper tree path into 
            an internal JList selection.
            It ignores any paths with which it cannot deal.
            */
          {
            selectRowV(   // Select row appropriate to...
              theTreeHelper.getPartIndexI() // the selected Part index.
              );  // Note, this might trigger ListSelectionEvent.
            }

        } // MyTreePathListener

    // List cell rendering.

      private ListCellRenderer<? super DataNode> theTitledListCellRenderer=
      		new TitledListCellRendererOfDataNodes(); // for custom cell rendering.

      public class TitledListCellRendererOfDataNodes
        extends JLabel
        implements ListCellRenderer<DataNode>
        {
      	  public TitledListCellRendererOfDataNodes() // Constructor.
	      	  {
	        		setOpaque(true);  // To display normally transparent background.
	      	  	}
      	  
          public Component getListCellRendererComponent
            ( JList<? extends DataNode> theJListOfDataNodes,
              DataNode theDataNode,
              int indexI,
              boolean isSelectedB,
              boolean hasFocusB
              )
            /* Returns a JLabel for displaying a DataNode in a JList. */
            {
          		setText( theDataNode.toString() );
              UIColor.setColorsV(
                this,
                cachedJListBackgroundColor,
              	theDataNode,
                isSelectedB,
                hasFocusB
                );
              return this;
              }

        } // class TitledListCellRendererOfDataNodes    
    
    // variables, most of them.

      private JLabel titleJLabel;  // Label with the title.

      private JList<DataNode> theJList; // Component to display content.
      private TreeListModel theTreeListModel; // Model with the content.

      private Color cachedJListBackgroundColor;

    } // TitledListViewer
