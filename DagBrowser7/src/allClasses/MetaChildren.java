package allClasses;

import java.util.Collection;
import java.util.Iterator;
import java.util.HashMap;
//import java.util.Map;

//public class MetaChildren<K,V>
public class MetaChildren

  // extends HashMap<K,V>  
  
  /* This class implements a Collection of child MetaNodes.
    Presently is uses a HashMap for which:
          The Key is the child's user DataNode, the MetaNode's TheDataNode.
          The Value is the associated MetaNode that contains
          that DataNode and its meta-data.

    The child was recently changed from a LinkedHashMap to a HashMap, 
    and more recently that HashMap was changed from a superclass to a field.
    */

  { // class MetaChildren 

    private HashMap< Object, MetaNode > TheHashMap;  // Container of the children.
    
		MetaChildren() 
      // Constructor.
      {
        TheHashMap=  // Construct the child MetaNode container in the form of a...
          new HashMap< Object, MetaNode >(  // ...HashMap...
            2, // ...with a small initial size...
            0.75f //,  // ...and this load factor...
            );
        }

    public Iterator<MetaNode> iterator()  
      /* This method returns an iterator for the child MetaNodes.
        Presently these are the child HashMap values.
        */
      { 
        Collection<MetaNode> ValuesCollection=  // Calculate the Collection.
          TheHashMap.values();
        return ValuesCollection.iterator();  // Return an iterator built from it.
        }

    public static MetaNode get
      ( 
        MetaChildren InMetaChildren,
        Object KeyObject
        )
      /* This method returns the MetaNode in InMetaChildren
        which is associated with DataNode KeyObject.
        */
      {
        return InMetaChildren.TheHashMap.get( KeyObject );
        }
    
    public static MetaNode put
      ( MetaChildren InMetaChildren,
        MetaNode InMetaNode 
        )
      /* This method adds InMetaNode to the InMetaChildren,
        unless it's already there.
        */
      { 
        return  // Return result of...
          InMetaChildren.TheHashMap.put( // ..putting into HashMap an entry with...
            InMetaNode.getDataNode(), // ...key == MetaNode's DataNode and...
            InMetaNode // ...value == the MetaNode itself.
            );
        }

    public static MetaChildren rwMetaChildren
      ( MetaChildren InMetaChildren,
        DataNode InParentDataNode
        )
      /* This rw-processes the MetaChildren.
          If InMetaChildren != null then it outputs the children
            to the MetaFile, and InParentDataNode is ignored.
          If InMetaChildren == null then it inputs the children
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
            MetaChildren.put(  // Store...
              newMetaChildren, // ...into the new MetaChildren...
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
            switch ( MetaFile.TheRwMode ) { // Write child based on RwMode.
              case FLAT:
                MetaFile.rwIndentedWhiteSpace( );  // Go to proper column.
                TheMetaNode.rwIDNumber();  // Write the ID #.
                break;
              case HIERARCHICAL:
                MetaNode.rwMultiMetaNode( TheMetaNode, null );  // Write MetaNode.
                break;
              } // Write based on mode.
            } // Write one child.
        }

    } // class MetaChildren 
