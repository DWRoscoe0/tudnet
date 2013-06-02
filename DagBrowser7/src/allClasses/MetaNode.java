package allClasses;


import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class MetaNode

  extends IDNumber

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

      //public IDNumber TheIDNumber= null;  // ID #.
      private DataNode TheDataNode= null;  // Associated DataNode.
      private HashMap< String, Object > AttributesHashMap= null;
        /* Attributes of the DataNode, if any.
          The Key is a String name.
          The Value is a String value.
          */
      protected LinkedHashMap< Object, MetaNode  > ChildrenLinkedHashMap= null;
        /* MetaNodes of DataNode children which themselves have meta-data.
          The Key is a child user DataNode.
          The Value is the associated MetaNode that contains
          the DataNode and its meta-data.
          */

    // Constructors (2).
    
      public MetaNode( )
        /* Constructor of blank MetaNode..  */
        { // MetaNode( )
          super( 0 );  // Set superclass ID # to 0 so it can be loaded.
          } // MetaNode( )
    
      public MetaNode( DataNode InDataNode )
        /* Full constructor of a MetaNode to be associated with
          a single DataNode InDataNode, 
          but no attributes or child MetaNodes, yet.  
          */
        {
          //TheIDNumber= new IDNumber( );  // Create a new IDNumber for this node.
          super( );  // Assign the superclass ID # to be something meaningful.

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

      public DataNode getDataNode()
        /* This returns the DataNode associated with this MetaNode.  */
        { // getUserObject()
          return TheDataNode;  // Return the user object associated with this node.
          } // getUserObject()

      public boolean purgeB()
        /* This method is used to purge MetaNode-s which contain
          no useful information, meaning no attributes.
          If this MetaNode has no attributes then it
          recursively tries purging its child MetaNode-s.
          It returns true if it finds attributes in any MetaNode, 
          false otherwise.
          */
        { // boolean purgeB()
          boolean OkayToRemoveB= false;  // Assume we can't complete purge.
          Processor: {  // Purge testing and processing.
            if ( ! AttributesHashMap.isEmpty() )  //* There are attributes.
              break Processor;  // Exit with default no-purge indication.
            Iterator  // Get an iterator for HashMap containing the children.
              < Map.Entry < Object, MetaNode > > MapIterator= 
              ChildrenLinkedHashMap.entrySet().iterator();
            ChildScanner: while (true) { // Scan children for purging. 
              if ( ! MapIterator.hasNext() )  //  There are no more children.
                break ChildScanner;  // Exit child scanner loop.
              MetaNode ChildMetaNode=  // Get a reference to...
                (MetaNode)  // ...the child MetaNode which is...
                MapIterator.next().  // ...that next Entry's...
                getValue();  // ...Value.
              if ( ! ChildMetaNode.purgeB() )  // Child is not purgable.
                break Processor;  // Exit with default no-purge indication.
              MapIterator.remove();  // Remove purgable child from map.
              } // Scan children for purging. 
            OkayToRemoveB= true;  // Indicate okay for complete purge.
            }  // Purge testing and processing.
          return OkayToRemoveB;  // Return calculated purge result.
          } // boolean purgeB()

      public static MetaNode rwNumberOrNode
        ( MetaNode InMetaNode, DataNode ParentDataNode )
        /* rw/Writes either an IDNumber node or the whole MetaNode, 
          depending on the RwMode context.
          */
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

      public static MetaNode rwMultiMetaNode
        ( MetaNode InMetaNode, DataNode ParentDataNode )
        /* This is like rwMetaNode(..) except that it will process
          InMetaNode and its descendents if in flat file mode.
          It does it either hierarchically as one chunk,
          or flat by splitting off the individual descendent nodes,
          depending on the value of MetaFile.FlatFileB.
          See rwMetaNode(..) for more information.
          */
        { // rwSplittableMetaNode()
          MetaNode ResultMetaNode=  // Process main MetaNode.
            rwMetaNode( InMetaNode, ParentDataNode );

          if  // Process the children again if we're splitting them off.
            ( MetaFile.TheRwMode == MetaFile.RwMode.FLAT )
            { // Process the children separately.
              Iterator < Map.Entry < Object, MetaNode > > MapIterator=  // Get an iterator...
                InMetaNode.ChildrenLinkedHashMap.
                entrySet().
                iterator();  // ...for HashMap entries.
              while // Save all the HashMap's entries.
                ( MapIterator.hasNext() ) // There is a next Entry.
                { // Write this HashMap entry.
                  Map.Entry < Object, MetaNode > AnEntry= // Get Entry 
                    MapIterator.next();  // ...that is next Entry.
                  MetaNode TheMetaNode= AnEntry.getValue( );  // Get the value MetaNode.
                  MetaNode.rwMultiMetaNode( TheMetaNode, null );  // Write MetaNode.
                  } // Write this HashMap entry.
              } // Process the children separately.

          return ResultMetaNode;  // Return the main MetaNode.
          } // rwSplittableMetaNode()

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
          //InMetaNode.TheIDNumber= IDNumber.rwIDNumber(  // Rw...
          //  InMetaNode.TheIDNumber  // ...TheIDNumber.
          //  );
          InMetaNode.rwIDNumber();  // Rw the ID #.
            
          MetaFile.rwIndentedWhiteSpace( );  // Indent correctly.
          MetaFile.rwLiteral( "MetaNode" );  // Label as MetaNode list.
          InMetaNode.TheDataNode= DataRw.rwDataNode(  // Rw...
            InMetaNode.TheDataNode,  // ...TheDataNode using...
            ParentDataNode  // ...ParentDataNode for name lookups.
            );
          InMetaNode.AttributesHashMap=  // Rw the attributes.
            Attributes.rwAttributesHashMap( InMetaNode.AttributesHashMap );
          InMetaNode.ChildrenLinkedHashMap=  // Rw...
            Children.rwChildrenLinkedHashMap(  // ...the children hash map...
              InMetaNode.ChildrenLinkedHashMap, 
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
          Piterator< Map.Entry < Object, MetaNode > > ChildPiterator= 
            getChildPiterator(  );
          Scanner: while (true) { // Scan children for desired attribute. 
            Tester: { // Test child MetaNode for attribute.
              if ( ChildPiterator.getE() == null )  //  There are no more children.
                break Scanner;  // Exit search loop with default null result.
              MetaNode ChildMetaNode=  // Get a reference to...
                (MetaNode)  // ...the child MetaNode which is...
                ChildPiterator.getE().  // ...that next Entry's...
                getValue();  // ...Value.
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

      Piterator< Map.Entry < Object, MetaNode > > getChildWithAttributePiterator
        ( String InKeyString, Object InValueObject )
        /* This method returns a Piterator into the child HashMap,
          pointing to the first child MetaNode, if any, 
          with an attribute with key InKeyString and value InValueObject.
          If no child MetaNode has the attribute then 
          the returned Piterator will point to null. 
          */
        {
          Piterator< Map.Entry < Object, MetaNode > > ChildPiterator= 
            getChildPiterator(  );
          Scanner: while (true) { // Scan children for desired attribute. 
            if ( ChildPiterator.getE() == null )  //  There are no more children.
              break Scanner;  // Exit loop with Piterator at null.
            MetaNode ChildMetaNode=  // Get a reference to...
              (MetaNode)  // ...the child MetaNode which is...
              ChildPiterator.getE().  // ...that next Entry's...
              getValue();  // ...Value.
            if  // Found child MetaNode with desired attribute.  Return it.
              ( ChildMetaNode.hasAttributeB( InKeyString, InValueObject ) )
              break Scanner;  // Exit loop with Piterator at found child.
            // The child MetaNode does not have the desired key and value.
            ChildPiterator.next();  // Point to next child MetaNode.
            } // Scan children looking for desired attribute.
          return ChildPiterator;
          }

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

      public Piterator< Map.Entry < Object, MetaNode > > getChildPiterator(  )
        /* This method returns a Piterator for iterating
          over the child hash map.
          */
        { // getChildPiterator( )
        
          Iterator  // Get an iterator for HashMap containing the children.
            < Map.Entry < Object, MetaNode > > MapIterator= 
              ChildrenLinkedHashMap.entrySet().iterator();
            
          Piterator  // Make Piterator from Iterator.
            < Map.Entry < Object, MetaNode > > ChildPiterator= 
              new Piterator<>( MapIterator );

          return ChildPiterator;
          } // getChildPiterator( )

      /*
      public MetaNode xGetLastChildMetaNode(  )
        /* This method gets the child MetaNode of this MetaNode 
          which was referenced last, or null if there are no children.  
          It makes use of the fact that ChildrenLinkedHashMap
          links its entries together in use-order.
          
          Use of this method is being phased out and replace by
          an equivalent method in class Selection.
          */
        /*
        { // GetLastChildMetaNode( )
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
          } // GetLastChildMetaNode( )
        */

      /*
      DataNode xGetLastReferencedChildDataNode(  )
        /* This method gets the user object DataNode from
          the child MetaNode in this MetaNode 
          which was referenced last, or null if there are no children.  */
      /*
        { // GetLastReferencedChildDagNode( )
          DataNode RecentChildDataNode= null;// assume default value of null.
          do { // override with child if there is one.
            MetaNode LastChildMetaNode= GetLastChildMetaNode( );
            if (LastChildMetaNode == null)  // there is no last child.
              break ;  // so keep the default null result.
            RecentChildDataNode=  // Result recent child DataNode is...
              LastChildMetaNode.   // ...the last child's...
              getDataNode();  // user object.
            } while ( false );  // override with child if there is one.
          return RecentChildDataNode; // return resulting DataNode, or null if none.
          } // GetLastReferencedChildDagNode( )
      */

    } // class MetaNode.
