package allClasses;

import static allClasses.Globals.appLogger;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.NavigableMap;

public class Persistent 

	/* This class implements persistent data for an app.  It is stored in 2 ways:
	  * It is stored on non-volatile external storage as a text file.
	  * It is stored in main memory as a tree of PersistingNode instances.

	  Unlike PersistingNode, which does not understand 
	  paths within a tree, this class does.
	  Unfortunately, this means that some operations,
	  the operations that involve long paths, can be slow.
	  Methods that do these long operations generally begin with "multilevel".
	  If many repeated operations are to be done, deep within the structure,
	  at the end of a long path, then a PersistentCursor should be used instead.

		A path can be:
		* relative to a given PersistingNode, or
		* absolute, meaning relative to the root PersistingNode.

		A path expressed as a String is a list of elements separated by a "/".
		A path can contain 0, 1, 2, or more elements.

	  In earlier versions of this class, "key" was synonymous with "full path".
	  Now "key" is only a single element of a path.
	  
	  A path does not end in a slash.  A prefix ends in a slash.
	  
	 	*/
	
	{
	
		private final String theFileString= "PersistentData.txt";
		  // This is where the data is stored on disk.
		private PersistingNode rootPersistingNode= null; // Root of tree.
    private PrintWriter thePrintWriter= null; ////
      ///org : This class uses a FileInputStream to read the data file,
      // but a PrintWriter to write the data file.
      // For symmetry, eventually switch to using a Scanner for reading. 

  	
  	// Initialization methods.
  	
	  public void initializeV()
	    /* This method loads the persistent data from an external file.
	      It is possible to eliminate this method, and triggered by lazy loading,
	      triggered by the first call that needs theMap variable defined.
	      But because finalizeV() must be called to write any changes,
	      we might as well just call initializeV() as well.
	      */
	    {
	  	  loadDataV( theFileString );
	      pollerPersistent= this;
	  	  }

    private static Persistent pollerPersistent= null;
    private static int oldSizeI= -1;
    public static void debugPollerV()
      /* This method is a debugging method for looking for 
        particular Persistent state changes and log them.
        */
      {
        if (pollerPersistent == null) return;
        PersistentCursor thePersistentCursor= // Used for iteration. 
            new PersistentCursor( pollerPersistent );
        thePersistentCursor.setListV("peers"); // Point to peer list.
        NavigableMap<String, PersistingNode> theNavigableMap= 
            thePersistentCursor.getNavigableMap();
        int newSizeI= theNavigableMap.size();
        if (oldSizeI != newSizeI) {
          appLogger.debug("Persistent.debugPollerV(), oldSizeI="+ oldSizeI + 
              ", newSizeI=" + newSizeI);
          oldSizeI= newSizeI;
          }
        }
	  
	  private void loadDataV( String fileString )
	    /* This method creates a Map 
	      from the contents of the external text file
	      whose name is fileString and stores it in theMap field.  
	      If no properties were read then the result will be empty.
	      If some properties were read then the result will contain them.
	      The result will never be null.
	      */
	    {
	  		rootPersistingNode= 
	  				new PersistingNode("this-is-root"); // Create empty Map. 

	  		FileInputStream configFileInputStream= null;

		  	try { 
		  		  configFileInputStream= // Prepare file containing persistent data.
		  		  		new FileInputStream(
		  		  		    AppSettings.makeRelativeToAppFolderFile( fileString ));
		  		  loadV(configFileInputStream); // Load data from file.
			  		} 
			  	catch (FileNotFoundException theFileNotFoundException) { 
			  		appLogger.warning("Persistent.loadDataV(..)"+theFileNotFoundException);
			  		}
			  	catch (Exception theException) { 
			  		appLogger.exception("Persistent.loadDataV(..)", theException);
			  		}
			  	finally { 
			  		try { 
			  			if ( configFileInputStream != null ) configFileInputStream.close(); 
			  			}
				  	catch (Exception theException) { 
				  		appLogger.exception("Persistent.loadDataV(..)", theException);
				  		}
			  		}
	    	}

	  private void loadV( FileInputStream configFileInputStream )
	    throws IOException
	    {
	  	  byte[] lineAB= new byte[1024];
	  	  int CI= configFileInputStream.read();
		  	while (true) { // Process all lines.
		  		if (CI==-1) break; // Exit loop if end of file.
		  		int offsetI= 0;
		  	  while (true) // Read, convert, and store a line in character array.
		  	  	{
		  	  	  if  // Exit loop if end of line character, or end of file.
		  	  	    ((CI=='\n') || (CI=='\r') || (CI==-1)) 
		  	  	  	break;
		  	  	  lineAB[offsetI++]= (byte)(0xff & CI); // Store converted byte.
		  		  	CI= configFileInputStream.read(); // Read next byte.
		  	  		}
		  	  String lineString= // Convert line char array to a String.
		  	  		new String(lineAB, 0, offsetI);
		  	  loadOrIgnoreLineV(lineString); // Process line appropriately.
  		  	CI= configFileInputStream.read(); // Read next byte.
		  		}
	    	}

	  private void loadOrIgnoreLineV(String lineString)
	    /* Reads lineString and saves any key-value entry present.  
	      */
		  { 
	  		toReturn: {
	  		toFail: {
	  	    if (lineString.isEmpty())  // Empty line. 
	  	    	break toReturn; // Ignore with success.
	  	    if (lineString.charAt(0) == '#') // Comment line.
	  	    	break toReturn; // Ignore with success.
	  	    int offsetOfEqualsI= lineString.indexOf('=', 0); // First '='.
	  	    if (offsetOfEqualsI < 1) // '=' absent or at beginning of line.
	  	    	break toFail; // Fail.
	  	    // All is well.  Extract key and value and save them.
	  	    loadParsedLineV(lineString,offsetOfEqualsI);
	  	    break toReturn; // Return with success.
		  	} // goFail:
		  		appLogger.warning("processLineV(..) failed, line=\""+lineString+"\"");
		  	} // goReturn:
		  }

    private void loadParsedLineV(String lineString, int offsetOfEqualsI)
      /* Processes a line which has already been determined to be correct.
        offsetOfEqualsI of the offset of the "=" within the line lineString.
        
        This method has been switched to store into 
        the multilevel Persistent structure.
        Previously it stored only in the rootPersistingNode,
        with path separators in the keyString if they were there.
        */
	    {
    	  // Extract key path and value.
		    String keyPathString= lineString.substring(0, offsetOfEqualsI);
		    String valueString= lineString.substring(offsetOfEqualsI+1);

		    // Store value in the appropriate node based on the path.
		    putB(keyPathString,valueString);
		    }


    // Service methods for get and put operations.
	  
	  public boolean putB( String pathString, String valueString )
	    /* This is like

	       		putB(thePersistingNode,keyString,valueString)

	      but with thePersistingNode set to the rootPersistingNode. 
	      
	      Note, dealing with absolute paths, paths starting at rootPersistingNode.
	      So this method should be used infrequently. 
	     	*/
	    {
		  	return putB(rootPersistingNode,pathString,valueString);
		    }

	  private boolean putB(
	  		PersistingNode thePersistingNode,String pathString,String valueString)
	    /* This recursive method stores valueString into thePersistingNode
	      or one of its descendants, based on pathString.
	      It uses recursion to reach or create the needed descendants.
	      If pathString contains no path separator then 
	      the entire pathString is used to get or create
	      the child node into which valueString is stored directly.
	      If pathString contains a path separator then 
	      the part before the separator is used to get or create
	      the child node into which valueString is stored
	      by calling this method recursively on that child node
	      using the remainder of pathString as the new pathString.
	      If the key or any of its parts is empty then an error is logged and 
	      true is returned, otherwise false is returned.
	      Actually, I doubt the return value is used much, if at all.
	      
	      ///opt Could this be converted to not recurse?
	     	*/
	    {
	  			boolean resultB= false; // Set default return value to be success.
	  		toReturn: {
	  		toFail: {
			    int offsetOfSeparatorI= // Offset of first key/path separator. 
			    		pathString.indexOf(Config.pathSeperatorC, 0);
			    if (offsetOfSeparatorI < 0) // There is no path separator in key.
				    { // Store value in this node's child for this key, return success.
				    	thePersistingNode.putV(pathString, valueString);
					    break toReturn;
				    	}
			    if (offsetOfSeparatorI < 1) // Separator is at beginning of key.
				    break toFail; // Key head would be empty.
			    String keyHeadString= pathString.substring(0, offsetOfSeparatorI);
			    if (offsetOfSeparatorI >= pathString.length()) // Separator is at end.
				    break toFail; // Key tail would be empty.
			    String keyTailString= pathString.substring(offsetOfSeparatorI+1);
	    		PersistingNode childPersistingNode= // Get or make appropriate child. 
		    			thePersistingNode.getOrMakePersistingNode(keyHeadString);
	    		resultB= // Recurse using tail of key as new key.
	    				putB(
	    						childPersistingNode,keyTailString,valueString);
	    		break toReturn;
		  	} // toFail:
		  		appLogger.warning("multiSetValueB(..) failed.");
		  		resultB= true;  // Set failure return value
		  	} // toReturn:
	  			return resultB;
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
			/* This is like getPersistingNode(pathString) except that
			  instead of returning a PersistingNode,
			  it returns the value String stored there.
			  If either the node or the value String are not at
			  the location specified by pathString
			  then null is returned.
		   	*/
		  {
	  			String resultValueString= null; // Default null result value.
	  	  goReturn: {
    			PersistingNode valuePersistingNode= // Get associated PersistingNode. 
    					getPersistingNode(pathString);
		  	  if (valuePersistingNode == null) // If there is no node with this path
		  	  	break goReturn; // return with default null String.
		  	  resultValueString= // Get possibly null string value from this node. 
		  	  		valuePersistingNode.getString();
		  	} // goReturn:
	  			return resultValueString;
		  }

	  public PersistingNode getOrMakePersistingNode(String pathString)
			/* This is equivalent to
		
		  	getOrMakePersistingNode(basePersistingNode, pathString)
		
				with basePersistingNode set to rootPersistingNode.
	      
	      Note, dealing with absolute paths, paths starting at rootPersistingNode.
	      So this method should be used infrequently. 
				*/
		  {
	  		return getOrMakePersistingNode(
		  		rootPersistingNode, pathString);
	  		}
	  
	  private PersistingNode getOrMakePersistingNode(
	  		PersistingNode basePersistingNode, String pathString)
			/* Returns the PersistingNode associated with pathString.
			  If there is none, then it makes one, along with 
			  all the other PersistingNodes between it and basePersistingNode. 
			  It interprets pathString as a path from basePersistingNode
		    to the desired PersistingNode.
		    Each path element is used as a key to select or create
		    the next child in the PersistingNode hierarchy.
		    It does one key lookup, or new node creation,
		    for every element of the path.
		    An empty pathString is interpreted to mean basePersistingNode.
		    This method never returns null.
		    It returns a new PersistingNode whose value is an error String
		    if there is an error parsing pathString.
		   	*/
		  {
				// appLogger.debug(
				// 		"Persistent.getOrMakePersistingNode("
				// 			+pathString+") begins.");
  			PersistingNode resultPersistingNode= // Initial result value
  					basePersistingNode; // is base node.
  	  goReturn: {
		    int separatorKeyOffsetI; // Offset of next key/path separator. 
		  goLogError: {
    	  if (pathString.isEmpty()) break goReturn; // return base node.
			  int scanKeyOffsetI= 0; // Starting offset for path separator search.
			  while (true) { // Get/make child nodes until desired one is reached.
			    separatorKeyOffsetI= // Get offset of next key/path separator. 
			    		pathString.indexOf(Config.pathSeperatorC, scanKeyOffsetI);
			    if (separatorKeyOffsetI < 0) // There is no next path separator...
			    	{	// So next node is final node.  Return appropriate child node.
			    	  String keyString= // Extract final key from path.
			    	  		pathString.substring(scanKeyOffsetI, pathString.length());
			    	  if (keyString.isEmpty()) break goLogError;
			    	  resultPersistingNode= // Get or make associated child node.
			    	  	resultPersistingNode.getOrMakePersistingNode(keyString);
				  	  break goReturn; // Return with the non-null value.
			    		}
			    String keyString= // Extract next key from path up to separator.
	    	  		pathString.substring(scanKeyOffsetI, separatorKeyOffsetI);
	    	  if (keyString.isEmpty()) break goLogError;
	    	  resultPersistingNode= // Get or make associated/next child node.
		    	  	resultPersistingNode.getOrMakePersistingNode(keyString);
		  	  scanKeyOffsetI= separatorKeyOffsetI+1; // Compute next key offset.
			  } // while (true)... Loop to select or make next descendant node.
		  } // goLogError:
  			String errorString= "Persistent.getPersistingNode(..), "
	  				+"error getting value, path="+pathString;
	  		appLogger.error(errorString); // Log error string.
		    resultPersistingNode= // and return same string in new PersistingNode.
	  				new PersistingNode(errorString);
	  	} // goReturn:
  			return resultPersistingNode;
	  }

	  public PersistingNode getPersistingNode(String pathString)
			/* This is equivalent to

			    	getPersistingNode(basePersistingNode, pathString)

			  with basePersistingNode set to rootPersistingNode.
	      
	      Note, dealing with absolute paths, paths starting at rootPersistingNode.
	      So this method should be used infrequently. 
			  */
		  {
	  	  return getPersistingNode(rootPersistingNode, pathString);
		  	}

	  private PersistingNode getPersistingNode(
	  		PersistingNode basePersistingNode, String pathString)
			/* Returns the PersistingNode associated with pathString, 
			  or null if there is no PersistingNode stored.
		    It interprets pathString as a path from basePersistingNode
		    to the desired PersistingNode.
		    Each path element is used as a key to select 
		    the next child in the PersistingNode hierarchy.
		    It does one key lookup for every element of the path.
		    An empty pathString is interpreted to mean basePersistingNode.
		    If any key lookup fails, an error occurs,
		    or a value is not found in the final node,
		    then null is returned.
		   	*/
		  {
				 // appLogger.debug(
				 // 				"Persistent.getPersistingNode("+pathString+") begins.");
	  			PersistingNode resultPersistingNode= // Initial result value
	  					basePersistingNode; // is base node.
	  	  goReturn: {
			  goLogError: {
	    	  if (pathString.isEmpty()) break goReturn; // return base node.
				  int scanKeyOffsetI= 0; // Starting offset for path separator search.
				  while (true) { // Select child nodes until desired one is reached.
				    int separatorKeyOffsetI= // Offset of next key/path separator. 
				    		pathString.indexOf(Config.pathSeperatorC, scanKeyOffsetI);
				    if (separatorKeyOffsetI < 0) // There is no next path separator...
				    	{	// So next node is final node.  Return appropriate child node.
				    	  String keyString= // Extract final key from path.
				    	  		pathString.substring(scanKeyOffsetI, pathString.length());
				    	  if (keyString.isEmpty()) break goLogError;
				    	  resultPersistingNode= // Get associated child node, if present.
				    		  resultPersistingNode.getNavigableMap().get(keyString);
					  	  break goReturn; // Return with the possibly null value.
				    		}
				    String keyString= // Extract next key from path up to separator.
		    	  		pathString.substring(scanKeyOffsetI, separatorKeyOffsetI);
		    	  if (keyString.isEmpty()) break goLogError;
		    	  resultPersistingNode= // Get associated child node, if present.
			    		  resultPersistingNode.getNavigableMap().get(keyString);
			  	  if (resultPersistingNode == null) // If no child node with the key 
			  	  	break goReturn; // return the null.
			  	  scanKeyOffsetI= separatorKeyOffsetI+1; // Compute next key offset.
				  } // while (true)... Loop to test next descendant node.
			  } // goLogError:
		  		appLogger.error( "Persistent.getPersistigNode(..), "
		  				+"error getting value, path="+pathString);
			    resultPersistingNode= null; // Override result with null for error.
		  	} // goReturn:
	  			return resultPersistingNode;
		  }


	  // Finalization methods not called by initialization or service sections.
	  
	  public void finalizeV()
	  /* This method stores the persistent data to the external file.  */
	  {
		  storeDataV( theFileString );
	  	}
		  
	  private void storeDataV( String fileString )
	    /* This method stores the Persistent data that is in main memory to 
	      the external text file whose name is fileString.
	      Presently it stores twice:
	      * The data only in the root node.
	      * The data in all nodes of the tree.
	      So some information appears twice in the file,
	      but when reloaded it should go to the same place.  
	      */
	    {
		  	try {
            thePrintWriter= new PrintWriter(
                AppSettings.makeRelativeToAppFolderFile(fileString));
			  		writingV(
			  				"#---multi-element path and data output follows---\n");
		  			multilevelStoreNodeV("", rootPersistingNode);
			  		}
			  	catch (Exception theException) { 
			  		appLogger.exception("Config.storeDataV(..)", theException);
			  		}
			  	finally {
			  		try {
              if ( thePrintWriter != null ) thePrintWriter.close(); 
			  			}
			  		catch ( Exception theException ) { 
				  		appLogger.exception("Config.storeDataV(..)", theException);
				  		}
			  		}
	    	}
	
	  private void multilevelStoreNodeV(
	  		String prefixString, PersistingNode thePersistingNode)
	    throws IOException
	    /* This recursive method stores thePersistingNode 
	      by writing to the stream.
	      thePersistingNode is assumed to be a descendant of rootPersistingNode,
	      with prefixString describing the path from
	      rootPersistingNode to thePersistingNode.
	     */
		  {
		  	{ // Store this node's value, if present.
		  	  String valueString= thePersistingNode.getString();
		  	  if (valueString==null) valueString= "";
		  	  if (! prefixString.isEmpty())
		  	  	writingLineV(prefixString + "=" + valueString);
		  		}
		  	{ // Store child nodes, if present.
			  	Iterator<String> theIterator= // Make iterator for the children. 
		  				thePersistingNode.getNavigableMap().keySet().iterator();
		  		while (theIterator.hasNext()) // Store all children.
				  	{ // Write one child node.
		  				String childKeyString= theIterator.next();
		  				String childPrefixString= prefixString;
		  				if (! childPrefixString.isEmpty()) 
		  					childPrefixString+= Config.pathSeperatorC;
		  				childPrefixString+= childKeyString;
		  				NavigableMap<String,PersistingNode> childNavigableMap=
		  						thePersistingNode.getNavigableMap();
		  				multilevelStoreNodeV(
		  						childPrefixString,childNavigableMap.get(childKeyString));
				  		}
		  			}
		  	}
	
	  public void writingCommentLineV( String commentString ) 
	  	throws IOException
	    /* This method writes theString followed by a newline
	      to the text file OutputStream.
	      */
	    {
        thePrintWriter.print('#'); // Write comment character.
	  		writingLineV(commentString); // Write comment content as line.
	      }
	
	  public void writingLineV( String lineString ) throws IOException
	    /* This method writes theString followed by a newline
	      to the text file OutputStream.
	      */
	    {
	  		writingV(lineString); // Write string.
        thePrintWriter.println(); // Terminate line.
	      }
	
	  public void writingV( String theString ) throws IOException
	    // This method writes theString to the text file OutputStream.
	    {
	  		thePrintWriter.print(theString);
	      }
	  
		}
