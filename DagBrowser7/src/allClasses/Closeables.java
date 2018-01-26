package allClasses;

import static allClasses.Globals.*;  // appLogger;

import java.io.Closeable;

public class Closeables 

	/* This class contains static methods which do helpful things with 
	  closable class instances. 
	  
	  ///ehn: maybe add a method which takes an array... of Closeables.
	  
	 	*/
 
	{

	  public static void closeCleanlyV(Closeable theCloseable)
	    /* This method does nothing if theCloseable == null.
	      Otherwise it closes theClosable and logs any exception that occurs.
	      */
	    {
	  	  if (theCloseable != null)
		  	  try { 
			  	  	theCloseable.close(); 
			  	  } catch (Exception theException) {
				  		appLogger.exception(
				  				"Closeable.closeCleanly(..): ", theException
				  				);		  	  	
			  	  }
		      }
		}
