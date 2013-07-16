package allClasses;

import java.util.Collection;
import java.util.Iterator;
//import java.util.HashMap;
//import java.util.Map;
import java.util.ArrayList;

//public class MetaChildren<K,V>
public class MetaChildren
  
  /* This class implements a Collection of child MetaNodes.
    Presently is uses an ArrayList to store them.
    Before that is used a HashMap and before that a LinkedHashMap,
    in which the MetaNode's DataNode was the key 
    and the MetaNode was the value.
    */

  { // class MetaChildren 

    private ArrayList< IDNumber > TheArrayList;  // Container for children.

		MetaChildren() 
      // Constructor.
      {
        TheArrayList=  // Construct the child MetaNode container as...
          new ArrayList< IDNumber >( ); // ...an ArrayList of IDNumbe-s.
        }

    public Collection<MetaNode> getCollectionOfMetaNode()
      /* This method returns a Collection containing the child MetaNodes.  */
      { 
        @SuppressWarnings("unchecked")
        Collection<MetaNode> ValuesCollectionOfMetaNode= 
          (Collection<MetaNode>)  // Kludgey double-caste needed...
          (Collection<?>)  // ...because of use of generic types.
          TheArrayList;
        
        return ValuesCollectionOfMetaNode;
        }

    public Iterator<MetaNode> iterator()  
      /* This method returns an iterator for the child MetaNodes.
        Presently these are the child HashMap values.
        */
      { 
        Collection<MetaNode> ValuesCollection=  // Calculate the Collection.
          getCollectionOfMetaNode();
        return ValuesCollection.iterator();  // Return an iterator built from it.
        }

    public Piterator<MetaNode> getPiteratorOfMetaNode()
      /* This method returns a Piterator for this MetaNode's 
        child MetaNodes.  Presently these are the child HashMap values.
        */
      { 
        Iterator<MetaNode> ValuesIteratorMetaNode=
          iterator();
        Piterator<MetaNode> ValuesPiteratorMetaNode=
              new Piterator<>( ValuesIteratorMetaNode );
        return ValuesPiteratorMetaNode;
        }

    public MetaNode get( Object KeyObject )
      /* This method returns the child MetaNode 
        which is associated with DataNode KeyObject,
        or null if there is no such MetaNode.
        */
      {
        MetaNode scanMetaNode;
        Piterator < MetaNode > ChildPiterator= getPiteratorOfMetaNode();
    	  while (true) {
    	  	scanMetaNode= ChildPiterator.getE();  // Cache present candidate. 
    	    if ( scanMetaNode == null )  // Exit if past end.
            break; 
    	    if  // Exit if found.
            ( KeyObject.equals(scanMetaNode.getDataNode()) )
            break; 
    	    ChildPiterator.next();  // Advance Piterator to next candidate.
      	  }
        return scanMetaNode;
        }
    
    public MetaNode add( MetaNode InMetaNode )
      /* This method adds InMetaNode to this MetaChildren instance.
        There should not already be a MetaNode with the same DataNode.
        Returns InMetaNode.
        */
      { 
        TheArrayList.add( InMetaNode );
        return InMetaNode;
        }

    public static MetaChildren rwMetaChildren
      ( MetaChildren InMetaChildren,
        DataNode InParentDataNode
        )
      /* This rw-processes the MetaChildren.
          If InMetaChildren != null then it writes the children
            to the MetaFile, and InParentDataNode is ignored.
          If InMetaChildren == null then it reads the children
            using InParentDataNode to look up DataNode names,
            and returns a new MetaChildren instance as the function value.
          */
      {
        MetaFile.rwListBegin( );
        MetaFile.rwLiteral( " MetaChildren" );

        if ( InMetaChildren == null )
          InMetaChildren= 
            readMetaChildren( InParentDataNode );
          else
          writeMetaChildren( InMetaChildren );

        MetaFile.rwListEnd( );
        return InMetaChildren;
        }

    private static MetaChildren readMetaChildren( DataNode InParentDataNode )
      /* This reads a MetaChildren from the file and returns it as the result.  
        It uses InParentDataNode for name lookups.  
        */
      {
        MetaChildren newMetaChildren =    // Initialize newMetaChildren to be...
          new MetaChildren( ); // ...an empty default instance.
        while ( true )  // Read all children.
          { // Read a child or exit.
            MetaFile.rwIndentedWhiteSpace( );  // Go to proper column.
            if  // Exit loop if end character present.
              ( MetaFile.testTerminatorI( ")" ) != 0 )
              break;  // Exit loop.
            MetaNode newMetaNode=  // Read the possibly nested MetaNode.
              MetaNode.rwMultiMetaNode( null, InParentDataNode );
            newMetaChildren.add(  // Store...
              newMetaNode // ...the new child MetaNode.
              );
            } // Read a child or exit.
        return newMetaChildren;  // Return resulting MetaChildren instance.
        }

    private static void writeMetaChildren
      ( MetaChildren InMetaChildren )
      /* This writes the MetaChildren instance InMetaChildren.  
        If FlatFileB == true then it writes ID numbers only,
        otherwise it recursively writes the complete MetaNodes.
        */
      {
        Iterator<MetaNode> childIterator=  // Create child iterator.
          InMetaChildren.iterator();
        while // Write all the children.
          ( childIterator.hasNext() ) // There is a next child.
          { // Write one child.
            MetaNode TheMetaNode= childIterator.next();  // Get the child MetaNode.
            switch // Write child based on RwStructure.
              ( MetaFile.TheRwStructure )
              {
                case FLAT:
                  MetaFile.rwIndentedWhiteSpace( );  // Go to proper column.
                  TheMetaNode.rwIDNumber();  // Write the ID #.
                  break;
                case HIERARCHICAL:
                  MetaNode.rwMultiMetaNode( TheMetaNode, null );  // Write MetaNode.
                  break;
                }
            } // Write one child.
        }

    } // class MetaChildren 
