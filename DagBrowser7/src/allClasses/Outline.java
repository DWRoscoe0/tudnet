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
import static allClasses.Globals.NL;

public class Outline

	extends MutableList

  /* The purpose of this class is to create 
    a large subtree for Infogora demonstrations
    from a text file that uses indented lines as in an outline.
    
    This class does lazy-loading, based on needs.
    
    ?? This class is supposed to be temporary.
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
      private long startingOffsetL;  // Nodes starting file offset.
        // This value is sufficient to distinguish one node from another.
      private LoadState theLoadState= LoadState.CONSTRUCTED;

    public enum LoadState {

      CONSTRUCTED, // has temporary name. and is not displayable yet.
      LEAF, // displayable.  name defined.  body text to be loaded from file.
      EMPTY_BRANCH, // displayable.  name defined.  has no children yet.
        // children will be lazy loaded after node is selected.
      FULL_BRANCH // displayable.  has all its children.

      };

    static class Aid { // Helper class for Outline.

      long fileOffsetL;  // File offset of file when reading.
      String debugString;  // Line data for use during debugging.

      // pseudo-statics variables for readline() processing.
      long lineOffsetL;  // File offset of line.
      int lineIndentI;  // Indent level (# of leading blanks) in line
      String lineString;  // Line data without leading and trailing blanks.

      // pseudo-statics variables for cached node properties.
      int nodeIndentI;  // Indent level (# of leading blanks) for this node.
      int theChildCountI= -1;  // cached count of children.

      private static boolean readLineReentryB= false; // ?? Debug.
        // Should no longer be needed because of synchronized (lockObject). 

      private void readLineV( )
        /* Reads one next line of the outline file.
          It stores information about what it read in theAid object.
          If an EndOfFile is encountered then it simulates
          the reading of a line containing only a ".".
          */
        { // readLineV.
      		synchronized (lockObject) {
	      		if ( readLineReentryB ) // ?? catch 2nd thread at entrance.
	      			{ readLineReentryB= false;             
	      			  appLogger.error( "Outline: readLineV() REENTRY Begin.");
	      				}
	      			else
	      			readLineReentryB= true;
	          int maxTriesI= 3;
	          int triesI= 1;
	          readlineRetryLoop: while (true) {
	            if  // Checking and exiting if maximum retries were exceeded.
	              ( triesI > maxTriesI )  // Maximum attempts exceeded.
	              { // Terminating thread.
	                appLogger.error( "Outline.readLineV() retries failed." );
	                break readlineRetryLoop; // Exiting with failure.
	                }
	            try { 
	            	  doReadLineV( ); 
	                break readlineRetryLoop; // Exiting with success.
	            	  } 
	              catch ( IOException e ) {
	              	try {
		                appLogger.info( "Outline.readLineV() re-openning file." );
	              		theRandomAccessFile.close(); // Closing bad file.
	                  theRandomAccessFile=  // Re-opening it.
	                      new RandomAccessFile( "Outline.tmp", "r" );
	              		}
		              catch ( IOException e1 ) {
		                appLogger.error( "Outline.readLineV() re-open failed." );
		                }
	              	};
	    	      triesI++;
	            } // readlineRetryLoop: 
	      		if ( ! readLineReentryB ) // ?? catch 1st thread at exit.
		    			{ readLineReentryB= true;
		  			  	appLogger.error( "Outline: readLineV() REENTRY End.");
		    				}
	      			else
	      			readLineReentryB= false;
      			}
          } // readLineV.

      private void doReadLineV( )
        throws IOException
        // Does readLine() except for setup and error recovery and retrying.
        {
	    		lineString= null; // Clearing String accumulator.
	    	  lineOffsetL= fileOffsetL;  // Saving file offset of line being read.
	        try 
	          {
	        	  theRandomAccessFile.seek(fileOffsetL);  // Seeking read point.
	            }
	          catch (IOException e) { // Logging and re-throwing.
	            appLogger.error(
	            		"Outline.readLineV() seek() "+fileOffsetL + NL + e
	            		);
	            throw e;
	            } // Handle errors.
	        try 
		        {
		      		lineString = theRandomAccessFile.readLine();  // Reading line.
		          }
	          catch (IOException e) { // Logging and re-throwing.
		          appLogger.error("Outline.readLineV() readline()" + NL + e);
	            throw e;
		          } // Handle errors.
	        try 
		        {
		      	  fileOffsetL= // Saving end point.
		      	  		theRandomAccessFile.getFilePointer(); 
		          }
	          catch (IOException e) { // Logging and re-throwing.
		          appLogger.error("Outline.readLineV() getFilePointer()" + NL +e);
	            throw e;
		          } // Handle errors.
	        if (lineString == null) // Handling End of file (shouldn't happen).
	          {
	            //appLogger.info( "Outline: readLineV() replacing EOF with '.'");
	        	  lineString= ".";  // Simulating read of a terminator line.
	            }
	        { // Measuring line indent level.
	        	lineIndentI= 0;  // Clearing indent level.
	          //appLogger.info( "Outline: readLineV(),lineString= "+lineString );
	          while // Incrementing indent level for each leading space in line.
	            ( ( lineIndentI < lineString.length() ) &&
	              ( lineString.charAt( lineIndentI ) == ' ' )
	              )
	          	lineIndentI++;
	          } // Measure indent level.
	        lineString=  // Removing leading and trailing spaces from line.
	        		lineString.trim( ); 
	        }

      public String getBodyString()
        /* This returns a String containing all the text
          at the beginning of a node, after its name,
          but before its children, if any.
          It assumes that the line containing the name has already been read.
          */
        { // String p;( )
      		readLineV( );  // Skipping line containing name.
          String totalString= "";  // Initialize String accumulator.
          while // Accumulate all lines in the section which...
            ( ( lineIndentI <= nodeIndentI ) && // ...are not indented more...
              ( !lineString.equals( "." ) ) // ...and isn't a single '.'.
              )
            { // Accumulate line.
              totalString+= lineString;  // Append present line.
              totalString+= '\n';  // Append newline character.
              readLineV( );  // Read next line.
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
              readLineV( );  // Skip child line.
              } // Skip child lines but stop after last one.
          } // skipChild( )
    
      } // class Aid

    public Outline( 
    		long offsetInL 
    		) // Constructor.
      /* Constructs an Outline node whose source can be found
        in Outline file at offset offsetInL.
        Normally the root node is constructed with Outline( 0 ).
        */
      { // Outline(.)
    		initializeV(
	        NamedNonLeaf.temporaryNameString, 
          new DataNode[]{} // Initially empty List for lazy-loaded children.
	        );
        startingOffsetL= offsetInL;  // Save starting file offset.
        } // Outline(.)

    // A subset of delegated AbstractTreeModel interface methods.

      public boolean isLeaf( ) 
        /* Returns whether the line after the first section read
          is more indented, indicating the start of a child.
          */
        {
	      	preparePartiallyLoadedNodeV();
	      	return (theLoadState == LoadState.LEAF );
          }

      public int getChildCount( ) 
        /* Returns the number of outline subsections of this section.  */
        {
      		prepareFullyLoadedNodeV();
      	  return super.getChildCount();
      	  }
        
        public DataNode getChild( int indexI ) 
          // This returns the child with index indexI.
        {
      		prepareFullyLoadedNodeV();
      	  return super.getChild(indexI);
        	}
        
    // Other getter methods.

      public String getNameString( )
	      /* Returns String representing name of this Object.  */
	      {
	      	preparePartiallyLoadedNodeV();
	        //return theAid.lineString;  // Return data from line read.
	        return super.getNameString( );
	        }

      public String getContentString( )
	      // Returns String representing the body text of this Object.
	      {
	        Aid theAid= prepareAndGetAid( );
	        return theAid.getBodyString( );
	        }

      public String toString()
        /* Returns a String representing this object, for JList. */
        { 
          return getNameString( );
          }

      // Other methods.

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

      // Access preparation methods.

      private void prepareFullyLoadedNodeV()
        // Prepares for accessing this Outline node as a fully loaded node.
        {
      		stateLoop: while (true) {
	      	  switch ( theLoadState ) {
			    	  case CONSTRUCTED: 
			    	  	preparePartiallyLoadedNodeV();
			    	  	break;
			    	  case EMPTY_BRANCH:
			    	  	loadChildrenV();
			      	  break;
			    	  case LEAF: 
			    	  	break stateLoop; // Exiting loop with terminal state.
			    	  case FULL_BRANCH: 
			    	  	break stateLoop; // Exiting loop with terminal state.
		    	  	}
      			} // stateLoop:
      	  //if ( theLoadState.ordinal() < LoadState.FULL_BRANCH.ordinal() )
          }

	  	private void loadChildrenV() 
		  	{
	        Aid theAid= prepareAndGetAid( );
      		theAid.getBodyString( );  // Read past header.
          theAid.theChildCountI= 0;  // Initialize child count.
          while  // Processing all children indicated by indented first lines.
            ( theAid.lineIndentI > theAid.nodeIndentI )
            { // process this child.
	            DataNode childDataNode= new Outline( // Constructing child node
		            theAid.lineOffsetL //,  // for this file offset.
	            	);
	            addRawV(childDataNode); // Add to parent List, not on EDT. 
	            //addB(childDataNode); // Add to parent List, on EDT.  Fails.
	              // This causes a StackOverflowError.
          		theAid.theChildCountI++;  // Count it.
              theAid.skipChild( );  // Skip past this child.
              } // process this child.
      	  theLoadState= LoadState.FULL_BRANCH; // Recording new node state.
	  	  	}
	  	
      private void preparePartiallyLoadedNodeV()
      	/* Prepares for accessing this Outline node as a partially loaded node.
          It return a helper Aid object. 
          */
        {
      	  switch ( theLoadState ) {
	      	  case CONSTRUCTED: ;
		      	  { // Doing partial loading.
	      	  		Aid theAid= prepareAndGetAid();
		      	  	setNameStringV(  // Redefining name from 
		      	  			theAid.lineString  // first line read from node text.
		      	  			);
		        	  theAid.getBodyString( );  // Read past header section.
		            if ( // Setting the LoadState based on whether  
			              ! ( theAid.lineIndentI > theAid.nodeIndentI ) 
			              ) // line's indent is not greater than node's.
		            	theLoadState= LoadState.LEAF;
		            	else
		            	theLoadState= LoadState.EMPTY_BRANCH;
		      	  	}
	      	    break;
	      	  default:
	      	  }
          }

      private Aid prepareAndGetAid()
        /* Prepares for accessing this Outline node in the way before 
          lazy loading was added.
          This includes returning a helper Aid object 
          containing variables needed for more operations. 
          This is the original Aid getter before lazy loading.
          */
        { // prepareAndGetAid( )
      		Aid theAid= makeAid();  // Create theAid.
      	  theAid.fileOffsetL= startingOffsetL;
      	  prepareFileV();
          theAid.readLineV( );  // Read first line.
          theAid.debugString= theAid.lineString;  // Save first line as ID for debugging.
          theAid.nodeIndentI= theAid.lineIndentI;  // Save indent level of section.
          return theAid;
          } // prepareAndGetAid( )

      private Aid makeAid()
	    	/* Makes and returns a helper Aid object.  */
        { // prepareAndGetAid( )
      		Aid theAid= new Aid();  // Create theAid.
          return theAid;
          }
  
  	  private void prepareFileV()
  	    // Prepares theRandomAccessFile if it was not done already.
	  	  {
		  		synchronized (lockObject) { // Thread-safe creation of file.
		        if ( theRandomAccessFile == null )  // Open the file if needed.
		          createTheRandomAccessFileV();
		  			}
		  	  }

      private void createTheRandomAccessFileV()
        /* This grouping method prepares the RandomAccessFile by
          making a copy of resource file Outline.txt.
          The reason for this is because when resource files 
          are inside jar files they can be read only as streams, 
          probably because they must be uncompressed as a stream.
          To access them randomly they must be uncompressed and copied first.
          
          ?? Create LazyRandomAccessFile which acts like RandomAccessFile
          but appends lines of resource stream only as needed.
          This might not help much with this Outline class because 
          Jtree seems to need to know the child count,
          but it might be helpful for other resources 
          distributed in jar files.
          */
        { // createTheRandomAccessFileV(..)
          InputStream resourceInputStream= 
            getClass().getResourceAsStream("Outline.txt");
          { // open random-access file for creation and reading.
            try { // Try creating file.
              theRandomAccessFile=  // For open random access Outline file.
                new RandomAccessFile( "Outline.tmp", "rw" );
              } // Try creating file.
            catch (FileNotFoundException e) { // Handle any errors.
              appLogger.error("Outline.createTheRandomAccessFileV(), new "+e);
              } // Handle any errors.
            } // open random-access file for creation and reading.
          { // copy bytes from ResourceInputStream to theRandomAccessFile.
            int byteI;
            try {
            	theRandomAccessFile.setLength(0); // Empty the file.
              while ( ( byteI= resourceInputStream.read() ) != -1) 
                theRandomAccessFile.write( byteI );
              }
            catch ( IOException e ) {
              appLogger.error("Outline.createTheRandomAccessFileV(), read() "+e);
              }
            }
          } // createTheRandomAccessFileV(..)

    } // class Outline
