package allClasses.ifile;

import java.io.File;

import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Objects;

import allClasses.LazyNamedList;
//// import allClasses.NamedList;

public class INamedList 

  extends LazyNamedList
  ////  extends NamedList
  
  {
  
    /* This class is meant to be the base class for all IFile related classes.
     * It adds knowledge about Files to the class NamedList.
     *
     * There are 0 or more File instances 
     * associated with each instance of this class.
     *
     * * One File reference is stored as a field in this class,
     *   unless it's an IRoots subclass.
     *   IRoots is a list of Files, but not a File itself.
     *
     * * The superclass NamedList may have 
     *   children which may contain File references.
     *
     */
  
    // Variables.
      
    protected File theFile;  // File name path associated with this instance.
      // This is stored for accessing content and attributes, but not name.
    
    /* Constructors and initialization.  These might be simplified.  */

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
            (File)null
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


    // Identity methods.

    public boolean equals( Object otherObject )
      /* This method compares this object to otherObject.
       * It returns true if equal, false otherwise.  
        This might not be a complete implementation of equals(..),
        but it suffices for how it is used.
        It assumes objects are equal if their theFile fields are equal,
        because all other content is a function of this field.
        */
      {
        boolean resultB;
        goReturn: {
          resultB= true; // Assume equal.
          if (this == otherObject) // Exit if references equal.
            break goReturn; 
          resultB= false; // Assume not equal.
          if // Exit if classes not equal.
            (this.getClass() != otherObject.getClass()) 
            break goReturn; 
          INamedList otherINamedList= (INamedList) otherObject;
          resultB= // Set result based on equality of field theFile. 
            Objects.equals(theFile, otherINamedList.theFile );
          } // goReturn:
        return resultB;
        }

    public int hashCode() 
      // This method returns the hash code of the single field theFile.
      {
        return Objects.hashCode(theFile);
        }


    // Methods which do or are related to returning Strings about this DataNode.

    public String getMetaDataString()
      /* This method returns a human-readable String representing 
       * information about this object. 
       */
      { // GetInfoString()
        String resultInfoString= "";
        try { // Build information string about file.
          resultInfoString+= ""
            + "Name=\"" + getNameString() + "\""; // file name.
          if (null != theFile) { // Add the following if theFile not null.
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
          resultInfoString+= " "+AThrowable;  // ...appending its description.
          }
        return resultInfoString;  // return the accumulated string.
        } // GetInfoString()

    protected static String getNodeNameString(File theFile)
      /* This method returns a String which is the last element of theFile.
       * The name is the last element of the File path.
       * If the path represents a file or directory
       * then it is the last name in the path.
       * If it represents a filesystem root,
       * then it is the path prefix, which is also the entire path String.
       * This is sort of a kludge, 
       * but that's the way the Java File class works.
       * 
       * ///opt? Eliminate this and get the name from 
       * the String stored in the NamedDataNode?
       */
      {
          String resultString= null;
        toReturn: {
          resultString= // Try getting the last file-name element.
            theFile.getName();
          if // If got a file-name part, return with it.
            ( ! resultString.equals( "" ) )
            break toReturn; // This if for files and directories.

          resultString= // get the whole path, which is parent. 
              theFile.getPath(); // This is for filesystem roots.
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
