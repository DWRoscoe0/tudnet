package allClasses;

public class OSTime 
  {
    /* This class is used to measure and report
     * the amount of time spent on various OS operations. 
     */

  static String getOSReportString()
      /* This method returns a String representing
       * the amounts of time spend in various OS operations.
       */
    {
      long nowTimeNsL= System.nanoTime();
      if  // If 1/2 second has passed
        ( 500000 <= (nowTimeNsL - OSTime.osLastTimeNsL) ) 
        { // update report String.
          OSTime.osReportString= "";
          
          OSTime.osReportString+= OSTime.DutyCycle.resetAndGetOSString(
              OSTime.directoryDutyCycle, " dir:", nowTimeNsL);
          OSTime.osReportString+= OSTime.DutyCycle.resetAndGetOSString(
              OSTime.deletingDutyCycle, " del:", nowTimeNsL);
          OSTime.osReportString+= OSTime.DutyCycle.resetAndGetOSString(
              OSTime.closingDutyCycle, " clo:", nowTimeNsL);
          OSTime.osReportString+= OSTime.DutyCycle.resetAndGetOSString(
              OSTime.writingDutyCycle, " wrt:", nowTimeNsL);
          OSTime.osReportString+= OSTime.DutyCycle.resetAndGetOSString(
              OSTime.readingDutyCycle, " rea:", nowTimeNsL);
          OSTime.osReportString+= OSTime.DutyCycle.resetAndGetOSString(
              OSTime.syncingDutyCycle, " syn:", nowTimeNsL);
          OSTime.osLastTimeNsL= nowTimeNsL; // Reset for next time.
          }
    
      return OSTime.osReportString;
      }
  
    static String quotientAsPerCentString(long dividentL,long divisorL)
    {
      String resultString;
      double perCentD= (100. * dividentL) / divisorL;
      if (0.5 > perCentD) 
        resultString= ""; // Was "00%";
      else if (99.5 <= perCentD)
        resultString= "99+";
      else
        resultString= String.format("%02d%%",Math.round(perCentD));
      return resultString;
      }
  
    static OSTime.DutyCycle directoryDutyCycle= new OSTime.DutyCycle();
    static OSTime.DutyCycle writingDutyCycle= new OSTime.DutyCycle();
    static OSTime.DutyCycle syncingDutyCycle= new OSTime.DutyCycle();
    static OSTime.DutyCycle closingDutyCycle= new OSTime.DutyCycle();
    static OSTime.DutyCycle readingDutyCycle= new OSTime.DutyCycle();
    static OSTime.DutyCycle deletingDutyCycle= new OSTime.DutyCycle();
    static String osReportString;
    static long osLastTimeNsL;
  
    static class DutyCycle
    /* This class is used to calculate the duty-cycle of operations.
     * It was created to do this for IO operations,
     * but it could be used for anything.
     * The code is based on the assumption of a single thread.
     */
    {
      private boolean operationIsActiveB= false;
      private long timeOfLastActivityChangeNsL;
      private long timeActiveNsL;
      private long timeInactiveNsL;
    
      public void updateActivityWithTrueV()
        {
          updateActivityV(true);
          }
    
      public void updateActivityWithFalseV()
        {
          updateActivityV(false);
          }
    
      public void updateActivityV(boolean isActiveB)
        {
          updateActivityV(isActiveB,System.nanoTime());
          }
    
      public void updateActivityV(boolean newActivityB,long timeNowNsL)
        {
          long timeSinceLastActivityChangeNsL= 
            timeOfLastActivityChangeNsL - timeNowNsL;
          if (operationIsActiveB)
            timeActiveNsL+= timeSinceLastActivityChangeNsL;
            else
            timeInactiveNsL+= timeSinceLastActivityChangeNsL;
          operationIsActiveB= newActivityB;
          timeOfLastActivityChangeNsL= timeNowNsL;
          }
    
      public String resetAndGetOSString(long timeNowNsL)
        {
          updateActivityV( // Adjust the proper total for present time.
              operationIsActiveB,timeNowNsL);
          long totalTimeSincePreviousReportNsL= timeActiveNsL + timeInactiveNsL;
          String resultString= quotientAsPerCentString(
              timeActiveNsL, totalTimeSincePreviousReportNsL);
    
          // Reset accumulators for next time period.
          timeActiveNsL= 0;
          timeInactiveNsL= 0;
    
          return resultString;
          }
    
      static String resetAndGetOSString(
        OSTime.DutyCycle theDutyCycle, String labelString, long timeNowNsL)
      /* Returns string representing OS%, or "" if % is 0.
       * It also resets for the next measurement.
       */
      {
        String resultString= theDutyCycle.resetAndGetOSString(timeNowNsL);
        if ("" != resultString) // % not 0
          resultString= labelString + resultString; // so append to label.
        return resultString;
        }
    
      } // DutyCycle

    }
