package allClasses;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledThreadPoolExecutor;

public class ThreadScheduler extends ScheduledThreadPoolExecutor
  {
    /* ///ano This class was added to help analyze and eventually fix 
     * a ScheduledThreadPoolExecutor thread termination anomaly.
     * It appeared that the threads provided by the method
     *   ScheduledThreadPoolExecutor.scheduleAtFixedRate(.)
     * which was being used to produce VolumeChecker progress reports,
     * would sometimes terminate for no known reason.
     * The problem was intermittent.  Sometimes all worked well, sometimes not. 
     *
     * Eventually it was discovered that the anomaly was a bug 
     * and was being caused by a combination of 2 things:
     * * A divide-by-zero ArithmeticException was occurring in 
     *   the VolumeChecker.speedString() method.  That method divided by
     *     (timeNowMsL - passStartTimeMsL)
     *   which was sometimes 0 ms.  
     *   This was changed to prevent divide-by-zero.
     * * The threads provided by ScheduledThreadPoolExecutor
     *   ignored the Exception and terminated immediately.
     *   The exception was also not handled by the DefaultExceptionHandler
     *   that was set for this purpose. 
     *   try-catch blocks were added to report these Exceptions.
     *
     *   This class simply wraps ScheduledThreadPoolExecutor.  
     *   It is used but is also a no-op.
     *   It was decided to leave the class to hold this documentation,
     *   and in case ScheduledThreadPoolExecutor monitoring is needed later.
     */

    public ThreadScheduler( // constructor
        int corePoolSize, RejectedExecutionHandler handler) 
      {
        super(corePoolSize, handler);
        }
    
  
}
