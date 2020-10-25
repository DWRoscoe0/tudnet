package allClasses.ifile;

import java.io.File;

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
    
    // Constructors.

      public IDirectory( String pathString ) 
        /* Constructs an IDirectory from pathString.
         * pathString could represent more than one element,
         * but presently this constructor is used only by FileRoots 
         * to create single-element paths for 
         * filesystem partition names (roots).
         */
        { 
          theFile= new File( pathString );
          initializeChildrenV();
          }
    
      public IDirectory( 
          IDirectory ancestorPathIDirectory, String descendantPathString ) 
        /* Constructs an IDirectory by combining the paths
         * from ancestorPathIDirectory and descendantPathString.
         * ancestorPathIDirectory and descendantPathString 
         * could be arbitrary paths,
         * but in this app ancestorPathIDirectory usually represents a directory,
         * and descentantPathString is the name of a file or directory
         * within the first directory. 
         */
        { 
          theFile= 
              new File( ancestorPathIDirectory.theFile, descendantPathString );
          initializeChildrenV();
          }

    // Object overrides.

    // A subset of delegated DataTreeModel methods.

      public boolean isLeaf()
        {
          return false; // Because directories are branches and can have children.
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
            if  // If did got place-holder
              (! (childDataNode instanceof NamedLeaf)) {
                break goReturn; // exit with that as result.
                }
            String childString= // Get name of child. 
                childDataNode.getNameString();
            File childFile= new File(theFile,childString);
            if (childFile.isDirectory()) // Convert based on type.
              childDataNode=  // Calculate new child from child name
                new IDirectory(this, childString); // to be directory.
              else
              childDataNode=  // Calculate new child from child name
                new IFile(this, childString); // to be regular file.
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

      private void initializeChildrenV()
        /* Sets up the child cache array. 
         * This is meaningful only if this IDirectory represents a directory.
         */
        {
          String childStrings[]= null;

          childStrings=  // Define by reading names of children from directory.
            theFile.list();
          if ( childStrings == null )  // Make certain the array is not null.
            childStrings=  // Make it be a zero-length array.
              new String[ 0 ];

          for (int indexI= 0; indexI<childStrings.length; indexI++) {
            childMultiLinkOfDataNodes.addV( // Store in the NamedList's array
                indexI, // at this location
                NamedLeaf.makeNamedLeaf( // this name place-holder.
                    childStrings[indexI])
                );
            }
          }

    }
