package allClasses.ifile;

import java.io.File;

import allClasses.NamedList;

public class INamedList 

  extends NamedList
  
  /* This class is meant to be the base class for all
   * ifile / IFile classes.
   */
  
  {
  
    // Variables.
      
      protected File theFile;  // File associated with this node.

      public File getFile()
        /* This method returns the theFile associated with this DataNode.  */
        {
          return theFile;
          }
  
    }
