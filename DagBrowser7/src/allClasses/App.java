package allClasses;

import java.math.BigInteger;
import java.util.Random;

import static allClasses.Globals.appLogger;

public class App { // The App, especially pre-GUI stuff.

  AppFactory theAppFactory;
  Persistent thePersistent;
  Shutdowner theShutdowner;
  AppInstanceManager theAppInstanceManager;
  TCPCopier theTCPCopier;

  public App(   // Constructor.  For app creation.
      AppFactory theAppFactory,
      Persistent thePersistent,
      Shutdowner theShutdowner,
      AppInstanceManager theAppInstanceManager,
      TCPCopier theTCPCopier
      )
    {
  		this.theAppFactory= theAppFactory;
  	  this.thePersistent= thePersistent; 
      this.theShutdowner= theShutdowner;
      this.theAppInstanceManager= theAppInstanceManager;
      this.theTCPCopier= theTCPCopier;
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
  		appLogger.info("App.run() begins.");
  		thePersistent.initializeV();  // Prepare access to persistent data.
  	  definePeerIdentityV();
			theShutdowner.initializeV();

			theAppInstanceManager.initializeV();

			delegateOrDoV(); // Actually do some work.

      // App shutdown.
      appLogger.info("App.run() shutting down.");
      theAppInstanceManager.finalizeV();
  	  thePersistent.finalizeV();  // Write any new or changed app properties.
  		//appLogger.info("App calling Shutdowner.finishV().");
      theShutdowner.finishAppShutdownV();  // Doing final app shutdown jobs.
        // This might not return if shutdown began in the JVM. 
      appLogger.setBufferedModeV( 
          true ); // Because finishAppShutdownV() disables it for JVM exit. 
      appLogger.info("App.run() ends.");
      
      // After this method returns, the main thread of this app should exit.
      } // runV().


  private void definePeerIdentityV()
    /* This method creates a node identity number for this peer
      if it does not already exist.
      Though the id is 256 bits long, it does not have that much entropy.
      ///fix This is temporary.  This code will eventually be replaced.
     	*/
	  {
	    String nodeIdentyString= 
	    		thePersistent.getDefaultingToBlankString("PeerIdentity");
	    if ( ! nodeIdentyString.isEmpty() ) {
	    	  ; // Do nothing because identity is already defined.
	    	} else { // Define and store identity.
	    		Random theRandom= new Random();  // Construct random # generator.
          theRandom.setSeed( System.currentTimeMillis() ); // Seed with time.
	    		BigInteger identityBigInteger= new BigInteger(256, theRandom);
	    		thePersistent.putB("PeerIdentity", ""+identityBigInteger);
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
