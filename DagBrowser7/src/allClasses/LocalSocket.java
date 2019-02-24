package allClasses;

import static allClasses.Globals.appLogger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class LocalSocket

  /* This class performs the following functions:
    * Some of the functions of a TCP server socket,
      such as binding to a port that is not already bound elsewhere.
    * Is limited to use on the LocalHost loopback network interface.
    * It has a couple of options for specifying the port to use.
    * It can accept connections on the ServerSocket,
      create a socket, and process data received on it. 
    * If a port is already bound elsewhere,
      it can open a Socket for the purpose of sending command
      to the process that bound the port. 
    
    It can be used for 2 things:
    * To signal the existence of an older running instance of an app.
    * To communicate commands from one running app instance to another,
      or between different apps.
 
    This Socket code originally came from AppInstanceManager.

    */

  {
    private ServerSocket theServerSocket = null;

    private Socket clientSocket= null; 

    private CommandArgs theCommandArgs = null; // For message output.

    public synchronized void bindV( int portI )
      throws IOException
      /* Tries to opens theServerSocket,
        binding the ServerSocket to portI,
         in preparation for receiving connections.
        It tries to bind the ServerSocket to portI.
        It catches IOException, meaning the bind fails,
        and interprets this as portI already being bound elsewhere.
       */
      {
        appLogger.info("bindB(..) begins, portI= "+portI);
        //// boolean successB= false; // Set default value indicating bind failure.
        try {
            theServerSocket=  // Try opening listener socket.
              new ServerSocket(
                portI, 10, InetAddress.getLoopbackAddress() 
                );
            //// successB= true; // Indicate bind success.
            appLogger.info("bindB(..) success.");
          } catch (IOException e) {
            appLogger.info("bindB(..) failure.  Port probably in use.");
            throw e;
          }
        //// return successB;
        }

    // Beginning of methods that normally appear in a loop.
    
    public void acceptV()
      throws IOException
      /* Waits for and accepts a connection request,
        It throws an exception if there is an Error.
        */
      {
        clientSocket= // Wait until accept or exception. 
            theServerSocket.accept();
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
             new InputStreamReader(clientSocket.getInputStream()
             )
           );
         String readString = inBufferedReader.readLine();
         appLogger.info(
             "inputFromConnectionV() 1 ======== RECEIVED LINE VIA TCP FROM ANOTHER APP. ======== :\n  " 
             + readString
             );
         appLogger.info(
             "inputFromConnectionV() 2 ======== RECEIVED LINE VIA TCP FROM ANOTHER APP. ======== :\n  " 
             + readString
             );
         theCommandArgs= // Parse string into separate string arguments using
             new CommandArgs(readString.split("\\s")); // white-space as delimiters.
         appLogger.debug("inputFromConnectionV(): theCommandArgs created.");
         inBufferedReader.close();
         appLogger.debug("inputFromConnectionV(): inBufferedReader.closeed.");
         }
    
    public synchronized CommandArgs getCommandArgs()
      /* This method returns the previously parsed CommandArgs,
        stored by processLineFromSocketV(..). 
        */
      { return theCommandArgs; }
    
    public synchronized void closeConnectionV()
      /* This method closes the clentSocket connection 
        that was created by accepting a connection on theServerSocket.
        Once this is done, another connection can be accepted and processed.
        */
      { 
        try { 
          if (clientSocket != null)
            clientSocket.close(); 
          } catch (IOException e) {
          } 
        clientSocket= null;
        }

    // End of methods that normally appear in a loop.

    public synchronized boolean isClosedB()
      /* This method returns true if the ServerSocket is closed,
        false otherwise.
        */
      {
        return (theServerSocket == null);
        }
      

    public synchronized void closeAllV()
        /* This method closes theServerSocket.
          It is commonly used to end a wait for acceptance of 
          connections to that server socket,
          ending a loop, and ending the thread that contains that loop.
          If the connection is open, it is closed first.
          */
        {
          closeConnectionV(); // Close the single associated socket connection.
          try {
              if (theServerSocket != null) 
                theServerSocket.close();
            } catch (IOException e) {
              appLogger.error(
                  "LocalSocket.closeV(),"
                  +"Error closing instanceServerSocket: " + e
                  );
              e.printStackTrace();
            }
          theServerSocket= null; // Indicate closed no matter what.
          }
  }
