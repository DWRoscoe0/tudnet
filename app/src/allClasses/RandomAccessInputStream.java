package allClasses;

import java.io.IOException;
import java.io.InputStream;

public abstract class RandomAccessInputStream
  
  extends InputStream

  /* This abstract class is an InputStream that has random access capabilities.
   This is useful for parsers which do multiple byte backtracking. 

   Some subclass of this class must implement the abstract methods below and
   at least the inherited abstract method InputStream.read().

   ///new It would probably be worthwhile to add a RandomAccessReader,
   a Character Stream class with similar RandomAccess capabilities.
   Then the EpiNode parsers could be rewritten to be able to 
   deal with characters instead of bytes.
   */

  {

    /* Stream position methods follow.

      ///enh  The int used to represent InputStream position
        could eventually be replaced by some type of Object,
        if what underlies the stream is more complex than an array of bytes.
       
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

    public byte readB() throws IOException
      /* This is a convenience method that returns a byte instead of an int.
       * This means that the value -1 for no byte available
       * will be converted to 0xff.
       * It is assumed that end-of-stream will be detected elsewhere. 
       */
      {
        return (byte)read();
        }

    }
