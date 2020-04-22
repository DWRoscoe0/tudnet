package allClasses;

import java.awt.Component;
import java.awt.event.InputEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Enumeration;
import java.util.Vector;

import javax.swing.JComponent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreePath;

/// import static allClasses.AppLog.theAppLog;

public class TreeHelper 
  
  implements KeyListener, MouseListener, FocusListener

  /* This TreeHelper class, along with the associated TreeAware interface,
    is used to add capabilities to TreeAware JComponent subclasses.

    The reason these 2 classes are used instead of 
    adding needed code to JComponent subclasses is:
    * there is a large amount of needed code, and
    * Java lacks multiple inheritance.

    TreeHelpers and TreeWare JComponents complement each other and work together.
    They are linked together by Event Listeners and other methods.
    Both JComponent subclasses and TreeHelper can be subclassed as needed.

    The present TreeHelper code is mostly about commands for 
    navigating, viewing, and manipulating the app's tree of DataNodes.

    When a JComponent's subclass has a TreeHelper, 
    the variable that references it is typically named "theTreeHelper".
    So, for example, code to access the present Part TreePath:
    would be "theTreeHelper.getPartTreePath()";

    Access to a JComponent's TreeHelper can be had by calling 
    the getTreeHelper() method of the TreeAware interface.
    
    It is possible to move most code in a TreeAware JComponent subclass
    to a nested non-static TreeHelper subclass. 
    An appropriate name for this sub-class is MyTreeHelper.
    Instance methods can be added or overridden here as needed.
    These methods can access all of the fields of the TreHelper subclass 
    and all of the fields of the JComponent subclass.
    These methods can include overrides of Event handlers of 
    several Event Listener interfaces implemented by TreeHelper.

    A TreeHelper object instance typically interacts with 2 other objects:
    * Its owning, DataNode viewing, TreeAware JComponent, 
      such as TitledListViewer, TitledTextViewer, and RootJTree.
    * A coordinating JComponent, presently only DagBrowserPanel,
      used to coordinate the simultaneous display of 
      2 side by side TreeAware JComponents.

    The above mentioned 3 objects interact in the following ways.
    
      * The coordinating component listens for TreeHelper Part TreePathEvent-s
        and makes TreePath adjustments in the components which
        it is coordinating.
    
      * The coordinating component calls a TreeHelper setting method
        or a command method which sets the TreeHelper Part TreePath.

      * The viewer component listens for its own selection events and
        translates them into Part TreePath settings in the TreeHelper
        by calling a TreeHelper Part setting method.
    
      * The viewer component listens for TreeHelper Part TreePathEvent-s
        and translates them into viewer component selections.

      * The TreeHelper listens for KeyEvent-s and MouseEvent-s and
        gives standard meaning to some of them.

    Concepts and their names:

    * Whole (formerly Subject): 
      The tree node displayed by this JComponent.

    * Part (formerly Selection within Subject): 
      Highlighted tree node within the Whole,
      usually one of its children.

    * Selection: The thing selected in this Component.
      This is the standard Java meaning.
      It is not a node in the tree,
      though it might be closely associated with a node in the tree.

    * TreePathEvent: The class used to communicate locations of
      Wholes and Parts within the Infogora DAG.
      In earlier versions the TreeSelectionEvent was used.

    Things provided by this class:

      * A constructor.

      * Initialization method initializeHelperV():
        This method is called by the coordinating JComponent.
        The name might be misleading because its job is to initialize both
        the TreeHelper and any initialization not done by 
        its associated JComponent's constructor.
        This base class method does default initialization, including
        making the TreeHelper a listener for various events.
        If overridden by a JComponent's nested non-static [aka inner] 
        TreeHelper class subclass,
        then it can also do some JComponent initialization. 

      * Finalization method finalizeHelperV():
        The minimum purpose of this method is to prevent Listener memory leaks.
        This can happen because TreeAware components are TreeModelListeners.
        The TreeModel holds references to its listeners.
        When one TreeAware viewer JComponent is replaced by another in a JPanel
        the old component must be unregistered as a TreeModelalistener,
        otherwise it would not be garbage collected.
        It doesn't unregister other types of listeners because there is no need.

      * Default keyboard and mouse listener methods for 
        handling of user input common to tree node viewers
        which differ from the default ones provided by Java libraries.

      * Command methods, which can be used for
        testing for the validity of, or actual execution of,
        various tree cursor moving commands (next, previous, child, parent).

      * Methods for getting and setting the Whole,
        by TreePath, with optional auto-select.

      * Methods for getting and setting the Part,
        by TreePath, DataNode, and maybe later child Index.

      * Code which manages and uses TreePathListener-s.

    This class was originally intended for 
    right panel viewer JComponents only,
    but now it is used by RootJTree in the left navigation panel also.

    Because there are closed TreePathListener paths between 
    the coordinating component and TreeHelpers, 
    and between the owning component and its TreeHelper,
    there is the danger of infinite recursion.
    This is now detected and prevented in TreeHelper by testing for
    a new TreePath which is not different from the old one,
    and taking no action if they are equivalent.

    ///org? Divide into 2 classes:
      * ComponentHelper which knows about Listeners, but not trees.
      * TreeHelper extends ComponentHelper   

    ///enh?? This class will probably be changed to not force the Whole 
    to be a direct parent of the Part, as it is now.
    It would remain constant through the life of the object.
    This will allow it to be used for more components, 
    including JTree with Whole being the tree root.
    
    ///doc?? Break-out an interface of public methods for documentation purposes.

    ///doc?? Name this class something else, such as TreeLogic.

    */

  { // class TreeHelper

    protected JComponent owningJComponent;  /* TreeAware JComponent being helped.
      This is the JComponent to which other parts of the system will refer.
      That JComponent has a reference to this object also,
      so this linkage might be referred to as mutual-composition.  
      */
    MetaRoot theMetaRoot;

    // Constructor, initialization, and finalization.
      
      TreeHelper( 	// Constructor.
          JComponent inOwningJComponent,
          MetaRoot theMetaRoot,
          TreePath inWholeTreePath  // Eliminate this constructor injection.
          )
        /* Constructs a TreeHelper.
          inWholeTreePath identifies the root of the Whole subtree to display.
          The Part TreePath is auto-selected if possible.
          initializeHelperV(..) must be called later to complete initialization.
          */
        {
          this.owningJComponent= inOwningJComponent; // Saving owning JComponent.
          this.theMetaRoot= theMetaRoot;
  
          setWholeWithPartAutoSelectV(  // Making initial sSelection.
            inWholeTreePath 
            );
          }
      
      public void initializeHelperV( 
      		TreePathListener theTreePathListener,
      		FocusListener theFocusListener,
      		DataTreeModel theDataTreeModel
      		)
        /* This method is called after construction to do or finish initialization
          of both this TreeHelper and its associated TreeAware JComponent,
          This default version does the following:
          * It makes this TreeHelper be both a FocusListener and a KeyListener
            of the TreeAware JComponent.  This makes it easy for a JComponent
            to respond to Focus or Key events by simply adding 
            the appropriate Listener interface method to its TreeHelper subclass.
          * It adds theTreePathListener to this TreeHelper 
            so that it can respond to and distribute TreePathEvents.
          * It sets DataTreeModel to be theDataTreeModel,
            which includes making the TreeAware JComponent a listener of
            theDataTreeModel. 

          This method can be overridden in a subclass, usually to add functionality.
          
      	  See the sister method, finalizeHelperV(), which unsets the DataTreeModel.
      	  */
        {
      		// appLogger.debug("TreeHelper.initializeHelperV(.) begins.");

      		// Doing final common listener registrations.
  	      owningJComponent.addFocusListener(this); // Making this TreeHelper 
  	        // be a FocusListener of its owning JComponent.
          addFocusListener( theFocusListener ); // Making coordinating
            // components FocusListener be a FocusListener of this Helper.

  	      owningJComponent.addKeyListener(this); // Make this TreeHelper
  	      // be a KeyListener of its owning JComponent.
  	      ///? Should we addMouseListener(this) also?

      	  addTreePathListener( theTreePathListener ); // Coordinating Component.

      	  // Setting the TreeModel, which also sets JComponent as TreeModelListener.
          setDataTreeModelV(theDataTreeModel); // This also registers a Listener.
        
          // appLogger.debug("TreeHelper.initializeHelperV(.) ends.");
        	}

      public void finalizeHelperV() 
  	  	/* This method, or its equivalent in a subclass, 
    		  does JTreeHelper and TreeAware Component finalization. 
      	  It exists mainly for undoing non-final listener registrations
      	  to prevent listener leaks.
      	  It does this by setting the TreeModel to null.

      	  Not all Listeners need to be unregistered.
      	  The Infogora TreeModelListeners must be unregistered because
      	  they are components with shorter scope than the TreeModel
      	  and many of them can be created, used briefly, and then dereferenced.
      	  Failure to unregister them would result in massive Listener memory leaks.
    		  */
        {
        	setDataTreeModelV( null ); // Unregister TreeModelListener JComponent.
          }
      
    // TreeModel code with Listener registration, etc.

      protected DataTreeModel theDataTreeModel;
      
      public void setDataTreeModelV(DataTreeModel newDataTreeModel)
      /* Sets new DataTreeModel by storing it in a TreeHelper variable,
        and it adjusts TreeModelListener registrations.

        First it adjusts TreeModelListener registrations.
        If the present DataTreeModel is not null,
        then the owning JComponent is a listener of that model and will be unregistered.
        Next, if newDataTreeModel is not null,
        the owning JComponent will be registered as a listener of the newDataTreeModel.

        After listener registrations are done, the newDataTreeModel 
        is stored in a TreeHelper variable as the present DataTreeMode.

        In normal usage this method will be called only twice per JComponent:
        * once with newDataTreeModel != null, during component initialization,
        * and once with newDataTreeModel == null during component finalization,
          mainly to prevent TreeModelListener memory leaks.
        However it should be able to be called any number of times
        with any of 4 null/not-null combinations.
        */
      {
        // appLogger.debug("TreeHelper.setDataTreeModel(.) begins with "+newDataTreeModel);'
        if // If component implements TreeModelListener then adjust registrations.
          (owningJComponent instanceof TreeModelListener) 
          { // Link or unlink JComponent as TreeModelListener.
            DataTreeModel oldDataTreeModel= theDataTreeModel;
            if ( oldDataTreeModel != null ) {
              // theAppLog.debug("TreeHelper.setDataTreeModel(.) "
              //     + "removing TreeModelListener " + owningJComponent);
              oldDataTreeModel.removeTreeModelListener(
                (TreeModelListener)owningJComponent);
              }
            if ( newDataTreeModel != null ) {
              newDataTreeModel.addTreeModelListener(
                (TreeModelListener)owningJComponent);
              }
            }
        theDataTreeModel= newDataTreeModel; // Finally, store the new model.
        // appLogger.debug("setDataTreeModelV.setDataTreeModel(.) ends with "+newDataTreeModel);
        }
      
    // FocusListener code: registration, firing, and the interface.

      /* This code deals with FocusEvents.
        The TreeHelper is automatically made 
        a FocusListener of its JComponent.
        
        The TreeHelper's FocusEvent handler methods can be used in 2 ways:
        * The handler methods can be used to pass FocusEvents to 
          other objects by making those objects FocusListeners of 
          the TreeHelper.  This is their default behavior.
        * The handler methods can be overridden by a TreeHelper subclass
          used by the JComponent.
        
        An object should be a FocusListener of either a JComponent or
        its TreeHelper, but not both.
        */

      private Vector<FocusListener> focusListenerVector =   // Listeners.
        new Vector<FocusListener>();

      public void addFocusListener( FocusListener listener ) 
        {
          if ( listener != null && 
              !focusListenerVector.contains( listener ) 
              )
            focusListenerVector.addElement( listener );
          }

      public void removeFocusListener(FocusListener listener)
        {
          if ( listener != null )
            focusListenerVector.removeElement( listener );
          }

      public void focusGained(FocusEvent theFocusEvent)
        /* This interface method simply passes the event and the call to 
         * focus listeners of this TreeHelper instance.
          Note, even though the FocusListener was registered with
          this TreeHelper, listener calls and FocusEvents 
          will appear be come from another object, 
          the source in the FocusEvent that is received by this TreeHelper.
          */
        {
      	  /* debug 
		      appLogger.debug(
		        "TreeHelper.focusGained(...) for "
		      	+ Misc.componentInfoString(owningJComponent)
		      	+ " begin," + NL + "  gained by "
		        + Misc.componentInfoString(theFocusEvent.getComponent())
		       	+", lost by "
		        + Misc.componentInfoString(theFocusEvent.getOppositeComponent())
		       	);
		      */ 
          Enumeration<FocusListener> listeners = 
            focusListenerVector.elements();
          while ( listeners.hasMoreElements() ) 
            {
              FocusListener listener = 
                (FocusListener)listeners.nextElement(); // caste.
              listener.focusGained( theFocusEvent );
              }
	        /* debug
	        appLogger.debug(
	          "TreeHelper.focusGained(...) for "
	        	+ Misc.componentInfoString(owningJComponent)
	        	+ " end."
	          );
	        */
          }

      public void focusLost(FocusEvent theFocusEvent)
        /* This interface method simply passes 
          the event and the call to other listeners.
          */
        { 
      		/* debug 
		      appLogger.debug(
		        "TreeHelper.focusLost(...) for "
		      	+ Misc.componentInfoString(owningJComponent)
		      	+ " begin," + NL + "  lost by "
		        + Misc.componentInfoString(theFocusEvent.getComponent())
		       	+", gained by "
		        + Misc.componentInfoString(theFocusEvent.getOppositeComponent())
		       	);
		       */
          Enumeration<FocusListener> listeners = 
            focusListenerVector.elements();
          while ( listeners.hasMoreElements() ) 
            {
              FocusListener listener = 
                (FocusListener)listeners.nextElement();
              listener.focusLost( theFocusEvent );
              }
      		/* debug 
	        appLogger.debug(
	           "TreeHelper.focusLost(...) for "
	           + Misc.componentInfoString(owningJComponent)
	           + " end."
	           );
	        */
          }

    // KeyListener methods.
    
      /* KeyListener methods, for 
        overriding normal Tab key processing
        and providing command key processing.

        Normally the Tab key moves the selection within a Table
        from cell to cell.  The modification causes Tab 
        to move keyboard focus out of the table to the next Component.  
        (Shift-Tab) moves it in the opposite direction.
        
        ?? Maybe replace with a more compact KeyAdapter implementation.
        */
    
      public void keyPressed(KeyEvent inKeyEvent) 
        /* Processes KeyEvent-s.  
          The keys processed and consumed include:
            * Tab and Shift-Tab for focus transferring.
            * Right-Arrow and Enter keys to go to child.
            * Left-Arrow keys to go to parent.
          */
        { // keyPressed.
          int keyCodeI = inKeyEvent.getKeyCode(); // Copying key pressed.
          boolean keyProcessedB=  // Assuming key event will be processed.
            true;
          { // Trying to process the key event.
            // /* Tab decoded elsewhere by JList.
            if (keyCodeI == KeyEvent.VK_TAB) // Handling Tab key maybe.
              { // Handling Tab key.
                Component sourceComponent= 
                  (Component)inKeyEvent.getSource();
                int shift = // Determine (Shift) key state.
                  inKeyEvent.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK;
                if // Moving focus forward or backward. 
                  (shift == 0) // (Shift key) not down.
                  sourceComponent.transferFocus();  // Move forward.
                  else   // (Shift key) is down.
                  sourceComponent.transferFocusBackward(); // Move backward.
                } // process Tab key.
            else 
            // Tab decoded elsewehre by JList.
            // */
            if (keyCodeI == KeyEvent.VK_LEFT) // Handling left-arrow maybe.
              commandGoToParentB(true);  // Going to parent folder.
            else if (keyCodeI == KeyEvent.VK_RIGHT) // right-arrow maybe.
              commandGoToChildB(true);  // Going to child folder.
            else if (keyCodeI == KeyEvent.VK_ENTER)  // Enter maybe.
              commandGoToChildB(true);  // Going to child folder.
            else  // no more keys to check.
              keyProcessedB= false;  // Indicating no key was handled.
            }
          if  // Preventing processing by Event source of key handled.
            (keyProcessedB)
            inKeyEvent.consume(); // Preventing more processing of the key.
          } // keyPressed.

      public void keyReleased(KeyEvent inKeyEvent) { }  // Unused method.
      
      public void keyTyped(KeyEvent inKeyEvent) { }  // Unused method.

    // MouseListener methods.

      /* MouseListener methods, for user input from mouse.
        Because this is a helper class, putting the MouseListener methods
        in a MouseAdapter subclass would not be very helpful.
        */

      @Override
      public void mouseClicked(MouseEvent inMouseEvent) 
        /* Checks for double click on mouse,
          which now means to go to the child folder,
          so is synonymous with the right arrow key.
          */
        {
          if (inMouseEvent.getClickCount() >= 2)
            commandGoToChildB(true);  // go to child folder.
          }
          
      @Override
      public void mouseEntered(MouseEvent arg0) { }  // Unused method.
      
      @Override
      public void mouseExited(MouseEvent arg0) { }  // Unused method.
      
      @Override
      public void mousePressed(MouseEvent arg0) { }  // Unused method.
      
      @Override
      public void mouseReleased(MouseEvent arg0) { }  // Unused method.
      
    // Command methods.

      /* Each of the following command methods can be used for two things:
        * Executing particular commands.
        * Testing whether particular commands are legal,
          given the present cursor/selection possibles, etc.
          This is useful in determining whether command gadgets,
          such as buttons and menus, should be grayed out.

        ?? Maybe implement the Command pattern.
        In Java the Command pattern is implemented with the Action interface
        which extends the ActionListener interface.
        This might do everything I eventually want to do.
        */

      public boolean commandGoToParentB( boolean doB  ) 
        /* Tries to go to the parent node.
          First it checks whether moving is doable,
          that there is enough valid TreePath 
          to move toward the parent.
          If there is and doB is true then it removes one element 
          from the present Part path and notifies the TreePath listeners.
          In any case it returns whether moving was doable.
          */
        {
          boolean doableB= false;  // Assuming command isn't legal.

          toReturn: {
            // Exiting if not enough path for movement toward parent.
              TreePath a0TreePath= getPartTreePath();  // Getting present TreePath.
              if (a0TreePath == null)  // Exiting if null.
                break toReturn;
              TreePath a1TreePath= a0TreePath.getParentPath(); // Getting its parent.
              if ( ! testSetPartTreePathB( a1TreePath ) )  // Handling illegal parent.
                break toReturn;
   
            doableB= true;  // Override result to indicate command is legal.

            if (! doB)  // Command execution is not desired.
              break toReturn; // So exit now with calculated result.

            setSelectionAndNotifyListenersV(  // Moving by notifying listeners that...
              a1TreePath  // ...parent of part path should be new part path.
              );

          } // toReturn
            return doableB;  // Return doable result.
          }

      public boolean commandGoToChildB( boolean doB )
        /* Tries to go to an appropriate child if doB is true.
          It returns true if the command is/was doable, false otherwise.
          To facilitate the viewing of leaves, though they have no children,
          it returns true for them and executes the command by 
          adding an UnknownDataNode to the end of the present Part path.  
          ?? This method is too long.  Break it up.
          */
        {
            boolean doableB= false;  // Assuming command is not doable.
          toReturn: {
            if ( getPartDataNode() == null )  // No part path.
              break toReturn; // So exit with not doable.
            if // Part is itself an UnknownDataNode.
              ( UnknownDataNode.isOneB( getPartDataNode() ) )
              break toReturn; // So exit with not doable.
            DataNode childDataNode= null;  // Storage for findingChild result.
          findingChild: {
            childDataNode=  // Try to find best child of part node.
              findBestChildDataNode( getPartTreePath() );
            if (childDataNode != null) break findingChild;  // Exit if found.
            if (!getPartDataNode().isLeaf())  // It is an empty non-leaf.
              break toReturn; // So exit with not doable.
            childDataNode=  // Use a place holder child for entering leaf.
              UnknownDataNode.newUnknownDataNode();
          } // findingChild end.
            doableB= true;  // Override result to indicate command doable.

            if (! doB)  // Command execution is not desired.
              break toReturn; // So exit with result.

            // Command execution begins.
	          /* debug
	          appLogger.debug(
	             "TreeHelper.commandGoToChildB(...) command execution begin."
	             );
	          */
            setSelectionAndNotifyListenersV(  // Set new part path to be...
              getPartTreePath().pathByAddingChild( // ...present part plus...
                childDataNode  // ...whatever child was found.
                )
              );  // Using setPartTreePathV(..) here doesn't work.
        	  /* debug 
						appLogger.debug(
	             "TreeHelper.commandGoToChildB(...) command execution end."
	             );
	          */
          } // toReturn end.
            return doableB;  // Return whether command is/was doable.
          }

      public boolean commandGoToNextB( boolean doB )
        /* Tries to go to the next node if doB is true.
          It returns true if the command is/was doable, false otherwise.
          */
        { return commandGoToPreviousOrNextB( doB, +1 ); }
      
      public boolean commandGoToPreviousB( boolean doB )
        /* Tries to the previous node if doB is true.
          It returns true if the command is/was doable, false otherwise.
          */
        { return commandGoToPreviousOrNextB( doB, -1 ); }
      
      public boolean commandGoToPreviousOrNextB( 
          boolean doB, int incrementI 
          )
        /* This is a helper method.  It is called by
          commandGoDownB(..) and commandGoUpB(..).
          If incrementI == +1 it tries to go to the next node.
          If incrementI == -1 it tries to go to the previous node.
          */
        { 
            boolean doableB= false;  

          toReturn: {
            TreePath a0TreePath= getPartTreePath();
            if (a0TreePath == null) break toReturn;  // Check base path.
            TreePath a1TreePath= a0TreePath.getParentPath();
            if (a1TreePath == null) break toReturn;  // Check parent path.
            DataNode a0DataNode= (DataNode)a0TreePath.getLastPathComponent();
            if // Check that base node is not an UnknownDataNode.
              ( UnknownDataNode.isOneB( a0DataNode ) )
              break toReturn;
            DataNode a1DataNode= (DataNode)a1TreePath.getLastPathComponent();
            int a0IndexI= // Get present index.
               a1DataNode.getIndexOfChild( a0DataNode );
            a0IndexI+= incrementI;  // Increment index to go up or down one.
            DataNode a0NewDataNode= a1DataNode.getChild(a0IndexI);
            if (a0NewDataNode == null) break toReturn;  // Check new node.
            
            doableB= true;  // Indicate that command is doable.
            if (! doB)  // Command execution is not desired.
              break toReturn; // So exit with result.
            
            setPartDataNodeV(a0NewDataNode);  // Move to sibling.

          } // toReturn
            return doableB;
            
          }
          
    // Whole-related code.  
    
      /* The Whole is the whole tree node being viewed.
        This was previously called the Subject.
        It tends to be the immediate parent of the Part,
        but this might change when viewsrs are able to display
        more than one level.  

        After initialization, Whole is never changed??
        */

      private TreePath theWholeTreePath;  /* TreePath of whole node displayed.  
        This variable is never null.
        */

      private DataNode theWholeDataNode;  /* DataNode of Whole.
        This is the last element of the theWholeTreePath.
        This variable is never null.
        */

      public TreePath getWholeTreePath( )
        /* This method returns the TreePath representing the node
          being displayed.
          */
        { 
          return theWholeTreePath;  // Return it.
          }

      public DataNode getWholeDataNode()
        /* This method returns the DataNode of the Whole.  */
        { 
          return theWholeDataNode;  // return DataNode of Part.
          }

      private void setWholeV( TreePath inTreePath )
        /* This method stores inTreePath as the TreePath of the Whole
          which is stored in theWholeTreePath.
          The Part variables are set to to non-null sentinel values.
          This method should be used only when there is no accessible Part,
          or the part will be set as a later separate step.
          Presently now it is called only by setWholeWithPartAutoSelectV(..)
          which is called only by constructor.

          To prevent infinite recursion it acts only if
          inTreePath is different from the present theWholeTreePath.
          */
        { 
          if  // Updating state if new TreePath if different from old one.
            ( ! inTreePath.equals( theWholeTreePath ) )

            { // Updating state with new TreePath.

              { // Store Whole Part variables.
                theWholeTreePath= inTreePath;  // Store Whole TreePath.
                theWholeDataNode=  // Store last element for easy access.
                  (DataNode)theWholeTreePath.getLastPathComponent();
                }

              { // Setting Part variables to non-null sentinel.
                thePartDataNode= UnknownDataNode.newUnknownDataNode();
                thePartTreePath= theWholeTreePath.pathByAddingChild( 
                  thePartDataNode 
                  );
                }
                
              } // Update TreePath variables using the new value.
          }

        private void setWholeWithPartAutoSelectV(TreePath inTreePath)
          /* This method sets the Whole variables from inTreePath
            and sets the Part also to the most recently
            selected child of the Whole, if there is one.
            It is presently called only by a TreeHelper constructor,
            which constructs using a Whole TreePath.
            */
          {
            if (inTreePath != null)  // TreePath is not null.
              { // Set things from inTreePath.
                setWholeV( inTreePath );  // Save Whole TreePath no
                DataNode childDataNode=  // Calculate best child.
                  findBestChildDataNode(inTreePath);
                if (childDataNode != null)  // There is an accessible child.
                  setPartDataNodeV( childDataNode );  // Store it.
                } // Set things from inTreePath.
            }

      private DataNode findBestChildDataNode(TreePath inTreePath)
        /* This method finds a child for autoselect.
          It tries to find and return the best child DataNode 
          of the node identified by inTreePath.  The best is:
            * The most recently visited child, if there is one.
            * The first child, if there is one.
          Otherwise it returns null.

          ?? Maybe instead of null it should return UnknownDataNode?
          */
        {
          DataNode childDataNode=  // Try to get the child...
            theMetaRoot.  // ...from the visits tree that was the...
              setAndReturnDataNode( // ...most recently visited child...
                inTreePath  // ...of the List at the end of the TreePath.
                );
          if (childDataNode == null)  // if no recent child try first one.
            { // try getting first ChildDataNode.
              DataNode theDataNode=  // Get DataNode at end of TreePath.
                (DataNode)inTreePath.getLastPathComponent();
              if   // There are no children.
                (theDataNode.getChildCount() <= 0)
                  ;  // Do nothing.
                else  // There are children.
                  childDataNode=   // get first ChildDataNode.
                    theDataNode.getChild(0);
              } // get name of first child.
          return childDataNode;  // Return final result.
          }

    // Part-related code.  
      
      /* The Part is the tree node displayed highlighted within the Whole.
        It it one of possibly multiple child nodes being displayed.
        This was previously called the Selection,
        but was changed to avoid confusion with selection within
        the JComponent being used to represent the Whole.
        The part variables should probably never remain null.
        thePartDataNode might be the UnknownDataNode,
        which means that the whole is a leaf,
        which can be displayed but not as a collection of children.
        */

      private TreePath thePartTreePath; /* TreePath of Part.  
        The parent of this TreePath is theWholeTreePath
        which represents the entire set being displayed.
        This assumes that there is only one sub-level displayed,
        so all items displayed have the same parent.
        This restriction might be removed later if
        multiple levels are displayed simultaneously.
        This variable is null if the Whole contains 
        no highlighted child.
        */

      private DataNode thePartDataNode; /* DataNode of Part.
        This is the last element of the thePartTreePath.
        This variable is null if the JComponent is displaying 
        a node with no highlighted child.
        */

      private int thePartIndexI; // Index of Part.

      protected TreePath getPartTreePath()
        /* This method returns the TreePath representing the Part. */
        { 
          return thePartTreePath;  // return TreePath of Part.
          }

      protected DataNode getPartDataNode()
        /* This method returns the DataNode of the Part.  */
        { 
          return thePartDataNode;  // return DataNode of Part.
          }

      public int getPartIndexI() // Return child/Part index.
        { return thePartIndexI; }

      public boolean setPartTreePathB( TreePath inTreePath ) // Set TreePath.
        /* This method sets the Part TreePath to inTreePath if it is legal.
          It returns whether the TreePath was legal.
          It is equivalent to setPartTreePathB( inTreePath, true ).
          */
        { 
          return setPartTreePathB( inTreePath, true );
          }

      private boolean setPartTreePathB( // Test or test and set TreePath.
          TreePath inTreePath, boolean doB )
        /* This method tests the legality of setting a TreePath or sets it.
          It always tests whether inTreePath is
          a legal value for the Part TreePath.
          It returns true if it is legal.  It returns false otherwise.
          If it is legal and doB is true then
          it actually sets the Part TreePath to inTreePath.

          It calls the TreePathListener-s as part of this process but 
          not if inTreePath is the same as the present thePartTreePath.
          
          ?? This used to set Whole to be its parent, but no longer does.
          It is the responsibility of part TreePathListener-s to
          change the JComponent if the whole needs to be changed.
          */
        { 
          boolean legalB= testSetPartTreePathB( inTreePath );
          if ( legalB && doB ) 
            doSetPartTreePathB( inTreePath );
          return legalB;
          }

      boolean testSetPartTreePathB(TreePath inTreePath)
        /* This method tests whether inTreePath is legal to be displayed
          by the owning JComponent in the present context,
          which depends on the component in which 
          the owning JComponent is nested.
          It returns true if legal, false otherwise.
          */
        { 
          boolean legalB;
          goReturn: {
            if ( inTreePath == null )  // Handling case of null inTreePath.
              { legalB= false; break goReturn; } // Exit, not legal.
            if  // Handling case of no change in path.
              ( inTreePath.equals( thePartTreePath ) )
              //{ legalB= true; break goReturn; } // Exit, legal.
            	{ legalB= false; break goReturn; } // Exit, not legal.
            TreePath parentTreePath=  // Getting parent of path.
              inTreePath.getParentPath();
            if  // Handling case of null parent of the root-parent.
              ( parentTreePath == null )
              { legalB= false; break goReturn; } // Exit, not legal.
            if  // Handling rejection of path by any of the listeners.
              ( ! fireTestPartTreePathB( inTreePath ) )
              { legalB= false; break goReturn; } // Exit, not legal.

            legalB= true;  // Setting legal because path passed all tests.
            }
          return legalB;
          }

      void doSetPartTreePathB(TreePath inTreePath)  // Assuming it's legal.
        /* This method is used to set the Part assuming that legality checks
          have already been done.
          */
        { 
          thePartTreePath= inTreePath;  // Storing Part TreePath.
          thePartDataNode=  // Storing last element as Part DataNode.
            (DataNode)thePartTreePath.getLastPathComponent();
          TreePath parentTreePath=  // Getting parent of path.
            inTreePath.getParentPath();
          DataNode parentDataNode=
            (DataNode)parentTreePath.getLastPathComponent();
          thePartIndexI=  // Storing index of child DataNode.
            parentDataNode.getIndexOfChild( thePartDataNode );
          setSelectionAndNotifyListenersV( inTreePath );
          }  

      protected void setPartDataNodeV( DataNode inDataNode )
        /* This method sets the Part DataNode to be inDataNode.
          It updates other variables appropriately.
          It also notifies any TreePathListener about it.
          This method no longer uses the Whole variables.
          */
        {
          TreePath childTreePath= // Calculate child TreePath to be...
            thePartTreePath  // ...present path's...
              .getParentPath()  // ...parent with...
              .pathByAddingChild( inDataNode );  // ...new child added.
          setPartTreePathB( childTreePath );  // Set Part TreePath.
          }

    // TreePathListener code for adding, removing, and triggering with them.

      /* This code manages TreePathListener-s and sends TreePathEvent-s to them.

        Typically each TreeHelper instance will have two listeners:

          * owningJComponent, the JComponent being helped.
            This is how selections are made in the owningJComponent
            which are not the result of user input directly to
            that component.

          * The DagBrowserPanel, which coordinates the left and right panels.
            This is how the coordinating DagBrowserPanel learns of
            selections in the owningJComponent resulting from
            user input directly to that component.

        The TreePathListener interface has one method for 
        testing a TreePath value for legality and another for 
        setting a TreePath value.
        */
      
      private Vector<TreePathListener> listenerVector =   // Listeners.
        new Vector<TreePathListener>();

      public void addTreePathListener( TreePathListener listener ) 
        {
          if ( listener != null && !listenerVector.contains( listener ) )
            listenerVector.addElement( listener );
          }

      public void removeTreePathListener(TreePathListener listener)
        {
          if ( listener != null )
            listenerVector.removeElement( listener );
          }

      public void notifyListenersAboutChangeV( )
        /* This method is used to notify listeners that 
          something other than the node TreePath, 
          such as the display of other nodes, has changed,
          and there is no need to reload the entire right sub-panel.
          This was communicated by sending a TreePath of null,
          but now it resends the unchanged Part TreePath.

          Presently it is used to communicate when a node 
          is expanded or collapsed, which can happen when,
          when in the RootJTree highlighted node, either:
          * The (Enter) key is pressed.
          * The mouse is double-clicked.
          See RootJTree and Infogora details.
          */
        {
          fireSetPartTreePathV(  // Notify the listeners about node change
            getPartTreePath()  // by sending an unchanged path.
            );
          }

      private void setSelectionAndNotifyListenersV( TreePath inTreePath )
        /* This method records inTreePath as most recent selection and
          notifies the listeners about our newest TreePath.
          */
        {
          theMetaRoot.set( inTreePath );  // Record as a selection.
          fireSetPartTreePathV(  // Notify the listeners about...
            inTreePath  // ...to the Part path.
            );
          }

      private void fireSetPartTreePathV( TreePath inTreePath )
        /* Notifies any TreePathListeners of the setting of the
          Part TreePath to be inTreePath.
          It returns true if the event's operation was legal,
          false otherwise.
          */
        {
          TreePathEvent theTreePathEvent= //Constructing the event,...
            new TreePathEvent (  // ...a TreePathEvent from...
              owningJComponent  // ...the source JComponent being helped,...
              ,inTreePath  // ...and the TreePath of interest,
              ); 
          Enumeration<TreePathListener> listeners =  // Getting listenres.
            listenerVector.elements();
          while  // Calling listeners while...
            ( theTreePathEvent.getLegalB()  // ...it's still legal and...
              && listeners.hasMoreElements()  // ...there are more listeners.
              )
            { // Calling one listener.
              TreePathListener listener =  // Getting next listener.
                (TreePathListener)listeners.nextElement();
              listener.setPartTreePathV( theTreePathEvent );  // Calling it.
              }
          //theTreePathEvent.getLegalB(); // Returning whether successful.
          }

      private boolean fireTestPartTreePathB( TreePath inTreePath )
        /* Calls TreePathListeners to test whether inTreePath is legal
          as the setting of the Part TreePath in the present display context.
          It returns true if all the listeners return true, that the TreePath is legal.
          It returns false as soon as any listener returns false.
          */
        {
          TreePathEvent theTreePathEvent= //Constructing the event,...
            new TreePathEvent (  // ...a TreePathEvent from...
              owningJComponent  // ...the source JComponent being helped,...
              ,inTreePath  // ...and the TreePath of interest,
              ); 
          Enumeration<TreePathListener> listeners =  // Getting listeners.
            listenerVector.elements();
          boolean legalB= true;  // Assuming inTreePath is legal.
          while  // Calling listeners while...
            ( legalB  // ...it's still legal and...
              && listeners.hasMoreElements()  // ...there are more listeners.
              )
            { // Calling one listener.
              TreePathListener listener =  // Getting next listener.
                (TreePathListener)listeners.nextElement();
              legalB= listener.testPartTreePathB( theTreePathEvent );  // Calling it.
              }
          return legalB;  // Returning whether legal.
          }

    } // class TreeHelper
