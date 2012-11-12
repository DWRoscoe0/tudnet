package allClasses;

import java.io.File;
import java.io.Serializable;

import javax.swing.JComponent;
import javax.swing.tree.TreePath;

public class RootTreeModel 
  extends AbstractTreeModel 
  implements Serializable
  
  /* This class implements an extended TreeModel 
    used for browsing the Infogora.
    In addition to implimenting the TreeModel interface, 
    this class also calculates a JComponent which represents 
    the contents of a desired tree node.
    */
    
  { // class RootTreeModel.

    // variables.

        private static final long serialVersionUID = 1L;

        private Object ObjectRoot;  // Object which is root of modelled tree.

    // constructor methods.

      public RootTreeModel( DagNode DagNodeRoot ) 
        /* Constructs a RootTreeModel using TreeModelUserObjectRoot
          as the root of the tree.
          */
        { // RootTreeModel( DagNode TreeModelUserObjectRoot )
          ObjectRoot= DagNodeRoot;  // store root.
          } // RootTreeModel( DagNode TreeModelUserObjectRoot )
    
    // AbstractTreeModel/TreeModel methods.
      
      // input methods.
  
        public void valueForPathChanged
          /* Do-nothing stub to satisfy interface.  */
          ( TreePath TheTreePath, Object NewValueObject ) 
          { }
  
      /* output methods.
        This TreeModel does no filtering,
        so it passes all calls through to the DagNode Object for processing.
        */
  
        public Object getRoot() 
          /* Returns tree root.  */
          {
            return ObjectRoot ;
            }
  
        public Object getChild( Object ParentObject, int IndexI ) 
          /* Returns the Object which is the child of ParentObject
            whose index is IndexI.  The child must exist.
            This operation is delegated to ParentObject which
            is assumed to satisfy the DagNode interface.
            */
          {
            return ((DagNode)ParentObject).getChild( IndexI );
            }
  
        public boolean isLeaf( Object NodeObject ) 
          /* Returns an indication whether NodeObject is a leaf.
            This operation is delegated to NodeObject which
            is assumed to satisfy the DagNode interface.
            */
          {
            return ((DagNode)NodeObject).isLeaf();
            }
  
        public int getChildCount( Object ParentObject ) 
          /* Returns the number of children of the ParentObject.
            This operation is delegated to ParentObject which
            is assumed to satisfy the DagNode interface.
            */
          {
            if ( Misc.ReminderB )
              System.out.println( 
                "RootTreeModel.getChildCount() " +
                ((AbDagNode)ParentObject).IDCode()
                );
            return ((DagNode)ParentObject).getChildCount();
            }
  
        public int getIndexOfChild( Object ParentObject, Object ChildObject ) 
          /* Returns the index of ChildObject in directory ParentObject.
            This operation is delegated to ParentObject which
            is assumed to satisfy the DagNode interface.
            */
          {
            return ((DagNode)ParentObject).
              getIndexOfChild( ChildObject ); 
            }


    // methods which are not part of AbstractTreeModel.

      // output (getter) methods.
        
        public JComponent GetDataJComponent( TreePath InTreePath )
          /* Returns a JComponent which is appropriate for viewing 
            the current tree node represented specified by InTreePath.  
            */
          { // GetDataJComponent.
            DagNode InDagNode= // extract...
              (DagNode)  // ...user Object...
              InTreePath.getLastPathComponent();  // ...from the TreePath.
            JComponent ResultJComponent; 
            try { 
	            ResultJComponent= 
	              //InDagNode.GetDataJComponent( InTreePath );  // ??
                InDagNode.GetDataJComponent( InTreePath, this );
              }
            catch ( IllegalArgumentException e) {
	            ResultJComponent= null;  
              // System.out.println( "GetDataJComponent : "+e);              
              ResultJComponent=  // calculate a blank JLabel with message.
                //new DagNodeViewer( 
                //  InTreePath,
                //  new IJTextArea( "GetDataJComponent : "+e)
                //  );
                new TextViewer( InTreePath, this, "GetDataJComponent : "+e );
              }
            return ResultJComponent;
            } // GetDataJComponent.
  
        public String GetNameString( Object InObject )
          /* Returns a String representing the name of InObject,
            or null if InObject is null.
            This operation is delegated to InObject which
            is assumed to satisfy the DagNode interface.
            */
          {
            String ResultString= null;
            if ( InObject != null )
              ResultString= ((DagNode)InObject).GetNameString();
            return ResultString;
            }

        public String GetLastComponentNameString(TreePath InTreePath)
          /* Returns String represention of the name of 
            the last element of InTreePath.
            */
          {
            DagNode LastDagNode= 
              (DagNode)(InTreePath.getLastPathComponent());
            String TheNameString= 
              LastDagNode.GetNameString();
            return TheNameString;
            }

        public String GetAbsolutePathString(TreePath InTreePath)
          /* Returns String representation of TreePath InTreePath.  
            It presently does this by converting to IFile first.
            
            This could be made more efficient for the common case
            of only the last component changing by 
            encapsulating in a class the TreePath and its
            associated String representation.
            */
          { // GetAbsolutePathString(.)
            String ResultString= "";  // Initialize ResultString to empty.
            while ( InTreePath != null )  // While more TreePath to process...
              { // ...process one element of TreePath onto ResultString.
                DagNode LastDagNode=  // Get last element.
                  (DagNode)(InTreePath.getLastPathComponent());
                String LastNameString=  // Get its name.
                  LastDagNode.GetNameString();
                if  // Add seperator character to ResultString if needed,...
                  ( ResultString.length() != 0)  // ...if it is not empty...
                  ResultString=  // ...by prepending...
                    File.separator +  // ...the standard File seperator...
                    ResultString;  // ...to the ResultString.
                ResultString=  // Prepend...
                  LastNameString +  // ...the element name String...
                  ResultString;  // ...to the ResultString.
                InTreePath=  // Replace the TreePath...
                  InTreePath.getParentPath();  // ...by its parent TreePath.                
                } // ...process one element of TreePath onto ResultString.
            //ResultString=  // Kludge Windows drive letter by prepending...
            //  "DriveC" +  // ...volume prefix...
            //  ResultString;  // ...to the ResultString.
            return ResultString;  // Return completed ResultString.
            } // GetAbsolutePathString(.)

        public String GetInfoString(TreePath InTreePath)
          /* Returns a String representing information about 
            TreePath InTreePath. 
            */
          {
            DagNode LastDagNode= 
              (DagNode)(InTreePath.getLastPathComponent());
            return LastDagNode.GetInfoString();
            }

    } // class RootTreeModel.
