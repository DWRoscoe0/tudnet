package allClasses;

// The following code was based on code gotten from
// https://stackoverflow.com/questions/4687615/how-to-achieve-transfer-file-between-client-and-server-using-java-socket

import java.io.*;
import java.net.*;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

import static allClasses.AppLog.theAppLog;
import static allClasses.SystemSettings.NL;


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

    PeersCursor thePeersCursor= null; // Used for scanning known peers.

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

    public void initializeV()
      /* Call this method to start things. 
        First it updates the TCP update staging area if possible.
        Then it starts this thread.
        */
      {
        theAppLog.info("TCPCopier.initializeV() begins.");
        toReturn: { // Act only if all conditions are true.
          if (theAppLog.testAndLogDisabledB( // Return if manually disabled.
              Config.tcpThreadsDisableB,"TCPCopier"))
            break toReturn;
          if // Don't operate on .class file, which means we are running under Eclipse.
            (".class".equals(AppSettings.initiatorExtensionString))
            break toReturn;
          theAppLog.info(
              "TCPCopier.initializeV() all conditions met, operations beginning.");
          { // Proceed with TCPCopier operations.
            updateTCPCopyStagingAreaV();
            startV(); // Start TCPCopier thread.
            }
          } // toReturn: {
        theAppLog.info("TCPCopier.initializeV() ends.");
        }

    public void finalizeV() 
      /* Call this method to stop things. 
        It initiates thread termination and waits for completion.
        */
      {
        stopAndJoinV();
        }

    public void run()
    /* This is the main method of the thread.
      After a delay and some initialization,
      it alternates between roles of client and server.

      The method will return early if the thread is interrupted.
      */
    {
      /// theAppLog.info("run() begins.");
      EpiThread.interruptibleSleepB(Config.tcpCopierRunDelayMsL);
      theAppLog.info("run() start delay done.");

      thePeersCursor=
          PeersCursor.makeOnFirstEntryPeersCursor( thePersistent );
      loopAlternatingRolesV();
      theAppLog.info("run() ends.");
      }

    public synchronized void stopV()
      /* This is a subclass method stopV() for stopping the thread.
        In addition to interrupting the thread in the usual way
        using Thread.interrupt(),
        it closes the Sockets and the ServerSocket 
        which might have blocked methods.
        */
      {
        super.stopV(); // Terminate the default way by interrupting thread.
        
        try { // Also close the server ServerSocket.
            if (serverServerSocket!= null) {
              theAppLog.info("TCPCopier.stopV(): closing serverServerSocket");            
              serverServerSocket.close();
              }
          } catch (Exception theException) {
            theAppLog.exception("TCPCopier.stopV(): closing", theException);            
          }
        
        // Also close the Sockets of
        Closeables.closeIfNotNullWithLoggingB(clientSocket); // the client
        Closeables.closeIfNotNullWithLoggingB(serverSocket); // and server.

        theAppLog.info("TCPCopier.stopV(): interrupt and socket closes done.");
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
        FileOps.updateFromToV( // Update staging area from standard folder.
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
        It spends most of its time as a server waiting for connections.

        The method will return early if the thread is interrupted.
        */
      {
        while (true)  // Repeat until exit requested.
          {
            if ( EpiThread.testInterruptB() ) break;
            actAsAClientB();
            if ( EpiThread.testInterruptB() ) break;
            actAsAServerV();
            EpiThread.interruptibleSleepB( // Random delay to prevent hogging.
                theRandom.nextInt(Config.antiCPUHogLoopDelayMsI));
            theAppLog.info(
                "loopAlternatingRolesV() looping after random delay.");
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
        /// theAppLog.info("actAsAClientB() begins.");
        boolean successB= false;
        toReturn: {
          if (tryExchangeWithServerFromQueueB()) successB= true;
          if ( EpiThread.testInterruptB() ) break toReturn;
          if (tryExchangeWithServerFromPersisentDataB()) successB= true;
          } // toReturn:
        /// theAppLog.info("actAsAClientB() ends.");
        return successB; 
        }

    private boolean tryExchangeWithServerFromQueueB()
      /* Tries to exchange files with next server peer nodes on the peer input queue.
        It will keep going until the queue is empty.
        It will return immediately if the queue is empty.
        It will wait for a maximum of maxWaitMSL milliseconds.
        It returns true if any update file was exchanged, false otherwise.

        The method will return early if the thread is interrupted.
        */
      {
          boolean successB= false;
        loop: while(true) { // Process all queue elements.
          process: { // Try processing one queue element.
            IPAndPort theIPAndPort= peerQueueOfIPAndPort.poll();
            if ( theIPAndPort == null) { 
              theAppLog.debug("tryExchangeWithServerFromQueueB() queue empty.");
              break loop; // Exit loop if no peer.
              }
            String serverIPString= 
                 theIPAndPort.getInetAddress().getHostAddress(); 
            String serverPortString= 
                Integer.toString(theIPAndPort.getPortI());
            long resultL= 
                tryExchangingFilesWithServerL(serverIPString, serverPortString);
            if (resultL == 0) break process; // No file transfered.
            theAppLog.debug(
                "tryExchangeWithServerFromQueueB() success for "+ theIPAndPort);
            successB= true; // File was transfered.
            } // process:
        } // loop:
          return successB;
        }
      
    private boolean tryExchangeWithServerFromPersisentDataB()
      /* Tries to exchange files with the next server peer node based on 
        the state of thePeersCursor and its peer list.
        If there is an element saved in the list, 
        then an attempt will be made to exchange files with it,
        even if wrapping around to the beginning of the list is necessary.
        Returns true if a file was transfered, false otherwise.

        The method will return early if the thread is interrupted.
        */
      {
          boolean successB= false;
          String serverIPString= null;
          String serverPortString= null;
        toReturn: {
          if ( thePeersCursor.getEntryKeyString().isEmpty() )
            break toReturn; // Do nothing because peer list is empty.
          serverIPString= thePeersCursor.getFieldString("IP");
          serverPortString= thePeersCursor.getFieldString("Port");
          long resultL= 
              tryExchangingFilesWithServerL(serverIPString,serverPortString);
          if (resultL == 0) break toReturn; // No file data was transfered.
          successB= true; // Everything worked.
        } // toReturn:
          thePeersCursor.nextWithWrapKeyString(); // Advance cursor to next peer.
          theAppLog.debug( "tryExchangeWithServerFromPersisentDataB() successB="+successB
              + " for " + serverIPString + "," + serverPortString );
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
        /// theAppLog.debug(
        ///       "tryExchangingFilesWithServerL()"
        ///     + ", serverIPString= " + serverIPString
        ///     + ", serverPortString= " + serverPortString);
        File clientFile= 
            AppSettings.makeRelativeToAppFolderFile( clientFileString );
        int serverPortI= Integer.parseUnsignedInt( serverPortString );
        InetSocketAddress theInetSocketAddress= null; 
        try {
            theInetSocketAddress= 
              new InetSocketAddress( serverIPString, serverPortI );
            /// theAppLog.debug(
            /// "tryExchangingFilesWithServerL() theInetSocketAddress= "
            /// + theInetSocketAddress);
            clientSocket= new Socket();
            /// theAppLog.debug(
            ///   "tryExchangingFilesWithServerL() before connect"
            ///   + "," + NL + "  clientSocket= " + clientSocket
            ///   + "," + NL + "  theInetSocketAddress= " + theInetSocketAddress);
            clientSocket.connect(  // Connect with time-out.
                theInetSocketAddress, Config.tcpConnectTimeoutMsI); 
            /// theAppLog.debug(
            /// "tryExchangingFilesWithServerL() after successful connect"
            /// + "," + NL + "  clientSocket= " + clientSocket);
            long clientFileLastModifiedL= clientFile.lastModified();
            long clientFileFizeL= clientFile.length();
            resultL= tryTransferingFileL( clientSocket, 
                clientFile, clientFile, clientFileLastModifiedL, clientFileFizeL );
            
            thePeersCursor.addInfoUsingPeersCursor(
                  new IPAndPort(serverIPString, serverPortString),null);
            if (resultL != 0)
              theAppLog.info( 
                  "tryExchangingFilesWithServerL() copied using"
                  + NL + "  clientSocket= " + clientSocket);
          } catch (SocketTimeoutException theIOException) {
            // A timeout is not considered an error, so it is no longer logged.
            /// theAppLog.debug(
            ///   "tryExchangingFilesWithServerL() failed" + NL + "  "+ theIOException);
          } catch (IOException theIOException) {
            theAppLog.info(
              "tryExchangingFilesWithServerL() failed" + NL + "  "+ theIOException);
          } finally {
            Closeables.closeWithErrorLoggingB(clientSocket);
          }
        return resultL;
        }
    
    private void actAsAServerV()
      /* This method acts as a server for a maximum of tcpServerMaximumWaitMsL.
        During that time it waits for and processes requests from clients.
        A request might result in an update file being sent to the client,
        an update file being received from the client,
        or no file transfered at all.
        It will process as many requests as it receives in the time available.
        If no request is received within that time then it returns.
        The method will return early if the thread is terminated,
        either by being interrupted or by another thread closing a socket.
        See stopV().
        */
      { 
        /// theAppLog.debug("actAsAServerV() begins.");
        long endMsL= // Calculate when time as server will exit.
            System.currentTimeMillis() + Config.tcpCopierServerTimeoutMsI;
        while (true) { // Looping until server time has expired.
          if ( EpiThread.testInterruptB() ) break;
          final long nowMsL= System.currentTimeMillis();
          final int remainingMsI= (int)(endMsL-nowMsL);
          if ( remainingMsI <= 0) // Exiting if server time has expired.
            break;
          tryServicingOneRequestFromAnyClientB(
              remainingMsI); // Use remaining time as time-out.
          EpiThread.interruptibleSleepB( // Random delay to prevent hogging.
              theRandom.nextInt(Config.antiCPUHogLoopDelayMsI));
          /// theAppLog.debug("actAsAServerV() looping after random delay.");
          } // while(true)
        /// theAppLog.debug("actAsAServerV() ends.");
        }

    private boolean tryServicingOneRequestFromAnyClientB(int maximumWaitMsI)
      /* This method waits for a maximum of maximumWaitMsI for
        a request from a client, and processes that request. 
        This might result in a file being sent to the client,
        a file being received from the client,
        or no file transfered at all.
        It returns true if a request was processed, 
        false if a request was not processed for any reason.
        The method will return early if the thread is terminated,
        either by being interrupted or by another thread closing a socket.
        See stopV().
        */
      {
        boolean successB= false;
        toReturn: try {
            int tcpPortI=thePortManager.getNormalPortI();
            synchronized(this) { // Do this for unblocking of accept() ahead.
              if (EpiThread.testInterruptB()) break toReturn;
              serverServerSocket= new ServerSocket(tcpPortI);
              }
            serverServerSocket.setSoTimeout( maximumWaitMsI );
            /// theAppLog.debug(
            ///     "serviceOneRequestFromAnyClientV()() trying ServerSocket.accept() to "
            ///   + serverServerSocket);
            serverSocket= serverServerSocket.accept();
            /// theAppLog.debug(
            /// "serviceOneRequestFromAnyClientV() accepted connection on "
            ///   + serverSocket );
            processServerConnectionV(serverSocket); 
            successB= true; // Completed without throwing an exception.
          } catch (SocketTimeoutException ex) { // Treat time-out as okay way to end.
            theAppLog.info("serviceOneRequestFromAnyClientV() time-out.");
          } catch (IOException ex) { // Handle thrown exceptions.
            if (EpiThread.testInterruptB())
              theAppLog.info("serviceOneRequestFromAnyClientV() termination plus "+ex);
              else
              theAppLog.exception("serviceOneRequestFromAnyClientV()",ex);
          } finally { // Be certain resources are closed.
            Closeables.closeIfNotNullWithLoggingB(serverSocket);
            Closeables.closeIfNotNullWithLoggingB(serverServerSocket);
            } // toReturn: 
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
        long serverFileFizeL= serverFile.length();
        long resultL= tryTransferingFileL(
          serverSocket, serverFile, serverFile, serverFileLastModifiedL, serverFileFizeL );
        if (resultL != 0)
          theAppLog.info( "processServerConnectionV() copied using " 
              +serverSocket);
        }

    public synchronized void queuePeerConnectionV( 
        IPAndPort remoteIPAndPort )
      /* This method adds remoteIPAndPort to the peer queue.
        This is a way to learn about new peers which might be used as
        sources or destinations of software updates.
        */
      {
        theAppLog.debug( "TCPCopier.reportPeerConnectionV(..): queuing peer." );
        peerQueueOfIPAndPort.add(remoteIPAndPort); // Add peer to queue.
        notify(); // Wake up the Client thread.
        }

	  
		private static long tryTransferingFileL(
				Socket theSocket,
				File localSourceFile, 
				File localDestinationFile, 
				long localLastModifiedL,
				long localFileSizeL
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
		    this method returns the received file's newer time-stamp.
		    If a file is sent, it returns the negative of the remote time-stamp.
		    If a file is neither sent nor received,
		    because their time-stamps are equal, then 0 is returned. 
				This method is used by both the client and the server.

				localSourceFile and localDestinationFile are normally the same file.
				Using two parameters instead of one made earlier testing easier.
				Also localLastModifiedL is normally the time-stamp of that same file,
				and could be eliminated, but exists, again, to make testing easier.
		   */
			{
		    /// theAppLog.debug("tryTransferingFileL(..) beginning.");
			  long transferResultL= 0;
		  	try {
			  		/// theAppLog.debug("tryTransferingFileL(..) before exchange...");
		  	    LongLike remoteFileSizeLongLike= new DefaultLongLike();
			  		long timeStampResultL=
		      			TCPCopier.exchangeAndCompareFileAttributesRemoteToLocalL(
		      				theSocket, localLastModifiedL, localFileSizeL, remoteFileSizeLongLike);
			  		/// theAppLog.debug("tryTransferingFileL(..) after exchange...");
						if ( timeStampResultL > 0 ) { // Remote file is newer.
				  			theAppLog.info("tryTransferingFileL(..) Remote file is newer.");
								if ( receiveNewerRemoteFileB(localDestinationFile, theSocket, 
								    timeStampResultL, remoteFileSizeLongLike.getValueL() ) )
									transferResultL= timeStampResultL;
						  } else if ( timeStampResultL < 0 ) { // Local file is newer.
				  			theAppLog.info("tryTransferingFileL(..) Local file is newer.");
						  	sendNewerLocalFileV(localSourceFile, theSocket);
								transferResultL= timeStampResultL;
						  } else { ; // Files are same age, so do nothing. 
						    /// theAppLog.debug("tryTransferingFileL(..) Files are same age.");
						  }
				} catch (IOException ex) {
			  		theAppLog.debug("tryTransferingFileL(..) [intentional?] abort with " + ex);
			    } finally {
			      // Closing is done elsewhere.
			  		}
		  	if (transferResultL != 0)
		  		theAppLog.info("tryTransferingFileL(..) exchanged using "+theSocket);
	  	  /// theAppLog.debug("tryTransferingFileL(..) ends.");
		  	return transferResultL;
	    	}
	
		private static void sendNewerLocalFileV(
				File localFile,
				Socket theSocket
				)
			throws IOException
			/* This method sends the file localFile,
			  which should be newer that its remote counterpart,
			  through theSocket, to replace its remote counterpart.
			  It closes the send side of the socket after the last byte of the file.
			  Then it waits for End-Of-File on the receive side before
			  closing the localFile and returning.
			 	*/
			{ // Local file newer.
		    OutputStream socketOutputStream= theSocket.getOutputStream();
				FileInputStream localFileInputStream= null;
				try { 
						theAppLog.info("sendNewerLocalFileV() sending file "
								+ FileOps.fileDataString(localFile));
						localFileInputStream= new FileInputStream(localFile);
						FileOps.copyStreamBytesB(
								localFileInputStream, socketOutputStream);
            theSocket.shutdownOutput(); // Do an output half-close, signaling EOF.
              // This signals end of file data.
            theAppLog.info("sendNewerLocalFileV() output shutdown after sending file."
                + FileOps.fileDataString(localFile));
            skipToEndOfFileV(theSocket);
              // This signals that remote peer has received all our sent file data.
            theAppLog.info("sendNewerLocalFileV() remote peer output shutdown.");
				  } finally {
			  		Closeables.closeWithErrorLoggingB(localFileInputStream);
					}
				}

    private static void skipToEndOfFileV(Socket theSocket) 
        throws IOException
      /* This method skips to the end of the theSocket's InputStream, blocking if needed.
        This can be used to receive a signal indicating that the remote peer
        has received all sent data and shutdown its output.
       */
      {
        while (true) { // Read from stream until -1 received. 
          int dataI= theSocket.getInputStream().read();
          if (0 > dataI) break;
          theAppLog.debug("skipToEndOfFileV(..) byte read = " + dataI);
          }
        theAppLog.debug("skipToEndOfFileV(..) EOF reached.");
        }

    private static boolean receiveNewerRemoteFileB(
        File localFile,
        Socket theSocket,
        long remoteTimeStampL,
        long remoteLengthL
        )
      throws IOException
      /* This method receives the remote counterpart of file localFile,
        via theSocket. and if the number of bytes received is remoteLengthL,
        then it replaces the localFile with the remote one.
        The new file has its TimeStamp set to remotetimeStamp.
        The above operations are done in a two-step process 
        using an intermediate temporary file.
        The final step is an atomic rename of the temporary file to the local file.
        
        This method returns true if the entire file transfer finished, 
        false otherwise.
        */
      {
        theAppLog.info("receiveNewerRemoteFileB() receiving file "
            + FileOps.fileDataString(localFile));
        InputStream socketInputStream= theSocket.getInputStream();
        File tmpFile= null;
        FileOutputStream tmpFileOutputStream= null;
        boolean successB= false; // Assume we will not be successful.
        toReturn: {
          tmpFile= FileOps.createTemporaryFile("TCPUpdate");
          if (tmpFile == null) break toReturn;
          try { tmpFileOutputStream= new FileOutputStream( tmpFile ); 
            } catch ( FileNotFoundException e ) {
              theAppLog.exception("receiveNewerRemoteFileB() open failure", e);
              break toReturn;
            }
          if (!FileOps.copyStreamBytesB( // Copy stream to file or
              socketInputStream, tmpFileOutputStream))
            break toReturn; // terminate if failure.
          theSocket.getOutputStream().write(0); // If this executes without IOException
            // then peer finished sending its file and hasn't closed the socket.
          theAppLog.debug(
              "receiveNewerRemoteFileB() 0 byte sent back.  doing shutdownOutput().");
          theSocket.shutdownOutput(); // Do an output half-close, signaling EOF.
          try { 
              tmpFileOutputStream.close(); // Close output file, not the socket.
            } catch ( IOException e ) {
              theAppLog.exception("receiveNewerRemoteFileB() close failure", e);
              break toReturn; // Exit because close failed.
            }
          tmpFile.setLastModified(remoteTimeStampL); // Set time stamp.
          if (remoteLengthL != tmpFile.length()) {
            theAppLog.info("receiveNewerRemoteFileB(..) wrong file length=");
            break toReturn; // Exit because received file has wrong length.
            }
          if (!FileOps.atomicRenameB(tmpFile.toPath(), localFile.toPath())) 
            break toReturn; // Exit because rename failed.
          successB= true; // Success because everything finished.
          } // toReturn:
        Closeables.closeWithErrorLoggingB(tmpFileOutputStream); ///opt done needed?
        FileOps.deleteDeleteable(tmpFile); // Delete possible temporary debris.
        theAppLog.info("receiveNewerRemoteFileB(..) successB="+successB);
        return successB;
        }
	  
		private static long exchangeAndCompareFileAttributesRemoteToLocalL(
		    Socket theSocket,
	  		long localLastModifiedL,
	  		long localFileSizeL,
        LongLike remoteFileSizeLongLike 
				)
	    /* This method exchanges attributes of a file on both the local and remote peers
	      as part of an effort to decide which, if either, of the files is newer
	      and should be copied to the other peer.
	      It exchanges the time-stamps and file sizes of the remote and local files.
	      The parameters are as follows:

	      * theSocket is the open Socket over which the exchange of data takes place.
        * localLastModifiedL is used as the time-stamp of the local file.
        * localFileSizeL is used as the length of the local file.
        * remoteFileSizeLongLike is used to return the length of the remote file.

	      This method returns a value indicating the result of the comparison.
	  		If the returned value == 0 then the two files 
	  		have the same lastModified time-stamps and are equally old.
	  		If the returned value > 0 then the remote file is newer
	  		and the returned value is the remote file time-stamp. 
	  		If the returned value < 0 then the local file is newer
	  		and the returned value is negative the value of the local time-stamp. 

	      A file that does not exist is considered infinitely old.
	      
	      ///fix Add time-out for receiving time-stamp from remote node.  See:
	         https://stackoverflow.com/questions/804951/is-it-possible-to-read-from-a-inputstream-with-a-timeout
	         No, this will busy-wait.  Use Socket.setSoTimeout(int timeout).
	      */
		  throws IOException
			{
    		/// theAppLog.debug( 
		    ///   		"exchangeAndCompareFileAttributesRemoteToLocalL() begins.");
        InputStream socketInputStream= theSocket.getInputStream();
        OutputStream socketOutputStream= theSocket.getOutputStream();
        
	  		// Send digits of local file time stamp and terminator to remote end.
        TCPCopier.sendNumberV(socketOutputStream, localLastModifiedL); // Time-stamp.
        TCPCopier.sendNumberV(socketOutputStream, localFileSizeL); // File size.
	  		socketOutputStream.write( (byte)('#') ); // File starts after this.
  			socketOutputStream.flush(); // Be certain the data is sent.
    		/// theAppLog.debug( 
    		/// 	"exchangeAndCompareFileAttributesRemoteToLocalL() "
    		/// 	+"after sending digits.");
	  		
	  		// Receive and decode similar digits of remote file time stamp.
        long remoteLastModifiedL= receiveNumberL(socketInputStream);
        remoteFileSizeLongLike.setValueL(receiveNumberL(socketInputStream));
        int socketByteI= socketInputStream.read(); // Read first byte.
    		while (true) { // Skip characters through '#' or to end of input.
    			if (socketByteI==-1) break; // Exit if end of stream.
    			if (socketByteI==(int)'#') break; // Exit if terminator found. 
      		socketByteI= socketInputStream.read(); // Read next byte.
      		}
      	/// theAppLog.debug( "exchangeAndCompareFileAttributesRemoteToLocalL() "
      	/// 	+"after receiving digits.");
    		
	  		long compareResultL= remoteLastModifiedL - localLastModifiedL;
	  		if (compareResultL > 0 ) 
	  			  compareResultL= remoteLastModifiedL;
	  			else if (compareResultL < 0 ) 
	  			  compareResultL= -localLastModifiedL;
	  		/*  ///
	  		theAppLog.debug( // Log result of comparison.
  	  	  	"exchangeAndCompareFileAttributesRemoteToLocalL() returning "
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
  	  	theAppLog.debug( "exchangeAndCompareFileAttributesRemoteToLocalL() ends.");
        */ ///
				return compareResultL;
				}

    private static long receiveNumberL(InputStream socketInputStream)
        throws IOException
      /* This method return a long number from the socketInputSteam.
        */
    {
      long theL= 0; // Digit accumulator.
      int socketByteI= socketInputStream.read(); // Read first byte.
      while (true) { // Skip all possible non-digit bytes before number.
        if (socketByteI==-1) break; // Exit if end of stream.
        if (Character.isDigit(socketByteI)) break; // Exit if digit.
        socketByteI= socketInputStream.read(); // Read next byte.
        }
      while (true) { // Accumulate all digits of remote file time-stamp.
        if (socketByteI==-1) break; // Exit if end of stream.
        if (! Character.isDigit(socketByteI)) break; // Exit if not digit.
        theL= // Combine new digit with digit accumulator.  
            10 * theL + Character.digit(socketByteI, 10); 
        socketByteI= socketInputStream.read(); // Read next byte.
        }
      // appLogger.debug( "readNumberL(..): "+theL);
      return theL;
      }

    private static void sendNumberV( 
      OutputStream socketOutputStream, long theL)
          throws IOException
      /* This method sends the long number theL to socketOutputStream.
        The number consists of digits followed by a new-line.
       */
      {
        // appLogger.debug( "sendNumberL(..): "+theL);
        TCPCopier.sendDigitsOfNumberV(socketOutputStream, theL);
        socketOutputStream.write( NL.getBytes() );
        }

		private static void sendDigitsOfNumberV( 
					OutputStream socketOutputStream, long theL)
			  throws IOException
			  /* This recursive method sends decimal digits of the long number theL
			    to socketOutputStream.  
			    For code simplicity, the number always begins with a [leading] '0'.
			    
			    ///fix This will fail when used for sending a time in 2038 
			      when the 32-bit signed number overflows.
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
