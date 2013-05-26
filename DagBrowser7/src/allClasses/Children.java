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
      { // readChildrenLinkedHashMap()
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
            //MetaFile.rwListBegin( );
            //MetaFile.rwIndentedWhiteSpace( );  // Indent correctly.
            //DataNode ChildDataNode=  // Set ChildDataNode by...
            //  DataRw.readDataNode( // ...reading DataNode name and looking up in...
            //    InParentDataNode  // ...this parent DataNode.
            //    );
            MetaNode ValueMetaNode= MetaNode.rwMetaNode( null, InParentDataNode );
            //MetaFile.rwIndentedWhiteSpace( );  // Go to proper column.
            //MetaFile.rwListEnd( );
            //ChildrenLinkedHashMap.put( ChildDataNode, ValueMetaNode );
            ChildrenLinkedHashMap.put( // Create map entry from...
              ValueMetaNode.getDataNode(), // ...MetaNode's DataNode and...
              ValueMetaNode // ...The MetaNode itself.
              );
            } // Read a child or exit.
        return ChildrenLinkedHashMap;
        } // readChildrenLinkedHashMap()

    private static void writeChildrenLinkedHashMap
      ( LinkedHashMap< Object, MetaNode  > InChildrenLinkedHashMap )
      /* This writes the Children HashMap InChildrenLinkedHashMap.  
        If FlatFileB == true then it writes ID numbers only,
        otherwise it recursively writes the entire hash entry,
        with node name string and child MetaNode and their descendents.
        */
      { // writeChildrenLinkedHashMap()
        Iterator < Map.Entry < Object, MetaNode > > MapIterator=  // Get an iterator...
          InChildrenLinkedHashMap.
          entrySet().
          iterator();  // ...for HashMap entries.
        while // Save all the HashMap's entries.
          ( MapIterator.hasNext() ) // There is a next Entry.
          { // Save this HashMap entry.
            //MetaFile.rwListBegin( );
            Map.Entry < Object, MetaNode > AnEntry= // Get Entry 
              MapIterator.next();  // ...that is next Entry.
            { // Save key.
              //DataNode TheDataNode= (DataNode)AnEntry.getKey( );
              //MetaFile.rwIndentedWhiteSpace( );
              ////MetaFile.writeToken( TheDataNode.GetNameString( ) );
              //MetaFile.writeToken( "JUNK-FILLER" );
              } // Save key.
            MetaNode.rwMetaNode( AnEntry.getValue( ), null );  // Save value MetaNode.
            //MetaFile.rwListEnd( );
            } // Save this HashMap entry.
        } // writeChildrenLinkedHashMap()

    } // class Children 
