package allClasses;

public class NamedDataNode 

  extends DataNode 

  /* This class is the base class for all DataNodes with a stored name.
    It adds a name field and methods to manipulate that name
    to its superclass, DataNode.  

    This class defines only a 0-argument constructor.
    It relies on setter methods for other initialization.
    Their names usually include the word "initialize". 
    Initialization is done this way for the following reasons:
    * Some subclasses, such as StateList, do their initialization this way
      to reduce boilerplate code.
    * Some subclasses don't actually know their names until 
      long after construction, for example, 
      during the lazy loading of class instances such as Outline. 
    */

  {
    
    private String nameString; // Storage for the set-able name of this node. 

    public NamedDataNode() // Constructor.
      /* This constructor has no parameters because 
       * non-default initialization is done with setter methods.
       */
      {
        doSetNameV( // Set default name to be 
            NamedDataNode.class.getSimpleName() 
              + '@' 
              + Integer.toHexString(((Object)this).hashCode())
            );
        }
      
    /* Initialization/setter methods.
     * 
     * ///? Change all of these methods to return (this)
     * so it can be used later by caller as a method parameter.
     */
    
    public void setNameV( String nameString )
      // Sets a caller-provided name.
      { 
        doSetNameV( nameString );
        }
    
    private void doSetNameV( String nameString )
      /* Replaces the String representing name of this Object with nameString.

        Normally this is used shortly after construction,
        as part of initialization, but in some cases, as with the class Outline,
        it might happen later as part of lazy loading of the node.
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
