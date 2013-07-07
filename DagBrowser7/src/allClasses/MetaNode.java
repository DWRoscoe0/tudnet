package allClasses;


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
      private DataNode TheDataNode= null;  // Associated DataNode.
      private HashMap< String, Object > AttributesHashMap= null;
        /* Attributes of the DataNode, if any.
          The Key is a String name.
          The Value is a String value.
          */
      protected MetaChildren theMetaChildren= null;
        /* MetaNodes of DataNode children which themselves have meta-data.  */

    // Constructors (2).
    
      private MetaNode( )
        /* Constructor of blank MetaNodes.  */
        {
          super( 0 );  // Set superclass ID # to 0 so it can be loaded later.
          }
    
      public MetaNode( DataNode InDataNode )
        /* This constructs a MetaNode associated with 
          the single DataNode InDataNode, 
          but with no attributes or child MetaNodes, yet.  
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

    // Read/Write methods.

      public static MetaNode rwMultiMetaNode
        ( MetaNode InMetaNode, DataNode ParentDataNode )
        /* This is like rwMetaNode(..) except that in addition to InMetaNode, 
          it also rw-processes its descendents if in flat file mode.
          It processes either hierarchically as one chunk,
          or flat by splitting off the individual descendent nodes,
          depending on the value of MetaFile.TheRwMode.
          See rwMetaNode(..) for more information.
          */
        {
          InMetaNode=  // Process main MetaNode.
            rwMetaNode( InMetaNode, ParentDataNode );

          if  // Process the children again if we're splitting them off.
            ( MetaFile.TheRwMode == MetaFile.RwMode.FLAT )
            { // Process the children separately.
              Iterator < MetaNode > ChildIterator=  // Get an iterator...
                InMetaNode.theMetaChildren.
                iterator();  // ...for MetaChildren MetaNodes.
              while // Save all the HashMap's entries.
                ( ChildIterator.hasNext() ) // There is a next Entry.
                { // Write this HashMap entry.
                  MetaNode TheMetaNode=  // Get the MetaNode...
                    ChildIterator.next();  // ...that is next Entry.
                  MetaNode.rwMultiMetaNode( TheMetaNode, null );  // Write MetaNode.
                  } // Write this HashMap entry.
              } // Process the children separately.

          return InMetaNode;  // Return the main MetaNode.
          }

      private static MetaNode rwMetaNode
        ( MetaNode InMetaNode, DataNode ParentDataNode )
        /* This completely rw-processes the node InMetaNode and 
          all its descendants in the MetaFile hierarchically.  
          It does the child MetaNodes recursively.
          ParentDataNode is used for name lookup in the case of Reading.
          It returns the MetaNode processed.
          */
        {
          if ( InMetaNode == null ) // If there is no MetaNode then...
            InMetaNode= new MetaNode( ); // ...create an empty one to be filled.

          MetaFile.rwIndentedWhiteSpace( );  // Indent correctly.
          MetaFile.rwListBegin( );  // Mark the beginning of the list.
            
          MetaFile.rwIndentedWhiteSpace( );  // Indent correctly.
          InMetaNode.rwIDNumber();  // Rw the ID #.
            
          MetaFile.rwIndentedWhiteSpace( );  // Indent correctly.
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

    // Attribute tester and child searcher methods.
    // These methods could benefit from refactoring.

      boolean hasAttributeB( String InKeyString, Object InValueObject )
        /* This method tests whether this MetaNode contains an attribute entry
          with key InKeyString and value InValueObject.
          */
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

      
    // Methods which deal more with the children of this MetaNode.

      MetaNode getChildWithKeyMetaNode( String InKeyString )
        /* This method returns the first child MetaNode, if any, 
          with an attribute with key InKeyString.
          It returns null if no child MetaNode attribute has that key.
          
          Maybe refactor this use AttributePiterator subclasses???
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

      Piterator< MetaNode > getChildWithAttributePiteratorOfMetaNode
        ( String InKeyString, Object InValueObject )
        /* This method returns a PiteratorOfMetaNode into the MetaChildren,
          pointing to the first child MetaNode, if any, 
          with an attribute with key==InKeyString and value==InValueObject.
          If no child MetaNode has this attribute combination then 
          the returned PiteratorOfMetaNode will point to null. 
          
          Maybe refactor this to put more logic into 
          AttributePiterator subclasses which understand attributes ???
          */
        {
          Piterator<MetaNode>  // Get initializzed PiteratorOfMetaNode.
            ChildPiteratorOfMetaNode= getChildPiteratorOfMetaNode(  );
          Scanner: while (true) { // Scan children for desired attribute. 
            if   //  There are no more children.
              ( ChildPiteratorOfMetaNode.getE() == null )
              break Scanner;  // Exit loop with Piterator at null.
            MetaNode ChildMetaNode=  // Get a reference to...
              (MetaNode)  // ...the child MetaNode which is...
              ChildPiteratorOfMetaNode.getE(); // ...the present element.
            if  // Child MetaNode has desired attribute.  Return it.
              ( ChildMetaNode.hasAttributeB( InKeyString, InValueObject ) )
              break Scanner;  // Exit loop with Piterator at found child.
            // This child MetaNode does not have the desired key and value.
            ChildPiteratorOfMetaNode.next();  // Advance to next child.
            } // Scan children looking for desired attribute.
          return ChildPiteratorOfMetaNode;  // Return child-pointing Piterator.
          }

    // Miscellaneous methods.

      public DataNode getDataNode()
        /* This returns the DataNode associated with this MetaNode.  */
        {
          return TheDataNode;  // Return the user object associated with this node.
          }

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
              theMetaChildren.put(   // Add...
                MapChildMetaNode  // ... the child MetaNode.
                );
              } // Create new HashMap entry.
          return MapChildMetaNode;  // Return new/old child from map as result.
          }

      public Piterator<MetaNode> getChildPiteratorOfMetaNode(  )
        /* This method returns a Piterator for iterating
          through this MetaNode's MetaChildren.
          */
        {
          return theMetaChildren.getPiteratorOfMetaNode();
          }

    } // class MetaNode.
