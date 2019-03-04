package allClasses;

import java.util.Random;

import static allClasses.Globals.appLogger;

public class App { // The App, especially pre-GUI stuff.

  AppFactory theAppFactory;
  Persistent thePersistent;
  Shutdowner theShutdowner;
  AppInstanceManager theAppInstanceManager;
  TCPCopier.TCPServer theTCPServer;
  TCPCopier.TCPClient theTCPClient;

  public App(   // Constructor.  For app creation.
      AppFactory theAppFactory,
      Persistent thePersistent,
      Shutdowner theShutdowner,
      AppInstanceManager theAppInstanceManager,
      TCPCopier.TCPServer theTCPServer,
      TCPCopier.TCPClient theTCPClient
      )
    {
  		this.theAppFactory= theAppFactory;
  	  this.thePersistent= thePersistent; 
      this.theShutdowner= theShutdowner;
      this.theAppInstanceManager= theAppInstanceManager;
      this.theTCPServer= theTCPServer;
      this.theTCPClient= theTCPClient;
      }

  public void runV()  // This is for app Running.
    /* This method does any executable instance management it can.
      If the instance manager says it's okay then
      it presents the GUI to the user and interacts with him or her.
      At the same time normal network operations are done.
      When it's time to exit it uses the Shutdowner to
      do any final shutdown jobs.
     */
    {
      // App initialization.
  		//appLogger.info("App beginning.");
  		thePersistent.initializeV();  // Prepare access to persistent data.
  	  defineNodeIdentityV();
			theTCPServer.startV();
			theTCPClient.startV();
			theShutdowner.initializeV();
			theAppInstanceManager.initializeV();

			delegateOrDoV(); // Actually do some work.

      // App shutdown.
  	  thePersistent.finalizeV();  // Write any new or changed app properties.
  		//appLogger.info("App calling Shutdowner.finishV().");
      theShutdowner.finishAppShutdownV();  // Doing final app shutdown jobs.
        // This might not return if shutdown began in the JVM. 
      appLogger.setBufferedModeV( 
          true ); // Because finishAppShutdownV() disables it for JVM exit. 
  		appLogger.info("App exiting.");
      
      // After this method returns, the main thread of this app should exit.
      } // runV().


  private void defineNodeIdentityV()
    /* This method creates a node identity number for this peer
      if it does not already exist.
     	*/
	  {
	    String nodeIdentyString= 
	    		thePersistent.getDefaultingToBlankString("NodeIdentity");
	    if ( ! nodeIdentyString.isEmpty() ) {
	    	  ; // Do nothing because identity is already defined.
	    	} else { // Define and store identity.
	    		Random theRandom= new Random();  // Construct random # generator.
	    		theRandom.setSeed( System.currentTimeMillis() ); // Seed with time.
	    		int skipCountI= 8 + theRandom.nextInt(8);
	    		while ( --skipCountI >= 0 ) // Randomly skip 8 to 16 generated values. 
	    			theRandom.nextInt();
	    		int identityI= theRandom.nextInt();
	    		thePersistent.putB("NodeIdentity", ""+identityI);
	    	}
	  	}

  private void delegateOrDoV()
    /* This method checks with theAppInstanceManager,
      trying to delegate actions to another app instance.
      If delegation succeeds then its work is done and it exits.
      If delegation fails then it starts and runs the GUI
      to interact with the user and do various other things
      until a shutdown is requested, then it exits.
      */
  	{
		  if ( theAppInstanceManager.tryDelegatingToAnotherAppInstanceB() )
		    ; // Delegation succeeded.  Do nothing except exit.
		    else
		    { // Delegation failed.  Presenting GUI to user and interacting.
		  	  AppGUIFactory theAppGUIFactory= 
		  	  		theAppFactory.lazyGetAppGUIFactory();
		      AppGUI theAppGUI= theAppGUIFactory.getAppGUI();
		      theAppGUI.runV(); // Running GUI until it has shut down.
		      	// Network operations happen at this time also.
		      }
	  	}

  } // class App.
