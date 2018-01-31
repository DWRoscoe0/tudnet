package allClasses;

// The following code was based on code gotten from
// https://stackoverflow.com/questions/4687615/how-to-achieve-transfer-file-between-client-and-server-using-java-socket

import java.io.*;
import java.net.*;

import static allClasses.Globals.appLogger;


public class TCPCopier {

	private final static String serverIP = "127.0.0.1";
  private final static int serverPort = 11111;
  private final static String serverFileString = "TCPCopierServer.txt";
  private final static String clientFileString = "TCPCopierClient.txt";

	static class TCPClient extends EpiThread {

    public TCPClient(String threadNameString) { // Constructor.
			super(threadNameString);
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
  			
	    	EpiThread.interruptableSleepB(4000); ///tmp ///dbg organize log.
      	boolean oldConsoleModeB= appLogger.getAndEnableConsoleModeB(); ///tmp ///dbg
      	
	    	while ( ! EpiThread.exitingB() ) // Repeatedly try getting update file.
	    		{ 
	    			tryGettingFileV();
						///dbg appLogger.info("run() sleeping 5s before next attempt.",true);
				    EpiThread.interruptableSleepB(8000); // Sleep 8s to prevent hogging.
	    			}
  			appLogger.info("run() ending.",true);
  			
      	appLogger.restoreConsoleModeV( oldConsoleModeB ); /// tmp ///dbg
	    	}
    
		private void tryGettingFileV()
			{ 
				Socket clientSocket = null;
				File clientFile= null;
				try {
		      	clientSocket= new Socket( serverIP , serverPort );
			  		clientFile= AppFolders.resolveFile( clientFileString );
			  		transactionV( clientSocket, clientFile );
	        } catch (IOException theIOException) {
		  			appLogger.info("tryGettingFileV()",theIOException);
		    	} finally {
		    		///dbg appLogger.info("tryGettingFileV() closing resources.");
	    		  Closeables.closeCleanlyV(clientSocket);
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
        ServerSocket serverServerSocket = null;
        Socket serverSocket = null;
	    	while ( ! EpiThread.exitingB() ) {  // Repeatedly try providing file. 
	    		///dbg appLogger.info("run() loop beginning.");
		    	try {
	            serverServerSocket = new ServerSocket(serverPort);
	        		///dbg appLogger.info("run() trying ServerSocket.accept().");
	            serverSocket = serverServerSocket.accept();
	            serverServerSocket.close();
	        		serverFile= // Calculating File name.
	        				AppFolders.resolveFile( serverFileString );
	        		transactionV( serverSocket, serverFile );
	          } catch (IOException ex) {
	        		appLogger.info("run() IOException",ex);
			      } finally {
			    		///dbg appLogger.info("run() closing resources.");
              Closeables.closeCleanlyV(serverSocket);
			    		}
	    		///dbg appLogger.info("run() loop ending with 10s sleep.");
			    EpiThread.interruptableSleepB(4000); // Sleep 4s to prevent hogging.
	    		} // while...
      	appLogger.restoreConsoleModeV( oldConsoleModeB );
	    	}
    
		}
  
	private static void transactionV(
			Socket theSocket,
			File localFile
			)
	  throws IOException
	  /*///fix To prevent race condition while updating local files,
	      might need to write to temporary file and then rename with 
        Files.move(movefrom, target, StandardCopyOption.REPLACE_EXISTING).
	   */
		{
			///dbg appLogger.info("transactionV() beginning.");
      InputStream socketInputStream= null;
			OutputStream socketOutputStream = null;
			FileInputStream localFileInputStream = null;
  		FileOutputStream localFileOutputStream= null;
	  	try {
	  			socketInputStream= theSocket.getInputStream();
		  		socketOutputStream= theSocket.getOutputStream();
	      	long timeStampResultL= TCPCopier.exchangeAndCompareFileTimeStampsL(
	      			socketInputStream, socketOutputStream, localFile);
					if ( timeStampResultL > 0 ) { // for server.
				  		localFileOutputStream= new FileOutputStream( localFile );
				  		TCPCopier.copyStreamBytesV( socketInputStream, localFileOutputStream);
				  		Closeables.closeCleanlyV(localFileOutputStream);
				  		localFile.setLastModified(timeStampResultL);
							appLogger.info("transactionV() file received.");
		  			}	else if ( timeStampResultL < 0 ) { // for server.
				  		localFileInputStream= new FileInputStream(localFile);
							TCPCopier.copyStreamBytesV( localFileInputStream, socketOutputStream);
							appLogger.info("transactionV() file sent.");
		  			} else ; // Do nothing because files are same age.
		    } catch (IOException ex) {
		  		appLogger.info("transactionV() IOException",ex);
		    } finally {
		  		///dbg appLogger.info("transactionV() closing resources.");
		  		Closeables.closeCleanlyV(localFileInputStream);
		  		Closeables.closeCleanlyV(localFileOutputStream);
		  		}
  	  ///dbg appLogger.info("transactionV() ending.");
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
  
	private static long exchangeAndCompareFileTimeStampsL( 
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
  				////Long.compareUnsigned(remoteLastModifiedL, localLastModifiedL);
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
