package allClasses;

public class DataRw 

  /* This static class does MetaFile reading and writing of 
    DataNode information.  */

  { // class DataRw

      public static DataNode rwDataNode( 
          MetaFile inMetaFile, DataNode theDataNode, DataNode parentDataNode 
          )
        /* This rw-processes the node InMetaNode
          using MetaFile inMetaFile.
          If theDataNode != null then it writes the name of that DataNode
            to the MetaFile, and parentDataNode is ignored.
          If theDataNode == null then it reads a name 
            from the MetaFile, returns the DataNode with that name
            from the children of parentDataNode.
          */
        { // rwDataNode( DataNode theDataNode )
          //Misc.DbgOut( "DataRw.rwDataNode(..)" );

          inMetaFile.rwIndentedWhiteSpace( );  // Do line and indent.

          if ( theDataNode == null )  // Reading...
            theDataNode= readDataNode(   // Read DataNode name...
              inMetaFile,  // ...with inMetaFile...
              parentDataNode  // ...and lookup in this parent DataNode.
              );
            else  // Writing...
            inMetaFile.writeToken( theDataNode.GetNameString( ) );  // Write name.

          return theDataNode;
          } // rwDataNode( DataNode theDataNode )

      private static DataNode readDataNode( 
          MetaFile inMetaFile, DataNode parentDataNode
          )
        /* This method reads a name string from inMetaFile and
          returns a DataNode that best matches that name.
          If first tries to find a child DataNode of parentDataNode
          with the name.  If it finds none then it returns
          an UnknownDataNode instance with that name.
          It never returns a null.
          */
        { // readDataNode(..)
          String nameString=  // Get name of DataNode...
            inMetaFile.readTokenString( );  // ...by reading a String
          { // Prevent troublesome values for name.
            if ( nameString == null ) 
              nameString= "ERROR-NullString";
            else if ( nameString.equals( "" ) )
              nameString= "ERROR-EmptyString";
            else
              ; // Okay as is.
            } // Prevent troublesome values for name.
          DataNode theDataNode =  // Set DataNode to be...
            parentDataNode.getNamedChildDataNode(  // ...the child...
              nameString  // ...with that name.
              );
          if ( theDataNode == null )  // replace with error object if null.
            theDataNode= new UnknownDataNode( nameString );
          return theDataNode;
          } // readDataNode(..)

    } // class DataRw 
