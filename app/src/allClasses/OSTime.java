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
      /* 
       * This method returns a String representing the result of
       * dividing dividendL by divisorL.
       * The value is represented as follows:
       * * "" means exactly 0^
       * * "00%" to "99%" and "100" for other values.
       * 
       */
      {
        String resultString;
        if (0 >= dividendL)
          resultString= ""; // We don't bother showing if exactly 0%.
          else 
          resultString= quotientAs3CharacterPercentString(dividendL,divisorL);
        return resultString;
        }
  
    static String quotientAs3CharacterPercentString(
        long dividendL,long divisorL)
      /* 
       * This method returns a String representing the result of
       * dividing dividendL by divisorL.
       * The value is represented by the values
       * "00%" to "99%" and "100".
       * 
       */
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
  
    static DutyCycle directoryDutyCycle= new DutyCycle("Directory-Read");
    static DutyCycle writingDutyCycle= new DutyCycle("Data-Write");
    static DutyCycle syncingDutyCycle= new DutyCycle("File-Sync");
    static DutyCycle closingDutyCycle= new DutyCycle("Close");
    static DutyCycle readingDutyCycle= new DutyCycle("Data-Read");
    static DutyCycle deletingDutyCycle= new DutyCycle("File-Delete");
    static long osTotalTimeActiveMsL;
    static String reportAccumulatorString;
    static long lastReportTimeNsL;
    static long thisReportTimeNsL;


    static class DutyCycle
      /* This class is used to calculate the duty-cycle of operations.
       * It was created to do this for IO operations,
       * but it could be used for any operation with a beginning and an ending.
       * The code is based on the assumption of a single thread.
       */
      {
        static void accumulateOSTimeReportV(
          DutyCycle theDutyCycle, String labelString)
        /* Calculates a String representing percentage activity
         * for theDutyCycle using laabelString as the label,
         * and appends it to reportAccumulatorString.
         * It also resets for the next activity measurement.
         */
        {
          String measurementString= 
              theDutyCycle.resetAndGetOSString(thisReportTimeNsL);
          if ("" != measurementString) // If got non-0 measurement,
            measurementString= labelString+measurementString; // prepend label.
          reportAccumulatorString+= measurementString;
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
          /* This method updates variables based on 
           * newActivity and the time timeNowNsL.
           * This method should be called whenever activity has changed or
           * an activity report is being generated.
           */
          {
            long timeSinceLastActivityChangeNsL= 
               timeNowNsL - timeOfLastActivityChangeNsL;
            if (operationIsActiveB)
              timeActiveNsL+= timeSinceLastActivityChangeNsL;
              else
              timeInactiveNsL+= timeSinceLastActivityChangeNsL;
            operationIsActiveB= newActivityB;
            timeOfLastActivityChangeNsL= timeNowNsL;
            doLimitCheckV();
            }

        private void doLimitCheckV()
          /* This method tests whether the activity maximum time limit
           * has been exceeded.  If it has then it reports it.
           * This method should be called often enough
           * to give the user timely reports.
           */
          {
            Anomalies.testAndDisplayDialogReturnString(
                nameString,
                operationIsActiveB ? "ACTIVE" : "Complete", 
                1500, // temporary 1.5 second limit on all operations.
                timeActiveNsL/1000000 // convert ns to ms
                );
            }

        private DutyCycle(String nameString) // constructor.
          {
            super();
            this.nameString= nameString;
            }

        private String nameString;
        private boolean operationIsActiveB= false;
        private long timeOfLastActivityChangeNsL;
        private long timeActiveNsL;
        private long timeInactiveNsL;

        } // DutyCycle


    }
