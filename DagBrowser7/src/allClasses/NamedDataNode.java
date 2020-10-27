package allClasses;

public class NamedDataNode 

  extends DataNode 

  /* This class is the base class for all named DataNodes.
    It adds a name and methods to manipulate that name
    to the base class DataNode.  

    This class defines no constructors.
    It relies on variations of the initializeV(...) method for initialization.
    One, and only one of these methods should be called after construction.
    Initialization was done this way for the following reasons:
    * Some subclasses, such as StateList, do their initialization this way
      to reduce boilerplate code.
    * Some subclasses don't actually know their names until later,
      for example, during the lazy loading of class instances such as Outline. 

    */

  {
    public static final String temporaryNameString= "NamedDataNode.TEMPORARY-NAME";
  
    
    private String nameString= temporaryNameString; // The name associated with this node.

    /* Initialization methods.
     * 
     * ///? Change all initializeV(..) methods to return (this)
     * so it can be used later by caller as a method parameter.
     */

    public void initializeV()
      // Sets a default name.
      { 
        setNameStringV( // Change the default name be 
            temporaryNameString  // the original temporary name 
            + ":"  // concatenated with
            + getClass().getSimpleName()  // the name of this class. 
            );
        }
    
    public void initializeV( String nameString )
      // Sets a caller-provided name.
      { 
        setNameStringV( nameString );
        }

    
    // Setters.
    
    public void setNameStringV( String nameString )
      /* Replaces the String representing name of this Object with nameString.
        This should happen once only very shortly after construction.

        This is used for lazy loading of the remainder of the node.
        This is used by the class Outline only.
        */
      {
        Nulls.fastFailNullCheckT(nameString);
        this.nameString= nameString;
        }

    
    // Getters.
    
    public String getNameString()
      /* Returns String representing name of this Object.  */
      {
        return nameString;  // Simply return the name.
        }

    }
