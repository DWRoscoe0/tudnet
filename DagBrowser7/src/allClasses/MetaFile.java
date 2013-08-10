package allClasses;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

public class MetaFile
 
  /* This class manages the file(s) that contains the external 
    representation of this app's meta-data which is stored in
    the MetaNode-s rooted at MetaRoot associated with 
    the DataNode-s rooted at the DataNode root.
    It reads from the file(s) after the application starts,
    and it writes to the file(s) before the application terminates.
    */

  { // class MetaFile.

    public enum RwStructure {  // Structure of the Meta state file text.
      // Although different, both of the following modes represent
      // a depth-first pre-order traversal.

    	NESTED,  // Relationships are encoded by syntactic nesting.  
               // ID numbers are present but redumdant. 
               // Representing a general DAG is impossible.
               // Eventually this mode will be used only for debugging.

      FLAT     // Relationships are encoded by ID number references.
               // Representing a general DAG is at least possible.
      };

    // Constants.

      private static final String FlatFileNameString= "Flat.txt";
      private static final String NestedFileNameString= "Nested.txt";

      private static final String FlatHeaderTokenString= 
        "Infogora-Flat-Meta-Data-File";
      private static final String NestedHeaderTokenString= 
        "Infogora-Nested-Meta-Data-File";

    // Instance variables.

      private String FileNameString;  // Name of state file.
      public RwStructure TheRwStructure;  // Text file structure.
      private String HeaderTokenString;  // First string in file.
      private boolean WritingB;  // true means Writing.  false means Reading.
      private RandomAccessFile theRandomAccessFile= null;
      private int indentLevelI; // Indent level in text file.
      private int columnI;  // Column of rw in text file.

      // Saved stream state, used to restart scan of MetaNodes in FLAT file.
      private long savedFileOffsetLI;  // Saved offset of MetaNodes.
      private int savedIndentLevelI; // Saved indent level.
      private int savedColumnI;  // Saved column.
    
    // Methods.

      public static MetaNode start( DataNode InRootDataNode )
        /* Starts activity in this MetaFile class.
          It tries to read at least the root MetaNode from external file(s).
          It also prepares to run finish() at shutdown.
          It returns the root MetaNode gotten by reading, 
          or null if reading failed.
          */
        { // start()
          // System.out.println( "MetaFile.start()");

          MetaNode RootMetaNode= null;  // Place for result root MetaNode.

          RootMetaNode= // Read state from flat file.  
            readFlatStateMetaNode( );

          // ??? Test by writing special Debug file.
          writeDebugState( RootMetaNode );

          RootMetaNode= // Read state from hierarchical file.   
            readNestedStateMetaNode( );

          { // Prepare to run finish() when app terminates.
            ShutdownHook shutdownHook = new ShutdownHook();
            Runtime.getRuntime().addShutdownHook(shutdownHook);
            } // Prepare to run finish() when app terminates.

          return RootMetaNode;  // Return the read root MetaNode, if any.
          } // start()

      private static MetaNode readFlatStateMetaNode( )
        /* Reads all MetaNodes from a single Flat state file.  
          It returns the root MetaNode.

          This does the same thing as readNestedStateMetaNode( )
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
          MetaNode RootMetaNode=   // Do the actual read.
            theMetaFile.readStateMetaNode( );
          
          return RootMetaNode;
          }

      private static MetaNode readNestedStateMetaNode( )
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
          MetaNode RootMetaNode=   // Do the actual read.
            theMetaFile.readStateMetaNode( );
          
          return RootMetaNode;
          }

      private MetaNode readStateMetaNode( )
        /* This method reads all MetaNodes, or at least the root one, 
          from this MetaFile.  
          Returns the root MetaNode of what was read.  
          */
        {
          WritingB=  false;  // Indicate that we are reading.
          MetaNode RootMetaNode= null;  // Set null root because we are reading.

          try { // Read state.
            if  //  Read state from file if...
              ( (new File( FileNameString )).exists() )  // ...the file exists.
              { //  Read state from file.
                theRandomAccessFile=  // Open random access file.
                  new RandomAccessFile( FileNameString, "r" );
                RootMetaNode= rwFileMetaNode( RootMetaNode );  // Read all state.
                DumpRemainder( );  // Output any file remainder for debugging.
                theRandomAccessFile.close( );  // Close the input file.
                } //  Read state from file.
            } // Read state.
          catch ( IOException | NumberFormatException e ) {  // Process any errors.
            e.printStackTrace();
            }  // Process any errors.

          return RootMetaNode;
          }

      public static void finish()
        /* Finishes activity in this MetaFile class.
          All new or modified MetaNode-s are saved to external file(s).  
          Presently it writes all nodes, whether modified or not.
          Also presently it recursively writes the entire RootMetaNode tree.
          This should be called before application termination.  
          Presently it is called by ShutdownHook.run().
          */
        { // finish()
          // System.out.println( "\n\nMetaFile.finish()");

          writeNestedState( );  // Write in Nested text format.

          writeFlat( );  // Write in Flat text format.

          } // finish()

      private static void writeFlat( )
        /* Writes all MetaNodes to Flat file.  */
        {
          MetaFile theMetaFile= new MetaFile();
          // Set some appropriate mode variables.
          theMetaFile.TheRwStructure= RwStructure.FLAT;
          theMetaFile.FileNameString= FlatFileNameString;
          theMetaFile.HeaderTokenString= FlatHeaderTokenString;
          theMetaFile.writeStateV(   // Do the actual write.
            MetaRoot.getRootMetaNode( ) 
            );
          }

      public static void writeNestedState( )
        /* Writes all MetaNodes to Nested file.  */
        {
          MetaFile theMetaFile= new MetaFile();
          // Set some appropriate mode variables.
          theMetaFile.TheRwStructure= RwStructure.NESTED;
          theMetaFile.FileNameString= NestedFileNameString;
          theMetaFile.HeaderTokenString= NestedHeaderTokenString;
          theMetaFile.writeStateV(   // Do the actual write.
            MetaRoot.getRootMetaNode( ) 
            );
          }

      public static void writeDebugState( MetaNode InMetaNode )
        /* This method is used for debugging this class.
          It writes all MetaNodes in Nested format to the Debug file.  
          */
        {
          MetaFile theMetaFile= new MetaFile();
          // Set some appropriate mode variables.
          theMetaFile.TheRwStructure= RwStructure.NESTED;
          theMetaFile.FileNameString= "Debug.txt";
          theMetaFile.HeaderTokenString= "Debug-State";
          theMetaFile.writeStateV( InMetaNode );  // Do the actual write.
          }

      private void writeStateV( MetaNode inRootMetaNode )
        /* Writes all MetaNodes rooted at inRootMetaNode to inMetaFile.  */
        { // writeAllState(()
          WritingB=  true;  // Indicate that we are writing.

          try { // Try opening or creating file.
            theRandomAccessFile=  // For open random access text file.
              new RandomAccessFile( FileNameString, "rw" );
            } // Try opening or creating file.
          catch (FileNotFoundException e) { // Handle any errors.
            e.printStackTrace();
            } // Handle any errors.
          if  // Write if file was opened or created.
            ( theRandomAccessFile != null )
            try { // Try writing all MetaNodes.
              rwFileMetaNode( inRootMetaNode );
              theRandomAccessFile.setLength( // Truncate file at...
                theRandomAccessFile.getFilePointer( )  // ...file pointer.
                );
              theRandomAccessFile.close( );
              } // Try writing all MetaNodes.
            catch ( IOException e ) { // Handle any exception.
              e.printStackTrace();
              } // Handle any exception.
           } // writeAllState(()

      private MetaNode rwFileMetaNode( MetaNode inRootMetaNode )
        throws IOException
        /* This method reads or write the Meta state file,
          which is assumed to have already been opened.
          This includes the file header token.
          It will read or write depending on the values of 
          WritingB and inRootMetaNode.
          If WritingB then inRootMetaNode references the
          MetaNode which is the root of the Meta state.
          Returns the root MetaNode, either the one which was written
          or the one which was read.
          */
        {
          indentLevelI= 0;  // Initialize indent level of text in file.
          columnI= 0;  // Initialize column of text in file.

          rwLiteral( HeaderTokenString ); // Begin file with header token.
          rwIndentedWhiteSpace( );  // reset column to 0.
          saveStreamStateV( );  // Save stream state at 1st MetaNode.

          inRootMetaNode= MetaNode.rwFlatOrNestedMetaNode(  // Read or write... 
            this, // ...using this MetaFile...
            inRootMetaNode,  // ...the root MetaNode using...
            DataRoot.getParentOfRootDataNode()  // ...parent for read lookups.
            );

          return inRootMetaNode;  // Return the new or old root.
          }

      public IDNumber readAndConvertIDNumber
        ( IDNumber inIDNumber, DataNode parentDataNode )
        throws IOException
        /* This method does conversions of IDNumber nodes into
          MetaNodes by looking up the numbers in a FLAT text file.
          It should be called only if ( TheRwStructure == RwStructure.FLAT ).
          It returns the MetaNode equivalent from the state file
          of IDNumber TheIDNumber.
          It does this by finding and loading text of the unique MetaNode
          which has the same IDNumber value.
          As usual, DataNode parentDataNode is for name lookup during reading,
          but is ignored during writing.
          
          SkipFlagI is being used temporarily for testing.  ???
          */
        {
          IDNumber resultIDNumber= null;
          int DesiredI= inIDNumber.getTheI();
          Misc.DbgOut( "readAndConvertIDNumber("+DesiredI+") begin");  // Debug.
          int SkipFlagI= 0;  // 0 causes no skip.  Posotive causes skips.
          while (true) { // Search state file for desired MetaNode.
            resultIDNumber= 
              readWithWrapFlatMetaNode( parentDataNode, inIDNumber );
            if ( resultIDNumber == null )  // Exit if no MetaNode readable.
              break; 
            Misc.DbgOut( "readAndConvertIDNumber("+DesiredI+") read "+resultIDNumber.getTheI());  // Debug.
            if  // Exit if node found.
              ( ( SkipFlagI-- <= 0 ) &&  // Debug: force no find.
                ( DesiredI == resultIDNumber.getTheI() )
                )
              break;
            Misc.DbgOut( "readAndConvertIDNumber("+DesiredI+") loop");  // Debug.
            } // Search state file for desired MetaNode.
          if ( resultIDNumber == null ) // Did not find matching MetaNode.
            {
              Misc.DbgOut( "readAndConvertIDNumber(..) failed");  // Debug.
              resultIDNumber= inIDNumber;  // Return original IDNumber node.
              }
          Misc.DbgOut( "readAndConvertIDNumber("+DesiredI+") end");  // Debug.
          return resultIDNumber;
          }

      private IDNumber readWithWrapFlatMetaNode
        ( DataNode parentDataNode, IDNumber inIDNumber )
        /* This is like rwMetaNode(..) reading an assumed flat file,
          but if reading fails because an an end-of-file is encountered
          then it rewinds the file and tries one more time.
          */
        throws IOException
        {
          Misc.DbgOut( "readWithWrapFlatMetaNode(..) begin");  // Debug.
          IDNumber resultIDNumber= null;
          try {
            resultIDNumber=  // Try to...
              //MetaNode.rwMetaNode( // ...read a MetaNode.
              MetaNode.readParticularFlatMetaNode( // ...read flat MetaNode(s).
                this, inIDNumber, parentDataNode 
                );
            }
          catch ( Exception theException ) {  // Wrap if end of file.
            Misc.DbgOut( "readWithWrapFlatMetaNode(..) wrapping" );
            restoreStreamStateV( );  // Move stream state back to...
                                     // ...start of MetaNodes.
            resultIDNumber=  // Try again to...
              //MetaNode.rwMetaNode( // ...read a MetaNode.
              MetaNode.readParticularFlatMetaNode( // ...read flat MetaNode(s).
                this, inIDNumber, parentDataNode 
                );
            }
          Misc.DbgOut( "readWithWrapFlatMetaNode(..) end");  // Debug.
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
          */
        {
          theRandomAccessFile.seek(  // Move file pointer back to...
            savedFileOffsetLI  // ...previosly saved point.
            );
          indentLevelI= savedIndentLevelI; // Restore indent level.
          columnI= savedColumnI;  // Restore column.
          }

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
        /* If WritingB == true it writes the literal InString to the file 
          If WritingB == false it reads a literal String from the file and
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
            if ( WritingB )  // Writing state.
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
    
      private void DumpRemainder( ) throws IOException
        /* Dumps the remainder of the state file to System.out.
          This is mainly for debugging.
          */
        { // DumpRemainder( )
          int ByteI= theRandomAccessFile.read( );  // Try to read first byte.
          if ( ByteI != -1 ) // If success then output it and remainder.
            { // Output header and all file bytes.
              Misc.DbgOut( "DumpRemainder( ) ");  // Debug.
              System.out.print( // Introduce the data which will follow.
                "  Unread file bytes follow arrow ->" 
                );
              do { // Display bytes until done.
                System.out.print( (char)ByteI );  // Display the byte already read.
                ByteI= theRandomAccessFile.read( );  // Try to read next byte.
                } while ( ByteI != -1 ); // Display bytes until done.
              } // Output header and all file bytes.
          } // DumpRemainder( )

      public boolean getWritingB()
        { return WritingB; }

    } // class MetaFile.
    

class ShutdownHook extends Thread 
  {
    public void run() {
      MetaFile.finish();  // Write any changed state information.
      }
    }
