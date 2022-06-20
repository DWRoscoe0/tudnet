package allClasses;

public class OSTime 
  {
    /* This class is used to measure and report
     * the amount of time spent on various OS operations. 
     */

    static String getOsIoString()
      /* This method builds and returns a String representing
       * the percentage amounts of time spent in various OS operations.
       */
      {
        long timeNowNsL= System.nanoTime();
        if  // If 1/2 second has passed
          ( 500000 <= (timeNowNsL - lastReportTimeNsL) ) 
          { // accumulate report String.
            thisReportTimeNsL= timeNowNsL;
            osTotalTimeActiveMsL= 0;
            reportAccumulatorString= "";
            DutyCycle.accumulateOSTimeReportV(directoryDutyCycle, " dir:");
            DutyCycle.accumulateOSTimeReportV(deletingDutyCycle, " del:");
            DutyCycle.accumulateOSTimeReportV(closingDutyCycle, " clo:");
            DutyCycle.accumulateOSTimeReportV(writingDutyCycle, " wrt:");
            DutyCycle.accumulateOSTimeReportV(readingDutyCycle, " rea:");
            DutyCycle.accumulateOSTimeReportV(syncingDutyCycle, " syn:");
            reportAccumulatorString= 
                "\nOS:"
                + quotientAs3CharacterPercentString(
                    osTotalTimeActiveMsL,thisReportTimeNsL-lastReportTimeNsL)
                + " :"
                + reportAccumulatorString;
            lastReportTimeNsL= thisReportTimeNsL; // Record time for next run.
            }
        return reportAccumulatorString;
        }
  
    static String nonZeroQuotientAsPerCentString(long dividendL,long divisorL)
      {
        String resultString;
        if (0 >= dividendL)
          resultString= ""; // We don't bother showing if 0%.
          else 
          resultString= quotientAs3CharacterPercentString(dividendL,divisorL);
        return resultString;
        }
  
    static String quotientAs3CharacterPercentString(
        long dividendL,long divisorL)
      {
        double perCentD= 100. * (double)dividendL / divisorL;
        String resultString;
        if (0.5 > perCentD) // Lower boundary case.
          resultString= "00%";
        else if (99.5 <= perCentD) // Upper boundary case.
          resultString= "100";
        else // All other cases.
          resultString= String.format("%02d%%",Math.round(perCentD));
        return resultString;
        }
  
    static DutyCycle directoryDutyCycle= new DutyCycle();
    static DutyCycle writingDutyCycle= new DutyCycle();
    static DutyCycle syncingDutyCycle= new DutyCycle();
    static DutyCycle closingDutyCycle= new DutyCycle();
    static DutyCycle readingDutyCycle= new DutyCycle();
    static DutyCycle deletingDutyCycle= new DutyCycle();
    static long osTotalTimeActiveMsL;
    static String reportAccumulatorString;
    static long lastReportTimeNsL;
    static long thisReportTimeNsL;


    static class DutyCycle
      /* This class is used to calculate the duty-cycle of operations.
       * It was created to do this for IO operations,
       * but it could be used for anything.
       * The code is based on the assumption of a single thread.
       */
      {
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
               timeNowNsL - timeOfLastActivityChangeNsL;
            if (operationIsActiveB)
              timeActiveNsL+= timeSinceLastActivityChangeNsL;
              else
              timeInactiveNsL+= timeSinceLastActivityChangeNsL;
            operationIsActiveB= newActivityB;
            timeOfLastActivityChangeNsL= timeNowNsL;
            }

        static void accumulateOSTimeReportV(
          DutyCycle theDutyCycle, String labelString)
        /* Returns string representing OS%, or "" if % is 0.
         * It also resets for the next measurement.
         */
        {
          String resultString= theDutyCycle.resetAndGetOSString(thisReportTimeNsL);
          if ("" != resultString) // % not 0
            resultString= labelString + resultString; // so append to label.
          reportAccumulatorString+= resultString;
          }

        public String resetAndGetOSString(long timeNowNsL)
          {
            updateActivityV( // Adjust appropriate accumulator for present time.
                operationIsActiveB,timeNowNsL);
            osTotalTimeActiveMsL+= timeActiveNsL;
            long totalTimeSincePreviousReportNsL= 
                timeActiveNsL + timeInactiveNsL;
            String resultString= nonZeroQuotientAsPerCentString(
                timeActiveNsL, totalTimeSincePreviousReportNsL);
      
            // Reset both accumulators for next time period.
            timeActiveNsL= 0;
            timeInactiveNsL= 0;
      
            return resultString;
            }

        private boolean operationIsActiveB= false;
        private long timeOfLastActivityChangeNsL;
        private long timeActiveNsL;
        private long timeInactiveNsL;

        } // DutyCycle


    }
