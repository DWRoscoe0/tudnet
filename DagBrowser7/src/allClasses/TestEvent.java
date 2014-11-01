package allClasses;

import java.util.EventObject;

public class TestEvent extends EventObject {

  /* This class is an Event that can be used to
    perform operation like most other events.
    But it can be used to only test whether
    such execution is legal given other event fields.
    The legality status is passed via field legalB
    and its access methods.
    */

  private boolean doB= true;  // true means execution is requested.
    // false means only return whether execution requested is legal.

  private boolean legalB= true;  // true means request is legal.

	public TestEvent(Object inSourceObject)  // Constructor.
	{
	  super(inSourceObject);
	  }

	public TestEvent(Object inSourceObject, boolean inDoB)  // Constructor.
	{
	  this(inSourceObject);
    doB= inDoB;
	  }

  public void setDoV( boolean inDoB )
    {
      doB= inDoB;
      }

  public boolean getDoB( )
    {
      return doB;
      }

  public void setLegalV( boolean inLegalB )
    {
      legalB= inLegalB;
      }
    
  public boolean getLegalB( )
    {
      return legalB;
      }

  }
