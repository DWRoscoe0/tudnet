package allClasses;

// The following code was based on code gotten from
// https://stackoverflow.com/questions/4687615/how-to-achieve-transfer-file-between-client-and-server-using-java-socket

import java.io.*;
import java.net.*;

import static allClasses.Globals.appLogger;


public class TCPCopier {
	
	private final static String serverIP = "127.0.0.1";
  private final static int serverPort = 11111;
  private final static String inFileString = "TCPCopierIn.txt";
  private final static String outFileString = "TCPCopierOut.txt";
	
	static class TCPClient extends EpiThread {

    public TCPClient(String threadNameString) { // Constructor.
			super(threadNameString);
			}
    
	  private File clientFile= null;

    public void run()
      // This is the main method of the Client thread.
      {
  			appLogger.consoleInfo("TCPClient beginning.");
	    	EpiThread.interruptableSleepB(10000); ///tmp ///dbg organize log.
	    	while ( ! EpiThread.exitingB() ) // Repeatedly try getting new file.
	    		{ 
	    			tryGettingV();
						appLogger.consoleInfo("TCPClient sleeping 5s before next attempt.");
				    EpiThread.interruptableSleepB(5000); // Sleep 5s to prevent hogging.
	    			}
  			appLogger.consoleInfo("TCPClient ending.");
	    	}
    
		private void tryGettingV()
			{
		    Socket clientSocket = null;
		    InputStream clientSocketInputStream = null;
		    try {
	        clientSocket = new Socket( serverIP , serverPort );
	        clientSocketInputStream = clientSocket.getInputStream();
					if (clientSocketInputStream != null) { 
			  		clientFile= // Calculating File name in standard location.
			      		AppFolders.resolveFile( outFileString );
				    FileOutputStream clientFileOutputStream= 
				    		new FileOutputStream( clientFile );
						clientTransactionV( 
								clientSocketInputStream, clientFileOutputStream );
		        clientFileOutputStream.close();
						}
	        clientSocket.close();
		    } catch (ConnectException ex) {
		  		appLogger.consoleInfo("TCPClient socket connection."+ex);
		    } catch (IOException ex) {
		  		appLogger.exception("TCPClient socketting.",ex);
		    }
			}
	    
		private void clientTransactionV( 
				InputStream theInputStream, OutputStream theOutputStream )
		  throws IOException
			{
	      appLogger.consoleInfo(
	      		"TCPClient made connection, beginning transaction.");
        while (true) {
        	int byteI= theInputStream.read();
        	if ( byteI == -1 ) break;
        	theOutputStream.write(byteI);
        	}
	      appLogger.consoleInfo(
	      		"TCPClient transaction ended, breaking connection.");
	    	}

	}

	
	static class TCPServer extends EpiThread {
	
	  private File serverFile= null;

    public TCPServer(String threadNameString) { // Constructor.
			super(threadNameString);
    	}

    public void run() {
  		appLogger.consoleInfo("TCPServer beginning.");
    	EpiThread.interruptableSleepB(5000); ///tmp Prevent initial error.
      while (true) {
        ServerSocket serverServerSocket = null;
        Socket serverSocket = null;
        OutputStream serverSocketOutputStream = null;
        try {
            serverServerSocket = new ServerSocket(serverPort);
        		appLogger.consoleInfo("TCPServer trying ServerSocket.accept().");
            serverSocket = serverServerSocket.accept();
            serverServerSocket.close();
            serverSocketOutputStream = serverSocket.getOutputStream();
          } catch (IOException ex) {
        		appLogger.exception("TCPServer socketting",ex);
          }
        if (serverSocketOutputStream != null) {
      		serverFile= // Calculating File name.
  	      		AppFolders.resolveFile( inFileString );
          FileInputStream serverFileInputStream = null;
          try {
              serverFileInputStream = new FileInputStream(serverFile);
            } catch (FileNotFoundException ex) {
          		appLogger.exception("TCPServer opening file.",ex);
            }
          try { // Send all bytes of file and close.
          		serverTransactionV( 
          				serverFileInputStream, serverSocketOutputStream );
              serverSocketOutputStream.close();
              serverSocket.close();
            } catch (IOException ex) {
          		appLogger.exception("TCPServer reading and closing.",ex);
            }
      		}
    		appLogger.consoleInfo("TCPServer sleeping 10s.");
		    EpiThread.interruptableSleepB(10000); // Sleep 10s to prevent hogging.
    		appLogger.consoleInfo("TCPServer loop ending.");
      	} // while (true)
    	}
    
	private void serverTransactionV( 
			InputStream theInputStream, OutputStream theOutputStream )
	  throws IOException
		{
  		appLogger.consoleInfo("TCPServer made connection, beginnig transaction.");
  	  while (true) {
  	  	int byteI= theInputStream.read();
  	  	if (byteI == -1) break;
  	  	theOutputStream.write(byteI);
    	  }
  		appLogger.consoleInfo("TCPServer transaction ended, breaking connection.");
    	}

		}

	}
