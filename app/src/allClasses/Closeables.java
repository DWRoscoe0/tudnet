package allClasses;

import static allClasses.AppLog.theAppLog;


public class Closeables 

  {
    /* This class contains static methods that provide several ways 
     * of closing resources that implement the AutoCloseable interface.
     *   
     * The methods provide 
     * 1 SOME of the convenience of the Java try-with-resources statement, and
     * 2 the ability to monitor time used by the OS API close method
     *   for progress reports and over limit detection.
     * 
     * The methods differ in:
     * * Whether they report close() Exceptions.
     * * Whether they report null AutoCloseable references.
     * * Whether they report excessive time used by the close().
     * * Whether they handle or throw close() Exceptions.
     *
     * Note that AutoCloseable, even though it came later, 
     * was made a super interface of Closeable, for back-compatibility.

      ///enh: maybe add methods which take an array... of AutoCloseables
        instead of a single AutoCloseable.

      ///enh: maybe add methods which return errors as a String.

     */


    /* The following methods close AutoCloseable resources,
     * but handling and-or reports close() Exceptions.  
     * It does not throw them.
     */

    public static void closeV(AutoCloseable theAutoCloseable)
      {
        closeControlledByOptionsV(theAutoCloseable,
            false, // Don't report null. 
            false, // Don't report close Exceptions.
            false // Don't report over time limit.
            );
        }

    public static void closeAndReportTimeUsedV(AutoCloseable theAutoCloseable)
      {
        closeControlledByOptionsV(theAutoCloseable,
            false, // Don't report null. 
            false, // Don't report close Exceptions.
            true // Report over time limit.
            );
        }

    public static void closeAndReportTimeUsedAndExceptionsV(
        AutoCloseable theAutoCloseable)
      {
        closeControlledByOptionsV(theAutoCloseable, 
            false, // Don't report null. 
            true, // Report close Exceptions.
            true // Report over time limit.
            );
        }

    private static void closeControlledByOptionsV(
        AutoCloseable theAutoCloseable,
        boolean reportNullsB,
        boolean reportExceptionsB,
        boolean reportOverLimitB
        )
      /* This method is for closing resources with several options.
        It is private, but is called by other local public methods.
        It reports when theAutoCloseable is null if reportNullsB is true.
        It reports close exceptions if reportExceptionsB is true.
        It reports excessive close time exceptions if reportOverLimitB is true.
        */
      {
        if (theAutoCloseable == null) {
            if (reportNullsB) 
              theAppLog.error(
                "Closeables.closeWithOptionsV(.): null AutoCloseable resource");            
        } else { // theAutoCloseable != null
            try {
                if (reportOverLimitB)
                  closeAndReportTimeUsedAndThrowExceptionsV(theAutoCloseable);
                  else
                  closeAndThrowExceptionsV(theAutoCloseable);
              } catch (Exception theException) {
                if (reportExceptionsB) 
                  theAppLog.exception(
                      "Closeables.closeWithOptionsV(.): ", theException);            
              }
          }
        }
  
    @SuppressWarnings("unused") ///
    private static Exception closeAndAccumulateAndReturnException( // Unused.
          AutoCloseable theAutoCloseable, Exception earlierException)
      /* This method is for closing a resource but retaining
        the ability to detect and process exceptions during the close.
        It is presently unused and should be considered untested.
  
        If earlierException is not null then it contains an exception
        that has already been occurred, and any exception during
        the requested close() is added to it as a suppressed exception.
        If earlierException is null and a close exception happens,
        then a new exception will be constructed and assigned to
        earlierException and the close exception will be added to it 
        as a suppressed exception.  
        Any number of suppressed exceptions may be added.
  
        This method is meant to be called from the finally block,
        which is where resources are recommended to be closed.
  
        The possibly modified value of earlierException is returned.
  
        ///enh Use a special WhileClosingException instead of Exception.
  
        ///enh This method is for Exceptions associated with close(),
          but it might be generalized, with generics, for others Throwables.
        */
      {
        if (theAutoCloseable != null)
          try { 
              OSTime.closingDutyCycle.updateActivityWithTrueV();
              closeAndReportTimeUsedAndThrowExceptionsV(theAutoCloseable);
            } catch (Exception newException) {
              theAppLog.exception(
                  "closeAndAccumulateException(..): ", newException
                  );
              if ( earlierException == null ) // Create first exception if none.
                earlierException= new Exception( "while closing" );
              earlierException.addSuppressed(newException);
            } finally { // Do this regardless of exceptions.
              OSTime.closingDutyCycle.updateActivityWithFalseV();
            }
        return earlierException;
        }


    /* The following methods also close AutoCloseable resources,
     * but instead of handling and-or reporting close() Exceptions, 
     * it throws them.
     */

    public static void closeAndReportTimeUsedAndThrowExceptionsV(
          AutoCloseable theAutoCloseable)
        throws Exception
      {
        try { 
            OSTime.closingDutyCycle.updateActivityWithTrueV(); // Monitor on.
            closeAndThrowExceptionsV(theAutoCloseable);
          } finally { // Do this regardless of exceptions.
            OSTime.closingDutyCycle.updateActivityWithFalseV(); // Monitor off.
          }
        }

    public static void closeAndThrowExceptionsV(AutoCloseable theAutoCloseable)
        throws Exception
      {
        theAutoCloseable.close();
        }

    }
