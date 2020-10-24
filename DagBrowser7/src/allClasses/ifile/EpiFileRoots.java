package allClasses.ifile;

import java.io.File;

import allClasses.DataNode;

public class EpiFileRoots extends DataNode

  /* This class is the root of the Infogora user's file system.  
   * It is similar in structure and operation to EpiDirectory,
   * which is used for other, non-root folders.
   */

  { // class EpiFileRoots

    // Variables.

     /* Child cache arrays.  
       They are the same length.
       Each slot represents one filesystem root.
       */
      File[] childFiles;  // Initially empty cache array of root Files.
      EpiDirectory childEpiDirectorys[];  // Initially empty cache array of root EpiDirectorys.
    
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
          EpiDirectory childEpiDirectory= null; // Default result of null.
          setupCacheArrays();  // Prepare the cache arrays for use.

          toReturn: { // block beginning.

            if  // Handling IndexI out of range.
              ( ( indexI < 0 ) ||  // Too low.
                ( indexI >= getChildCount() )  // Too High.
                )
              break toReturn;  // Exiting with null result.

            childEpiDirectory=  // Try to get child EpiDirectory from cache.
              childEpiDirectorys[ indexI ];
            if  // Fix the cache entry if the value is undefined.
              ( childEpiDirectory == null )
              {
                childEpiDirectory=  // Calculate EpiDirectory value to be...
                  new EpiDirectory(  // ...a new EpiDirectory constructed from...
                    childFiles[indexI].  // ...the child File's...
                      getAbsolutePath()  // ...absolute path name.
                    );
                childEpiDirectorys[ indexI ]=  // Save in cache...
                  childEpiDirectory;  // the resulting EpiDirectory.
                }

          } // toReturn
            return childEpiDirectory;  // Return EpiDirectory as result DataNode.
          }

      public void setupCacheArrays()
        /* Sets up the cache arrays of Files and EpiDirectorys 
          associated with this object.
          It loads the Files array if it has not already been loaded,
          and it allocates a blank array of EpiDirectorys which is
          the same size as the Files array if it hasn't yet.
          */
        {
          getArrayOfFiles();  // Load array of Files if needed.
          if ( childEpiDirectorys == null )  // Create array of EpiDirectorys if needed.
            childEpiDirectorys=  // Create array of EpiDirectorys with same length as...
              new EpiDirectory[ childFiles.length ];  // ... array of Files.
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
          return "Epi-File-System-Roots";
          }

    } // class EpiFileRoots
