package allClasses;

public class DataRw 

  /* This static class does MetaFile reading and writing of 
    DataNode information.  */

  { // class DataRw

      public static DataNode rwDataNode
        ( MetaFile inMetaFile, DataNode TheDataNode, MetaNode parentMetaNode  )
        /* This rw-processes the node InMetaNode
          using MetaFile inMetaFile.
          If TheDataNode != null then it writes the name of that DataNode
            to the MetaFile, and parentMetaNode is ignored.
          If TheDataNode == null then it reads a name 
            from the MetaFile, returns the DataNode with that name
            from the children of parentMetaNode.
          */
        { // rwDataNode( DataNode TheDataNode )
          //Misc.DbgOut( "DataRw.rwDataNode(..)" );

          inMetaFile.rwIndentedWhiteSpace( );  // Do line and indent.

          if ( TheDataNode == null )  // Reading...
            TheDataNode= readDataNode(   // Read DataNode name...
              inMetaFile,  // ...with inMetaFile...
              parentMetaNode.getDataNode()  // ...and lookup in this parent DataNode.
              );
            else  // Writing...
            inMetaFile.writeToken( TheDataNode.GetNameString( ) );  // Write name.

          return TheDataNode;
          } // rwDataNode( DataNode TheDataNode )

      /*
      public static DataNode rwDataNode
        ( MetaFile inMetaFile, DataNode TheDataNode, DataNode ParentDataNode  )
        /* This rw-processes the node InMetaNode
          using MetaFile inMetaFile.
          If TheDataNode != null then it writes the name of that DataNode
            to the MetaFile, and ParentDataNode is ignored.
          If TheDataNode == null then it reads a name 
            from the MetaFile, returns the DataNode with that name
            from the children of ParentDataNode.
          */
        /*
        { // rwDataNode( DataNode TheDataNode )
          Misc.DbgOut( "DataRw.rwDataNode(..)" );

          inMetaFile.rwIndentedWhiteSpace( );  // Do line and indent.

          if ( TheDataNode == null )  // Temporary reading...
            TheDataNode= readDataNode(   // Read DataNode name...
              inMetaFile,  // ...with inMetaFile...
              ParentDataNode  // ...and lookup in this parent DataNode.
              );
            else
            inMetaFile.writeToken( TheDataNode.GetNameString( ) );  // Write name.

          return TheDataNode;
          } // rwDataNode( DataNode TheDataNode )
        */

      private static DataNode readDataNode
        ( MetaFile inMetaFile, DataNode ParentDataNode  )
        { // readDataNode(..)
          String NameString=  // Get name of DataNode...
            inMetaFile.readTokenString( );  // ...by reading a String
          { // Prevent troublesome values for name.
            if ( NameString == null ) 
              NameString= "ERROR-NullString";
            else if ( NameString.equals( "" ) )
              NameString= "ERROR-EmptyString";
            else
              ; // Okay as is.
            } // Prevent troublesome values for name.
          DataNode TheDataNode =  // Set DataNode to be...
            ParentDataNode.getNamedChildDataNode(  // ...the child...
              NameString  // ...with that name.
              );
          if ( TheDataNode == null )  // replace with error object if null.  ???
            //TheDataNode= UnknownDataNode.newErrorDataNode( );
            TheDataNode= new UnknownDataNode( NameString );
          return TheDataNode;
          } // readDataNode(..)

    } // class DataRw 
