package allClasses;

import java.net.InetSocketAddress;


public class NetCasterValue  // Value of HashMap entry.

 /* This class was created when it became necessary to
   have access to both the Unicaster class and its Thread.
   */

  {

    private Unicaster theUnicaster;
    private EpiThread theEpiThread;

    public NetCasterValue(  // Constructor. 
        InetSocketAddress peerInetSocketAddress,
        Unicaster theUnicaster
        )
      {
        this.theUnicaster= theUnicaster;
        this.theEpiThread= new EpiThread( 
          theUnicaster,
          "Unicaster-"+peerInetSocketAddress
          );
        }
        
    public Unicaster getUnicaster()
      { 
        return theUnicaster; 
        }
        
    public EpiThread getEpiThread()
      { 
        return theEpiThread; 
        }
    }
