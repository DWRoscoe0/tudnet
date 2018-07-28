package allClasses;

import static allClasses.Globals.appLogger;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.TreeMap;
import java.util.Iterator;

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
		private TreeMap<Object,Object> theMap= null;
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
	  		theMap= new TreeMap<Object,Object>(); // Create empty Map. 

	  		FileInputStream configFileInputStream= null;

		  	try { 
		  		  configFileInputStream = 
		  		  		new FileInputStream(Config.makeRelativeToAppFolderFile( fileString ));
		  		  loadV(configFileInputStream);
			  		} 
			  	catch (FileNotFoundException theFileNotFoundException) { 
			  		appLogger.warning("Config.loadDataV(..)"+theFileNotFoundException);
			  		}
			  	catch (Exception theException) { 
			  		appLogger.exception("Config.loadDataV(..)", theException);
			  		}
			  	finally { 
			  		try { 
			  			if ( configFileInputStream != null ) configFileInputStream.close(); 
			  			}
				  	catch (Exception theException) { 
				  		appLogger.exception("Config.loadDataV(..)", theException);
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
		  	  while (true) // Read, convert, and store a line in characters array.
		  	  	{
		  	  	  if  // Exit loop if end of line character, or end of file.
		  	  	    ((CI=='\n') || (CI=='\r') || (CI==-1)) 
		  	  	  	break;
		  	  	  lineAB[offsetI++]= (byte)(0xff & CI); // Store converted byte.
		  		  	CI= configFileInputStream.read(); // Read next byte.
		  	  		}
		  	  String lineString= // Convert char array to a String.
		  	  		new String(lineAB, 0, offsetI);
		  	  processLineV(lineString); // Process string appropriately.
  		  	CI= configFileInputStream.read(); // Read next byte.
		  		}
	    	}

	  private void processLineV(String lineString)
	    /* Reads lineString and saves any key-value entry present.  */
		  { 
	  		goReturn: {
	  		goFail: {
		  	    if (lineString.isEmpty())  // Empty line. 
		  	    	break goReturn; // Ignore with success.
		  	    if (lineString.charAt(0) == '#') // Comment line.
		  	    	break goReturn; // Ignore with success.
		  	    int offsetOfEqualsI= lineString.indexOf('=', 0); // First '='.
		  	    if ( offsetOfEqualsI < 1) // No '=' in line or it's at beginning
		  	    	break goFail; // Fail.
		  	    // All is well.  Extract key and value and save them.
		  	    String keyString= lineString.substring(0, offsetOfEqualsI);
		  	    String valueString= lineString.substring(offsetOfEqualsI+1);
		  	    theMap.put(keyString,valueString);
		  	    break goReturn; // Return with success.
		  	} // goFail:
		  		appLogger.warning("processLineV(..) failed, line=\""+lineString+"\"");
		  	} // goReturn:
		  }
	  
	  private void storeDataV( String fileString )
	    /* This method stores the Map data to 
	      the external text file whose name is fileString.  
	      */
	    {
		  	try { 
		  		  theFileOutputStream= new FileOutputStream(
		  		  		Config.makeRelativeToAppFolderFile( fileString ));
		  			storeV( theFileOutputStream );
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

	  private void storeV( FileOutputStream theFileOutputStream)
	    throws IOException
		  {
	  		writingStringV("#---NEW Infogora persistent data file---\n");
	  		Iterator<?> theIterator = theMap.keySet().iterator();
	  		while (theIterator.hasNext())
			  	{ // Write one element.
	  				String keyString = (String)theIterator.next();
	          String valueString = (String)theMap.get(keyString);
	          writingStringV(keyString + "=" + valueString + "\n"); // Write line.
			  		}
		  	}

    public void writingStringV( String theString ) throws IOException
      // This method writes theString to the text file OutputStream.
      {
    		byte[] buf = theString.getBytes(); // Getting byte buffer from String
    		theFileOutputStream.write(buf); // Writing it to stream memory.
        }

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
	
	  public String getDefaultingToBlankString( String keyString )
		  /* Returns the value String associated with keyString,
		    or the empty string if there is none.
		   	*/
		  {
				return getString( keyString, "" );
			  }
	
	  private String getString( String keyString, String defaultValueString )
			/* Returns the value String associated with keyString,
		    or defaultValueString if there is none.
		   	*/
		  {
	  	  String valueString= (String)theMap.get(keyString);
	  	  if (valueString == null)
	  	  	valueString= defaultValueString;
	  	  return valueString;
			  }
	
	  public void putV( String keyString, String valueString )
		  // Stores valuesString as the value associated with keyStrig.
		  {
	  		theMap.put( keyString, valueString );
			  }
		
		}
