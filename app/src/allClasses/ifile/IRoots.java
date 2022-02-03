package allClasses.ifile;

import java.io.File;

public class IRoots

  extends IDirectory

  {

    /* This class is a TUDNet hierarchy node which represents 
     * a list of the user's OS file system roots.
     *
     * This class subclasses IDirectory for 
     * its ability to deal with lists of OS directories,
     * including lazy evaluation of those OS directories,
     * though this class is not itself a directory.
     * 
     */

    // Variables.


    // Constructors.

    public IRoots() 
      {
        super(
          "File-System-Roots", // Name for this node.
          File.listRoots() // List of Filesystem root directory Objects.
          );
        }

    }
