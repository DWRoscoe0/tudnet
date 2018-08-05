package allClasses;

import static allClasses.Globals.appLogger;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.TreeMap;

public class Persistent 

	/* This class implements persistent data for an app.
	  It is implemented with a text file.
	  It provides additional support for hierarchical properties
	  using property path names whose components are separated by "/".
	  So "key" is synonymous with "full path".
	  A key does not end in a slash.  A prefix ends in a slash.
	  
	  There is also some support for linked lists.
	  ///org Maybe that should be provided by the PersistentCursor class only?
	 	*/
	
	{
	
		private final String theFileString= "PersistentData.txt";
		
		// Internal state.
		private PersistingNode rootPersistingNode= null;
		//// private TreeMap<String,PersistingNode> theMap= null;
		  // TreeMap provides alphabetically sorted output to text file.
  	private FileOutputStream theFileOutputStream = null;
		
	  public void initializeV()
	    /* This method loads the persistent data from an external file.
	      It is possible to eliminate this method, and triggered by lazy loading,
	      triggered by the first call that needs theMap variable defined.
	      But because finalizeV() must be called to write any changes,
	      we might as well just call initializeV() as well.
	      */
	    {
	  	  loadDataV( theFileString );
	  	  
	  		///dbg storePairV( "keyX", "valueX" );
		  	///dbg storePairV( "keyY", "valueY" );
		  	///dbg storePairV( "keyZ", "valueZ" );
	  	  
	    	}

	  public void finalizeV()
	  /* This method stores the persistent data to the external file.  */
	  {
		  storeDataV( theFileString );
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
	  		//// theMap= new TreeMap<String,PersistingNode>(); // Create empty Map. 
	  		rootPersistingNode= 
	  				new PersistingNode("this-is-root"); // Create empty Map. 

	  		FileInputStream configFileInputStream= null;

		  	try { 
		  		  configFileInputStream= // Prepare file containing persistent data.
		  		  		new FileInputStream(
		  		  				Config.makeRelativeToAppFolderFile( fileString ));
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
		  	  processLineV(lineString); // Process line appropriately.
  		  	CI= configFileInputStream.read(); // Read next byte.
		  		}
	    	}

	  private void processLineV(String lineString)
	    /* Reads lineString and saves any key-value entry present.  
	      This is called "process" and not "load" because
	      some lines, such as comments, are ignored.
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
		    multilevelPutB(keyPathString,valueString);
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
		  		  theFileOutputStream= new FileOutputStream(
		  		  		Config.makeRelativeToAppFolderFile( fileString ));
			  		writingV(
			  				"#---Infogora persistent data, single level only---\n");
		  			storeNodeV("", rootPersistingNode);
			  		writingV(
			  				"#---EXPERIMENTAL multilevel data output follows---\n");
		  			multilevelStoreNodeV("", rootPersistingNode);
			  		}
			  	catch (Exception theException) { 
			  		appLogger.exception("Config.storeDataV(..)", theException);
			  		}
			  	finally {
			  		try {
			  			if ( theFileOutputStream != null ) theFileOutputStream.close(); 
			  			}
			  		catch ( Exception theException ) { 
				  		appLogger.exception("Config.storeDataV(..)", theException);
				  		}
			  		}
	    	}

	  private void storeNodeV(
	  		String prefixString, PersistingNode thePersistingNode)
	    throws IOException
	    /* Recursively stores thePersistingNode to the file stream.
	      If there is a valueString in thePersistingNode then
	      a line is output of the form: "prefixString=valueString".
	      If thePersistingNode has any children then
	      this method is called recursively with 
	      Key Strings are appended to prefixString.
	      prefixString becomes longer with each level of recursion.
	      */
		  {
	  		Iterator<String> theIterator= 
	  				thePersistingNode.getChildrenTreeMap().keySet().iterator();
	  		while (theIterator.hasNext()) // Iterate over all children.
			  	{ // Write one child element.
	  				String childKeyString= theIterator.next();
	  				PersistingNode childPersistingNode= 
	  						thePersistingNode.getChildrenTreeMap().get(childKeyString);  
	          String childValueString= childPersistingNode.getValueString();
	  				String lineString= prefixString;
	  				if (! lineString.isEmpty()) lineString+= Config.pathSeperatorC;
	  				lineString+= childKeyString + "=" + childValueString;
	  				writingLineV(lineString); // Write a line containing value.
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
		  	  String valueString= thePersistingNode.getValueString();
		  	  if (valueString==null) valueString= "";
		  	  if (! prefixString.isEmpty() 
		  	  		//// && valueString!=null && ! valueString.isEmpty()
		  	  		)
	  		  	////writingStringV(prefixString + "=" + valueString + "\n");
		  	  	writingLineV(prefixString + "=" + valueString);
		  		}
		  	{ // Store child nodes, if present.
			  	Iterator<String> theIterator= // Make iterator for the children. 
		  				thePersistingNode.getChildrenTreeMap().keySet().iterator();
		  		while (theIterator.hasNext()) // Store all children.
				  	{ // Write one child node.
		  				String childKeyString= theIterator.next();
		  				String childPrefixString= prefixString;
		  				if (! childPrefixString.isEmpty()) 
		  					childPrefixString+= Config.pathSeperatorC;
		  				childPrefixString+= childKeyString;
		  				TreeMap<String,PersistingNode> childTreeMap=
		  						thePersistingNode.getChildrenTreeMap();
		  				////writingCommentStringV("experimentalStoreNodeV(..) recursing.");
		  				multilevelStoreNodeV(
		  						childPrefixString,childTreeMap.get(childKeyString));
				  		}
		  			}
		  	}

    public void writingCommentLineV( String commentString ) 
    	throws IOException
      /* This method writes theString followed by a newline
        to the text file OutputStream.
        */
      {
    		theFileOutputStream.write('#'); // Write comment character.
    		writingLineV(commentString); // Write comment content as line.
        }

    public void writingLineV( String lineString ) throws IOException
      /* This method writes theString followed by a newline
        to the text file OutputStream.
        */
      {
    		writingV(lineString); // Write string.
    		theFileOutputStream.write('\n'); // Terminate line.
        }

    public void writingV( String theString ) throws IOException
      // This method writes theString to the text file OutputStream.
      {
    		byte[] buf= // Getting byte buffer equivalent of String
    				theString.getBytes();
    		theFileOutputStream.write(buf); // Writing it to stream.
        }

    
    // Link list kludge methods.
    
	  public String entryInsertOrMoveToFrontString( 
	  		String entryIDString, String listNameString )
		  /* This method updates information about the linked list entry
			  whose list name is listNameString and 
			  whose ID String is entryIDString.
			  The named entry is always moved to or 
			  inserted at the front of the list.
			  The following are all the test cases for processing an entry into
			  lists of lengths 0, 1, 2, and 3.  These were tested manually.
			  / (),a -> (a)
			  / (a),a -> (a)
			  / (a),b -> (b,a) 
			  / (b,a),b -> (b,a) 
			  / (b,a),a -> (a,b)
			  / (b,a),c -> (c,a,b)
			  / (c,a,b),c -> (c,a,b)
			  / (c,a,b),a -> (a,c,b)
			  / (a,c,b),b -> (b,a,c)
			  */
	  	{ 
	  		String listFirstKeyString= getListFirstKeyString( listNameString );
	  		String entryIDKeyString= 
	  				getListEntryIDPrefixString( listNameString, entryIDString );
	  		String scanIDEntryKeyString= listFirstKeyString;
	  		String scanIDEntryValueString;
	  		searchAndUnlinkIfPresent: do {
	    		scanIDEntryValueString= 
	    				getDefaultingToBlankString( scanIDEntryKeyString );
	    		if (scanIDEntryValueString.isEmpty()) // Exit because search failed.
	    			break searchAndUnlinkIfPresent;
	    		if // Search succeeded.  Unlink entry and exit.
	    		  (scanIDEntryValueString.equals(entryIDString))
	    			{ putV( // Link list around peer being removed. 
			    				scanIDEntryKeyString, 
			    				getDefaultingToBlankString( getListEntryNextKeyString( 
			    						listNameString, entryIDString ) ) );
		    			break searchAndUnlinkIfPresent;
		    			}
	    		scanIDEntryKeyString= // Get link to next entry to check.
	    				"peers/entry/"+scanIDEntryValueString+"/next";
	  			} while (true); // searchAndUnlinkIfPresent: 
	  		putV( // Attach chain to entry being moved to front. 
	  				entryIDKeyString + "next",
	  				getDefaultingToBlankString( listFirstKeyString ) 
	  				);
	  		putV( listFirstKeyString, entryIDString );
	  		  // Set chain pointer to entry moved to front.
	  		return entryIDKeyString; 
	  		}
	
	  private String getListFirstKeyString( String listNameString )
		  {
				return keyToPrefixString( listNameString ) + "first";
			  }
	
	  private String getListEntryNextKeyString( 
				String listNameString, String entryIDString )
		  {
				return getListEntryIDFieldKeyString( 
						listNameString, entryIDString, "next"
						);
			  }
	
	  private String getListEntryIDFieldKeyString( 
				String listNameString, String entryIDString, String fieldNameString )
			{
				return 
						getListEntryIDPrefixString( listNameString, entryIDString ) 
						+ fieldNameString ;
			  }

	  private String getListEntryIDPrefixString( 
				String listNameString, String entryIDString )
		  /* This method returns a prefix for the list entry whose
		    list name is listNameString and whose entry name is entryIDString.
		   */
		  {
				return keyToPrefixString( listNameString ) 
						+ "entries/"
						+ entryIDString 
						+ "/";
			  }

	  private String keyToPrefixString( String prefixString )
		  /* Makes a prefix from a key by appending the separator character,
		    which is the slash.
		    */
		  {
				return prefixString + "/";
			  }
		
	  
	  // Basic get and put methods.
	  
	  public void XputV( String pathString, String valueString ) ////
		  /* Stores valuesString as the value associated with pathStrig.
		    It does this only on a single level, in the rootPersistingNode.
		   	*/
		  {
	  		rootPersistingNode.getChildrenTreeMap().
	  		  put( pathString, new PersistingNode(valueString));
			  }
	  
	  public void putV( String pathString, String valueString )
		  /* Stores valuesString as the value associated with pathStrig.
		    It does this only on a single level, in the rootPersistingNode.
		   	*/
		  {
	  		multilevelPutB(pathString,valueString);
			  }

	  private boolean multilevelPutB(String pathString,String valueString)
	    /* This is like multilevelPutB(thePersistingNode,keyString,valueString)
	      but with thePersistingNode set to the rootPersistingNode. 
	     	*/
	    {
		  	return multilevelPutB(rootPersistingNode,pathString,valueString);
		    }

	  private boolean multilevelPutB(
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
				    	thePersistingNode.setChildValueV(pathString, valueString);
					    break toReturn;
				    	}
			    if (offsetOfSeparatorI < 1) // Separator is at beginning of key.
				    break toFail; // Key head would be empty.
			    String keyHeadString= pathString.substring(0, offsetOfSeparatorI);
			    if (offsetOfSeparatorI >= pathString.length()) // Separator is at end.
				    break toFail; // Key tail would be empty.
			    String keyTailString= pathString.substring(offsetOfSeparatorI+1);
	    		PersistingNode childPersistingNode= // Get or make appropriate child. 
		    			thePersistingNode.getOrMakeChildPersistingNode(keyHeadString);
	    		resultB= // Recurse using tail of key as new key.
	    				multilevelPutB(
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
		
	  private String XgetString( String pathString) ////
			/* Returns the value String associated with pathString,
		    or null if there is no value String stored.
		    It does not try to interpret path separator characters in the key.
		    It does a single-level lookup only.
		   	*/
		  {
					String childValueString= null;
			  goReturn: {
			  goReturnNull: {
		  	  PersistingNode childPersistingNode= 
		  	  		rootPersistingNode.getChildrenTreeMap().get(pathString);
		  	  if (childPersistingNode == null) break goReturnNull;
		  	  childValueString= childPersistingNode.getValueString();
		  	  if (childValueString == null) break goReturnNull;
		  	  break goReturn; // Use retrieved valueString.
			  } // goUseDefault:
					childValueString= null;
				  break goReturn;
		  	} // goReturn:
					return childValueString;
			  }
		
	  private String getString( String pathString)
			/* Returns the value String associated with pathString,
		    or null if there is no value String stored.
		    This new version does a multilevel lookup.
		   	*/
		  {
	  	  return multilevelGetString(pathString);
			  }

	  private String multilevelGetString(
	  		String pathString, String defaultValueString)
			/* This is like multilevelGetString(String pathString) but
			  instead of returning null if no value is found,
			  it returns defaultValueString.
			 */
		  {
				String resultValueString= multilevelGetString(pathString); 
	  	  if (resultValueString == null) 
	  	  	resultValueString= defaultValueString;
				return resultValueString;
			  }

	  private String multilevelGetString(String pathString)
			/* Returns the value String associated with pathString, 
			  or null if there is no value String stored.
		    It interprets pathString as a path from the root PersistingNode
		    to the node that contains the desired value.
		    Each path element is used as a key to select 
		    the next child in the PersistingNode hierarchy.
		    It does one key lookup for every element of the path.
		    If any key lookup fails or a value is not found in the final node
		    then null is returned.
		   	*/
		  {
	  			String resultValueString; // Storage for result value.
	  			PersistingNode scanPersistingNode= // Start scan at root node. 
	  					rootPersistingNode;
			    int scanKeyOffsetI= 0; // Offset of beginning of separator search. 
	  	  goReturn: {
	  	  goReturnNull: {
			  goLogError: {
				  while (true) { // Select child nodes until one with value is reached.
				    int separatorKeyOffsetI= // Offset of next key/path separator. 
				    		pathString.indexOf(Config.pathSeperatorC, scanKeyOffsetI);
				    if (separatorKeyOffsetI < 0) // There is no path separator in path..
				    	{	// So this is final node.  Return value from appropriate child.
				    	  String keyString= // Key is entire remainder of path.
				    	  		pathString.substring(scanKeyOffsetI, pathString.length());
				    	  if (keyString.isEmpty()) break goLogError;
			    			PersistingNode childPersistingNode= // Get associated child. 
				    		  scanPersistingNode.getChildrenTreeMap().get(keyString);
					  	  if (childPersistingNode == null) // If no child with this key.. 
					  	  	break goReturnNull;
					  	  resultValueString= childPersistingNode.getValueString();
					  	  if (resultValueString == null) // If the child has no value..
					  	  	break goReturnNull;
					  	  break goReturn; // Use the value from the child.
				    		}
				    if (separatorKeyOffsetI == scanKeyOffsetI) // Separator is at start.
					    break goLogError; // So there is no key to select a child.
		    	  String keyString= // Extract key from path.
		    	  		pathString.substring(scanKeyOffsetI, separatorKeyOffsetI);
	    			PersistingNode childPersistingNode= // Get associated child. 
			    		  scanPersistingNode.getChildrenTreeMap().get(keyString);
			  	  if (childPersistingNode == null) // If no child node with this key.. 
			  	  	break goReturnNull;
			  	  scanPersistingNode= childPersistingNode; // Select next node.
			  	  scanKeyOffsetI= separatorKeyOffsetI+1; // Select next key offset.
				  } // while (true)... Loop to test next descendant node.
			  } // goLogError:
		  		appLogger.error(
		  				"Persistent.multiGetString(..), error getting value, path="+
		  				pathString);
	  	  	break goReturnNull;
				} // goReturnNull:
					resultValueString= null;
				  break goReturn;
		  	} // goReturn:
	  			return resultValueString;
		  }

		}
