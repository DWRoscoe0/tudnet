package allClasses.multilink;

import java.util.Iterator;


public interface ElementMultiLink<
    E // Element type. 
    > 

  extends 
    MultiLink<E>,
    Iterable<E>

  {

  /* Related originally auto-generated method stubs for interface MultiLink.
    Some of these are incorrect, but none are used presently. 
   */

  @Override
  default boolean isEmptyB() {
    return true;
    }

  @Override
  default int getCountI() 
    {
      return 0;
      }

  @Override
  default E getE(int indexI) 
    {
      return null;
      }

  @Override
  default int getIndexOfI(E theE) 
    {
      return -1;
      }

  @Override
  default void addV(int indexI, E theE) 
    {
      // Do nothing.  Could also thrown an exception or log an error.
      }

  @Override
  default void removeV(int indexI)
    {
      // Do nothing.  Could also thrown an exception or log an error.
      }

  @Override
  default Iterator<E> iterator()
    {
      return null; // Do nothing.  Could also throw an exception or log an error.
      }

    }
