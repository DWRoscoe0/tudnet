package allClasses;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class State {

	/*  This class is the base class for all state objects and state-machines.

	  States are hierarchical.  A state can be both:
	  * a sub-state of a larger state-machines 
	  * a state-machine with its own sub-states

		State machines run when their handler methods are called.
		Handler methods produce a success indication, usually a boolean value.
		The exact interpretation of this value depends on the State sub-class
		in which the method appears, but the following is always true:
		* true means that some type of machine progress was made, either:
		  * computational progress was made, or 
		  * one or more inputs were processed, or 
		  * one or more outputs were produced.
		* false means that:
			* no machine progress was made, or
			* progress was made, but was recorded in some other way,
			  probably in a variable field.
		
	  When writing code it is important to distinguish between
	  * the current State, designated by "this", and
	  * its current sub-state, or child state, 
	    designated by "this.subState".

		These classes presently support the processing of only asynchronous inputs, 
		variable values which can change at any time.

		//// This state and its subclasses AndState and OrState
		  do not [yet] provide 
		  * behavioral inheritance, which is
		  	the most important benefit of hierarchical state machines.
		* //// Maybe merge AndState and OrState into State?
	  */

	public static State nullState= new State();
	
	protected State parentState= null;

  protected List<State> theListOfSubStates=
      new ArrayList<State>(); // Initially empty list.

  protected String registeredInputString; // Stores an input stream event word,
    // until a state handler has had the opportunity to check it.

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
	    by state subclasses that require entry actions.
	    This is not the same as initializingV(),
	    which does actions needed when the State object is being built.
	    */
	  { 
			}
	
	public void stateHandlerV() throws IOException

	  /* A state class overrides either this method or stateHandlerB()
	    as part of how it controls its behavior.
	    Overriding this method instead of stateHandlerB() can result in
	    more compact code.  An override like this:

				public void stateHandlerV() throws IOException
				  { 
				    some-code;
				    }

			is a more compact version of, but equivalent to, this:

				public boolean stateHandlerB() throws IOException
				  { 
				    some-code;
				    return false;
				    }

	   */

	  { }
	
	public boolean stateHandlerB() throws IOException
	  /* A state class overrides either this method or stateHandlerV()
	    to control how its state is handled.
	    This method does nothing except return false unless overridden.
	    All overridden versions of this method should return 
	    * true to indicate that some computational input-processing progress 
	      was made,
	    * false if no computational progress is made, 
	      or progress was indicated in some other way.
	    To return false without needing to code a return statement,
	    override the stateHandlerV() method instead.
	    */
	  { 
			stateHandlerV();
			return false; 
		  }

	public String getInputString()
	  {
			return registeredInputString;
		  }

	public void setInputV(String inputString)
	  {
		  registeredInputString= inputString; // Storing input in field variable.
		  }
	
	public boolean handleInputB(String inputString) throws IOException
	  /* This method is the state handler when there is a synchronous input.
	    It stores wordString in a field variable and then 
	    calls the regular state handler.
	    
	    The synchronous input might be processed by that regular handler,
	    or a sub-state's regular handler, if it calls tryInputB(String). 
	    Other inputs, specifically asynchronous inputs, might also be processed.
	    If the handler processes the synchronous input then 
	    the stored value is replaced with null
	    and a true is returned, false is returned otherwise.
	    In either case the stored value is replaced by null before returning.
	    */
	  {
		  registeredInputString= inputString; // Register input in field variable.
			stateHandlerB(); // Call regular handler to process it.  Return value 
			  // is ignored because we are interested in String processing.
			boolean successB= // Word was processed if it's now gone. 
					(registeredInputString == null);
			registeredInputString= null; // Unregister input to State machine..
			return successB; // Returning whether input was processed.
		  }
	
	public boolean tryInputB(String testString) throws IOException
	  /* This method tries to process an input string.
	    If inputString is the registered input string then it is consumed
	    and true is returned, otherwise the registered input string
	    is not consumed and false is returned.
	    If true is returned then it is the responsibility of the caller
	    to process other data associated with testString in the input stream.
	   	*/
	  {
			boolean successB= // Comparing registered input to test input. 
					(testString.equals(registeredInputString));
		  if (successB) // Consuming registered input if it matched.
		  	registeredInputString= null;
			return successB; // The result of the test.
		  }

	public void exitV() throws IOException
	  /* This method is called when a state is exited.
	    It does actions needed when a state is exited.
	    This version does nothing.
	    It should be overridden in state subclasses that need exit actions.
	    */
	  { 
			}

	
	/*  Methods which return results from stateHandlerB().  
	  In addition to these methods, stateHandlerB() returns 
	  a boolean value which has its own meaning.
	  They, or the methods which override them,
	  will probably also indicate that computation progress was made.  
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

	}  // State class 

class OrState extends State {

	/*  This class is the base class for all "or" state machine objects.
	  OrState machines have sub-states, but unlike AndStates,
	  only one sub-state can be active at a time.

	  There is no concurrency in an OrState machine, at least not at this level.
	  It sub-states are active one at a time.
		*/

  private State presentSubState= // State machine's state.
  		State.nullState; // Initial default no-op state.
        // null means no state is active.  non-null means that state is active.
  
  private State requestedSubState= null; // Becomes non-null when 
    // state-machine initializes and when machine's requests new state.

  boolean substateProgressB= false; // Handler progress accumulator.
    // This variable accumulates sub-state handler progress, presently from:
    // * the sub-state handlerV() method value,
    // * a synchronous input String was processed,
    // * requestSubStateV(State nextState)
    // It is reset to false after it is aggregated into,
    // and returned as, a stateProgressB value.

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
	    
	    //// Must check for sub-state validity vs. super-state 
	      when behavioral inheritance is added.
      */
	  { 
		  boolean stateProgressB= false;
  		while (true) { // Cycle sub-states until done.
  	  		if  (requestedSubState != null) // Handling state change request.
  	  		  { // Exit present state, enter requested one, and reset request.
  	  			  presentSubState.exitV(); 
	    	  		presentSubState= requestedSubState;
  	  			  presentSubState.enterV(); 
  	  				requestedSubState= null;
			  			}
  	  		presentSubState.setInputV(getInputString());
  	  		  // Store input string, if any, in sub-state.
		  	  if ( presentSubState.stateHandlerB() )
				  	substateProgressB= true;
  	  		if // Detect and record whether synchronous input was consumed.
  	  			( (getInputString() != null) &&
  	  			  (presentSubState.getInputString() == null)
  	  				)
						{ setInputV(null);
							substateProgressB= true;
							}
  	  	  if (!substateProgressB) // Exiting loop if no sub-state progress made. 
  	  	  	break;
	  	  	stateProgressB= true; // Accumulate sub-state progress in state.
		  		substateProgressB= false; // Reset for later use.
	  	  	}
			return stateProgressB; // Returning accumulated state progress result.
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
			substateProgressB= true;  // Force progress because state changed.
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
		  boolean anyscanMadeProgressB= false;
			boolean thisScanMadeProgressB;
  	  do  // Repeat scanning until no sub-machine progresses.
	  	  { // Scan all sub-state-machines once each.
		  		thisScanMadeProgressB= false;
	  	  	for (State subState : theListOfSubStates)
		  	  	{
			  	  	subState.setInputV(getInputString());
		  	  		  // Store input string, if any, in sub-state.
				  	  if ( subState.stateHandlerB() )
						  	thisScanMadeProgressB= true;
		  	  		if // Detect and record whether synchronous input was consumed.
		  	  			( (getInputString() != null) &&
		  	  			  (subState.getInputString() == null)
		  	  				)
								{ setInputV(null);
									thisScanMadeProgressB= true;
									}
		  	  		}
	  	  	if (thisScanMadeProgressB) 
	  	  		anyscanMadeProgressB= true; 
	  	  	} while (thisScanMadeProgressB);
			return anyscanMadeProgressB; 
			}
  
	}  // AndState 
