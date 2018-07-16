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

		//// private static final String testServerIPString = "127.0.0.1";
	  //// private static final int testServerPortI = 11111;
	  
	  /* File name and path definitions.
	    2 file names are used, one for the client and done for the server.
	    This is done to for testing with localhost and 2 separate folder.
	    During normal operations there will be only one folder and file name.
	   	*/
	  //private static final String fileToUpdateString = "TCPCopier.txt";
	  private static final String fileToUpdateString= Config.appJarString;
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
	  			appLogger.info("run() client start delay beginning.");
		  		EpiThread.interruptableSleepB(Config.tcpClientRunDelayMsL);
	  			appLogger.info("run() client start delay done.");

		  		updateTCPCopyStagingAreaV();
	      	PersistentCursor thePersistentCursor= 
	      			new PersistentCursor( thePersistent );
	      	thePersistentCursor.setListNameV("peers");
	      	interactWithTCPServersV(thePersistentCursor);
	  			appLogger.info("run() ending.",true);
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
		    	while ( ! EpiThread.exitingB() ) // Repeat until exit requested.
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
				    		EpiThread.interruptableSleepB( // Wait any remainder of period. 
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
	       	*/
		    {
					File thatFolderFile= Config.makeRelativeToAppFolderFile( 
							Config.tcpCopierInputFolderString );
					thatFolderFile.mkdir();  // Create destination folder if needed.
		  		Misc.updateFromToV( // Update staging area from standard folder.
						Config.userAppJarFile,
						new File( thatFolderFile, Config.appJarString )
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
				  if ( thePersistentCursor.getElementIDString().isEmpty() ) { 
			      ; // Do nothing because peer list must be empty.
				  	} else { // Process peer list element.
							String serverIPString= 
									thePersistentCursor.getFieldString("IP");
							String serverPortString= 
									thePersistentCursor.getFieldString("Port");
				  		tryExchangingFilesWithServerV(serverIPString,serverPortString);
					  }
				  thePersistentCursor.nextWithWrapElementIDString(); // Advance cursor.
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
								Config.makeRelativeToAppFolderFile( clientFileString );
						//// serverPortString= "11111"; ///tmp
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
	      /* Add peer list the peer whose IP and port are ipString and portString.
	        */
		    {	
	    		String peerIDString= ipString+"-"+portString; // Calculate ID string.
	    		
			  	// Store or update the list structure.
	    		String entryIDKeyString= thePersistent.entryInsertOrMoveToFrontString( 
	    				peerIDString, "peers" 
	    				);

	    	  // Store or update the other fields.
	    		thePersistent.putV( // IP address.
	    				entryIDKeyString + "IP", ipString
	    				);
	    		thePersistent.putV( // Port.
	    				entryIDKeyString + "Port", portString
	    				);
	    		} 
	
			} // TCPClient

		static class TCPServer extends EpiThread {
		
		  private File serverFile= null;

		  private final PortManager thePortManager; // External data.
			
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
		  		appLogger.info("run() server start delay begins.");
		    	EpiThread.interruptableSleepB(  // Delay to organize log and to give
		    			Config.tcpServerRunDelayMsL );  // connection advantage to client.
		  		appLogger.info("run() server start delay done.");
		    	while  // Repeatedly service one client request. 
		    		( ! EpiThread.exitingB() ) 
			    	{ 
			    		serviceOneRequestFromAnyClientV();
			    	  EpiThread.interruptableSleepB(Config.tcpServerCyclePauseMsL); 
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
			    ServerSocket serverServerSocket= null;
			    Socket serverSocket= null;
			  	try {
			        serverServerSocket= 
			        		new ServerSocket(thePortManager.getNormalPortI());
			        		//// new ServerSocket( 11111 ); ///tmp
			    		appLogger.debug(
			    				"serviceOneRequestFromClientV()() trying ServerSocket.accept() to "
			    				+ serverServerSocket);
			        serverSocket = serverServerSocket.accept();
							appLogger.debug(
									"serviceOneRequestFromClientV() accepted connection from "
									+ serverSocket );
							synchronized (serverLockObject) 
			        	{ 
									appLogger.debug(
										"serviceOneRequestFromClientV() begin synchronized block.");
									processServerConnectionV(serverSocket); 
									appLogger.debug(
											"serviceOneRequestFromClientV() end synchronized block.");
									} 
			      } catch (IOException ex) {
			    		appLogger.info( "serviceOneRequestFromClientV() using "
			    				+serverServerSocket, ex );
			      } finally {
			    		appLogger.info( "serviceOneRequestFromClientV() closing begins.");
			      	Closeables.closeWithErrorLoggingB(serverSocket);
			      	Closeables.closeWithErrorLoggingB(serverServerSocket);
			    		appLogger.info( "serviceOneRequestFromClientV() closing ends.");
			        }
					}

      private void processServerConnectionV( Socket serverSocket) 
      	throws IOException
	      {
		  		serverFile= // Calculating File name.
		  				Config.makeRelativeToAppFolderFile( serverFileString );
		  		long serverFileLastModifiedL= serverFile.lastModified();
		  		long resultL= tryTransferingFileL(
			  		serverSocket, serverFile, serverFile, serverFileLastModifiedL );
			  	if (resultL != 0)
						appLogger.info( "serviceOneRequestFromClientV() copied using " 
								+serverSocket);
		      }

			} // TCPServer 
	  
		private static long tryTransferingFileL(
				Socket theSocket,
				File localSourceFile, 
				File localDestinationFile, 
				long localLastModifiedL
				)
		  throws IOException
		  /* This method communicates with the peer on the other end of
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
				This method is called by both the client and the server.
		    
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
			  		appLogger.info("tryTransferingFileL(..) aborted because of ",ex);
			  		EpiThread.uninterruptableSleepB( // Random delay of up to 2 seconds.
			  				theRandom.nextInt(2000)); ///fix make interruptable.
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
						TCPCopier.copyStreamBytesV( 
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
			throws IOException
			/* This method receives the remote counterpart of file localFile,
			  via socketInputStream. and replaces the localFile.
			  The new file has its TimeStamp set to timeStampToSet.
			  It does this in a two-step process using an intermediate temporary file.
			  Returns true if the file transfer completed, false otherwise.
			  
			  This method is longer than sendNewerLocalFileV(..) because
			  it must set the files LastModified value and do an atomic rename.
			  
			  ///org Don't return value.  Use IOException to indicate failure.
			  
			 	*/
			{
			  appLogger.info("receiveNewerRemoteFileB() receiving file "
			  		+ Misc.fileDataString(localFile));
				FileOutputStream temporaryFileOutputStream= null;
				boolean fileWriteCompleteB= false;
				File temporaryFile= null;
				try { 
					temporaryFile= Config.makeRelativeToAppFolderFile( 
							Config.tcpCopierOutputFolderString );
					temporaryFile.mkdir();  // Create folder if needed.
					temporaryFile= Config.makeRelativeToAppFolderFile( 
							Config.tcpCopierOutputFolderString 
							+ File.separator + "Temporary.file" );
					temporaryFileOutputStream= new FileOutputStream( temporaryFile );
					TCPCopier.copyStreamBytesV( 
							socketInputStream, temporaryFileOutputStream);
					fileWriteCompleteB= true;
					} finally {
						Closeables.closeWithErrorLoggingB(temporaryFileOutputStream);
				  }
				if (fileWriteCompleteB) { // More processing if write completed. 
						temporaryFile.setLastModified(timeStampToSetL); // Set time stamp.
						Path temporaryPath= temporaryFile.toPath(); //convert to Path.
						Path localPath = localFile.toPath(); //convert to Path.
						Files.move( // Rename file, replacing existing file with same name.
								temporaryPath, localPath, StandardCopyOption.REPLACE_EXISTING, 
								StandardCopyOption.ATOMIC_MOVE);
					  appLogger.info("receiveNewerRemoteFileB() received file "
					  		+ Misc.fileDataString(localFile));
					}
				return fileWriteCompleteB;
				}
	
		private static void copyStreamBytesV( 
				InputStream theInputStream, OutputStream theOutputStream)
		  throws IOException
		  /* This method copies all [remaining] file bytes
		    from theInputStream to theOutputStream.
		    Any IOException encountered is re-thrown and terminate the copy.
		   	*/
			{
	      appLogger.info("copyStreamBytesV() beginning.");
	      int byteCountI= 0;
	  		while (true) { int byteI;
	      	try { byteI= theInputStream.read(); } // Read byte. 
			    catch (IOException theIOException) {
			    	appLogger.info("copyStreamBytesV() reading",theIOException);
			    	throw theIOException; // Re-throw.
			    	}
	  	  	///dbg appLogger.info("copyStreamBytesV() byteI="+byteI+" "+(char)byteI);
	      	if ( byteI == -1 ) break;
	      	try { theOutputStream.write(byteI); } // Write byte.
			    catch (IOException theIOException) { 
			    	appLogger.info("copyStreamBytesV() writing",theIOException);
			    	throw theIOException; // Re-throw.
			    	}
	      	byteCountI++;
	      	}
	      appLogger.info(
	      		"copyStreamBytesV() ended.  Bytes transfered: " + byteCountI);
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
	  			////if ( compareResultL != 0 ) // Log only if not 0. ///tmp
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
