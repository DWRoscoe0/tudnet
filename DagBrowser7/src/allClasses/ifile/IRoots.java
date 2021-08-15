package allClasses.ifile;

import java.io.File;

public class IRoots

  extends IDirectory

  /* This class represents the root of the TUDNet user's filesystems.
   * It is the root of only the user's filesystems, 
   * not the TUDNet hierarchy, or the user's part of it that hierarchy,
   *   
   * This class subclasses IDirectory for 
   * its ability to deal with lists of other directories.
   * This class is not a directory itself,
   * so it is constructed without its own File object.
   */

  {

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
