package allClasses;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;  // File utility package.
import java.nio.file.LinkOption;
import java.nio.file.Path;

import javax.swing.JComponent;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

public class IFile 

  extends AbDataNode

  { // class IFile
  
    // Variables.
      
      File theFile;  // File associated with this DataNode.

      String[] childStrings= null;  // Initially empty array of child names.
      IFile[] childIFiles= null;  // Initially empty array of child IFiles.
    
    // Constructors.

      IFile ( String inString ) 
        // Constructs an IFile with a single element whose name is inString.
        { 
          theFile= new File( inString );
          }
    
      IFile ( IFile inIFile, String inString ) 
        /* Constructs an IFile whose parent is inIFile and 
          whose last element has name inString.
          */
        { 
          theFile= new File( inIFile.theFile, inString );
          }

    // theFile pass-through methods.
      
      public File GetFile( )
        /* This method returns the theFile associated with this DataNode.  */
        {
          return theFile;
          }
      
      public boolean equals( Object inIFile )
        /* Compares this to inIFile.  
          This is not a complete implementation of equals(..).
          */
        {
          boolean resultB = false;
          if (inIFile instanceof IFile) {
              IFile OtherIFile = (IFile) inIFile;
              resultB = theFile.equals( OtherIFile.theFile );
              }
          return resultB;
          }

      public int hashCode() 
        // Returns the hash code of the single File field.
        {
          return theFile.hashCode();
          }

    // A subset of delegated TreeModel methods.

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
          IFile childIFile= null;
          do {  // Exittable block.
            if  // Exit if index out of bounds.
              ( IndexI < 0 || IndexI >= childIFiles.length ) 
              break;
            childIFile=  // Try to get child IFile from cache.
              childIFiles[ IndexI ];
            if ( childIFile == null )  // Fix the cache if IFile slot was empty.
              { // Fill the empty cache slot.
                childIFile=  // Calculate IFile slot value
                  new IFile(   // return representation of desired child.
                    this, 
                    GetArrayOfStrings( )[IndexI] 
                    );
                childIFiles[ IndexI ]= childIFile;  // Save IFile in cache slot.
                } // Fill the empty cache slot.
            } while ( false );  // Exittable block.
          return childIFile;  // Return IFile as result.
          } // getChild( int IndexI ) 

      public int getIndexOfChild( Object childObject ) 
        /* Returns the index of childObject in directory ParentObject.  
          It does this by searching in the childStrings array,
          because file name strings uniquely identify the file.
          It doesn't need to calculate or use the childIFiles array,
          which would happen if AbDataNode.getIndexOfChild(.) were used.
          */
        {
          IFile childIFile =  // Caste Child to this type.
             (IFile)childObject;
          String[] childrenStrings =  // Get local reference to Strings array.
            GetArrayOfStrings( );

          int resultI = -1;  // Initialize result for not found.
          for ( int i = 0; i < childrenStrings.length; ++i ) 
            {
              if ( childIFile.theFile.getName().equals( childrenStrings[i] ) ) 
              {
                resultI = i;  // Set result to index of found child.
                break;
                }
              }

          return resultI;
          }
          
    // other interface DataNode methods.

      public String GetInfoString()
        /* Returns a String representing information about this object. */
        { // GetInfoString()
          String resultInfoString= "";
          try { // Build information string about file.
            resultInfoString+= ""
              + "Name=\"" + GetNameString() + "\""; // file name.
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

      public String GetNameString( )
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
      
      public JComponent GetDataJComponent
        ( TreePath inTreePath, TreeModel inTreeModel )
        /* Returns a JComponent capable of displaying this IFile.  
          using a TreeModel argument.  
          This ignores the TreeModel for DataNode subclasses
          which do not yet support it.
          */
        { // GetDataJComponent.
          Object inObject= inTreePath.getLastPathComponent();
          IFile inIFile= // convert to what we know it is, an abstract IFile name...
            (IFile)inObject;  // ...from the input Object.
          JComponent resultJComponent= null;  // allocate result space.

          { // calculate the associated DagNodeViewer.
            if ( inIFile.theFile.isDirectory() )  // file is a directory.
              resultJComponent= 
                new DirectoryTableViewer( inTreePath, inTreeModel );
            else if ( inIFile.theFile.isFile() )  // file is a regular file.
              resultJComponent= JComponentForJTextAreaFrom(
                inTreePath, inTreeModel 
                );
            else  // file is neither.
              { // Handle unreadable folder or device.
                resultJComponent=  // calculate a blank JPanel DagNodeViewer.
                  new TextViewer( 
                    inTreePath, 
                    inTreeModel, 
                    "\n\n    UNREADABLE AS FILE OR FOLDER\n" 
                    );
                resultJComponent.setBackground(Color.PINK);  // Indicate error with color.
                } // Handle unreadable folder or device.
            } // calculate the associated DagNodeViewer.
          return resultJComponent;  // return the final result.
          } // GetDataJComponent.

      private JComponent JComponentForJTextAreaFrom
        ( TreePath inTreePath, TreeModel inTreeModel )
        /* This grouping returns a DagNodeViewer of a JTextArea 
          for displaying the IFile named by inTreePath.
          */
        { // JComponentForJTextAreaFrom(inIFile)
          IFile inIFile= (IFile)inTreePath.getLastPathComponent();
          return new TextViewer( inTreePath, inTreeModel, inIFile );
          }  // JComponentForJTextAreaFrom(inIFile)
          
    // other methods.

      private IFile[] setupCacheArrays( )
        /* Sets up the array of Strings and IFile-s 
          associated with this object.
          It loads the String array if it has not already been loaded,
          and it allocates a blank array of IFile-s which is
          the same size as the String array if it hasn't yet.
          */
        {
          GetArrayOfStrings( );  // Load array of Strings if needed.
          if ( childIFiles == null )  // Create array of IFiles if needed.
            childIFiles=  // Create array of IFiles with same size as...
              new IFile[GetArrayOfStrings( ).length];  // ... childStrings.
          return childIFiles;  // Return the array.
          }

      private String[] GetArrayOfStrings( )
        /* Returns an array of Strings of names of files in 
          directory associated with this object.
          It loads this array if it has not already been loaded.
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

      public String toString( ) { return GetNameString( ); }
        /* it appears that JList uses toString() but
          JTree uses something else (getName()?).
          */

    } // class IFile
