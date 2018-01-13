package allClasses;

import static allClasses.Globals.appLogger;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Properties;
import java.util.TreeSet;

public class Persistent {

	private final String theFileString= "PersistantData.txt";
	
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
	  		  		new FileInputStream(AppFolders.resolveFile( fileString ));
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
	  			  AppFolders.resolveFile( fileString ));
	  			theProperties.store(
	  				theFileOutputStream, "---Infogora persistant data file---"
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

  public String updateEntryString( 
  		String entryIDString, String entriesKeyString )
  	{ 
  		String entriesFirstKeyString= entriesKeyString + "/first";
  		String entryIDKeyString= entriesKeyString+"/entry/"+entryIDString+"/";
  		String scanKeyString= entriesFirstKeyString;
  		String scanValueString;
  		searchAndUnlinkIfPresent: do {
    		scanValueString= getString( scanKeyString, "" );
    		if (scanValueString.equals("")) // Exit because search failed. 
    			break searchAndUnlinkIfPresent;
    		if // Search succeeded.  Unlink entry and exit.
    		  (scanValueString.equals(entryIDString))
    			{
		    		putV( // Link list around peer being removed. 
		    				scanKeyString, 
		    				getString( 
		    						"peer/entry/"+entryIDString+"/next","") 
		    				);
	    			break searchAndUnlinkIfPresent;
	    			}
    		scanKeyString= // Link to next entry.
    				"peer/entry/"+scanValueString+"/next";
  			} while (true);
  		putV( 
  				entryIDKeyString + "next",
  				getString( entriesFirstKeyString, "" ) 
  				);
  		putV( entriesFirstKeyString, entryIDString );
  		return entryIDKeyString; 
  		}

	public String getString( String keyString )
	  {
			return theProperties.getProperty( keyString );
		  }

	public String getString( String keyString, String defaultValueString )
	  {
			return theProperties.getProperty( keyString, defaultValueString );
		  }

	public void putV( String keyString, String valueString )
	  {
			theProperties.setProperty( keyString, valueString );
		  }
	
	}
