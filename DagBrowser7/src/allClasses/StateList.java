package allClasses;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class StateList extends MutableList implements Runnable {

	/*  This class is the base class for all state objects and state-machines.

	  States are hierarchical.  A state can be both:
	  * a sub-state of a larger state-machine 
	  * a state-machine with its own sub-states

		State machines run when their handler methods are called.
		Handler methods produce a success indication, usually a boolean value.
		The exact interpretation of this value depends on the StateList sub-class
		in which the method appears, but the following is always true:
		* true means that some type of machine progress was made, either:
		  * computational progress was made, or 
		  * one or more inputs were processed, or 
		  * one or more outputs were produced.
		* false means that:
			* no machine progress was made, or
			* progress was made, but was recorded in some other way,
			  probably in a variable field.

		State machines normally do not wait for things,
		or do other time-consuming activities such as long loops. 
		Their handler methods return quickly, especially if they need input.
		It's okay to break this rule temporarily,
		while private thread code is being translated to state-machine code,
		if it is understood that doing so could disable other parts 
		of the hierarchical state machine.

	  When writing state-machine code it is important to distinguish between
	  * the current State, designated by "this", and
	  * its current sub-state, or child state, designated by "this.subState".

		To reduce boilerplate code, constructor source code has been eliminated.
		There are constructors, but they are the default parameterless constructors.
		Instance variables are initialized using the initializerV(..) method.

		//// StateList and its subclasses AndState and OrState
		  do not [yet] provide behavioral inheritance, which is
		  the most important benefit of hierarchical state machines.
		  Add it?
	  */

	public static StateList nullStateList= new StateList();
	
	protected StateList parentStateList= null;

  protected List<StateList> theListOfSubStateLists=
      new ArrayList<StateList>(); // Initially empty list.

  private String synchronousInputString; /* Stores an input stream event word,
    for the purpose of giving the state handler 
    the opportunity to process it.
    This variable is set to:
    * a non-null reference to the input String by the handler's caller
      immediately before the handler is called to try to process it
    * null 
  	  * by the handler immediately after successfully processing the input, 
        thereby consuming the input
      * by the handler's caller immediately after 
        a handler fails to process the input
    */

	private IOException delayedIOException= null; /* Storage for an exception
	  thrown in a thread, such as a Timer thread which can't handle it,
	  for re-throwing later in a thread that can handle it. 
	  */

	/* Methods used to build state objects. */
  
  public StateList initializeWithIOExceptionStateList() throws IOException
    /* This method initializes this state object, 
      It does actions needed when this state object is being built.
      This is not the same as the entryV() method, which
      does actions needed when the associated state-machine state is entered.

			Like constructors, this method should be called first
			in the sub-class versions of this method.

      This version only calls 
      the no-IOException superclass initializeV() version.
      
      //// Change to return (this) so it can be used as method argument.
  		*/
    {
  	  super.initializeV(); // IOExceptions are not thrown in super-classes.
  	  return this;
    	}

  public void initAndAddStateListV(StateList theSubStateList) throws IOException
    /* This method is used as part of the StateList building process.  It:
      * does an initialization of theSubState using 
        the method initializeWithIOExceptionState(),
      * next it adds theSubState to this state's list of sub-states.
      This can save a little code when adding states that
      can use the default initializer method initializeWithIOExceptionState().
      In cases where initialization with parameters is needed,
      it should be done by doing what this method does but
      using the initializer with parameters instead of the default one.
     	*/
  	{ 
  	  addStateListV(  // Initialize and add theSubState to list of sub-states.
  	  		theSubStateList.initializeWithIOExceptionStateList()
  	  		); // Adding also sets the theSubState's parent state to be this state.
  	  }

  public void addStateListV(StateList theSubStateList)
    /* This method adds one sub-state to this state.
			It part of the StateList building process.  
      It adds theSubState to the state's sub-state list,
      including setting the parent of the sub-state to be this state.
      */
  	{ 
  	  theListOfSubStateLists.add( theSubStateList ); // Add theSubState to
  	    // this state's list of sub-states.
  	  theSubStateList.setParentStateListV( this ); // Store this state as
  	  	// the sub-state's parent state.

  	  addB( theSubStateList ); // Add to this DataNode's list of DataNodes.
  	  }

  public synchronized void finalizeV() throws IOException
    /* This method processes any pending loose ends before shutdown.
      In this case it finalizes each of its sub-states.
      This is not the same as the exitV() method, which
      does actions needed when the associated state-machine state is exited.
      */
	  {
	  	for (StateList subStateList : theListOfSubStateLists)
		  	{
	  			subStateList.finalizeV();
		  		}
      }

	public void setParentStateListV( StateList parentStateList )
	  // Stores parentStateList as the parent state of this state.
		{ this.parentStateList= parentStateList; }

	
	/*  Methods which do actions for previously built StateList objects.  */
	
	public void enterV() throws IOException
	  /* This method is called when 
	    the state associated with this object is entered.
	    This version does nothing, but it should be overridden 
	    by state subclasses that require entry actions.
	    This is not the same as initialize*V(),
	    which does actions needed when the StateList object is being built.
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

		  See stateHandlerB() .
	    */
	  { 
		  // This default version does nothing.
	    }
	
	public boolean stateHandlerB() throws IOException
	  /* A state class overrides either this method or stateHandlerV()
	    to control how its state is handled.

	    This method does nothing except return false unless 
	    it or stateHandlerV() is overridden.
	    All overridden versions of this method should return 
	    * true to indicate that some computational input-processing progress 
	      was made,
	    * false if no computational progress is made, 
	      or progress made was indicated in some other way.
	    To return false without needing to code a return statement,
	    override the stateHandlerV() method instead.
	    
	    A stateHandlerB() method does not return until
	    everything that can possibly be done has been done, meaning:
	    * All available inputs that it can process have been processed.
	    * All outputs that it can produce have been been produced.
	    * All changes to extended state variables have been made.
	    * A request for a state transition to the qualitative state 
	      has been made if it is possible.
	    
	    */
	  { 
			stateHandlerV(); // Call this in case it is overridden instead.
			return false; // This default version returns false.
		  }

	public void delayedExceptionCheckV() throws IOException
		/* 	This method re-throws any delayedIOException that
			might have occurred in a handler running on a Timer thread. 
		  */
		{ 
		  if  // Re-throw any previously saved exception from timer thread.
		    (delayedIOException != null) 
		  	throw delayedIOException;
		  }

	public void delayExceptionV( IOException theIOException )
		/* 	This method records an IOException that 
		  occurred in a handler running on a Timer thread
		  needs to be re-throw later on the normal thread
		  by calling delayedExceptionCheckV(). 
		  */
		{ 
		  delayedIOException= theIOException;
		  }

	protected String getSynchronousInputString()
	  // See handleSynchronousInputB(..).
	  {
			return synchronousInputString;
		  }

	public void setSynchronousInputV(String synchronousInputString)
		// See handleSynchronousInputB(..).
	  {
		  this.synchronousInputString= synchronousInputString;
		  }
	
	public boolean handleSynchronousInputB(String inputString) throws IOException
	  /* This method should be used as the state handler 
	    when there is a synchronous input to be processed.
	    It stores wordString in a field variable and then 
	    calls the regular state handler.
	    The synchronous input might be processed by that regular handler,
	    or a sub-state's regular handler, if it calls tryInputB(String). 
	    Other inputs, specifically asynchronous inputs, might also be processed.
	    
	    If any handler processes the synchronous input then 
	    the stored value is replaced with null and a true is returned.
	    True is also returned if other handler progress is made.
	    False is returned otherwise.
	    In any case the stored value is replaced by null before returning.

		  //// If state machines are used more extensively to process
		    messages of this type, it might make sense to:
		    * Cache the StateList that processes the keyString in a HashMap
		      and use the HashMap to dispatch the message.
		    * Add discrete event processing to State machines and
		      make these keyString messages a subclass of those events.
	    */
	  {
		  synchronousInputString= inputString; // Store input in field variable.
			boolean successB=  // Call regular handler to process it.  Return value
					stateHandlerB(); 
			  // is ignored because we are interested in String processing.
			successB|= // Combine success with result of synchronous input processing. 
					(synchronousInputString == null);
			synchronousInputString= null; // Remove input to from field variable.
			return successB; // Returning whether input was processed.
		  }
	
	public boolean tryInputB(String testString) throws IOException
	  /* This method tries to process a synchronous input string.
	    If inputString is the stored input string then it is consumed
	    and true is returned, otherwise the stored input string
	    is not consumed and false is returned.
	    If true is returned then it is the responsibility of the caller
	    to process other data associated with testString in the input stream.
	   	*/
	  {
			boolean successB= // Comparing requested synchronous input to test input. 
					(testString.equals(synchronousInputString));
		  if (successB) // Consuming stored input if it matched.
		  	synchronousInputString= null;
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

	protected void requestStateListV(StateList nextStateList)
		/* This is called to change the state of a the state machine.
		  to the sibling state nextStateList.  It does this by 
		  calling the parent state's setNextSubStateListV(..) method. 
		  Its affect is not immediate.
		  */
		{
			parentStateList.requestSubStateListV(nextStateList);
			}
	
	protected void requestSubStateListV(StateList nextStateList)
	  /* This method provides a link between setNextStateListV(StateList)
	    and OrState.setNextSubStateListV(StateList).
	    It is meaningful only in the OrState class in which
	    it is used to select a new active sub-state.
	    In the AndState class, all sub-states are active at the same time.
	    It is overridden by the OrState class.
	    It has no meaning when inherited by other classes and throws an Error.
	    This method could be eliminated if setNextStateV(StateList)
	    casted parentStateList to an OrState.
	    */
	  {
			throw new Error("requestSubStateListV(..) not overridden.");
			}
	
	public void run()
	  /* This method is run by TimerInput to run 
	    the stateHandlerB() method when the timer is triggered.
	    If an IOException occurs then it is saved for processing later by
	    a non-Timer thread.
	    */
		{
			try { 
				stateHandlerB(); // Try to process timer event with state handler. 
				}
		  catch ( IOException theIOException) { 
		    delayExceptionV( // Postpone exception processing to other thread.
		    		theIOException
		    		); 
		    }
		}

	}  // StateList class 

class OrState extends StateList {

	/*  This class is the base class for all "or" state machine objects.
	  OrState machines have sub-states, but unlike AndStates,
	  only one sub-state can be active at a time.

	  There is no concurrency in an OrState machine, at least not at this level.
	  Its sub-states are active one at a time.
		*/

  private StateList presentSubStateList= // Machine's qualitative state.
  		StateList.nullStateList; // Initial default no-op state.

  private StateList requestedSubStateList= null; // Becomes non-null when 
  	// machine requests a new state, even if the same as the present state.

  boolean substateProgressB= false; // Handler progress accumulator.
    // This variable accumulates sub-state handler progress, 
    // presently the "or" of:
    // * the sub-state stateHandlerB() function value,
    // * if a synchronous input String was processed,
    // * if requestSubStateV(StateList nextState) was called
    // This variable is reset to false after it is aggregated into,
    // and returned as, a stateHandlerB() stateProgressB value.

  public boolean stateHandlerB() throws IOException
	  /* This handles the OrState by cycling it's machine.
	    It does this by calling the handler method 
	    associated with the present state-machine's sub-state.
      The sub-state might change with each of these calls.
      It keeps calling sub-state handlers until no computation progress is made.
      It returns true if computational progress was made by at least one state.
	    It returns false otherwise.
	    Each sub-state handler gets a chance to process the synchronous input,
	    if one is available, until it is consumed. 
	    
	    //// Must check for sub-state validity vs. super-state 
	      when behavioral inheritance is added.
      */
	  { 
  		delayedExceptionCheckV(); // Throw exception if one was saved.
	  	boolean stateProgressB= false;
  		while (true) { // Cycle sub-states until done.
  	  		if  (requestedSubStateList != null) // Handling state change request.
  	  		  { // Exit present state, enter requested one, and reset request.
  	  			  presentSubStateList.exitV(); 
	    	  		presentSubStateList= requestedSubStateList;
  	  			  presentSubStateList.enterV(); 
  	  				requestedSubStateList= null;
			  			}
  	  		presentSubStateList.setSynchronousInputV(getSynchronousInputString());
  	  		  // Store input string, if any, in sub-state.
		  	  if ( presentSubStateList.stateHandlerB() )
				  	substateProgressB= true;
  	  		if // Detect and record whether synchronous input was consumed.
  	  			( (getSynchronousInputString() != null) &&
  	  			  (presentSubStateList.getSynchronousInputString() == null)
  	  				)
						{ setSynchronousInputV(null);
							substateProgressB= true;
							}
  	  	  if (!substateProgressB) // Exiting loop if no sub-state progress made. 
  	  	  	break;
	  	  	stateProgressB= true; // Accumulate sub-state progress in this state.
		  		substateProgressB= false; // Reset for later use.
	  	  	} // while
			return stateProgressB; // Returning accumulated state progress result.
			}
		
  protected void requestSubStateListV(StateList nextStateList)
	  /* This method requests the next state-machine state,
	    which is the same thing as this state's next sub-state.
	    It overrides StateList.requestSubStateV(StateList nextState) to work.
	    
	    Note, though the requestedSubStateList variable changes immediately,
	    control is not transfered to the new sub-state until
	    the handler of the present sub-state exits.
	    */
	  {
  	  requestedSubStateList= nextStateList;
			substateProgressB= true;  // Force state change as sub-state progress.
			}
  
	}  // OrState 

class AndState extends StateList {

	/* This class is the base class for all "and" state machine objects.
	  AndState machines have sub-states, but unlike OrStates,
	  all AndState sub-states are active at the same time.
	  There is concurrency in an AndState machine, at least at this level.
	  */

  public StateList initializeWithIOExceptionStateList() throws IOException
    {
  		super.initializeWithIOExceptionStateList();
  		return this;
    	}

	public boolean stateHandlerB() throws IOException
	  /* This method handles this AndState by cycling all of its sub-machines
	    until none of them makes any computational progress.
      It keeps going until all sub-state handlers return false,
      indicating that they have stopped reporting progress.
	    This method returns true if any computational progress was made
	    in any sub-state, false otherwise.

    	//// rewrite loops for faster exit for 
    	 	minimum sub-state stateHandlerB() calls.
	    */
	  { 
			delayedExceptionCheckV(); // Throw exception if one was saved.
		  boolean anyscanMadeProgressB= false;
			boolean thisScanMadeProgressB;
  	  do  // Repeat scanning until no sub-machine progresses.
	  	  { // Scan all sub-state-machines once each.
		  		thisScanMadeProgressB= false;
	  	  	for (StateList subStateList : theListOfSubStateLists)
		  	  	{
			  	  	subStateList.setSynchronousInputV(getSynchronousInputString());
		  	  		  // Store input string, if any, in sub-state.
				  	  if ( subStateList.stateHandlerB() )
						  	thisScanMadeProgressB= true;
		  	  		if // Detect and record whether synchronous input was consumed.
		  	  			( (getSynchronousInputString() != null) &&
		  	  			  (subStateList.getSynchronousInputString() == null)
		  	  				)
								{ setSynchronousInputV(null);
									thisScanMadeProgressB= true;
									}
		  	  		}
	  	  	if (thisScanMadeProgressB) 
	  	  		anyscanMadeProgressB= true; 
	  	  	} while (thisScanMadeProgressB);
			return anyscanMadeProgressB; 
			}

	}  // AndState 
