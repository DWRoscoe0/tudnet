package allClasses;

public interface LongLike

  /* This is used when method access to a long value is needed.
    Maybe break into a ReadableLongLike and WriteableLongLike?
    */

  {
    public long getValueL( );
    public long addDeltaL( long deltaL );
    public long setValueL( final long newL );
    }
