package allClasses;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class MetaChildren<K,V>  
//public class MetaChildren

  extends LinkedHashMap<K,V>  
  
  /* This class implements a Collection of child MetaNodes.
    Presently is uses a LinkedHashMap for which:
          The Key is the child's user DataNode, the MetaNode's TheDataNode.
          The Value is the associated MetaNode that contains
          that DataNode and its meta-data.

    ??? Now in process of changing LinkedHashMap from a superclass to a field.
    */

  { // class MetaChildren 

    private static final long serialVersionUID = 1L;  // Added to remove warning.

		MetaChildren() 
      // Default constructor.
      {
        super(  // Construct superclass LinkedHashMap...
          2, // ...with a small initial size...
          0.75f,  // ...and this load factor...
          true  // ...and with access-order enabled.
          );
        }

    public Iterator<V> iterator()  
      /* This method returns an iterator for the MetaNode values,
        not the superclass LinkedHashMap entries.
        */
      { return super.values().iterator(); }

    public Collection<V> values()  // ???  Temp. method to find Map references.
      { return null; }

    public static MetaNode put
      ( 
        MetaChildren< Object, MetaNode > InMetaChildren,
        MetaNode InMetaNode 
        )
      /* This method puts InMetaNode into the InMetaChildren, 
        using its DataNode as the key, and InMetaNode as the value.
        */
      { 
        //return null;
        return 
         InMetaChildren.put( // Put in LinkedHashMap an entry with...
              InMetaNode.getDataNode(), // ...key == MetaNode's DataNode and...
              InMetaNode // ...value == the MetaNode itself.
              );
        }
    
    public static MetaChildren< Object, MetaNode  > rwMetaChildren
      ( MetaChildren< Object, MetaNode  > InMetaChildren,
        DataNode InParentDataNode
        )
      /* This rw-s the theMetaChildren.
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
        return InMetaChildren;  // Return new or original map.
        }

    private static MetaChildren< Object, MetaNode  > 
      readMetaChildren( DataNode InParentDataNode )
      /* This reads a MetaChildren and returns it as the result.  
        It uses InParentDataNode for name lookups.  */
      {
        MetaChildren< Object, MetaNode  >  // Initialize MetaChildren to be...
          newMetaChildren =
          new MetaChildren< Object, MetaNode >( ); // ...a default one.
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
        return newMetaChildren;
        }

    private static void writeMetaChildren
      ( MetaChildren< Object, MetaNode  > InMetaChildren )
      /* This writes the MetaChildren HashMap InMetaChildren.  
        If FlatFileB == true then it writes ID numbers only,
        otherwise it recursively writes the entire hash entry,
        with node name string and child MetaNode and their descendents.
        */
      {
        Iterator < Map.Entry < Object, MetaNode > >  // Get an iterator...
          MapIterator=
            InMetaChildren.entrySet().iterator();  // ...for HashMap entries.
        while // Save all the HashMap's entries.
          ( MapIterator.hasNext() ) // There is a next Entry.
          { // Write this HashMap entry.
            Map.Entry < Object, MetaNode > AnEntry= // Get Entry 
              MapIterator.next();  // ...that is next Entry.
            // The DataNode key is no longer saved because it is also in MetaNode.
            MetaNode TheMetaNode= AnEntry.getValue( );  // Get the value MetaNode.
            switch ( MetaFile.TheRwMode ) { // Write based on mode.
              case FLAT:
                MetaFile.rwIndentedWhiteSpace( );  // Go to proper column.
                TheMetaNode.rwIDNumber();  // Rw the ID #.
                break;
              case HIERARCHICAL:
                MetaNode.rwMultiMetaNode( TheMetaNode, null );  // Save MetaNode.
                break;
              } // Write based on mode.
            } // Write this HashMap entry.
        }

    } // class MetaChildren 
