package allClasses;

import java.util.concurrent.ScheduledFuture;
import java.util.function.Supplier;

public class ProgressReport 
  {
  
    /* General ProgressReport code.
     * 
     * Presently this is only variables.
     * The methods that use them are in the class ConsoleBase. 
     *
     * ///ano ScheduledThreadPoolExecutor thread termination anomaly:
     * This anomaly turned out to be a bug.  
     * For more information, see class ThreadScheduler.
     * 
     */
    
    public boolean progressReportsEnabledB= true;
    public final long progressReportDefaultPollPeriodMsL= 250;
    public final long progressReportBackgroundPeriodMsL= 3000; /// 1000;
    public int progressReportHeadOffsetI= -1; /* -1 means report inactive. 
      This is the offset in the document of the progress report beginning.  */
    public int progressReportMaximumLengthI= -1;
    public long timeOfPreviousUpdateMsL;
    public ScheduledFuture<?> outputFuture;
    public Supplier<String> emptyProgressReportSupplierOfString= 
      () -> "!" ;
    public Supplier<String> theProgressReportSupplierOfString= 
      () -> "UNDEFINED PROGRESS-REPORT" ;

  
    }
