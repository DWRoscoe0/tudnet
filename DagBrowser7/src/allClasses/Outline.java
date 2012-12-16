package allClasses;

//import java.io.File;
//import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
//import java.nio.channels.FileChannel;
//import java.net.URI;
//import java.net.URISyntaxException;
//import java.net.URL;


//import javax.swing.JComponent;
//import javax.swing.tree.TreeModel;
//import javax.swing.tree.TreePath;

public class Outline
  //extends Object
  //implements DataNode
  extends AbDataNode
  /* The purpose of this class is to create a large subtree
    for Infogora demonstration purposes
    from a text file that uses indented lines as in an outline.
    */
  { // class Outline

    // Variables.
      // static variables.
        static RandomAccessFile TheRandomAccessFile= null;  // For random access to file.
    
      // instance variables.
        // Initial values.
          private long StartingOffsetL;  // Starting read position in file.
        
        // General state values.
          //private FileChannel TheFileChannel;
          int NodeIndentI;  // Indent level (# of leading blanks) for this node.
          String IDString;  // Line data for use during debugging.
          int TheChildCountI= -1;  // cached count of children.
        
        // LineBuffer variables for information about previous line read.
          long LineOffsetL;  // File offset of this line.
          int LineIndentI;  // Indent level (# of leading blanks).
          String LineString;  // Line data without leading and trailing blanks.
      
    // Constructors.

        public Outline ( long OffsetInL )
          /* Constructs an Outline node that will be found
            at Outline file at offset OffsetInL.
            Normally the root node is constructed with Outline( 0 ).
            */
          { // Outline(.)
            StartingOffsetL= OffsetInL;  // Save starting file offset.
            } // Outline(.)
    
    // A subset of delegated AbstractTreeModel interface methods.

      public boolean isLeaf( ) 
        /* Returns whether the line after the first section read
          is more indented, indicating the start of a child.
          */
        {
          StartFile( );  // Prepare reading at the start position.
          GetSectionString( );  // Read past header section.
          return   // Is a leaf if line's indent is not greater than node's.  
            ! ( LineIndentI > NodeIndentI );
          }

      public int getChildCount( ) 
        /* Returns the number of outline subsections of this section.  */
        {
          if ( Misc.ReminderB )
            System.out.println( "Outline.getChildCount() " + IDCode());
          if ( TheChildCountI <= 0 )  // calculate if not doneyet.
            { // calculate child count.
              StartFile( );  // Prepare reading at the start position.
              GetSectionString( );  // Try to read past 1st section to 1st child.
              TheChildCountI= 0;  // Initialize child count.
              while  // Count all children.
                ( LineIndentI > NodeIndentI )  // There is a child here.
                { // process this child.
                  TheChildCountI++;  // Count it.
                  SkipChild( );  // Skip past this child.
                  } // process this child.
              } // calculate child count.
          return TheChildCountI;  // Return accumulated child count.
          }

      private void SkipChild( )
        /* Skips over all lines in the child at which file is positioned.
          */
        { // SkipChild( )
          int ChildIndentI= LineIndentI;  // Save indent of child.
          boolean DoneB= false;  // Set loop control flag to loop.
          while ( !DoneB ) // Skip past all child lines.
            { // Skip child lines but stop after last one.
              if ( LineString.length( ) == 0 ) // blank line.
                ; // Do nothing to skip and keep going.
              else if ( LineIndentI > ChildIndentI ) // Line is more indented.
                ; // Do nothing to skip and keep going.
              else if ( LineString.equals( "." ) )  // Line is a single '.'.
                DoneB= true;  // It is last child line, so terminate loop.
              ReadLine( );  // Skip child line.
              } // Skip child lines but stop after last one.
          } // SkipChild( )
    
      public DataNode getChild( int IndexI ) 
        /* This returns the child with index IndexI.
          It gets the child from an array cache if possible.
          If not then it calculates the child and 
          saves it in the cache for later.
          In either case it returns it.
          */
        { // getChild( int ) 
          StartFile( );  // Prepare reading at the start position.
          GetSectionString( );  // Read past 1st section to 1st child.
          for (int i=0; i < IndexI; i++)  // Skip to the correct child.
            SkipChild( );
          return new Outline(  // Return Outline node for this record with...
            LineOffsetL  // ...its file offset.
            );
          } // getChild( int ) 

      public int getIndexOfChild( Object ChildObject ) 
        /* Returns the index of the filesystem root named by ChildObject 
          in the list of filesystem roots, or -1 if it is not found.
          It does they by comparing Object-s as File-s.
          ??? This is very inefficient because it calls getChild(int),
          which is itself slow, inside of a loop.
          Fortunately it doesn't seem to be called very often.
          But it could be made much faster by
          rewriting like getChild(.).
          */
        {
          if ( Misc.ReminderB )
            System.out.println( "Outline.getIndexOfChild(...)" );

          int ResultI = -1;  // Set default result for not found.
          Outline ChildOutline = (Outline)ChildObject;  // Caste Object to Outline.
          int ChildCountI= getChildCount( );  // cache the child count.
          for ( int i = 0; i < ChildCountI; ++i ) 
            {
              if ( ChildOutline.equals( getChild( i) ) ) 
              {
                ResultI = i;
                break;
                }
              }
          return ResultI;
          }

    // Other methods.

      public String GetNameString( )
        /* Returns String representing name of this Object.  */
        {
          // return "INFOGORA NAME-SPACE";
          //return ReadLine( );

          StartFile( );  // Prepare reading at the start position.
          return LineString;  // Return data from line read.
          }
      
      /* public JComponent GetDataJComponent( TreePath InTreePath )
        /* Returns a JComponent which is appropriate for viewing 
          the current tree node represented specified by InTreePath.  
          */
        /*
        { // GetDataJComponent()
          System.out.println( "Outline.GetDataJComponent(InTreePath)" );
          JComponent ResultJComponent= null;  // For result.

          if ( isLeaf( ) )
            {
              StartFile( );  // Prepare reading at the start position.
              String TextString=  // Read header section as text.
                GetSectionString( );
              ResultJComponent= 
                new TextViewer( InTreePath, TextString );
              }
            else
            ResultJComponent= // Calculate a ListViewer.
              new ListViewer( InTreePath );
          return ResultJComponent;  // return the final result.
          } // GetDataJComponent()
        */

      private void StartFile( )
        /* Prepares the outline file for reading by:
            Opening it if needed.
            
            Seeking the position of the data for this object.
            
            Reading and storing the first line in the file.
          */
        { // StartFile( )
          if ( TheRandomAccessFile == null )  // Open the file if needed.
            PrepareTheRandomAccessFile();
          try { // Position input file to starting position.
            TheRandomAccessFile.seek(StartingOffsetL);  // Go to start.
            } // Position input file to starting position.
          catch (IOException e) { // Handle remaining errors.
            e.printStackTrace();
            } // Handle remaining errors.
          ReadLine( );  // Read first line.
          IDString= LineString;  // Save first line as ID for debugging.
          NodeIndentI= LineIndentI;  // Save indent level of section.
          } // StartFile( )

      private void PrepareTheRandomAccessFile()
        /* This grouping method prepares the RandomAccessFile by
          creating it using resource Outline.txt.
          The reason for this is because files inside jar files
          can be read only as streams, probably because
          they must be uncompressed as a stream.
          To access them randomly they must be extracted.
          */
        { // PrepareTheRandomAccessFile(..)
          InputStream ResourceInputStream= 
            getClass().getResourceAsStream("Outline.txt");
          { // open random-access file for creation and reading.
            try { // Try creating file.
              TheRandomAccessFile=  // For open random access Outline file.
                new RandomAccessFile( "Outline.tmp", "rw" );
              } // Try creating file.
            catch (FileNotFoundException e) { // Handle any errors.
              e.printStackTrace();
              } // Handle any errors.
            } // open random-access file for creation and reading.
          { // copy ResourceInputStream to TheRandomAccessFile.
            // Misc.DbgOut( "Outline.StartFile() ");
            int ByteI;
            try {
              while ( ( ByteI= ResourceInputStream.read() ) != -1) 
                TheRandomAccessFile.write( ByteI );
              }
            catch ( IOException e ) {
              e.printStackTrace();
              } // copy ResourceInputStream to TheRandomAccessFile.
            }
          } // PrepareTheRandomAccessFile(..)

      public String GetHeadString()
        /* Returns a String representing this node excluding any children. */
        { 
          StartFile( );  // Prepare reading at the start position.
          String TextString=  // Read header section as text.
            GetSectionString( );  // ??? change to toString().
          //return GetNameString( );
          return TextString;
          }

      public String toString()
        /* Returns a String representing this object, for JList. */
        { 
          return GetNameString( );
          }
          
      public String GetSectionString( )
        /* This returns a String containing all the text
          at the beginning of a node before its children, if any.
          */
        { // String GetSectionString( )
          String TotalString= "";  // Initialize String accumulator.
          while // Accumulate all lines in the section which...
            ( ( LineIndentI <= NodeIndentI ) && // ...are not indented more...
              ( !LineString.equals( "." ) ) // ...and isn't a single '.'.
              )
            { // Accumulate line.
              TotalString+= LineString;  // Append present line.
              TotalString+= '\n';  // Append newline character.
              ReadLine( );  // Read next line.
              } // Accumulate line.
          return TotalString;  // Return final result.
          } // String GetSectionString( )

      private void ReadLine( )
        /* Reads one next line of the outline file.
          It stores information about what it read in the
          ReadNext instance variable.
          */
        { // ReadLine.
          LineString= null; // Empty String accumulator.
          try 
            {
              LineOffsetL=  // save file offset of record.
                  TheRandomAccessFile.getFilePointer(); 
              LineString = TheRandomAccessFile.readLine();
              }
            catch (IOException e) { // Handle errors.
              e.printStackTrace();
              } // Handle errors.
          { // Measure indent level.
            LineIndentI= 0;  // Clear indent level.
            while // Increment indent level with each leading spaces
              ( ( LineIndentI < LineString.length() ) &&
                ( LineString.charAt( LineIndentI ) == ' ' )
                )
              LineIndentI++;
            } // Measure indent level.
          LineString=  // Remove leading and trailing spaces.
            LineString.trim( ); 
          } // ReadLine.

      @Override public boolean equals(Object other) 
        /* This is the standard equals() method.  */
        {
          boolean result = false;
          if (other instanceof Outline) {
              Outline that = (Outline) other;
              result = (this.StartingOffsetL == that.StartingOffsetL);
              }
          return result;
          }

      @Override public int hashCode() 
        /* This is the standard hashCoe() method.  */
        {
          return (int) (41 * StartingOffsetL);
          }
            

    } // class Outline
