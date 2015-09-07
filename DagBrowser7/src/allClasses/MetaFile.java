package allClasses;

import static allClasses.Globals.appLogger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

//import static allClasses.Globals.*;  // For appLogger;


public class MetaFile { // For app's meta-data files.

  /* This class helps to manage the file(s) that contains the external 
    representation of this app's meta-data.
    Related classes are MetaFileManager and MetaFileManager.Finisher.

    An instance of this class maintains state for one file,
    and provides some low level routines from which 
    file read and write operations can be built.
    */

  // Injection instance variables.

    private MetaFileManager theMetaFileManager;

    private MetaFileManager.RwStructure TheRwStructure;  // File's text structure.
    private String FileNameString;  // Name of associated external file.
    private String HeaderTokenString;  // First token in file.
    private MetaFileManager.Mode theMode;  // read/write/lazy-load.

  // Other instance variables.

    private RandomAccessFile theRandomAccessFile= // For file access.
      null;
    private int indentLevelI; // Indent level of cursor in text file.
    private int columnI;  // Column of cursor  in text file.

    /* Saved stream state.  This is used for rewinding the file 
      in lazy-load node searches.  */
      private long savedFileOffsetLI;  // Saved offset of MetaNodes.
      private int savedIndentLevelI; // Saved indent level.
      private int savedColumnI;  // Saved column.

  public MetaFile( // Constructor.
      MetaFileManager theMetaFileManager,
      MetaFileManager.RwStructure theRwStructure, 
      String FileNameString, 
      String HeaderTokenString,
      MetaFileManager.Mode theMode
      ) 
    {
      this.theMetaFileManager= theMetaFileManager;
      this.TheRwStructure= theRwStructure;
      this.FileNameString= FileNameString;
      this.HeaderTokenString= HeaderTokenString;
      this.theMode= theMode;
      }

  // Instance methods related to lazy loading.

    public MetaNode lazyLoadFileMetaNode( )
      /* This is a helper method which finishes the job of
        reading and returning the root MetaNode from a Flat meta file.  
        It includes opening the RandomAccessFile stream,
        but not closing, unless there was an error,
        so that it can be used for lazy-loading
        other nodes after this method returns.
        If there was an error then it returns null.
        If there was no error then it returns the root MetaNode.
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
              } //  Read state from file.
          } // Read state.
        catch ( IOException e ) {
          appLogger.error(
              "lazyLoadFileMetaNode( ) closing theRandomAccessFile : "+e
              );
        	}
        catch ( NumberFormatException e ) {
          appLogger.error(
              "lazyLoadFileMetaNode( ) aborting load : "+e
              );
        	}

        return loadedMetaNode;
        }

    public IDNumber readAndConvertIDNumber( 
        IDNumber inIDNumber, DataNode parentDataNode
        )
      throws IOException
      /* This method is used in the loading of flat meta files,
        both lazy loading and greedy loading.
        It converts an IDNumber node into an equivalent MetaNode 
        by looking up the ID number in the FLAT text file
        and building a MetaNode from the text it finds there.
        It should be called only if ( TheRwStructure == RwStructure.FLAT ).

        It is called by special iterators that do lazy loading 
        by replacing IDNumber List elements by returned MetaNodes.
        It is also called by other load routines that do greedy loading.

        If doing lazy loading then only one MetaNode is returned
        with no children attached, but maybe other IDNumbers instances.
        If doing greedy loading then the MetaNode is returned
        with all its children and other descendants attached.

        A RepeatDetector is used to detect repeating nodes because
        the node reader has flag MetaFile wrap-around.

        parentDataNode is used for DataNode name lookup.
        */
      {
        RepeatDetector theRepeatDetector=  // Preparing repeat detector.
          new RepeatDetector();
        IDNumber resultIDNumber= null;
        int desiredI= inIDNumber.getTheI();
        //Misc.DbgOut( "MetaFile.readAndConvertIDNumber("+desiredI+") begin");  // Debug.
        while (true) { // Searching state file for desired MetaNode.
          resultIDNumber=  // Setting tentative result to be next MetaNode.
            readFlatWithWrapMetaNode( inIDNumber, parentDataNode 
            );
          if ( resultIDNumber == null )  // Exitting if no MetaNode readable.
            break; 
          if  // Exitting if node with desired ID number found.
            ( desiredI == resultIDNumber.getTheI() )
            { 
              break;
              }
          if  // Exitting if read ID numbers are repeating. 
            ( theRepeatDetector.testB( resultIDNumber.getTheI() ) ) 
            break;
          } // Search state file for desired MetaNode.
        if ( resultIDNumber == null ) // Did not find matching MetaNode.
          resultIDNumber= inIDNumber;  // Return original IDNumber node.
        return resultIDNumber;
        }

