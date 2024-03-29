package allClasses;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import static allClasses.AppLog.theAppLog;
import static allClasses.SystemSettings.NL;
 

public class MetaFile { // For app's meta-data files.

  /* This class helps to manage the file(s) that contains the external 
    representation of this app's DataNode meta-data.
    Related classes are MetaFileManager and MetaFileManager.Finisher.

    An instance of this class maintains state for one file,
    and provides some low level routines from which 
    file read and write operations can be built.

    ///enh Fix error handling.
    Errors in the external file meta-data text don't appear to cause crashes,
    but sometimes it can loss of non-corrupted meta-data,
    generally nodes that appear after the point of the error in the file.

    */

  // Injection instance variables.

    private MetaFileManager theMetaFileManager;

    private MetaFileManager.RwStructure theRwStructure;  // Text structure
      // of File.
    private String fileNameString;  // Name of associated external file.
    private String headerTokenString;  // First token in file.
    private MetaFileManager.Mode theMode;  // read/write/lazy-load.

  // Other instance variables.

    private RandomAccessFile theRandomAccessFile= // For file access.
      null;
    private int indentLevelI; // Indent level of cursor in text file.
    private int columnI;  // Column of cursor  in text file.
    private int linesI= 0; // Number of lines.

    /* Saved stream state.  This is used for rewinding the file 
      in lazy-load node searches.  */
      private long savedFileOffsetLI;  // Saved offset of MetaNodes.
      private int savedIndentLevelI; // Saved indent level.
      private int savedColumnI;  // Saved column.
      private int savedLinesI; // Save line #.

  public MetaFile( // Constructor.
      MetaFileManager theMetaFileManager,
      MetaFileManager.RwStructure theRwStructure, 
      String fileNameString, 
      String headerTokenString,
      MetaFileManager.Mode theMode
      )
    {
      this.theMetaFileManager= theMetaFileManager;
      this.theRwStructure= theRwStructure;
      this.fileNameString= fileNameString;
      this.headerTokenString= headerTokenString;
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
            ( (FileOps.makeRelativeToAppFolderFile( fileNameString )).exists() )  // ...the file exists.
            { //  Read state from file.
              theRandomAccessFile=  // Open random access file.
                new RandomAccessFile( 
                    FileOps.makeRelativeToAppFolderFile( fileNameString ), 
                 "r" 
                 );
              loadedMetaNode=   // Immediately read root node.
                rwFileMetaNode( loadedMetaNode );
              } //  Read state from file.
          } // Read state.
        catch ( IOException e ) {
          theAppLog.error(
              "lazyLoadFileMetaNode( ) closing theRandomAccessFile : "+e
              );
          }
        catch ( NumberFormatException e ) {
          theAppLog.error(
              "lazyLoadFileMetaNode( ) aborting load : "+e
              );
          }

