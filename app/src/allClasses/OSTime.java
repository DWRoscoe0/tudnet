package allClasses;


public class OSTime 
  {
    /* This class does 2 thing related to time spent in operating system APIs:
     * * It measures and reports the percentage of time in 
     *   particular API methods.
     * * It detects and reports excessive time spent in particular API methods.
     *
     * ///ano Excessive Operating System API Time Anomaly:
     *   During testing of the Volume-Checker feature,
     *   the app seemed to run very slowly sometimes.
     *   Sometimes the slowing was severe enough to render the app useless.
     *   So code was added to measure, detect, and report exc\essive time
     *   spent in operating system API methods.
     *   
     *   The following is a snapshot of code from 2022-07-10
     *   showing limits needed to prevent reports of excessive API times.
     *   
      static DutyCycle directoryDutyCycle= new DutyCycle("Directory-Read",4000);
      static DutyCycle writingDutyCycle= new DutyCycle("Data-Write",4404);
      static DutyCycle syncingDutyCycle= new DutyCycle("File-Sync",81000);
      static DutyCycle closingDutyCycle= new DutyCycle("Close",83);
      static DutyCycle readingDutyCycle= new DutyCycle("Data-Read",193);
      static DutyCycle deletingDutyCycle= new DutyCycle("File-Delete",50000);
     * 
     *   The limits are expressed in milliseconds.
     *   The limits shown were produced by gradually increasing them
     *   until no API method times were reported as excessive.
     *   As you can see, several of these times were multiple seconds,
     *   though none of them should be.
     * 
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
            DutyCycle.accumulateOSTimeReportV(
                directoryDutyCycle, " files-list:");
            DutyCycle.accumulateOSTimeReportV(readingDutyCycle, " read:");
            DutyCycle.accumulateOSTimeReportV(writingDutyCycle, " write:");
            DutyCycle.accumulateOSTimeReportV(syncingDutyCycle, " sync:");
            DutyCycle.accumulateOSTimeReportV(closingDutyCycle, " close:");
            DutyCycle.accumulateOSTimeReportV(deletingDutyCycle, " delete:");
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

    /* ///ano Each limit should be only a few milliseconds without anomalies.
     * The following constructor maximumTimeMsL values are from trial-and-error.
     */
    static DutyCycle directoryDutyCycle= new DutyCycle("Directory-Read",4000);
    static DutyCycle writingDutyCycle= new DutyCycle("Data-Write",16300);
    static DutyCycle syncingDutyCycle= new DutyCycle("File-Sync",114000);
    static DutyCycle closingDutyCycle= new DutyCycle("Close",86);
    static DutyCycle readingDutyCycle= new DutyCycle("Data-Read",193);
    static DutyCycle deletingDutyCycle= new DutyCycle("File-Delete",50000);

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
            long timeSinceLastActivityUpdateNsL= 
               timeNowNsL - timeOfLastActivityUpdateNsL;
            if (operationIsActiveB)
              timeActiveNsL+= timeSinceLastActivityUpdateNsL;
              else
              timeInactiveNsL+= timeSinceLastActivityUpdateNsL;
            operationIsActiveB= newActivityB;
            timeOfLastActivityUpdateNsL= timeNowNsL;
            doLimitCheckV(timeNowNsL);
            }

        private void doLimitCheckV(long timeNowNsL)
          /* This method tests whether the activity maximum time limit
           * has been exceeded.  If it has then it reports it.
           * This method should be called often enough
           * to give the user timely reports.
           */
          {
            if (limitedPeriodIsActiveB)
              Anomalies.testAndDisplayDialogReturnString(
                  nameString,
                  operationIsActiveB ? "ACTIVE" : "Done", 
                  maximumTimeMsL, // maximum time for operation
                  (timeNowNsL - activePeriodStartTimeNsL) / 1000000
                  );
            if (limitedPeriodIsActiveB != operationIsActiveB)
              {
                limitedPeriodIsActiveB= operationIsActiveB;
                activePeriodStartTimeNsL= timeNowNsL;
                }
            }

        private DutyCycle(String nameString,long maximumTimeMsL) // constructor.
          {
            super();
            this.nameString= nameString;
            this.maximumTimeMsL= maximumTimeMsL;
            }

        private String nameString;
        private long maximumTimeMsL= 1500; // Default value, to be overridden.
        private boolean limitedPeriodIsActiveB= false;
        private long activePeriodStartTimeNsL;
        private boolean operationIsActiveB= false;
        private long timeOfLastActivityUpdateNsL;
        private long timeActiveNsL;
        private long timeInactiveNsL;

        } // DutyCycle


    }
