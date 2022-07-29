package allClasses;

import static allClasses.AppLog.theAppLog;

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
     * This class simply wraps ScheduledThreadPoolExecutor.  
     * It is used but is also a no-op.
     * It was decided to leave the class to hold this documentation,
     * and in case ScheduledThreadPoolExecutor monitoring is needed later.
     * 
     * ScheduledThreadPoolExecutor is used by extension.
     * Unfortunately it appears that ScheduledThreadPoolExecutor disables
     * some functionality of the ThreadPoolExecutor for controlling
     * the thread pool.  In the ScheduledThreadPoolExecutor,
     * the pool size, the so-called core size, is fixed.
     * It can not expand or contract as needed.
     * 
     * ///enh It might be necessary to create a new class that uses
     * the ThreadPoolExecutor configured for a widely variable number of threads
     * to provide a ThreadScheduler-like class
     * that can provide a potentially large number of timer threads.
     */

    public ThreadScheduler( // constructor
        int corePoolSize, RejectedExecutionHandler handler) 
      {
        super(corePoolSize, handler);
        }

    static public ThreadScheduler 
        // Single ThreadScheduler for many app threads and timers.
        theThreadScheduler= new ThreadScheduler(
            10, /// 5, // Fixed thread pool size.
            (theRunnable,theThreadPoolExecutor) -> { theAppLog.error(
                "ThreadScheduler rejected execution."); }
            ); // Minimum of 1 thread,

  }
