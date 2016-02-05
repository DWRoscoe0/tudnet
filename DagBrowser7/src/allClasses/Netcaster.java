package allClasses;

import java.io.IOException;

//import static allClasses.Globals.*;  // appLogger;

public class Netcaster 

	extends Streamcaster< IPAndPort >

	// This class is the superclass of Unicaster and Multicaster.

	{
	  public Netcaster(  // Constructor. 
	      LockAndSignal netcasterLockAndSignal,
	      NetInputStream theNetInputStream,
	      NetcasterOutputStream theNetcasterOutputStream,
	      DataTreeModel theDataTreeModel,
	      IPAndPort  remoteIPAndPort, 
	      String nameString
	      )
	    {
	  		// Superclass's injections.
	  	  super( // Constructing Streamcaster DataNodeWithKey superclass.
			      theDataTreeModel,
			      nameString,
			      remoteIPAndPort, // key K
			      netcasterLockAndSignal,
			      theNetInputStream,
			      theNetcasterOutputStream
			      );

        packetIDI= 0; // Setting starting packet sequence number.
	      }

    protected void initializingV()
	    throws IOException
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

		    addB( 	theNetcasterOutputStream.getCounterNamedInteger() );
		    addB( 	theNetInputStream.getCounterNamedInteger());
	    	}

    protected boolean testingMessageB( String aString ) throws IOException
      /* This method tests whether the next message String in 
        the next received packet in the queue, if there is one,  is aString.
        It returns true if so, false otherwise.
        The message is not consumed, so can be read later.
        */
      { 
        boolean resultB= false;  // Assuming aString is not present.
        decodingPacket: {
          String packetString= // Getting string from packet if possible. 
          	peekingMessageString( );
          if ( packetString == null ) // Exiting if no packet or no string.
            break decodingPacket;  // Exiting with false.
          if   // Exiting if the desired String is not equal to packet String.
          	( ! packetString.equals( aString ) )
            break decodingPacket;  // Exiting with false.
          resultB= true;  // Setting result true because Strings are equal.
          } // decodingPacket:
        return resultB;  // Returning the result.
        }

    private String peekingMessageString( ) throws IOException
      /* This method returns the next message String in 
        the next received packet in the queue, if there is one.  
        If there's no message then it returns null.
        The message is not consumed, so can be read later.
        */
      { 
    		String inString= null;
	  		if ( theNetInputStream.available() > 0) // Reading string if available.
		  		{
			  		theNetInputStream.mark(0); // Marking stream position.
			  	  inString= readAString();
		  	  	theNetInputStream.reset(); // Resetting so String is not consumed.
			  		}
	  	  return inString;
	  		}

		}
