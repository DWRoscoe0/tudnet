package allClasses;

import static allClasses.Globals.*;  // appLogger;

import java.io.Closeable;
import java.io.IOException;

public class Closeables 

	/* This class contains static methods which do helpful things with 
	  closable resources.

	  The question of what to do if an exception happens during a close
	  if a difficult one to answer.  It depends on the type of resource 
	  being closed and the context in which it is being closed.  Issues include:
	  * Is the resource read-only?  If true, no data will be lost in the resource.
	  * Is the resource writable?  If true, data might be lost in the resource,
	    and maybe the resource should be deleted.
	  * Is the close being done after an otherwise error-free operation? 
	  * Is the close being done as part of recovery from an error
	    that has occurred on the same resource?

	  ///ehn: maybe add methods which takes an array... of Closeables
	    instead of a single Closable.

	 	*/
 
	{

  public static boolean closeWithoutErrorLoggingB(Closeable theCloseable)
    /* This method is for closing a resource with a minimum of fuss.
      It doesn't even do logging or exceptions.
      
      This method does nothing if theCloseable == null.
      Otherwise it closes theClosable but does not log any exception.
      
      It returns true if either theCloseable is null or there was an exception,
      false otherwise.
      */
    {
  	  boolean errorOccurredB= false;
  	  if (theCloseable == null)
  	  	errorOccurredB= true;
	  	  try { 
		  	  	theCloseable.close(); 
		  	  } catch (Exception theException) {
		  	  	errorOccurredB= true;
		  	  }
	  	return errorOccurredB;
	    }

  public static boolean closeWithErrorLoggingB(Closeable theCloseable)
    /* This method is for closing a resource with a minimum of fuss.
      It assumes that if an exception occurs during the close,
      then simply logging that exception is sufficient handling.
      
      It returns true if either theCloseable is null or there was an exception,
      false otherwise.
      */
    {
  	  boolean errorOccurredB= false;
  	  if (theCloseable == null) {
  	  		errorOccurredB= true;
  	  	} else {
		  	  try { 
			  	  	theCloseable.close(); 
			  	  } catch (Exception theException) {
			  	  	errorOccurredB= true;
			  			appLogger.exception(
				  				"closeWithErrorLoggingB(..): ", theException
				  				);		  	  	
			  	  }
  	  	}
	  	return errorOccurredB;
	    }

  public static IOException closeAndAccumulateIOException(
  			Closeable theCloseable, IOException earlierIOException)
    /* This method is for closing a resource but retaining
      the ability to detect and process exceptions during the close.
      If earlierIOException is not null then it contains an exception
      that has already been created, and any exception during
      the requested close() is added to it as a suppressed exception.
      If earlierIOException is null and a close exception happens,
      then a new exception will be constructed and assigned to
      earlierIOException and the close exception will be added to it 
      as a suppressed exception.  
      Any number of suppressed exceptions may be added.

      This method is meant to be called from the finally block,
      which is where resources are recommended to be closed.

			The possibly modified value of earlierIOException is returned.

			///enh Use a special WhileClosingException instead of Exception.

			///enh Maybe store the first close() exception as the cause,
			  and later ones as suppressed exceptions?

			///enh This method is for IOExceptions associated with close(),
			  but it might be generalized, with generics, for others Throwables.
      */
    {
  	  if (theCloseable != null)
	  	  try { 
		  	  	theCloseable.close(); 
		  	  } catch (IOException newIOException) {
		  			appLogger.exception(
			  				"closeDuringCatchB(..): ", newIOException
			  				);
		  			if ( earlierIOException == null ) // Create first exception if none.
		  				earlierIOException= new IOException( "while closing" );
		  			earlierIOException.addSuppressed(newIOException);
		  	  }
	  	return earlierIOException;
	    }

		}
