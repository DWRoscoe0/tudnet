package allClasses;

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
import static allClasses.SystemSettings.NL;

public class IFile 

  //// extends DataNode
  extends NamedList

  /* This class extends DataNode to represent files and directories.
    It does not distinguish duplicate links to 
    files and directories from full copies of files and directories.
    Java JTrees can't have duplicate siblings.
    */
  
  { // class IFile
  
    // Variables.
      
      File theFile;  // File associated with this DataNode.

      ////// The following 2 fields are being replaced with 
      ////// NameList.childMultiLinkOfDataNodes.
      //// String[] childStrings= null;  // Initially empty array of child names.
      //// IFile[] childIFiles= null;  // Initially empty array of child IFiles.
        // The above 2 variables will be wasted if this is not a directory.
    
    // Constructors.

      IFile ( String pathString ) 
        /* Constructs an IFile from pathString.
         * pathString could represent more than one element,
         * but presently this constructor is used only by FileRoots 
         * to create single-element paths for 
         * filesystem partition names (roots).
         */
        { 
          theFile= new File( pathString );
          setupCacheArrays();
          }
    
      IFile ( IFile ancestorPathIFile, String descentantPathString ) 
        /* Constructs an IFile by combining the paths
         * from ancestorPathIFile and descentantPathString.
         * ancestorPathIFile and descentantPathString 
         * could be arbitrary paths,
         * but in this app ancestorPathIFile usually represents a directory,
         * and descentantPathString is the name of a file or directory
         * within the first directory. 
         */
        { 
          theFile= 
              new File( ancestorPathIFile.theFile, descentantPathString );
          setupCacheArrays();
          }

    // theFile pass-through methods.
      
      public File getFile()
        /* This method returns the theFile associated with this DataNode.  */
        {
          return theFile;
          }
      
      public boolean equals( Object pathIFile )
        /* Compares this to pathIFile.  
          This is not a complete implementation of equals(..).
          */
        {
          boolean resultB = false;
          if (pathIFile instanceof IFile) {
              IFile OtherIFile = (IFile) pathIFile;
              resultB = theFile.equals( OtherIFile.theFile );
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

      public int getChildCount()
        {
          return childMultiLinkOfDataNodes.getCountI();
          }
      
        /* This is pretty fast because it doesn't do actual counting.
          It returns the length of the file name string array.
          */

        /*  ////
        {
          int childCountI= 0;  // Assume count of 0 children.

          goReturn: {  // For block-exit breaks below.
            if ( ! theFile.isDirectory() )  //  This is not a directory
              break goReturn; //  Keep 0 as number of children.
            String[] childrenStrings=   // Calculate list of child file names.
              getArrayOfFileNameStrings();
            if   // No list produced because directory inaccessible then
              (childrenStrings == null)
              break goReturn; //  Keep 0 as number of children.
            childCountI=   // Override 0 count with actual child file count.
              childrenStrings.length;

          } // goReturn
            return childCountI;  // return the final child count.

          }
          */  ////
    
      public DataNode getChild( int indexI ) 
        /* This returns the child with index IndexI.
          It gets the child from an array cache if it is there.
          If not then it calculates the child and 
          saves it in the cache for later.
          In either case it returns a reference to the child.
          */
        { // getChild( int IndexI ) 
          //// setupCacheArrays();  // Setup the cache arrays for use.
          IFile childIFile= null;

          goReturn: {
            if  // Exit if index out of bounds.
              //// ( indexI < 0 || indexI >= childIFiles.length ) 
              ( indexI < 0 || indexI >= childMultiLinkOfDataNodes.getCountI())
              break goReturn; // exit with null.
            DataNode childDataNode= childMultiLinkOfDataNodes.getE(indexI);
            //// if ( null != childIFile ) // If got IFile from cache
            if (childDataNode instanceof IFile) { // If got actual IFile 
              childIFile= (IFile)childDataNode; // set it as result.
              break goReturn; // exit with it.
              }
            String childString= // Get name of child. 
                //// getArrayOfFileNameStrings()[indexI];
                childMultiLinkOfDataNodes.getE(indexI).getNameString();
            childIFile=  // Calculate IFile from child name.
              new IFile(
                this, 
                //// getArrayOfFileNameStrings()[indexI]
                childString
                );
            //// childIFiles[ indexI ]= childIFile;  // Save in IFile cache.
            childMultiLinkOfDataNodes.setE( // Save in NamedList cache also.
                indexI, // at this location
                //// (DataNode)(NamedLeaf.makeNamedLeaf( // this name place-holder.
                //// ////     childStrings[indexI]))
                ////     childString
                (DataNode)childIFile
                ////    )));
                );
            } // goReturn:

          return childIFile;  // Return IFile as result.
          }

      public int getIndexOfChild( Object childObject ) 
        /* Returns the index of childObject in directory ParentObject.  
          It does this by searching in the childStrings array,
          because file name strings uniquely identify the file.
          It doesn't need to calculate or use the childIFiles array,
          which would happen if AbDataNode.getIndexOfChild(.) were used.
          */
        {
      		DataNode childDataNode= (DataNode)childObject;
      		String childString= childDataNode.toString();

          //// String[] childrenStrings =  // Get local reference to Strings array.
          ////   getArrayOfFileNameStrings();

          int resultI = -1;  // Initialize result for not found.
          for 
            ( int i = 0; 
              //// i < childrenStrings.length;
              i < childMultiLinkOfDataNodes.getCountI();
              ++i 
              ) 
            {
          		if ( childString.equals( 
          		    //// childrenStrings[i]
          		    childMultiLinkOfDataNodes.getE(i).getNameString()
          		    ) )
	              {
	                resultI = i;  // Set result to index of found child.
	                break;
	                }
              }

          return resultI;
          }
          
    // other interface DataNode methods.

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
        /* This method produces the value which is used by
          TitledTextViewer to display the contents of a file.
          */
        {
          StringBuilder theStringBuilder= new StringBuilder();
          { // Read in file to JTextArea.
            String lineString;  // temporary storage for file lines.
            try {
              FileInputStream theFileInputStream = 
              		new FileInputStream(getFile());
              //// @SuppressWarnings("resource")
              BufferedReader theBufferedReader = 
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
        /* Returns a JComponent capable of displaying IFile at the end of inTreePath. */
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

      private IFile[] setupCacheArrays()
        /* Sets up the array of Strings and IFile-s 
          associated with this object.
          It loads the String array if it has not already been loaded.
          Each string is the name of one file.
          It also allocates an array of null IFile references
          with the same number of elements.
          */
        {
          //// getArrayOfFileNameStringsV();  // Load array of Strings if needed.
          String childStrings[]= null;
          //// if ( childStrings == null ) { // Define child string array if needed.

            childStrings=  // Define by reading names of children from directory.
              theFile.list();
            if ( childStrings == null )  // Make certain the array is not null.
              childStrings=  // Make it be a zero-length array.
                new String[ 0 ];

            //// copyChildStringsToMultiLinkV();
            for (int indexI= 0; indexI<childStrings.length; indexI++) {
              childMultiLinkOfDataNodes.addV( // Store in the NamedList's array
                  indexI, // at this location
                  (DataNode)(NamedLeaf.makeNamedLeaf( // this name place-holder.
                      childStrings[indexI]))
                  );
              }

          //// if ( childIFiles == null )  // Create array of IFiles if needed.
          ////   childIFiles=  // Create array of IFiles with same size as...
          ////     new IFile[getArrayOfFileNameStrings().length];  // ... childStrings.

          //// return childIFiles;  // Return the array.
          return null;  // Return the array.
          }

      //// private String[] getArrayOfFileNameStringsV()
      @SuppressWarnings("unused") //// 
      private void getArrayOfFileNameStringsV()
        /* Returns an array of Strings of names of files in 
          directory associated with this object.
          It loads this array if it has not already been loaded.
          ///enh The use of File.list() could be replaced with 
            a DirectoryStream<Path> iterator, and be able to handle very long
            unsorted directories.  Some caching would be needed for reverse scrolling.
          */
        {
          String childStrings[]= null;
          //// if ( childStrings == null ) { // Define child string array if needed.

            childStrings=  // Define by reading names of children from directory.
              theFile.list();
            if ( childStrings == null )  // Make certain the array is not null.
              childStrings=  // Make it be a zero-length array.
                new String[ 0 ];

            //// copyChildStringsToMultiLinkV();
            for (int indexI= 0; indexI<childStrings.length; indexI++) {
              childMultiLinkOfDataNodes.addV( // Store in the NamedList's array
                  indexI, // at this location
                  (DataNode)(NamedLeaf.makeNamedLeaf( // this name place-holder.
                      childStrings[indexI]))
                  );
              }

            //// }

          //// return childStrings;  // Return the array.
          }

      /*  ////
      @SuppressWarnings("unused")
      private void copyChildStringsToMultiLinkV()  ////
        { 
          for (int indexI= 0; indexI<childStrings.length; indexI++) {
            childMultiLinkOfDataNodes.addV( // Store in the NamedList's array
                indexI, // at this location
                (DataNode)(NamedLeaf.makeNamedLeaf( // this name place-holder.
                    childStrings[indexI]))
                );
            
            }
          }
      */  ////

      public String toString() { return getNameString(); }
        /* it appears that JList uses toString() but
          JTree uses something else (getName()?).
          */

    } // class IFile
