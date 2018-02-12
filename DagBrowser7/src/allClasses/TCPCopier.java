package allClasses;

// The following code was based on code gotten from
// https://stackoverflow.com/questions/4687615/how-to-achieve-transfer-file-between-client-and-server-using-java-socket

import java.io.*;
import java.net.*;

import static allClasses.Globals.appLogger;


public class TCPCopier 

  /* This class is supposed to be a simple software updater
    which can run in spite of other app malfunctions.
    It must be kept as simple as possible and
    it must rely on as few other modules as possible.
    
    ///enh Eventually it should maintain its own data file.
      Any peers with which it communicates for updating may be added
      only by it and only after it tests them for operability.
      These peers can be supplied by by the connection manager
      when it discovers them.
    */

	{
	
		//private final static String testServerIPString = "127.0.0.1";
	  private final static int testServerPortI = 11111;
	  private final static String testServerFileString = "TCPCopierServer.txt";
	  private final static String testClientFileString = "TCPCopierClient.txt";
	
		static class TCPClient extends EpiThread {
	
		  private final Persistent thePersistent;
	
	    public TCPClient(  // Constructor.
	  			String threadNameString, Persistent thePersistent) 
	    {
				super(threadNameString);
	
			  this.thePersistent= thePersistent;
				}
	    
	    public void run()
	      /* This is the main method of the Client thread.
	        Presently it only receives bytes from the server and
	        stores them in its file.
	        Later it will sent the Last-Modified value of its file,
	        and only store file data bytes if the client's Last-Modified value.  
	        */
	      {
	  			appLogger.info("run() beginning.",true);
	
		    	EpiThread.interruptableSleepB(4000); ///dbg delay to organize log.
	      	boolean oldConsoleModeB= appLogger.getAndEnableConsoleModeB(); ///tmp ///dbg
	
	      	PersistentCursor thePersistentCursor= 
	      			new PersistentCursor( thePersistent );
	      	thePersistentCursor.setListNameStringV("peers");
		    	while ( ! EpiThread.exitingB() ) // Repeat until exit requested.
		    		{ // Process entire peer list.
			    		{ 
			    			tryExchangingFileWithNextPeerString(thePersistentCursor);
								///dbg appLogger.info("run() sleeping 5s before next attempt.",true);
						    EpiThread.interruptableSleepB(8000); // Sleep 8s to prevent hogging.
			    			}
	      			}
	  			appLogger.info("run() ending.",true);
	
	      	appLogger.restoreConsoleModeV( oldConsoleModeB ); /// tmp ///dbg
		    	}
	
			private void tryExchangingFileWithNextPeerString( 
					PersistentCursor thePersistentCursor)
			  /* Tries to exchange a file with next peer in present list.
			   */
				{
				  if ( thePersistentCursor.getEntryIDNameString().isEmpty() ) { 
				      ; // Do nothing because peer list is wrapping-around.
				  	} else { // Process peer list element.
							String serverIPString= thePersistentCursor.getFieldString("IP");
							String serverPortString= thePersistentCursor.getFieldString("Port");
				  		tryExchangingFileWithPeerV(serverIPString,serverPortString);
					  }
				  thePersistentCursor.nextString();
					}
	
			private void tryExchangingFileWithPeerV(
					String serverIPString, String serverPortString)
				{
					Socket clientSocket = null;
					File clientFile= AppFolders.resolveFile( testClientFileString );
					int serverPortI= Integer.parseUnsignedInt( serverPortString );
					try {
							clientSocket= new Socket( serverIPString, serverPortI );
				  		transactionV( clientSocket, clientFile );
				    } catch (IOException theIOException) {
							appLogger.info("tryGettingFileV()",theIOException);
				  	} finally {
						  Closeables.closeWithErrorLoggingB(clientSocket);
						}
					}
	
		}
	
		
		static class TCPServer extends EpiThread {
		
		  private File serverFile= null;
	
	    public TCPServer(String threadNameString) { // Constructor.
				super(threadNameString);
	    	}
	
	    public void run() 
		    /* This is the main method of the Server thread.
		      It repeatedly waits for client connections and processes them.
		      
		      For each connection it presently 
		      only sends the Last-Modified value of its file,
			    followed by the bytes of its file.
			    
			    Later it will first decode a Last-Modified string from the client,
			    and the server will only send its file data if
			    its Last-Modified value is later that the client's Last-Modified value.
			    It will send its Last-Modified value in either case.
			    */
		    {
		  		appLogger.info("run() beginning.",true);
		    	///dbg EpiThread.interruptableSleepB(5000); ///tmp Prevent initial error.
		    	EpiThread.interruptableSleepB(2000); // Organize log.
	    		boolean oldConsoleModeB= appLogger.getAndEnableConsoleModeB();
		    	while ( ! EpiThread.exitingB() ) {  // Repeatedly try providing file. 
		    		///dbg appLogger.info("run() loop beginning.");
		    		trySendingFileV();
		    		///dbg appLogger.info("run() loop ending with 10s sleep.");
				    EpiThread.interruptableSleepB(4000); // Sleep 4s to prevent hogging.
		    		} // while...
	      	appLogger.restoreConsoleModeV( oldConsoleModeB );
		    	}
	
			private void trySendingFileV()
				{
			    ServerSocket serverServerSocket= null;
			    Socket serverSocket= null;
			  	try {
			        serverServerSocket = new ServerSocket(testServerPortI);
			    		///dbg appLogger.info("run() trying ServerSocket.accept().");
			        serverSocket = serverServerSocket.accept();
			    		serverFile= // Calculating File name.
			    				AppFolders.resolveFile( testServerFileString );
			    		transactionV( serverSocket, serverFile );
			      } catch (IOException ex) {
			    		appLogger.info("run() IOException",ex);
			      } finally {
			    		///dbg appLogger.info("run() closing resources.");
			      	Closeables.closeWithErrorLoggingB(serverSocket);
			      	Closeables.closeWithErrorLoggingB(serverServerSocket);
			        }
					}
			
			}
	  
		private static void transactionV( Socket theSocket, File localFile )
		  throws IOException
		  /*///fix To prevent race condition while updating local files,
		      might need to write to temporary file and then rename with 
	        Files.move(movefrom, target, StandardCopyOption.REPLACE_EXISTING).
		   */
			{
				///dbg appLogger.info("transactionV() beginning.");
	      InputStream socketInputStream= null;
				OutputStream socketOutputStream = null;
		  	try {
		  			socketInputStream= theSocket.getInputStream();
			  		socketOutputStream= theSocket.getOutputStream();
		      	long timeStampResultL= 
		      			TCPCopier.exchangeAndCompareTimeStampsRemoteToLocalL(
		      				socketInputStream, socketOutputStream, localFile);
						if ( timeStampResultL > 0 ) receiveNewerRemoteFileV(
								localFile,  socketInputStream, timeStampResultL );
			  			else if ( timeStampResultL < 0 ) sendNewerLocalFileV(
			  				localFile, socketOutputStream );
			  			else ; // Do nothing because files are same age.
			    } catch (IOException ex) {
			  		appLogger.info("transactionV() IOException",ex);
			    } finally {
			  		///dbg appLogger.info("transactionV() closing resources.");
			  		}
	  	  ///dbg appLogger.info("transactionV() ending.");
	    	}
	
		private static void sendNewerLocalFileV(
				File localFile,
				OutputStream socketOutputStream
				)
			throws IOException
		  ///fix close cleanly.
			{ // Local file newer.
				FileInputStream localFileInputStream= null;
				try { 
						localFileInputStream= new FileInputStream(localFile);
						TCPCopier.copyStreamBytesV( localFileInputStream, socketOutputStream);
					} finally {
			  		Closeables.closeWithErrorLoggingB(localFileInputStream);
						appLogger.info("sendNewerLocalFileV() file sent.");
					}
				}
		
		private static void receiveNewerRemoteFileV(
				File localFile, 
				InputStream socketInputStream, 
				long timeStampToSetL
				)
			throws IOException
			{
				FileOutputStream localFileOutputStream= null;
				boolean fileWriteCompleteB= false;
				try { 
						localFileOutputStream= new FileOutputStream( localFile );
						TCPCopier.copyStreamBytesV( socketInputStream, localFileOutputStream);
						fileWriteCompleteB= true;
					} finally { 
						Closeables.closeWithErrorLoggingB(localFileOutputStream);
				  }
				if (fileWriteCompleteB) { // Set time stamp and log if write completed. 
					localFile.setLastModified(timeStampToSetL);
					appLogger.info("receiveNewerRemoteFileV() file received.");
					}
				}
	
		private static void copyStreamBytesV( 
				InputStream theInputStream, OutputStream theOutputStream)
		  throws IOException
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
	  
		private static long exchangeAndCompareTimeStampsRemoteToLocalL( 
				InputStream socketInputStream, 
				OutputStream socketOutputStream, 
				File theFile
				)
	    /* 
	  		Returns +lastModified time of remote file if it is newer than local file.
	  		Returns -lastModified time of local file if it is newer than remote file.
	  		Returns 0 if the two files have the same lastModified time.
	      
	      A file that does not exist will be considered to have
	      a lastModified time-stamp of 0, which is considered infinitely old.
	     */
		  throws IOException
			{
	  		long localLastModifiedL= theFile.lastModified();
	  		long remoteLastModifiedL= 0; // Initial accumulator value.
	  		{ // Send digits of local file time stamp to remote end, plus terminator.
	  			TCPCopier.sendDigitsOfNumberV( socketOutputStream, localLastModifiedL);
		  		socketOutputStream.write( (byte)('\n') );
		  		socketOutputStream.write( (byte)('#') );
	  			socketOutputStream.flush();
	  			}
	  		{ // Receive and decode digits of remote file time stamp.
	    		remoteLastModifiedL= 0;
	    		int socketI= socketInputStream.read(); // Read first byte.
	  			char socketC;
	    		while (true) { // Accumulate all digits of remote file time-stamp.
	    			if (socketI==-1) break; // Exit if end of stream.
	    			socketC= (char)socketI;
	    			if (! Character.isDigit(socketC)) break; // Exit if not digit.
	    			remoteLastModifiedL= // Combine new digit with digit accumulator.  
	    					10*remoteLastModifiedL+Character.digit(socketC, 10); 
	      		socketI= socketInputStream.read(); // Read next byte.
	      		}
	    		while (true) { // Skip characters through '#' or to end of input.
	    			if (socketI==-1) break; // Exit if end of stream.
	    			if (socketI==(int)'#') break; // Exit if terminator found. 
	      		socketI= socketInputStream.read(); // Read next byte.
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
