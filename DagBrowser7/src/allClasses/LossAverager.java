package allClasses;

public class LossAverager

	/* This class calculates packet losses based on
	  changing counts of sent and received packets.
    
    ?? Maybe have multiple methods, different ones for "N" and "NFB" processing,
    or have different subclasses.

    */

	{
		private LongLike oldPacketsSentLongLike;
		private LongLike oldPacketsReceivedLongLike;
  	
    private int samplesArraySizeI= 40; // # of samples in array.
    private byte sampleBytes[]; // Array of bytes used as a fixed-length queue.
      // 0 means packet received, 1 means packet lost.
    private int indexI = 0; // Index of next element of queue to be used.
    private int totalI = 0; // Total of all array elements.
    private NamedFloat averagedLossRatioNamedFloat; // Total / # samples.

    LossAverager(  // Constructor.		
    		LongLike oldPacketsSentLongLike,
    		LongLike oldPacketsReceivedLongLike,
    		NamedFloat averagedLossRatioNamedFloat
    		) 
      {
	  		this.oldPacketsSentLongLike= oldPacketsSentLongLike;
	  		this.oldPacketsReceivedLongLike= oldPacketsReceivedLongLike;
      	this.averagedLossRatioNamedFloat= averagedLossRatioNamedFloat;

        sampleBytes = new byte[samplesArraySizeI];
        for (int i = 0; i < samplesArraySizeI; i++) sampleBytes[i] = 0;
        }

		public void recordPacketsReceivedOrLostV(
  			LongLike newPacketsSentLongLike,
  			LongLike newPacketsReceivedLongLike
				)
	    /* This method records packet lost and received by
	      discarding old array samples at the beginning, 
	      adding an equal amount of new ones at the end,
	      and sometimes changes the values of some recent samples, 
	      all according to its arguments:
			  * The value of newPacketsSentLongLike comes from a packet from 
			    the remote peer and represents the number of packets sent.
			    It usually increases, but might decrease 
			    if those packets arrive out-of-order.
			  * The value of newPacketsReceivedLongLike is a receive packet counter 
			    and it always increases.

		    First it records on-time packets, which are received packets
		    associated with new sent packets, by putting them in new queue entries.

		    Next, if any unrecorded received packets remain, 
		    they are recorded as late packets in slots before 
		    the end of the sample queue. 
		    */
			{
				int oldTotalI= totalI; // Saving present total of sample array.
				int endIndexI= indexI; // Saving index of end of queue.

			  recordOnTimePacketsV( 
			  		newPacketsSentLongLike,newPacketsReceivedLongLike 
			  		);
			  recordLatePacketsV( 
			  		endIndexI, newPacketsReceivedLongLike 
			  		);
			  // The above 2 calls are done in this order to prevent moving losses.

			  if ( totalI != oldTotalI) // Updating ratio if total changed.
        	averagedLossRatioNamedFloat.setValueF(getAverageF());
			  }

		private void recordOnTimePacketsV( 
  			LongLike newPacketsSentLongLike,
  			LongLike newPacketsReceivedLongLike
  			)
		  /* Note, if newPacketsSentLongLike has become less than its previous value
				then this method does nothing.  Nothing is added to the sample queue.
				*/
			{
		    while (true) { // Processing in-order packets.
		    	if // Exiting if all sent packet counts processed.
			    	( oldPacketsSentLongLike.getValueL() >= 
			    	  newPacketsSentLongLike.getValueL() )
		    		break;
					if // Processing whether or not a packet was also received. 
					  ( oldPacketsReceivedLongLike.getValueL() < 
							newPacketsReceivedLongLike.getValueL() 
							)
						{ // Processing packet received.
							recordPacketReceivedOrLostV( false );
			      	oldPacketsReceivedLongLike.addDeltaL(1); // Incrementing.
			      	}
			      else 
			      { // Processing packet lost.
							recordPacketReceivedOrLostV( true );
			      	}
					oldPacketsSentLongLike.addDeltaL(1); // Incrementing sent count.
		      }
				}
		
    private void recordLatePacketsV( 
    		int lateIndexI, LongLike newPacketsReceivedLongLike 
    		)
      {
	  		while // Processing whether or not there's an unprocessed received packet. 
				  ( oldPacketsReceivedLongLike.getValueL() < 
						newPacketsReceivedLongLike.getValueL() 
						)
					{
			  		if  // Acting based on whether stored value is for lost packet.
			  			( 1 == sampleBytes[lateIndexI] )
			  			{ // Changing from indicated loss packet to received packet.
				        totalI -= 1; // Removing the loss indication.
				        sampleBytes[lateIndexI] = 0;
				  			///averagedLossRatioNamedFloat.setValueF(getAverageF());
			    		  }
			      if (--lateIndexI < 0 ) // Decrementing index and if needed 
			      	lateIndexI = samplesArraySizeI-1; // wrapping it around.
		      	oldPacketsReceivedLongLike.addDeltaL(1); // Incrementing.
						}
	      }

	  public void recordPacketReceivedOrLostV(boolean lostB)
      /* This method discards one old sample indicated by lostB
        at the beginning of the queue and replaces it with a new one at the end.
        If the values were different then 
        it calculates and stores a new total.  
        During most runs they are not different and 
        it only increments the sample indexI.
        */
      {
    		byte newValueByte=  // Convert boolean to byte.
    				(byte)(lostB ? 1 : 0);
    		if  // Act based on whether new value different from stored value.
    			( newValueByte == sampleBytes[indexI] )
    			; // Same, so nothing needs changing.
    			else
	    		{ // Account for new value different from stored old value.
		        totalI -= sampleBytes[indexI];
		        totalI += newValueByte;
		        sampleBytes[indexI] = newValueByte;
	    		  }
        if (++indexI >= samplesArraySizeI) // Increment indexI and if needed 
        	indexI = 0; // wrap it around.
      	}

    public float getAverageF() 
      /* Calculates and returns the present average.
         It does this by returning the sample total by the number of samples.
         */
      {
        return (float)totalI / samplesArraySizeI;
    		}

		} // LossAverager
