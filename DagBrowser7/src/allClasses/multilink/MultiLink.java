package allClasses.multilink;

import java.util.Iterator;

public interface MultiLink<
    E // Element type. 
    > 

  extends Iterable<E>

  {
  
    /* This interface defines the methods used to access 
      reference links to a list of elements.
      At first this will be used to access DataNode child links, but eventually 
      it might be used to access multiple parent DAG DataNode links.
      It is not limited to elements that are DataNodes.

      The public methods of this class are similar in function to
      methods in DataTreeModel.

      This interface is fully implemented by ListMultiLink so that
      it can be used like an ArrayList with a reference to it in an element field.  
      It is now in full use this way and working.

      But this interface is also partially implemented for use in another way.
      DataNode implements the interface so that a DataNode can eventually act as
      a single-element MultiLink.  This makes it possible for storage compression.
      ///opt The following changes are tentatively planned:
      * Create class EmptyMultiLink and create a singleton instance of it.
      * Modify the MultiLink mutators to return a MultiLink which is
        either the original one, or a different one that is equivalent.
        Calculate that MultiLink return value as follows:
        * If the MultiLink has become empty, return the singleton EmptyMultiLink.
        * If the MultiLink has only 1 element, return the element,
          which implements the ElementMultiLink interface.
        * If the MultiLink has 2 or more elements, return a ListMultiLink,
          maybe the same one whose method was called.
        This value can be stored back in the element field from which it came.
        This can save a lot of storage if many MultiLinks have either 0 or 1 elements.
      */
  
    public boolean isEmptyB(); // Equivalent of isLeaf().
  
    public int getCountI(); // Equivalent of getChildCount().
  
    public E getE(int indexI); // Equivalent of DataNode getChild( int indexI );
  
    public int getIndexOfI(E theE); // int getIndexOfChild( Object inChildObject );
    
    public void addV(int indexI, E theLink);
    
    public void removeV(int indexI);
    
    public Iterator<E> iterator();
    
    }
