package allClasses;

import static allClasses.Globals.*;  // appLogger;

public class Nulls

  // Contains static methods which do meaningful things with null arguments. 

	{
	
	  public static <T> T fastFailNullCheckT( T testT )
	    // Throws a NullPointerException if testT == null, 
	    // Otherwise returns testT.
	    {
			  if ( testT == null ) // Doing fast-fail construction check.
				  {
			  		appLogger.error("fastFailNullCheckT( T testT ):");
				  	throw new NullPointerException();
				  	}
			  return testT;
	      }
	
		public static boolean equals(Object the1stObject, Object the2ndObject) 
		  /* This static equals(..) method is based on one proposed at     
		    http://bugs.java.com/view_bug.do?bug_id=6797535 .
		    It compares two objects passed by reference.
		    Either reference may be null, but in that case 
		    the result is true only if both are null.
		    */
			{
				return 
					(the1stObject == the2ndObject) // true if references are equal. 
					|| // or 
					( (the1stObject != null) && // true if the1stObject is non-null and 
					  the1stObject.equals(the2ndObject) // its equals(..) returns true. 
					  );
				}
		
		public static int hashCode(Object theObject) 
		  /* This static hashCode(..) method is based on one proposed at     
		    http://bugs.java.com/view_bug.do?bug_id=6797535 .
		    It works like the regular instance hashCode() but
		    returns 0 if the object reference is null.
		    */
			{
	  		return 
	  				theObject != null // If object reference is not null 
	  				? theObject.hashCode() // then use regular hashCode() method
	  				: 0; // otherwise return 0.
	  		}
		
		public static String toString(Object theObject) 
		  /* This static toString(..) works like the regular instance tpStromg() 
		    but returns "null" if the object reference is null.
		    */
			{
	  		return 
	  				theObject != null // If object reference is not null 
	  				? theObject.toString() // then use regular toString() method
	  				: "null"; // otherwise return the word "null".
	  		}
	
		}
