package allClasses;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class Children 
  { // class Children 

    public static LinkedHashMap< Object, MetaNode  > ioChildrenLinkedHashMap
      ( LinkedHashMap< Object, MetaNode  > InChildrenLinkedHashMap,
        DataNode InParentDataNode
        )
      /* This io-s the InChildrenLinkedHashMap.
          If InChildrenLinkedHashMap != null then it outputs the map
            to the MetaFile, and InParentDataNode is ignored.
          If InChildrenLinkedHashMap == null then it inputs the map
            using InParentDataNode to look up DataNode names,
            and returns the map as the function value.
          */
      { // ioChildrenLinkedHashMap()
        MetaFile.ioListBegin( );
        MetaFile.ioLiteral( " Children" );

        if ( InChildrenLinkedHashMap == null )
          InChildrenLinkedHashMap= 
            ioChildrenLinkedHashMapRead( InParentDataNode );
          else
            ioChildrenLinkedHashMapWrite( InChildrenLinkedHashMap );

        MetaFile.ioListEnd( );
        return InChildrenLinkedHashMap;  // Return new or original map.
        } // ioChildrenLinkedHashMap()

    public static LinkedHashMap< Object, MetaNode  > 
      ioChildrenLinkedHashMapRead( DataNode InParentDataNode )
      /* This reads a Children HashMap and returns it as the result.  
        It uses InParentDataNode for name lookups.  */
      { // ioChildrenLinkedHashMapRead()
        LinkedHashMap< Object, MetaNode  > 
          ChildrenLinkedHashMap =  // Initialize to be...
          new LinkedHashMap< Object, MetaNode  >(  // ...a LinkedHashMap with...
            2, // ...a small initial size...
            0.75f,  // ...and this load factor...
            true  // ...and with access-order.
            );  
        while ( true )  // Read all children.
          { // Read a child or exit.
            MetaFile.ioIndentedWhiteSpace( );  // Go to proper column.
            if  // Exit loop if end character present.
              ( MetaFile.testTerminatorI( ")" ) != 0 )
              break;  // Exit loop.
            MetaFile.ioListBegin( );
            MetaFile.ioIndentedWhiteSpace( );  // Indent correctly.
            /*
            String KeyString= MetaFile.readTokenString( );
            DataNode ChildDataNode=  // Set DataNode to be...
              InParentDataNode.getChild(  // ...the child...
                InParentDataNode.getIndexOfNamedChild( 
                  KeyString  // ...with Key string as name.
                  )
                );
            */
            DataNode ChildDataNode=  // Set ChildDataNode by...
              DataIo.readDataNode( // ...reading DataNode name and looking up in...
                InParentDataNode  // ...this parent DataNode.
                );
            //MetaFile.ioLiteral( " " );
            MetaNode ValueMetaNode= MetaNode.io( null, InParentDataNode );
            MetaFile.ioIndentedWhiteSpace( );  // Go to proper column.
            MetaFile.ioListEnd( );
            ChildrenLinkedHashMap.put( ChildDataNode, ValueMetaNode );
            } // Read a child or exit.
        return ChildrenLinkedHashMap;
        } // ioChildrenLinkedHashMapRead()

    public static void ioChildrenLinkedHashMapWrite
      ( LinkedHashMap< Object, MetaNode  > InChildrenLinkedHashMap )
      /* This io-s the Children HashMap.  */
      { // ioChildrenLinkedHashMapWrite()
        Iterator < Map.Entry < Object, MetaNode > > MapIterator=  // Get an iterator...
          InChildrenLinkedHashMap.
          entrySet().
          iterator();  // ...for HashMap entries.
        while // Save all the HashMap's entries.
          ( MapIterator.hasNext() ) // There is a next Entry.
          { // Save this HashMap entry.
            MetaFile.ioListBegin( );
            Map.Entry < Object, MetaNode > AnEntry= // Get Entry 
              MapIterator.next();  // ...that is next Entry.
            { // Save key.
              DataNode TheDataNode= (DataNode)AnEntry.getKey( );
              //MetaFile.ioIndentedLiteral( 
              //  TheDataNode.GetNameString( )
              //  );
              MetaFile.ioIndentedWhiteSpace( );
              MetaFile.outToken( TheDataNode.GetNameString( ) );
              } // Save key.
            MetaNode.io( AnEntry.getValue( ), null );  // Save value MetaNode.
            MetaFile.ioListEnd( );
            } // Save this HashMap entry.
        //MetaFile.ioListEnd( );  ???
        } // ioChildrenLinkedHashMapWrite()

    } // class Children 
