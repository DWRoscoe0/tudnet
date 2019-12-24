package allClasses;

import static allClasses.AppLog.theAppLog;

//// import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

public class RandomFileInputStream 

  extends RandomAccessInputStream
  //implements RandomAccessInputStream
  
  {
  
    private RandomAccessFile theRandomAccessFile= null;

    public RandomFileInputStream(RandomAccessFile theRandomAccessFile) 
      throws FileNotFoundException
      {
        //// super((String)null);
        this.theRandomAccessFile= theRandomAccessFile;
        }
    
    public int getPositionI()
      { 
        int positionI= 0;
        try { 
          positionI= //// kludge 
              (int)theRandomAccessFile.getFilePointer();
        } catch (IOException theIOException) {
          theAppLog.debug( "getPositionI() "+theIOException); ////

        }
        return positionI;  
        } 
  
    public void setPositionV(int thePositionI) throws IOException {
      theRandomAccessFile.seek(thePositionI);
    } 
    
    public int read() throws IOException
    /* This method returns one byte from the byte buffer, 
      or the end-of-stream value -1 if the buffer is exhausted.  
      So this method never blocks.

      Each UDP packet is considered to be one complete stream.
      This is because, since UDP is an unreliable protocol,
      each UDP packet should contain one or more complete pieces of data.
      No piece of data may span multiple packets.
      The end-of-stream condition can be cleared either
      * by loading a new packet containing at least one byte,
        which can be triggered by calling the method available(), or
      * by calling setPosition(int) to move the buffer pointer 
        back from the end of the buffer.

      */
    {
      int resultByteI= theRandomAccessFile.read();
      return resultByteI;
      }

  }
