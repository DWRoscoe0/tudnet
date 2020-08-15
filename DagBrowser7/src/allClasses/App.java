package allClasses;

import java.math.BigInteger;
import java.util.Random;

import static allClasses.AppLog.theAppLog;

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
  		theAppLog.info("App.run() begins.");
  		thePersistent.initializeV();  // Prepare access to persistent data.
  	  defineRootIdV();
			theShutdowner.initializeV();

			theAppInstanceManager.initializeV();

			delegateOrDoV(); // Actually do some work.

      // App shutdown.
      theAppLog.info("App.run() shutting down.");
      theAppInstanceManager.finalizeV();
  	  thePersistent.finalizeV();  // Write any new or changed app properties.
  		//appLogger.info("App calling Shutdowner.finishV().");
      theShutdowner.finishAppShutdownV();  // Doing final app shutdown jobs.
        // This might not return if shutdown began in the JVM. 
      theAppLog.setBufferedModeV( 
          true ); // Because finishAppShutdownV() disables it for JVM exit. 
      theAppLog.info("App.run() ends.");
      
      // After this method returns, the main thread of this app should exit.
      } // runV().


  private void defineRootIdV()
    /* This method creates a RootId number value for this peer,
      unless that value has already been created.
      This is done quickly to get a node operational quickly.
      It does not require any user interaction.
      
      ///enh Though the ID is 256 bits long, it does not contain that much entropy.
      This is not a problem now, but when the rate of node creation increases,
      ID collisions will become possible, even probable.
      At that time, it will be necessary to collect more entropy. 
      It might be possible to collect one-time entropy from user file metadata or data,
      or from the time needed to access files on mass storage. 
      
      A more sophisticated system for entropy collection will be needed
      for generating the ID (private and public keys) for a user, because
      * This might not be a one-time operation
      * It requires user interaction anyway for saving a copy of 
        the private key.
     	*/
	  {
	    String nodeIdentyString= 
	    		thePersistent.getEmptyOrString(Config.userIdString);
	    if ( ! nodeIdentyString.isEmpty() ) {
	    	  ; // Do nothing because identity is already defined.
	    	} else { // Define and store identity.
	    		Random theRandom= new Random();  // Construct random # generator.
          theRandom.setSeed( System.currentTimeMillis() ); // Seed with time.
	    		BigInteger identityBigInteger= new BigInteger(256, theRandom);
	    		thePersistent.putV(Config.userIdString, ""+identityBigInteger);
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
