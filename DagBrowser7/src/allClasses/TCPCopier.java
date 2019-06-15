package allClasses;

// The following code was based on code gotten from
// https://stackoverflow.com/questions/4687615/how-to-achieve-transfer-file-between-client-and-server-using-java-socket

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

import static allClasses.Globals.appLogger;


public class TCPCopier

  /* This class is supposed to be a simple software updater
    which can run in spite of other app malfunctions.
    It must be kept as simple as possible and
    it must rely on as few other modules as possible.
    
    It contains code for both client and server,
    which are terms meaningful only for making network connections.
    After a connection is made, all interactions are peer-to-peer
    and completely symmetrical. 
    
    The code is [to be] written so that if a file to be update is replaced,
    it will trigger an update even to the same node, i.e., localhost.
    This is done by defining 2 folders.
    They are different folders for testing.
    They are the same folder for production.
    
    There are 2 threads.
    * The server thread only services requests,
      and pauses for a while after each one to mitigate request spam.
    * The client thread attempts both 
    	* file updating across a socket connection and
    	* local inter-folder file updating.
		
    ///enh Eventually it should maintain its own data file.
      Any peers with which it communicates for updating may be added
      only by it and only after it tests them for operability.
      These peers can be supplied by by the connection manager
      when it discovers them.
    */

	{

	  /* File name and path definitions.
	    2 file names are used, one for the client and done for the server.
	    This is done to for testing with localhost and 2 separate folder.
	    During normal operations there will be only one folder and file name.
	   	*/
	  //private static final String fileToUpdateString = "TCPCopier.txt";
    private static final String fileToUpdateString= 
        AppSettings.initiatorNameString;
	  //private static final String serverFileString = "TCPCopierServer.txt";
	  //private static final String serverFileString = fileToUpdateString;
	  private static final String serverFileString = 
	  		Config.tcpCopierInputFolderString + File.separator + fileToUpdateString;
	  //private static final String clientFileString = "TCPCopierClient.txt";
	  private static final String clientFileString = // sub-folder and file.
	  		Config.tcpCopierOutputFolderString + File.separator + fileToUpdateString;


		/* Synchronization is used in 2 places:
			* Once to control shared access between the TCPClient and
			  another thread providing notification about a new connection.
			* Once to prevent TCPClient and TCPServer trying to 
			  communicate at the same time.
			*/
    private static final Object tcpCopyLockObject= new Object();
    private static final Object serverLockObject= tcpCopyLockObject;
    private static final Object clientLockObject= tcpCopyLockObject;
      // Presently a single lock object prevents 
      // some simultaneous client and server.
      // They would need to be different if doing local-only testing.

    // Random number generator.
		private static Random theRandom= new Random();
		static {
			theRandom.setSeed( System.currentTimeMillis() ); // Seed with time now.
			}

	  static class TCPClient extends EpiThread {

	  	private ConcurrentLinkedQueue<IPAndPort> peerQueueOfIPAndPort=
	  			new ConcurrentLinkedQueue<IPAndPort>();
	  			///opt ConcurrentLinkedQueue might be overkill 
	  			// given other synchronization in use.
	  			///org Maybe add Unicaster instead of IPAndPort?

		  private final Persistent thePersistent; // External data.
			
	    public TCPClient(  // Constructor.
	  			String threadNameString, Persistent thePersistent) 
	    {
				super(threadNameString);
	
			  this.thePersistent= thePersistent;
				}

	    public void run()
	      /* This is the main method of the Client thread.
	        After a delay and some initialization,
	        it calls a method to repeatedly attempt updates 
	        by contacting TCPServers.
	        */
	      {
          if (appLogger.testAndLogDisabledB( Config.tcpThreadsDisableB,
              "run() tcp client thread") 
              )
            return;

          appLogger.info("run() start delay beginning.");
		  		EpiThread.interruptibleSleepB(Config.tcpClientRunDelayMsL);
	  			appLogger.info("run() start delay done.");
		  		updateTCPCopyStagingAreaV();
          appLogger.debug("run() after staging area update attempt.");

	      	PersistentCursor thePersistentCursor= 
	      			new PersistentCursor( thePersistent );
	      	thePersistentCursor.setListV("peers");
	      	interactWithTCPServersV(thePersistentCursor);
	  			appLogger.info("run() ending.");
		    	}


	    private void interactWithTCPServersV(PersistentCursor thePersistentCursor)
	      /* This method repeatedly tries to connect to TCPServers
	        of various peer nodes.
	        When a connection is made, 
	        it executes the file update/exchange protocol.
	        If an app file can be updated then it will be updated.
	        It does this in a way that it is impossible for rogue nodes
	        to cause the hogging of resources.
	        It initially looks for a new connected peer
	        and tries to do updates with it first.
	        This is to reduce the debug cycle time.
	        Later new peers and saved peers are given approximately equal weight.

	        ///enh Eventually it needs to be very careful about always working,
	        because it will be used as a backup update mechanism.
	        Ideally it should respond quickly to a new connection,
	        which is probably one being used for testing and debugging,
	        but won't do it again until after pausing for several seconds.
	        Instead it will concentrate on saved peer IDs.
				  */
		    {
	    		long targetMsL= System.currentTimeMillis(); // Getting now time.
		    	while ( ! EpiThread.testInterruptB() ) // Repeat until exit requested.
		    		{ // Try one update.
		    			targetMsL+= Config.tcpClientPeriodMsL;
		    				///fix Make this immune to skipped time.
			        try {  // Wait for and process a queue entry up to period length.
			    			appLogger.debug("interactWithTCPServersV(..) from queued peers.");
								tryExchangingFilesWithServerFromQueueB(
										targetMsL - System.currentTimeMillis()
										); // This also serves as the main loop delay.
								appLogger.debug("interactWithTCPServersV(..) from saved peers.");
				    		tryExchangingFilesWithNextSavedServerV(thePersistentCursor);
				    		appLogger.debug(
				    				"interactWithTCPServersV(..) wait remainder period.");
				    		EpiThread.interruptibleSleepB( // Wait any remainder of period. 
				    				targetMsL - System.currentTimeMillis() );
					    	}
			        catch (InterruptedException e) { // Handling thread interrupt.
			          Thread.currentThread().interrupt(); // Reestablish it.
			          }
		      		}
			    }

	    private void updateTCPCopyStagingAreaV()
	      /* This method updates the app file 
	        from the standard folder to the TCP copy staging area folder.
	        It needs to be called only once, at app start. 
	       	*/
		    {
					File tcpFolderFile= AppSettings.makeRelativeToAppFolderFile( 
							Config.tcpCopierInputFolderString );
					tcpFolderFile.mkdir();  // Create destination folder if needed.
		  		Misc.updateFromToV( // Update staging area from standard folder.
		  		  AppSettings.makeRelativeToAppFolderFile(
		  		      Config.appString + AppSettings.initiatorExtensionString),
            new File( tcpFolderFile, AppSettings.initiatorNameString)
		    		);
			    }

			private boolean tryExchangingFilesWithServerFromQueueB(long maxWaitMSL)
	        throws InterruptedException
			  /* Tries to exchange files with next server peer node 
			    on the peer input queue.
			    It will wait for a maximum of maxWaitMSL milliseconds.
			    It returns true if it tried to process a peer from the queue,
			    false otherwise.
			   */
				{
				  IPAndPort theIPAndPort= waitForPeerIPAndPort( maxWaitMSL );
				  boolean gotPeerB= (theIPAndPort != null);
					if (gotPeerB) 
						{ // Try update.
							appLogger.info(
									"tryExchangingFilesWithServerFromQueueB() at "+theIPAndPort);
							String serverIPString= 
									 theIPAndPort.getInetAddress().getHostAddress(); 
							String serverPortString= 
									Integer.toString(theIPAndPort.getPortI());
							tryExchangingFilesWithServerV(serverIPString, serverPortString);
							///fix Add to saved peers.
							}
				  return gotPeerB;
					}

	    public synchronized void reportPeerConnectionV( 
	    		IPAndPort remoteIPAndPort )
	      /* This method adds remoteIPAndPort to the peer queue.
	        This is a way to learn about new peers which might be used as
	        sources or destinations of software updates.
	       	*/
		    {
					appLogger.debug( "reportPeerConnectionV(..): queuing peer." );
	    		peerQueueOfIPAndPort.add(remoteIPAndPort); // Add peer to queue.
			    notify(); // Wake up the TCPClient thread.
			    }

	    public synchronized IPAndPort waitForPeerIPAndPort(long maxWaitMSL)
	        throws InterruptedException
	      /* This method tests whether a peer 
	        is available in new connections queue.
	        Returns the next peer as soon as it is available.
	        If no peer is available for a maximum of maxWaitMSL,
	        then it returns null.
	        It also returns null if a spurious wake up or an interrupt happens
	        before a peer becomes available.
	        ///fix  rewrite as loop?
	       	*/
		    {
	    	  long targetMsL= System.currentTimeMillis()+maxWaitMSL;
	    	  IPAndPort resultIPAndPort;
	    	  while (true)
			    	{
			    	  resultIPAndPort= peerQueueOfIPAndPort.poll();
			        if ( resultIPAndPort != null) break; // Exit if got peer.
			        long waitMsL= // Calculate remaining time to wait. 
			        		targetMsL - System.currentTimeMillis();
				      if ( waitMsL <= 0 ) break; // Exit if time limit has been reached.
					    wait( waitMsL ); // Wait for time or notification, 
				        				// Interrupt will cause exception.
		    	  	}
	    		return resultIPAndPort;
			    }

			private void tryExchangingFilesWithNextSavedServerV( 
					PersistentCursor thePersistentCursor)
			  /* Tries to exchange files with next server peer node based on 
			    the state of thePersistentCursor into the peer list.
			    If there is an element saved in the list, it will be processed,
			    even if wrapping around to the beginning is necessary.
			   */
				{
				  if ( thePersistentCursor.getEntryKeyString().isEmpty() ) { 
			      ; // Do nothing because peer list must be empty.
				  	} else { // Process one peer list element.
							String serverIPString= 
									thePersistentCursor.getFieldString("IP");
							String serverPortString= 
									thePersistentCursor.getFieldString("Port");
				  		tryExchangingFilesWithServerV(serverIPString,serverPortString);
					  }
				  thePersistentCursor.nextWithWrapKeyString(); // Advance cursor.
					}
	
			private void tryExchangingFilesWithServerV(
					String serverIPString, String serverPortString)
			  /* Tries to exchange files with the peer node TCPServer 
			    that might or might not be listening 
			    at IPAddress serverIPString and at port serverPortString.
			    */
				{
						appLogger.debug(
								"tryExchangingFilesWithServerV()"
								+ ", serverIPString= " + serverIPString
								+ ", serverPortString= " + serverPortString);
		      	Socket clientSocket = null;
						File clientFile= 
						    AppSettings.makeRelativeToAppFolderFile( clientFileString );
						int serverPortI= Integer.parseUnsignedInt( serverPortString );
					 	InetSocketAddress theInetSocketAddress= null; 
						try {
							 	theInetSocketAddress= 
									new InetSocketAddress( serverIPString, serverPortI );
								appLogger.debug(
										"tryExchangingFilesWithServerV() theInetSocketAddress= "
										+ theInetSocketAddress);
								clientSocket= new Socket();
								appLogger.debug(
										"tryExchangingFilesWithServerV() before connect"
										+ ",\n  clientSocket= " + clientSocket
										+ ",\n  theInetSocketAddress= " + theInetSocketAddress);
								clientSocket.connect(  // Connect with time-out.
										theInetSocketAddress, Config.tcpConnectTimeoutMsI); 
								appLogger.debug(
										"tryExchangingFilesWithServerV() after successful connect"
										+ ",\n  clientSocket= " + clientSocket);
					  		long clientFileLastModifiedL= clientFile.lastModified();
					  		long resultL;
								synchronized (clientLockObject) { 
									appLogger.debug(
											"tryExchangingFilesWithServerV() begin synchronized block.");
						  		resultL= tryTransferingFileL(
						  			clientSocket, clientFile, clientFile, clientFileLastModifiedL );
									appLogger.debug(
											"tryExchangingFilesWithServerV() end synchronized block.");
								 	} // synchronized (clientLockObject)
								addPeerInfoV( serverIPString, serverPortString);
					  		if (resultL != 0)
									appLogger.info( 
											"tryExchangingFilesWithServerV() copied using"
											+ "\n  clientSocket= " + clientSocket);
					    } catch (IOException theIOException) {
								appLogger.info(
									"tryExchangingFilesWithServerV() error "+ theIOException);
					  	} finally {
							  Closeables.closeWithErrorLoggingB(clientSocket);
							}
					}

	    public void addPeerInfoV(String ipString, String portString)
	      /* Add to peer list the peer whose IP and port 
		      are ipString and portString respectively.
		      This should only be called when a TCP connection
		      has actually been made.
		      */
		    {	
	    		appLogger.debug( "TCPCopier..addPeerInfoV() called." );
	    		IPAndPort.addPeerInfoV(thePersistent, ipString, portString);
	    		} 
	
			} // TCPClient

		static class TCPServer extends EpiThread {
		
		  private File serverFile= null;
		  private final PortManager thePortManager; // External data.
      private ServerSocket serverServerSocket= null;
			
	    public TCPServer( // Constructor.
	  			String threadNameString, PortManager thePortManager) 
		    {
					super(threadNameString);
		
				  this.thePortManager= thePortManager;
					}
	
	    public void run()
		    /* This is the main method of the Server thread.
		      It repeatedly waits for client connections and processes them
		      by executing the file update/exchange protocol.
			    */
		    {
          if (appLogger.testAndLogDisabledB( Config.tcpThreadsDisableB,
            "run() tcp server thread"))
            return;

		  		appLogger.info("run() server start delay begins.");
		    	EpiThread.interruptibleSleepB(  // Delay to organize log and to give
		    			Config.tcpServerRunDelayMsL );  // connection advantage to client.
		  		appLogger.info("run() server start delay done.");
		    	while  // Repeatedly service one client request. 
		    		( ! EpiThread.testInterruptB() ) 
			    	{ 
			    		serviceOneRequestFromAnyClientV();
			    	  EpiThread.interruptibleSleepB(Config.tcpServerCyclePauseMsL); 
			    	    // Sleep to prevent [malicious] hogging.
			    		} // while...
		    	}
	
			private void serviceOneRequestFromAnyClientV()
			  /* This method waits for and processes one request from a client.
			    This might result in a file being send to the client,
			    a file being received from the client,
			    or no file transfered at all.
 			    */
				{
			    Socket serverSocket= null;
			  	try {
			        serverServerSocket= 
			        		new ServerSocket(thePortManager.getNormalPortI());
			    		appLogger.debug(
			    				"serviceOneRequestFromAnyClientV()() trying ServerSocket.accept() to "
			    				+ serverServerSocket);
			        serverSocket = serverServerSocket.accept();
							appLogger.debug(
									"serviceOneRequestFromAnyClientV() accepted connection from "
									+ serverSocket );
							synchronized (serverLockObject) 
			        	{ 
									appLogger.debug(
										"serviceOneRequestFromAnyClientV() begin synchronized block.");
									processServerConnectionV(serverSocket); 
									appLogger.debug(
											"serviceOneRequestFromAnyClientV() end synchronized block.");
									} 
			      } catch (IOException ex) {
			    		appLogger.info(ex, "serviceOneRequestFromAnyClientV()");
			      } finally {
			    		appLogger.info( "serviceOneRequestFromAnyClientV() closing begins.");
			      	Closeables.closeWithoutErrorLoggingB(serverSocket);
			      	Closeables.closeWithoutErrorLoggingB(serverServerSocket);
			    		appLogger.info( "serviceOneRequestFromAnyClientV() closing ends.");
			        }
					}

      private void processServerConnectionV( Socket serverSocket) 
      	throws IOException
	      {
		  		serverFile= // Calculating File name.
		  		    AppSettings.makeRelativeToAppFolderFile( serverFileString );
		  		long serverFileLastModifiedL= serverFile.lastModified();
		  		long resultL= tryTransferingFileL(
			  		serverSocket, serverFile, serverFile, serverFileLastModifiedL );
			  	if (resultL != 0)
						appLogger.info( "serviceOneRequestFromAnyClientV() copied using " 
								+serverSocket);
		      }

      public void stopV()
        {
          Closeables.closeWithoutErrorLoggingB(serverServerSocket);
            // Terminate possibly blocked ServerSocket.accept().
          super.stopV(); // Also signal termination desired.
          }

			} // TCPServer
	  
		private static long tryTransferingFileL(
				Socket theSocket,
				File localSourceFile, 
				File localDestinationFile, 
				long localLastModifiedL
				)
		  throws IOException
		  /* This method transfers a file through theSocket.
		    The direction depends on the file time-stamps on each end.
		    This node communicates with the peer on the other end of
		    the connection on theSocket and compares
		    the LastModefied TimeStamp of the remote file to localLastModifiedL
		    which should be the LastModefied TimeStamp of the local file.
		    If they are not equal then the newer file with its newer TimeStamp
		    is copied across the connection to replace the older file.

		    If a file is received then it replaces localDestinationFile and
		    this method returns the received file's newer time-stamp,
		    If a file is sent then it returns the negative of the remote time-stamp.
		    If a file is neither sent nor received,
		    because their time-stamps are equal, then 0 is returned. 
				This method is used by both the client and the server.

				localSourceFile and localDestinationFile are normally the same file.
				Using two parameters instead of one made earlier testing easier.
				Also localLastModifiedL is normally the time-stamp of that same file,
				and could be eliminated, but exists, again, to make testing easier.
		   */
			{
				appLogger.info("tryTransferingFileL(..) beginning.");
			  long transferResultL= 0;
	      InputStream socketInputStream= null;
				OutputStream socketOutputStream = null;
		  	try {
		  		  theSocket.setSoTimeout( Config.tcpCopierTimeoutMsI );
		  			socketInputStream= theSocket.getInputStream();
			  		socketOutputStream= theSocket.getOutputStream();
		  			appLogger.info("tryTransferingFileL(..) before exchange...");
			  		long timeStampResultL= 
		      			TCPCopier.exchangeAndCompareFileTimeStampsRemoteToLocalL(
		      				socketInputStream, socketOutputStream, localLastModifiedL);
		  			appLogger.info("tryTransferingFileL(..) after exchange...");
						if ( timeStampResultL > 0 ) { // Remote file is newer.
				  			appLogger.info("tryTransferingFileL(..) Remote file is newer.");
								if ( receiveNewerRemoteFileB(
										localDestinationFile, socketInputStream, timeStampResultL ) )
									transferResultL= timeStampResultL;
						  } else if ( timeStampResultL < 0 ) { // Local file is newer.
				  			appLogger.info("tryTransferingFileL(..) Local file is newer.");
						  	sendNewerLocalFileV(
						  			localSourceFile, socketOutputStream );
								transferResultL= timeStampResultL;
						  } else { ; // Files are same age, so do nothing. 
				  			appLogger.info("tryTransferingFileL(..) Files are same age.");
						  }
						theSocket.shutdownOutput(); // Prevent reset at Socket close.
				} catch (IOException ex) {
			  		appLogger.info(ex, "tryTransferingFileL(..) aborted because of ");
			  		EpiThread.uninterruptibleSleepB( // Random delay of up to 2 seconds.
			  				theRandom.nextInt(2000)); ///fix make interruptible.
			  		appLogger.info("tryTransferingFileL(..) end of random delay.");
			    } finally {
			  		}
		  	if (transferResultL != 0)
		  		appLogger.info("tryTransferingFileL(..) exchanged using "+theSocket);
	  	  ///dbg appLogger.info("transactionV() ending.");
		  	return transferResultL;
	    	}
	
		private static void sendNewerLocalFileV(
				File localFile,
				OutputStream socketOutputStream
				)
			throws IOException
			/* This method sends the file localFile,
			  which should be newer that its remote counterpart,
			  over the socketOutputStream, to replace the remote counterpart.
			 	*/
		  ///fix close cleanly.
			{ // Local file newer.
				FileInputStream localFileInputStream= null;
				try { 
						appLogger.info("sendNewerLocalFileV() sending file "
								+ Misc.fileDataString(localFile));
						localFileInputStream= new FileInputStream(localFile);
						TCPCopier.copyStreamBytesB( 
								localFileInputStream, socketOutputStream);
					} finally {
						appLogger.info("sendNewerLocalFileV() sent file "
								+ Misc.fileDataString(localFile));
			  		Closeables.closeWithErrorLoggingB(localFileInputStream);
					}
				}
		
		private static boolean receiveNewerRemoteFileB(
				File localFile,
				InputStream socketInputStream,
				long timeStampToSetL
				)
			/* This method receives the remote counterpart of file localFile,
			  via socketInputStream. and replaces the localFile.
			  The new file has its TimeStamp set to timeStampToSet.
			  The above operations are done in a two-step process 
			  using an intermediate temporary file.
			  This method returns true if the file transfer completed, 
			  false otherwise.
			  
			  This method is longer than sendNewerLocalFileV(..) because
			  it must set the files LastModified value and do an atomic rename.

			 	*/
			{
			  appLogger.info("receiveNewerRemoteFileB() receiving file "
			  		+ Misc.fileDataString(localFile));
				FileOutputStream temporaryFileOutputStream= null;
				boolean fileWriteCompleteB= false;
				File temporaryFile= null;
				try { 
					temporaryFile= AppSettings.makeRelativeToAppFolderFile( 
							Config.tcpCopierOutputFolderString );
					temporaryFile.mkdir();  // Create folder if needed.
					temporaryFile= AppSettings.makeRelativeToAppFolderFile( 
							Config.tcpCopierOutputFolderString 
							+ File.separator + "Temporary.file" );
					temporaryFileOutputStream= new FileOutputStream( temporaryFile );
					fileWriteCompleteB= TCPCopier.copyStreamBytesB(
              socketInputStream, temporaryFileOutputStream);
  				} catch ( FileNotFoundException e ) {
  				  appLogger.exception("open failure", e);
					} finally {
						Closeables.closeWithErrorLoggingB(temporaryFileOutputStream);
				  }
				if (fileWriteCompleteB) { // More processing if write completed. 
						temporaryFile.setLastModified(timeStampToSetL); // Set time stamp.
						Path temporaryPath= temporaryFile.toPath(); //convert to Path.
						Path localPath = localFile.toPath(); //convert to Path.
						try {
  						Files.move( // Rename file, replacing existing file with same name.
  								temporaryPath, localPath, StandardCopyOption.REPLACE_EXISTING, 
  								StandardCopyOption.ATOMIC_MOVE);
  						} catch ( IOException e ) {
                appLogger.exception("rename failure", e);
              }
					  appLogger.info("receiveNewerRemoteFileB() received file "
					  		+ Misc.fileDataString(localFile));
					}
				return fileWriteCompleteB;
				}
	
		private static boolean copyStreamBytesB( 
				InputStream theInputStream, OutputStream theOutputStream)
		  /* This method copies all [remaining] file bytes
		    from theInputStream to theOutputStream.
		    The streams are assumed to be open at entry 
		    and they will remain open at exit.
		    It returns true if the copy of all data finished, 
		    false if it does not finish for any reason.
		    Thread.currentThread().interrupt() will interrupt the copy,
		    but the status will remain set after exit.

		    ///opt Copy more than 1 byte at a time.
		   	*/
			{
	      appLogger.info("copyStreamBytesV() begins.");
	      int byteCountI= 0;
	      boolean completedB= false;
	      try {
  	  		while (true) { // Copy bytes as long as possible. 
  	  		  int byteI;
  	      	byteI= theInputStream.read(); // Read byte. 
  	      	if ( byteI == -1 ) break; // -1 means end of stream, so exit.
  	      	theOutputStream.write(byteI); // Write byte.
  	      	byteCountI++;
  	      	if (EpiThread.testInterruptB()) break; // Exit if interrupted.
  	      	}
          completedB= true;
  	      appLogger.info(
  	      		"copyStreamBytesV() success, bytes transfered: " + byteCountI);
	        } 
	      catch (IOException theIOException) {
          appLogger.exception("copyStreamBytesV() failed",theIOException);
	        }
	      return completedB;
	      }
	  
		private static long exchangeAndCompareFileTimeStampsRemoteToLocalL( 
				InputStream socketInputStream, 
				OutputStream socketOutputStream, 
	  		long localLastModifiedL
				)
	    /* This method compares the time-stamps of the remote and local files.

	  		localLastModified is used as the time-stamp of the local file.
	  		It is also sent to the remote peer via socketOutputStream.
	  		The time-stamp of the remote file is received 
	  		from the remote peer via socketInputStream.

	      This method returns a value indicating the result of the comparison.
	  		If the returned value == 0 then the two files 
	  		have the same lastModified time-stamps and are equally old.
	  		If the returned value > 0 then the remote file is newer
	  		and the returned value is the remote time-stamp. 
	  		If the returned value < 0 then the local file is newer
	  		and the returned value is negative the value of the local time-stamp. 
	      
	      A file that does not exist is considered to have
	      a lastModified time-stamp of 0, which is considered infinitely old.
	      
	      ///fix Add time-out for receiving time-stamp from remote node.
	      */
		  throws IOException
			{
	  		appLogger.debug( 
	  				"exchangeAndCompareFileTimeStampsRemoteToLocalL() begins.");
	  		long remoteLastModifiedL= 0; // Initial accumulator value.
	  		{ // Send digits of local file time stamp and terminator to remote end.
	  			TCPCopier.sendDigitsOfNumberV(socketOutputStream, localLastModifiedL);
		  		socketOutputStream.write( (byte)('\n') );
		  		socketOutputStream.write( (byte)('#') );
	  			socketOutputStream.flush();
	  			}
	  		appLogger.debug( 
	  				"exchangeAndCompareFileTimeStampsRemoteToLocalL() "
	  				+"after sending digits.");
	  		{ // Receive and decode similar digits of remote file time stamp.
	    		remoteLastModifiedL= 0;
	    		int socketByteI= socketInputStream.read(); // Read first byte.
	  			char socketC;
	    		while (true) { // Accumulate all digits of remote file time-stamp.
	    			if (socketByteI==-1) break; // Exit if end of stream.
	    			socketC= (char)socketByteI;
	    			if (! Character.isDigit(socketC)) break; // Exit if not digit.
	    			remoteLastModifiedL= // Combine new digit with digit accumulator.  
	    					10 * remoteLastModifiedL + Character.digit(socketC, 10); 
	      		socketByteI= socketInputStream.read(); // Read next byte.
	      		}
	    		while (true) { // Skip characters through '#' or to end of input.
	    			if (socketByteI==-1) break; // Exit if end of stream.
	    			if (socketByteI==(int)'#') break; // Exit if terminator found. 
	      		socketByteI= socketInputStream.read(); // Read next byte.
	      		}
	  			}
	  		appLogger.debug( "exchangeAndCompareFileTimeStampsRemoteToLocalL() "
	  				+"after receiving digits.");
	  		long compareResultL= remoteLastModifiedL - localLastModifiedL;
	  		if (compareResultL > 0 ) 
	  			  compareResultL= remoteLastModifiedL;
	  			else if (compareResultL < 0 ) 
	  			  compareResultL= -localLastModifiedL;
		  			appLogger.info( // Log result of comparison.
		  				"exchangeAndCompareFileTimeStampsRemoteToLocalL() returning "
		  				+ ( ( compareResultL == 0 )
		  						? "0 meaning equal"
		  					  : "unequal: " + 
		  						  ( ( compareResultL >= 0 ) 
				  						? Misc.dateString( compareResultL ) 
				  				    : "-" + Misc.dateString( -compareResultL )
				  				    )
		  				    )
		  				+ ", localLastModifiedL=" + Misc.dateString(localLastModifiedL)
		  				+ ", remoteLastModifiedL=" + Misc.dateString(remoteLastModifiedL)
		  				);
    		appLogger.debug( "exchangeAndCompareFileTimeStampsRemoteToLocalL() ends.");
				return compareResultL;
				}
	
		private static void sendDigitsOfNumberV( 
					OutputStream socketOutputStream, long theL)
			  throws IOException
			  /* This recursive method sends decimal digits of the long number theL
			    to socketOutputStream.  The number always begins with a '0'.
			    
			    ///fix This will fail in 2038 when the 32-bit signed number overflows.
			   	*/
				{	
				  if ( theL == 0 ) { // Output final [leading] 0 if number is 0.
				  		socketOutputStream.write( (byte) ((byte)('0') + theL));
				  	} else { // Use recursion to output earlier other digits.
				  		TCPCopier.sendDigitsOfNumberV( socketOutputStream, theL / 10 );
				  		socketOutputStream.write( (byte) ( (byte)('0') + (theL % 10) ) );
				  	}
				}
	
	  }
