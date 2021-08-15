package allClasses.ifile;

import java.io.File;

import allClasses.DataNode;
import allClasses.NamedList;

public class EpiFileRoots

  extends NamedList

  /* This class is the root of the TUDNet user's file system.  
   * It is similar in structure and operation to EpiDirectory,
   * which is used for other, non-root folders.
   * 
   * ///org The base class is NamedList, not because
   *   it any of its functionality is needed,
   *   but because the parameter for DataNode.setParentToV(.) 
   *   must be a NamedList.
   */

  { // class EpiFileRoots

    // Variables.

     /* Child cache arrays.  
       They are the same length.
       Each slot represents one filesystem root.
       */
      File[] childFiles;  // Initially empty cache array of root Files.
      EpiDirectory childEpiDirectories[];  // Initially empty cache array of root EpiDirectories.
    
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
              childEpiDirectories[ indexI ];
            if  // Fix the cache entry if the value is undefined.
              ( childEpiDirectory == null )
              {
                childEpiDirectory=  // Calculate EpiDirectory value to be...
                  new EpiDirectory(  // ...a new EpiDirectory constructed from...
                    childFiles[indexI].  // ...the child File's...
                      getAbsolutePath()  // ...absolute path name.
                    );
                childEpiDirectories[ indexI ]=  // Save in cache...
                  childEpiDirectory;  // the resulting EpiDirectory.
                childEpiDirectory.setTreeParentToV( this ); // Set parent link.
                }

          } // toReturn
            return childEpiDirectory;  // Return EpiDirectory as result DataNode.
          }

      public void setupCacheArrays()
        /* Sets up the cache arrays of Files and EpiDirectories 
          associated with this object.
          It loads the Files array if it has not already been loaded,
          and it allocates a blank array of EpiDirectories which is
          the same size as the Files array if it hasn't yet.
          */
        {
          getArrayOfFiles();  // Load array of Files if needed.
          if ( childEpiDirectories == null )  // Create array of EpiDirectories if needed.
            childEpiDirectories=  // Create array of EpiDirectories with same length as...
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
