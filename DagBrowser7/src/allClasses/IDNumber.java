package allClasses;

public class IDNumber // Superclass of MetaNode.

  /* This is the class which represents an ID number.  
    It serves two purposes:
    * It is used as a place-holder for as yet to be loaded
      data in the MetaFile lazy-loading process.
    * It is the superclass of the MetaNode class, 
      the class of objects that store meta-data about DataNodes.
      MetaNodes are saved to and loaded from the state MetaFiles.

    The values for the number in this class can come from two places:
    * It can be a new value created by incrementing the static counter nextI.
    * It can be loaded from the MetaFile using rwIDNumber( ).

    At first this class will be used for IDs for MetaNode's only.
    Later it might be used for IDs other subclasses.
    */

  {

    private static int nextI= 1;  // Counter with next ID number to be used.

    private int theI= 0;  // ID number value.  0 means undefined.

    // Constructors.

      public IDNumber( )  
        /* This constructor is used for creation of new objects.
          It assigns the next available ID number using counter nextI.
          */
        { 
          theI= nextI++;  // Allocate and store a new number.
          }

      public IDNumber( int theI )  
        /* This constructor is used for loading old objects.
          The ID # value is InI.
          It can be used to set a particular non-zero value,
          or a zero value so that rwIDNumber( ) will set it later.
          */
        { 
          this.theI= theI;  // Use the number provided to oonstructor.
          }

    // static methods.

      private static void skipThisNumber( int numbersToSkipI )
        /* Makes certain that numbersToSkipI is not used as the ID number
          in any new instances.  It does this simply by making certain that
          nextI is greater than this number.  */
        {
          if ( numbersToSkipI >= nextI )  // Increase nextI if needed.
            nextI= numbersToSkipI + 1;  // Increase nextI.
          }

      public static IDNumber rwIDNumber(  // Reader/writer.
          MetaFile inMetaFile, 
          IDNumber inOutIDNumber 
          )
        /* This rw-processes IDNumber inOutIDNumber
          with the MetaFile inMetaFile.
          If inOutIDNumber == null the it allocates 
          an actual IDNumber instance
          with field theEpiInputStreamI == 0 so that it will be read.
          */
        { 
          if ( inOutIDNumber == null )  // Allocate IDNumber if none provided.
            inOutIDNumber= new IDNumber( 0 );

          inOutIDNumber.rw( inMetaFile );  // Process the fields.

          return inOutIDNumber;  // Return possible new IDNumber.
          }

    // Instance methods.

      public int getTheI( )  
        /* This method returns the IDNumber int value.  */
        { 
          return theI;
          }

      public void rw( MetaFile inMetaFile )
        /* This rw-processes this IDNumber's fields
          with MetaFile inMetaFile.
          This is for access by subclasses that want
          to process the number field of their superclass only.
          */
        {
          inMetaFile.rwIndentedWhiteSpace( );  // Rw white-space.
          inMetaFile.rwLiteral( "#" );  // Rw special introducer character.
          { // Load or save theEpiInputStreamI.
            if ( theI == 0 )  // Value hasn't been defined yet.
              { // Read and define value.
                String NumberString= inMetaFile.readTokenString( );
                int I= Integer.parseInt( NumberString );
                theI= I;  // Save value in instance variable.
                skipThisNumber( I );  // Make certain this # is not reused.
                } // Read and define value.
            else  // An IDNumber was provided.
              { // Save IDNumber to file.
                inMetaFile.writeToken( Integer.toString( theI ) );
                } // Save IDNumber to file.
            } // Load or save theEpiInputStreamI.
          }

    }
