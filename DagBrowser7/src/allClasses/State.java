package allClasses;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class State {

	/*  This class is the base class for all state objects.

	  States are hierarchical, meaning that
	  states may have child states, or sub-states.
	  Sub-states may themselves be state machines 
	  with their own sub-states.

	  When writing code it is important to distinguish between
	  * the current State, designated by "this", and
	  * its current sub-state, or child state, 
	    designated by "this.subState".

		//// This state and its subclasses AndState and OrState
		  do not [yet] provide 
		  * It does not provide behavioral inheritance, which is
		  	the most important benefit of hierarchical state machines.
		  * It does not handle synchronous inputs, aka events.
		    It handles only asynchronous inputs, variable values.
		  * //// Maybe merge AndState and OrState into State?
	  */

	protected State parentState= null;

  protected List<State> theListOfSubStates=
      new ArrayList<State>(); // Initially empty list.

  
  /* Methods used to build state objects. */
  
  public void initializingV() throws IOException
    /* This method initializes this state object, 
      It does actions needed when this state object is being bult.
      This is not the same as the entryV() method, which
      does actions needed when the associated state-machine state is entered.

      This version does nothing.
      It does not recursively initialize the sub-states because
      typically that is done by initAndAddV(..) as sub-states are added.
  		*/
    {
    	}

  public void initAndAddV(State theSubState) throws IOException
    /* This method is used as part of the State building process.  It:
      * adds theSubState this state's list of sub-states, and 
      * does an initialize of theSubState.
      It does them in this order because initialization needs to know 
      the parent state.
     	*/
  	{ 
  	  addV( theSubState ); // Add theSubState to list of sub-states.
  	    // This also sets the theSubState's parent state to be this state.

  		theSubState.initializingV(); // Initializing the added sub-state.
  	  }

  public void addV(State theSubState)
    /* This method adds/injects one sub-state to this state.
			It part of the State building process.  
      It adds theSubState to the state's sub-state list.
      It also sets the parent of the sub-state to be this state.
      */
  	{ 
  	  theListOfSubStates.add( theSubState ); // Add theSubState to
  	    // this state's list of sub-states.

  	  theSubState.setParentStateV( this ); // Store this state as
  	    // the sub-state's parent state.
  	  }

  public synchronized void finalizingV() throws IOException
    /* This method processes any pending loose ends before shutdown.
      In this case it finalizes each of its sub-states.
      This is not the same as the exitV() method, which
      does actions needed when the associated state-machine state is exited.
      */
	  {
	  	for (State subState : theListOfSubStates)
		  	{
	  			subState.finalizingV();
		  		}
      }

	public void setParentStateV( State parentState )
	  // Stores parentState as the parent state of this state.
		{ this.parentState= parentState; }

	
	/*  Methods which do actions for previously built State objects.  */
	
	public void enterV() throws IOException
	  /* This method is called when 
	    the state associated with this object is entered.
	    This version does nothing, but it should be overridden 
	    by subclasses that require entry actions.
	    This is not the same as initializingV(),
	    which does actions needed when the State object is being built.
	    */
	  { 
			}
	
	public boolean stateHandlerB() throws IOException
	  /* This is the default state handler. 
	    It does nothing because this is the superclass of all States.
	    All versions of this method should return 
	    * true to indicate that some computational input-processing progress 
	      was made,
	    * false otherwise.

	    This version does nothing and returns false because 
	    this is the superclass of all States.
	    */
	  { 
		  return false; 
		  }

	public void exitV() throws IOException
	  /* This method is called when a state is exited.
	    It does actions needed when a state is exited.
	    This version does nothing.
	    It should be overridden in subclasses that need exit actions.
	    */
	  { 
			}

	
	/*  Methods which return results from stateHandlerB().  
	  In addition to these methods, stateHandlerB() returns 
	  a boolean value which has its own meaning  
	  */

	protected void requestStateV(State nextState)
		/* This is called to change the state of a the state machine.
		  to the sibling state nextState.
		  It does this by calling the parent state's setNextSubStateV(..) method. 
		  Its affect is not immediate.
		  */
		{
			parentState.requestSubStateV(nextState);
			}
	
	protected void requestSubStateV(State nextState)
	  /* This method provides a link between setNextStateV(State)
	    and OrState.setNextSubStateV(State).
	    It is meaningful only in the OrState class,
	    and is meant to be overridden by the OrState class.
	    It has no meaning when inherited by other classes and throws an Error.
	    This method could be eliminated if setNextStateV(State)
	    casted parentState to an OrState.
	    */
	  {
			throw new Error();
			}

	protected void setNoProgressInParentV()
		{
			((OrState)parentState).setNoProgressV();
			}

	}  // State class 

class OrState extends State {

