package allClasses;


import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
// import java.util.Map.Entry;

// import javax.swing.tree.DefaultMutableTreeNode;

public class MetaNode

  // extends DefaultMutableTreeNode
  
  /* This class represents DataNode metadata, information about DagNodes.
    
    Some of that data is now stored in a variable called AttributesHashMap.
    Before it was stored in the superclass DefaultMutableTreeNode.
    
    Originally the class DefaultMutableTreeNode stored all the metadata.
    This subclass was created for the convenience of a shorter name.
    New metadata was added to it later.
    */
    
  { // class MetaNode.
  
    // Variables.
    
      // private static final long serialVersionUID = 1L;

      private DataNode TheDataNode= null;
        // User object associated with this node.
        // was: private Object UserObject;  // User object associated with this node.
      private HashMap< String, Object > AttributesHashMap= null;
        // public boolean AutoExpandedB= false;  // [moved to AttributesHashMap]
      public LinkedHashMap< Object, MetaNode  > ChildrenLinkedHashMap= null;
        /* In each child entry:
          The Key is a child user Object.
          The Value is the child MetaNode that contains the user Object and other data.
          */

    // Constructors.
    
      public MetaNode( )
        /* Constructor of blank MetaNode..  */
        { // MetaNode( )
          } // MetaNode( )
    
      public MetaNode( DataNode InDataNode )
        /* Full constructor of a MetaNode with
          a single child InDataNode and no attributes.  */
        {
          TheDataNode=  // Save DataNode associated with this MetaNode.
            InDataNode; 

          AttributesHashMap =  // Initialize attributes to be...
            new HashMap<String, Object>( 2 );  // ...a small empty map.
          ChildrenLinkedHashMap =  // Initialize children to be an empty...
            new LinkedHashMap< Object, MetaNode  >( // ...LinkedHashMap...
              2, // ...with a small initial size...
              0.75f,  // ...and this load factor...
              true  // ...and with access-order enabled.
              );  
          }

    /* Pass-through methods which reference AttributesHashMap where 
      attributes are stored.  */

      public boolean containsKey( String KeyString ) 
        /* This is a pass-through to AttributesHashMap. */
        { // get(..)
          return AttributesHashMap.containsKey( KeyString );
          } // get(..)

      public Object get( String KeyString ) 
        /* This is a pass-through to AttributesHashMap. */
        { // get(..)
          return AttributesHashMap.get( KeyString );
          } // get(..)

      public Object remove( String KeyString ) 
        /* This is a pass-through to AttributesHashMap. */
        { // remove(..)
          return AttributesHashMap.remove( KeyString );
          } // remove(..)

      public Object put( String KeyString, Object ValueObject ) 
        /* This is a pass-through to AttributesHashMap. */
        { // put(..)
          return AttributesHashMap.put( KeyString, ValueObject );
          } // put(..)

    // Other methods.

      // public Object getUserObject()
      public DataNode getDataNode()
        /* This returns the DataNode associated with this MetaNode.  */
        { // getUserObject()
          return TheDataNode;  // Return the user object associated with this node.
          } // getUserObject()

      public static MetaNode io
        ( MetaNode InMetaNode, DataNode ParentDataNode )
        /* This io-processes the node InMetaNode and all its descendeenta 
          in the MetaFile.  
          ParentDataNode is used for name lookup in the case of loading.
          It returns the MetaNode processed.
          */
        { // io()
          MetaFile.ioIndentedWhiteSpace( );  // Indent correctly.  // not needed ???
          MetaFile.ioListBegin( );  // Mark the beginning of the list.
          MetaFile.ioLiteral( " MetaNode" );
          
          if ( InMetaNode == null ) // If there is no MetaNode then...
            InMetaNode= new MetaNode( ); // ...create an empty one to be filled.
          
          InMetaNode.TheDataNode= DataIo.ioDataNode(  // Io...
            InMetaNode.TheDataNode,  // ...TheDataNode using...
            ParentDataNode  // ...ParentDataNode for name lookups.
            );
          InMetaNode.AttributesHashMap=  // Io the attributes.
            Attributes.ioAttributesHashMap( InMetaNode.AttributesHashMap );
          InMetaNode.ChildrenLinkedHashMap=  // Io...
            Children.ioChildrenLinkedHashMap(  // ...the children hash map...
              InMetaNode.ChildrenLinkedHashMap, 
              InMetaNode.TheDataNode  // ...using this DataNode for lookups.
              );

          MetaFile.ioListEnd( );  // Mark the end of the list.
          return InMetaNode;  // Return the new or the original MetaNode.
          } // io()

      
    // Methods which deal with the children.

			MetaNode PutChildUserObjectMetaNode( Object InObject )
        /* This method puts the Object InObject in a child MetaNode
          within this its parent MetaNode.
          It creates a new child MetaNode if one with InObject
          does not already exist.
          In either case, it returns the child MetaNode with InObject.
          */
        { // MetaNode PutChildUserObjectITreeNode( InObject )
          MetaNode MapChildMetaNode=  // Try to get the MetaNode and move-to-front...
            ChildrenLinkedHashMap.get(  // ...in the child LinkedHashMap...
              InObject );  // ... from the entry containing InObject.
          if ( MapChildMetaNode == null ) // Create new HashMap entry if not there.
            { // Create new HashMap entry.
              MapChildMetaNode= // Create new MetaNode with desired Object.
                new MetaNode( (DataNode)InObject );
              ChildrenLinkedHashMap.put(   // Add new entry which maps...
                InObject,  // ...key InObject to...
                MapChildMetaNode  // ... the value MetaNode containing it.
                );
              } // Create new HashMap entry.
          return MapChildMetaNode;  // Return new/old child from map as result.
          } // MetaNode PutChildUserObjectITreeNode( InObject )

			public MetaNode GetLastChildMetaNode(  )
        /* This method gets the child MetaNode of this MetaNode 
          which was referenced last, or null if there are no children.  */
        { // GetLastChildITreeNode( )
          MetaNode LastChildMetaNode= null;  // Assume there is no last child.

          Iterator < Map.Entry < Object, MetaNode > > MapIterator=  // Get an iterator...
            ChildrenLinkedHashMap.entrySet().iterator();  // ...for HashMap entries.
          while // Use Iterator to get the HashMap's last Entry's Value.
            ( MapIterator.hasNext() ) // If there is a next Entry...
            LastChildMetaNode= // ...get a reference to...
              (MetaNode)  // ...the MetaNode which is...
              MapIterator.next().  // ...that next Entry's...
              getValue();  // ...Value.
        
          return LastChildMetaNode; // return last child MetaNode result, if any.
          } // GetLastChildITreeNode( )

			DataNode GetLastReferencedChildDataNode(  )
        /* This method gets the user object DataNode from
          the child MetaNode in this MetaNode 
          which was referenced last, or null if there are no children.  */
        { // GetLastReferencedChildDagNode( )
          DataNode RecentChildDataNode= null;// assume default value of null.
          switch (0) { // override with child if there is one.
            default:  // always start here.  switch allows break-outs.
            //if (this == null)  // no MetaNode was provided.
            //  break ;  // so keep the null result.
            MetaNode LastChildMetaNode= GetLastChildMetaNode( );
            if (LastChildMetaNode == null)  // there is no last child.
              break ;  // so keep the default null result.
            RecentChildDataNode=  // Result recent child DataNode is...
              //(DataNode)  // ...a DataNode caste of...
              LastChildMetaNode.   // ...the last child's...
              getDataNode();  // user object.
            } // override with child if there is one.
          return RecentChildDataNode; // return resulting DataNode, or null if none.
          } // GetLastReferencedChildDagNode( )

    } // class MetaNode.
