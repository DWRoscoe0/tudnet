package allClasses;

public class DataIo 

  /* This static class does MetaFile Io of DataNode information.  */

  { // class DataIo

      public static DataNode readDataNode( DataNode ParentDataNode  )
        { // readDataNode(..)
          String NameString=  // Get name of DataNode...
            MetaFile.readTokenString( );  // ...by reading a String
          { // Prevent troublesome values for name.
            if ( NameString == null ) 
              NameString= "ERROR-NullString";
            else if ( NameString.equals( "" ) )
              NameString= "ERROR-EmptyString";
            else
              ; // Okay as is.
            } // Prevent troublesome values for name.
          DataNode IoDataNode =  // Set DataNode to be...
            ParentDataNode.getChild(  // ...the child...
              ParentDataNode.getIndexOfNamedChild( 
                NameString  // ...with that name.
                )
              );
          if ( IoDataNode == null )  // replace with error object if null.  ???
            IoDataNode= ErrorDataNode.getSingletonErrorDataNode( );
          return IoDataNode;
          } // readDataNode(..)

      public static DataNode ioDataNode
        ( DataNode IoDataNode, DataNode ParentDataNode  )
        /* This io-processes the node InMetaNode.
          If IoDataNode != null then it outputs the name of that DataNode
            to the MetaFile, and ParentDataNode is ignored.
          If IoDataNode == null then it inputs a name 
            from the MetaFile, returns the DataNode with that name
            from the children of ParentDataNode.
          */
        { // ioDataNode( DataNode IoDataNode )

          MetaFile.ioIndentedWhiteSpace( );  // Do line and indent.

          if ( IoDataNode == null )  // Temporary reading...
            IoDataNode= readDataNode(   // Read DataNode name and lookup in...
              ParentDataNode  // ...this parent DataNode.
              );
            else
            MetaFile.outToken( IoDataNode.GetNameString( ) );  // Write name.
            // MetaFile.ioLiteral( IoDataNode.GetNameString( ) );  // Write name.

          return IoDataNode;
          } // ioDataNode( DataNode IoDataNode )

    } // class DataIo 
