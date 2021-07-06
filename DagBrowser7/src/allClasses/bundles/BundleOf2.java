package allClasses.bundles;

public class BundleOf2<V1,V2> {

  /* This class is simply a bundle of 2 values.
   * It was created for returning multiple-valued results from functions.
   * 
   * Unfortunately it refers to its components by number
   */

  private final V1 theV1;
  private final V2 theV2;

  public BundleOf2(V1 theV1, V2 theV2) // Constructor.
    {
      this.theV1= theV1;
      this.theV2= theV2;
      }

  // Value getters.
  
  public V1 getV1() { return theV1; }

  public V2 getV2() { return theV2; }
  
  }

