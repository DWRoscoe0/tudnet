package allClasses;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

//import java.nio.channels.FileChannel;

//import javax.swing.JComponent;
//import javax.swing.tree.TreeModel;
//import javax.swing.tree.TreePath;


import static allClasses.Globals.*;  // appLogger;

public class Outline

  extends AbDataNode

  /* The purpose of this class is to create 
    a large subtree for Infogora demonstrations
    from a text file that uses indented lines as in an outline.
    
    ??? This class is supposed to be temporary.
    It is very inefficient.
    It might benefit from caching as is done with IFile and FileRoots.
    There is a 2-second pause when it is first accessed in right pane.
    */

  { // class Outline

    // static lock-protected variables.
			static Object lockObject= new Object();
      static RandomAccessFile theRandomAccessFile= // For random file access.
      		null;
  
    // instance variables.
      // Initial values.
        private long startingOffsetL;  // Nodes starting file offset.
          // This value is sufficient to distinguish one node from another.

          // pseudo-statics variables for general state values.

        	// pseudo-statics variables for information about previous line read.

    static class Aid { // Helper class for Outline.

      long fileOffsetL;  // File offset of file when reading.
      String debugString;  // Line data for use during debugging.

      // pseudo-statics variables for read process.
      long lineOffsetL;  // File offset of line.
      int lineIndentI;  // Indent level (# of leading blanks) of line
      String lineString;  // Line data without leading and trailing blanks.
    	
      // pseudo-statics variables for cached node properties.
      int nodeIndentI;  // Indent level (# of leading blanks) for this node.
      int theChildCountI= -1;  // cached count of children.

      private static boolean readLineReentryB= false; // ?? Debug.

      private void readLine( )
        /* Reads one next line of the outline file.
          It stores information about what it read in the
          ReadNext instance variable.
          If an EndOfFile is encountered then it simulates
          the reading of a line containing only a ".".
          */
        { // readLine.
      		synchronized (lockObject) {
	      		if ( readLineReentryB ) // ?? catch 2nd thread at entrance.
	      			{ readLineReentryB= false;             
	      			  appLogger.error( "Outline: readLine() REENTRY Begin.");
	      				}
	      			else
	      			readLineReentryB= true;
	      		lineString= null; // Clearing String accumulator.
        	  lineOffsetL= fileOffsetL;  // Saving file offset of line being read.
	          try 
	            {
	          	  theRandomAccessFile.seek(fileOffsetL);  // Seeking read point.
	          		lineString = theRandomAccessFile.readLine();  // Reading line.
	          	  fileOffsetL= // Saving end point.
	          	  		theRandomAccessFile.getFilePointer(); 
	              }
	            catch (IOException e) { // Handle errors.
	              e.printStackTrace();
	              } // Handle errors.
	          if (lineString == null) // Handling End of file (shouldn't happen).
		          {
		            //appLogger.info( "Outline: readLine() replacing EOF with '.'");
	          	  lineString= ".";  // Simulating read of a terminator line.
		            }
	          { // Measuring line indent level.
	          	lineIndentI= 0;  // Clearing indent level.
	            //appLogger.info( "Outline: readLine(),lineString= "+lineString );
	            while // Incrementing indent level for each leading space
	              ( ( lineIndentI < lineString.length() ) &&
	                ( lineString.charAt( lineIndentI ) == ' ' )
	                )
	            	lineIndentI++;
	            } // Measure indent level.
	          lineString=  // Removing leading and trailing spaces from line.
	          		lineString.trim( ); 
	      		if ( ! readLineReentryB ) // ?? catch 1st thread at exit.
		    			{ readLineReentryB= true;
		  			  	appLogger.error( "Outline: readLine() REENTRY End.");
		    				}
	      			else
	      			readLineReentryB= false;
      			}
          } // readLine.

      public String getSectionString( )
        /* This returns a String containing all the text
          at the beginning of a node before its children, if any.
          */
        { // String getSectionString( )
          String totalString= "";  // Initialize String accumulator.
          while // Accumulate all lines in the section which...
            ( ( lineIndentI <= nodeIndentI ) && // ...are not indented more...
              ( !lineString.equals( "." ) ) // ...and isn't a single '.'.
              )
            { // Accumulate line.
              totalString+= lineString;  // Append present line.
              totalString+= '\n';  // Append newline character.
              readLine( );  // Read next line.
              } // Accumulate line.
          return totalString;  // Return final result.
          } // String getSectionString( )

      private void skipChild( )
        // Skips over all lines in the child at which file is positioned.
        { // skipChild( )
          int childIndentI= lineIndentI;  // Save indent of child.
          //appLogger.info( "Outline: skipChild(), lineIndentI= "+lineIndentI);
          boolean doneB= false;  // Set loop control flag to loop.
          while ( !doneB ) // Skip past all child lines.
            { // Skip child lines but stop after last one.
              if ( lineString.length( ) == 0 ) // blank line.
                ; // Do nothing to skip and keep going.
              else if ( lineIndentI > childIndentI ) // Line is more indented.
                ; // Do nothing to skip and keep going.
              else if ( lineString.equals( "." ) )  // Line is a single '.'.
                {
	                //appLogger.info( "Outline: skipChild(), Done.");
	                doneB= true;  // It is last child line, so terminate loop.
	                }
              readLine( );  // Skip child line.
              } // Skip child lines but stop after last one.
          } // skipChild( )
    
      } // class Aid
      
    // Constructors.

        public Outline ( long offsetInL )
          /* Constructs an Outline node that will be found
            at Outline file at offset offsetInL.
            Normally the root node is constructed with Outline( 0 ).
            */
          { // Outline(.)
            startingOffsetL= offsetInL;  // Save starting file offset.
            } // Outline(.)
    
    // A subset of delegated AbstractTreeModel interface methods.

      public boolean isLeaf( ) 
        /* Returns whether the line after the first section read
          is more indented, indicating the start of a child.
          */
        {
      	  Aid theAid= prepareAndGetAid( );
      	  theAid.getSectionString( );  // Read past header section.
          return   // Is a leaf if line's indent is not greater than node's.  
            ! ( theAid.lineIndentI > theAid.nodeIndentI );
          }

      public int getChildCount( ) 
        /* Returns the number of outline subsections of this section.  */
        {
          if ( Misc.reminderB ) // Reminding me of this rarely called method.
            appLogger.debug("Outline.getChildCount() " + IDCode());
          Aid theAid= prepareAndGetAid( );
          if ( theAid.theChildCountI <= 0 )  // calculate if not done yet.
            { // calculate child count.
          		theAid.getSectionString( );  // Read past header.
              theAid.theChildCountI= 0;  // Initialize child count.
              while  // Count all children.
                ( theAid.lineIndentI > theAid.nodeIndentI )  // There is a child here.
                { // process this child.
              		theAid.theChildCountI++;  // Count it.
                  theAid.skipChild( );  // Skip past this child.
                  } // process this child.
              } // calculate child count.
          return theAid.theChildCountI;  // Return accumulated child count.
          }
    
      public DataNode getChild( int indexI ) 
        /* This returns the child with index indexI.
          If not then it calculates the child and 
          saves it in the cache for later.
          In either case it returns it.

          It doesn't get the child from an array cache if possible,
          but maybe it should???
          */
        { // getChild( int ) 
      	  Aid theAid= prepareAndGetAid( );

      	  theAid.getSectionString( );  // Read past 1st section to 1st child.

          for (int i=0; i < indexI; i++)  // Skip to the correct child.
          	theAid.skipChild( );

          if ( theAid.lineString.equals( "." ) )  // Line is a single '.'.
            return null;
            else
            return new Outline(  // Return Outline node for this record with...
              theAid.lineOffsetL  // ...its file offset.
            );
          } // getChild( int ) 

      public int getIndexOfChild( Object childObject ) 
        /* Returns the index of the filesystem root named by childObject 
          in the list of filesystem roots, or -1 if it is not found.
          It does they by comparing Object-s as File-s.

          ??? This is very inefficient because it calls getChild(int),
          which is itself slow, inside of a loop.
          This amounts to a doubly nested loop.
          Fortunately it doesn't seem to be called very often,
          except at startup sometimes.
          But it could be made much faster by
          rewriting like getChild(.).
          */
        {
          if ( Misc.reminderB ) // Reminding me of this rarely called method.
            appLogger.debug("Outline.getIndexOfChild(...)" );
            //System.out.println( "Outline.getIndexOfChild(...)" );

          return super.getIndexOfChild( childObject ) ;
          }

      /* ???
      public String getValueString( ) 
        { 
      	  return prepareAndGetAid().getSectionString( );
      	  }
      ??? */

    // Other methods.

      public String getNameString( )
        /* Returns String representing name of this Object.  */
        {
          Aid theAid= prepareAndGetAid( );
          return theAid.lineString;  // Return data from line read.
          }

      public String getContentString( )
	      /* Returns String representing name of this Object.  */
	      {
	        Aid theAid= prepareAndGetAid( );
	        return theAid.getSectionString( );
	        }

      private Aid prepareAndGetAid( )
        /* Prepares the Outline file for reading by:
            * Opening it if needed.
            * Seeking the position of the data for this object.
            * Reading and storing the first line in the file.
            * 
          It also returns an Aid object containing variables needed for
          more operations. 
          */
        { // prepareAndGetAid( )
      		Aid theAid= new Aid();  // Create theAid.
      	  theAid.fileOffsetL= startingOffsetL;
      		synchronized (lockObject) { // Thread-safe preparation of file.
	          if ( theRandomAccessFile == null )  // Open the file if needed.
	            prepareTheRandomAccessFile();
      			}
          theAid.readLine( );  // Read first line.
          theAid.debugString= theAid.lineString;  // Save first line as ID for debugging.
          theAid.nodeIndentI= theAid.lineIndentI;  // Save indent level of section.
          return theAid;
          } // prepareAndGetAid( )

      private void prepareTheRandomAccessFile()
        /* This grouping method prepares the RandomAccessFile by
          creating it using resource Outline.txt.
          The reason for this is because files inside jar files
          can be read only as streams, probably because
          they must be uncompressed as a stream.
          To access them randomly they must be extracted.
          
          ??? Create RandomAccessStream which reads acts like RandomAccessFile
          but appends lines of stream only as needed.
          This might hot help much with Outline because Jtree
          seems to need to know the child count,
          but it might be helpful for other resources 
          distributed in jar files.
          */
        { // prepareTheRandomAccessFile(..)
          InputStream resourceInputStream= 
            getClass().getResourceAsStream("Outline.txt");
          { // open random-access file for creation and reading.
            try { // Try creating file.
              theRandomAccessFile=  // For open random access Outline file.
                new RandomAccessFile( "Outline.tmp", "rw" );
              } // Try creating file.
            catch (FileNotFoundException e) { // Handle any errors.
              e.printStackTrace();
              } // Handle any errors.
            } // open random-access file for creation and reading.
          { // copy bytes from ResourceInputStream to theRandomAccessFile.
            int byteI;
            try {
              while ( ( byteI= resourceInputStream.read() ) != -1) 
                theRandomAccessFile.write( byteI );
              }
            catch ( IOException e ) {
              e.printStackTrace();
              }
            }
          } // prepareTheRandomAccessFile(..)

      public String toString()
        /* Returns a String representing this object, for JList. */
        { 
          return getNameString( );
          }

      @Override public boolean equals(Object other) 
        /* This is the standard equals() method.  */
        {
          boolean result = false;
          if (other instanceof Outline) {
              Outline that = (Outline) other;
              result = (this.startingOffsetL == that.startingOffsetL);
              }
          return result;
          }

      @Override public int hashCode() 
        /* This is the standard hashCode() method.  */
        {
          return (int) (41 * startingOffsetL);
          }

    } // class Outline
