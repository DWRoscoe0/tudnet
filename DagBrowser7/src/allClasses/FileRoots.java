package allClasses;

import java.io.File;

import javax.swing.JComponent;
import javax.swing.tree.TreePath;

public class FileRoots

  extends AbDataNode

  /* This class is the root of the Infogora user's file system.  */

  { // class FileRoots

    // Variables.

     /* Child cache arrays.  
       They are the same length.
       Each slot represents one filesystem root.
       */
      
      File[] ChildFiles;  // Initially empty cache array of root Files.
      IFile ChildIFiles[];  // Initially empty cache array of root IFiles.
    
    // Constructors (none yet).
    
    // A subset of delegated AbstractDataTreeModel methods.

      /*
      public boolean isLeaf( ) 
        /* Returns false, because every computer has filesystem roots,
          even if there are 0 of them.  */
      /*
        {
          return false;  
          }
      */

      public int getChildCount( )
        /* Returns the number of filesystem roots.  */
        {
          return // Return...
            GetArrayOfFiles( )  // ...the child File array...
              .length;  // ...length.
          }
    
      public DataNode getChild( int IndexI ) 
        /* This returns the child with index IndexI.
          It gets the child from an array cache if possible.
          If not then it calculates the child and 
          saves it in the cache for later.
          In either case it returns it.
          */
        {
          IFile ChildIFile= null; // Default result of null.
          SetupCacheArrays( );  // Setup the cache arrays for use.

          toReturn: { // block beginning.

            if  // Handling IndexI out of range.
              ( ( IndexI < 0 ) ||  // Too low.
                ( IndexI >= getChildCount( ) )  // Too High.
                )
              break toReturn;  // Extting with null.

            ChildIFile=  // Try to get child IFile from cache.
              ChildIFiles[ IndexI ];
            if  // Fix the cache if IFile slot is empty.
              ( ChildIFile == null )  // null means empty.
              { // Fill the empty cache slot.
                ChildIFile=  // Calculate IFile slot value to be...
                  new IFile(  // ...a new IFile constructed from...
                    ChildFiles[IndexI].  // ...the child File's...
                      getAbsolutePath()  // ...absolute path name.
                    );
                ChildIFiles[ IndexI ]=  // Save in cache slot...
                  ChildIFile;  // the resulting IFile.
                } // Fill the empty cache slot.

          } // toReturn
            return ChildIFile;  // Return IFile as result DataNode.
          }

      /*
      public int getIndexOfChild( Object ChildObject ) 
        /* Returns the index of the filesystem root named by ChildObject 
          in the list of filesystem roots, or -1 if it is not found.
          It does they by comparing Object-s as File-s.
          */
      /*
        {
          //System.out.println( "FileRoots.getIndexOfChild(...) is untested" );

          int ResultI = -1;  // Set default result for not found.
          File ChildFile = ((IFile)ChildObject).GetFile();  // Caste Object to File.
          File[] ChildFiles= File.listRoots();  // Get array of roots.
          for ( int i = 0; i < ChildFiles.length; ++i ) 
            {
              if ( ChildFile.equals( ChildFiles[i] ) ) 
              {
                ResultI = i;
                break;
                }
              }
          return ResultI;
          }
      */

      public void SetupCacheArrays( )
        /* Sets up the cache arrays of Files and IFiles 
          associated with this object.
          It loads the Files array if it has not already been loaded,
          and it allocates a blank array of IFiles which is
          the same size as the Files array if it hasn't yet.
          */
        {
          GetArrayOfFiles( );  // Load array of Files if needed.
          if ( ChildIFiles == null )  // Create array of IFiles if needed.
            ChildIFiles=  // Create array of IFiles with same length as...
              new IFile[ ChildFiles.length ];  // ... array of Files.
          }

      public File[] GetArrayOfFiles( )
        /* Returns an array of Files representing the filesystem roots.
          It loads this array if it has not already been loaded.
          */
        {
          if ( ChildFiles == null )  // Read the filesystem roots if needed.
            ChildFiles= File.listRoots();  // Read the filesystem roots.
          return ChildFiles;  // Return the array.
          }

      public String toString()
        /* Override of the standard converter because List uses it. */
        { 
          return getNameString( );
          }

      public String getInfoString()
        /* Returns a String representing information about this object. */
        { 
          return getNameString( );
          }

      public String getNameString( )
        /* Returns String representing name of this Object.  */
        {
          return "File-System-Roots";
          }
      
      public JComponent GetDataJComponent( 
          TreePath InTreePath, 
          MetaRoot theMetaRoot,
          DataTreeModel InDataTreeModel
          )
        /* Returns a JComponent which is appropriate for viewing 
          the current tree node represented specified by InTreePath
          using context from InDataTreeModel.
          */
        { // GetDataJComponent.
          JComponent ResultJComponent=  // Calculate a ListViewer.
            new TitledListViewer( InTreePath, InDataTreeModel );
          return ResultJComponent;  // return the final result.
          } // GetDataJComponent.

        public static TreePath TreePathStart()
          /* Returns the TreePath representing the path to the tree node
            that should be selected when beginning browsing FileRoots.
            The user DataNode at the beginning of the path
            can be used as the root of the tree when building a
            DataDataTreeModel.
            */
          { // TreePathStart()
            TreePath StartTreePath=  // Create the StartTreePath from...
              new TreePath(  // ...a single...
                new FileRoots()  // ...FilesRoots object.
                );
            return StartTreePath;  // Return it.
            } // TreePathStart()

    } // class FileRoots
