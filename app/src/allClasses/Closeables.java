package allClasses;

import static allClasses.AppLog.theAppLog;

/*///////////////////////
 * Add note about Closeable vs. AutoCloseable (more of a tag interface).  
 */

public class Closeables 

  /* This class contains static methods which do helpful things with 
    AutoCloseable resources.
    
    The methods provide several ways of closing resources that differ in:
    * Whether they do error logging.
    * How they handle Exceptions that might happen during the close.
    * Whether they accept a null reference to an AutoCloseable.
    
  ! Though these methods do manual closing of resources,
    they are passed as AutoCloseable references.  This was done because:
    * Manual closing is needed to provide monitoring of time in the OS API.
    * Some newer resource classes, such as Stream<T>, implement AutoCloseable.
    * AutoCloseable was made a superclass of Closeable for back-compatibility.

    The question of which method to use is difficult to answer.  
    It depends on several factors:
    * Whether the call is being made from an exception catch block,
      or is the close being done after an error-free operation? 
    * The type of resource being closed.
    * Is the resource read-only?  If true, no data will be lost in the resource.
    * Is the resource writable?  If true, data might be lost in the resource,
      and maybe the resource should be deleted afterward.

    ///enh: Maybe make these methods more orthogonal,
       maybe by using a new lowest-level buck-stops-here method, 
       and having other methods call it, a method such as:
         static boolean closeB(
           boolean nullI\sErrorB,
           boolean reportErrorsB
           AutoCloseable theAutoCloseable
           )B
           
    ///enh: maybe add methods which take an array... of AutoCloseables
      instead of a single AutoCloseable.

     */
 
  {

  public static boolean closeWithoutErrorLoggingB(AutoCloseable theAutoCloseable)
    /* This method is for closing a resource with a minimum of fuss.
      It doesn't even do logging of errors.

      This method does nothing if theAutoCloseable == null.
      Otherwise it closes theClosable but does not log any exception.

      It returns false if either theAutoCloseable is null or there was an exception,
      true otherwise.
      */
    {
      return closeAndWarnMaybeB(theAutoCloseable, false);
      }

  public static boolean closeWithErrorLoggingB(AutoCloseable theAutoCloseable)
    /* This method is for closing a resource with a minimum of fuss.
      It assumes that if an exception occurs during the close,
      then simply logging that exception is sufficient handling.
      theAutoCloseable==null is considered an error and is also logged.

      It returns false if either theAutoCloseable is null 
      or there was an exception, true otherwise.

      It does not log a successful close.
      */
    {
      return closeAndWarnMaybeB(theAutoCloseable, true);
      }

  public static boolean closeWithLoggingIfNotNullB(
      AutoCloseable theAutoCloseable)
    /* This method does nothing and returns true if theAutoCloseable is null.

      If theAutoCloseable is NOT null then it closes theAutoCloseable,
      and logs any exception that occurs during the close.
      It returns false if there was an exception during the close, 
      true otherwise.
      */
    {
      boolean successB= true;
      if (theAutoCloseable != null)
        successB= closeAndWarnMaybeB(theAutoCloseable, true);
      return successB;
      }

  public static boolean closeAndWarnMaybeB(
      AutoCloseable theAutoCloseable, boolean logB) ////////// warnAboutErrorsB?
    /* This method is for closing resources.
      It also logs an error that occurs if logB is true.
      It returns true if the close completes without error, 
      false if there was an error, meaning either 
      theAutoCloseable reference was null or there was an exception.
      
      This method can be used in the finally clause of a block
      with logB= the success of the code above.  This is because:
      * If that code was successful, theAutoCloseable should be non null
        and be in a condition to close without errors.
      * Otherwise there was an error, and errors during closing may be ignored.
      
      */
    {
      boolean successB= true;
      if (theAutoCloseable == null) {
          if (logB) 
            theAppLog.error(
              "closeAndWarnMaybeB(..): null closeble resource pointer.");            
          successB= false;
        } else {
          try { 
              theAutoCloseable.close();
            } catch (Exception theException) {
              if (logB) 
                theAppLog.exception("closeAndWarnMaybeB(..): ", theException);            
              successB= false;
            }
        }
      return successB;
      }

  @SuppressWarnings("unused") ///
  private static Exception closeAndAccumulateException(
        AutoCloseable theAutoCloseable, Exception earlierException)
    /* This method is for closing a resource but retaining
      the ability to detect and process exceptions during the close.
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
            theAutoCloseable.close(); 
          } catch (Exception newException) {
            theAppLog.exception(
                "closeAndAccumulateException(..): ", newException
                );
            if ( earlierException == null ) // Create first exception if none.
              earlierException= new Exception( "while closing" );
            earlierException.addSuppressed(newException);
          }
      return earlierException;
      }

    }
