package allClasses;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Attributes 
  {

      public static HashMap< String, Object >  rwAttributesHashMap
        ( HashMap< String, Object > InAttributesHashMap )
        /* This rw-processes the Attributes HashMap.  */
        { // rwAttributesHashMap()
          MetaFile.rwListBegin( );
          MetaFile.rwLiteral( " Attributes" );
          if ( InAttributesHashMap == null )
            InAttributesHashMap=  // Read entries to make a map.
              rwAttributesHashMapRead( );
            else
            rwAttributesHashMapWrite(  // Write entries.
              InAttributesHashMap );
          MetaFile.rwListEnd( );
          return InAttributesHashMap;  // Return new or original HashMap.
          } // rwAttributesHashMap()

      private static HashMap< String, Object > rwAttributesHashMapRead( )
        /* This reads hash map entries and returns a HashMap
          that contains them.  */
        { // rwAttributesHashMapRead( )
          HashMap<String, Object> OutAttributesHashMap =  // Initialize...
            new HashMap<String, Object>( 2 );  // ...to a small empty map.
          while ( true )  // Read all entries.
            { // Read an entry or exit.
              MetaFile.rwIndentedWhiteSpace( );  // Go to proper column.
              if  // Test for leading parenthesis and exit loop if fail.
                ( MetaFile.testTerminatorI( ")" ) != 0 )
                break;  // Exit loop.
              MetaFile.rwLiteral( "( " );  // Read the same thing.
              String KeyString= MetaFile.readTokenString( );
              MetaFile.rwLiteral( " " );
              String ValueString= MetaFile.readTokenString( );
              MetaFile.rwLiteral( " )" );
              OutAttributesHashMap.put( KeyString, ValueString );
              } // Read an entry or exit.
          return OutAttributesHashMap;
          } // rwAttributesHashMapRead( )

      private static void rwAttributesHashMapWrite
        ( HashMap< String, Object > InAttributesHashMap )
        /* This writes the entries of an Attributes HashMap.  */
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
              MetaFile.rwIndentedLiteral( "(" );
              MetaFile.rwLiteral( " "+AnEntry.getKey( ) );
              MetaFile.rwLiteral( " "+(String)AnEntry.getValue( ) );
              MetaFile.rwLiteral( " )" );
              } // Save this HashMap entry.
          } // rwAttributesHashMapWrite()

    }
