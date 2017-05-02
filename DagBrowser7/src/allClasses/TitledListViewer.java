package allClasses;

//import static allClasses.Globals.appLogger;

//import static allClasses.Globals.appLogger;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
//import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
//import java.awt.event.MouseEvent;





import javax.swing.BorderFactory;
//import javax.swing.BoxLayout;
//% import javax.swing.DefaultListCellRenderer;
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

public class TitledListViewer // adapted from TitledListViewer.

  extends JPanel
 
  implements 
    ListSelectionListener
    , FocusListener
    , TreeAware
    , TreeModelListener
  
  /* This class was developed from, and is intended to replace, 
    the ListViewer class.
    It was developed and tested without disrupting other code by 
    temporarily replacing single ListViewer references to 
    TitledListViewer references for the duration of the test.

    This class provides a simple titled DagNodeViewer that 
    displays and browses List-s using a JList.
    It was created based on code from DirectoryTableViewer.
    Eventually it might be a good idea to create 
    a static or intermediate base class that handles common operations.
    */
    
  { // TitledListViewer
  
    // variables, most of them.

      private JLabel titleJLabel;  // Label with the title.

      //% private JList<Object> theJList;  // Component with the content.
      private JList<DataNode> theJList;  // Component with the content.
      private TreeListModel theTreeListModel;  // Model with the content.

      private Color cachedJListBackgroundColor;

    // constructor and constructed-related methods.

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
            aTreeHelper=  // Construct helper object to be
              new MyTreeHelper(  // an instance of my customized TreeHelper.
                this, theDataTreeModel.getMetaRoot(), inTreePath 
                );  // Note, subject not set yet.
            
            // Code moved to MyTreeHelper.initializeV().
            } // Prepare the helper object.

          } // TitledListViewer(.)

      class MyTreeHelper  // TreeHelper customization subclass.

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
          	/* This method is the start and end of the initialization of 
		      	  this JComponent/TreeAware and its TreeHelpe, 
		      	  not including what should be very simple construction. 
		      	  */
            {
        	    initializeHelpeeV( theDataTreeModel );

            	super.initializeHelperV( // This links the common listeners.
	          		theTreePathListener,
	          		theFocusListener,
	          		theDataTreeModel
	          		);
            	
            	// Non-final settings.
    		      setTitleTextV();
              setJListSelection();
              setJListScrollState();
            	}
            	  
          public void finalizeHelperV() 
    	      {
          		finalizeHelpeeV( theDataTreeModel );
        			super.finalizeHelperV();
    	        }

          public void setDataTreeModelV(DataTreeModel newDataTreeModel)
            /* Sets new DataTreeModel.
             * It also makes the present ListModel be a Listener of
             * newDataTreeModel so the former reflects the latter.
             * The JList should be made a Listener of the ListModel
  		       * For a given instance this method will normally be called twice:
  		       * * once with newDataTreeModel != null during initialization,
  		       * * and once with newDataTreeModel == null during finalization,
  		       * but it should be able to work with any null combination.
             */
            {
          		super.setDataTreeModelV( newDataTreeModel ); // Needed??

  		    		//appLogger.debug("TitledListViewer.setDataTreeModel()\n  theTreeListModel: "+theTreeListModel);

          	  theTreeListModel.setDataTreeModel( newDataTreeModel );
          	  }

          } // MyTreeHelper

  	  public void initializeHelpeeV( DataTreeModel theDataTreeModel)
  	    /* This method initializes the owning JComponent/TreeAware, 
  	      aka Helpee.  It is called by TreeHelper.
  	      Final initialization is followed by non-final initialization.
  	      */
	    	{ // initializeHelpeeV(..).
	  	  	{ // Final initialization.
		        cachedJListBackgroundColor= getBackground();  // Saving background for later use.
		        setLayout( new BorderLayout() );
		        
		        titleJLabelInitializationV(); // Initializing titleJLabel.
	
		      	theJListInitializationV( theDataTreeModel );
		      	
		      	// Final listener registrations.
            aTreeHelper.addTreePathListener(   // Listen for TreePath changes.
                theTreePathListener
                );
            theJList.addKeyListener(aTreeHelper);
            theJList.addMouseListener(aTreeHelper);
            theJList.addFocusListener(aTreeHelper);
            theJList.addFocusListener(this);  // For kludgy Java bug fix.
            addFocusListener(this);  // For kludgy Java bug fix.
            theJList.getSelectionModel().
              addListSelectionListener(this);
	  	  		}

          { // Non-final initialization.
          	// A listener registration that must be undone to prevent leak.
            theDataTreeModel.  // For displaying changing title.
              addTreeModelListener(this);
            }
	      	} // initializeHelpeeV(..).

  		public void finalizeHelpeeV( DataTreeModel theDataTreeModel )
	  		{
	      	// A listener registration that must be undone to prevent leak.
	        theDataTreeModel.  // For displaying changing title.
	          removeTreeModelListener(this); // Remove what was previously added.
	  			
	  		  }
  		
      public void titleJLabelInitializationV()
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

      private void theJListInitializationV( DataTreeModel theDataTreeModel )
        /* This grouping method creates and initializes the JList.  
          */
        { // theJListInitializationV( )
	        //% theJList= new JList<Object>();  // Construct JList.
      		theJList= new JList<DataNode>();  // Construct JList.
	        add(theJList,BorderLayout.CENTER); // Adding it to main JPanel.
	        //add(theJList); // Adding it to main JPanel.
          { // Set ListModel for the proper type of elements.
            theTreeListModel= new TreeListModel(
          		aTreeHelper.getWholeDataNode( ),
          		aTreeHelper.getWholeTreePath( )
              );
          	theTreeListModel.setDataTreeModel( theDataTreeModel );
          	theJList.setModel( theTreeListModel );  // Define its ListModel.
            } // Set ListModel for the proper type of elements.
          theJList.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
          theJList.setCellRenderer(   // Setting custom rendering.
            theTitledListCellRenderer 
            );
          } // theJListInitializationV( )

	    private void setTitleTextV() // Sets text in titleJLabel.
	      /* This method is called during initialization and DataNode change
	        to update the displays title text.
	       	*/
		   	{
	        titleJLabel.setText(
	        		aTreeHelper.getWholeDataNode().getLineSummaryString( )
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
            aTreeHelper.getWholeDataNode( ).getIndexOfChild( 
              aTreeHelper.getPartDataNode() 
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
        Only treeNodesInserted(..) does anything.
        It updates the displayed title if the TreePath is of this List.
        Changes to the List itself are handled by the ListModel,
        which is itself also a TreeModelListener.
        */

		    public void treeStructureChanged(TreeModelEvent theTreeModelEvent)
		      { 
		    		//appLogger.debug("TitledListViewer.treeStructureChanged()\n  "+theTreeModelEvent);
			    	}

		    public void treeNodesRemoved(TreeModelEvent theTreeModelEvent) 
		      { 
		    		//appLogger.debug("TitledListViewer.treeNodesRemoved()\n  "+theTreeModelEvent);
		      	}
		
		    public void treeNodesInserted(TreeModelEvent theTreeModelEvent) 
		      { 
		    		//appLogger.debug("TitledListViewer.treeNodesInserted()\n  "+theTreeModelEvent);
		      	}
		
		    public void treeNodesChanged(TreeModelEvent theTreeModelEvent) 
		      { 
		        //appLogger.debug("TitledListViewer.treeNodesChanged()\n  "+theTreeModelEvent);
		    	  if ( // Ignoring event if parent TreePath doesn't match our List's. 
	    	      	!aTreeHelper.getWholeTreePath().getParentPath().equals(
	    	      			theTreeModelEvent.getTreePath()
	    	      			)
	    	      	)
  	    	  	; // Ignoring.
	    	  	else
	    	  		for // Updating title text if our List matches a child. 
	    	  		  (Object childObject: theTreeModelEvent.getChildren()) 
	    	  			if ( childObject == aTreeHelper.getWholeDataNode())
	  				    	setTitleTextV(); // Updating title text.
		      	}

    /* ListSelectionListener interface method, 
     * for processing ListSelectionEvent-s from the List's SelectionModel.
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
    			Misc.requestFocusV(theJList);
      	  setJListScrollState();
          repaint();  // bug fix Kluge to display cell in correct color.  
          }
    
      @Override
      public void focusLost(FocusEvent arg0) 
        {
      		//appLogger.debug("TitledListViewer.focusLost() adjusting JList.");
          setJListScrollState();
          repaint();  // bug fix Kluge to display cell in correct color.  
          }

    // interface TreeAware code for TreeHelper access.

			public TreeHelper aTreeHelper;

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
        { // MyTreePathListener

          public void setPartTreePathV( TreePathEvent inTreePathEvent )
            /* This TreePathListener method translates 
              inTreePathEvent TreeHelper tree path into 
              an internal JList selection.
              It ignores any paths with which it cannot deal.
              */
            {
              selectRowV(   // Select row appropriate to...
                aTreeHelper.getPartIndexI() // index.
                );  // Note, this might trigger ListSelectionEvent.
              }

          } // MyTreePathListener

    // List cell rendering.

      ////////private TitledListCellRenderer theTitledListCellRenderer=
      ////////  new TitledListCellRenderer(); // for custom cell rendering.
      private ListCellRenderer<? super DataNode> theTitledListCellRenderer=
      		new TitledListCellRendererOfDataNodes(); // for custom cell rendering.
        //// setCellRenderer(..) incompatible with this.  super keyword.

      public class TitledListCellRendererOfDataNodes
        extends JLabel
        implements ListCellRenderer<DataNode>
        ////// This will replace TitledListCellRenderer.
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

    } // TitledListViewer