        return loadedMetaNode;
        }

    public IDNumber readAndConvertIDNumber(
        IDNumber inIDNumber, DataNode parentDataNode
        )
      throws IOException
      /* This method searches for and loads a MetaNode whose ID number
       * is the same as the node referred to by inIDNumber.
       * This method works by searching the FLAT meta data text file
       * for text that represents a MetaNode with the same ID number.
       * parentDataNode is searched for the associated, same-named DataNode.
       * 
       * This method should be called only if loading from a FLAT file,
       * but it may be used for either lazy loading and greedy loading.
       * Loading can be fast if the load order matches the save order,
       * either Inorder, Preorder or Postorder.  
       * This app uses Preorder saving and loading.
       * 
       * If doing lazy loading then the returned MetaNode
       * has only children that are place-holder IDNumber objects
       * which can be evaluated later.
       * If doing greedy loading then the returned MetaNode 
       * has all its descendant MetaNodes loaded, evaluated and attached.
       * 
       * This method will search the entire FLAT metadata text file 
       * for desired MetaNodes.  The search wraps around to 
       * the beginning of the file if necessary.
       * A RepeatDetector class instance is used 
       * to limit the search to one complete scan of the file per MetaNode.
       * 
       * This method returns:
       * * a reference to a MetaNode with the same ID number as inIDNumber
       *   if one was found in the text file.
       *   One or more descendants might be IDNodes instead of MetaNodes,
       *   depending on loading type and loading errors.
       * * a reference to the original inIDNumber node, not a MetaNode, 
       *   if the ID number could not be found in the file.
       *   This is considered a loading error.
       */
      {
          RepeatDetector theRepeatDetector= new RepeatDetector();
          IDNumber resultIDNumber= null;
          int desiredI= inIDNumber.getTheI(); // Cache the ID #.
          //Misc.DbgOut( "MetaFile.readAndConvertIDNumber("+desiredI+") begin");  // Debug.
        goReturn: {
        goFail: {
          while (true) { // Searching state file for desired MetaNode.
            resultIDNumber=  // Setting tentative result to be next MetaNode.
              readFlatWithWrapMetaNode( inIDNumber, parentDataNode );
            if ( resultIDNumber == null )  // Exiting if no MetaNode readable.
              break goFail; 
            if  // Exiting if node with desired ID number found.
              ( desiredI == resultIDNumber.getTheI() )
              break goReturn;
            if  // Exiting if every node in file was checked once. 
              ( theRepeatDetector.repeatedB( resultIDNumber.getTheI() ) )
              { theAppLog.error(
                  "MetaFile.readAndConvertIDNumber(.) repeat detected.");
                break goFail;
                }
            } // while (true) Searching state file for desired MetaNode.
        } // goFail:
          resultIDNumber= inIDNumber;  // Return original IDNumber node.
        } // goReturn:
        return resultIDNumber;
        }

    private IDNumber readFlatWithWrapMetaNode( 
        IDNumber inIDNumber, DataNode parentDataNode
        )
      /* This method tries to one MetaNode from a FLAT metadata file,
       * if reading fails because an end-of-file is encountered
       * then it rewinds the file to the beginning and tries one more time.
       * inIDNumber contains the ID number of the MetaNode that is expected

         * parentDataNode is searched for the associated, same-named DataNode.
       * This method returns a reference to the MetaNode that it read,
       * or null if it was unable to read one.
       */
      throws IOException
      {
        //Misc.DbgOut( "MetaFile.readWithWrapFlatMetaNode(..) begin");  // Debug.
        IDNumber resultIDNumber= null;  // Set default result of not gotten.
        try {
          resultIDNumber=  // Try to read MetaNode.
            theMetaFileManager.readParticularFlatMetaNode( 
              this, inIDNumber, parentDataNode
              );
          }
        catch ( Exception theException ) { // Handle end of file.
          //Misc.DbgOut( "MetaFile.readWithWrapFlatMetaNode(..) wrapping" );
          restoreStreamStateV( );  // Rewind stream to beginning of MetaNodes.
          resultIDNumber=  // Try again to read same MetaNode.
            theMetaFileManager.readParticularFlatMetaNode(
              this, inIDNumber, parentDataNode
              ); // This is assumed to succeed.
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
        savedIndentLevelI= indentLevelI; // Save indent level.
        savedColumnI= columnI;  // Save column.
        savedLinesI= linesI; // Save line #.
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
          savedFileOffsetLI  // ...previously saved point.
          );
        indentLevelI= savedIndentLevelI; // Restore indent level.
        columnI= savedColumnI;  // Restore column.
        linesI= savedLinesI; // Restore line #.
        }
  
    public void closeV( )
      throws Exception
      /* This method closes the file.
        It must be called for lazilly loaded files because
        they are left open for extended periods of time
        because they are not read or written all at once.
        */
      {
        if ( theRandomAccessFile != null )
          Closeables.closeAndReportTimeUsedAndThrowExceptionsV(theRandomAccessFile);

        }

  // Method to read or write entire file, depending on context.

    private MetaNode rwFileMetaNode( MetaNode inRootMetaNode )
      throws IOException
      /* This method reads or writes the Meta state file,
        which is assumed to have already been opened.
        It will read or write depending on context,
        the values of theMode and inRootMetaNode.
        If Writing then inRootMetaNode provides the
        MetaNode which is the root of the Meta state to be output,
        unless inRootMetaNode is null which results in no operation.
        If Reading then inRootMetaNode is ignored.
        Returns the root MetaNode, either the one which was written
        or the one which was read.
        */
      {
        theAppLog.debug("MetaFile","MetaFile.rwFileMetaNode(.) begins, "
            +theMode+", "+getRwStructure());
        if // Do nothing or process depending on conditions.
          ( ( theMode == MetaFileManager.Mode.WRITING ) &&
            ( inRootMetaNode == null )
            ) 
          { // Do nothing because there is nothing to write.
            theAppLog.debug("MetaFile.rwFileMetaNode(.) null operation.");
            }
          else
          { // Read or write.
            indentLevelI= 0;  // Initialize indent level of text in file.
            columnI= 0;  // Initialize column of text in file.
            linesI= 0; // Initialize line number.

            rwLiteral( headerTokenString ); // Begin file with header token.
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

        theAppLog.debug("MetaFile","MetaFile.rwFileMetaNode(.) ends, linesI="+linesI);
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
            File inputFile= 
                FileOps.makeRelativeToAppFolderFile( fileNameString );
            File outFile= 
                FileOps.makeRelativeToAppFolderFile( fileNameString+".~" );

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
                Closeables.closeAndReportTimeUsedAndThrowExceptionsV(theRandomAccessFile);
                { // Replace input file by output file.
                  inputFile.delete();
                  outFile.renameTo(inputFile);
                  } // Replace input file by output file.
                } // Try writing all MetaNodes.
              catch ( Exception e ) { // Handle any exception.
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
        File FileNameFile= 
            FileOps.makeRelativeToAppFolderFile( fileNameString );
        try { // Read state.
          if  //  Read state from file if...
            ( FileNameFile.exists() )  // ...the file exists.
            { //  Read state from file.
              theRandomAccessFile=  // Open random access file.
                new RandomAccessFile( FileNameFile, "r" );
              loadedMetaNode= rwFileMetaNode( loadedMetaNode );  // Read all state.
              dumpRemainder( );  // Output any file remainder for debugging.
              Closeables.closeAndReportTimeUsedAndThrowExceptionsV(theRandomAccessFile); // Close the input file.
              } //  Read state from file.
          } // Read state.
        catch ( Exception e ) { // Process any exceptions.
          e.printStackTrace(); // Handle NumberFormatException or close error.
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
        String tokenString= "";  // Set token character accumulator to empty.
        try {
          long startingOffsetLI= // Save offset of beginning of token.
            theRandomAccessFile.getFilePointer();
          int byteI= theRandomAccessFile.read( );  // Try reading first byte.
          if ( (char)byteI == '\"') // Handle quoted string.
            while (true) { // Process entire token, if any.
              byteI= theRandomAccessFile.read( );  // Try reading token byte.
              if ( byteI == -1 || byteI == '\"' )  // End of token.
                break;  // Exit loop. 
              tokenString+= (char)byteI;  // Append byte to string.
              } // Process entire token, if any.
            else  // Handle white-space delimited string.
            while (true) { // Process entire token, if any.
              if ( byteI == -1 || byteI == ' ' || SystemSettings.NLTestB(byteI) )  // End of token.
                { // Back up file offset and exit.
                  theRandomAccessFile.seek( // Move file pointer back to...
                    startingOffsetLI + tokenString.length() ); // ...end of token.
                  break;  // Exit loop. 
                  } // Back up file offset and exit.
              tokenString+= (char)byteI;  // Append byte to string.
              byteI= theRandomAccessFile.read( );  // Try reading next byte.
              } // Process entire token, if any.
          columnI+= // Adjust columnI for file offset movement.
            ( theRandomAccessFile.getFilePointer() - startingOffsetLI );
          }
        catch ( IOException e ) {
          e.printStackTrace();
          }
        return tokenString.intern( );  // Return string or an older equal one.
        } // readTokenString( String InString )
  
    public void writeToken( String inTokenString )
      /* Outputs InTokenString using the appropriate delimiters.  */
      { // writeToken(..)
        int inStringLengthI= inTokenString.length();

        try {
          if ( inTokenString.indexOf( ' ' ) >= 0 )  // Token contains space.
            theRandomAccessFile.writeByte( '\"' );  // Write double-quote.
          { // Write litterl string.
            theRandomAccessFile.writeBytes( inTokenString );
            columnI+= inStringLengthI;  // Adjust columnI for string length.
            } // Write litterl string.
          if ( inTokenString.indexOf( ' ' ) >= 0 )  // Token contains space.
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
        linesI++; // Increment number of lines.
        if // Go to a new line if...
          ( columnI > indentLevelI )  // ...past indent level.
          { // Go to a new line.
            rwLiteral( NL );  // Go to new line.
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

    public boolean rwLiteral( String inString )
      /* If writing then it writes the literal InString to the file 
        If reading then it reads a literal String from the file and
        verifies that it equals InString.
        It also handles IOException-s.  
        Literals can be or can include white space.
        It returns true if there is an error, false otherwise.
        The return value is used mainly to prevent infinite loops.
        */
      { // rwLiteral( String InString )
        boolean errorB= false;  // Assume no error.
        int inStringLengthI= inString.length();

        try {
          if ( theMode == MetaFileManager.Mode.WRITING )  // Writing state.
            { // Write literal string.
              theRandomAccessFile.writeBytes( inString );
              columnI+= inStringLengthI;  // Adjust columnI for string length.
              } // Write litterl string.
            else  // Reading state.
            { // Read and verify String.
              if ( testLiteralB( inString ))
                {
                  theRandomAccessFile.skipBytes( inStringLengthI );
                  columnI+= inStringLengthI;  // Adjust columnI for string length.
                  }
                else
                {
                  errorB= true;  // Set error return value.
                  System.out.print( 
                    NL+"rwLiteral( '"+
                    inString+
                    "' ) MISMATCH!!!"
                    );
                  dumpRemainder( );  // Output anything that remains.
                  }
              } // Read and verify String.
          }
        catch ( IOException e ) {
          e.printStackTrace();
          }
        return errorB;  // Return error value.
        } // rwLiteral( String InString )

    public int testTerminatorI( String desiredString )
      /* Tests whether a terminator is next in the file.
        A terminator is either the literal String desiredString 
        or the EndOfFile.
        If DesiredString is there then it returns an int > 0.
        If EndOfFile is there then it returns an int < 0.
        If neither terminator is ther then it returns 0.
        In any case the stream position is unchanged.
        It also handles IOException-s.  */
      { // testTerminatorI( String DesiredString )
        int resultI= 1;  // Set default result to indicate terminator found.
        try {
          long startingOffsetLI= // Save offset of beginning of token.
            theRandomAccessFile.getFilePointer();
          int byteI;  // Place for bytes input.
          int indexI= 0; 
          while ( true ) // Process all characters if possible.
            { // Process one character or exit loop.
              if ( indexI >= desiredString.length() )  // String exhausted.
                break;  // Exit loop with ResultI indicating terminator-found.
              byteI= // Try reading a byte.
                theRandomAccessFile.read( );
              if ( byteI != desiredString.charAt( indexI ) )
                { // Exit loop with either string found or End-Of-File.
                  resultI= 0;  // Set result Indicating terminator-not-found.
                  if ( byteI < 0 ) // If End-Of-File encountered...
                    resultI= -1;  // ...override for End-Of-File result.
                  break;  // Exit loop.
                  } // Exit loop with either string found or End-Of-File.
              indexI++;  // Advance index.
              } // Process one character or exit loop.
          theRandomAccessFile.seek( // Move file pointer...
            startingOffsetLI );  // ... back to original position.
          }
        catch ( IOException e ) {
          e.printStackTrace();
          }
        return resultI;  // Return result calculated above.
        } // testTerminatorI( String DesiredString )
  
    public boolean testLiteralB( String desiredString )
      /* Tests whether the literal String desiredString 
        is next in the file.
        If desiredString is there then it returns true.
        If desiredString is not read then it returns false.
        In either case the stream position is unchanged.
        It also handles IOException-s.  */
      { // testLiteral( String DesiredString )
        return testTerminatorI( desiredString ) != 0;
        } // testLiteral( String DesiredString )

  // Miscellaneous instance methods.
  
    private void dumpRemainder( ) throws IOException
      /* This method is used to help debug MetaFile code.
        It dumps the remainder of the text file to System.out.
        If there is no remaining text then it outputs nothing.
        If there is remainder text then it outputs it enclosed
        inside of labeled arrows.
        This method can be called when there are file parsing errors,
        and when a file appears to be completely parsed.
        */
      { // DumpRemainder( )
        int byteI= theRandomAccessFile.read( );  // Try to read first byte.
        if ( byteI != -1 ) // If success then output it and remainder.
          { // Output header and all file bytes.
            //Misc.DbgOut( "MetaFile.DumpRemainder( ) ");  // Debug.
            System.out.print( // Introduce the data which will follow.
              "  Unread file bytes follow arrow ->" 
              );
            do { // Display bytes until done.
              System.out.print( (char)byteI );  // Display the byte already read.
              byteI= theRandomAccessFile.read( );  // Try to read next byte.
              } while ( byteI != -1 ); // Display bytes until done.
            System.out.print( // Introduce the data which will follow.
              "<- Unread file bytes precede the arrow." 
              );
            } // Output header and all file bytes.
        } // dumpRemainder( )

  // Getter instance methods for access to useful modes and values.
  
    public MetaFileManager.Mode getMode()
      { return theMode; }

    public MetaFileManager.RwStructure getRwStructure()
      { return theRwStructure; }

  } // class MetaFile.
