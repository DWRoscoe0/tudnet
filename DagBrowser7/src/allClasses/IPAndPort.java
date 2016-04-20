package allClasses;

import java.net.InetAddress;

public class IPAndPort 

  //extends InetSocketAddress // Temporary extension while replacing class. 

  /* This class is a mutable replacement for InetSocketAddress.
   It's main purpose is to make Unicaster lookup by 
   remote address from a DatagramPacket faster by reducing
   the number of new-operators performed.
   Unfortunately it doesn't eliminate them because
   DatagramPacket.getAddress() returns a [new] InetAddress, and
   there doesn't appear to be a way to return the IP address another way.
   */

  {
		private InetAddress netcasterInetAddress;
		private int netcasterPortI;

		public IPAndPort( InetAddress netcasterInetAddress, int netcasterPortI )
			{
				this.netcasterInetAddress= netcasterInetAddress;
				this.netcasterPortI= netcasterPortI;
				}

		// Getters.
		public InetAddress getInetAddress() 
	  	{ return netcasterInetAddress; }
		public int getPortI() 
			{ return netcasterPortI; }

		// Setters.
		public void setInetAddressV( InetAddress netcasterInetAddress) 
		  { this.netcasterInetAddress= netcasterInetAddress; }
		public void setPortI( int netcasterPortI )
		  { this.netcasterPortI= netcasterPortI; }

		// Other methods.
	  public boolean equals(Object otherObject) 
      // This is the standard equals() method.  
      {
        boolean resultB = false;  // assume objects are not equal.
        Comparer: {  // Comparer.
          if ( otherObject == null )  // Other object is null.
            break Comparer;  // Exiting with false.
          if ( ! ( otherObject instanceof IPAndPort ) )  // Unequal classes.
            break Comparer;  // Exiting with false.
          IPAndPort otherIPAndPort=  // Creating easy field-access variable.
            (IPAndPort)otherObject; 
          if  // Unequal ports.
            ( this.netcasterPortI != otherIPAndPort.netcasterPortI )
            break Comparer;  // Exiting with false.
          if ( ! Nulls.equals( // Unequal InetAddresses.
          			this.netcasterInetAddress, otherIPAndPort.netcasterInetAddress
          			))
            break Comparer;  // Exiting with false.
          resultB= true;  // All parts are equal, so override result.
          }  // Comparer.
        return resultB;
        }

	  public int hashCode() 
		  {
        return // Returning sum of the two component hashes. 
        		Nulls.hashCode(netcasterInetAddress) + netcasterPortI;
        }

		public String toString() 
		  {
				return netcasterInetAddress.toString() + ":" + netcasterPortI;
			  }

    
		}
