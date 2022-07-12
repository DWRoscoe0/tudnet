package allClasses;

import static allClasses.AppLog.theAppLog;


public class Closeables 

  {
  
    /* This class contains static methods that provide several ways 
     * of closing resources that implement the AutoCloseable interface.
     *   
     * The methods provide 
     * 1 SOME of the convenience of the Java try-with-resources statement, and
     * 2 the ability to monitor time spent in the OS API close method.
     * 
     * The methods differ in:
     * * Whether they do error logging.
     * * Whether and how they handle Exceptions that happen during the close.
     * * Whether and how they handle a null reference to the AutoCloseable.
     *
     * Note that AutoCloseable, even though it came later, 
     * was made a super interface of Closeable, for back-compatibility.
  
      ///enh: Maybe make these methods more orthogonal,
         maybe by using a new lowest-level buck-stops-here method, 
         and having other methods call it, a method such as:
           static boolean closeB(
             boolean nullIsErrorB,
             boolean reportErrorsB
             AutoCloseable theAutoCloseable
             )B

      ///enh: maybe add methods which take an array... of AutoCloseables
        instead of a single AutoCloseable.

      ///enh: maybe add methods which return errors as a String.

       */
   
    public static void closeAndReportNothingV(
        AutoCloseable theAutoCloseable)
      /* This method closes theAutoCloseable if it's not null.
       * It does not report a null theAutoCloseable or close errors. 
       */
      {
        closeWithOptionsV(theAutoCloseable,
            false, // Don't report null. 
            false // Don't report close errors.
            );
        }
  
    public static void closeAndReportErrorsV(
        AutoCloseable theAutoCloseable)
      /* This method closes theAutoCloseable if it's not null.
       * It does not report a null theAutoCloseable 
       * but does report close errors. 
       */
      {
        closeWithOptionsV(theAutoCloseable, 
            false, // Don't report null. 
            true // Report close errors.
            );
        }
  
    private static void closeWithOptionsV(
        AutoCloseable theAutoCloseable,
        boolean reportNullsB,
        boolean reportExceptionsB
        )
      /* This method is for closing resources.
        It is private, but is called by other local public methods.
        It reports when theAutoCloseable is null if reportNullsB is true.
        It reports close exceptions if reportExceptionsB is true.
        */
      {
        if (theAutoCloseable == null) {
            if (reportNullsB) 
              theAppLog.error(
                "Closeables.closeWithOptionsV(.): null AutoCloseable resource");            
        } else { // theAutoCloseable != null
            try { 
                closeV(theAutoCloseable);
              } catch (Exception theException) {
                if (reportExceptionsB) 
                  theAppLog.exception(
                      "Closeables.closeWithOptionsV(.): ", theException);            
              }
          }
        }
  
    @SuppressWarnings("unused") ///
    private static Exception closeAccumulateAndReturnException(
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
              closeV(theAutoCloseable);
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

    public static void closeV(AutoCloseable theAutoCloseable)
        throws Exception
      /* This method attempts to close theAutoCloseable.
       * It works like the regular close() method.
       * It passes on any Exception that the close() method throws.
       * 
       * The only difference is that this method also monitors 
       * time spent in the operating system doing the close operation.
       * 
       * This method may be used in place of close() 
       * if handling exceptions locally is desired.  However,
       * it might be necessary to catch Exception instead of IOException.
       */
      {
        try { 
            OSTime.closingDutyCycle.updateActivityWithTrueV(); // Monitor on.
            theAutoCloseable.close();
          } finally { // Do this regardless of exceptions.
            OSTime.closingDutyCycle.updateActivityWithFalseV(); // Monitor off.
          }
        }

    }
