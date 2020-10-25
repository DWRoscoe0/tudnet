package allClasses.ifile;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;  // File utility package.
import java.nio.file.LinkOption;
import java.nio.file.Path;

import javax.swing.JComponent;
import javax.swing.tree.TreePath;

import allClasses.DataNode;
import allClasses.DataTreeModel;
import allClasses.NamedLeaf;
//// import allClasses.NamedList;
import allClasses.TitledTextViewer;

import static allClasses.SystemSettings.NL;

public class IFile 

  //// extends NamedList
  extends INamedList

  /* This class extends DataNode to represent files and directories.

    It does not distinguish duplicate links to 
    files and directories from full copies of files and directories.
    Java JTrees can't have duplicate siblings.
    */
  
  { // class IFile
  
    // Variables.
    
    // Constructors.

      public IFile ( String pathString ) 
        /* Constructs an IFile from pathString.
         * pathString could represent more than one element,
         * but presently this constructor is used only by FileRoots 
         * to create single-element paths for 
         * filesystem partition names (roots).
         */
        { 
          theFile= new File( pathString );
          initializeChildrenV();
          }
    
      public IFile ( IFile ancestorPathIFile, String descendantPathString ) 
        /* Constructs an IFile by combining the paths
         * from ancestorPathIFile and descendantPathString.
         * ancestorPathIFile and descendantPathString 
         * could be arbitrary paths,
         * but in this app ancestorPathIFile usually represents a directory,
         * and descentantPathString is the name of a file or directory
         * within the first directory. 
         */
        { 
          theFile= 
              new File( ancestorPathIFile.theFile, descendantPathString );
          initializeChildrenV();
          }

    // Object overrides.

      public boolean equals( Object pathObject )
        /* Compares this to pathIFile.  
          This is not a complete implementation of equals(..).
          */
        {
          boolean resultB= false;
          if (pathObject instanceof IFile) {
              IFile otherIFile= (IFile) pathObject;
              resultB= theFile.equals( otherIFile.theFile );
              }
          return resultB;
          }

      public int hashCode() 
        // Returns the hash code of the single File field.
        {
          return theFile.hashCode();
          }

    // A subset of delegated DataTreeModel methods.

      public boolean isLeaf()
        /* Returns true if this is a file, false if a directory.  */
        {
          return theFile.isFile();
          }
    
      public DataNode getChild( int indexI ) 
        /* This returns the child with index IndexI.
          It gets the child from the child cache if it is there.
          If not then it calculates the child and 
          saves it in the cache for later.
          In either case it returns a reference to the child.
          */
        { // getChild( int IndexI ) 
          IFile childIFile= null;
          DataNode childDataNode= null;

          goReturn: {
            if  // Exit if index out of bounds.
              ( indexI < 0 || indexI >= childMultiLinkOfDataNodes.getCountI())
              break goReturn; // exit with null.
            childDataNode= childMultiLinkOfDataNodes.getE(indexI);
            //// if (childDataNode instanceof IFile) { // If got actual IFile 
            if  // If did got place-holder
              (! (childDataNode instanceof NamedLeaf)) {
              //// childIFile= (IFile)childDataNode; // set it as result.
              break goReturn; // exit with that as result.
              }
            String childString= // Get name of child. 
                //// childMultiLinkOfDataNodes.getE(indexI).getNameString();
                childDataNode.getNameString();
            File childFile= new File(theFile,childString);
            if (childFile.isDirectory())
              childIFile=  // Calculate new child IFile from child name.
                new IFile(this, childString);
              else
              childIFile=  // Calculate new child IFile from child name.
                new IFile(this, childString);
            childMultiLinkOfDataNodes.setE( // Save in cache.
                indexI, (DataNode)childIFile);
            childDataNode= childIFile;
            } // goReturn:

          //// return childIFile;  // Return IFile as result.
          return childDataNode;
          }

      public int getIndexOfChild( Object childObject ) 
        /* Returns the index of childObject in directory ParentObject.  
          It does this by searching for a child with a matching name.
          This works regardless of how many the children
          have been converted to IFiles.
          This avoids calculating a lot of IFiles unnecessarily.
          */
        {
      		DataNode childDataNode= (DataNode)childObject;
      		String childString= childDataNode.toString(); // Get name of target.

          int resultI = -1;  // Initialize result for not found.
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

      public String getFileString() // Was previously named getContentString().
        /* This method produces a String containing the contents of a file.
         * It is used by TitledTextViewer to display a file.
          */
        {
          StringBuilder theStringBuilder= new StringBuilder();
          { // Read in file to JTextArea.
            String lineString;  // temporary storage for file lines.
            try {
              FileInputStream theFileInputStream= 
                new FileInputStream(getFile());
              BufferedReader theBufferedReader= 
                new BufferedReader(new InputStreamReader(theFileInputStream));
              
              while ((lineString = theBufferedReader.readLine()) != null) {
                  theStringBuilder.append(lineString + NL);
                  }
              }
            catch (Exception ReaderException){
              // System.out.println("error reading file! " + ReaderException);
              theStringBuilder.append(
            			NL + "Error reading file!" + NL + NL + ReaderException + NL
            			);
              }
            } // Read in file to JTextArea.
          return theStringBuilder.toString();
          }
      
      public JComponent getDataJComponent(
          TreePath inTreePath,
          DataTreeModel inDataTreeModel 
          )
        /* Returns a JComponent capable of displaying the 
         * IFile at the end of inTreePath. 
         */
        {
          IFile pathIFile= (IFile)inTreePath.getLastPathComponent();  // Get the IFile.
          JComponent resultJComponent= null;  // allocate result space.
          { // calculate the appropriate IFile viewer.
            if ( pathIFile.theFile.isDirectory() )  // file is a directory.
              resultJComponent=  // Return a directory table viewer to view it.
                new DirectoryTableViewer(inTreePath, inDataTreeModel);
            else if ( pathIFile.theFile.isFile() )  // file is a regular file.
              resultJComponent=  // Return a text viewer to view the file.
	              new TitledTextViewer(inTreePath, inDataTreeModel, getFileString());
            else  // file is not a valid file or directory.
              { // Inform user of error condition.
                resultJComponent= new TitledTextViewer( // Return a view of error message. 
                    inTreePath, 
                    inDataTreeModel, 
                    NL + NL + "    UNREADABLE AS FILE OR FOLDER" + NL 
                    );
                resultJComponent.setBackground(Color.PINK); // Show it in color.
                } // Handle unreadable folder or device.
            }
          return resultJComponent;  // return the final result.
          }
          
    // other methods.

      private void initializeChildrenV()
        /* Sets up the child cache array. 
         * This is meaningful only if this IFile represents a directory.
         */
        {
          String childStrings[]= null;

          childStrings=  // Define by reading names of children from directory.
            theFile.list();
          if ( childStrings == null )  // Make certain the array is not null.
            childStrings=  // Make it be a zero-length array.
              new String[ 0 ];

          for (int indexI= 0; indexI<childStrings.length; indexI++) {
            childMultiLinkOfDataNodes.addV( // Store in the NamedList's array
                indexI, // at this location
                NamedLeaf.makeNamedLeaf( // this name place-holder.
                    childStrings[indexI])
                );
            }
          }

      public String toString() 
        /* This is needed because it appears that JList uses toString() but
          JTree uses something else (getName()?).
          */
        { 
          return getNameString(); 
          }
      
      public File getFile()
        /* This method returns the theFile associated with this DataNode.  */
        {
          return theFile;
          }

    } // class IFile
