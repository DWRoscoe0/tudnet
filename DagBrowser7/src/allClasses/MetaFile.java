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

    public enum RwMode {  // Mode of reading of writing to the state files.
      FLAT,
      HIERARCHICAL
      };
    public static RwMode TheRwMode;  // Whether Rw is split or not.

    private static final String FlatFileNameString= "Flat.txt";
    private static final String HierarchicalFileNameString= "Hierarchical.txt";
    private static String FileNameString;

    private static String HeaderTokenString;  // First string in file.

    private static boolean WritingB;  // true means Writing.  false means Reading.
    private static RandomAccessFile TheRandomAccessFile= null;
    private static int indentLevelI; // Indent level in text file.
    private static int columnI;  // Column of rw in text file.

    public static MetaNode start( DataNode InRootDataNode )
      /* Starts activity in this MetaFile class.
        It tries to read at least the root MetaNode from external file(s).
        It also prepares to run finish() at shutdown.
        It returns the root MetaNode gotten by reading, 
        or null if reading failed.
        */
      { // start()
        // System.out.println( "MetaFile.start()");

        // Set some appropriate mode variables.
        TheRwMode= RwMode.HIERARCHICAL;
        FileNameString= HierarchicalFileNameString;
        HeaderTokenString= "Infogora-Hierarchical-Meta-Data-File";

        MetaNode RootMetaNode= readAllStateMetaNode( );  // Do the actual read.

        { // Prepare to run finish() when app terminates.
          ShutdownHook shutdownHook = new ShutdownHook();
          Runtime.getRuntime().addShutdownHook(shutdownHook);
          } // Prepare to run finish() when app terminates.

        return RootMetaNode;  // Return the read root MetaNode, if any.
        } // start()

    private static MetaNode readAllStateMetaNode( )
      /* Read all MetaNodes, or at least the root one, from file.  
        Returns the root MetaNode read.  
        */
      {
        WritingB=  false;  // Indicate that we are reading.
        MetaNode RootMetaNode= null;  // Set null root because we are reading.

        try { // Read state.
          if  //  Read state from file if...
            ( (new File( HierarchicalFileNameString )).exists() )  // ...the file exists.
            { //  Read state from file.
              TheRandomAccessFile=  // Open random access file.
                new RandomAccessFile( HierarchicalFileNameString, "r" );
              RootMetaNode= rwAllStateMetaNode( RootMetaNode );  // Read all state.
              DumpRemainder( );  // Output remainder for debugging.
              TheRandomAccessFile.close( );  // Close the input file.
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
        Presently it writes all nodes, modified or now.
        Also presently it recursively writes the entire RootMetaNode tree.
        This should be called before application termination.  */
      { // finish()
        // System.out.println( "\n\nMetaFile.finish()");

        writeFlat( );  // Write in Flat text format.

        writeHierarchicalState( );  // Write in Hierarchical text format.

        } // finish()

    private static void writeFlat( )
      /* Writes all MetaNodes to Flat file.  */
      {
        // Set some appropriate mode variables.
        TheRwMode= RwMode.FLAT;
        FileNameString= FlatFileNameString;
        HeaderTokenString= "Infogora-Flat-Meta-Data-File";
        writeAllState( );  // Do the actual write in flat text format.
        }

    private static void writeHierarchicalState( )
      /* Writes all MetaNodes to Hierarchical file.  
        This will eventually exist only for debugging.
        */
      {
        // Set some appropriate mode variables.
        TheRwMode= RwMode.HIERARCHICAL;
        FileNameString= HierarchicalFileNameString;
        HeaderTokenString= "Infogora-Hierarchical-Meta-Data-File";
        writeAllState( );  // Do the actual write.
        }

    private static void writeAllState( )
      /* Writes all MetaNodes to file.  */
      { // writeAllState(()
        WritingB=  true;  // Indicate that we are writing.
        MetaNode RootMetaNode= MetaRoot.getRootMetaNode( );  // Get root MetaNode.

        try { // Try opening or creating file.
          TheRandomAccessFile=  // For open random access text file.
            new RandomAccessFile( FileNameString, "rw" );
          } // Try opening or creating file.
        catch (FileNotFoundException e) { // Handle any errors.
          e.printStackTrace();
          } // Handle any errors.
        if ( TheRandomAccessFile != null ) // Write if file was opened or created.
          try { // Try writing all MetaNodes.
            rwAllStateMetaNode( RootMetaNode );
            TheRandomAccessFile.setLength( // Truncate file at...
              TheRandomAccessFile.getFilePointer( )  // ...file pointer.
              );
            TheRandomAccessFile.close( );
            } // Try writing all MetaNodes.
          catch ( IOException e ) { // Handle any exception.
            e.printStackTrace();
            } // Handle any exception.
         } // writeAllState(()

    private static MetaNode rwAllStateMetaNode( MetaNode RootMetaNode )
      /* If RootMetaNode == null then all state is read from Meta file.
        If RootMetaNode != null then all state is written to the Meta file.
        Returns the RootMetaNode, either the original, or a read one.
        */
      {
        indentLevelI= 0;  // Initialize indent level of text in file.
        columnI= 0;  // Initialize column of text in file.

        rwLiteral( HeaderTokenString ); // Begin file with header token.
        RootMetaNode= MetaNode.rwMultiMetaNode(  // Read or write... 
          RootMetaNode,  // ...the root MetaNode using...
          DataRoot.getParentOfRootDataNode()  // ...its parent for read lookups.
          );

        return RootMetaNode;  // Return the new or old root.
        }

    public static void writeToken( String InTokenString )
      /* Outputs InTokenString using the appropriate delimiters.  */
      { // writeToken(..)
        int InStringLengthI= InTokenString.length();

        try {
          if ( InTokenString.indexOf( ' ' ) >= 0 )  // Token contains space.
            TheRandomAccessFile.writeByte( '\"' );  // Write double-quote.
          { // Write litterl string.
            TheRandomAccessFile.writeBytes( InTokenString );
            columnI+= InStringLengthI;  // Adjust columnI for string length.
            } // Write litterl string.
          if ( InTokenString.indexOf( ' ' ) >= 0 )  // Token contains space.
            {
              TheRandomAccessFile.writeByte( '\"' );  // Write double-quote.
              columnI+= 2; // Adjust columnI double-quotes.
              }
          }
        catch ( IOException e ) {
          e.printStackTrace();
          }
        } // writeToken(..)
  
    public static String readTokenString( )  // MAYBE PREVENT EMPTY RESULT???
      /* Reads a token from file and returns it as a String.
        The token is assumed to begin at the current file position and
        be terminated by either a space or new-line,
        or be delimited by double-quotes.
        On return the file position is after the token,
        and before the delimiting white space, if any.
        This also handles IOException-s.  */
      { // readTokenString( String InString )
        String TokenString= "";  // Token character accumulator initially empty.
        try {
          long StartingOffsetLI= // Save offset of beginning of token.
            TheRandomAccessFile.getFilePointer();
          int ByteI= TheRandomAccessFile.read( );  // Try reading the first byte.
          if ( (char)ByteI == '\"') // Handle quoted string.
            while (true) { // Process entire token, if any.
              ByteI= TheRandomAccessFile.read( );  // Try reading token byte.
              if ( ByteI == -1 || ByteI == '\"' )  // End of token.
                break;  // Exit loop. 
              TokenString+= (char)ByteI;  // Append byte to string.
              } // Process entire token, if any.
            else  // Handle white-space delimited string.
            while (true) { // Process entire token, if any.
              if ( ByteI == -1 || ByteI == ' ' || ByteI == '\n' )  // End of token.
                { // Back up file offset and exit.
                  TheRandomAccessFile.seek( // Move file pointer back to...
                    StartingOffsetLI + TokenString.length() ); // ...end of token.
                  break;  // Exit loop. 
                  } // Back up file offset and exit.
              TokenString+= (char)ByteI;  // Append byte to string.
              ByteI= TheRandomAccessFile.read( );  // Try reading next byte.
              } // Process entire token, if any.
          columnI+= // Adjust columnI for file offset movement.
            ( TheRandomAccessFile.getFilePointer() - StartingOffsetLI );
          }
        catch ( IOException e ) {
          e.printStackTrace();
          }
        return TokenString.intern( );  // Return string or an older equal one.
        } // readTokenString( String InString )
  
    public static void rwIndentedWhiteSpace( )
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
  
    public static void rwListBegin( )
      /* Begins a section.  */
      { 
        rwIndentedLiteral( "(" );  // Go to new line.
        indentLevelI += 2;  // Increment indent level.
        }
  
    public static void rwListEnd( )
      /* Ends a section.  */
      { 
        rwIndentedLiteral( ")" );  // Output end token.
        indentLevelI -= 2;  // Decrement indent level.
        }
  
    public static void rwIndentedLiteral( String InString )
      /* rw-processes a string in the file, on a new line, 
        at correct indent level.  */
      { 
        rwIndentedWhiteSpace( );  // rw-process line and indent.
        rwLiteral( InString );  // rw-process string.
        }

    public static boolean rwLiteral( String InString )
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
            { // Write litterl string.
              TheRandomAccessFile.writeBytes( InString );
              columnI+= InStringLengthI;  // Adjust columnI for string length.
              } // Write litterl string.
            else  // Reading state.
            { // Read and verify String.
              if ( testLiteralB( InString ))
                {
                  TheRandomAccessFile.skipBytes( InStringLengthI );
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

    public static int testTerminatorI( String DesiredString )
      /* Tests whether a terminator is is next in the file.
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
            TheRandomAccessFile.getFilePointer();
          int ByteI;  // Place for bytes input.
          int IndexI= 0; 
          while ( true ) // Process all characters if possible.
            { // Process one character or exit loop.
              if ( IndexI >= DesiredString.length() )  // String exhausted.
                break;  // Exit loop with ResultI indicating terminator-found.
              ByteI= // Try reading a byte.
                TheRandomAccessFile.read( );
              if ( ByteI != DesiredString.charAt( IndexI ) )
                { // Exit loop with either string found or End-Of-File.
                  ResultI= 0;  // Set result Indicating terminator-not-found.
                  if ( ByteI < 0 ) // If End-Of-File encountered...
                    ResultI= -1;  // ...override for End-Of-File result.
                  break;  // Exit loop.
                  } // Exit loop with either string found or End-Of-File.
              IndexI++;  // Advance index.
              } // Process one character or exit loop.
          TheRandomAccessFile.seek( // Move file pointer...
            StartingOffsetLI );  // ... back to original position.
          }
        catch ( IOException e ) {
          e.printStackTrace();
          }
        return ResultI;  // Return result calculated above.
        } // testTerminatorI( String DesiredString )

    public static boolean testLiteralB( String DesiredString )
      /* Tests whether the literal String DesiredString 
        is next in the file.
        If DesiredString is there then it returns true.
        If DesiredString is not read then it returns false.
        In either case the stream position is unchanged.
        It also handles IOException-s.  */
      { // testLiteral( String DesiredString )
        return testTerminatorI( DesiredString ) != 0;
        } // testLiteral( String DesiredString )
  
    private static void DumpRemainder( ) throws IOException
      /* Dumps the remainder of the state file to System.out.
        This is mainly for debugging.
        */
      { // DumpRemainder( )
        int ByteI= TheRandomAccessFile.read( );  // Try to read first byte.
        if ( ByteI != -1 ) // If success then output it and remainder.
          { // Output header and all file bytes.
            System.out.print( // Introduce the data which will follow.
              "  Unread file bytes follow arrow ->" 
              );
            do { // Display bytes until done.
              System.out.print( (char)ByteI );  // Display the byte already read.
              ByteI= TheRandomAccessFile.read( );  // Try to read next byte.
              } while ( ByteI != -1 ); // Display bytes until done.
            } // Output header and all file bytes.
        } // DumpRemainder( )
    
    } // class MetaFile.
    

class ShutdownHook extends Thread 
  {
    public void run() {
      MetaFile.finish();  // Write any changed state information.
      }
    }
