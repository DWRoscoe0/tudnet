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

      private DataNode UserObject;  // User object associated with this node.
        // private Object UserObject;  // User object associated with this node.
      private HashMap< String, Object > AttributesHashMap;
        // public boolean AutoExpandedB= false;  // [moved to AttributesHashMap]
      public LinkedHashMap< Object, MetaNode  > ChildrenLinkedHashMap;
        /* In each child entry:
          The Key is a child user Object.
          The Value is the child MetaNode that contains the user Object and other data.
          */

    // Constructor.
    
      public MetaNode(Object ObjectUserIn)
        /* constructor.  */
        {
          //super( ObjectUserIn );  // call superclass constructor.  ???
          // super( null );  // call superclass constructor.  ???
          UserObject= (DataNode)ObjectUserIn;  // Save user object associated with this node.

          AttributesHashMap =  // For storage of attribute.
            new HashMap<String, Object>( 2 );  // Construct only a little Map at first.
          ChildrenLinkedHashMap =  // Initialize the child LinkedHashMap.
            new LinkedHashMap< Object, MetaNode  >( 
              2, // Initial size (small).
              0.75f,  // Load factor
              true  // access-order.
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

      public Object getUserObject()
        /* This returns the UserObject associated with this node.  */
        { // getUserObject()
          return UserObject;  // Return the user object associated with this node.
          } // getUserObject()

      public static void io( MetaNode InMetaNode )
        /* This ios the node InMetaNode and all its descendeenta 
          to the MetaFile.  */
        { // io()
          MetaFile.ioListBegin( );
          MetaFile.io( " MetaNode" );
          DataNode UserDataNode= (DataNode)InMetaNode.UserObject;
          MetaFile.ioIndentedField( 
            UserDataNode.GetNameString( )
            );
          ioAttributes( InMetaNode.AttributesHashMap );
          ioChildren( InMetaNode.ChildrenLinkedHashMap );
          MetaFile.ioListEnd( );
          } // io()

      private static void ioAttributes
        ( HashMap< String, Object > InAttributesHashMap )
        /* This ios the Attributes HashMap.  */
        { // ioAttributes()
          MetaFile.ioListBegin( );
          MetaFile.io( " Attributes" );
          Iterator < Map.Entry < String, Object > > MapIterator=  // Get an iterator...
            InAttributesHashMap.
            entrySet().
            iterator();  // ...for HashMap entries.
          while // Save all the HashMap's entries.
            ( MapIterator.hasNext() ) // There is a next Entry.
            { // Save this HashMap entry.
              MetaFile.ioIndentedField( "(" );
              Map.Entry < String, Object > AnEntry= // Get Entry 
                MapIterator.next();  // ...that is next Entry.
              MetaFile.io( " "+AnEntry.getKey( ) );
              MetaFile.io( " "+(String)AnEntry.getValue( ) );
              MetaFile.io( " )" );
              } // Save this HashMap entry.
          MetaFile.ioListEnd( );
          } // ioAttributes()

      private static void ioChildren
        ( LinkedHashMap< Object, MetaNode  > InChildrenLinkedHashMap )
        /* This ios the Children HashMap.  */
        { // ioChildren()
          MetaFile.ioListBegin( );
          MetaFile.io( " Children" );
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
                DataNode UserDataNode= (DataNode)AnEntry.getKey( );
                MetaFile.ioIndentedField( 
                  UserDataNode.GetNameString( )
                  );
                } // Save key.
              MetaNode.io( AnEntry.getValue( ) );  // Save value MetaNode.
              MetaFile.ioListEnd( );
              } // Save this HashMap entry.
          MetaFile.ioListEnd( );
          } // ioChildren()

      
    // Methods which deal with the children.

			MetaNode PutChildUserObjectMetaNode( Object UserObject )
        /* This method puts the Object UserObject in a child MetaNode
          within this its parent MetaNode.
          It creates and uses a new MetaNode if one with the UserObject
          does not already exist.
          In either case, it returns the MetaNode with the UserObject.
          */
        { // MetaNode PutChildUserObjectITreeNode( UserObject )
          MetaNode MapChildMetaNode=  // Try to get the MetaNode and move-to-front...
            ChildrenLinkedHashMap.get(  // ...in the child LinkedHashMap...
              UserObject );  // ... from the entry containing the UserObject.
          if ( MapChildMetaNode == null ) // Create new HashMap entry if not there.
            { // Create new HashMap entry.
              MapChildMetaNode= // Create new MetaNode with desired Object.
                new MetaNode( UserObject );
              ChildrenLinkedHashMap.put(   // Add new entry which maps...
                UserObject,  // ...key UserObject to...
                MapChildMetaNode  // ... the value MetaNode containing it.
                );
              } // Create new HashMap entry.
          return MapChildMetaNode;  // Return new/old child from map as result.
          } // MetaNode PutChildUserObjectITreeNode( UserObject )

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
              (DataNode)  // ...a DataNode caste of...
              LastChildMetaNode.   // ...the last child's...
              getUserObject();  // user object.
            } // override with child if there is one.
          return RecentChildDataNode; // return resulting DataNode, or null if none.
          } // GetLastReferencedChildDagNode( )

    } // class MetaNode.
