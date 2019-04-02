package allClasses;

import static allClasses.Globals.appLogger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class LocalSocket

  /* This class is used for simple communication between
    processes on the local computer.  
    It does this using a TCP server socket on the loopback interface.
    
    It can be used for 2 things:
    * To signal the existence of this process to other processes.
    * To communicate commands from other processes to this process.
    It is typically used to coordinate multiprocess applications,
    including the case of multiple instances of the same process.

    It has the following attributes:
    * It performs some of the functions of a TCP server socket,
      such as binding to a port that is not already bound elsewhere.
    * Is limited to use on the LocalHost loopback network interface.
    * It has a couple of options for specifying what port to use.
    * It can accept connections on the ServerSocket,
      create a socket, and process data received on that socket. 
    * If a port is already bound by another process,
      it can open a Socket for the purpose of sending commands
      to that other process. 
 
    Much of this Socket code originally came from AppInstanceManager.

    */

  {
    private ServerSocket theServerServerSocket = null;
      /* The Java ServerSocket should have been called something else.
        It is less a socket than it is a socket factory. 
        */

    private Socket theServerSocket= null; 

    private CommandArgs theCommandArgs = null; // For message output.

    public synchronized boolean bindB( int portI )
      /* This is like bindV(..) except that 
        instead of throwing an exception if the bind fails,
        it returns false.  It returns true if the bind succeeds. 
       */
      {
        /// appLogger.debug("bindB(..) begins, portI= "+portI);
        boolean successB= false; // Set default value indicating bind failure.
        try {
            bindV(portI);
            successB= true; // Indicate bind success.
            appLogger.info("bindB(..) success.");
          } catch (IOException e) {
            appLogger.info("bindB(..) failure.  Port probably in use.");
          }
        return successB;
        }

    public synchronized void bindV( int portI )
      throws IOException
      /* Tries to open theServerSocket, 
        binding the ServerSocket to portI on the loopback interface,
        in preparation for receiving connections.
        It tries to bind the ServerSocket to portI.
        It catches IOException, meaning the bind fails,
        and interprets this as portI already being bound.
        If portI=0 then an ephemeral port is used,
        in which case the bind should succeed.
       */
      {
        /// appLogger.debug("bindV(..) begins, portI= "+portI);
        try {
            theServerServerSocket=  // Try opening listener socket.
              new ServerSocket(
                portI, 10, InetAddress.getLoopbackAddress() 
                );
            appLogger.info("bindV(..) success.");
          } catch (IOException e) {
            appLogger.info("bindV(..) failure.  Port probably in use.");
            throw e; // Rethrow exception.
          }
        }

    public synchronized ServerSocket getServerSocket()
      /* This method returns the ServerSocket.  */
      { return theServerServerSocket; }

    public synchronized Socket getSocket()
      /* This method returns the ServerSocket.  */
      { return theServerSocket; }
 
    // Beginning of methods that normally appear in a loop.
    
    public void acceptV()
      throws IOException
      /* Waits for and accepts a connection request,
        It throws an exception if there is an Error.
        */
      {
        theServerSocket= // Wait until accept or exception. 
            theServerServerSocket.accept();
        }

    public synchronized void inputFromConnectionV()
      throws IOException
      /* This method processes a line from the just-opened socket theSocket.
        The line is parsed and the results stored in theCommandArgs.
        It throws an exception if there is an Error.
        */
      {
        BufferedReader inBufferedReader= 
          new BufferedReader(
             new InputStreamReader(theServerSocket.getInputStream()
             )
           );
         String readString = inBufferedReader.readLine();
         appLogger.info(
             "inputFromConnectionV() ======== RECEIVED DATA VIA TCP FROM ANOTHER APP. ======== :\n  " 
             + readString
             );
         theCommandArgs= // Parse string into separate string arguments using
           new CommandArgs(readString.split("\\s")); // white-space delimiters.
         appLogger.debug("inputFromConnectionV(): theCommandArgs created.");
         inBufferedReader.close();
         appLogger.debug("inputFromConnectionV(): inBufferedReader.closed.");
         }
    
    public synchronized CommandArgs getCommandArgs()
      /* This method returns the previously parsed CommandArgs,
        stored by processLineFromSocketV(..). 
        */
      { return theCommandArgs; }
    
    public synchronized void closeConnectionV()
      /* This method closes the Socket connection 
        that was created by accepting a connection on theServerSocket.
        Once this is done, another connection can be accepted and processed.
        */
      { 
        try { 
          if (theServerSocket != null)
            theServerSocket.close(); 
          } catch (IOException e) {
            appLogger.exception("closeConnectionV()",e);
          } 
        theServerSocket= null;
        }

    // End of methods that normally appear in a loop.

    public synchronized boolean isClosedB()
      /* This method returns true if the ServerSocket is closed,
        false otherwise.
        */
      {
        return (theServerServerSocket == null);
        }

    public synchronized void closeAllV()
      /* This method closes theServerSocket.
        It is commonly used to end a wait for acceptance of 
        connections to that server socket,
        ending a loop, and ending the thread that contains that loop.
        If the connection is open, it is closed first.
        */
      {
        closeConnectionV(); // Close any single associated socket connection.
        try { // Close the server socket.
            if (theServerServerSocket != null) 
              theServerServerSocket.close();
          } catch (IOException e) {
            appLogger.exception("LocalSocket.closeV()",e);
          }
        theServerServerSocket= null; // Indicate closed no matter what.
        }

    

    public static synchronized boolean localSendToPortB(
        String outputString, int portI)
      /* This method sends dataString to portI on the loopback interface.
        It sends only a single line, then closes the socket.
        Returns true if the send succeeded, false otherwise.
        */
      {
        boolean successB= false;
        String commonString= "\n  data=\""+outputString+"\", port= "+portI;
        try {
          Socket theClientSocket= // Create socket for send.
            new Socket(InetAddress.getLoopbackAddress(), portI);
          OutputStream theOutputStream= // Get its stream.
              theClientSocket.getOutputStream();
          theOutputStream.write(  // Send output string to other app via stream.
            outputString.getBytes());
          theOutputStream.close();  // Close stream.
          theClientSocket.close();  // Close socket.
          appLogger.info(
            "======== SUCCESS SENDING TCP LOOPBACK PACKET ========"
            +commonString);
          successB= true;  // Packet sent, meaning success and should exit.
        } catch (IOException e1) {
          appLogger.exception(
            "======== FAILED SENDING LOOPBACK PACKET ========" 
            + commonString, e1);
        }
      return successB;
      }
    
    }
