package allClasses.ifile;

import java.io.File;
//// import java.util.List;

import javax.swing.JComponent;
import javax.swing.tree.TreePath;

import allClasses.DataNode;
import allClasses.DataTreeModel;
import allClasses.NamedLeaf;

public class IDirectory 

  extends INamedList

  {

    /* This class is a TUDNet hierarchy DataNode which represents 
      a directory in an OS filesystem.
      It contains a list of 0 or more children which are
      files, sub-directories, or both.

      This class does lazy evaluation of its list elements.
      Initially each list element is stored only as the slement's name
      in the form of an instance of the NamedLeaf class.
      Evaluation can happen in 2 ways:
      * When and if a particular element is needed 
        for something other than its name, and it has not yet been evaluated,
        the element is evaluated and the NamedLeaf instance
        is replaced with an instance of either an IFile or an IDirectory.
      * If the entire list is needed in evaluated form,
        any elements that have not yet been evaluated are evaluated
        before the list is returned. 

      ///org
      This class does not distinguish duplicate links to files and directories,
      from links to separate copies of files and directories.
      This can't be done until the TUDNet hierarchy changes
      from a tree to a DAG.
  
      */


    // Constructors and initialization.

    public IDirectory( String pathString, Object childNameObjects[]) 
      /* pathString provides provides the name of this directory.
       * childNameObjects provides the names of the children.
       * 
       * This constructor is used by the IRoots() constructor,
       * for the list of filesystem roots, 
       * which has no File instance associated with it.
       */
      { 
        super(pathString);
        initializeChildrenFromChildNameObjectsV(childNameObjects);
        }

    private IDirectory(File theFile)
      /* theFile provides the name, attributes, and children
       * of the directory.  theFile must be non-null.
       */
      { 
        this(
            theFile, // Name of this directory.
            theFile.list() // This directory's children.
            );
        }

    private IDirectory(File theFile, Object childNameObjects[]) 
      /* Non-null theFile provides name, attributes.
       * childNameObjects provides the names of the children.
       */
      { 
        super(theFile); // Store name of this directory.
        initializeChildrenFromChildNameObjectsV(childNameObjects);
        }

    protected void initializeChildrenFromChildNameObjectsV(
        Object childNameObjects[])
      /* This method is part of the lazy evaluation system.
       * This method initializes the superclass's child storage
       * from the childNameObjects array.
       * childNameObjects may contain any Objects 
       * provided they all have unique names.
       * Each element of child storage will be set to contain
       * one instance of the NamedLeaf class with 
       * the name from one element of the childNameObjects array.
       * If more than the name of a child is ever needed,
       * the NamedLeaf will be evaluated and replaced with either 
       * an IFile or an IDirectory with the same name.
       */
      {
        if ( childNameObjects == null ) // If input array is null,
          childNameObjects=  // replace it with 
            new String[ 0 ]; // a zero-length array of anything.

        for // For every input child Object, add its name to this DataNode. 
          ( // For every index position in input child name array
              int childI= 0; childI<childNameObjects.length; childI++
              ) 
          { // add the associated input child's name to our DataNode.
            childMultiLinkOfDataNodes // In our superclass's child storage 
              .addV( // add, which in this case means append
                childI, // at the next index location,
                NamedLeaf.makeNamedLeaf( // a temporary NamedLeaf DataNode
                  childNameObjects[childI] // with the associated child's
                    .toString()) // name.
                );
            }
        }


    // A subset of delegated DataTreeModel methods.

    public boolean isLeaf()
      /* This method returns false because directories can have children, 
       * therefore they are branches, not leaves.
       */
      {
        return false; 
        }

    public DataNode evaluateDataNode(DataNode childDataNode)
      /* This method returns childDataNode evaluated 
       * in the context of this DataNode which should be its parent.
       * If childDataNode has already been evaluated
       * then it returns childDataNode.
       * 
       * This method does not read any children from the cache.
       * This method does not write a new evaluated child to the cache.
       * This method does not link a new child to its parent-to-be or
       * unlink the old child from its present parent.
       * This method does not propagate LogLevel or TreeLevel from the parent.
       * These things must be done by the caller.
       */
      {
        goReturn: {
          if // If child value is not a lazy evaluation place-holder 
            (! (childDataNode instanceof NamedLeaf)) {
              break goReturn; // exit with input child as evaluated result.
              }
          String childString= // Get name of child. 
              childDataNode.getNameString();
          File childFile= // Calculate File path name from child name. 
              new File(getFile(),childString);
          if (childFile.isDirectory()) // Calculate DataNode based on File type.
            childDataNode=  // Calculate new child from child name
              new IDirectory(childFile); // to be directory.
            else
            childDataNode=  // Calculate new child from child name
              new IFile(childFile); // to be regular file.
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
