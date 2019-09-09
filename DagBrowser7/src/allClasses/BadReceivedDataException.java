package allClasses;

public class BadReceivedDataException extends Exception 

	/* This exception is thrown to indicate that data received from somewhere
    does not conform to its requirements.
   	*/

	{ 
  
    public BadReceivedDataException(Throwable theThrowable)
    {
      super(theThrowable);
    }
    
	}
