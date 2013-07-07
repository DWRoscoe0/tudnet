package allClasses;

public class DataRw 

  /* This static class does MetaFile reading and writing of 
    DataNode information.  */

  { // class DataRw

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
          /*
          DataNode TheDataNode =  // Set DataNode to be...
            ParentDataNode.getChild(  // ...the child...
              ParentDataNode.getIndexOfNamedChild( 
                NameString  // ...with that name.
                )
              );
          */
          DataNode TheDataNode =  // Set DataNode to be...
            ParentDataNode.getNamedChildDataNode(  // ...the child...
              NameString  // ...with that name.
              );
          if ( TheDataNode == null )  // replace with error object if null.  ???
            TheDataNode= ErrorDataNode.getSingletonErrorDataNode( );
          return TheDataNode;
          } // readDataNode(..)

      public static DataNode rwDataNode
        ( DataNode TheDataNode, DataNode ParentDataNode  )
        /* This rw-processes the node InMetaNode.
          If TheDataNode != null then it writes the name of that DataNode
            to the MetaFile, and ParentDataNode is ignored.
          If TheDataNode == null then it reads a name 
            from the MetaFile, returns the DataNode with that name
            from the children of ParentDataNode.
          */
        { // rwDataNode( DataNode TheDataNode )

          MetaFile.rwIndentedWhiteSpace( );  // Do line and indent.

          if ( TheDataNode == null )  // Temporary reading...
            TheDataNode= readDataNode(   // Read DataNode name and lookup in...
              ParentDataNode  // ...this parent DataNode.
              );
            else
            MetaFile.writeToken( TheDataNode.GetNameString( ) );  // Write name.
            // MetaFile.rwLiteral( TheDataNode.GetNameString( ) );  // Write name.

          return TheDataNode;
          } // rwDataNode( DataNode TheDataNode )

    } // class DataRw 
