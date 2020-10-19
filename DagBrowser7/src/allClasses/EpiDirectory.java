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

//// public class EpiDirectory 
public class EpiDirectory extends DataNode {

  /* This class extends DataNode to represent files and directories.
    It does not distinguish duplicate links to 
    files and directories from full copies of files and directories
    */
  
    // Variables.
      
      File theFile;  // File associated with this DataNode.

      String[] childStrings= null;  // Initially empty array of child names.
      EpiDirectory[] childEpiDirectories= null;  // Same for EpiDirectory-s..
    
    // Constructors.

      EpiDirectory( String inString ) 
        // Constructs an EpiDirectory with a single element whose name is inString.
        { 
          theFile= new File( inString );
          }
    
      EpiDirectory( EpiDirectory inEpiDirectory, String inString ) 
        /* Constructs an EpiDirectory whose parent is inEpiDirectory and 
          whose last element has name inString.
          */
        { 
          theFile= new File( inEpiDirectory.theFile, inString );
          }

    // theFile pass-through methods.
      
      public File getFile( )
        /* This method returns the theFile associated with this DataNode.  */
        {
          return theFile;
          }
      
      public boolean equals( Object inEpiDirectory )
        /* Compares this to inEpiDirectory.  
          This is not a complete implementation of equals(..).
          */
        {
          boolean resultB = false;
          if (inEpiDirectory instanceof EpiDirectory) {
              EpiDirectory OtherEpiDirectory = (EpiDirectory) inEpiDirectory;
              resultB = theFile.equals( OtherEpiDirectory.theFile );
              }
          return resultB;
          }

      public int hashCode() 
        // Returns the hash code of the single File field.
        {
          return theFile.hashCode();
          }

    // A subset of delegated DataTreeModel methods.

      public boolean isLeaf( )
        /* Returns true if this is a file, false if a directory.  */
        {
          return theFile.isFile();
          }

      public int getChildCount( ) 
        /* This is pretty fast because it doesn't do actual counting.
          It returns the length of the file name string array.
          */
        {
          int childCountI= 0;  // Assume count of 0 children.

          goReturn: {  // For block-exit breaks below.
            if ( ! theFile.isDirectory() )  //  This is not a directory
              break goReturn; //  Keep 0 as number of children.
            String[] childrenStrings=   // Calculate list of child file names.
              GetArrayOfStrings( );
            if   // No list produced because directory inaccessible then
              (childrenStrings == null)
              break goReturn; //  Keep 0 as number of children.
            childCountI=   // Override 0 count with actual child file count.
              childrenStrings.length;

          } // goReturn
            return childCountI;  // return the final child count.

          }

      public DataNode getChild( int IndexI ) 
        /* This returns the child with index IndexI.
          It gets the child from an array cache if that is possible.
          If not then it calculates the child and 
          saves it in the cache for later.
          In either case it returns a reference to the child.
          */
        { // getChild( int IndexI ) 
          setupCacheArrays( );  // Setup the cache arrays for use.
          EpiDirectory childEpiDirectory= null;

          do {  // Exit-able block.
            if  // Exit if index out of bounds.
              ( IndexI < 0 || IndexI >= childEpiDirectories.length ) 
              break;
            childEpiDirectory=  // Try to get child EpiDirectory from cache.
              childEpiDirectories[ IndexI ];
            if ( childEpiDirectory == null )  // Fix the cache if EpiDirectory slot was empty.
              { // Fill the empty cache slot.
                childEpiDirectory=  // Calculate EpiDirectory slot value
                  new EpiDirectory(   // return representation of desired child.
                    this, 
                    GetArrayOfStrings( )[IndexI] 
                    );
                childEpiDirectories[ IndexI ]= childEpiDirectory;  // Save EpiDirectory in cache slot.
                } // Fill the empty cache slot.
            } while ( false );  // Exittable block.

          return childEpiDirectory;  // Return EpiDirectory as result.
          } // getChild( int IndexI ) 

      public int getIndexOfChild( Object childObject ) 
        /* Returns the index of childObject in directory ParentObject.  
          It does this by searching in the childStrings array,
          because file name strings uniquely identify the file.
          It doesn't need to calculate or use the childEpiDirectorys array,
          which would happen if AbDataNode.getIndexOfChild(.) were used.
          */
        {
          DataNode childDataNode= (DataNode)childObject;
          String childString= childDataNode.toString();

          String[] childrenStrings =  // Get local reference to Strings array.
            GetArrayOfStrings( );

          int resultI = -1;  // Initialize result for not found.
          for ( int i = 0; i < childrenStrings.length; ++i ) 
            {
              if ( childString.equals( childrenStrings[i] ) )
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

      public String getNameString( )
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
        /* Returns a JComponent capable of displaying EpiDirectory at the end of inTreePath. */
        {
          EpiDirectory inEpiDirectory= (EpiDirectory)inTreePath.getLastPathComponent();  // Get the EpiDirectory.
          JComponent resultJComponent= null;  // allocate result space.
          { // calculate the appropriate EpiDirectory viewer.
            if ( inEpiDirectory.theFile.isDirectory() )  // file is a directory.
              resultJComponent=  // Return a directory table viewer to view it.
                new DirectoryTableViewer(inTreePath, inDataTreeModel);
            else if ( inEpiDirectory.theFile.isFile() )  // file is a regular file.
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

      private EpiDirectory[] setupCacheArrays( )
        /* Sets up the array of Strings and EpiDirectory-s 
          associated with this object.
          It loads the String array if it has not already been loaded,
          and it allocates a blank array of EpiDirectory-s which is
          the same size as the String array if it hasn't yet.
          */
        {
          GetArrayOfStrings( );  // Load array of Strings if needed.

          if ( childEpiDirectories == null )  // Create array of EpiDirectorys if needed.
            childEpiDirectories=  // Create array of EpiDirectorys with same size as...
              new EpiDirectory[GetArrayOfStrings( ).length];  // ... childStrings.

          return childEpiDirectories;  // Return the array.
          }

      private String[] GetArrayOfStrings( )
        /* Returns an array of Strings of names of files in 
          directory associated with this object.
          It loads this array if it has not already been loaded.
          ///enh The use of File.list() could be replaced with 
            a DirectoryStream<Path> iterator, and be able to handle very long
            unsorted directories.  Some caching would be needed for reverse scrolling.
          */
        {
          if ( childStrings == null )  // Read names of children if needed.
            childStrings=  // Read names of children from directory.
              theFile.list();

          if ( childStrings == null )  // Make certain the array is not null.
            childStrings=  // Make it be a zero-length array.
              new String[ 0 ];

          return childStrings;  // Return the array.
          }

      public String toString( ) { return getNameString( ); }
        /* it appears that JList uses toString() but
          JTree uses something else (getName()?).
          */

  }
