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
  		//appLogger.info("App beginning.");
  		thePersistent.initializeV();  // Prepare access to persistent data.
  	  defineNodeIdentityV();
			theTCPServer.startV();
			theTCPClient.startV();
			theShutdowner.initializeV();
			theAppInstanceManager.initializeV();

			doAppStuff(); // This runs until an app shutdown is underway.

  	  thePersistent.finalizeV();  // Write any new or changed app properties.
  		//appLogger.info("App calling Shutdowner.finishV().");
      theShutdowner.finishAppShutdownV();  // Doing final app shutdown jobs.

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

  private void doAppStuff()
    /* This method checks with theAppInstanceManager.
      If theAppInstanceManager wants to terminate the app then it returns immediately.
      Otherwise it starts the app GUI and does user and network interactions
      until a shutdown is requested, then it returns.
      */
  	{
		  if ( ! theAppInstanceManager.managingInstancesWithExitB( ) ) 
		
		    { // Presenting GUI to user and interacting.
		  	  AppGUIFactory theAppGUIFactory= 
		  	  		theAppFactory.lazyGetAppGUIFactory();
		      AppGUI theAppGUI= theAppGUIFactory.getAppGUI();
		      theAppGUI.runV(); // Running GUI until finished.
		      	// Network operations happen at this time also.
		      }
	  	}

  } // class App.
