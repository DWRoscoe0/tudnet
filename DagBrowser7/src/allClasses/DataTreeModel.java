package allClasses;

import java.io.File;
import java.io.Serializable;

import javax.swing.JComponent;
import javax.swing.tree.TreePath;

public class DataTreeModel 
  extends AbstractTreeModel 
  implements Serializable
  
  /* This class implements an extended TreeModel 
    used for browsing the Infogora.
    In addition to implementing the TreeModel interface, 
    this class also calculates a JComponent which represents 
    the contents of a desired tree node as a JComponent.
    */

  { // class DataTreeModel.

    // variables.

        private static final long serialVersionUID = 1L;

        private Object ObjectRoot;  // Object which is root of modelled tree.

    // constructor methods.

        public DataTreeModel( DataNode DataNodeRoot ) 
          /* Constructs a DataTreeModel using TreeModelUserObjectRoot
            as the root of the tree.
            */
          { // DataTreeModel( DataNode TreeModelUserObjectRoot )
            ObjectRoot= DataNodeRoot;  // store root.
            } // DataTreeModel( DataNode TreeModelUserObjectRoot )
    
    // AbstractTreeModel/TreeModel methods.
      
      // input methods.
  
        public void valueForPathChanged
          /* Do-nothing stub to satisfy interface.  */
          ( TreePath TheTreePath, Object NewValueObject ) 
          { }
  
      /* output methods.
        This TreeModel does no filtering,
        so the methods which access a node's contents
        simply delegate the call to the ParentObject,
        which is assumed to be a DataNode,
        and can perform the operation context-free.
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
            is assumed to satisfy the DataNode interface.
            */
          {
            return ((DataNode)ParentObject).getChild( IndexI );
            }
  
        public boolean isLeaf( Object NodeObject ) 
          /* Returns an indication whether NodeObject is a leaf.
            This operation is delegated to NodeObject which
            is assumed to satisfy the DataNode interface.
            */
          {
            return ((DataNode)NodeObject).isLeaf();
            }
  
        public int getChildCount( Object ParentObject ) 
          /* Returns the number of children of the ParentObject.
            This operation is delegated to ParentObject which
            is assumed to satisfy the DataNode interface.
            */
          {
            if ( Misc.ReminderB )
              System.out.println( 
                "DataTreeModel.getChildCount() " +
                ((AbDataNode)ParentObject).IDCode()
                );
            return ((DataNode)ParentObject).getChildCount();
            }
  
        public int getIndexOfChild( Object ParentObject, Object ChildObject ) 
          /* Returns the index of ChildObject in directory ParentObject.
            This operation is delegated to ParentObject which
            is assumed to satisfy the DataNode interface.
            */
          {
            return ((DataNode)ParentObject).
              getIndexOfChild( ChildObject ); 
            }

    // methods which are not part of AbstractTreeModel.

      // output (getter) methods.
        
        public JComponent GetDataJComponent( TreePath inTreePath )
          /* Returns a JComponent which is appropriate for 
            viewing and possibly manipulating
            the current tree node specified by inTreePath.  
            */
          { // GetDataJComponent.
            DataNode InDataNode= // extract...
              (DataNode)  // ...user Object...
              inTreePath.getLastPathComponent();  // ...from the TreePath.
            JComponent ResultJComponent; 
            try { 
	            ResultJComponent= 
	              //InDagNode.GetDataJComponent( inTreePath );  // ??
                InDataNode.GetDataJComponent( inTreePath, this );
              }
            catch ( IllegalArgumentException e) {
	            ResultJComponent= null;  
              // System.out.println( "GetDataJComponent : "+e);              
              ResultJComponent=  // calculate a blank JLabel with message.
                //new DagNodeViewer( 
                //  inTreePath,
                //  new IJTextArea( "GetDataJComponent : "+e)
                //  );
                new TextViewer( inTreePath, this, "GetDataJComponent : "+e );
              }
            return ResultJComponent;
            } // GetDataJComponent.
  
        public String GetNameString( Object InObject )
          /* Returns a String representing the name of InObject,
            or null if InObject is null.
            This operation is delegated to InObject which
            is assumed to satisfy the DataNode interface.
            */
          {
            String resultString;
            if ( InObject != null )
              resultString= ((DataNode)InObject).GetNameString();
              else
              resultString= "NULL-DataTreeModel-Object";
            return resultString;
            }

        public String GetLastComponentNameString(TreePath inTreePath)
          /* Returns String represention of the name of 
            the last element of inTreePath.
            */
          {
            DataNode lastDataNode= 
              (DataNode)(inTreePath.getLastPathComponent());
            String TheNameString= 
              lastDataNode.GetNameString();
            return TheNameString;
            }

        public String GetAbsolutePathString(TreePath inTreePath)
          /* Returns String representation of TreePath inTreePath.  
            ??? It is being modified to handle illegal TreePath values:
            * null
            * a reference to a pseudo-parent sentinal TreePath.
            */
          { // GetAbsolutePathString(.)
            String resultString= "";  // Initializing resultString to empty.
            while (true) {  // While more TreePath to process...
              if // Handling detection of illegal null TreePath terminator.
                ( inTreePath == null )
                { resultString+= "NULL-PATH-TERMINATOR";
                  break;
                  }
              DataNode lastDataNode=  // Getting last path element.
                (DataNode)(inTreePath.getLastPathComponent());
              if // Handling detection of normal TreePath root sentinel.
                ( DataRoot.getParentOfRootTreePath( ).equals( inTreePath ) )
                { if // Handling illegal sentinel-only TreePath.
                    ( resultString == "" ) 
                    resultString= lastDataNode.GetNameString();
                  break;
                  }
              String LastNameString=  // Get its name.
                lastDataNode.GetNameString();
              if  // Add seperator character to resultString if needed,...
                ( resultString.length() != 0)  // ...if it is not empty...
                resultString=  // ...by prepending...
                  File.separator +  // ...the standard File seperator...
                  resultString;  // ...to the resultString.
              resultString=  // Prepend...
                LastNameString +  // ...the element name String...
                resultString;  // ...to the resultString.
              inTreePath=  // Replace the TreePath...
                inTreePath.getParentPath();  // ...by its parent TreePath.                
              }
            return resultString;  // Return completed resultString.
            } // GetAbsolutePathString(.)

        public String GetInfoString(TreePath inTreePath)
          /* Returns a String representing information about 
            TreePath inTreePath. 
            */
          {
            DataNode lastDataNode= 
              (DataNode)(inTreePath.getLastPathComponent());
            return lastDataNode.GetInfoString();
            }

    } // class DataTreeModel.
