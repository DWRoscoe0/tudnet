package allClasses;

//import static allClasses.Globals.*;  // appLogger;

public class LockAndSignal  // Combination lock and signal class.
  /* This class serves the following two roles:
    * As a monitor-lock for Object.wait(..) and Object.notify(), 
    * As the associated boolean signal variable.

    Threads can use these objects to synchronize 
    the passing of data between them.
    The LockAndSignal class can be used by itself,
    for example to signal boolean conditions such as task completion,
    or integrated within other classes such as the SignallingQueue class
    to signal the arrival of new input data.

    The doNotify() method is used by source threads
    to indicate to destination threads the readiness of new input.

    The doWait...() methods are called by destination threads
    to wait until new input has been provided by source threads.

    This class was created because neither the boolean primitive nor 
    the wrapper Boolean class could not be used for the two roles.
    The wrapper Boolean is immutable.  Assuming aBoolean == false, then
    "aBoolean= true;" is equivalent to "aBoolean= new Boolean(true);", 
    which changes the object referenced by aBoolean,
    which causes an IllegalMonitorStateException.
    This class solves this problem by using
    methods to access an internal mutable boolean value.
    
    ??? It might be worthwhile to use an object reference, or a null,
    instead of a boolean, to be the signal.
    Additional versions of the doWait() and doNotify() methods
    could pass these values.  This would allow passing of
    more complex information as if through a single element queue,
    but only from one specific object to another.
    */
  {

    public enum Input {  // Return value indicating input which ended a wait.
      NOTIFICATION, // doNotify() was called.
      TIME, // The time limit, either time-out or absolute time, was reached.
      INTERRUPTION // The thread's isInterrupted() status is true.
      }  // These return values can save input decoding time.
      
    private boolean signalB= false;  // Signal storage and default value.

    LockAndSignal( boolean aB )  // Constructor.
      { setV( aB ); }

    public Input doWaitE()
      /* This method, called by the source thread,
        waits for any of the following:
          * An input signal.
          * The condition set by Thread.currentThread().interrupt().
            This condition is not cleared by this method.
        It returns an Input value indicating why the wait ended.
        */
      {
        return doWaitWithTimeOutE( 
          Long.MAX_VALUE  // Effectively an infinite time-out.
          );
        }

    public Input doWaitWithTimeOutE( long delayMillisL )
      /* This method, called by the source thread,
        waits for any of the following:
          * An input signal.
          * The condition set by Thread.currentThread().interrupt().
            This condition is not cleared by this method.
          * A time at (delayMillisL) milliseconds in the future.
        It returns an Input value indicating why the wait ended.
        */
      {
        return doWaitUntilE( 
          System.currentTimeMillis() +
          delayMillisL
          );
        }

    public synchronized Input doWaitUntilE(long realTimeMillisL)
      /* This method, called by the source thread,
        waits for any of the following:
          * An input signal.
          * The condition set by Thread.currentThread().interrupt().
            This condition is not cleared by this method.
          * The time realTimeMillisL.
        It returns an Input value indicating why the wait ended.

        Change to use await() instead of wait() ???
        */
      {
        Input theInput;  // For type of Input that ended wait.
        
        while (true) { // Looping until any of several conditions is true.
          if ( getB() ) // Handling input signal present.
            { theInput= Input.NOTIFICATION; break; } // Exiting loop.
          long timeToNextJobMillisL= // Converting the real-time to a delay.
            realTimeMillisL - System.currentTimeMillis();
          if ( !( timeToNextJobMillisL > 0) )  // Handling time-out expired.
            { theInput= Input.TIME; break; } // Exiting loop.
          if // Handling thread interrupt signal.
            ( Thread.currentThread().isInterrupted() )
            { theInput= Input.INTERRUPTION; break; }  // Exiting loop.
          try { // Waiting for notification or time-out.
            wait(  // Wait for input notification or...
              timeToNextJobMillisL  // ...time of next scheduled job.
              );
            } 
          catch (InterruptedException e) { // Handling wait interrupt.
            Thread.currentThread().interrupt(); // Re-establishing for tests.
            }
          }
        //appLogger.debug( 
        //  Thread.currentThread().getName() + " doWaitUntilE(..) "+theInput 
        //  );

        setV(false);  // Resetting input signal for next time.
          // ??? Maybe this should be within only NOTIFICATION branch?

        return theInput;  // Return reason the wait ended.
        }

    public synchronized void doNotifyV()
      /* This method is called by threads which are data sources.
        It signals that new input has been provided 
        to the destination thread by the source thread.
        It is called by the source thread.
        */
      {
        setV(true);  // Set signal to end doWait() loop.
        notify();  // Terminate any active wait() blocking.
        }

    private boolean getB()  // Get mutable boolean value.
      { return signalB ; }

    private void setV( boolean aB )  // Set mutable boolean value.
      { signalB= aB; }

    }
