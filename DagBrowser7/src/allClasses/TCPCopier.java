package allClasses;

// imports for TCPClient.
//?import java.io.*;

// The following code was based on code gotten from
// https://stackoverflow.com/questions/4687615/how-to-achieve-transfer-file-between-client-and-server-using-java-socket

// imports for TCPServer.
import java.io.*;
import java.net.*;

//?import java.net.*;
import static allClasses.Globals.appLogger;

public class TCPCopier {
	
	private final static String serverIP = "127.0.0.1";
  private final static int serverPort = 11111;
  private final static String inFileString = "TCPCopierIn.txt";
  private final static String outFileString = "TCPCopierOut.txt";
	
	//? package filesendtest;
	
	static class TCPClient extends EpiThread {

    public TCPClient(String nameString) { // Constructor.
			super(nameString);
			}
    
	  private File theFile= null;

    public void run()
      // This is the main method of the Client thread.
      {
  			appLogger.info("TCPClient.run() beginning.");
	    	EpiThread.interruptableSleepB(10000); ///tmp Prevent initial error.
	    	appLogger.info("TCPClient.run() initial delay done.");
	    	while ( ! EpiThread.exitingB() ) // Repeatedly try getting new file.
	    		{ 
	    			appLogger.info("TCPClient.run() loop beginning.");
	    			tryGettingV();
						appLogger.info("TCPClient.tryGettingV() sleeping 5s.");
				    EpiThread.interruptableSleepB(5000); // Sleep 5s to prevent hogging.
	    			appLogger.info("TCPClient.run() loop ending.");
	    			}
  			appLogger.info("TCPClient.run() ending.");
	    	}
    
		private void tryGettingV()
			{
				appLogger.info("TCPClient.tryGettingV() beginning.");
		    Socket clientSocket = null;
		    InputStream is = null;
		    try {
		        clientSocket = new Socket( serverIP , serverPort );
		        is = clientSocket.getInputStream();
						if (is != null) tryCopyingFileFromV( is ); 
		        clientSocket.close();
		    } catch (ConnectException ex) {
		  		appLogger.info("TCPClient.run() socket connection."+ex);
		    } catch (IOException ex) {
		  		appLogger.exception("TCPClient.run() socketting.",ex);
		    }
				
				appLogger.info("TCPClient.tryGettingV() ending.");
			}
	    
		private void tryCopyingFileFromV( InputStream is )
			{
	  		theFile= // Calculating File name in standard location.
	      		AppFolders.resolveFile( outFileString );
	      try {
				    FileOutputStream fos= new FileOutputStream( theFile );
			      appLogger.info("TCPClient starting byte copy.");
	          while (true) {
	          	int byteI= is.read();
	          	if ( byteI == -1 ) break;
	          	fos.write(byteI);
		  		  	appLogger.info("TCPClient copied one byte.");
	          	}
	          //fos.flush();
	          fos.close();
	        } catch (IOException ex) {
	      		appLogger.exception("TCPClient.tryCopyingFileFromV() copying.",ex);
	        }
	    	}

	}

	
	//? package filesendtest;
	
	static class TCPServer extends EpiThread {
	
	  private File theFile= null;

    public TCPServer(String nameString) { // Constructor.
			super(nameString);
    	}

    public void run() {
  		appLogger.info("TCPServer.run() beginning.");
    	EpiThread.interruptableSleepB(5000); ///tmp Prevent initial error.
    	appLogger.info("TCPServer.run() initial delay done.");
      while (true) {
    		appLogger.info("TCPServer.run() loop beginning.");
        ServerSocket theServerSocket = null;
        Socket theSocket = null;
        BufferedOutputStream socketOutputStream = null;
        try {
            theServerSocket = new ServerSocket(serverPort);
            theSocket = theServerSocket.accept();
            theServerSocket.close();
            socketOutputStream = new BufferedOutputStream(theSocket.getOutputStream());
          } catch (IOException ex) {
        		appLogger.exception("TCPServer.run() socketting",ex);
          }
    		appLogger.info("TCPServer.run() request received.");
        if (socketOutputStream != null) {
      		appLogger.info("TCPServer.run() processing request.");
          //% File theFile = new File( inFileString );
      		theFile= // Calculating File name.
  	      		AppFolders.resolveFile( inFileString );
          //byte[] mybytearray = new byte[(int) theFile.length()];
          FileInputStream theFileInputStream = null;
          try {
              theFileInputStream = new FileInputStream(theFile);
            } catch (FileNotFoundException ex) {
          		appLogger.exception("TCPServer.run() opening file.",ex);
            }
          //BufferedInputStream bis = new BufferedInputStream(fis);
          try { // Send all bytes of file and close.
          	  while (true) {
          	  	int byteI= theFileInputStream.read();
          	  	if (byteI == -1) break;
          	  	socketOutputStream.write(byteI);
            		appLogger.info("TCPServer.run() one byte read and sent.");
	          	  }
              //bis.read(mybytearray, 0, mybytearray.length);
              //socketOutputStream.write(mybytearray, 0, mybytearray.length);
              //socketOutputStream.flush();
              socketOutputStream.close();
              theSocket.close();
              // File sent, exit the main method
              //return;
            } catch (IOException ex) {
          		appLogger.exception("TCPServer.run() reading and closing.",ex);
            }
      		}
    		appLogger.info("TCPServer.run() request completed, sleeping 10s.");
		    EpiThread.interruptableSleepB(10000); // Sleep 10s to prevent hogging.
    		appLogger.info("TCPServer.run() loop ending.");
      	} // while (true)
    	}
		}

	}
