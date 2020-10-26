package allClasses.ifile;

import java.io.File;

//// import java.io.File;

public class IRoot

  extends IDirectory

  /* This class is the root of the Infogora user's file system.  
   * It is similar in structure and operation to IDirectory,
   * which is used for other, non-root folders.
   */

  { // class IRoot

    // Variables.
    
    // Constructors.
  
    public IRoot() 
      {
        super(
            "IRoot-Roots", // Name for this node.
            File.listRoots() // List of Filesystem roots.
            );
        }

    /*  ////
    public void Root()  ////////////// 
      {
        super(
            //// null, // Null parent.
            new File("IRoot-File!"), // Null parent of filesystem roots. 
            File.listRoots() // Filesystem roots.
            );
        /*  ////
        childFiles= File.listRoots();
        IDirectory();
        initializeChildrenFromObjectsV(File.listRoots());
        */  ////
        /*  ////
        }
        */  ////
    
    // A subset of delegated AbstractDataTreeModel methods.

    } // class FileRoots
