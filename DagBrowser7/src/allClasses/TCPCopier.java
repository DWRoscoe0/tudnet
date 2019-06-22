package allClasses;

// The following code was based on code gotten from
// https://stackoverflow.com/questions/4687615/how-to-achieve-transfer-file-between-client-and-server-using-java-socket

import java.io.*;
import java.net.*;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

import static allClasses.Globals.appLogger;


public class TCPCopier extends EpiThread

  /* This class is supposed to be a simple software updater
    which can run in spite of other app malfunctions.
    It must be kept as simple as possible and
    it must rely on as few other modules as possible.
    
    It contains code for both client and server,
    which are terms meaningful only for making network connections.
    After a connection is made, all interactions are peer-to-peer
    and completely symmetrical. 
    
    The code is [to be] written so that if a file to be updated is replaced,
    it will trigger an update even to the same node, i.e., localhost.
    This is done by defining 2 folders.
    They are different folders for testing.
    They are the same folder for production.
    
    There are presently 2 threads, but these are being replaced by one.
    * The server thread only services requests,
      and pauses for a while after each one to mitigate request spam.
    * The client thread attempts both 
    	* file updating across a socket connection and
    	* local inter-folder file updating.
    	
    The new single-thread system will work as follows.
    * On startup it will update the TCP staging area if the app file is new.
    * It will enter a loop which alternates between client and server modes.
      * In client modethread attempts both 
        * file updating across a socket connection and
        * local inter-folder file updating.
		
    ///enh Eventually it should maintain its own data file.
      Any peers with which it communicates for updating may be added
      only by it and only after it tests them for operability.
      These peers can be supplied by by the connection manager
      when it discovers them.
    */

	{

    private final Persistent thePersistent; // External data.

	  /* File name and path definitions.
	    2 file names are used, one for the client and one for the server.
	    This is done to for testing with localhost and 2 separate folder.
	    During normal operations there will be only one folder and file name.
	   	*/
	  //private static final String fileToUpdateString= "TCPCopier.txt";
    private static final String fileToUpdateString=
        AppSettings.initiatorNameString;
	  //private static final String serverFileString= "TCPCopierServer.txt";
	  //private static final String serverFileString= fileToUpdateString;
	  private static final String serverFileString=
	  		Config.tcpCopierInputFolderString + File.separator + fileToUpdateString;
	  //private static final String clientFileString= "TCPCopierClient.txt";
	  private static final String clientFileString= // sub-folder and file.
	  		Config.tcpCopierOutputFolderString + File.separator + fileToUpdateString;

    private ConcurrentLinkedQueue<IPAndPort> peerQueueOfIPAndPort=
        new ConcurrentLinkedQueue<IPAndPort>();
        ///opt ConcurrentLinkedQueue might be overkill 
        // given other synchronization in use.
        ///org Maybe add Unicaster instead of IPAndPort?

    private ServerSocket serverServerSocket= null;
    private Socket serverSocket= null;
    private Socket clientSocket = null;
    private final PortManager thePortManager; // External data.
    private File serverFile= null;

		/* Synchronization is used in 2 places:
			* Once to control shared access between the TCPClient and
			  another thread providing notification about a new connection.
			* Once to prevent TCPClient and TCPServer trying to 
			  communicate at the same time.
			*/

    PersistentCursor thePersistentCursor= null;

    // Random number generator.
		private static Random theRandom= new Random();
		static {
			theRandom.setSeed( System.currentTimeMillis() ); // Seed with time now.
			}

    public TCPCopier(  // Constructor.
        String threadNameString, 
        Persistent thePersistent, 
        PortManager thePortManager) 
    {
      super(threadNameString);

      this.thePersistent= thePersistent;
      this.thePortManager= thePortManager;
      }

    public void run()
    /* This is the main method of the thread.
      After a delay and some initialization,
      it alternates between roles of client and server.

      The method will return early if the thread is interrupted.
      */
    {
      if (appLogger.testAndLogDisabledB( Config.tcpThreadsDisableB,
          "run() TCPCopier thread") 
          )
        return;

      EpiThread.interruptibleSleepB(Config.tcpCopierRunDelayMsL);
      appLogger.info("run() start delay done.");
      updateTCPCopyStagingAreaV();
      appLogger.debug("run() after staging area update attempt.");

      thePersistentCursor= new PersistentCursor( thePersistent );
      thePersistentCursor.setListV("peers");
      loopAlternatingRolesV();
      appLogger.info("run() ending.");
      }

    public void stopV()
      /* This is a subclass method stopV() or stopping the thread.
        In addition to interrupting the thread in the usual way,
        it closes the Sockets and the ServerSocket which might be blocked.
        */
      {
        super.stopV(); // Try termination the default way.
        
        try { // Also close the server ServerSocket 
            if (serverServerSocket!= null) serverServerSocket.close();
          } catch (Exception theException) {
            appLogger.exception("TCPCopier.stopV()(..): ", theException);            
          }
        Closeables.closeWithErrorLoggingB(clientSocket); // and client Socket
        Closeables.closeWithErrorLoggingB(serverSocket); // and server Socket.
        appLogger.info("TCPCopier.stopV(): closes done.");
        }

    private void updateTCPCopyStagingAreaV()
      /* This method updates the app file 
        from the standard folder to the TCP staging area folder.
        It needs to be called only once, at app start. 

        The method will return early if the thread is interrupted.
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

    private void loopAlternatingRolesV()
      /* This method loops until thread termination is requested.
        In that loop it alternately
        * acts as a client, by trying to connect to a peer 
          to do an update with it, or
        * acts as a server, by accepting connections from other peers
          to do an update with each of them.
        It starts acting as a client.
        It spends most of its time as a server waiting for connections.

        The method will return early if the thread is interrupted.
        */
      {
        while ( ! EpiThread.testInterruptB() ) // Repeat until exit requested.
          {
            actAsAClientB();
            actAsAServerB();
            EpiThread.interruptibleSleepB(Config.antiCPUHogLoopDelayMsL); 
              // Brief sleep to prevent [malicious] hogging.
            }
        }

    private boolean actAsAClientB()
      /* This tries to connect to a TCPServer.
        If a connection is made, 
        it executes the file update/exchange protocol.
        If an app file can be updated then it will be updated.
        It does this in a way that it is impossible for rogue nodes
        to cause the hogging of resources.
        It initially looks for a new connected peer from the connection queue.
        If that fails, it tries to use the next saved peer.

        The method will return early if the thread is interrupted.
        */
      { 
        boolean successB= true; // Assume something will succeed.
        toReturn: {
          if (tryExchangeWithServerFromQueueB()) break toReturn;
          if (tryExchangeWithServerFromPersisentDataB()) break toReturn;
          successB= false; // All failed.  Return failure.
          } // toReturn:
        return successB; 
        }

    private boolean tryExchangeWithServerFromQueueB()
      /* Tries to exchange files with next server peer node 
        on the peer input queue.
        It will wait for a maximum of maxWaitMSL milliseconds.
        It returns true if an update file was exchanged, false otherwise.

        The method will return early if the thread is interrupted.
        */
      {
          boolean successB= false;
        toReturn: {
          IPAndPort theIPAndPort= peerQueueOfIPAndPort.poll();
          if ( theIPAndPort == null) break toReturn; // Exit if no peer.
          String serverIPString= 
               theIPAndPort.getInetAddress().getHostAddress(); 
          String serverPortString= 
              Integer.toString(theIPAndPort.getPortI());
          long resultL= 
              tryExchangingFilesWithServerL(serverIPString, serverPortString);
          if (resultL == 0) break toReturn; // No file transfered.
          successB= true; // File was transfered.
          } // toReturn:
        appLogger.info("tryExchangeWithServerFromQueueB() successB="+successB);
        return successB;
        }
      
    private boolean tryExchangeWithServerFromPersisentDataB()
      /* Tries to exchange files with next server peer node based on 
        the state of thePersistentCursor into the peer list.
        If there is an element saved in the list, it will be processed,
        even if wrapping around to the beginning is necessary.
        Returns true if a file was transfered, false otherwise.

        The method will return early if the thread is interrupted.
        */
      {
          boolean successB= false;
        toReturn: {
          if ( thePersistentCursor.getEntryKeyString().isEmpty() )
            break toReturn; // Do nothing because peer list is empty.
          String serverIPString= thePersistentCursor.getFieldString("IP");
          String serverPortString= thePersistentCursor.getFieldString("Port");
          long resultL= 
              tryExchangingFilesWithServerL(serverIPString,serverPortString);
          if (resultL == 0) break toReturn; // No file data was transfered.
        } // toReturn:
          thePersistentCursor.nextWithWrapKeyString(); // Advance cursor.
          return successB;
        }

    private long tryExchangingFilesWithServerL(
        String serverIPString, String serverPortString)
      /* Tries to exchange files with the peer node TCPServer 
        that might or might not be listening 
        at IPAddress serverIPString and at port serverPortString.

        If a file is received then it replaces localDestinationFile and
        this method returns the received file's newer time-stamp,
        If a file is sent then it returns the negative of the remote time-stamp.
        If a file is neither sent nor received,
        because their time-stamps are equal, or because of an error,
        then 0 is returned. 

        This method is used by both the client and the server.

        The method will return early if the thread is interrupted.
        */
      {
        long resultL= 0;
        appLogger.debug(
              "tryExchangingFilesWithServerV()"
              + ", serverIPString= " + serverIPString
              + ", serverPortString= " + serverPortString);
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
            resultL= tryTransferingFileL(
              clientSocket, clientFile, clientFile, clientFileLastModifiedL );
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
        return resultL;
        }
    
    private boolean actAsAServerB()
      /* This method is presently a do-nothing routine that 
        does nothing except call tryServicingOneRequestFromAnyClientB().

        This method waits for and tries for a maximum of 
        tcpServerMaximumWaitMsL to process one request from a client.
        This might result in an update file being send to the client,
        an update file being received from the client,
        or no file transfered at all.
        If no request is received within a limited amount of time
        then it gives up.
        It returns true if a file was transfered, false otherwise.
        The method will return early if the thread is interrupted.
        */
      { 
        appLogger.info("actAsAServerB() begins.");
        boolean successB= true; // Assume something will succeed.
        successB= tryServicingOneRequestFromAnyClientB();
        appLogger.info("actAsAServerB() ends.");
        return successB; 
        }

    private boolean tryServicingOneRequestFromAnyClientB()
      /* This method waits for and processes one request from a client.
        This might result in a file being sent to the client,
        a file being received from the client,
        or no file transfered at all.
        It returns true if a request was processed, 
        false if not for any reason.
        The method will return early if the thread is interrupted.
        */
      {
        boolean successB= false;
        try {
            serverServerSocket= 
                new ServerSocket(thePortManager.getNormalPortI());
            serverServerSocket.setSoTimeout( Config.tcpCopierTimeoutMsI );
            appLogger.debug(
                "serviceOneRequestFromAnyClientV()() trying ServerSocket.accept() to "
                + serverServerSocket);
            serverSocket = serverServerSocket.accept();
            appLogger.debug(
                "serviceOneRequestFromAnyClientV() accepted connection on "
                + serverSocket );
            processServerConnectionV(serverSocket); 
            successB= true; // Completed without thrown exceptions.
          } catch (SocketTimeoutException ex) { // Treat time-out as normal.
            ; // Do nothing.
          } catch (IOException ex) { // Handle thrown exceptions.
            appLogger.exception("serviceOneRequestFromAnyClientV()",ex);
          } finally {
            appLogger.info( "serviceOneRequestFromAnyClientV() closing begins.");
            Closeables.closeWithoutErrorLoggingB(serverSocket);
            Closeables.closeWithoutErrorLoggingB(serverServerSocket);
            appLogger.info( "serviceOneRequestFromAnyClientV() closing ends.");
            }
        return successB;
        }

    private void processServerConnectionV( Socket serverSocket) 
      throws IOException
      /* This method tries to transfer a newer update file
        through serverSocket based on the time stamps of
        the files on each end.  
        No transfer is tried if the time-stamps are equal.
        An IOException is thrown if an error occurs during the process.
        The method will return early if the thread is interrupted.
        */
      {
        serverFile= // Calculating File name.
            AppSettings.makeRelativeToAppFolderFile( serverFileString );
        long serverFileLastModifiedL= serverFile.lastModified();
        long resultL= tryTransferingFileL(
          serverSocket, serverFile, serverFile, serverFileLastModifiedL );
        if (resultL != 0)
          appLogger.info( "processServerConnectionV() copied using " 
              +serverSocket);
        }

    
    public void addPeerInfoV(String ipString, String portString)
      /* Adds to the Persistent peer list the peer whose IP and port 
        are ipString and portString respectively.
        This should only be called when a TCP connection
        has actually been made.
        */
      { 
        appLogger.debug( "TCPCopier..addPeerInfoV() called." );
        IPAndPort.addPeerInfoV(thePersistent, ipString, portString);
        } 

    public synchronized void queuePeerConnectionV( 
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
        This method returns true if the entire file transfer finished, 
        false otherwise.
        
        This method is longer than sendNewerLocalFileV(..) because
        it must set the files LastModified value and do an atomic rename.

        ///org Do complete rewrite using atomic rename, etc.
        */
      {
        appLogger.info("receiveNewerRemoteFileB() receiving file "
            + Misc.fileDataString(localFile));
        File tmpFile= null;
        FileOutputStream tmpFileOutputStream= null;
        boolean successB= false; // Assume we will not be successful.
        toReturn: {
          tmpFile= Misc.createTemporaryFile("TCPUpdate");
          if (tmpFile == null) break toReturn;
          try { tmpFileOutputStream= new FileOutputStream( tmpFile ); 
            } catch ( FileNotFoundException e ) {
              appLogger.exception("open failure", e);
              break toReturn;
            }
          if (!TCPCopier.copyStreamBytesB( // Copy stream to file or
              socketInputStream, tmpFileOutputStream))
            break toReturn; // terminate if failure.
          tmpFile.setLastModified(timeStampToSetL); // Set time stamp.
          if (!Misc.atomicRenameB(tmpFile.toPath(), localFile.toPath())) 
            break toReturn;
          successB= true; // Success because everything finished.
          } // toReturn:
        Closeables.closeWithErrorLoggingB(tmpFileOutputStream);
        Misc.deleteDeleteable(tmpFile); // Delete possible temporary debris.
        appLogger.info("receiveNewerRemoteFileB(..) successB="+successB);
        return successB;
        }
	
		private static boolean copyStreamBytesB( 
				InputStream theInputStream, OutputStream theOutputStream)
		  /* This method copies all [remaining] file bytes
		    from theInputStream to theOutputStream.
		    The streams are assumed to be open at entry 
		    and they will remain open at exit.
		    It returns true if the copy of all data finished, 
		    false if it does not finish for any reason.
		    A Thread interrupt will interrupt the copy.
		   	*/
			{
	      appLogger.info("copyStreamBytesV(..) begins.");
	      int byteCountI= 0;
	      boolean successB= false; // Assume we fill fail.
	      try {
          byte[] bufferAB= new byte[1024];
          int lengthI;
          while (true) {
            lengthI= theInputStream.read(bufferAB);
            if (lengthI <= 0) // Transfer completed.
              { successB= true; break; } // Indicate success and exit loop.
            theOutputStream.write(bufferAB, 0, lengthI);
            byteCountI+= lengthI;
            if (EpiThread.testInterruptB()) { // Thread interruption.
              appLogger.info(true, 
                  "copyStreamBytesV(..) interrupted");
              break; // Exit loop without success.
              }
            }
	        } 
	      catch (IOException theIOException) {
          appLogger.exception("copyStreamBytesV(..)",theIOException);
	        }
        appLogger.info( "copyStreamBytesV() successB="+successB
            +", bytes transfered=" + byteCountI);
	      return successB;
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
			    to socketOutputStream.  
			    For code simplicity, the number always begins with a [leading] '0'.
			    
			    ///fix This will fail in 2038 when the 32-bit signed number overflows.
			   	*/
				{	
				  if ( theL == 0 ) { // Output first digit which is always 0.
				  		socketOutputStream.write( (byte) ((byte)('0') + theL));
				  	} else { // Use recursion to output other digits.
				  		TCPCopier.sendDigitsOfNumberV( // Recurse for earlier digits.
				  		    socketOutputStream, theL / 10 );
				  		socketOutputStream.write( // Output final digit. 
				  		    (byte) ( (byte)('0') + (theL % 10) ) );
				  	}
				}
	
	  }
