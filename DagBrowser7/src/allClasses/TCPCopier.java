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
    
	  private File clientFile= null;
    private Socket clientSocket = null;
    private InputStream clientSocketInputStream = null;
    private FileOutputStream clientFileOutputStream= null; 
    private PrintWriter clientSocketPrintWriter= null;

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
	    	while ( ! EpiThread.exitingB() ) // Repeatedly try getting update file.
	    		{ 
	    			tryGettingFileV();
						appLogger.info("run() sleeping 5s before next attempt.",true);
				    EpiThread.interruptableSleepB(5000); // Sleep 5s to prevent hogging.
	    			}
  			appLogger.info("run() ending.",true);
	    	}
    
		private void tryGettingFileV()
			{ 
				try {
		      	clientSocket = new Socket( serverIP , serverPort );
			      clientSocketInputStream = clientSocket.getInputStream();
			  		clientFile= // Calculating File name in standard location.
			      		AppFolders.resolveFile( clientFileString );
				    clientFileOutputStream= new FileOutputStream( clientFile );
				    clientSocketPrintWriter=
				    		new PrintWriter(clientFileOutputStream);
				    clientTransactionV();
		  		} catch (ConnectException ex) {
	        	appLogger.info("tryGettingFileV() socket connection. "+ex,true);
	        } catch (IOException theIOException) {
		  			appLogger.exception("tryGettingFileV() new Socket.",theIOException);
		    	} finally {
	    		  Closeables.closeCleanlyV(clientFileOutputStream);
	    		  Closeables.closeCleanlyV(clientSocket);
					}
			}
	    
		private void clientTransactionV()
		  throws IOException
			{
	      appLogger.info(
	      		"clientTrnsactionV() made connection, beginning transaction.",true);
	  		clientSocketPrintWriter.print( clientFile.lastModified());
        while (true) { int byteI;
        	try { byteI= clientSocketInputStream.read(); } // Receive byte. 
			    catch (IOException theIOException) { 
			    	appLogger.exception("clientTransaction() read()",theIOException);
			    	throw theIOException; // Re-throw.
			    	}
        	///dbg appLogger.info(clientTrnsactionV() byteI="+byteI+" "+(char)byteI,true);
        	if ( byteI == -1 ) break;
        	try { clientFileOutputStream.write(byteI); } // Write byte.
			    catch (IOException theIOException) { 
			    	appLogger.exception("clientTransaction() write()",theIOException);
			    	throw theIOException; // Re-throw.
			    	}
        	}
	      appLogger.info(
	      		"clientTrnsactionV() transaction ended, breaking connection.",true);
	    	}

	}

	
	static class TCPServer extends EpiThread {
	
	  private File serverFile= null;
	  private FileInputStream serverFileInputStream = null;
	  private PrintWriter serverSocketPrintWriter= null;

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
	    	while ( ! EpiThread.exitingB() ) 
		    	{ // Repeatedly try providing new file.
		        ServerSocket serverServerSocket = null;
		        Socket serverSocket = null;
		        OutputStream serverSocketOutputStream = null;
		        try {
		            serverServerSocket = new ServerSocket(serverPort);
		        		appLogger.info("run() trying ServerSocket.accept().",true);
		            serverSocket = serverServerSocket.accept();
		            serverServerSocket.close();
		            serverSocketOutputStream= serverSocket.getOutputStream();
		            serverSocketPrintWriter= new PrintWriter(serverSocketOutputStream);
		            
		          } catch (IOException ex) {
		        		appLogger.exception("run() socketting",ex);
		          }
		        if (serverSocketOutputStream != null) {
		      		serverFile= // Calculating File name.
		  	      		AppFolders.resolveFile( serverFileString );
		          try {
		              serverFileInputStream = new FileInputStream(serverFile);
		            } catch (FileNotFoundException ex) {
		          		appLogger.exception("run() opening file.",ex);
		            }
		          try { // Send all bytes of file and close.
		          		serverTransactionV();
		              ////serverSocketOutputStream.close();
		          		serverSocketPrintWriter.close();
		              serverSocket.close();
		            } catch (IOException ex) {
		          		appLogger.exception("run() reading and closing.",ex);
		            }
		      		}
		    		appLogger.info("run() sleeping 10s.",true);
				    EpiThread.interruptableSleepB(10000); // Sleep 10s to prevent hogging.
		    		appLogger.info("run() loop ending.",true);
		      	}
	    	}
    
	private void serverTransactionV()
	  throws IOException
		{
  		appLogger.info("serverTransactionV() made connection, beginnig transaction.",true);
  		serverSocketPrintWriter.print( serverFile.lastModified());
  		serverSocketPrintWriter.println(); // Terminate previous line of data.
  		serverSocketPrintWriter.println(); // Output a blank line.
  	  while (true) {
  	    	int byteI;
  	    	try { byteI= serverFileInputStream.read(); }
			    catch (IOException theIOException) { 
			    	appLogger.exception("serverTransaction()",theIOException);
			    	throw theIOException; // Re-throw.
			    	}
	  	  	///dbg appLogger.info(serverTransactionV() byteI="+byteI+" "+(char)byteI,true);
	  	  	if (byteI == -1) break;
	  	  	serverSocketPrintWriter.write(byteI);
					}
	  	 appLogger.info("serverTransactionV() transaction ended, breaking connection.",true);
    	}

		}

	}
