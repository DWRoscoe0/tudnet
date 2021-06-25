package allClasses;

import static allClasses.AppLog.theAppLog;


public class AppGUIFactory {  // For classes with GUI lifetimes.

  // Documentation moved to AppFactory.
  
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
