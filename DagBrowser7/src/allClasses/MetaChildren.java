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

      public static MetaChildren rwMetaChildren
        ( MetaChildren inMetaNode,
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
          MetaFile.rwListBegin( );
          MetaFile.rwLiteral( " MetaChildren" );

          if ( inMetaNode == null )
            inMetaNode= 
              readMetaChildren( InParentDataNode );
            else
            writeMetaChildren( inMetaNode );

          MetaFile.rwListEnd( );
          return inMetaNode;
          }

      private static MetaChildren readMetaChildren( DataNode InParentDataNode )
        throws IOException
        /* This reads a MetaChildren from the file and returns it as the result.  
          It uses InParentDataNode for name lookups.  
          */
        {
          MetaChildren newMetaChildren =    // Initialize newMetaChildren to be...
            new MetaChildren( ); // ...an empty default instance.
          while ( true )  // Read all children.
            { // Read a child or exit.
              IDNumber newIDNumber= null; // Variable for use in reading ahead.
              MetaFile.rwIndentedWhiteSpace( );  // Go to proper column.
              if  // Exit loop if end character present.
                ( MetaFile.testTerminatorI( ")" ) != 0 )
                break;  // Exit loop.
              switch // Read child based on RwStructure.
                ( MetaFile.TheRwStructure )
                {
                  case FLAT:
                    newIDNumber= // Read a single IDNumber.
                      MetaNode.rwIDNumber( null );
                    break;
                  case HIERARCHICAL:
                    newIDNumber=  // Read the possibly nested MetaNode.
                      MetaNode.rwFlatMetaNode( null, InParentDataNode );
                    break;
                  }
              newMetaChildren.add(  // Store...
                newIDNumber // ...the new child MetaNode.
                );
              } // Read a child or exit.
          return newMetaChildren;  // Return resulting MetaChildren instance.
          }

      private static void writeMetaChildren
        ( MetaChildren inMetaNode )
        throws IOException
        /* This writes the MetaChildren instance inMetaNode.  
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
                ( MetaFile.TheRwStructure )
                {
                  case FLAT:
                    TheIDNumber.rwNumberField();  // Write the ID # only.
                    break;
                  case HIERARCHICAL:
                    if (TheIDNumber instanceof MetaNode)
                      //MetaNode TheMetaNode=  // Get the MetaNode...
                        MetaNode.rwFlatMetaNode(   // Write MetaNode.
                          (MetaNode)TheIDNumber, null );
                      else
                      IDNumber.rwIDNumber( TheIDNumber );
                    break;
                  }
              } // Write one child.
          }


        public void rwFlatV( DataNode parentDataNode )
          throws IOException
          /* This method is a companion to MetaNode.rwFlatMetaNode(..).
            It rw-processes a MetaNode's children but only in flat mode.
            It should be called only if 
            ( MetaFile.TheRwStructure == MetaFile.RwStructure.FLAT ).
            The difference between this method and rwMetaChildren(..)
            is that this method does the delayed part of the processing
            of the children.  For each child:
            * If writing then it writes the MetaNode or IDNumber,
              whichever is the class of the child.
              If it is a MetaNode then only the IDField value 
              was written earlier.
            * If reading then the present child should be an IDNumber instance.
              whose IDNumber value was read earlier.
              It will be replaced with the MetaNode from the state file
              that has same IDNumber value.
            DataNode parentDataNode is for name lookup during reading,
            but is ignored during writing.

            Presently it does nothing in Read mode.  ????
            */
          {
            ListIterator < IDNumber > ChildListIterator=   // Get iterator.
              listIterator();
            while // rw-process all the children.
              ( ChildListIterator.hasNext() ) // There is a next child.
              { // Process this child.
                IDNumber TheIDNumber=   // Get the child.
                  ChildListIterator.next();
                if ( TheIDNumber instanceof MetaNode )  // Is MetaNode.
                  MetaNode.rwFlatMetaNode(   // Write MetaNode.
                    (MetaNode)TheIDNumber, null );
                  else  // Is IDNumber.
                  if( MetaFile.getWritingB() )  // Writing.
                    IDNumber.rwIDNumber( TheIDNumber );   // Write IDNumber.
                    else  // Reading.
                    ChildListIterator.set( // Replace the child by the...
                      MetaFile.rwConvertIDNumber( // ...MetaNode equivalent...
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
