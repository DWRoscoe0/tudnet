package allClasses;

import java.io.IOException;

//import static allClasses.Globals.*;  // appLogger;

public class Netcaster 

	extends Streamcaster< 
			IPAndPort,
			NetcasterPacket,
			NetcasterQueue,
			NetcasterPacketManager,
			NetcasterInputStream,
			NetcasterOutputStream
			>

	/*

	  This class extends the UDP Streamcaster.
	  The main thing it adds is knowledge of the IPAndPort address,
	  which Streamcaster uses as a key and to differentiate 
	  this Netcaster from other Netcasters.
	  
	  This class is extended by:
	  * Unicaster for unicast UDP communications.
	  * Multicaster for multicast UDP communications.
    
    */

	{
	  public Netcaster(  // Constructor. 
	      LockAndSignal netcasterLockAndSignal,
	      NetcasterInputStream theNetcasterInputStream,
	      NetcasterOutputStream theNetcasterOutputStream,
        Shutdowner theShutdowner,
	      DataTreeModel theDataTreeModel,
	      IPAndPort  remoteIPAndPort, 
	      String nameString
	      )
	    {
	  		// Superclass's injections.
	  	  super( // Constructing Streamcaster DataNodeWithKey superclass.
			      theDataTreeModel,
			      nameString,
		        theShutdowner,
		        false,
			      remoteIPAndPort, // key K
			      netcasterLockAndSignal,
			      theNetcasterInputStream,
			      theNetcasterOutputStream
			      );
	      }

    protected void initializingV()
	    throws IOException
	    /* This initializing method includes stream packet counts.
	      */
	    {
		    initializingWithoutStreamsV();
		    
		    // Initializing stream monitors.
		    addB( 	theEpiOutputStreamO.getCounterNamedInteger() );
		    addB( 	theEpiInputStreamI.getCounterNamedInteger());
		    }

    protected void initializingWithoutStreamsV()
	    throws IOException
	    /* This special version does not include stream packet counts so
	      they can be place in a different position.
	      It exists so subclass Netcaster can reference it.
	      */
	    {
    		IPAndPort remoteIPAndPort= getKeyK();
    		addB( 	new NamedMutable( 
		        theDataTreeModel, 
		        "IP-Address", 
		        "" + remoteIPAndPort.getInetAddress()
		      	)
					);
		    addB( 	new NamedMutable( 
				    theDataTreeModel, "Port", "" + remoteIPAndPort.getPortI()
				  	) );

		    super.initializingV();
	    	}

    protected void writingNumberedPacketV( String aString ) 
    		throws IOException
      /* This method is like writingPacketV(..) but
        it prepends a packet ID / sequence number.
        ??? Maybe have a version that appends number?
        */
      {
    		writingSequenceNumberV();
    		writingPacketV(aString); // Writing string into buffer.
        }

    protected void writingSequenceNumberV() throws IOException
      /* This method increments and writes the packet ID (sequence) number
        to the EpiOutputStream.
        It doesn't flush().
        ??? Shouldn't this be a Unicaster method?
        */
      {
	  		writingTerminatedStringV( "N" );
	  		writingTerminatedLongV( 
	  				(theEpiOutputStreamO.getCounterNamedInteger().getValueL()) 
	  				);
        }

		}
