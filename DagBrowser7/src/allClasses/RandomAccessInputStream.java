package allClasses;

import java.io.IOException;
import java.io.InputStream;

public abstract class RandomAccessInputStream
  
  extends InputStream

  /* This abstract class is an InputStream that has random access capabilities.
   This is useful for parsers which do multiple byte backtracking. 

   Some subclass of this class must implement the abstract methods below and
   at least the inherited abstract method InputStream.read().
   
   */
  
  {

    /* Stream position methods follow.
     
      ///enh  The int use to represent InputStream position
        could eventually be replaced by some type of Object,
        if what underlies the stream is more complex than an array of bytes.
        list of sequence elements and an index to the next one.
       
     */
  
    public abstract int getPositionI(); 
      /* This method gets the present position within the InputStream so that 
        it might be restored later with the method setPositionV(int).
        This is more useful than the not nest-able InputStream.mark(int) method.
        */
  
    public abstract void setPositionV(int thePositionI) throws IOException; 
      /* This method restores the InputStream to thePositionI,
        which was probably a value returned by getPositionI().
        This method is more general than the not nest-able reset() method.
        */ 

    
    protected int bufferByteCountI()

      /* Returns the number of bytes remaining in the InputStream buffer, if any.  
       This should be used instead of the method available() when
       data is dealt with in blocks that are loaded into buffers for processing
       and block boundaries are significant. 
       Unlike the method available(),
       this method will not load the buffer with the next packet
       if the end of buffer is reached.
       This version returns 0.
       */
      
      { return 0; }
    
    }
