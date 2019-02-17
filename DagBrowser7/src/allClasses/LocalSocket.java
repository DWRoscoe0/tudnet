package allClasses;

import static allClasses.Globals.appLogger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class LocalSocket

  /* This class encapsulates a TCP server socket.
    It can be used to accept connections from,
    and receive command lines from other apps,
    or more likely, other instances of the same app. 
 
    This Socket code originally came from AppInstanceManager.

    */

  {
    private ServerSocket theServerSocket = null;

    private Socket clientSocket= null; 

    private CommandArgs theCommandArgs = null; // For message output.

    public void initializeV( int portI )
      throws IOException
      {
        theServerSocket =  // Try opening listener socket.
            new ServerSocket(
              portI, 10, InetAddress.getLoopbackAddress() 
              );
        }

    // Beginning of methods that appear in a loop.
    
    public void acceptV()
      throws IOException
      {
        clientSocket= // Wait until accept or exception. 
            theServerSocket.accept();
        }

    public void inputFromConnectionV()
      throws IOException
      /* This method processes a line from just opened socket theSocket.
        */
      {
        BufferedReader inBufferedReader= 
          new BufferedReader(
             new InputStreamReader(clientSocket.getInputStream()
             )
           );
         String readString = inBufferedReader.readLine();
         appLogger.info(
           "======== RECEIVED LINE VIA TCP FROM ANOTHER APP. ======== :\n  " 
           + readString
           );
         theCommandArgs= // Parse string into separate string arguments using
             new CommandArgs(readString.split("\\s")); // white-space as delimiters.
         inBufferedReader.close();
         }
    
    public CommandArgs getCommandArgs()
      /* This method returns the previously parsed CommandArgs,
        set stored by processLineFromSocketV(..). 
        */
      { return theCommandArgs; }
    
    public void closeConnectionV()
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

    // End of methods that appear in a loop.

    public boolean isClosedB()
      /* This method returns true if the ServerSocket is closed,
        false otherwise.
        */
      {
        return (theServerSocket == null);
        }
      

      public void closeAllV()
        /* This method closes theServerSocket.
          It is commonly used to end a wait for acceptance of 
          connections to that server socket,
          ending a loop, and ending the thread that contains that loop.
          If the connection is open, it is closed first.
          */
        {
          closeConnectionV(); // Close the single associated socket connection.
          try {
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
