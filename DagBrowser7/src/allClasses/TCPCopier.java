package allClasses;

// The following code was based on code gotten from
// https://stackoverflow.com/questions/4687615/how-to-achieve-transfer-file-between-client-and-server-using-java-socket

import java.io.*;
import java.net.*;

import static allClasses.Globals.appLogger;


public class TCPCopier {

	///enh PrintWriter might be overkill.  Maybe output to OutputStream.
	
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
		      	clientSocket = new Socket( serverIP , serverPort );
			  		clientFile= AppFolders.resolveFile( clientFileString );
				    clientTransactionV(
				    		clientSocket,
								clientFile
				    		);
	        } catch (IOException theIOException) {
		  			appLogger.info("tryGettingFileV()",theIOException);
		    	} finally {
		    		appLogger.info("tryGettingFileV() closing resources.");
	    		  Closeables.closeCleanlyV(clientSocket);
					}
			}
  
		private void clientTransactionV(
				Socket clientSocket,
				File clientFile
				)
		  throws IOException
			{
	      appLogger.info( "clientTrnsactionV() beginning.");
	      InputStream clientSocketInputStream= null;
      	OutputStream clientSocketOutputStream = null;
	      PrintWriter clientSocketPrintWriter= null;
	  		FileOutputStream clientFileOutputStream= null;
	      try {
	      		clientSocketInputStream= clientSocket.getInputStream();
		      	clientSocketOutputStream = clientSocket.getOutputStream();
		      	clientSocketPrintWriter= new PrintWriter(clientSocketOutputStream);
		      	clientSocketPrintWriter.print( clientFile.lastModified());
			  		clientSocketPrintWriter.println(); // Terminate previous line of data.
			  		clientSocketPrintWriter.println(); // Output a blank line.
			  		clientFileOutputStream= new FileOutputStream( clientFile );
			  		TCPCopier.copyStreamBytesV( clientSocketInputStream, clientFileOutputStream);
		      } catch (IOException ex) {
			  		appLogger.info("clientTransactionV() IOException",ex);
			    } finally {
			  		appLogger.info("clientTransactionV() closing resources.");
			  		Closeables.closeCleanlyV(clientSocketPrintWriter);
			  		Closeables.closeCleanlyV(clientFileOutputStream);
			  		}
		  	appLogger.info( "clientTrnsactionV() ending.");
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
          		serverTransactionV(
          			serverSocket,
  	        	  serverFile
	        			);
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
    
	private void serverTransactionV(
			Socket serverSocket,
			File serverFile
			)
	  throws IOException
		{
			appLogger.info("serverTransactionV() beginning.");
			FileInputStream serverFileInputStream = null;
			PrintWriter serverSocketPrintWriter= null;
			OutputStream serverSocketOutputStream = null;
	  	try {
				long localLastModifiedL= serverFile.lastModified();
	  		serverFileInputStream= new FileInputStream(serverFile);
	  		serverSocketOutputStream= serverSocket.getOutputStream();
	  		serverSocketPrintWriter= new PrintWriter(serverSocketOutputStream);
	  		serverSocketPrintWriter.println( localLastModifiedL );
	  		serverSocketPrintWriter.flush();
	  		TCPCopier.copyStreamBytesV( serverFileInputStream, serverSocketOutputStream);
		    } catch (IOException ex) {
		  		appLogger.info("serverTransactionV() IOException",ex);
		    } finally {
		  		appLogger.info("serverTransactionV() closing resources.");
		    	Closeables.closeCleanlyV(serverSocketPrintWriter);
		  		Closeables.closeCleanlyV(serverSocketOutputStream); ///del?
		  		Closeables.closeCleanlyV(serverFileInputStream);
		  		Closeables.closeCleanlyV(serverSocket);
		  		}
  	  appLogger.info("serverTransactionV() ending.");
    	}

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

	}
