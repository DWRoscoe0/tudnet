package allClasses;

import java.net.DatagramSocket;
import java.net.SocketException;

public class EpiDatagramSocket extends DatagramSocket

  {
    private EpiDatagramSocket() // Never-used constructor to satisfy compiler. 
      throws SocketException 
      {
        super(); // Auto-generated constructor stub
        }

    static public boolean isNullOrClosedB( DatagramSocket theDatagramSocket )
      {
         return (
             ( theDatagramSocket == null ) || ( theDatagramSocket.isClosed() )
             ) ;
         }

    static public void closeIfNotNullV( DatagramSocket theDatagramSocket )
      {
        Closeables.closeAndReportErrorsV(theDatagramSocket);
        }
    
    }
