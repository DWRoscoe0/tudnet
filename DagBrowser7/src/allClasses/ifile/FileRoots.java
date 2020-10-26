package allClasses.ifile;

import java.io.File;

import allClasses.DataNode;

public class FileRoots extends DataNode

  /* This class is the root of the Infogora user's file system.  
   * It is similar in structure and operation to IDirectory,
   * which is used for other, non-root folders.
   */

  { // class FileRoots

    // Variables.

     /* Child cache arrays.  
       They are the same length.
       Each slot represents one filesystem root.
       */
      File[] childFiles;  // Initially empty cache array of root Files.
      IDirectory childIDirectories[];  // Initially empty cache array of root IDirectories.
    
    // Constructors (none yet).
    
    // A subset of delegated AbstractDataTreeModel methods.

      public int getChildCount()
        /* Returns the number of filesystem roots.  */
        {
          return // Return...
            getArrayOfFiles()  // ...the child File array...
              .length;  // ...length.
          }
    
      public DataNode getChild( int indexI ) 
        /* This returns the child with index indexI.
          It gets the child from an array cache if possible.
          If not then it calculates the child and 
          saves it in the cache for later.
          In either case it returns it.
          */
        {
          IDirectory childIDirectory= null; // Default result of null.
          setupCacheArrays();  // Prepare the cache arrays for use.

          toReturn: { // block beginning.

            if  // Handling IndexI out of range.
              ( ( indexI < 0 ) ||  // Too low.
                ( indexI >= getChildCount() )  // Too High.
                )
              break toReturn;  // Exiting with null result.

            childIDirectory=  // Try to get child IDirectory from cache.
              childIDirectories[ indexI ];
            if  // Fix the cache entry if the value is undefined.
              ( childIDirectory == null )
              {
                String childString=  // Set child name string to
                  childFiles[indexI].  // the child File's
                    getAbsolutePath();  // absolute path name.
                childIDirectory=  // Calculate IDirectory value to be
                  new IDirectory(  // a new IDirectory constructed from
                    new File(childString)); // childs file name.
                childIDirectories[ indexI ]=  // Save in cache...
                  childIDirectory;  // the resulting IDirectory.
                }

          } // toReturn
            return childIDirectory;  // Return IDirectory as result DataNode.
          }

      public void setupCacheArrays()
        /* Sets up the cache arrays of Files and IDirectories 
          associated with this object.
          It loads the Files array if it has not already been loaded,
          and it allocates a blank array of IDirectories which is
          the same size as the Files array if it hasn't yet.
          */
        {
          getArrayOfFiles();  // Load array of Files if needed.
          if ( childIDirectories == null )  // Create array of IDirectories if needed.
            childIDirectories=  // Create array of IDirectories with same length as...
              new IDirectory[ childFiles.length ];  // ... array of Files.
          }

      public File[] getArrayOfFiles()
        /* Returns an array of Files representing the filesystem roots.
          It loads this array if it has not already been loaded.
          */
        {
          if ( childFiles == null )  // Read the filesystem roots if needed.
            childFiles= File.listRoots();  // Read the filesystem roots.
          return childFiles;  // Return the array.
          }

      public String toString()
        /* Override of the standard converter because List uses it. */
        { 
          return getNameString();
          }

      public String getMetaDataString()
        /* Returns a String representing information about this object. */
        { 
          return getNameString();
          }
      
      public String getCellString()  
        {
          return getNameString();
          }

      public String getNameString()
        /* Returns String representing name of this Object.  */
        {
          return "File-System-Roots";
          }

    } // class FileRoots
