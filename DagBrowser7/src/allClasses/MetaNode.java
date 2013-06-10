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
    the order of entries in the children LinkedHashaaMap.
    Also the remaining metadata was moved to attributes stored in
    instance variable AttributesHashMap.
    
    Next the selection order information was moved to attributes,
    and the order-preserving feature of LinkedHashMaps was no longer used.
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
      //protected LinkedHashMap< Object, MetaNode  > theMetaChildren= null;
      protected MetaChildren < Object, MetaNode  > theMetaChildren= null;
        /* MetaNodes of DataNode children which themselves have meta-data.  */

    // Constructors (2).
    
      private MetaNode( )
        /* Constructor of blank MetaNodes.  */
        {
          super( 0 );  // Set superclass ID # to 0 so it can be loaded later.
          }
    
      public MetaNode( DataNode InDataNode )
        /* Full constructor of a MetaNode to be associated with
          the single DataNode InDataNode, 
          but with no attributes or child MetaNodes, yet.  
          */
        {
          //TheIDNumber= new IDNumber( );  // Create a new IDNumber for this node.
          super( );  // Assign the superclass ID # to be something meaningful.

          TheDataNode=  // Save DataNode associated with this MetaNode.
            InDataNode; 

          AttributesHashMap =  // Initialize attributes to be...
            new HashMap<String, Object>( 2 );  // ...a small empty map.
          theMetaChildren =  // Initialize children to be an empty...
            new MetaChildren<Object, MetaNode>( );  // ...and empty MetaChildren.
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
          return AttributesHashMap.put( KeyString, ValueObject );
          }

    // Other methods.

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
          meaning the node can't be purged, true otherwise.
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

      /*
      public static MetaNode XrwNumberOrNode
        ( MetaNode InMetaNode, DataNode ParentDataNode )
        /* rw/Writes either an IDNumber node or the whole MetaNode, 
          depending on the RwMode context.
          */
      /*
        { 
          switch ( MetaFile.TheRwMode ) {
            case FLAT:
              //InMetaNode.TheIDNumber= IDNumber.rwIDNumber(  // Rw...
              //  InMetaNode.TheIDNumber  // ...TheIDNumber.
              //  );
              InMetaNode.rwIDNumber();  // Rw the ID #.
              break;
            case HIERARCHICAL:
              rwMultiMetaNode( InMetaNode, ParentDataNode );
              break;
            }

          return InMetaNode;  // Return the new or the original MetaNode.
          }
      */

      public static MetaNode rwMultiMetaNode
        ( MetaNode InMetaNode, DataNode ParentDataNode )
        /* This is like rwMetaNode(..) except that it will process
          InMetaNode and its descendents if in flat file mode.
          It does it either hierarchically as one chunk,
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

      
    // Methods which deal with the children.

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

      MetaNode getChildWithAttributeMetaNode
        ( String InKeyString, Object InValueObject )
        /* This method returns the first child MetaNode, if any, 
          with an attribute with key InKeyString and value InValueObject.
          If no child MetaNode has the attribute then null is returned.
          */
        { // MetaNode getChildWithAttributeMetaNode( String InKeyString )
          MetaNode ResultChildMetaNode= null;  // Assume no result child.
          Piterator< MetaNode > ChildPiterator= 
            getChildPiterator(  );
          Scanner: while (true) { // Scan children for desired attribute. 
            Tester: { // Test child MetaNode for attribute.
              if ( ChildPiterator.getE() == null )  //  There are no more children.
                break Scanner;  // Exit search loop with default null result.
              MetaNode ChildMetaNode=  // Get a reference to...
                (MetaNode)  // ...the child MetaNode which is...
                ChildPiterator.getE(); // .  // ...that next Entry's...
                // getValue();  // ...Value.
              if  // Child MetaNode does not have the desired key and value.
                ( ! ChildMetaNode.hasAttributeB( InKeyString, InValueObject ) )
                break Tester;  // Exit Tester.
              { // Found child MetaNode with desired attribute.  Return it.
                ResultChildMetaNode=  // Override default null result with...
                  ChildMetaNode;  // ...found child MetaNode.
                break Scanner;  // Exit search loop with found child.
                } // Found child MetaNode with desired attribute.  Return it.
              } // Test child MetaNode for attribute.
            ChildPiterator.next();  // Point to next child MetaNode.
            } // Scan children looking for desired attribute.
          return ResultChildMetaNode; // return result child, if any.
          } // MetaNode getChildWithAttributeMetaNode( String InKeyString )

      Piterator< MetaNode > getChildWithAttributePiterator
        ( String InKeyString, Object InValueObject )
        /* This method returns a Piterator into the MetaChildren,
          pointing to the first child MetaNode, if any, 
          with an attribute with key==InKeyString and value==InValueObject.
          If no child MetaNode has this attribute then 
          the returned Piterator will point to null. 
          */
        {
          Piterator< MetaNode > ChildPiterator=  // Get initializzed Piterator.
            getChildPiterator(  );
          Scanner: while (true) { // Scan children for desired attribute. 
            if ( ChildPiterator.getE() == null )  //  There are no more children.
              break Scanner;  // Exit loop with Piterator at null.
            MetaNode ChildMetaNode=  // Get a reference to...
              (MetaNode)  // ...the child MetaNode which is...
              ChildPiterator.getE(); // .  // ...the present element.
            if  // Child MetaNode has desired attribute.  Return it.
              ( ChildMetaNode.hasAttributeB( InKeyString, InValueObject ) )
              break Scanner;  // Exit loop with Piterator at found child.
            // The child MetaNode does not have the desired key and value.
            ChildPiterator.next();  // Advance Piterator to next child MetaNode.
            } // Scan children looking for desired attribute.
          return ChildPiterator;  // Return Piterator pointing to found child.
          }

      MetaNode PutChildUserObjectMetaNode( Object InObject )
        /* This method puts the Object InObject in a child MetaNode
          within this its parent MetaNode.
          It creates a new child MetaNode if one with InObject
          does not already exist.
          In either case, it returns the child MetaNode with InObject.
          */
        {
          /*
          MetaNode MapChildMetaNode=  // Try to get the MetaNode...
            theMetaChildren.get(  // ...from the MetaChildren...
              InObject );  // ... from the entry containing InObject.
          */
          MetaNode MapChildMetaNode=  // Try to get the MetaNode...
            MetaChildren.get(  // ...from the MetaChildren...
              theMetaChildren,
              InObject );  // ... from the entry containing InObject.
          if ( MapChildMetaNode == null ) // Create new HashMap entry if not there.
            { // Create new HashMap entry.
              MapChildMetaNode= // Create new MetaNode with desired Object.
                new MetaNode( (DataNode)InObject );
              MetaChildren.put(   // Add...
                theMetaChildren,  // ...to MetaChildren...
                MapChildMetaNode  // ... the child MetaNode.
                );
              } // Create new HashMap entry.
          return MapChildMetaNode;  // Return new/old child from map as result.
          }

      public Piterator< MetaNode > getChildPiterator(  )
        /* This method returns a Piterator for iterating
          over the MetaChildren MetaNodes.
          */
        {
        
          Iterator  // Get a regular iterator for the MetaChildren.
            < MetaNode > ChildIterator= 
              theMetaChildren.iterator();
            
          Piterator  // Make a Piterator from the Iterator.
            < MetaNode > ChildPiterator= 
              new Piterator<>( ChildIterator );

          return ChildPiterator;  // Return the Piterator.

          }

    } // class MetaNode.