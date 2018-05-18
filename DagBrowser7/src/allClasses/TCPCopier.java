package allClasses;

// The following code was based on code gotten from
// https://stackoverflow.com/questions/4687615/how-to-achieve-transfer-file-between-client-and-server-using-java-socket

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

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

		//private static final String testServerIPString = "127.0.0.1";
	  private static final int testServerPortI = 11111;
	  
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
	  
	  static class TCPClient extends EpiThread {
	
		  private final Persistent thePersistent; // External data.
			
	    public TCPClient(  // Constructor.
	  			String threadNameString, Persistent thePersistent) 
	    {
				super(threadNameString);
	
			  this.thePersistent= thePersistent;
				}

	    public void run()
	      /* This is the main method of the Client thread.
	        It repeatedly tries to connect to various peer nodes,
	        all of which should be acting as servers.
	        When a connection is made, 
	        it executes the file update/exchange protocol.
	        */
	      {
	  			appLogger.info("run() beginning.");
	
		  		EpiThread.interruptableSleepB(4000); ///dbg delay to organize log.
	      	////boolean oldConsoleModeB= appLogger.getAndEnableConsoleModeB(); ///tmp ///dbg
	
	      	PersistentCursor thePersistentCursor= 
	      			new PersistentCursor( thePersistent );
	      	thePersistentCursor.setListNameStringV("peers");
		    	while ( ! EpiThread.exitingB() ) // Repeat until exit requested.
		    		{ // Process an element of list.
			    		{ 
								File thatFolderFile= Config.makeRelativeToAppFolderFile( 
										Config.tcpCopierInputFolderString );
								thatFolderFile.mkdir();  // Create destination folder if needed.
			  	  		Misc.updateFromToV( // Update staging area from standard folder.
    							Config.userAppJarFile,
    							new File( thatFolderFile, Config.appJarString )
				      		/*  ////
				      		  Config.makeRelativeToAppFolderFile( 
				      		  Config.tcpCopierInputFolderString
				      			+ File.separator 
				      			+ Config.appJarString 
				      			)
				      			*/  ////
				      		);
			    			tryExchangingFilesWithNextServerV(thePersistentCursor);
						    EpiThread.interruptableSleepB(8000); // Pause a while.
			    			}
	      			}
	  			appLogger.info("run() ending.",true);
	
	      	////appLogger.restoreConsoleModeV( oldConsoleModeB ); /// tmp ///dbg
		    	}
	
			private void tryExchangingFilesWithNextServerV( 
					PersistentCursor thePersistentCursor)
			  /* Tries to exchange files with next server peer node based on 
			    the state of thePersistentCursor into the peer list.
			   */
				{
				  if ( thePersistentCursor.getEntryIDNameString().isEmpty() ) { 
				      ; // Do nothing this time because peer list is wrapping-around.
				  	} else { // Process peer list element.
							String serverIPString= 
									thePersistentCursor.getFieldString("IP");
							String serverPortString= 
									thePersistentCursor.getFieldString("Port");
				  		tryExchangingFilesWithServerV(serverIPString,serverPortString);
					  }
				  thePersistentCursor.nextString(); // Go to next element.
					}
	
			private void tryExchangingFilesWithServerV(
					String serverIPString, String serverPortString)
			  /* Tries to exchange files with the peer node that is   
			    at IPAddress serverIPString is listening on port serverPortString.
			    */
				{
					Socket clientSocket = null;
					File clientFile= 
							Config.makeRelativeToAppFolderFile( clientFileString );
					int serverPortI= Integer.parseUnsignedInt( serverPortString );
				 	InetSocketAddress theInetSocketAddress= null; 
					try {
						 	theInetSocketAddress= 
								new InetSocketAddress( serverIPString, serverPortI );
							appLogger.info(
									"tryExchangingFilesWithServerV() at "+ theInetSocketAddress);
							clientSocket= new Socket();
							clientSocket.connect(theInetSocketAddress, 5000);
							  // Connect with time-out.
				  		long clientFileLastModifiedL= clientFile.lastModified(); 
				  		tryTransferingFilesL(
				  			clientSocket, clientFile, clientFile, clientFileLastModifiedL );
				    } catch (IOException theIOException) {
							appLogger.info(
									"tryExchangingFilesWithServerV() at "+ theInetSocketAddress,
									theIOException
									);
				  	} finally {
						  Closeables.closeWithErrorLoggingB(clientSocket);
						}
					}
	
		} // TCPClient
	
		
		static class TCPServer extends EpiThread {
		
		  private File serverFile= null;
	
	    public TCPServer(String threadNameString) { // Constructor.
				super(threadNameString);
	    	}
	
	    public void run() 
		    /* This is the main method of the Server thread.
		      It repeatedly waits for client connections and processes them
		      by executing the file update/exchange protocol.
			    */
		    {
		  		appLogger.info("run() beginning.");
		    	///dbg EpiThread.interruptableSleepB(5000); ///tmp Prevent initial error.
		    	EpiThread.interruptableSleepB(2000); // Organize log.
	    		////boolean oldConsoleModeB= appLogger.getAndEnableConsoleModeB();
		    	while  // Repeatedly service one client request. 
		    		( ! EpiThread.exitingB() ) 
			    	{ 
			    		serviceOneRequestFromClientV();
			    	  EpiThread.interruptableSleepB(4000); // Sleep to prevent hogging.
			    		} // while...
	      	////appLogger.restoreConsoleModeV( oldConsoleModeB );
		    	}
	
			private void serviceOneRequestFromClientV()
			  /* This method waits for and processes one request from a client.
			    This might result in a file being send to the client,
			    a file being received from the client,
			    or no file transfered at all.
 			    */
				{
			    ServerSocket serverServerSocket= null;
			    Socket serverSocket= null;
			  	try {
			        serverServerSocket = new ServerSocket(testServerPortI);
			    		///dbg appLogger.info("run() trying ServerSocket.accept().");
			        serverSocket = serverServerSocket.accept();
			    		serverFile= // Calculating File name.
			    				Config.makeRelativeToAppFolderFile( serverFileString );
				  		long serverFileLastModifiedL= serverFile.lastModified();
			    		tryTransferingFilesL(
			    				serverSocket, serverFile, serverFile, serverFileLastModifiedL );
			      } catch (IOException ex) {
			    		appLogger.info("run() IOException",ex);
			      } finally {
			    		///dbg appLogger.info("run() closing resources.");
			      	Closeables.closeWithErrorLoggingB(serverSocket);
			      	Closeables.closeWithErrorLoggingB(serverServerSocket);
			        }
					}
			
			} // TCPServer 
	  
		private static long tryTransferingFilesL(
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
		    is copied across the connection to replaced the older file.
		    If a file is received then it replaces localDestinationFile and
		    this method returns the received file's newer time-stamp,
		    If a file is sent or no file is transfered at all then 0 is returned. 
		    This method is called by both the client and the server.

				localSourceFile and localDestinationFile are normally the same file.
				Using two parameters instead of one made testing easier.
				Also localLastModifiedL is normally the time-stamp of that same file,
				and could be eliminated, but exists, again, to make testing easier.
		   */
			{
				///dbg appLogger.info("transactionV() beginning.");
			  long receivedLastModifiedL= 0;
	      InputStream socketInputStream= null;
				OutputStream socketOutputStream = null;
		  	try {
		  			socketInputStream= theSocket.getInputStream();
			  		socketOutputStream= theSocket.getOutputStream();
			  		long timeStampResultL= 
		      			TCPCopier.exchangeAndCompareFileTimeStampsRemoteToLocalL(
		      				socketInputStream, socketOutputStream, localLastModifiedL);
						if ( timeStampResultL > 0 ) { // Remote file is newer.
				  			appLogger.info("tryTransferingFilesL(..) Remote file is newer.");
								if ( receiveNewerRemoteFileB(
										localDestinationFile, socketInputStream, timeStampResultL ) )
									receivedLastModifiedL= timeStampResultL;
						  } else if ( timeStampResultL < 0 ) { // Local file is newer.
				  			appLogger.info("tryTransferingFilesL(..) Local file is newer.");
						  	sendNewerLocalFileV(
						  			localSourceFile, socketOutputStream );
						  } else { ; // Files are same age, so do nothing. 
				  			appLogger.info("tryTransferingFilesL(..) Files are same age.");
						  }
			    } catch (IOException ex) {
			  		appLogger.info("transactionV() IOException",ex);
			    } finally {
			  		///dbg appLogger.info("transactionV() closing resources.");
			  		}
	  	  ///dbg appLogger.info("transactionV() ending.");
		  	return receivedLastModifiedL;
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
					temporaryFile= Config.makeRelativeToAppFolderFile( 
							Config.tcpCopierOutputFolderString );
					temporaryFile.mkdir();  // Create folder if needed.
					temporaryFile= Config.makeRelativeToAppFolderFile( 
							Config.tcpCopierOutputFolderString + File.separator + "Temporary.file" );
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
	      */
		  throws IOException
			{
	  		long remoteLastModifiedL= 0; // Initial accumulator value.
	  		{ // Send digits of local file time stamp and terminator to remote end.
	  			TCPCopier.sendDigitsOfNumberV(socketOutputStream, localLastModifiedL);
		  		socketOutputStream.write( (byte)('\n') );
		  		socketOutputStream.write( (byte)('#') );
	  			socketOutputStream.flush();
	  			}
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
	  		long compareResultL= remoteLastModifiedL - localLastModifiedL;
	  		if (compareResultL > 0 ) 
	  			  compareResultL= remoteLastModifiedL;
	  			else if (compareResultL < 0 ) 
	  			  compareResultL= -localLastModifiedL;
	  		////if ( compareResultL != 0 ) // Log only if not 0.
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
