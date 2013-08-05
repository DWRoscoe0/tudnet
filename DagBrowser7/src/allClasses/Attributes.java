package allClasses;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Attributes 
  {

      public static HashMap< String, Object >  rwAttributesHashMap
        ( MetaFile inMetaFile, HashMap< String, Object > InAttributesHashMap )
        /* This rw-processes the Attributes HashMap with
          MetaFile inMetaFile.  
          */
        { // rwAttributesHashMap()
          inMetaFile.rwListBegin( );
          inMetaFile.rwLiteral( " Attributes" );
          if ( InAttributesHashMap == null )
            InAttributesHashMap=  // Read entries to make a map.
              rwAttributesHashMapRead( inMetaFile );
            else
            rwAttributesHashMapWrite(  // Write entries...
              inMetaFile,  // ...to inMetaFile...
              InAttributesHashMap  // ...of this HashMap.
              );
          inMetaFile.rwListEnd( );
          return InAttributesHashMap;  // Return new or original HashMap.
          } // rwAttributesHashMap()

      private static HashMap< String, Object > rwAttributesHashMapRead
        ( MetaFile inMetaFile )
        /* This reads hash map entries from inMetaFile and returns a HashMap
          that contains them.  */
        {
          HashMap<String, Object> OutAttributesHashMap =  // Initialize...
            new HashMap<String, Object>( 2 );  // ...to a small empty map.
          while ( true )  // Read all entries.
            { // Read an entry or exit.
              inMetaFile.rwIndentedWhiteSpace( );  // Go to proper column.
              if  // Test for leading parenthesis and exit loop if fail.
                ( inMetaFile.testTerminatorI( ")" ) != 0 )
                break;  // Exit loop.
              inMetaFile.rwLiteral( "( " );  // Read the same thing.
              String KeyString= inMetaFile.readTokenString( );
              inMetaFile.rwLiteral( " " );
              String ValueString= inMetaFile.readTokenString( );
              inMetaFile.rwLiteral( " )" );
              OutAttributesHashMap.put( KeyString, ValueString );
              } // Read an entry or exit.
          return OutAttributesHashMap;
          }

      private static void rwAttributesHashMapWrite
        ( MetaFile inMetaFile, HashMap< String, Object > InAttributesHashMap )
        /* This writes to InMetaFile the entries of an Attributes HashMap.  */
        { // rwAttributesHashMapWrite()
          Iterator < Map.Entry < String, Object > > MapIterator=  // Get an iterator...
            InAttributesHashMap.
            entrySet().
            iterator();  // ...for HashMap entries.
          while // Save all the HashMap's entries.
            ( MapIterator.hasNext() ) // There is a next Entry.
            { // Save this HashMap entry.
              Map.Entry < String, Object > AnEntry= // Get Entry 
                MapIterator.next();  // ...that is next Entry.
              inMetaFile.rwIndentedLiteral( "(" );
              inMetaFile.rwLiteral( " "+AnEntry.getKey( ) );
              inMetaFile.rwLiteral( " "+(String)AnEntry.getValue( ) );
              inMetaFile.rwLiteral( " )" );
              } // Save this HashMap entry.
          } // rwAttributesHashMapWrite()

    }
