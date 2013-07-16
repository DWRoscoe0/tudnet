package allClasses;


import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
// import java.util.Map;

public class MetaNode

  extends IDNumber

  /* This class represents DataNode metadata, information about DagNodes.
    
    Previously metadata was stored in superclass DefaultMutableTreeNode.
    Information about previous selections was represented by
    the order of their child MetaNodes.
    
    Next the selection order information was represented by 
    the order of entries in the LinkedHashaaMap which contained the children.
    Also the remaining metadata was moved to attributes stored in
    instance variable AttributesHashMap.
    
    Next the selection order information was moved to attributes,
    and the order-preserving feature of LinkedHashMaps was no longer used.
    
    Next the children was moved to its own class, MetaChildren.
    */
    
  { // class MetaNode.
  
    // Variables.

      //public IDNumber TheIDNumber= null;  // ID #.  Moved to superclass.

      private DataNode TheDataNode= null;  /* Associated DataNode for which
        this MetaNode contains meta-data.  */
      private HashMap< String, Object > AttributesHashMap= null;  /* Attributes 
        of the associated DataNode, if any.
          The Key is a String name.
          The Value is a String value.
          */
      protected MetaChildren theMetaChildren= null;  /* Child MetaNodes 
        of this MetaNode, which are associated with child DataNodes of 
        the DataNode associated with this MetaNode.  */

    // Constructors (2).
    
      private MetaNode( )
        /* Constructor of blank MetaNodes.  
          These MetaNodes are filled in by the MetaNode loader.
          */
        {
          super( 0 );  // Set superclass ID # to 0 so it can be loaded later.
          }
    
      public MetaNode( DataNode InDataNode )
        /* This constructs a MetaNode associated with 
          an existing DataNode InDataNode.
          Initially it has no attributes or child MetaNodes.
          */
        {
          super( );  // Assign the superclass ID # to be something meaningful.

          TheDataNode=  // Save DataNode associated with this MetaNode.
            InDataNode; 

          AttributesHashMap =  // Initialize attributes to be...
            new HashMap< String, Object >( 2 );  // ...a small empty map.
          theMetaChildren =  // Initialize children to be...
            new MetaChildren( );  // ...an empty MetaChildren instance.
          }

    /* Pass-through methods which reference AttributesHashMap where 
      attributes are stored.  */

      public boolean containsKey( String KeyString ) 
        /* This is a pass-through to AttributesHashMap. */
        {
          return AttributesHashMap.containsKey( KeyString );
          }

      public Object get( String KeyString ) 
        /* This is a pass-through to AttributesHashMap. */
        {
          return AttributesHashMap.get( KeyString );
          }

      public Object remove( String KeyString ) 
        /* This is a pass-through to AttributesHashMap. */
        {
          return AttributesHashMap.remove( KeyString );
          }

      public Object put( String KeyString, Object ValueObject ) 
        /* This is a pass-through to AttributesHashMap. */
        {
          Object ResultObject= 
            AttributesHashMap.put( KeyString, ValueObject );
          return ResultObject;
          }

    // Methods for Read/Write from/to state files.

      public static MetaNode rwMultiMetaNode
        ( MetaNode InMetaNode, DataNode ParentDataNode )
        /* This is like rwMetaNode(..) except that in addition to InMetaNode, 
          it also rw-processes its descendents if in flat file mode.
          It processes either hierarchically as one chunk,
          or flat by splitting off the individual descendent nodes,
          depending on the value of MetaFile.TheRwStructure.
          It only works for RwStructure.FLAT when doing writing.  ????
          
          See rwMetaNode(..) for more information.
          */
        {
          InMetaNode=  // Process main MetaNode.
            rwMetaNode( InMetaNode, ParentDataNode );

          if  // Write the children separately if writing flat file.
            ( MetaFile.getWritingB() &&
              ( MetaFile.TheRwStructure == MetaFile.RwStructure.FLAT )
              )
            { // Write the children separately.
              Iterator < MetaNode > ChildIterator=  // Get an iterator...
                InMetaNode.theMetaChildren.
                iterator();  // ...for MetaChildren MetaNodes.
              while // Save all the HashMap's entries.
                ( ChildIterator.hasNext() ) // There is a next Entry.
                { // Write this HashMap entry.
                  MetaNode TheMetaNode=  // Get the MetaNode...
                    ChildIterator.next();  // ...that is next Entry.
                  MetaNode.rwMultiMetaNode(   // Write MetaNode.
                    TheMetaNode, null );
                  } // Write this HashMap entry.
              } // Write the children separately.

          return InMetaNode;  // Return the main MetaNode.
          }

      private static MetaNode rwMetaNode
        ( MetaNode InMetaNode, DataNode ParentDataNode )
        /* This rw-processes the node InMetaNode and its MetaNode children.  

          Whether it immediately and recursively rw-processes 
          all descendants in the MetaNode hierarchy or leaves them for later
          depends on the value of MetaFile.TheRwStructure.
          In the case of Reading, ParentDataNode is used for name lookup.
          That is handled by the MetaChildren class.
          
          It returns the MetaNode processed.

          Presently RwStructure.FLAT works only for writing.  ??? add reader code.
          */
        {
          if ( InMetaNode == null ) // If there is no MetaNode then...
            InMetaNode= new MetaNode( ); // ...create one to be filled.

          MetaFile.rwIndentedWhiteSpace( );  // Go to MetaFile.indentLevelI.
          MetaFile.rwListBegin( );  // RW the beginning of the list.
          MetaFile.rwIndentedWhiteSpace( );  // Go to MetaFile.indentLevelI.
          InMetaNode.rwIDNumber();  // Rw the ID #.
          MetaFile.rwIndentedWhiteSpace( );  // Go to MetaFile.indentLevelI.
          MetaFile.rwLiteral( "MetaNode" );  // Label as MetaNode list.
          InMetaNode.TheDataNode= DataRw.rwDataNode(  // Rw...
            InMetaNode.TheDataNode,  // ...TheDataNode using...
            ParentDataNode  // ...ParentDataNode for name lookups.
            );
          InMetaNode.AttributesHashMap=  // Rw the attributes.
            Attributes.rwAttributesHashMap( InMetaNode.AttributesHashMap );
          InMetaNode.theMetaChildren=  // Rw...
            MetaChildren.rwMetaChildren(  // ...the children hash map...
              InMetaNode.theMetaChildren, 
              InMetaNode.TheDataNode  // ...using this DataNode for lookups.
              );
          MetaFile.rwListEnd( );  // Mark the end of the list.

          return InMetaNode;  // Return the new or the original MetaNode.
          }

    // Simple getter methods.

      public DataNode getDataNode()
        /* This returns the DataNode associated with this MetaNode.  */
        {
          return TheDataNode;  // Return the user object associated with this node.
          }
    
      Collection<MetaNode> getChildrenCollectionOfMetaNode()
        /* This method returns the children of this MetaNode
          as a Collection of MetaNodes.
          */
        {
          return theMetaChildren.getCollectionOfMetaNode();
          }
    
    // Attribute tester and child searcher methods.

    /*
      boolean XhasAttributeB( String InKeyString, Object InValueObject ) //????
        /* This method tests whether this MetaNode contains an attribute entry
          with key InKeyString and value InValueObject.
          ?? This could be shortened by using only get().
          */
    /*
        { // hasAttributeB( .. )
          boolean ResultB = false;  // Set default test result.
          Tester: { // Test for attribute.
            if  // Does not have attribute with desired key String.
              ( ! containsKey( InKeyString ))
              break Tester;  // Exit Tester with default false result.
            if  // Attribute does not have the desired value Object.
              ( ! get( InKeyString ).equals( InValueObject ) )
              break Tester;  // Exit Tester with default false result.
            ResultB = true;  // Override default false result with true.
            } // Test for attribute.
          return ResultB;
          } // hasAttributeB( .. )
     */
      
      MetaNode getChildWithKeyMetaNode( String InKeyString )
        /* This method returns the first child MetaNode, if any, 
          with an attribute with key InKeyString.
          It returns null if no child MetaNode attribute has that key.
          */
        {
          KeyMetaPiteratorOfMetaNode ChildKeyMetaPiteratorOfMetaNode= 
            new KeyMetaPiteratorOfMetaNode( 
              theMetaChildren.getCollectionOfMetaNode(),
              InKeyString
              );
          return ChildKeyMetaPiteratorOfMetaNode.getE();
          }

      MetaNode getChildWithAttributeMetaNode
        ( String InKeyString, Object InValueObject )
        /* This method returns the first child MetaNode, if any, 
          with an attribute with key == InKeyString and value == InValueObject.
          If no child MetaNode has this attribute then null is returned.
          
          Maybe refactor this use AttributePiterator subclasses???
          */
        {
          KeyAndValueMetaPiteratorOfMetaNode ChildKeyAndValueMetaPiteratorOfMetaNode=
            new KeyAndValueMetaPiteratorOfMetaNode( 
              theMetaChildren.getCollectionOfMetaNode(),
              InKeyString,
              InValueObject
              );
          return ChildKeyAndValueMetaPiteratorOfMetaNode.getE();
          }

    // Miscellaneous methods.

      public boolean purgeB()
        /* This method is used to purge MetaNode-s which contain
          no useful information, meaning no attributes.
          If this MetaNode has no attributes then it
          recursively tries purging its child MetaNode-s.
          It returns false if it finds attributes in any MetaNode,
          meaning the node can't be purged.
          It returns true otherwise.
          */
        {
          boolean OkayToRemoveB= false;  // Assume we can't complete purge.
          Processor: {  // Purge testing and processing.
            if ( ! AttributesHashMap.isEmpty() )  //* There are attributes.
              break Processor;  // Exit with default no-purge indication.
            Iterator  // Get an iterator for children.
              < MetaNode > ChildIterator= 
              theMetaChildren.iterator();
            ChildScanner: while (true) { // Scan children for purging. 
              if ( ! ChildIterator.hasNext() )  //  There are no more children.
                break ChildScanner;  // Exit child scanner loop.
              MetaNode ChildMetaNode=  // Get a reference to...
                (MetaNode)  // ...the child MetaNode which is...
                ChildIterator.next();  // ...the next one.
              if ( ! ChildMetaNode.purgeB() )  // The child is not purgable.
                break Processor;  // Exit with default no-purge indication.
              ChildIterator.remove();  // Remove child from MetaChildren.
              } // Scan children for purging. 
            OkayToRemoveB= true;  // Indicate okay for complete purge.
            }  // Purge testing and processing.
          return OkayToRemoveB;  // Return calculated purge result.
          }

      MetaNode PutChildUserObjectMetaNode( Object InObject )
        /* This method puts the Object InObject in a child MetaNode
          within this its parent MetaNode.
          It creates a new child MetaNode if one with InObject
          does not already exist.
          In either case, it returns the child MetaNode with InObject.
          */
        {
          MetaNode MapChildMetaNode=  // Try to get the MetaNode...
            theMetaChildren.get(  // ...from the MetaChildren...
              InObject   // ... from the entry containing InObject.
              );
          if ( MapChildMetaNode == null ) // Create new HashMap entry if not there.
            { // Create new HashMap entry.
              MapChildMetaNode= // Create new MetaNode with desired Object.
                new MetaNode( (DataNode)InObject );
              theMetaChildren.add(   // Add...
                MapChildMetaNode  // ... the new child MetaNode.
                );
              } // Create new HashMap entry.
          return MapChildMetaNode;  // Return new/old child from map as result.
          }

    } // class MetaNode.
