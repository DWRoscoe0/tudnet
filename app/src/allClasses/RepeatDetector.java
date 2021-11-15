package allClasses;

public class RepeatDetector

  /* This is a class designed to eventually detect repeated use of integers
    but do so without storing a lot of them.
    It works best on non-sparse ranges of integers.
   */

	{
		private int previousValueI= Integer.MIN_VALUE;
    private int minimumI= Integer.MAX_VALUE;
    private int maximumI= Integer.MIN_VALUE;
    private int downCountI= 1000;

    RepeatDetector() {} // Constructor.  All initialization is done above.

    public boolean repeatedB( int inValueI )
      /* This method tests whether there must have been a repeat,
        not necessarily of the present invalueI, maybe,
        but possibly a previous inValueI.
        It returns true if there must have been a repeat,
        false if it's possible there might not have been a repeat.
        
        ///fix  This method is disable.  
        It always returns false because its caller doesn't respond well
        to true
        */
      {
        	boolean RepeatedB= false;   // Set default result.
        goReturn: {
        goExpandInterval: {
	        if ( previousValueI == inValueI )  // Quick check for previous value.
	        	{ RepeatedB= true; break goReturn; } // Yes, return true result.
	        if (inValueI > maximumI)
	        	{ maximumI= inValueI; break goExpandInterval; }
	        if (inValueI < minimumI)
	        	{ minimumI= inValueI; break goExpandInterval; }
          if ( downCountI == 0 ) // There must have been a repeat.
          	{ RepeatedB= true; break goReturn; } // Yes, return true result.
          --downCountI;  // Decrement search count for recorded value.
          previousValueI= inValueI;  // Save new repeat target.
          break goReturn;
          } // goExpandInterval:
        downCountI= maximumI-minimumI;
	        } // goReturn:
        return RepeatedB;
        }

    }
