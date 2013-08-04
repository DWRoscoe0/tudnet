package allClasses;

public class IDNumber 
  /* This is the class which represents an ID number.
    It is meant to be used as a place-holder and a superclass
    for objects that can be saved to and loaded from the state files.
    The values for the number in instances can come from two placs:
    * It can create a new value by incrementing the static counter NextI.
    * It can load a value from the MetaFile using rwIDNumber( ).
    At first it will be used for IDs for MetaNode's only.
    Later it might be used for other subclasses.
    */
  {

    private static int NextI= 1;  // Counter with next number to be used.
    
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
        /* This constructor is used for loading old objects.
          The ID # value is InI.
          It can be used to set a particular non-zero value,
          or a zero value so that rwIDNumber( ) will set it later.
          */
        { 
          TheI= InI;  // Use the number provided as argument to constructor.
          }

    // static methods.
    
      private static void skipThisNumber( int NumberToSkipI )
        /* Makes certain that NumberToSkipI is not used as the ID number
          in any new instances.  It does this simply by making certain that
          NextI is greater than this number.  */
        {
          if ( NumberToSkipI >= NextI )  // Increase NextI if needed.
            NextI= NumberToSkipI + 1;  // Increase NextI.
          }
      
      public static IDNumber rwIDNumber( IDNumber InOutIDNumber )
        /* This rw-processes IDNumber InOutIDNumber.
          If InOutIDNumber == null the it allocates an actual IDNumber instance
          with field TheI == 0 so that it will be read.
          */
        { 
          if ( InOutIDNumber == null )  // Allocate IDNumber if none provided.
            InOutIDNumber= new IDNumber( 0 );
            
          InOutIDNumber.rw( );  // Process the fields.

          return InOutIDNumber;  // Return possible new IDNumber.
          }
        
    // instance methods.
    
      public int getTheI( )  
        /* This method returns the IDNumber int value.  */
        { 
          return TheI;
          }

      public void rw( )
        /* This rw-processes this IDNumber's fields.
          It simply calls rwNumber( ).
          */
        {
          rwNumberField();
          }

      public void rwNumberField( )
        /* This rw-processes this IDNumber number field.
          This is for access by subclasses that want
          to process the number field of their superclass only.
          */
        {
          MetaFile.rwIndentedWhiteSpace( );  // Rw the obligatory white-space.
          MetaFile.rwLiteral( "#" );  // Rw the special introducer character.
          { // Load or save TheI.
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
            } // Load or save TheI.
          }
 
    }
