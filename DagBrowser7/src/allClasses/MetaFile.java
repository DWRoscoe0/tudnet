package allClasses;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

public class MetaFile 
  /* This class manages the file(s) that contains the external 
    representation of the MetaNode-s rooted at MetaRoot.  
    It reads from the file(s) after the application starts,
    and it writes to the file(s) before the application terminates.
	*/
  { // class MetaFile.
  
    private static RandomAccessFile TheRandomAccessFile= null;
    private static boolean SavingB;  // Direction of io, Saving vs. Loading.
    private static int indentLevelI; // = 0;  // Indent level of text in file.
    private static int columnI; // = 0;  // Column of io.
    
    public static MetaNode start( DataNode InRootDataNode )
      /* Initializes the MetaFile state manager.
        It loads [the root] MetaNode[s] from external file(s)
        that are associated with DataNode-s rooted at InRootDataNode.
        It also prepares to run finish() on shutdown to save MetaNode[s].
        It returns the root MetaNode gotten by loading, 
        or null if nothing was loaded.
        */
      { // start()
        System.out.println( "MetaFile.start()");
        MetaNode RootMetaNode= null;  // Set null root to indicate loading.
        try { // Load state.
          String StateFileString= "State.txt";  // Cache name of state file.
          if  //  Load state from file if...
            ( (new File( StateFileString )).exists() )  // ...the file exists.
            { //  Load state from file.
              TheRandomAccessFile=  // Open random access file.
                new RandomAccessFile( StateFileString, "r" );
              RootMetaNode= ioAllState( RootMetaNode );  // Load all state.
              DumpRemainder( );  // Output remainder for debugging.
              TheRandomAccessFile.close( );  // Close the input file.
              } //  Load state from file.
          } // Load state.
        catch ( IOException e ) {  // Process any errors.
          e.printStackTrace();
          }  // Process any errors.

        { // Prepare for finish() when app terminates.
          ShutdownHook shutdownHook = new ShutdownHook();
          Runtime.getRuntime().addShutdownHook(shutdownHook);
          } // Prepare for finish() when app terminates.

        return RootMetaNode;  // Return the root node for saving by caller.
        } // start()
  
    public static void finish()
      /* Finishes saving all new or modified MetaNode-s  
        to external file(s).  
        Presently it recursively saves the entire RootMetaNode tree.
        This should be called before application termination.  */
      { // finish()
        System.out.println( "\n\nMetaFile.finish()");
        
        try { // Try opening or creating file.
          TheRandomAccessFile=  // For open random access Outline file.
            new RandomAccessFile( "State.txt", "rw" );
          } // Try opening or creating file.
        catch (FileNotFoundException e) { // Handle any errors.
          e.printStackTrace();
          } // Handle any errors.

        if ( TheRandomAccessFile != null )
          try {
            //TheRandomAccessFile.writeBytes( "State-File\n" );
            MetaNode RootMetaNode= MetaRoot.getRootMetaNode( );  // Get root MetaNode.
            ioAllState( RootMetaNode );
            TheRandomAccessFile.setLength( // Truncate file at...
              TheRandomAccessFile.getFilePointer( )  // ...file pointer.
              );
            TheRandomAccessFile.close( );
            }
          catch ( IOException e ) {
            e.printStackTrace();
            } // Write some data to TheRandomAccessFile.
        } // finish()
  
    public static MetaNode ioAllState( MetaNode RootMetaNode )
      /* If RootMetaNode == null then all state is loaded from Meta file.
        If RootMetaNode != null then all state is saved to the Meta file.
        Returns the RootMetaNode, either the original, or a loaded one.
        */
      { // ioAllState( MetaNode RootMetaNode )
        indentLevelI= 0;  // Initialize indent level of text in file.
        columnI= 0;  // Initialize column of text in file.

        SavingB=  // Determine for this session the io direction based on...
          ( RootMetaNode != null);  // ...whether RootMetaNode is defined.

        ioLiteral(  // Begin file with identifying String.
          "Infogora-Meta-Data-File" 
          );
        RootMetaNode= MetaNode.io(  // Save or load... 
          RootMetaNode,  // ...the root MetaNode using...
          DataRoot.getParentOfRootDataNode()  // ...its parent for load lookups.
          );

        return RootMetaNode;  // Return the new or old root.
        } // ioAllState( MetaNode RootMetaNode )

    public static void outToken( String InTokenString )
      /* Outputs InTokenString using the appropriate delimiters.  */
      { // outToken(..)
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
        } // outToken(..)
  
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
  
    public static void ioIndentedWhiteSpace( )
      /* Goes to a new line if needed, and indents to the correct level.  */
      { 
        if // Go to a new line if...
          ( columnI > indentLevelI )  // ...past indent level.
          { // Go to a new line.
            ioLiteral( "\n" );  // Go to new line.
            columnI= 0;  // Reset to column 0.
            } // Go to a new line.
        while  // Add spaces to indent while...
          ( columnI < indentLevelI )  // ...column is less than indent level.
          if (ioLiteral( " " ))  // Try to add a single space.
            break;  // Exit loop if any error.
        }
  
    public static void ioListBegin( )
      /* Begins a section.  */
      { 
        ioIndentedLiteral( "(" );  // Go to new line.
        indentLevelI += 2;  // Increment indent level.
        }
  
    public static void ioListEnd( )
      /* Ends a section.  */
      { 
        ioIndentedLiteral( ")" );  // Output end token.
        indentLevelI -= 2;  // Decrement indent level.
        }
  
    public static void ioIndentedLiteral( String InString )
      /* Saves a string to the file, on a new line, 
        at correct indent level.  */
      { 
        ioIndentedWhiteSpace( );  // Save line and indent.
        ioLiteral( InString );  // Write string.
        }

    public static boolean ioLiteral( String InString )
      /* If SavingB == true it writes the literal InString to the file 
        If SavingB == false it reads a literal String from the file and
        verifies that it equals InString.
        It also handles IOException-s.  
        Literals can be or can include white space.
        It returns true is there is an error, false otherwise.
        The return value is used mainly to prevent infinite loops.
        */
      { // ioLiteral( String InString )
        boolean ErrorB= false;  // Assume no error.
        int InStringLengthI= InString.length();

        try {
          if ( SavingB )  // Writing state.
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
                    "\nioLiteral( '"+
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
        } // ioLiteral( String InString )

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
        System.out.print( "  Remainder of file follows arrow ->" );
        int ByteI;  // Place for bytes input.
        while (true) {  // Process entire remainder of file.
          ByteI= TheRandomAccessFile.read( );  // Read a byte.
          if (ByteI==-1) break;  // Exit loop if end-of-file.
          System.out.print( (char)ByteI );  // Display the byte.
          }
        } // DumpRemainder( )
    
    } // class MetaFile.
    

class ShutdownHook extends Thread 
  {
    public void run() {
      MetaFile.finish();  // Write any changed state information.
      }
    }
