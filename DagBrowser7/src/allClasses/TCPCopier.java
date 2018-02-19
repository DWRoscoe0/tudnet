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
    This is done by saving the LastModified TimeStamp of the local file
    and using it for comparisons.

    ///enh Eventually it should maintain its own data file.
      Any peers with which it communicates for updating may be added
      only by it and only after it tests them for operability.
      These peers can be supplied by by the connection manager
      when it discovers them.
    */

	{

		//private final static String testServerIPString = "127.0.0.1";
	  private final static int testServerPortI = 11111;
	  private final static String testFileString = "TCPCopier.txt";
	  //private final static String testServerFileString = "TCPCopierServer.txt";
	  private final static String testServerFileString = testFileString;
	  //private final static String testClientFileString = "TCPCopierClient.txt";
	  private final static String testClientFileString = testFileString;


	  static class TCPClient extends EpiThread {
	
		  private final Persistent thePersistent; // External data.
			
		  private static long localLastModifiedL= 0; /* This is where the time-stamp
		    of the local copy of the update file is stored until the next update.
		    If the file is replaced then this variable will be updated,
		    but it might not happen immediately.
		    The client uses this variables as the local file time-stamp.
		    The server uses the actual file time-stamp as the file time-stamp.
		    */
	
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
	  			appLogger.info("run() beginning.",true);
	
					File clientFile= AppFolders.resolveFile( testClientFileString );
		  		localLastModifiedL= // Saved time stamp of local file copy.
		  				clientFile.lastModified(); 

		  		EpiThread.interruptableSleepB(4000); ///dbg delay to organize log.
	      	boolean oldConsoleModeB= appLogger.getAndEnableConsoleModeB(); ///tmp ///dbg
	
	      	PersistentCursor thePersistentCursor= 
	      			new PersistentCursor( thePersistent );
	      	thePersistentCursor.setListNameStringV("peers");
		    	while ( ! EpiThread.exitingB() ) // Repeat until exit requested.
		    		{ // Process an element of list.
			    		{ 
			    			tryExchangingFileWithNextServerV(thePersistentCursor);
								///dbg appLogger.info("run() sleeping 5s before next attempt.",true);
						    EpiThread.interruptableSleepB(8000); // Prevent hogging.
			    			}
	      			}
	  			appLogger.info("run() ending.",true);
	
	      	appLogger.restoreConsoleModeV( oldConsoleModeB ); /// tmp ///dbg
		    	}
	
			private void tryExchangingFileWithNextServerV( 
					PersistentCursor thePersistentCursor)
			  /* Tries to exchange a file with next server peer node based on 
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
				  		tryExchangingFileWithServerV(serverIPString,serverPortString);
					  }
				  thePersistentCursor.nextString(); // Go to next element.
					}
	
			private void tryExchangingFileWithServerV(
					String serverIPString, String serverPortString)
			  /* Tries to exchange a file with the peer node that is   
			    at IPAddress serverIPString is listening on port serverPortString.
			    */
				{
					Socket clientSocket = null;
					File clientFile= AppFolders.resolveFile( testClientFileString );
					int serverPortI= Integer.parseUnsignedInt( serverPortString );
					try {
							clientSocket= new Socket( serverIPString, serverPortI );
				  		long clientFileLastModifiedL= localLastModifiedL;
				  		long newLastModifiedL= tryTransferingFileL( 
				  				clientSocket, clientFile, clientFileLastModifiedL );
				  		if ( newLastModifiedL != 0 ) // Update copy of local  time-stamp.
				  			localLastModifiedL= newLastModifiedL;
				    } catch (IOException theIOException) {
							appLogger.info("tryGettingFileV()",theIOException);
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
		  		appLogger.info("run() beginning.",true);
		    	///dbg EpiThread.interruptableSleepB(5000); ///tmp Prevent initial error.
		    	EpiThread.interruptableSleepB(2000); // Organize log.
	    		boolean oldConsoleModeB= appLogger.getAndEnableConsoleModeB();
		    	while ( ! EpiThread.exitingB() ) {  // Repeatedly try providing file. 
		    		///dbg appLogger.info("run() loop beginning.");
		    		tryExchangingFileWithClientV();
		    		///dbg appLogger.info("run() loop ending with 10s sleep.");
				    EpiThread.interruptableSleepB(4000); // Sleep 4s to prevent hogging.
		    		} // while...
	      	appLogger.restoreConsoleModeV( oldConsoleModeB );
		    	}
	
			private void tryExchangingFileWithClientV()
				{
			    ServerSocket serverServerSocket= null;
			    Socket serverSocket= null;
			  	try {
			        serverServerSocket = new ServerSocket(testServerPortI);
			    		///dbg appLogger.info("run() trying ServerSocket.accept().");
			        serverSocket = serverServerSocket.accept();
			    		serverFile= // Calculating File name.
			    				AppFolders.resolveFile( testServerFileString );
				  		long serverFileLastModifiedL= serverFile.lastModified();
			    		tryTransferingFileL( 
			    				serverSocket, serverFile, serverFileLastModifiedL );
			      } catch (IOException ex) {
			    		appLogger.info("run() IOException",ex);
			      } finally {
			    		///dbg appLogger.info("run() closing resources.");
			      	Closeables.closeWithErrorLoggingB(serverSocket);
			      	Closeables.closeWithErrorLoggingB(serverServerSocket);
			        }
					}
			
			} // TCPServer 
	  
		private static long tryTransferingFileL(
				Socket theSocket, File localFile, long localLastModifiedL)
		  throws IOException
		  /* This method communicates with the peer on the other end of
		    the connection on theSocket and compares
		    the LastModefied TimeStamp of the remote file to localLastModifiedL
		    which is the LastModefied TimeStamp of the local file.
		    If they are not equal then the newer file with its newer TimeStamp
		    is copied across the connection and replaces the older file.
		    If the file replaced is the local file then 
		    this method returns that file's new time-stamp,
		    otherwise it returns 0. 
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
								if ( receiveNewerRemoteFileB(
										localFile, socketInputStream, timeStampResultL ) )
									receivedLastModifiedL= timeStampResultL;
						  } else if ( timeStampResultL < 0 ) { // Local file is newer.
						  		sendNewerLocalFileV(
			  				localFile, socketOutputStream );
						  } else ; // Files are same age, so do nothing.
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
						localFileInputStream= new FileInputStream(localFile);
						TCPCopier.copyStreamBytesV( 
								localFileInputStream, socketOutputStream);
					} finally {
			  		Closeables.closeWithErrorLoggingB(localFileInputStream);
						appLogger.info("sendNewerLocalFileV() file sent.");
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
				FileOutputStream temporaryFileOutputStream= null;
				boolean fileWriteCompleteB= false;
				File temporaryFile= null;
				try { 
						temporaryFile= AppFolders.resolveFile( "Temporary.txt" );
						temporaryFileOutputStream= new FileOutputStream( temporaryFile );
						TCPCopier.copyStreamBytesV( 
								socketInputStream, temporaryFileOutputStream);
						fileWriteCompleteB= true;
					} finally { 
						Closeables.closeWithErrorLoggingB(temporaryFileOutputStream);
				  }
				if (fileWriteCompleteB) { // More processing if write completed. 
						localFile.setLastModified(timeStampToSetL); // Update time stamp.
						Path temporaryPath= temporaryFile.toPath(); //convert to Path.
						Path localPath = localFile.toPath(); //convert to Path.
						Files.move( // Rename file, replacing existing file with same name.
								temporaryPath, localPath, StandardCopyOption.REPLACE_EXISTING, 
								StandardCopyOption.ATOMIC_MOVE);
					  appLogger.info("receiveNewerRemoteFileB() file received.");
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
	      ///dbg appLogger.info("copyStreamBytesV() beginning.");
	  		while (true) { int byteI;
	      	try { byteI= theInputStream.read(); } // Receive byte. 
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
	      	}
	      ///dbg appLogger.info("copyStreamBytesV() ended.");
	    	}
	  
		private static long exchangeAndCompareFileTimeStampsRemoteToLocalL( 
				InputStream socketInputStream, 
				OutputStream socketOutputStream, 
	  		long localLastModifiedL
				)
	    /* 
	  		Returns +lastModified time of remote file 
	  		if it is newer than local file.
	  		Returns -lastModified time of local file 
	  		if it is newer than remote file.
	  		Returns 0 if the two files have the same lastModified time.
	  		
	  		localLastModified is used as the time-stamp of the local file.
	  		It is also sent to the remote peer.
	  		The time-stamp of the remote file is received from the remote peer.
	      
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
	    					10*remoteLastModifiedL+Character.digit(socketC, 10); 
	      		socketByteI= socketInputStream.read(); // Read next byte.
	      		}
	    		while (true) { // Skip characters through '#' or to end of input.
	    			if (socketByteI==-1) break; // Exit if end of stream.
	    			if (socketByteI==(int)'#') break; // Exit if terminator found. 
	      		socketByteI= socketInputStream.read(); // Read next byte.
	      		}
	  			}
	  		long compareResultL= remoteLastModifiedL -  localLastModifiedL;
	  		if (compareResultL > 0 ) 
	  			  compareResultL= remoteLastModifiedL;
	  			else if (compareResultL < 0 ) 
	  			  compareResultL= -localLastModifiedL;
	  		appLogger.info(
	  				"exchangeAndCompareFileTimeStampsL() returning "+compareResultL);
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
