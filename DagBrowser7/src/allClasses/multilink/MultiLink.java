package allClasses.multilink;

import java.util.Iterator;

public interface MultiLink<
    L // Link type.   //// Should this be ? extends MultiLink
    > 

  extends Iterable<L>

  {
  
    /* This interface defines the methods used to access a set of links.
      At first this will be used to access DataNode child links,
      but eventually it might be used multiple parent DAG DataNode links.
      
      This interface can be implemented several ways:
      * By an L object as a list of that L object and nothing else.
        This requires no MultiLink field in the object.
      * By a non-L object which is referenced by L object 
        childMultiLink field.  The possibilities are:
        * A class that contains a list of L objects.
        * A class that represents an empty list of L objects.
      ///ehn If mutator methods return MultiLink then in theory
      each time a mutator is called, the childMultiLink referenced
      could be replaced with with the most space-efficient representation. 

      The public methods of this class are similar in function to
      methods in DataTreeModel.
      */
  
    public boolean hasNoLinks(); // Equivalent of isLeaf().
  
    public int getLinkCountI(); // Equivalent of getChildCount().
  
    public L getLinkL(int indexI); // Equivalent of DataNode getChild( int indexI );
  
    public int getIndexOfLinkI(L theL); // int getIndexOfChild( Object inChildObject );
    
    public void addV(int indexI, L theLink);
    
    public void removeV(int indexI);
    
    public Iterator<L> iterator();
    
    }
