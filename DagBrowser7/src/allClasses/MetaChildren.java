package allClasses;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.ListIterator;
//import java.util.HashMap;
//import java.util.Map;
import java.util.ArrayList;

//public class MetaChildren<K,V>
public class MetaChildren
  
  /* This class implements a Collection of child MetaNodes.
    Presently is uses an ArrayList to store them.
    Before that is used a HashMap and before that a LinkedHashMap,
    in which the MetaNode's DataNode was the key 
    and the MetaNode was the value.
    */

  { // class MetaChildren 

    private ArrayList< IDNumber > TheArrayList;  // Container for children.

		MetaChildren() 
      // Constructor.
      {
        TheArrayList=  // Construct the child MetaNode container as...
          new ArrayList< IDNumber >( ); // ...an ArrayList of IDNumbe-s.
        }

    // Getter methods.

      public Collection<MetaNode> getCollectionOfMetaNode()
        /* This method returns a Collection containing the child MetaNodes.  */
        { 
          @SuppressWarnings("unchecked")
          Collection<MetaNode> ValuesCollectionOfMetaNode= 
            (Collection<MetaNode>)  // Kludgey double-caste needed...
            (Collection<?>)  // ...because of use of generic types.
            TheArrayList;
          
          return ValuesCollectionOfMetaNode;
          }

      public Iterator<MetaNode> iterator()  
        /* This method returns an iterator for the child MetaNodes. */
        { 
          Collection<MetaNode> ValuesCollection=  // Calculate the Collection.
            getCollectionOfMetaNode();
          return ValuesCollection.iterator();  // Return an iterator built from it.
          }

      public ListIterator<IDNumber> listIterator()
        /* This method returns a ListIterator for the child MetaNodes. */
        { 
          /*
          ArrayList<MetaNode> AnArrayList=
            (ArrayList<MetaNode>)  // Kludgey double-caste needed...
            (ArrayList<?>)  // ...because of use of generic types.
            TheArrayList;
          */
          return TheArrayList.listIterator();
          }

      public Piterator<MetaNode> getPiteratorOfMetaNode()
        /* This method returns a Piterator for this MetaNode's 
          child MetaNodes.  */
        { 
          Iterator<MetaNode> ValuesIteratorMetaNode=
            iterator();
          Piterator<MetaNode> ValuesPiteratorMetaNode=
                new Piterator<>( ValuesIteratorMetaNode );
          return ValuesPiteratorMetaNode;
          }

      public MetaNode get( Object KeyObject )
        /* This method returns the child MetaNode 
          which is associated with DataNode KeyObject,
          or null if there is no such MetaNode.
          */
        {
          MetaNode scanMetaNode;
          Piterator < MetaNode > ChildPiterator= getPiteratorOfMetaNode();
          while (true) {
            scanMetaNode= ChildPiterator.getE();  // Cache present candidate. 
            if ( scanMetaNode == null )  // Exit if past end.
              break; 
            if  // Exit if found.
              ( KeyObject.equals(scanMetaNode.getDataNode()) )
              break; 
            ChildPiterator.next();  // Advance Piterator to next candidate.
            }
          return scanMetaNode;
          }
    
    public void add( IDNumber InIDNumber )
      /* This method adds child InIDNumber to this MetaChildren instance.
        IDNumber is the superclass of MetaNode,
        and might be added as a MetaNode place-holder 
        during reading from disk.
        If the new child is an actual MetaNode then there should not 
        already be a MetaNode child with the same DataNode.
        */
      { 
        TheArrayList.add( InIDNumber );  // Add the child object.
        }

    // rw processors.

      public static MetaChildren rwGroupMetaChildren
        ( MetaFile inMetaFile, 
          MetaChildren inMetaNode,
          DataNode InParentDataNode
          )
        throws IOException
        /* This rw-processes the MetaChildren.
            If inMetaNode != null then it writes the children
              to the MetaFile, and InParentDataNode is ignored.
            If inMetaNode == null then it reads the children
              using InParentDataNode to look up DataNode names,
              and returns a new MetaChildren instance as the function value.
            */
        {
          inMetaFile.rwListBegin( );
          inMetaFile.rwLiteral( " MetaChildren" );

          if ( inMetaNode == null )
            inMetaNode= readMetaChildren( inMetaFile, InParentDataNode );
            else
            writeMetaChildren( inMetaFile, inMetaNode );

          inMetaFile.rwListEnd( );
          return inMetaNode;
          }

      private static MetaChildren readMetaChildren
        ( MetaFile inMetaFile, DataNode InParentDataNode )
        throws IOException
        /* This reads a MetaChildren from MetaFile inMetaFile
          and returns it as the result.  
          It uses InParentDataNode for name lookups.  
          */
        {
          MetaChildren newMetaChildren =    // Initialize newMetaChildren to be...
            new MetaChildren( ); // ...an empty default instance.
          while ( true )  // Read all children.
            { // Read a child or exit.
              IDNumber newIDNumber= null; // Variable for use in reading ahead.
              inMetaFile.rwIndentedWhiteSpace( );  // Go to proper column.
              if  // Exit loop if end character present.
                ( inMetaFile.testTerminatorI( ")" ) != 0 )
                break;  // Exit loop.
              switch // Read child based on RwStructure.
                ( inMetaFile.TheRwStructure )
                {
                  case FLAT:
                    newIDNumber= // Read a single IDNumber.
                      MetaNode.rwIDNumber( inMetaFile, null );
                    break;
                  case NESTED:
                    newIDNumber=  // Read the possibly nested MetaNode.
                      MetaNode.rwFlatOrNestedMetaNode( 
                        inMetaFile, 
                        null, 
                        InParentDataNode 
                        );
                    break;
                  }
              newMetaChildren.add(  // Store...
                newIDNumber // ...the new child MetaNode.
                );
              } // Read a child or exit.
          return newMetaChildren;  // Return resulting MetaChildren instance.
          }

      private static void writeMetaChildren
        ( MetaFile inMetaFile, MetaChildren inMetaNode )
        throws IOException
        /* This writes the MetaChildren instance inMetaNode
          using MetaFile inMetaFile.
          If MetaFile.TheRwStructure == FLAT then it writes ID numbers only,
          otherwise it recursively writes the complete MetaNodes.
          */
        {
          Iterator<MetaNode> childIterator=  // Create child iterator.
            inMetaNode.iterator();
          while // Write all the children.
            ( childIterator.hasNext() ) // There is a next child.
            { // Write one child.
              IDNumber TheIDNumber= childIterator.next();  // Get the child MetaNode.
              switch // Write child based on RwStructure.
                ( inMetaFile.TheRwStructure )
                {
                  case FLAT:
                    TheIDNumber.rwNumberField( inMetaFile );  // Write ID # only.
                    break;
                  case NESTED:
                    if (TheIDNumber instanceof MetaNode)
                      //MetaNode TheMetaNode=  // Get the MetaNode...
                        MetaNode.rwFlatOrNestedMetaNode(   // Write MetaNode.
                          inMetaFile, (MetaNode)TheIDNumber, null );
                      else
                      IDNumber.rwIDNumber( inMetaFile, TheIDNumber );
                    break;
                  }
              } // Write one child.
          }

        public void rwRecurseFlatV
          ( MetaFile inMetaFile, DataNode parentDataNode )
          throws IOException
          /* This method is used to recursively read-write process
            the children of this MetaChildren instance,
            using the MetaFile inMetaFile.
            It should be called only in FLAT mode, meaning when
            ( MetaFile.TheRwStructure == MetaFile.RwStructure.FLAT ).

            The difference between this method and rwGroupMetaChildren(..)
            is that this method processes the children in the text file
            as a flat sequence of MetaNodes, not as a 
            syntactically enclosed MetaChildren-group of MetaNodes.
            The text sequence should be a flat list of flat children, 
            using IDNumbers to refer to nested children.
            
            For each child:
            * If writing then it writes the MetaNode or IDNumber,
              whichever is the class of the child.
            * If reading then the present child should be 
              an IDNumber instance whose IDNumber value was read earlier.
              In this case it will call readAndConvertIDNumber(..) to search 
              the file text for the unique MetaNode with 
              the same IDNumber value.  The IDNumber instance 
              will be replaced with that a new constructed MetaNode instance
              with same IDNumber value.

            As usual, DataNode parentDataNode is used for name lookup 
            during reading, but is ignored during writing.
            */
          {
            ListIterator < IDNumber > ChildListIterator=   // Get iterator.
              listIterator();
            while // rw-process all the children.
              ( ChildListIterator.hasNext() ) // There is a next child.
              { // Process this child.
                IDNumber TheIDNumber=   // Get the child.
                  ChildListIterator.next();
                if // Process according to direction.
                  ( inMetaFile.getWritingB() )  // Writing.
                  if ( TheIDNumber instanceof MetaNode )  // Is MetaNode.
                    MetaNode.rwFlatOrNestedMetaNode(   // Write MetaNode.
                      inMetaFile, (MetaNode)TheIDNumber, null );
                    else  // Is IDNumber.
                    IDNumber.rwIDNumber(    // Write IDNumber.
                      inMetaFile, TheIDNumber );
                  else  // Reading.
                  ChildListIterator.set( // Replace the child by the...
                    inMetaFile.readAndConvertIDNumber( // ...MetaNode equivalent...
                      TheIDNumber,  // ...of IDNumber using...
                      parentDataNode  // ...provided parent for lookup.
                      )
                    ); // read.
                } // Process this child.
              }

      public boolean purgeTryB()
        /* This method tries to purge child MetaNode-s which contain
          no useful information, meaning no attributes in them
          or any of their descendents.
          
          It returns true if no child MetaNode-s survived the purge.
          It returns false otherwise.
          */
        {
          boolean childrenPurgedB=  // Set default result of purge failure.
            false;
          Processor: {
            Iterator < MetaNode > ChildIterator= iterator();
            Scanner: while (true) { // Try scanning all  children for purging. 
              if ( ! ChildIterator.hasNext() )  //  There are no more children.
                break Scanner;  // Exit child scanner loop.
              MetaNode ChildMetaNode=  // Get a reference to...
                (MetaNode)  // ...the child MetaNode which is...
                ChildIterator.next();  // ...the next one.
              if ( ! ChildMetaNode.purgeTryB() )  // The child is not purgable.
                break Processor;  // Exit with default no-purge indication.
              ChildIterator.remove();  // Remove child from MetaChildren.
              } // Try scanning all  children for purging. 
            childrenPurgedB= true; // Override result for purge success.
            }
          return childrenPurgedB;  // Return whether all children were purged.
          }

    } // class MetaChildren 
