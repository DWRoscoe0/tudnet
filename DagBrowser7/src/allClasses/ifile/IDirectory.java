package allClasses.ifile;

import java.io.File;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.tree.TreePath;

import allClasses.DataNode;
import allClasses.DataTreeModel;
import allClasses.NamedLeaf;

public class IDirectory 

  extends INamedList

  /* This class extends DataNode to represent files and directories.

    It does not distinguish duplicate links to 
    files and directories from full copies of files and directories.
    Java JTrees can't have duplicate siblings.
    */
  
  {
  
    // Variables.
  
      private boolean childrenValidB= false; // Means all children validated.
    
    // Constructors and initialization.

      public IDirectory( String pathString, Object childObjects[]) 
        /* pathString provides provides the name.
         * childObjects provides the children.
         */
        { 
          super(pathString);
          initializeChildrenFromObjectsV(childObjects);
          }

      private IDirectory(File theFile)
        /* Non-null theFile provides name, attributes, 
         * and the directory's children.
         */
        { 
          this(
              theFile, // Name of this directory.
              theFile.list() // This directory's children.
              );
          }

      private IDirectory(File theFile, Object childObjects[]) 
        /* Non-null theFile provides name, attributes.
         * childObjects provides the children.
         */
        { 
          super(theFile); // Store name of this directory.
          initializeChildrenFromObjectsV(childObjects);
          }

      protected void initializeChildrenFromObjectsV(Object childObjects[])
        /* Sets up the child cache array from the childObjects array.  */
        {
          if ( childObjects == null )  // Make certain the array is not null.
            childObjects=  // Make it be a zero-length array of something.
              new String[ 0 ];

          for (int indexI= 0; indexI<childObjects.length; indexI++) {
            childMultiLinkOfDataNodes.addV( // Store in the NamedList's array
                indexI, // at this location
                NamedLeaf.makeNamedLeaf( // this name place-holder.
                    childObjects[indexI].toString())
                );
            }
          }

    // Object overrides.

    // A subset of delegated DataTreeModel methods.

      public boolean isLeaf()
        {
          return false; // Because directories are branches and can have children.
          }

      public List<DataNode> getChildListOfDataNodes()
        /* This method returns the list of directory entries.
         * If the entire list has not been validated yet,
         * it validates every entry by getting each child.
         * After validation has been assured, it returns the list.
         */
      {
        if (! childrenValidB) { // Validate entire list if needed.
          for // For all the children.
            (
              int childIndexI= 0; 
              childIndexI < getChildCount();
              childIndexI++
              )
            getChild(childIndexI); // Evaluate and retrieve child.
          childrenValidB= true; // Mark entire list valid.
          }
        return childMultiLinkOfDataNodes.getListOfEs();
        }

      public DataNode getChild( int indexI ) 
        /* This returns the child with index IndexI.
          It gets the child from the child cache if it is there.
          If not then it calculates the child and 
          saves it in the cache for later.
          In either case it returns a reference to the child.
          */
        { // getChild( int IndexI ) 
          DataNode childDataNode= null;

          goReturn: {
            if  // Exit if index out of bounds.
              ( indexI < 0 || indexI >= childMultiLinkOfDataNodes.getCountI())
              break goReturn; // exit with null.
            childDataNode= childMultiLinkOfDataNodes.getE(indexI);
            if  // If did not get lazy evaluation place-holder 
              (! (childDataNode instanceof NamedLeaf)) {
                break goReturn; // exit with that as evaluated result.
                }
            String childString= // Get name of child. 
                childDataNode.getNameString();
            File childFile= new File(getFile(),childString);
            if (childFile.isDirectory()) // Convert based on type.
              childDataNode=  // Calculate new child from child name
                new IDirectory(childFile); // to be directory.
              else
              childDataNode=  // Calculate new child from child name
                new IFile(childFile); // to be regular file.
            childDataNode.setParentToV( this ); // Set this as child's parent.
            childMultiLinkOfDataNodes.setE( // Save in cache.
                indexI, childDataNode);
            } // goReturn:

          return childDataNode;
          }

      public int getIndexOfChild( Object childObject ) 
        /* Returns the index of childObject in directory ParentObject.  
          It does this by searching for a child with a matching name.
          This works regardless of how many the children
          have been converted from temporary simpler INamedList DataNodes.
          */
        {
          DataNode childDataNode= (DataNode)childObject;
          String childString= childDataNode.toString(); // Get name of target.

          int resultI = -1;  // Initialize result indicating child not found.
          for // Search for child with matching name. 
            ( int i = 0; i < childMultiLinkOfDataNodes.getCountI(); ++i) 
            {
              if 
                ( childString.equals( 
                    childMultiLinkOfDataNodes.getE(i).getNameString()))
                {
                  resultI = i;  // Set result to index of found child.
                  break;
                  }
              }

          return resultI;
          }

    // interface DataNode methods.
      
      public JComponent getDataJComponent(
          TreePath inTreePath,
          DataTreeModel inDataTreeModel 
          )
        /* Returns a JComponent capable of displaying the 
         * IDirectory at the end of inTreePath. 
         */
        {
          JComponent resultJComponent= // Return a directory table viewer.
                new DirectoryTableViewer(inTreePath, inDataTreeModel);
          return resultJComponent;  // return the final result.
          }
          
    // other methods.

    }
