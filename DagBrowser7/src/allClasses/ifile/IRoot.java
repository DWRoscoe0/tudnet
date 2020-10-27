package allClasses.ifile;

import java.io.File;

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
            "File-System-Roots", // Name for this node.
            File.listRoots() // List of Filesystem roots.
            );
        }

    } // class FileRoots
