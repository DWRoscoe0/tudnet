package allClasses;

import java.io.File;
import java.io.IOException;

//import javax.swing.JPanel;
import javax.swing.JComponent;
//import javax.swing.JTable;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

public class IFile 

  extends AbDagNode

  { // class IFile
  
    // Variables.
    
      //private static final long serialVersionUID = 1L;
      
      File TheFile;  // File associated with this DagNode.

      String[] ChildStrings= null;  // Initially empty array of child names.
      IFile[] ChildIFiles= null;  // Initially empty array of child IFiles.
    
    // Constructors.

      IFile ( String InString ) 
        { 
          TheFile= new File( InString );
          //super( InString ); 
          }
    
      IFile ( IFile IFileIn, String InString ) 
        { 
          TheFile= new File( IFileIn.TheFile, InString );
          //super( IFileIn, InString ); 
          }

    // TheFile pass-through methods.
      
      public File GetFile( )
        /* This method returns the TheFile accociated with this DagNode.  */
        {
          return TheFile;
          }
      
      public boolean equals( Object InObject )
        /* Compares this to InObject.
          They are considered equal if their TheFile components are equal.
          */
        {
          return TheFile.equals( ((IFile)InObject).TheFile );
          }
        
    // A subset of delegated TreeModel methods.

      public boolean isLeaf( ) 
        {
          return TheFile.isFile();
          }

      public int getChildCount( ) 
        // this is pretty fast because it doesn't do actual counting.
        {
          int ChildCountI= 0;  // assume 0 children.
          do { // override 0 child count if there are any children.
            if ( ! TheFile.isDirectory() )  //  if not a directory
              break; //  keep 0 as number of children.
            String[] ChildrenStrings=   // calculate list of child file names.
              GetArrayOfStrings( );
            if (ChildrenStrings == null)  // if no list produced because directory inaccessible then
              break; //  keep 0 as number of children.
            ChildCountI= ChildrenStrings.length;  // override with actual number of actual children.
            } while (false); // override child count if there are any children.
          return ChildCountI;  // return the final child count.
          }
    
      public DagNode getChild( int IndexI ) 
        /* This returns the child with index IndexI.
          It gets the child from an array cache if possible.
          If not then it calculates the child and 
          saves it in the cache for later.
          In either case it returns a reference to the child.
          */
        { // getChild( int IndexI ) 
          SetupCacheArrays( );  // Setup the cache arrays for use.
          IFile ChildIFile=  // Try to get child IFile from cache.
            ChildIFiles[ IndexI ];
          if ( ChildIFile == null )  // Fix the cache if IFile slot was empty.
            { // Fill the empty cache slot.
              //System.out.println( 
              //  "IFile.getChild(.) calculating IFile slot"
              //  );
              ChildIFile=  // Calculate IFile slot value
                new IFile(   // return representation of desired child.
                  this, 
                  GetArrayOfStrings( )[IndexI] 
                  );
              ChildIFiles[ IndexI ]= ChildIFile;  // Save IFile in cache slot.
              } // Fill the empty cache slot.
          return ChildIFile;  // Return IFile as result.
          } // getChild( int IndexI ) 

      public int getIndexOfChild( Object ChildObject ) 
        /* Returns the index of ChildObject in directory ParentObject.  
          It does this by searching in the ChildStrings array,
          because file name strings uniquely identify the file.
          It doesn't need to calculate or use the ChildIFiles array,
          which would happen if AbDagNode.getIndexOfChild(.) were used.
          */
        {
          // System.out.println( "IFile.getIndexOfChild(.)" );
          
          IFile ChildIFile =  // Caste Child to this type.
             (IFile)ChildObject;
          String[] ChildrenStrings =  // Get local reference to Strings array.
            GetArrayOfStrings( );

          int ResultI = -1;  // Initialize result for not found.
          for ( int i = 0; i < ChildrenStrings.length; ++i ) 
            {
              if ( ChildIFile.TheFile.getName().equals( ChildrenStrings[i] ) ) 
              {
                ResultI = i;  // Set result to index of found child.
                break;
                }
              }

          return ResultI;
          }
          
    // other interface DagNode methods.
      
      public String GetInfoString()
        /* Returns a String representing information about this object. */
        { 
          String ResultInfoString= ""
            + " " + (TheFile.isDirectory() ? "Directory" : "IFile") // type.
            + " Name=\"" + GetNameString() + "\"" // file name.
            + " Size=" + TheFile.length() // file size.
            + " " + (TheFile.canRead() ? "Readable " : "") // readability.
            + " " + (TheFile.canWrite() ? "Writable " : "") // writability.
            ;
          return ResultInfoString;  // return the information string.
          }

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
          String StringResult=   // Try getting the last name element.
            TheFile.getName();
          if // Get the prefix if there is no name.
            ( StringResult.equals( "" ) )
            try {
              StringResult= // Get the prefix which is actually...
                TheFile.getCanonicalPath();
              } catch (IOException e) {
                StringResult= "IOException";  // Get error string.
              }  // ...the canonical path name.
          return StringResult;  // Return the final result.
          //return "IFile:test";  // ???
          }
      
      /*
      public JComponent GetDataJComponent( TreePath InTreePath )
        /* Returns a DagNodeViewer which is appropriate for viewing 
          the current tree node represented specified by InTreePath.  
          */
        /*
        { // GetDataJComponent.
          System.out.println( "IFile.GetDataJComponent(InTreePath)" );
          Object InObject= InTreePath.getLastPathComponent();
          IFile InIFile= // convert to what we know it is, an abstract IFile name...
            (IFile)InObject;  // ...from the input Object.
          JComponent ResultJComponent= null;  // allocate result space.

          { // calculate the associated DagNodeViewer.
            if ( InIFile.TheFile.isDirectory() )  // file is a directory.
              ResultJComponent= JComponentForDirectoryJTable(InTreePath);
            else if ( InIFile.TheFile.isFile() )  // file is a regular file.
              // ResultJComponent= JComponentForJTextAreaFrom(InIFile);
              ResultJComponent= JComponentForJTextAreaFrom(InTreePath,null);
            else  // file is neither.
              ResultJComponent=  // calculate a blank JPanel DagNodeViewer.
                new TextViewer( InTreePath, "NOTHING VIEWABLE" );
            } // calculate the associated DagNodeViewer.
          return ResultJComponent;  // return the final result.
          } // GetDataJComponent.
        */

      public JComponent GetDataJComponent
        ( TreePath InTreePath, TreeModel InTreeModel )
        /* Returns a JComponent capable of displaying this DagNode.  
          using a TreeModel argument.  
          This ignores the TreeModel for DagNode subclasses
          which do not yet support it.
          */
        // { return GetDataJComponent( InTreePath ); }
        { // GetDataJComponent.
          Object InObject= InTreePath.getLastPathComponent();
          IFile InIFile= // convert to what we know it is, an abstract IFile name...
            (IFile)InObject;  // ...from the input Object.
          JComponent ResultJComponent= null;  // allocate result space.

          { // calculate the associated DagNodeViewer.
            if ( InIFile.TheFile.isDirectory() )  // file is a directory.
              ResultJComponent= 
                new DirectoryTableViewer( InTreePath, InTreeModel );
                //JComponentForDirectoryJTable( InTreePath, InTreeModel );
            else if ( InIFile.TheFile.isFile() )  // file is a regular file.
              ResultJComponent= JComponentForJTextAreaFrom(
                InTreePath, InTreeModel );
            else  // file is neither.
              ResultJComponent=  // calculate a blank JPanel DagNodeViewer.
                new TextViewer( InTreePath, InTreeModel, "NOTHING VIEWABLE" );
            } // calculate the associated DagNodeViewer.
          return ResultJComponent;  // return the final result.
          } // GetDataJComponent.

      /* private JComponent JComponentForDirectoryJTable(TreePath InTreePath)
        /* This grouping method returns a DagNodeViewer of 
          a Directory JTable for displaying the last Object in InTreePath.
          */
        /*
        { // JComponentForDirectoryJTable()
          return 
            new DirectoryTableViewer( 
              InTreePath
              //DirectoryIJTable
              , null
              );
          } // JComponentForDirectoryJTable()
        */

      // private DagNodeViewer JComponentForJTextAreaFrom(IFile InIFile)
      private JComponent JComponentForJTextAreaFrom
        ( TreePath InTreePath, TreeModel InTreeModel )
        /* This grouping returns a DagNodeViewer of a JTextArea 
          for displaying the IFile named by InTreePath.
          */
        { // JComponentForJTextAreaFrom(InIFile)
          IFile InIFile= (IFile)InTreePath.getLastPathComponent();
          //return new DagNodeViewer(
          //  // this, 
          //  // Misc.IFileToTreePath(InIFile), 
          //  InTreePath,
          //  new IJTextArea(InIFile)
          //  );
          return new TextViewer( InTreePath, InTreeModel, InIFile );
          }  // JComponentForJTextAreaFrom(InIFile)
          
    // other methods.
      
      public DagNode[] GetDagNodes( )
        {
          return null;  // ?? nothing for now.
          }

      public IFile[] SetupCacheArrays( )
        /* Sets up the array of Strings and IFiles 
          asscoaited with this object.
          It loads the String array if it has not already been loaded,
          and it allocates a blank array of IFiles which is
          the same size as the String array if it hasn't yet.
          */
        {
          GetArrayOfStrings( );  // Load array of Strings if needed.
          if ( ChildIFiles == null )  // Create array of IFiles if needed.
            ChildIFiles=  // Create array of IFiles with same size as...
              new IFile[GetArrayOfStrings( ).length];  // ... ChildStrings.
          return ChildIFiles;  // Return the array.
          }

      public String[] GetArrayOfStrings( )
        /* Returns an array of Strings of names of files in 
          directory associated with this object.
          It loads this array if it has not already been loaded.
          */
        {
          if ( ChildStrings == null )  // Read names of children if needed.
            ChildStrings=  // Read names of children from directory.
              TheFile.list();
          return ChildStrings;  // Return the array.
          }

      public String toString( ) { return GetNameString( ); }
        /* it appears that JList uses toString() but
          JTree uses something else (getName()?).
          */

      /* old method used during development.
      public static TreePath TreePathStart()  // ???
        /* Returns the TreePath representing the path to the tree node
          that should be selected when beginning browsing IFiles.
          The user DagNode at the beginning of the path
          can be used as the root of the tree when building a
          RootTreeModel.
          */
        /*
        { // TreePathStart()
          String StartPathString= System.getProperty( "user.dir" );
          IFile StartIFile= // initialize start IFile.
            new IFile( StartPathString ); 
          TreePath StartTreePath= Misc.IFileToTreePath(StartIFile);  // ??
          return StartTreePath;
          } // TreePathStart()
        */


    } // class IFile
