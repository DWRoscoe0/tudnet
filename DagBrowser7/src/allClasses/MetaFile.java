package allClasses;

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
    private static int indentLevelI= 0;  // Indent level of text in file.
    private static int columnI= 0;  // Column of io.
    
    public static void start()
	  /* Starts reading [and writing] MetaNode-s from external file(s).
	    This should be called after application startup.  */
	  { // start()
    
      // Loading goes here.
      
      { // Do MetaFile start and prepare for finish.
        ShutdownHook shutdownHook = new ShutdownHook();
        Runtime.getRuntime().addShutdownHook(shutdownHook);
        } // Do MetaFile start and prepare for finish.
      System.out.println( "MetaFile.start()");
      // Read state information.
      } // start()
  
    public static void finish()
	  /* Finishes saving all new or modified MetaNode-s  
	    to external file(s).  
      Presently it recursively saves the entire RootMetaNode tree.
	    This should be called before application termination.  */
	  { // finish()
      System.out.println( "MetaFile.finish()");
      
      try { // Try creating file.
        TheRandomAccessFile=  // For open random access Outline file.
          new RandomAccessFile( "State.txt", "rw" );
        } // Try creating file.
      catch (FileNotFoundException e) { // Handle any errors.
        e.printStackTrace();
        } // Handle any errors.

      if ( TheRandomAccessFile != null )
        try {
          TheRandomAccessFile.writeBytes( "State-File\n" );
          MetaNode RootMetaNode= MetaRoot.GetRootMetaNode( );
          MetaNode.io( RootMetaNode );  // Save node (with descendents).
          TheRandomAccessFile.setLength( // Truncate file at...
            TheRandomAccessFile.getFilePointer( )  // ...file pointer.
            );
          TheRandomAccessFile.close( );
          }
        catch ( IOException e ) {
          e.printStackTrace();
          } // Write some data to TheRandomAccessFile.
      } // finish()
  
    public static void io( String InString )
	  /* Saves a string to the file with exception checking.  */
	  { 
      try {
        TheRandomAccessFile.writeBytes( InString );
        columnI+= InString.length();  // Add string length to Column.

        }
      catch ( IOException e ) {
        e.printStackTrace();
        }
      }
  
    public static void ioIndented( )
	  /* Saves a new line, and indents to the correct level.  */
	  { 
      if // Go to a new line if...
        ( columnI > indentLevelI )  // ...past indent level.
        { // Go to a new line.
          io( "\n" );  // Go to new line.
          columnI= 0;  // Reset to column 0.
          } // Go to a new line.
      while ( columnI < indentLevelI )  // Write spaces to indent.
        io( " " );  // Write a single space.
      }
  
    public static void ioIndentedField( String InString )
	  /* Saves a string to the file, on a new line, 
      at correct indent level.  */
	  { 
      ioIndented( );  // Save line and indent.
      io( InString );  // Write string.
      }
  
    public static void ioListBegin( )
	  /* Begins a section.  */
	  { 
    	ioIndentedField( "(" );  // Go to new line.
      indentLevelI += 2;  // Increment indent level.
      }
  
    public static void ioListEnd( )
	  /* Ends a section.  */
	  { 
    	ioIndentedField( ")" );  // Output end token.
      indentLevelI -= 2;  // Decrement indent level.
      }
  
    } // class MetaFile.

class ShutdownHook extends Thread 
  {
    public void run() {
      MetaFile.finish();  // Write any changed state information.
      }
    }
