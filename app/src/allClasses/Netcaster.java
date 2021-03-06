package allClasses;


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
        IPAndPort  remoteIPAndPort, 
        String nameString,
        NamedLong initialRetryTimeOutMsNamedLong,
        DefaultBooleanLike leadingDefaultBooleanLike
        )
      {
        // Superclass's injections.
        super( // Constructing Streamcaster KeyedStateList superclass.
            nameString,
            theShutdowner,
            leadingDefaultBooleanLike,
            remoteIPAndPort, // key K
            netcasterLockAndSignal,
            theNetcasterInputStream,
            theNetcasterOutputStream,
            initialRetryTimeOutMsNamedLong
            );
        }

    public void initializeV()  // standard.
      /* This initializing method includes stream packet counts.
        This is called by Multicaster.
        */
      {
        this.initializeWithoutStreamsV();
        
        // Also do initializing of stream monitors.
        addAtEndV(   theEpiOutputStreamO.getCounterNamedLong() );
        addAtEndV(   theEpiInputStreamI.getCounterNamedLong());
        }

    protected void initializeWithoutStreamsV() // non-standard.
      /* This special version does not include stream packet counts so
        they can be place in a different position.
        It exists so subclass Netcaster can reference it.
        This is called by Unicaster.
        */
      {
        super.initializeV();
        IPAndPort remoteIPAndPort= getKeyK();
        addAtEndV(   new NamedMutable( 
            "IP-Address", 
            "" + remoteIPAndPort.getInetAddress()
            )
          );
        addAtEndV(   new NamedMutable( 
            "Port", "" + remoteIPAndPort.getPortI()
            ) );
        }

    /*///
    protected void writingNumberedPacketV( String aString ) 
        throws IOException
      /* This method is like writingAndSendingV(..) but
        it prepends a packet ID / sequence number.
        ??? Maybe have a version that appends number?
        */
    /*///
      {
        writingSequenceNumberV();
        writingAndSendingV(aString); // Writing string into buffer.
        }
    */ ///

    }
