package allClasses;

public class LossAverager

  /* This class stores data about numbers of sent and received packets
    and calculates packet losses based on those numbers.
    Ideally the numbers of send and received packets should be identical,
    for a loss ratio of 0.  If the numbers are not equal then
    the loss ratio is non-0 and could be as high as 1.000.
    The loss ratio is a moving average over a period measured in sent packets.

    Usually 2 instances of this class are used per packet link:
    * One for outgoing packets.
    * One for incoming packets.

    */

  {
    private LongLike oldPacketsSentLongLike;
    private LongLike oldPacketsReceivedLongLike;
    
    private int sampleArraySizeI= // # of samples in array.
      40; // 40 gives a fraction resolution of 0.025.
    private byte sampleBytes[]; // Array of bytes used as a fixed-length queue.
      // 0 means packet received, 1 means packet lost.
    private int indexI = 0; // Index of next element of queue to be used.
    private int lossTotalI = 0; // Total of elements indicating lost packets.
    private NamedFloat lossFractionNamedFloat; // loss-total / # samples.

    public LossAverager(  // Constructor.    
        LongLike oldPacketsSentLongLike,
        LongLike oldPacketsReceivedLongLike,
        NamedFloat averagedLossRatioNamedFloat
        ) 
      /* The constructor arguments are displayed variables that are updated
       * as lost and received packets are recorded.
       */
      {
        this.oldPacketsSentLongLike= oldPacketsSentLongLike;
        this.oldPacketsReceivedLongLike= oldPacketsReceivedLongLike;
        this.lossFractionNamedFloat= averagedLossRatioNamedFloat;

        sampleBytes = new byte[sampleArraySizeI];
        for (int i = 0; i < sampleArraySizeI; i++) sampleBytes[i] = 0;
        }

    public void recordPacketsReceivedOrLostV(
        LongLike newPacketsSentLongLike,
        LongLike newPacketsReceivedLongLike
        )
      /* This method processes new packet receive and loss data.
        This includes updating the sample array and
        updating the displayed variables defined by the constructor.
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
        int oldTotalI= lossTotalI; // Saving present total of sample array.
        int endIndexI= indexI; // Saving index of end of queue.

        { // The following 2 methods must called in this sequence.
          processSentPacketsV( 
              newPacketsSentLongLike,newPacketsReceivedLongLike 
              );
          processExtraReceivedPacketsV( 
              endIndexI, newPacketsReceivedLongLike 
              );
          }

        if  // Updating displayed loss fraction if total of lost packets changed.
          ( lossTotalI != oldTotalI)
          lossFractionNamedFloat.setValueF(getLossFractionF());
        }

    private void processSentPacketsV( 
        LongLike newPacketsSentLongLike,
        LongLike newPacketsReceivedLongLike
        )
      /* This method processes all sent packets,
       * and up to the same number of received packets.
       * If there are more sent packets than received packets
       * then the difference is considered the number of lost packets.
       * If there were more received packets than sent packets,
       * these are considered late and will be processed
       * in a different method.
       * 
       * Note, if newPacketsSentLongLike has become 
       * less than its previous value then this method does nothing.  
       * Nothing is added to the sample queue.
       */
      {
        while (true) { // Processing in-order packets.
          if // Exiting if all sent packet counts processed.
            ( oldPacketsSentLongLike.getValueL() >= 
              newPacketsSentLongLike.getValueL() )
            break;
          if // Processing whether or not the packet was also received. 
            ( oldPacketsReceivedLongLike.getValueL() < 
              newPacketsReceivedLongLike.getValueL() 
              )
            { // Processing packet received.
              recordPacketReceivedOrLostV( false );
              oldPacketsReceivedLongLike.addDeltaL(1); 
                // Increment old received count.
              }
            else 
            { // Processing packet lost.
              recordPacketReceivedOrLostV( true );
              }
          oldPacketsSentLongLike.addDeltaL(1); // Incrementing old sent count.
          }
        }
    
    private void processExtraReceivedPacketsV( 
        int lateIndexI, LongLike newPacketsReceivedLongLike 
        )
      /* If there remain unprocessed received packets then it must be because 
       * packets which were previously recorded as lost have arrived late,
       * so those recorded losses must be undone.  They are undone here.
       * 
       *  ///fix Instead of counting packets marked as received,
       *  it should skip them, and only stop, change, and count
       *  the ones marked as lost.  This probably means adding an inner loop.
       */
      {
        while // Processing whether there are unprocessed received packets. 
          ( oldPacketsReceivedLongLike.getValueL() < 
            newPacketsReceivedLongLike.getValueL() 
            )
          {
            if  // Acting based on whether stored value is for lost packet.
              ( 1 == sampleBytes[lateIndexI] )
              { // Changing from indicated lost packet to received packet.
                lossTotalI -= 1; // Removing the loss indication.
                sampleBytes[lateIndexI] = 0;
                ///averagedLossRatioNamedFloat.setValueF(getAverageF());
                }
            if (--lateIndexI < 0 ) // Decrementing index and if needed 
              lateIndexI = sampleArraySizeI-1; // wrapping it around.
            oldPacketsReceivedLongLike.addDeltaL(1); // Incrementing.
            }
        }

    private void recordPacketReceivedOrLostV(boolean lostB)
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
            lossTotalI -= sampleBytes[indexI];
            lossTotalI += newValueByte;
            sampleBytes[indexI] = newValueByte;
            }
        if (++indexI >= sampleArraySizeI) // Increment indexI and if needed 
          indexI = 0; // wrap it around.
        }

    private float getLossFractionF() 
      /* Calculates and returns the present average.
         It does this by returning the sample total by the number of samples.
         */
      {
        return (float)lossTotalI / sampleArraySizeI;
        }

    } // LossAverager
