package allClasses;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class ConnectionMaker extends Thread {

  /* This Singleton class makes and maintains simple UDP unicast connections.  
    It listens for connection requests from other nodes and accepts them.
    It also can be used to request a connection with another node.
    */
  
  private static ConnectionMaker theConnectionMaker= null;
    
  private ConnectionMaker()  // private to make this a singleton.
    throws IOException
    {
      connectionSocket = new DatagramSocket(PortManager.getLocalPortI());
      }
    
  public void activateV( )
    /* Normally this method is called at app start-up.
      It makes certain that the NetworkThread 
      which does network communication is running.
      It calls nothing but its class loader will
      construct and start the thread.
      */
    { 
      start();  // Start ConnectionMaker Thread.
      }

  public static ConnectionMaker getTheConnectionMaker()
    throws IOException
    /* Returns the Singleton ConnectionMaker.  */
    {
      if ( theConnectionMaker == null )  // Make the  singleton if needed.
      {
        theConnectionMaker= new ConnectionMaker();
        theConnectionMaker.setName( "ConnectionMaker" ); // For easy spotting.
        }
      return  theConnectionMaker;
      }

	public void requestConnectionV( InetAddress inInetAddress ) 
    throws IOException
    /* This method requests a connection with the node at inInetAddress. */
	  {
      DatagramSocket aDatagramSocket = new DatagramSocket();

      byte[] bytes= new byte[1];
      bytes[0]= 0;
      DatagramPacket packet= 
        new DatagramPacket(
          bytes, bytes.length, inInetAddress, PortManager.getLocalPortI()
          );
      aDatagramSocket.send(packet);
  
      // Get response.  Presently it just gets one.
      packet = new DatagramPacket(bytes, bytes.length);
      aDatagramSocket.receive(packet);

      // Display response
      String received = new String(packet.getData(), 0, packet.getLength());
      System.out.println("Quote of the Moment: " + received);
  
      aDatagramSocket.close();
		  }


    protected DatagramSocket connectionSocket = null;
    protected boolean makeConnectionsB = true;

    public void run() {

        while (makeConnectionsB) {
            try {
                byte[] buf = new byte[256];  // Create buffer.

                // receive request
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                connectionSocket.receive(packet);

                // figure out response
                String dString = "anything";

                buf = dString.getBytes();

		// send the response to the client at "address" and "port"
                InetAddress address = packet.getAddress();
                int port = packet.getPort();
                packet = new DatagramPacket(buf, buf.length, address, port);
                connectionSocket.send(packet);
            } catch (IOException e) {
                e.printStackTrace();
                makeConnectionsB = false;
            }
        }
        connectionSocket.close();
    }

  }
