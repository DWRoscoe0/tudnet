package allClasses;

public class RepeatDetector 
  {
    private int countI= 1;  // This will trigger a rescale immediately.
    private int SizeI= 1;  // Initial minimum size.
    private int savedValueI= 0;

    RepeatDetector()
      // Constructor.  All initialization is done above.
      {}
      
    public boolean testB( int inValueI )
      /* This method tests whether valueI occurred and was recorded before.
        It returns true if it has, false otherwise.
        */
      { // Repeat loop test.
        boolean RepeatedB= false;   // Set default result.
        if ( savedValueI == inValueI )  // Value is last recorded value?
          RepeatedB= true;  // Yes, set true result.
          else  // Value is not last recorded value.
          { // Count and maybe expand interval.
            System.out.print( "." );  // Dbg: iteration iIndicator.
            --countI;  // Decrement search count for recorded value.
            if ( countI == 0 )  // Time to record new value and expand count.
              { // Record new value and expand interval.
                SizeI*=2;  // Double the search test limit.
                countI= SizeI;  // Reset counter to new limit.
                savedValueI= inValueI;  // Save new repeat target.
                } // Record new value and expand interval.
            } // Count and maybe expand interval.
        return RepeatedB;
        } // Repeat loop test.
    
    }

