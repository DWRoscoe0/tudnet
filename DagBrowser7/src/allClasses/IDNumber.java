package allClasses;

public class IDNumber 
  /* This is the class which represents an ID number.
    The values for the number can come from two placs:
    * It can create new ones by incrementing the counter NextI.
    * It can load values from the MetaFile using rwIDNumber( ).
    At first it will be used for IDs for MetaNode's only.
    Later it might be used for other superclasses.
    */
  {

    private static int NextI= 1;  // Next value to be used.
    
    private int TheI= 0;  // ID Value.  0 means undefined.
    
    // Constructors.
      
      public IDNumber( )  
        /* This constructor is used for creation of new objects.
          It assigns the next available ID number using counter NextI.
          */
        { 
          TheI= NextI++;  // Allocate and store a new number.
          }
    
      public IDNumber( int InI )  
        /* This constructor was used for loading old objects.
          The ID # value is InI.
          It can be used to set a particular non-zero value,
          or a zero value so that rwIDNumber( ) will set it later.
          */
        { 
          TheI= InI;  // Use the number provided as argument to constructor.
          }
      
    // Other methods.
    
      private static void skipThisNumber( int NumberToSkipI )
        /* Makes certain that NumberToSkipI is not used as the ID number
          in any new IDNumbers.  It does this simply by making certain that
          NextI is greater than this number.  */
        {
          if ( NumberToSkipI >= NextI )  // Increase NextI if needed.
            NextI= NumberToSkipI + 1;  // Increase NextI.
          }
        
      
      public void rwIDNumber( )
        /* This io-processes this IDNumber's value int TheI.  */
        { // io()
          MetaFile.rwIndentedWhiteSpace( );  // Rw the obligatory white-space.
          
          MetaFile.rwLiteral( "#" );  // Rw the special introducer character.
          { // Load or save The.
            if ( TheI == 0 )  // Value hasn't been defined yet.
              { // Read and define value.
                String NumberString= MetaFile.readTokenString( );
                int I= Integer.parseInt( NumberString );
                TheI= I;  // Save value in instance variable.
                skipThisNumber( I );  // Make certain this # is not reused.
                } // Read and define value.
            else  // An IDNumber was provided.
              { // Save IDNumber to file.
                MetaFile.writeToken( Integer.toString( TheI ) );
                } // Save IDNumber to file.
            } // Load or save The.

          } // io()
 
      /*
      public static IDNumber rwIDNumber( IDNumber InIDNumber )  /* ??? This 
          will be deleted when no longer needed. */
        /* This io-processes an IDNumber InIDNumber.  */
      /*
      { // io()
          MetaFile.rwIndentedWhiteSpace( );  // Rw the obligatory white-space.
          
          MetaFile.rwLiteral( "#" );  // Rw the special introducer character.
          { // Load or save IDNumber.
            if ( InIDNumber == null )  // An IDNumber was not provided.
              { // Create and load an IDNumber from file.
                String NumberString= MetaFile.readTokenString( );
                int I= Integer.parseInt( NumberString );
                InIDNumber= new IDNumber( I );  // Create one.
                skipThisNumber( I );  // Make certain this # is not reused.
                } // Create and load an IDNumber from file.
            else  // An IDNumber was provided.
              { // Save IDNumber to file.
                MetaFile.writeToken( Integer.toString( InIDNumber.TheI ) );
                } // Save IDNumber to file.
            }  // Load or save IDNumber.

          return InIDNumber;
          } // io()
      */

    }
