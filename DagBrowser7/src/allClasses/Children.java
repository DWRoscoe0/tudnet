package allClasses;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class Children 
  { // class Children 

    public static LinkedHashMap< Object, MetaNode  > rwChildrenLinkedHashMap
      ( LinkedHashMap< Object, MetaNode  > InChildrenLinkedHashMap,
        DataNode InParentDataNode
        )
      /* This rw-s the ChildrenLinkedHashMap.
          If InChildrenLinkedHashMap != null then it outputs the map
            to the MetaFile, and InParentDataNode is ignored.
          If InChildrenLinkedHashMap == null then it inputs the map
            using InParentDataNode to look up DataNode names,
            and returns the map as the function value.
          */
      { // rwChildrenLinkedHashMap()
        MetaFile.rwListBegin( );
        MetaFile.rwLiteral( " Children" );

        if ( InChildrenLinkedHashMap == null )
          InChildrenLinkedHashMap= 
            readChildrenLinkedHashMap( InParentDataNode );
          else
            writeChildrenLinkedHashMap( InChildrenLinkedHashMap );

        MetaFile.rwListEnd( );
        return InChildrenLinkedHashMap;  // Return new or original map.
        } // rwChildrenLinkedHashMap()

    private static LinkedHashMap< Object, MetaNode  > 
      readChildrenLinkedHashMap( DataNode InParentDataNode )
      /* This reads a Children HashMap and returns it as the result.  
        It uses InParentDataNode for name lookups.  */
      {
        LinkedHashMap< Object, MetaNode  > 
          ChildrenLinkedHashMap =  // Initialize to be...
          new LinkedHashMap< Object, MetaNode  >(  // ...a LinkedHashMap with...
            2, // ...a small initial size...
            0.75f,  // ...and this load factor...
            true  // ...and with access-order.
            );  
        while ( true )  // Read all children.
          { // Read a child or exit.
            MetaFile.rwIndentedWhiteSpace( );  // Go to proper column.
            if  // Exit loop if end character present.
              ( MetaFile.testTerminatorI( ")" ) != 0 )
              break;  // Exit loop.
            MetaNode ValueMetaNode= MetaNode.rwMultiMetaNode( null, InParentDataNode );
            ChildrenLinkedHashMap.put( // Create map entry from...
              ValueMetaNode.getDataNode(), // ...MetaNode's DataNode and...
              ValueMetaNode // ...The MetaNode itself.
              );
            } // Read a child or exit.
        return ChildrenLinkedHashMap;
        }

    private static void writeChildrenLinkedHashMap
      ( LinkedHashMap< Object, MetaNode  > InChildrenLinkedHashMap )
      /* This writes the Children HashMap InChildrenLinkedHashMap.  
        If FlatFileB == true then it writes ID numbers only,
        otherwise it recursively writes the entire hash entry,
        with node name string and child MetaNode and their descendents.
        */
      {
        Iterator < Map.Entry < Object, MetaNode > > MapIterator=  // Get an iterator...
          InChildrenLinkedHashMap.
          entrySet().
          iterator();  // ...for HashMap entries.
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
                //IDNumber.rwIDNumber(  // Rw...
                //  TheMetaNode.TheIDNumber  // ...TheIDNumber.
                //  );
                TheMetaNode.rwIDNumber();  // Rw the ID #.
                break;
              case HIERARCHICAL:
                MetaNode.rwMultiMetaNode( TheMetaNode, null );  // Save MetaNode.
                break;
              } // Write based on mode.
            } // Write this HashMap entry.
        }

    } // class Children 
