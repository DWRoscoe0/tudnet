package allClasses;

import static allClasses.Globals.appLogger;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Properties;

public class Data {

	// Internal state.
	private Properties theProperties= null;
	
  public void initializeV()
    /* This method loads the configuration from external files.  */
    {
  	  theProperties= loadPropertiesV( "Config.txt");
    	}
	
  
  public void finalizeV()
  /* This method loads the configuration from external files.  */
  {
	  storePropertiesV( "Config.txt");
  	}
  
  public Properties loadPropertiesV( String fileString )
    /* This method creates and returns a Properties instance
      from the contents of the external properties file
      whose name is fileString.  
      If no properties were read then the result will be empty.
      If some properties were read then the result will contain them.
      The result will never be null.
      */
    {
  		Properties theProperties= new Properties();
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
  
  public void storePropertiesV( String fileString )
    /* This method stores the configuration properties
      to the external properties file whose name is fileString.  
      */
    {
	  	FileOutputStream theFileOutputStream = null;
	  	try { 
	  		  theFileOutputStream= new FileOutputStream(
	  			  AppFolders.resolveFile( fileString ));
	  			theProperties.store(
	  				theFileOutputStream, "---Infogora configuration and state file---"
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

	}
