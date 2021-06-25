package allClasses;

import static allClasses.AppLog.theAppLog;


public class AppGUIFactory {  // For classes with GUI lifetimes.

  /* This is the factory for all classes with GUI scope.
    Most have lifetimes close to the AppGUI lifetime, but some are shorter.
    The app has a maximum of one instance of this factory.

    This factory wires together the 2nd level of the application.
    The classes constructed here are mainly the ones 
    used to present a GUI (Graphical User Interface) to the user,
    and must of the GUI content.

    There is only one AppGUI. 
    All of its code could have been put in the AppFactory,
    but a GUI is not always needed,
    so it made sense to divide the code between the App and GUI factory classes.

    ScheduledThreadPoolExecutor is used in this class.
    Unfortunately it appears that ScheduledThreadPoolExecutor disables
    some functionality of the ThreadPoolExecutor in the control of 
    the thread pool.  In the ScheduledThreadPoolExecutor,
    the pool size, the so-called core size, is fixed.
    It can not expand or contract as needed.

    ///enh It might be necessary to create a new class that uses 
    the ThreadPoolExecutor configured for a widely variable number of threads
    to provide a ScheduledThreadPoolExecutor-like class 
    that can provide a potentially large number of timer threads.
    */

  // Injected dependencies that need saving for later.
  //// private final Shutdowner theShutdowner;
  //// private final PortManager thePortManager;
  //// private final Persistent thePersistent;

	// Other objects that will be needed later.

  //// Many variables moved to AppFactory.
  
  public AppGUIFactory(  // Factory constructor.
  	  Persistent thePersistent,
  	  PortManager thePortManager,
  		Shutdowner theShutdowner,
  		AppInstanceManager theAppInstanceManager,
  		TCPCopier theTCPCopier
  		)
  	/* This method builds all objects that are, or comprise, 
  	   unconditional singletons.
  	   
       ///org Note, no non-static maker methods are called from here, otherwise 
         it could go undetected by the compiler 
         and result in NullPointerExceptions.
         Find a better way to organize this factory so that 
         the compiler will detect this type of error. 
       */
    {
      theAppLog.info("AppGUIFactory(.) entry.");

      theAppLog.info("AppGUIFactory(.) THIS NOW DOES NOTHING!");

      // Save injected dependencies needed for use by factory methods.
      //// this.theShutdowner= theShutdowner;
      //// this.thePortManager= thePortManager;
      //// this.thePersistent= thePersistent;

      // Much code was moved from here to AppFactory.

      theAppLog.info("AppGUIFactory(.) exit.");
      }

  // Conditional singleton getter methods and storage.
  // None.

  

  } // class AppGUIFactory.
