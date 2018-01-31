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
  			
	    	EpiThread.interruptableSleepB(2000); ///tmp ///dbg organize log.
      	boolean oldConsoleModeB= appLogger.getAndEnableConsoleModeB(); ///tmp ///dbg
      	
	    	while ( ! EpiThread.exitingB() ) // Repeatedly try getting update file.
	    		{ 
	    			tryGettingFileV();
						appLogger.info("run() sleeping 5s before next attempt.",true);
				    EpiThread.interruptableSleepB(5000); // Sleep 5s to prevent hogging.
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
		    		appLogger.info("tryGettingFileV() closing resources.");
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
	    	EpiThread.interruptableSleepB(5000); ///tmp Prevent initial error.
    		boolean oldConsoleModeB= appLogger.getAndEnableConsoleModeB();
        ServerSocket serverServerSocket = null;
        Socket serverSocket = null;
	    	while ( ! EpiThread.exitingB() ) {  // Repeatedly try providing file. 
	    		appLogger.info("run() loop beginning.");
		    	try {
	            serverServerSocket = new ServerSocket(serverPort);
	        		appLogger.info("run() trying ServerSocket.accept().");
	            serverSocket = serverServerSocket.accept();
	            serverServerSocket.close();
	        		serverFile= // Calculating File name.
	        				AppFolders.resolveFile( serverFileString );
	        		transactionV( serverSocket, serverFile );
	          } catch (IOException ex) {
	        		appLogger.info("run() IOException",ex);
			      } finally {
			    		appLogger.info("run() closing resources.");
              Closeables.closeCleanlyV(serverSocket);
			    		}
	    		appLogger.info("run() loop ending with 10s sleep.");
			    EpiThread.interruptableSleepB(10000); // Sleep 10s to prevent hogging.
	    		} // while...
      	appLogger.restoreConsoleModeV( oldConsoleModeB );
	    	}
    
		}
  
	private static void transactionV(
			Socket theSocket,
			File localFile
			)
	  throws IOException
		{
			appLogger.info("transactionV() beginning.");
      InputStream socketInputStream= null;
			OutputStream socketOutputStream = null;
			FileInputStream localFileInputStream = null;
  		FileOutputStream localFileOutputStream= null;
	  	try {
    		//Marker2
	  			socketInputStream= theSocket.getInputStream();
		  		socketOutputStream= theSocket.getOutputStream();
	      	int compareResultI= TCPCopier.exchangeAndCompareFileTimeStampsI(
	      			socketInputStream, socketOutputStream, localFile);
					if ( compareResultI > 0 ) { // for server.
				  		localFileOutputStream= new FileOutputStream( localFile );
				  		TCPCopier.copyStreamBytesV( socketInputStream, localFileOutputStream);
		  			}	else if ( compareResultI < 0 ) { // for server.
				  		localFileInputStream= new FileInputStream(localFile);
							TCPCopier.copyStreamBytesV( localFileInputStream, socketOutputStream);
		  			}
	  			//Marker1
		    } catch (IOException ex) {
		  		appLogger.info("transactionV() IOException",ex);
		    } finally {
		  		appLogger.info("transactionV() closing resources.");
		  		Closeables.closeCleanlyV(localFileInputStream);
		  		Closeables.closeCleanlyV(localFileOutputStream);
		  		}
  	  appLogger.info("transactionV() ending.");
    	}
  
	private static void copyStreamBytesV( 
			InputStream theInputStream, OutputStream theOutputStream)
	  throws IOException
		{
      appLogger.info("copyStreamBytesV() beginning.");
  		while (true) { int byteI;
      	try { byteI= theInputStream.read(); } // Receive byte. 
		    catch (IOException theIOException) { 
		    	appLogger.info("copyStreamBytesV() reading",theIOException);
		    	throw theIOException; // Re-throw.
		    	}
  	  	appLogger.info("copyStreamBytesV() byteI="+byteI+" "+(char)byteI);
      	if ( byteI == -1 ) break;
      	try { theOutputStream.write(byteI); } // Write byte.
		    catch (IOException theIOException) { 
		    	appLogger.info("copyStreamBytesV() writing",theIOException);
		    	throw theIOException; // Re-throw.
		    	}
      	}
      appLogger.info("copyStreamBytesV() ended.");
    	}
  
	private static int exchangeAndCompareFileTimeStampsI( 
			InputStream socketInputStream, 
			OutputStream socketOutputStream, 
			File theFile
			)
    /* 
      Returns result > 0 if remote file is newer than local file.
      Returns result == 0 if remote file is same age as local file.
      Returns result < 0 if remote file is older than local file.
      
      A file that does not exist will be considered to have
      a time-stamp of 0, which is considered infinitely old.
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
  		int compareResultI= 
  				Long.compareUnsigned(remoteLastModifiedL, localLastModifiedL);
  		appLogger.info(
  				"exchangeAndCompareFileTimeStampsI() returning "+compareResultI);
			return compareResultI;
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
