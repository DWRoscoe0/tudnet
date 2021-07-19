package allClasses.ifile;

import java.io.File;
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
        // This is stored for accessing content and attributes, but not name.
    
    // Constructors and initialization.

      public INamedList(File theFile)
        /* This constructs an instance.
         * * theFile provides both
         *   * the node name from the last element of the path, and
         *   * how to find other content such as all other node content.
         */
        {
          this(
              getNodeNameString(theFile),
              theFile
              );
          }

      public INamedList(String nameString)
        /* This constructs an instance.
         * * nameString provides the node name.
         * * There is no File for this node, 
         *   though there will be for the children.
         * Child information is provided later.
         */
        {
          this(
              nameString,
              null
              );
          }

      private INamedList(String nameString, File theFile)
        /* This constructs an instance.
         * * nameString provides the node name.
         * * theFile provides all other node metadata.
         * Child information is provided later.
         */
        {
          super.setNameV(nameString);
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

      protected static String getNodeNameString(File theFile)
        /* Returns a String representing the name of this Object from theFile.  
          The name is the last element of the File path.
          If the path represents a file or directory
          then it is the last name in the path.
          If it represents a filesystem root,
          then it is the path prefix, which is also the entire path String.
          This is sort of a kludge, but that's the way the File class works.

          ///opt? get from the String stored in the NamedDataNode.
          */
        {
            String resultString= null;
          toReturn: {
            resultString= // Try getting the last file-name element.
              theFile.getName();
  
            if // If got a file-name part
              ( ! resultString.equals( "" ) )
              break toReturn;
            resultString= // get the whole path, which is parent. 
                theFile.getPath();
          } // toReturn:
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
