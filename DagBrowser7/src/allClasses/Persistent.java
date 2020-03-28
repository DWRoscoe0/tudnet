package allClasses;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;

import static allClasses.AppLog.theAppLog;
import static allClasses.SystemSettings.NL;


public class Persistent 

	/* This class implements persistent data for an app.  It is stored in 2 ways:

	  * It is stored in main memory as a tree of EpiNode instances

    * It is stored on non-volatile external storage as a text file.
      EpiNode data is expressed in a YAML subset.

    Each node can represent either a:
    * scalar value, a simple text string, or
    * a nested map of key-value pairs, with each value being another node.
    
	  Unlike EpiNode, which does not understand paths within a tree, this class does.
	  Unfortunately, this means that some operations,
	  the operations that involve long paths, can be slow.
	  Methods that do these long operations generally begin with "multilevel".
	  The internal use of paths to identify persistent data is being deprecated. 
	  If many repeated operations are to be done, deep within the structure,
	  at the end of a long path, then either
	  * data should be referenced through a PersistentCursor, or
	  * a reference should be gotten to the containing data node
	  ///opt Paths used internally might eventually be eliminated.

		A path can be:
		* relative to a given map node, or
		* absolute, meaning relative to the root map node.

		A path expressed as a String is a list of elements separated by a "/".
		A path can contain 0, 1, 2, or more elements.

	  In earlier versions of this class, "key" was synonymous with "full path".
	  Now "key" is only a single element of a path.
	  
	  A path does not end in a slash.  A prefix ends in a slash.
	  
	 	*/
	
	{

		private MapEpiNode rootMapEpiNode= null; // EpiNode root of tree data.

  	
  	// Initialization methods.
  	
	  public void initializeV()
	    /* This method loads the persistent data from an external file.
	      It also does some temporary data conversions and extra file outputs
	      as part of the conversion from PersistentNode data to EpiNode data. 
	     
	      It is possible to eliminate this method, and trigger things by lazy loading,
	      triggered by the first call that needs theMap variable defined.
	      But because finalizeV() must be called to write any changes,
	      we might as well just call initializeV() as well.
	      */
	    {
        rootMapEpiNode=  // Translate text file data to EpiNode data.
          loadMapEpiNode("PersistentEpiNode.txt");
        if (rootMapEpiNode == null) // Define root map to be empty map if load failed. 
          rootMapEpiNode= new MapEpiNode();
	  	  }
	  
    private MapEpiNode loadMapEpiNode( String fileString )
      /* This method creates an EpiNode from 
        the contents of the external text file whose name is fileString.  
        */
      {
        MapEpiNode resultMapEpiNode= null; 

        RandomAccessInputStream theRandomAccessInputStream= null;
        RandomAccessFile theRandomAccessFile= null;

        try { 
            theRandomAccessFile= new RandomAccessFile(
                AppSettings.makeRelativeToAppFolderFile( fileString ),"r");
            theRandomAccessInputStream= 
                new RandomFileInputStream(theRandomAccessFile);
            resultMapEpiNode= MapEpiNode.tryBlockMapEpiNode(theRandomAccessInputStream, 0 );
            } 
          catch (FileNotFoundException theFileNotFoundException) { 
            theAppLog.warning("Persistent.loadEpiNodeDataV(..)"+theFileNotFoundException);
            }
          catch (Exception theException) { 
            theAppLog.exception("Persistent.loadEpiNodeDataV(..)", theException);
            }
          finally { 
            try { 
              if ( theRandomAccessInputStream != null ) theRandomAccessInputStream.close(); 
              if ( theRandomAccessFile != null ) theRandomAccessFile.close(); 
              }
            catch (Exception theException) { 
              theAppLog.exception("Persistent.loadEpiNodeDataV(..)", theException);
              }
            }
        return resultMapEpiNode; 
        }


    // Service methods for get and put operations.
	  
	  public void putV( String keyString, String valueString )
	    /* This associates valueString with keyString.
	      keyString should not be a multiple element path, 
	      though in an earlier version it could be.
	     	*/
	    {
	      rootMapEpiNode.putV(keyString, valueString);
		  	}
	
	  public String getDefaultingToBlankString( String pathString )
		  /* Returns the value String associated with pathString,
		    or the empty string if there is none.
		   	*/
		  {
				return getString( pathString, "" );
			  }
		
	  private String getString( String pathString, String defaultValueString )
			/* Returns the value String associated with pathString,
		    or defaultValueString if there is no value String stored.
		    It does not try to interpret path separator characters in the key.
		    It does a single-level lookup only.
		   	*/
		  {
  			String childValueString= getString(pathString); 
	  	  if (childValueString == null) 
	  	  	childValueString= defaultValueString;
				return childValueString;
		  }
		
	  private String getString( String pathString)
			/* This is like getEpiNode(pathString) except that
			  instead of returning an EpiNode,
			  it returns the value String stored there.
			  If either the node or the value String are not at
			  the location specified by pathString
			  then null is returned.
		   	*/
		  {
  	      String resultValueString= null; // Default null result value, to be overridden.
        goReturn: {
          EpiNode keyEpiNode= new ScalarEpiNode(pathString);
          EpiNode valueEpiNode= rootMapEpiNode.getEpiNode(keyEpiNode);
          if (valueEpiNode == null) // If there is no node with this key
            break goReturn; // return with default null String.
          resultValueString= valueEpiNode.toString(); // Get node's string value. 
        } // goReturn:
	  			return resultValueString;
		  }

    public MapEpiNode getOrMakeMapEpiNode(String pathString)
      /* This is equivalent to
    
        getOrMakeMapEpiNode(baseMapEpiNode, pathString)
    
        with baseMapEpiNode set to rootMapEpiNode.
        
        Note, dealing with absolute paths, paths starting at rootMapEpiNode.
        So this method should be used infrequently. 
        */
      {
        return getOrMakeMapEpiNode(
          rootMapEpiNode, pathString);
        }

    private MapEpiNode getOrMakeMapEpiNode(
        MapEpiNode baseMapEpiNode, String pathString)
      /* Returns the MapEpiNodeNode associated with pathString.
        If there is none, then it makes one, along with 
        all the other MapEpiNodeNodes between it and baseMapEpiNodeNode. 
        It interprets pathString as a path from baseMapEpiNodeNode
        to the desired MapEpiNodeNode.
        Each path element is used as a key to select or create
        the next child in the MapEpiNodeNode hierarchy.
        It does one key lookup, or new node creation,
        for every element of the path.
        An empty pathString is interpreted to mean baseMapEpiNodeNode.
        This method never returns null.
        It returns a null if there is an error parsing pathString.
        
        ///opt Though this method accepts a path, it appears to be called with
          only single-element paths.
        */
      {
          // appLogger.debug(
          //    "Persistent.getOrMakeMapEpiNodeNode("
          //      +pathString+") begins.");
          MapEpiNode resultMapEpiNode= // Initial result value
              baseMapEpiNode; // is base node.
        goReturn: {
          int separatorKeyOffsetI; // Offset of next key/path separator. 
        goLogError: {
          if (pathString.isEmpty()) break goReturn; // return base node.
          int scanKeyOffsetI= 0; // Starting offset for path separator search.
          while (true) { // Get/make child nodes until desired one is reached.
            separatorKeyOffsetI= // Get offset of next key/path separator. 
                pathString.indexOf(Config.pathSeperatorC, scanKeyOffsetI);
            if (separatorKeyOffsetI < 0) // There is no next path separator...
              { // So next node is final node.  Return appropriate child node.
                String keyString= // Extract final key from path.
                    pathString.substring(scanKeyOffsetI, pathString.length());
                if (keyString.isEmpty()) break goLogError;
                resultMapEpiNode= // Get or make associated child node.
                  resultMapEpiNode.getOrMakeMapEpiNode(keyString);
                break goReturn; // Return with the non-null value.
                }
            //// This code does not appear to be reached.
            String keyString= // Extract next key from path up to separator.
                pathString.substring(scanKeyOffsetI, separatorKeyOffsetI);
            if (keyString.isEmpty()) break goLogError;
            resultMapEpiNode= // Get or make associated/next child node.
                resultMapEpiNode.getOrMakeMapEpiNode(keyString);
            scanKeyOffsetI= separatorKeyOffsetI+1; // Compute next key offset.
          } // while (true)... Loop to select or make next descendant node.
        } // goLogError:
          String errorString= "Persistent.getMapEpiNodeNode(..), "
              +"error getting value, path="+pathString;
          theAppLog.error(errorString); // Log error string.
          resultMapEpiNode= null; // and return null.
        } // goReturn:
          return resultMapEpiNode;
        }


	  // Finalization methods not called by initialization or service sections.
	  
	  public void finalizeV()
	  /* This method stores the persistent data to the external file.
	    Temporarily it stores both PersistentNode data and EpiNode data.
	    Eventually it will store only one.
	    */
	  {
      storeEpiNodeDataV(rootMapEpiNode, "PersistentEpiNode.txt"); // Write EpiNode data.
	  	}
  
    private void storeEpiNodeDataV( EpiNode theEpiNode, String fileString )
      /* This method stores the Persistent data that is in main memory to 
        the external text file whose name is fileString.
        Presently it stores twice:
        * The data only in the root node.
        * The data in all nodes of the tree.
        So some information appears twice in the file,
        but when reloaded it should go to the same place.
        
        The exception handling in this method is not required,
        but it does no harm.
        */
      {
        theAppLog.info("Persistent.storeEpiNodeDataV(..) begins.");
        FileOutputStream theFileOutputStream= null;
        try {
            theFileOutputStream= 
              new FileOutputStream(AppSettings.makeRelativeToAppFolderFile(fileString));  
            theFileOutputStream.write(
                "#---YAML-like EpiNode data output follows---".getBytes());
              theEpiNode.writeV(theFileOutputStream, // Write all of theEpiNode tree 
                0 // starting at indent level 0.
                );
            theFileOutputStream.write(
                (NL+"#--- end of file ---"+NL).getBytes());
            }
          catch (Exception theException) { 
            theAppLog.exception("Persistent.storeEpiNodeDataV(..)", theException);
            }
          finally {
            try {
              if ( theFileOutputStream != null ) theFileOutputStream.close(); 
              }
            catch ( Exception theException ) { 
              theAppLog.exception("Persistent.storeEpiNodeDataV(..)", theException);
              }
            }
        theAppLog.info("Persistent.storeEpiNodeDataV(..) ends.");
        }
	
	  public void writingCommentLineV( 
	      PrintWriter thePrintWriter, String commentString ) 
	  	throws IOException
	    /* This method writes theString followed by a newline
	      to the text file OutputStream.
	      */
	    {
        thePrintWriter.print('#'); // Write comment character.
	  		writingLineV(thePrintWriter, commentString); // Write comment content as line.
	      }
	
	  public void writingLineV( PrintWriter thePrintWriter, String lineString ) 
	    /* This method writes theString followed by a newline
	      to the text file OutputStream.
	      */
	    {
	  		writingV(thePrintWriter, lineString); // Write string.
        thePrintWriter.println(); // Terminate line.
	      }
	
	  public void writingV( PrintWriter thePrintWriter, String theString ) 
	    // This method writes theString to the text file OutputStream.
	    {
	  		thePrintWriter.print(theString);
	      }
	  
		}
