package allClasses;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Attributes 
  {

      public static HashMap< String, Object >  ioAttributesHashMap
        ( HashMap< String, Object > InAttributesHashMap )
        /* This io-processes the Attributes HashMap.  */
        { // ioAttributesHashMap()
          MetaFile.ioListBegin( );
          MetaFile.ioLiteral( " Attributes" );
          if ( InAttributesHashMap == null )
            InAttributesHashMap=  // Read entries to make a map.
              ioAttributesHashMapRead( );
            else
            ioAttributesHashMapWrite(  // Write entries.
              InAttributesHashMap );
          MetaFile.ioListEnd( );
          return InAttributesHashMap;  // Return new or original HashMap.
          } // ioAttributesHashMap()

      private static HashMap< String, Object > ioAttributesHashMapRead( )
        /* This reads hash map entries and returns a HashMap
          that contains them.  */
        { // ioAttributesHashMapRead( )
          HashMap<String, Object> OutAttributesHashMap =  // Initialize...
            new HashMap<String, Object>( 2 );  // ...to a small empty map.
          while ( true )  // Read all entries.
            { // Read an entry or exit.
              MetaFile.ioIndentedWhiteSpace( );  // Go to proper column.
              if  // Test for leading parenthesis and exit loop if fail.
                ( MetaFile.testTerminatorI( ")" ) != 0 )
                break;  // Exit loop.
              MetaFile.ioLiteral( "( " );  // Read the same thing.
              String KeyString= MetaFile.readTokenString( );
              MetaFile.ioLiteral( " " );
              String ValueString= MetaFile.readTokenString( );
              MetaFile.ioLiteral( " )" );
              OutAttributesHashMap.put( KeyString, ValueString );
              } // Read an entry or exit.
          return OutAttributesHashMap;
          } // ioAttributesHashMapRead( )

      private static void ioAttributesHashMapWrite
        ( HashMap< String, Object > InAttributesHashMap )
        /* This writes the entries of an Attributes HashMap.  */
        { // ioAttributesHashMapWrite()
          Iterator < Map.Entry < String, Object > > MapIterator=  // Get an iterator...
            InAttributesHashMap.
            entrySet().
            iterator();  // ...for HashMap entries.
          while // Save all the HashMap's entries.
            ( MapIterator.hasNext() ) // There is a next Entry.
            { // Save this HashMap entry.
              Map.Entry < String, Object > AnEntry= // Get Entry 
                MapIterator.next();  // ...that is next Entry.
              MetaFile.ioIndentedLiteral( "(" );
              MetaFile.ioLiteral( " "+AnEntry.getKey( ) );
              MetaFile.ioLiteral( " "+(String)AnEntry.getValue( ) );
              MetaFile.ioLiteral( " )" );
              } // Save this HashMap entry.
          } // ioAttributesHashMapWrite()

    }