	/*  This class is the base class for all "or" state machine objects.
	  OrState machines have sub-states, but unlike AndStates,
	  only one sub-state can be active at a time.

	  There is no concurrency in an OrState machine, at least not at this level.
	  It sub-states are active one at a time.
		*/

  private State presentSubState= // State machine's state.
  		null; //// new State(); // Initial default no-op state.
        // null means no state is active.  non-null means that state is active.
  
  private State requestedSubState= null; // Becomes non-null when 
    // state-machine initializes and when machine's requests new state.
    //// ? Change to: null means deactivate state?  non-null means go to state.
  
	private boolean handlerRecordedProgressB= true; // setNoProgressV() clears.
	  // true means progress was made and recorded, false means waiting.

	private boolean handlerReturnedProgressB= true; // Returned by handlerB().
		// Waiting for input, false, is the exception.

	public boolean stateHandlerB() throws IOException
	  /* This handles the OrState by cycling it's machine.
	    It does this by calling the handler method 
	    associated with the present state-machine's sub-state.
      The sub-state might change with each of these calls.
      It keeps calling sub-state handlers until a handler returns false,
      indicating that it has processed all available inputs
      and is waiting for new input.
	    It returns true if any computational progress was made,
	    false otherwise.
	    
	    This method manipulates handlerRecordedProgressB in a way that combines
	    input from setNoProgress() and stateHandlerB() return values.
      */
	  { 
		  boolean anyProgressMadeB= false;
  		while (true) { // Cycle sub-states until done.
  	  		if  // Handling state change, if any, by doing exit and entry.
  	  		  (requestedSubState != null)
  	  			{ // Exit present state and enter requested one.
  	  			  if (presentSubState != null)
  	  			  	presentSubState.exitV(); 
	    	  		presentSubState= requestedSubState;
	    	  		//// Should check for sub-state validity vs. super-state here.
  	  			  if (presentSubState != null)
  	  			  	presentSubState.enterV(); 
			  			}
	  	  	{ // Calling handler and combining its results.
			  		handlerRecordedProgressB= true; // Assume progress.
			  		requestedSubState= null; // Preparing for transition request.
	  			  if (presentSubState != null) // Calling handler if state active.
	  			  	handlerReturnedProgressB= presentSubState.stateHandlerB();
	  	  		// handlerReturnedProgressB is needed so &= uses correct values.
	  	  		// Is this true if handlerRecordedProgressB is volatile?
	  	  		handlerRecordedProgressB &= handlerReturnedProgressB;
	  	  		}
	  	  	anyProgressMadeB |= handlerRecordedProgressB;
  	  	  if (!handlerRecordedProgressB) // Exiting loop if no progress made. 
  	  	  	break;
	  	  	}
			return anyProgressMadeB; // Returning overall progress result.
			}

	protected void setNoProgressV()
	  /* This is an alternate way for stateHandlerB() to indicate
	    that it made no progress and is waiting for new input.
	    The other way is for stateHandlerB() to return false.
	    */
	  {
	  	handlerRecordedProgressB= false;
			}
		
  protected void requestSubStateV(State nextState)
	  /* This method sets the state-machine state,
	    which is the same as this state's sub-state.
	    It overrides State.requestSubStateV(State nextState) to work.
	    
	    Note, though the requestedSubState variable changes immediately,
	    control is not transfered to the new sub-state until
	    the handler of the present sub-state exits.
	    */
	  {
  	  requestedSubState= nextState;
			}

	}  // OrState 

class AndState extends State {

	/* This class is the base class for all "and" state machine objects.
	  AndState machines have sub-states, but unlike OrStates,
	  all sub-states are active at the same time.
	
	  There is concurrency in an AndState machine, at least at this level.
	  */

	public boolean stateHandlerB() throws IOException
	  /* This method handles this AndState by cycling all of its sub-machines
	    until none of them makes any computational progress.
      It keeps going until all sub-state handlers return false,
      indicating that they have processed all available inputs
      and are waiting for new input.
	    This method returns true if any computational progress was made, 
	    false otherwise.
	    
    	//// rewrite loops for faster exit for 
    	 	minimum sub-state stateHandlerB() calls.
	    */
	  { 
		  boolean anythigMadeProgressB= false;
			boolean cycleMadeProgressB;
  	  do  // Cycling until no sub-machine progresses.
	  	  { // Cycle all sub-state-machines once each.
		  		cycleMadeProgressB= // Will be true if any machine progressed. 
		  				false;
	  	  	for (State subState : theListOfSubStates)
		  	  	{
	  	  		  boolean handlerMadeProgressB= subState.stateHandlerB(); 
		  	  		cycleMadeProgressB|= handlerMadeProgressB; 
		  	  		}
  	  		anythigMadeProgressB|= cycleMadeProgressB; 
	  	  	} while (cycleMadeProgressB);
			return anythigMadeProgressB; 
			}
  
	}  // AndState 