    private IDNumber readFlatWithWrapMetaNode( 
        IDNumber inIDNumber, DataNode parentDataNode
        )
      /* This is like rwMetaNode(..) except:
        * It must read from a FLAT file,
        * but if reading fails because an an end-of-file is encountered
          then it rewinds the file to the beginning of the MetaNodes
          and tries one more time.
        It returns a reference to the MetaNode that it read,
        or null if it was unable to read one.
        parentDataNode is used for DataNode name lookup.
        */
      throws IOException
      {
        //Misc.DbgOut( "MetaFile.readWithWrapFlatMetaNode(..) begin");  // Debug.
        IDNumber resultIDNumber= null;  // Set default result of not gotten.
        try {
          resultIDNumber=  // Try to...
            theMetaFileManager.readParticularFlatMetaNode( // ...read flat MetaNode(s).
              this, inIDNumber, parentDataNode
              );
          }
        catch ( Exception theException ) {  // Wrap if end of file.
          //Misc.DbgOut( "MetaFile.readWithWrapFlatMetaNode(..) wrapping" );
          restoreStreamStateV( );  // Rewind stream to beginning of MetaNodes.
          resultIDNumber=  // Try again to...
            theMetaFileManager.readParticularFlatMetaNode( // ...read flat MetaNode(s).
              this, inIDNumber, parentDataNode
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
  
    public void closeV( )
      throws IOException
      /* This method closes the file.
        It must be called for lazilly loaded files because
        they are left open for extended periods of time
        because they are not read or written all at once.
        */
      {
        if ( theRandomAccessFile != null )
        	theRandomAccessFile.close();
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
          ( ( theMode == MetaFileManager.Mode.WRITING ) &&
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
              theMetaFileManager.rwFlatOrNestedMetaNode(  // ...read or write... 
                this, // ...using this MetaFile...
                inRootMetaNode,  // ...of the root MetaNode using...
                theMetaFileManager.getTheDataRoot().
                  getParentOfRootDataNode() // ...parent for lookup.
                );
            } // Read or write process.

        return inRootMetaNode;  // Return the new or old root.
        }

  // Instance methods for reading and writing smaller pieces of meta file.

    public void writeRootedFileV( MetaNode inRootMetaNode )
      /* Writes all MetaNodes rooted at inRootMetaNode to inMetaFile.  
        This includes opening the file, writing the data, and closing.
        The format depends on context.
        If inMetaNode == null then it does nothing.
        Otherwise it creates or overwrites the appropriate file.
        */
      {
        if ( inRootMetaNode == null ) // There is NO MetaNode to process.
          ; // Do nothing.
          else // There IS a MetaNode to process.
          { // Write the data rooted at inMetaNode.
            File inputFile = AppFolders.resolveFile( FileNameString );
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
         }

    public MetaNode readFileMetaNode( )
      /* This method reads all MetaNodes from this MetaFile.  
        The format it expects depends on context.
        Returns the root MetaNode of what was read.  
        */
      {
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

    public String readTokenString( )
      /* Reads a token from file and returns it as a String.
        The token is assumed to begin at the current file position and
        be terminated by either a space or new-line,
        or be delimited by double-quotes.
        On return the file position is after the token,
        and before the delimiting white space, if any.
        It uses String.intern( ) on returned strings can be compared using 
        "==" and "!=".  

        This also handles IOException-s.  
        
        Maybe prevent empty result ??
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
          if ( theMode == MetaFileManager.Mode.WRITING )  // Writing state.
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

  // Miscellaneous instance methods.
  
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

  // Getter instance methods for access to useful modes and values.
  
    public MetaFileManager.Mode getMode()
      { return theMode; }

    public MetaFileManager.RwStructure getRwStructure()
      { return TheRwStructure; }

  } // class MetaFile.
    