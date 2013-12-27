package allClasses;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
// import java.nio.file.Paths;
// import java.nio.file.Path;

public class MetaFile
 
  /* This class manages the file(s) that contains the external 
    representation of this app's meta-data.
    This MetaData is stored internally in MetaNode-s rooted at MetaRoot.
    Each MetaNode is associated with a DataNode.
    DataNodes are rooted at the DataNode root.
    The application reads from the file(s) after the application starts,
    and it writes to the file(s) before the application terminates.
    
    ??? Can this class be divided into smaller files?
    Maybe make additional classes be subclasses,
    with only statics, and pass calls to them with a reference
    to MetaFile instance?
    */

  { // class MetaFile.

    public void methodToPreventUnusedIdentifierCompilerWarnings() {
      readFlatFileMetaNode( );
      readNestedFileMetaNode( );
      }
      
    // Enums.

      public enum RwStructure {  // Structure of the Meta state file text.
        // Although different, both of the following modes represent
        // a depth-first pre-order traversal.

        NESTED,  // Relationships are encoded by syntactic nesting.  
                 // ID numbers are present but redumdant. 
                 // Representing a general DAG is impossible.
                 // Eventually this mode will be used only for debugging.

        FLAT,    // Relationships are encoded by ID number references.
                 // Representing a general DAG is at least possible.

        };

      public enum Mode {  // Mode of access to file.

        READING,      // Reading entire file.
        WRITING,      // Writing entire file.
        LAZY_LOADING  // Reading nodes only when needed.

        };

    // Constants.

      private static final String FlatFileNameString= "Flat.txt";
      private static final String NestedFileNameString= "Nested.txt";

      private static final String FlatHeaderTokenString= 
        "Infogora-Flat-Meta-Data-File";
      private static final String NestedHeaderTokenString= 
        "Infogora-Nested-Meta-Data-File";

  	// Static variables.

    // Static variables.

      private static MetaFile lazyLoadMetaFile= null;  // For lazy-loading.
        // It stores a reference to the MetaFile used to load
        // the root MetaNode in lazy-loading mode.
        // If more MetaNodes need to be loaded later then is used again.
        // finish() closes its associated file at app termination.
      private static boolean forcedLoadingEnabledB=   // Enable/disable.
        false;

    // Instance variables.

      private String FileNameString;  // Name of associated external file.
      private RwStructure TheRwStructure;  // File's text structure.
      private String HeaderTokenString;  // First token in file.
      private Mode TheMode;  // File access mode.
      private RandomAccessFile theRandomAccessFile= null;  // For file access.
      private int indentLevelI; // Indent level of cursor in text file.
      private int columnI;  // Column of cursor  in text file.

      /* Saved stream state.  This is used for rewinding the file 
        in lazy-load node searches.  */
        private long savedFileOffsetLI;  // Saved offset of MetaNodes.
        private int savedIndentLevelI; // Saved indent level.
        private int savedColumnI;  // Saved column.

    // Start/finish methods and associated shutdown hook class.

      public static MetaNode start()
        /* Starts activity in this MetaFile class.
          It tries to read at least the root MetaNode from external file(s).
          It also prepares to run finish() at shutdown.
          It returns the root MetaNode gotten by reading, 
          or null if reading failed.
          
          ???During development it might read and write several files
          in different format to aid testing and debugging.
          */
        { // start()
          // System.out.println( "MetaFile.start()");

          MetaNode loadedMetaNode= null;  // Place for result root MetaNode.

          Misc.DbgOut( "MetaFile.start(..) lazyLoadWholeMetaNode()");  // Debug.
          loadedMetaNode=  // Try doing a lazy-load of flat file root node.
            lazyLoadWholeMetaNode( );

          // ??? Test by above read by writing special Debug file.
          Misc.DbgOut( "MetaFile.start(..) writeDebugFileV(..)");  // Debug.
          writeDebugFileV( loadedMetaNode );

          /*
          Misc.DbgOut( "MetaFile.start(..) readFlatFileMetaNode()");  // Debug.
          loadedMetaNode= // Read state from flat file.  
            readFlatFileMetaNode( );

          // ??? Test by above read by writing special Debug file.
          Misc.DbgOut( "MetaFile.start(..) writeDebugFileV(..)");  // Debug.
          writeDebugFileV( loadedMetaNode );
          */

          /*
          Misc.DbgOut( "MetaFile.start(..) readNestedFileMetaNode(..)");  // Debug.
          loadedMetaNode= // Read state from hierarchical file.   
            readNestedFileMetaNode( );
          */

          { // Prepare to run finish() when app terminates.
            ShutdownHook theShutdownHook = new MetaFile.ShutdownHook();
            Runtime.getRuntime().addShutdownHook(theShutdownHook);
            } // Prepare to run finish() when app terminates.

          Misc.DbgOut( "MetaFile.start(..) end.");  // Debug.

          return loadedMetaNode;  // Return the last value, if any, as result.
          } // start()

      static class ShutdownHook extends Thread 
        /* This is used to execute finish() at shutdown to save things.  */
        {
          public void run() {
            MetaFile.finish();  // Write any changed state information.
            }
          }

      public static void finish()
        /* Finishes activity in this MetaFile class.
          All new or modified MetaNode-s are saved to external file(s).
          This should be called before application termination.  
          Presently it is called by ShutdownHook.run().

          Presently it writes all nodes, whether modified or not.
          Also presently it recursively writes the entire MetaNode tree.
          This will change.  ???

          Evantually only one format will be saved,
          but presently, for development and debugging,
          both nested and flat files.
          These should contain equivalent information.
          The order of the writes is important because
          the lazy load file Flat.txt remains for lazy loading.
          Nested.txt must be written first, 
          so that all MetaNodes will be loaded.
          Then Flat.txt can be closed.
          Then a new Flat.txt can be written.
          */
        { // finish()
          Misc.DbgOut( "MetaFile.finish() begin.");

          Misc.DbgOut( "MetaFile.finish(..) forcedLoadingEnabledB= true");  // Debug.
          forcedLoadingEnabledB= true;  // Turn on forced loading.

          Misc.DbgOut( "MetaFile.finish(..) writeNestedFileV()");  // Debug.
          writeNestedFileV( MetaRoot.getRootMetaNode( ) );  // Write Nested file.
            // This provides an easy read dump of all MetaNodes.

          if ( lazyLoadMetaFile != null )  // Lazy loading file open.
            try { // Close it.
              Misc.DbgOut( "MetaFile.finish() closing lazy-loading Flat.txt.");
              lazyLoadMetaFile.theRandomAccessFile.close( );  // Close it.
              } // Close it.
            catch ( IOException e ) {  // Process any errors.
              e.printStackTrace();
              }  // Process any errors.

          Misc.DbgOut( "MetaFile.finish(..) writeFlatFileV()");  // Debug.
          writeFlatFileV( MetaRoot.getRootMetaNode( ) );  // Write Flat file.
            // This complete dump is what is lazy-loaded during next run.

          Misc.DbgOut( "MetaFile.finish() end.");
          } // finish()

    // Whole-file read methods.

      private static MetaNode readFlatFileMetaNode( )
        /* Reads all MetaNodes from a single Flat state file.  
          It returns the root MetaNode.

          This does the same thing as readNestedFileMetaNode( )
          except it does it with a flat file instead of a hierarchical one.
          Though having all state in a single flat state file
          is not the way flat state files will eventually be used,
          this is a good first step to going there.
          */
        {
          MetaFile theMetaFile= new MetaFile();
          // Set some appropriate mode variables.
          theMetaFile.TheRwStructure= RwStructure.FLAT;
          theMetaFile.FileNameString= FlatFileNameString;
          theMetaFile.HeaderTokenString= FlatHeaderTokenString;
          MetaNode loadedMetaNode=   // Do the actual read.
            theMetaFile.readFileMetaNode( );
          
          return loadedMetaNode;
          }

      private static MetaNode readNestedFileMetaNode( )
        /* Reads all MetaNodes from Nested state file.  
          It returns the root MetaNode.
          This will eventually exist only for debugging.
          */
        {
          MetaFile theMetaFile= new MetaFile();
          // Set some appropriate mode variables.
          theMetaFile.TheRwStructure= RwStructure.NESTED;
          theMetaFile.FileNameString= NestedFileNameString;
          theMetaFile.HeaderTokenString= NestedHeaderTokenString;
          MetaNode loadedMetaNode=   // Do the actual read.
            theMetaFile.readFileMetaNode( );
          
          return loadedMetaNode;
          }

      private MetaNode readFileMetaNode( )
        /* This method reads all MetaNodes from this MetaFile.  
          The format it expects depends on context.
          Returns the root MetaNode of what was read.  
          */
        {
          TheMode= Mode.READING;  // Set Mode to reading.

          MetaNode loadedMetaNode= null;  // Set null root because we are reading.
          File FileNameFile= AppFolders.resolveFile( FileNameString );
          try { // Read state.
            if  //  Read state from file if...
              ( FileNameFile.exists() )  // ...the file exists.
              { //  Read state from file.
                theRandomAccessFile=  // Open random access file.
                  new RandomAccessFile( FileNameFile, "r" );
                loadedMetaNode= rwFileMetaNode( loadedMetaNode );  // Read all state.
                DumpRemainder( );  // Output any file remainder for debugging.
                theRandomAccessFile.close( );  // Close the input file.
                } //  Read state from file.
            } // Read state.
          catch ( IOException | NumberFormatException e ) {  // Process any errors.
            e.printStackTrace();
            }  // Process any errors.

          return loadedMetaNode;
          }

    // Whole-file write methods.

      private static void writeFlatFileV( MetaNode inMetaNode )
        /* Writes all MetaNodes rooted at MetaNode inMetaNode
          to Flat file in FLAT format.
          If inMetaNode == null then it does nothing.
          Otherwise tt creates or overwrites the appropriate file.
          */
        {
          MetaFile theMetaFile= new MetaFile();
          // Set some appropriate mode variables.
          theMetaFile.TheRwStructure= RwStructure.FLAT;
          theMetaFile.FileNameString= FlatFileNameString;
          theMetaFile.HeaderTokenString= FlatHeaderTokenString;
          theMetaFile.writeFileV( inMetaNode );
          }

      public static void writeNestedFileV( MetaNode inMetaNode )
        /* This method writes all MetaNodes rooted at MetaNode inMetaNode
          to Nested file in NESTED format.
          If inMetaNode == null then it does nothing.
          Otherwise tt creates or overwrites the appropriate file.
          */
        {
          MetaFile theMetaFile= new MetaFile();
          // Set some appropriate mode variables.
          theMetaFile.TheRwStructure= RwStructure.NESTED;
          theMetaFile.FileNameString= NestedFileNameString;
          theMetaFile.HeaderTokenString= NestedHeaderTokenString;
          theMetaFile.writeFileV( inMetaNode );
          }

      public static void writeDebugFileV( MetaNode inMetaNode )
        /* This method is used for debugging this class.
          It is used for checking the result of a previous load.
          If inMetaNode == null then it does nothing.
          Otherwise tt creates or overwrites the Debug file containing
          all MetaNodes in Nested format rooted with inMetaNode.
          */
        {
          MetaFile theMetaFile= new MetaFile();
          // Set some appropriate mode variables.
          theMetaFile.TheRwStructure= RwStructure.NESTED;
          theMetaFile.FileNameString= "Debug.txt";
          theMetaFile.HeaderTokenString= "Debug-State";
          theMetaFile.writeFileV( inMetaNode );  // Do the actual write.
          }

      private void writeFileV( MetaNode inRootMetaNode )
        /* Writes all MetaNodes rooted at inRootMetaNode to inMetaFile.  
          This includes opening the file, writing the data, and closing.
          The format depends on context.
          If inMetaNode == null then it does nothing.
          Otherwise it creates or overwrites the appropriate file.
          */
        { // writeAllState(()
          if ( inRootMetaNode == null ) // There is NO MetaNode to process.
            ; // Do nothing.
            else // There IS a MetaNode to process.
            { // Write the data rooted at inMetaNode.
              TheMode= Mode.WRITING;  // Set Mode to writing.
              //File inputFile = new File( HomeFolderFile, FileNameString );
              File inputFile = AppFolders.resolveFile( FileNameString );
              //File outFile = new File( HomeFolderFile, FileNameString+".~" );
              File outFile = AppFolders.resolveFile( FileNameString+".~" );

              try { // Try opening or creating file.
                theRandomAccessFile=  // For open random access text file.
                  new RandomAccessFile( outFile, "rw" );
                } // Try opening or creating file.
              catch (FileNotFoundException e) { // Handle any errors.
                e.printStackTrace();
                } // Handle any errors.
              if  // Write if file was opened or created.
                ( theRandomAccessFile != null )
                try { // Try writing all MetaNodes.
                  rwFileMetaNode( inRootMetaNode );
                  theRandomAccessFile.setLength( // Truncate file at file...
                    theRandomAccessFile.getFilePointer( )  // ...pointer.
                    );
                  theRandomAccessFile.close( );
                  { // Replace input file by output file.
                    inputFile.delete();
                    outFile.renameTo(inputFile);
                    } // Replace input file by output file.
                  } // Try writing all MetaNodes.
                catch ( IOException e ) { // Handle any exception.
                  e.printStackTrace();
                  } // Handle any exception.
                } // Write the data rooted at inMetaNode.
           } // writeAllState(()

    // Methods for lazy and greedy loading of FLAT files.

      private static MetaNode lazyLoadWholeMetaNode( )
        /* Reads and returns the root MetaNode from a Flat meta file.  
          Its children are stored as IDNumber instances.
          Other nodes are lazy-loaded later when, and if, needed,
          by replacing IDNumber children by equivalent MetaNode children.
          */
        {
          MetaFile theMetaFile= new MetaFile();

          theMetaFile.TheRwStructure= RwStructure.FLAT;
          theMetaFile.FileNameString= FlatFileNameString;
          theMetaFile.HeaderTokenString= FlatHeaderTokenString;
          theMetaFile.TheMode= Mode.LAZY_LOADING;
          MetaNode loadedMetaNode=   // Do the actual read.
            theMetaFile.lazyLoadFileMetaNode( );
          
          return loadedMetaNode;
          }

      private MetaNode lazyLoadFileMetaNode( )
        /* This is a helper method which finishes the job of
          reading and returning the root MetaNode from a Flat meta file.  
          It includes opening the RandomAccessFile stream
          but not closing it so that it can be used for lazy-loading
          other nodes after this method returns.
          */
        {
          MetaNode loadedMetaNode= null;  // Set null root because we are reading.

          try { // Read state.
            if  //  Read state from file if...
              ( (AppFolders.resolveFile( FileNameString )).exists() )  // ...the file exists.
              { //  Read state from file.
                theRandomAccessFile=  // Open random access file.
                  new RandomAccessFile( 
                   AppFolders.resolveFile( FileNameString ), 
                   "r" 
                   );
                loadedMetaNode=   // Immediately read root node.
                  rwFileMetaNode( loadedMetaNode );
                lazyLoadMetaFile= // Save for lazy-loading of other nodes.
                  this;
                } //  Read state from file.
            } // Read state.
          catch ( IOException | NumberFormatException e ) {  // Process any errors.
            e.printStackTrace();
            }  // Process any errors.

          return loadedMetaNode;
          }

      public IDNumber readAndConvertIDNumber
        ( IDNumber inIDNumber, MetaNode parentMetaNode )
        throws IOException
        /* This method is used for loading of flat meta files,
          both lazy loading and greedy loading.
          It converts an IDNumber node into an equivalent MetaNode 
          by looking up the ID number in the FLAT text file
          and building a MetaNode from the text it finds there.
          It should be called only if ( TheRwStructure == RwStructure.FLAT ).

          It is called by special iterators that do lazy loading 
          by replacing IDNumbers by MetaNodes.
          It is called by other load routines that do greedy loading.

          If doing lazy loading then only one MetaNode is returned
          with no children attached.
          If doing greedy loading then the MetaNode is returned
          with all its children and other descendants attached.

          As usual, MetaNode parentMetaNode is for name lookup.
          */
        {
          RepeatDetector theRepeatDetector= new RepeatDetector();
          IDNumber resultIDNumber= null;
          int DesiredI= inIDNumber.getTheI();
          //Misc.DbgOut( "MetaFile.readAndConvertIDNumber("+DesiredI+") begin");  // Debug.
          while (true) { // Search state file for desired MetaNode.
            resultIDNumber= 
              readFlatWithWrapMetaNode( parentMetaNode, inIDNumber );
            if ( resultIDNumber == null )  // Exit if no MetaNode readable.
              break; 
            if  // Exit if node found.
              ( DesiredI == resultIDNumber.getTheI() )
              { 
                System.out.print( " S:"+DesiredI );  // Indicate node found.
                break;
                }
            System.out.print( " F:"+DesiredI );  // Indicate node NOT found.
            if ( theRepeatDetector.testB( resultIDNumber.getTheI() ) ) break;
            } // Search state file for desired MetaNode.
          if ( resultIDNumber == null ) // Did not find matching MetaNode.
            resultIDNumber= inIDNumber;  // Return original IDNumber node.
          return resultIDNumber;
          }

      private IDNumber readFlatWithWrapMetaNode
        ( MetaNode parentMetaNode, IDNumber inIDNumber )
        /* This is like rwMetaNode(..) except:
          * It must read from a FLAT file,
          * but if reading fails because an an end-of-file is encountered
            then it rewinds the file to the beginning of the MetaNodes
            and tries one more time.
          It returns a reference to the MetaNode that it read,
          or null if it was unable to read one.
          */
        throws IOException
        {
          //Misc.DbgOut( "MetaFile.readWithWrapFlatMetaNode(..) begin");  // Debug.
          IDNumber resultIDNumber= null;  // Set default result of not gotten.
          try {
            resultIDNumber=  // Try to...
              MetaNode.readParticularFlatMetaNode( // ...read flat MetaNode(s).
                this, inIDNumber, parentMetaNode
                );
            }
          catch ( Exception theException ) {  // Wrap if end of file.
            //Misc.DbgOut( "MetaFile.readWithWrapFlatMetaNode(..) wrapping" );
            restoreStreamStateV( );  // Rewind stream to beginning of MetaNodes.
            resultIDNumber=  // Try again to...
              MetaNode.readParticularFlatMetaNode( // ...read flat MetaNode(s).
                this, inIDNumber, parentMetaNode
                );
            }
          //Misc.DbgOut( "MetaFile.readWithWrapFlatMetaNode(..) end");  // Debug.
          return resultIDNumber;
          }

      private void saveStreamStateV( )
        throws IOException
        /* This method saves the stream state,
          to be restored if needed by restoreStreamStateV( ).
          It is called at the beginning of the first MetaNode
          in a FLAT file.
          */
        {
          savedFileOffsetLI= // Save file offset.
            theRandomAccessFile.getFilePointer();
          savedIndentLevelI= indentLevelI; // Saved indent level.
          savedColumnI= columnI;  // Saved column.
          }
    
      private void restoreStreamStateV( )
        throws IOException
        /* This method restores the stream state
          from values saved by saveStreamStateV( ).
          It is called at the end of the last MetaNode
          in a FLAT file to prepare rescanning the MetaNode list.
          It is called during lazy-loading to restart a search for
          a particular MetaNode near the beginning of a FLAT file.
          */
        {
          theRandomAccessFile.seek(  // Move file pointer back to...
            savedFileOffsetLI  // ...previosly saved point.
            );
          indentLevelI= savedIndentLevelI; // Restore indent level.
          columnI= savedColumnI;  // Restore column.
          }

    // Method to read or write entire file, depending on context.

      private MetaNode rwFileMetaNode( MetaNode inRootMetaNode )
        throws IOException
        /* This method reads or writes the Meta state file,
          which is assumed to have already been opened.
          It will read or write depending on context,
          the values of Mode and inRootMetaNode.
          If Writing then inRootMetaNode provides the
          MetaNode which is the root of the Meta state to be output,
          unless inRootMetaNode is null which results in no operation.
          IF Reading then inRootMetaNode is ignored.
          Returns the root MetaNode, either the one which was written
          or the one which was read.
          */
        {
          if // Do nothing or process depending on conditions.
            ( ( TheMode == Mode.WRITING ) &&
              ( inRootMetaNode == null )
              ) 
            ;  // Do nothing because there is nothing to write.
            else
            { // Read or write process.
              indentLevelI= 0;  // Initialize indent level of text in file.
              columnI= 0;  // Initialize column of text in file.

              rwLiteral( HeaderTokenString ); // Begin file with header token.
              rwIndentedWhiteSpace( );  // reset column to 0.
              saveStreamStateV( );  // Save stream state at 1st MetaNode.

              inRootMetaNode=  // The root MetaNode becomes...
                MetaNode.rwFlatOrNestedMetaNode(  // ...read or write... 
                  this, // ...using this MetaFile...
                  inRootMetaNode,  // ...of the root MetaNode using...
                  MetaRoot.getParentOfRootMetaNode() // ...parent for lookup.
                  );
              } // Read or write process.

          return inRootMetaNode;  // Return the new or old root.
          }

    // Methods for reading and writing pieces of meta file.

      public String readTokenString( )
        // MAYBE PREVENT EMPTY RESULT???
        /* Reads a token from file and returns it as a String.
          The token is assumed to begin at the current file position and
          be terminated by either a space or new-line,
          or be delimited by double-quotes.
          On return the file position is after the token,
          and before the delimiting white space, if any.
          It uses String.intern( ) on returned strings can be compared using 
          "==" and "!=".  

          This also handles IOException-s.  
          */
        { // readTokenString( String InString )
          String TokenString= "";  // Set token character accumulator to empty.
          try {
            long StartingOffsetLI= // Save offset of beginning of token.
              theRandomAccessFile.getFilePointer();
            int ByteI= theRandomAccessFile.read( );  // Try reading first byte.
            if ( (char)ByteI == '\"') // Handle quoted string.
              while (true) { // Process entire token, if any.
                ByteI= theRandomAccessFile.read( );  // Try reading token byte.
                if ( ByteI == -1 || ByteI == '\"' )  // End of token.
                  break;  // Exit loop. 
                TokenString+= (char)ByteI;  // Append byte to string.
                } // Process entire token, if any.
              else  // Handle white-space delimited string.
              while (true) { // Process entire token, if any.
                if ( ByteI == -1 || ByteI == ' ' || ByteI == '\n' )  // End of token.
                  { // Back up file offset and exit.
                    theRandomAccessFile.seek( // Move file pointer back to...
                      StartingOffsetLI + TokenString.length() ); // ...end of token.
                    break;  // Exit loop. 
                    } // Back up file offset and exit.
                TokenString+= (char)ByteI;  // Append byte to string.
                ByteI= theRandomAccessFile.read( );  // Try reading next byte.
                } // Process entire token, if any.
            columnI+= // Adjust columnI for file offset movement.
              ( theRandomAccessFile.getFilePointer() - StartingOffsetLI );
            }
          catch ( IOException e ) {
            e.printStackTrace();
            }
          return TokenString.intern( );  // Return string or an older equal one.
          } // readTokenString( String InString )
    
      public void writeToken( String InTokenString )
        /* Outputs InTokenString using the appropriate delimiters.  */
        { // writeToken(..)
          int InStringLengthI= InTokenString.length();

          try {
            if ( InTokenString.indexOf( ' ' ) >= 0 )  // Token contains space.
              theRandomAccessFile.writeByte( '\"' );  // Write double-quote.
            { // Write litterl string.
              theRandomAccessFile.writeBytes( InTokenString );
              columnI+= InStringLengthI;  // Adjust columnI for string length.
              } // Write litterl string.
            if ( InTokenString.indexOf( ' ' ) >= 0 )  // Token contains space.
              {
                theRandomAccessFile.writeByte( '\"' );  // Write double-quote.
                columnI+= 2; // Adjust columnI double-quotes.
                }
            }
          catch ( IOException e ) {
            e.printStackTrace();
            }
          } // writeToken(..)
      
      public void rwIndentedWhiteSpace( )
        /* Goes to a new line if needed, and indents to the correct level.  */
        { 
          if // Go to a new line if...
            ( columnI > indentLevelI )  // ...past indent level.
            { // Go to a new line.
              rwLiteral( "\n" );  // Go to new line.
              columnI= 0;  // Reset to column 0.
              } // Go to a new line.
          while  // Add spaces to indent while...
            ( columnI < indentLevelI )  // ...column is less than indent level.
            if (rwLiteral( " " ))  // Try to add a single space.
              break;  // Exit loop if any error.
          }
      
      public void rwListBegin( )
        /* Begins a section.  */
        { 
          rwIndentedLiteral( "(" );  // Go to new line.
          indentLevelI += 2;  // Increment indent level.
          }
    
      public void rwListEnd( )
        /* Ends a section.  */
        { 
          rwIndentedLiteral( ")" );  // Output end token.
          indentLevelI -= 2;  // Decrement indent level.
          }
    
      public void rwIndentedLiteral( String InString )
        /* rw-processes a string in the file, on a new line, 
          at correct indent level.  */
        { 
          rwIndentedWhiteSpace( );  // rw-process line and indent.
          rwLiteral( InString );  // rw-process string.
          }

      public boolean rwLiteral( String InString )
        /* If wrriting then it writes the literal InString to the file 
          If reading then it reads a literal String from the file and
          verifies that it equals InString.
          It also handles IOException-s.  
          Literals can be or can include white space.
          It returns true if there is an error, false otherwise.
          The return value is used mainly to prevent infinite loops.
          */
        { // rwLiteral( String InString )
          boolean ErrorB= false;  // Assume no error.
          int InStringLengthI= InString.length();

          try {
            if ( TheMode == Mode.WRITING )  // Writing state.
              { // Write literal string.
                theRandomAccessFile.writeBytes( InString );
                columnI+= InStringLengthI;  // Adjust columnI for string length.
                } // Write litterl string.
              else  // Reading state.
              { // Read and verify String.
                if ( testLiteralB( InString ))
                  {
                    theRandomAccessFile.skipBytes( InStringLengthI );
                    columnI+= InStringLengthI;  // Adjust columnI for string length.
                    }
                  else
                  {
                    ErrorB= true;  // Set error return value.
                    System.out.print( 
                      "\nrwLiteral( '"+
                      InString+
                      "' ) MISMATCH!!!"
                      );
                    DumpRemainder( );  // Output anything that remains.
                    }
                } // Read and verify String.
            }
          catch ( IOException e ) {
            e.printStackTrace();
            }
          return ErrorB;  // Return error value.
          } // rwLiteral( String InString )

      public int testTerminatorI( String DesiredString )
        /* Tests whether a terminator is next in the file.
          A terminator is either the literal String DesiredString 
          or the EndOfFile.
          If DesiredString is there then it returns an int > 0.
          If EndOfFile is there then it returns an int < 0.
          If neither terminator is ther then it returns 0.
          In any case the stream position is unchanged.
          It also handles IOException-s.  */
        { // testTerminatorI( String DesiredString )
          int ResultI= 1;  // Set default result to indicate terminator found.
          try {
            long StartingOffsetLI= // Save offset of beginning of token.
              theRandomAccessFile.getFilePointer();
            int ByteI;  // Place for bytes input.
            int IndexI= 0; 
            while ( true ) // Process all characters if possible.
              { // Process one character or exit loop.
                if ( IndexI >= DesiredString.length() )  // String exhausted.
                  break;  // Exit loop with ResultI indicating terminator-found.
                ByteI= // Try reading a byte.
                  theRandomAccessFile.read( );
                if ( ByteI != DesiredString.charAt( IndexI ) )
                  { // Exit loop with either string found or End-Of-File.
                    ResultI= 0;  // Set result Indicating terminator-not-found.
                    if ( ByteI < 0 ) // If End-Of-File encountered...
                      ResultI= -1;  // ...override for End-Of-File result.
                    break;  // Exit loop.
                    } // Exit loop with either string found or End-Of-File.
                IndexI++;  // Advance index.
                } // Process one character or exit loop.
            theRandomAccessFile.seek( // Move file pointer...
              StartingOffsetLI );  // ... back to original position.
            }
          catch ( IOException e ) {
            e.printStackTrace();
            }
          return ResultI;  // Return result calculated above.
          } // testTerminatorI( String DesiredString )
    
      public boolean testLiteralB( String DesiredString )
        /* Tests whether the literal String DesiredString 
          is next in the file.
          If DesiredString is there then it returns true.
          If DesiredString is not read then it returns false.
          In either case the stream position is unchanged.
          It also handles IOException-s.  */
        { // testLiteral( String DesiredString )
          return testTerminatorI( DesiredString ) != 0;
          } // testLiteral( String DesiredString )

    // Miscellaneous methods.
    
      private void DumpRemainder( ) throws IOException
        /* This method is used to help debug MetaFile code.
          It dumps the remainder of the text file to System.out.
          If there is no remaining text then it outputs nothing.
          If there is remainder text then it outputs it enclosed
          inside of labeled arrows.
          This method can be called when there are file parsing errors,
          and when a file appears to be completely parsed.
          */
        { // DumpRemainder( )
          int ByteI= theRandomAccessFile.read( );  // Try to read first byte.
          if ( ByteI != -1 ) // If success then output it and remainder.
            { // Output header and all file bytes.
              //Misc.DbgOut( "MetaFile.DumpRemainder( ) ");  // Debug.
              System.out.print( // Introduce the data which will follow.
                "  Unread file bytes follow arrow ->" 
                );
              do { // Display bytes until done.
                System.out.print( (char)ByteI );  // Display the byte already read.
                ByteI= theRandomAccessFile.read( );  // Try to read next byte.
                } while ( ByteI != -1 ); // Display bytes until done.
              System.out.print( // Introduce the data which will follow.
                "<- Unread file bytes precede the arrow." 
                );
              } // Output header and all file bytes.
          } // DumpRemainder( )

    // Getter methods for access to useful modes and values.
    
      public Mode getMode()
        { return TheMode; }

      public RwStructure getRwStructure()
        { return TheRwStructure; }

      public static MetaFile getLazyLoadMetaFile()
        { return lazyLoadMetaFile; }

      public static boolean getForcedLoadingEnabledB()
        { return forcedLoadingEnabledB; }

    } // class MetaFile.
    