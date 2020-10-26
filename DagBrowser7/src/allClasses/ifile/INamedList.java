package allClasses.ifile;

import java.io.File;
//// import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Objects;

import allClasses.NamedList;

public class INamedList 

  extends NamedList
  
  /* This class is meant to be the base class for all
   * ifile / IFile-related classes.
   */
  
  {
  
    // Variables.
      
      protected File theFile;  // File name path associated with this node.
    
    // Constructors and initialization.

      public INamedList(File theFile)
        {
          //// super.initializeV();
          super.initializeV(
              getNameString(theFile)
              );
          this.theFile= theFile;
          }

    // Getter.
      
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
              resultB= Objects.equals(theFile, otherINamedList.theFile );
              }
          return resultB;
          }

      public int hashCode() 
        // Returns the hash code of the single File field.
        {
          return Objects.hashCode(theFile);
          }

    // interface DataNode methods.

      public String getMetaDataString()
        /* Returns a String representing information about this object. */
        { // GetInfoString()
          String resultInfoString= "";
          try { // Build information string about file.
            resultInfoString+= ""
              + "Name=\"" + getNameString() + "\""; // file name.
            if (null != theFile) { // Add the following only if theFile not null.
              resultInfoString+= " Size=" + theFile.length(); // file size.
              Path thePath= theFile.toPath();  // Convert to Path for following.
              if ( Files.isDirectory( thePath, LinkOption.NOFOLLOW_LINKS ) )
                resultInfoString+= " Directory";
              if ( Files.isRegularFile( thePath, LinkOption.NOFOLLOW_LINKS ) )
                resultInfoString+= " RegularFile";
              if ( Files.isSymbolicLink( thePath ) )
                resultInfoString+= " SymbolicLink";
              if ( Files.isReadable( thePath ) )
                resultInfoString+= " Readable";
              if ( Files.isWritable( thePath ) )
                resultInfoString+= " Writable";
              /* These always return true, so don't use.  
              if ( Files.isExecutable( ThePath ) )
                resultInfoString+= " isExecutable";
              if ( theFile.canExecute() )
                resultInfoString+= " canExecute";
               */
              if ( Files.isHidden( thePath ) )
                resultInfoString+= " Hidden";
              }
            } // Build information string about file.
          catch ( Throwable AThrowable ) {  // Handle any exception by...
            resultInfoString+= " "+AThrowable;  // ...appending its description to string.
            }
          return resultInfoString;  // return the accumulated information string.
          } // GetInfoString()

      //// public String getNameString() //////// should not be needed.
      ////   { return getNameString(theFile); }

      public String getNameString(File theFile) //////// should not be needed.
        /* Returns a String representing the name of this Object from theFile.  
          The name is the last element of the File path.
          If the path represents a file or directory
          then it is the last name in the path.
          If it represents a filesystem root,
          then it is the path prefix, which is also the entire path String.
          This is sort of a kludge, but that's the way the class File works.

          ///opt? get from the String stored in the NamedDataNode.
          */
        {
          String resultString= // Try getting the last file-name element.
            theFile.getName();

          if // There is no file-name part, get the whole path, which is parent.
            ( resultString.equals( "" ) )
            //// resultString= "INamedList.getNameString(): theFile==null.";
            resultString= theFile.getPath();

          return resultString;  // Return the final result.
          }

      public String toString() 
        /* This is needed because it appears that JList uses toString() but
          JTree uses something else (getName()?).
          */
        { 
          return getNameString(); 
          }

    }
