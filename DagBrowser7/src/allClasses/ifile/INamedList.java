package allClasses.ifile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;

import allClasses.NamedList;

public class INamedList 

  extends NamedList
  
  /* This class is meant to be the base class for all
   * ifile / IFile-related classes.
   */
  
  {
  
    // Variables.
      
      protected File theFile;  // File name path associated with this node.

      public File getFile()
        /* This method returns theFile associated with this DataNode.  */
        {
          return theFile;
          }

    // Object overrides.

      public boolean equals( Object pathObject )
        /* Compares this to pathIObject.  
          This is not a complete implementation of equals(..) for subclasses,
          but it suffices for how it is used.
          */
        {
          boolean resultB= false;
          if (pathObject instanceof INamedList) {
              INamedList otherINamedList= (INamedList) pathObject;
              resultB= theFile.equals( otherINamedList.theFile );
              }
          return resultB;
          }

      public int hashCode() 
        // Returns the hash code of the single File field.
        {
          return theFile.hashCode();
          }

    // interface DataNode methods.

      public String getMetaDataString()
        /* Returns a String representing information about this object. */
        { // GetInfoString()
          String resultInfoString= "";
          try { // Build information string about file.
            resultInfoString+= ""
              + "Name=\"" + getNameString() + "\""; // file name.
            resultInfoString+= " Size=" + theFile.length(); // file size.
            
            Path ThePath= theFile.toPath();  // Convert to Path for following.
            if ( Files.isDirectory( ThePath, LinkOption.NOFOLLOW_LINKS ) )
              resultInfoString+= " Directory";
            if ( Files.isRegularFile( ThePath, LinkOption.NOFOLLOW_LINKS ) )
              resultInfoString+= " RegularFile";
            if ( Files.isSymbolicLink( ThePath ) )
              resultInfoString+= " SymbolicLink";
            if ( Files.isReadable( ThePath ) )
              resultInfoString+= " Readable";
            if ( Files.isWritable( ThePath ) )
              resultInfoString+= " Writable";
            /* These always return true, so don't use.  
              if ( Files.isExecutable( ThePath ) )
                resultInfoString+= " isExecutable";
              if ( theFile.canExecute() )
                resultInfoString+= " canExecute";
              */
            if ( Files.isHidden( ThePath ) )
              resultInfoString+= " Hidden";
            } // Build information string about file.
          catch ( Throwable AThrowable ) {  // Handle any exception by...
            resultInfoString+= " "+AThrowable;  // ...appending its description to string.
            }
          return resultInfoString;  // return the accumulated information string.
          } // GetInfoString()

      public String getNameString()
        /* Returns a String representing the name of this Object.  
          This is the last element of the File path.
          If the path represents a file or directory
          then it is the last name in the path.
          If it represents a filesystem root,
          then it is the path prefix, which is also
          the entire canonical path.
          */
        {
          String stringResult=   // Try getting the last name element.
            theFile.getName();

          if // Get the prefix if there is no name.
            ( stringResult.equals( "" ) )
            try {
              stringResult= // Get the prefix which is actually...
                theFile.getCanonicalPath();
              } catch (IOException e) {
                stringResult= "IOException";  // Get error string.
              }  // ...the canonical path name.

          return stringResult;  // Return the final result.
          }

      public String toString() 
        /* This is needed because it appears that JList uses toString() but
          JTree uses something else (getName()?).
          */
        { 
          return getNameString(); 
          }

    }
