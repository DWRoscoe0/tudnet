package allClasses;

import static allClasses.Globals.appLogger;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Properties;
import java.util.TreeSet;

public class Persistent 

	/* This class implements persistent data for an app.
	  It is implemented with a Properties file.
	  It provides additional support for hierarchical properties
	  using property path names whose components are separated by "/".
	  So "key" is synonymous with "full path".
	  A key does not end in a slash.  A prefix ends in a slash.
	 	*/
	
	{
	
		private final String theFileString= "PersistentData.txt";
		
		// Internal state.
		private Properties theProperties= null;
		
	  public void initializeV()
	    /* This method loads the configuration from external files.  */
	    {
	  	  theProperties= loadPropertiesV( theFileString );
	  	  
	  		///dbg storePairV( "kayX", "valueX" );
		  	///dbg storePairV( "kayY", "valueY" );
		  	///dbg storePairV( "kayZ", "valueZ" );
	  	  
	    	}
		
	  
	  public void finalizeV()
	  /* This method loads the configuration from external files.  */
	  {
		  storePropertiesV( theFileString );
	  	}
	  
	  private Properties loadPropertiesV( String fileString )
	    /* This method creates and returns a Properties instance
	      from the contents of the external properties file
	      whose name is fileString.  
	      If no properties were read then the result will be empty.
	      If some properties were read then the result will contain them.
	      The result will never be null.
	      */
	    {
	  		Properties theProperties= // Adapt to write properties alphabetically.
					new Properties() { 
						@Override
		  	    public synchronized Enumeration<Object> keys() {
		  	        return Collections.enumeration(new TreeSet<Object>(super.keySet()));
		  	    	}
		  			};

	  		FileInputStream configFileInputStream= null;

		  	try { 
		  		  configFileInputStream = 
		  		  		new FileInputStream(Config.makeRelativeToAppFolderFile( fileString ));
				  	theProperties.load(configFileInputStream);
			  		} 
			  	catch (FileNotFoundException theFileNotFoundException) { 
			  		appLogger.warning("Config.loadPropertiesV(..)"+theFileNotFoundException);
			  		}
			  	catch (Exception theException) { 
			  		appLogger.exception("Config.loadPropertiesV(..)", theException);
			  		}
			  	finally { 
			  		try { 
			  			if ( configFileInputStream != null ) configFileInputStream.close(); 
			  			}
				  	catch (Exception theException) { 
				  		appLogger.exception("Config.loadPropertiesV(..)", theException);
				  		}
			  		}
		  	return theProperties;
	    	}
	  
	  private void storePropertiesV( String fileString )
	    /* This method stores the configuration properties
	      to the external properties file whose name is fileString.  
	      */
	    {
		  	FileOutputStream theFileOutputStream = null;
		  	try { 
		  		  theFileOutputStream= new FileOutputStream(
		  		  		Config.makeRelativeToAppFolderFile( fileString ));
		  			theProperties.store(
		  				theFileOutputStream, 
		  				"---Infogora persistent data file---, PRESENTLY TEST DATA ONLY." //tmp
		  				);
			  		} 
			  	catch (Exception theException) { 
			  		appLogger.exception("Config.storePropertiesV(..)", theException);
			  		}
			  	finally { 
			  		try { 
			  			if ( theFileOutputStream != null ) theFileOutputStream.close(); 
			  			}
			  		catch ( Exception theException ) { 
				  		appLogger.exception("Config.storePropertiesV(..)", theException);
				  		}
			  		}
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
	  			} while (true);
	  		putV( // Attach chain to entry being moved to front. 
	  				entryIDKeyString + "next",
	  				getDefaultingToBlankString( listFirstKeyString ) 
	  				);
	  		putV( listFirstKeyString, entryIDString );
	  		  // Set chain pointer to entry moved to front.
	  		return entryIDKeyString; 
	  		}
	
		public String getListEntryPrefixString( String listNameString )
		  {
				return keyToPrefixString( listNameString ) + "entries/";
			  }
	
		public String getListFirstKeyString( String listNameString )
		  {
				return keyToPrefixString( listNameString ) + "first";
			  }
	
		public String getListEntryIDFieldValueString( 
				String listNameString, String entryIDString, String fieldNameString )
			{
				return getString( 
						getListEntryIDFieldKeyString( 
								listNameString, entryIDString, fieldNameString 
								)
						);
			  }
	
		public String getListEntryNextKeyString( 
				String listNameString, String entryIDString )
		  {
				return getListEntryIDFieldKeyString( 
						listNameString, entryIDString, "next"
						);
			  }
	
		public String getListEntryIDFieldKeyString( 
				String listNameString, String entryIDString, String fieldNameString )
			{
				return 
						getListEntryIDPrefixString( listNameString, entryIDString ) 
						+ fieldNameString ;
			  }

		public String getListEntryIDPrefixString( 
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

		public String extendPrefixString( 
				String prefixString, String fieldNameString )
		  /* Converts prefixString to a key by appending fieldNameString
		    and a separator character.
		    */
		  {
				return keyToPrefixString(
						prefixToKeyString( prefixString, fieldNameString)
						);
			  }

		public String prefixToKeyString( 
				String prefixString, String fieldNameString )
		  // Converts prefixString to a key by appending fieldNameString.
		  {
				return prefixString + fieldNameString ;
			  }

		public String keyToPrefixString( String prefixString )
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
	
		public String getString( String keyString )
			/* Returns the value String associated with keyString,
		    or null if there is none.
		   	*/
		  {
				return theProperties.getProperty( keyString );
			  }
	
		public String getString( String keyString, String defaultValueString )
			/* Returns the value String associated with keyString,
		    or defaultValueString if there is none.
		   	*/
		  {
				return theProperties.getProperty( keyString, defaultValueString );
			  }
	
		public void putV( String keyString, String valueString )
		  // Stores valuesString as the value associated with keyStrig.
		  {
				theProperties.setProperty( keyString, valueString );
			  }
		
		}
