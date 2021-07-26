package allClasses.multilink;

import static allClasses.AppLog.theAppLog;

import java.util.ArrayList;
import java.util.List;


public interface ElementMultiLink<E extends ElementMultiLink<E>> 

  extends 
    MultiLink<E>

  {

  /* This is the MultiLink interface for a node that 
    acts as a MultiLink that contains only one element that this is this/us.
    It was created to be used with the common case of 
    a node that has only one child.

    DataNode implements this as ElementMultiLink<DataNode>.
    It doesn't actually have any of the interface methods,
    because this interface has default methods for all methods.
    These methods have not been [fully] tested and may contain errors.
    Testing will require implementing MultiLink with
    a MultiLink variable as class NamedList now does,
    but also replacing the variable's value with
    other MultiLink subclasses when circumstances merit. 

    This interface was originally created from auto-generated method stubs 
    for the interface MultiLink.
    
    */

  @Override
  default boolean isEmptyB() 
    {
      return false; // False because one element is more than zero.
      }

  @Override
  default int getCountI() 
    {
      return 1; // Single element means count is one.
      }

  @SuppressWarnings("unchecked") ///fix Find a way to type check?
  @Override
  default E getE(int indexI)
    {
      E resultE= // Result is this object,
          (E) this; // cast is needed to avoid type-conversion error. 
      if (0 != indexI) // If index does not select the only element
        resultE= null; // override result to indicate no element.
      return resultE;
      }

  @Override
  default int getIndexOfI(E theE) 
    {
      int resultI= 0; // Assume theE is this object.
      if (this != theE) // If it's not, override result
        resultI= -1; // with -1 to indicate that the element not present.
      return resultI;
      }

  @Override
  default void addV(int indexI, E theE) 
    {
      theAppLog.warning("ElementMultiLink.addV(): should never be called.");
      }

  @Override
  default E removeE(int indexI)
    {
      theAppLog.warning("ElementMultiLink.removeV(): should never be called.");
      return null;
      }

  @Override
  default E setE(int indexI, E theE)
    {
      theAppLog.warning("ElementMultiLink.setE(): should never be called.");
      return null;
      }

  default Iterable<E> getLinksIterable()
    { 
      return getListOfEs();
      }

  @SuppressWarnings("unchecked")
  @Override
  default List<E> getListOfEs()
    /* Returns a list of the links of this object.
     */
    {
      ArrayList<E> theArrayList= new ArrayList<E>();  // Create empty list. 
      theArrayList.add((E)this); // Add this as only element.
      return theArrayList;
      }

  }
