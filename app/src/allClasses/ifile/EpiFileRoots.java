package allClasses.ifile;

import java.io.File;

import allClasses.DataNode;
import allClasses.NamedList;

public class EpiFileRoots

  extends NamedList

  /* This class is part of an experiment with virtual file systems. 
   * 
   * This class is a TUDNet hierarchy node which represents 
   * the list of virtual file system roots.
   * 
   * This class is similar in function to class IRoots which represents
   * a list of the device's OS file system roots.
   * 
   * This class is similar in structure and operation to EpiDirectory,
   * which is used for other, non-root virtual directories.
   * 
   * This class does lazy evaluation of its list elements,
   * each of which is an EpiDirectory.
   *   
   * ///org 
   * This class does not use NameList's list functionality
   * because of the list element caching that must be done.
   * NameList is the base class because 
   * the parameter for DataNode.setParentToV(.),
   * which is called by this class' children, must be a NamedList.
   * 
   */

  { // class EpiFileRoots

    // Variables.

     /* Child cache arrays.  
       They are the same length.
       Each slot represents one filesystem root.
       */
      File[] childFiles;  // Initially empty cache array of root Files.
      EpiDirectory childEpiDirectories[];  // Initially empty cache array of 
        // root EpiDirectories.
    
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
