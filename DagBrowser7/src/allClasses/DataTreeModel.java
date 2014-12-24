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

    // Constants.

        private static final long serialVersionUID = 1L;

        private static final String spacyFileSeperator= 
          " "+File.separator+" ";

    // Injected dependency variables.

        private DataRoot theDataRoot;
        private MetaRoot theMetaRoot;

    // constructor methods.

        public DataTreeModel( 
            DataRoot theDataRoot, MetaRoot theMetaRoot 
            )
          {
            this.theDataRoot= theDataRoot;
            this.theMetaRoot= theMetaRoot;
            }
    
    // AbstractTreeModel/TreeModel methods.
      
      // input methods.
  
        public void valueForPathChanged( 
            TreePath TheTreePath, Object NewValueObject 
            )
          /* Do-nothing stub to satisfy interface.  */
          { }
  
      // output methods.
      
        /* These methods simply delegate the call to the ParentObject,
          which is assumed to be a DataNode,
          and can perform the operation context-free.
          This is because this TreeModel does no filtering.
          If and when filtering is done then 
          other processing will be needed.
          */
  
        public Object getRoot() 
          /* Returns tree root.  
            This is not the Infogora root DataNode.
            It is the parent of the root.
            See DataRoot for more information.
            */
          {
            return theDataRoot.getParentOfRootDataNode();
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
  
        public int getIndexOfChild( 
            Object ParentObject, Object ChildObject 
            ) 
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
          {
            DataNode InDataNode= // extract...
              (DataNode)  // ...user Object...
              inTreePath.getLastPathComponent();  // ...from the TreePath.
            JComponent ResultJComponent; 
            try { 
	            ResultJComponent= 
                InDataNode.getDataJComponent( 
                  inTreePath, this 
                  );
              }
            catch ( IllegalArgumentException e) {
	            ResultJComponent= null;  
              ResultJComponent=  // calculate a blank JLabel with message.
                new TitledTextViewer( inTreePath, this, "GetDataJComponent : "+e );
              }
            return ResultJComponent;
            }
  
        public String GetNameString( Object InObject )
          /* Returns a String representing the name of InObject,
            or null if InObject is null.
            This operation is delegated to InObject which
            is assumed to satisfy the DataNode interface.
            */
          {
            String resultString;
            if ( InObject != null )
              resultString= ((DataNode)InObject).getNameString();
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
              lastDataNode.getNameString();
            return TheNameString;
            }

        public String GetAbsolutePathString(TreePath inTreePath)
          /* Returns String representation of TreePath inTreePath.  
            ??? Maybe rewrite to be recursive so that 
            java-optimized += String operation can be used,
            maybe with StringBuilder with an initial capacity based on
            previous value of path String being calculated.
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
                ( theDataRoot.getParentOfRootTreePath( ).equals( inTreePath ) )
                { if // Handling illegal sentinel-only TreePath.
                    ( resultString == "" ) 
                    resultString= lastDataNode.getNameString();
                  break;
                  }
              String headString=  // Making head string be path element name.
                lastDataNode.getNameString();
              if  // Appending separator to headString if needed,...
                ( resultString.length() != 0)  // ...if result String is not empty...
                headString+=  // ...by appending to the head String...
                  spacyFileSeperator; // ...the File separator string.
              resultString=  // Prepending...
                headString +  // ...the head String...
                resultString;  // ...to the resultString.
              inTreePath=  // Replacing the TreePath...
                inTreePath.getParentPath();  // ...by its parent TreePath.                
              }
            resultString=  // Prepend...
              "  " +  // ...some space for looks..
              resultString;  // ...to the resultString.
            return resultString;  // Return completed resultString.
            } // GetAbsolutePathString(.)

        public String GetInfoString(TreePath inTreePath)
          /* Returns a String representing information about 
            TreePath inTreePath. 
            */
          {
            DataNode lastDataNode= 
              (DataNode)(inTreePath.getLastPathComponent());
            return lastDataNode.getInfoString();
            }

        public MetaRoot getMetaRoot()
          {
            return theMetaRoot;
            }

    } // class DataTreeModel.
