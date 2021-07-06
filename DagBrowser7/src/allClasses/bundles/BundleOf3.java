package allClasses.bundles;


class BundleOf3<V1,V2,V3> extends BundleOf2<V1,V2> {
  
  /* This class is simply a bundle of 3 values.
   * It extends BundleOf2.  
   * See BundleOf2 for details.
   */
  
  private final V3 theV3;
  
  public BundleOf3(V1 theV1, V2 theV2, V3 theV3) // Constructor.
    {
      super(theV1, theV2);
      this.theV3= theV3;
      }

  // Value getters.
  
  public V3 getV3() { return theV3; }
  
  }
