package allClasses;

import static allClasses.Globals.appLogger;
import static allClasses.AppLog.LogLevel.*;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class StateList extends MutableList implements Runnable {

	/*  This class is the base class for all state objects and state-machines.

	  States are hierarchical.  A state can be both:
	  * a sub-state of a larger state-machine 
	  * a state-machine with its own sub-states

    Classes in this file:
    
    This file defines several state-machine classes:
    * StateList: This is the base class of all states.
      It is also typically used as machine states with no children.
    * SentinelState: This class is used as an initial sentinel-state 
      for OrState state machines to reduce the number of null checks needed.
		* AndOrState: a superclass of the OrState and AndState classes.
    * OrState: OrState machines have sub-states, only one of which 
      can be active at a time.  A state-machine based on this class
      behaves like a classical finite state machine,
      unless some of it sub-states have sub-states of their own.
    * AndState: AndState machines also have sub-states, but unlike OrStates,
	  	all AndState sub-states are active at the same time.
	  	There is concurrency in an AndState machine, at least at this level.


		Threads:

		State machines don't have threads of their own. 
		State machine code is executed by external threads.
		At least one thread calls state-machine handler methods when it has
		something for the machine to do, 
		which is usually to process a new input which has become available.
		There might be many threads calling the handler methods of a state-machine,
		but state-machine handler methods are synchronized,
		so only one thread may execute state handler code at one time.
		Typically one thread receives one or more different types of 
		ordinary inputs and passes them to an associated state-machine handler,
		and one or more additional threads are used to deliver 
		inputs triggered by timers.

		State machines normally do not wait for things, except for the next event.
		or do other time-consuming activities such as executing long loops. 
		Their handler methods must return quickly, because
		not doing so could disable other parts of the hierarchical state machine.
		It's okay to break this rule temporarily, but only while 
		thread code is being translated to state-machine code.


    Handler Methods:

		State machines run when their handler methods are called.
		A handler's main job is to process events.
		In doing so they might change the machine's state
		or produce some output, or both. 
   	
   	There are two sets of state-machine handler methods:
   	* Final handler methods are the methods called to execute 
   	  a state's code.  These may not be overridden.
   	* Override handler methods may be overridden by state subclasses.
   	  These methods are called by the non-override-able final handler methods.


    Major states: 

    A hierarchical state machine has two major states.  They are:
    * Waiting for the next input.
    * Processing inputs.  It processes events until there are no more.


		Signals:

    Signals carry information between parts of the hierarchical state machine,
    and between the state machine and the external world.
    Signals can be of various types.

	  * Continuous time vs. discrete time:
	  	* A continuous time signal can change at any time,
	  	  like the value of a Java variable.
	  	  The only way to know it has changed is to examine it.
	  	  A continuous time signal can change at any time.
	  	  It's possible not all changes will be seen,
	  	  depending on when and how often it is examined.
	  	  An example of a continuous time signal is a thermometer reading.
	  	* A discrete time signal is a sequence of data values
	  	  that become available at at known times.
	  	  Every datum is accompanied by a notification of its arrival.
	  	  If makes no sense to read it more than once per notification.
	  	  An example of a discrete time signal is 
	  	  a stream of email messages arriving in an In-Box.

    * Input signals vs. output signals: 
    	* An input signal is one that is produced outside of a machine,
    	  and consumed inside of the machine.
	 		* An output signal is one that is produced inside of a machine,
    	  and consumed outside of the machine.
    	This property is contextual.  A signal which is
    	an output of one machine might be an input to another machine.

		
    Signals of special interest to these state machines:
  	* A state change request: 
  	  A state machine can make this request to its parent state machine.
  	  The parent responds by changing it state to the sub-state requested.
    * A timer being triggered.  
      The affected state handler will take an appropriate action.
    * Discrete data objects.  These can be:
      * queued objects such as received network packets,
    	* strings or other objects parsed from those packets, 
    	* bytes in those strings.
    	An affected state handler will take an appropriate action.
    	Very possible it will read additional data from the same input stream
    	and act on the entire sequence as a whole.
    * Handler return codes.  See the additional information below about
      handler methods and return code.


		Handler methods and their protocol (return status values):

		The purpose of a state machine handler method 
		is to process one or more inputs to its state machine.
		A state machine's handler method is called whenever it is possible that 
		one or more of the state machine's inputs has changed or appeared.
		The processing might include internal state transitions.
		passing of signals between internal sub-machines,
		and/or the production of one or more outputs.

		The handler method should not return until
		it has processed all available state machine inputs,
		which means that, for the moment, 
		there is nothing remaining for it to do.

		It's possible that the handler was called by 
		the handler of another state machine, the parent state machine.
		There are several ways that a handler 
		can pass information back to the parent machine.
		* state change request: This is a signal from the state machine
		  to its parent OrState machine.  The parent responds by
		  changing its sub-state to the requested child sub-state.
		* discrete input consumption: There are two cases:
		  * If the state's handler accepts and processes a discrete input, 
		    it clears the input register variable.
		    In this case the caller does not try to pass the input
		    to another sub-state's handler.
		  * If the state's handler does not accept and process a discrete input, 
		    the input datum remains in the state's input register variable.
		    In this case the caller moves the datum to
		    the input register of another sub-state and calls its handler,
		    or if there is no other sub-state then 
		    it might try to process the discrete input itself.
		* return code:  A handler may return a boolean value, 
		  with the following meanings: 
		  * false: No continuous outputs changes happened.
		    Either all continuous signal changes were internal, or there were none.
		    So there is no need for the caller to activate a sibling state handler
		    or do any other processing itself.
		  * true: Possible continuous outputs were produced.
		    So the caller should call sibling sub-state handlers,
		    to check for possible processing of those outputs itself.
		    If it is unable to completely process the outputs then
		    it should return true to its caller to continue processing.


		Exception Handling:

		It is common for a state-machine to receive input from InputStreams,
		and InputStreams produce IOExceptions.  These exceptions must be handled.
		* Most state-machine handler methods handle IOExceptions by 
			declaring that they may throw them.
			But not all threads that execute state-machine code can handle exceptions.
			An example of this is the Timer thread.
			To accommodate such threads, methods are provided that,
			instead of throwing an IOException, they record the IOException,
			so it can be re-thrown later by a different thread that can handle it.
		* Originally it was thought that IOExceptions must be handled
		  when a state object is constructed, but this is difficult to do.
		  If initialization is moved from constructors to initialization methods
		  the handling of exception becomes more manageable.
		  initializeWithIOExceptionStateList() was created for this purpose.
		  However it might be possible to eliminate this eventually.

	  When writing state-machine code it is important to distinguish between
	  * the current State, designated by "this", and
	  * its current sub-state, or child state, 
	    designated by "this.presentSubStateList".

		To reduce boilerplate code in low-level state machine states, 
		constructor source code has been eliminated.
		There are constructors, but they are the default parameterless constructors.
		Instance variables are initialized using various initialize methods.

		///enh StateList and its subclasses AndState and OrState
		  do not [yet] provide behavioral inheritance, which is
		  the most important benefit of hierarchical state machines.
		  Add it?

		///opt Presently a discrete input is never passed up from a state
		  unless it was passed down and went unprocessed.
		  This can be inefficient if a nested state processes much input
		  because it must be passed down through its ancestors first.
		  Maybe discrete input can be made to originate in any state
		  and be passed either up or down as conditions require it.
		  This would assume a model in which input can be consumed by
		  any state designed to handle it.
		  An extra input loop could be added to any state that inputs a lot.
		  Maybe an input looping method could be added to StateList.

		  The only complication I see is synchronized sections.
		  A message loop would use the timer associated with 
		  a LockAndSignal instance.  Normally the wait() unlocks
		  to allow entry to another synchronized method containing a notify() call.  
		  Unfortunately this would allow entry by other threads also.
		  Maybe for those classes, the timer thread could call notify().

	  */

	
	/* Variables used for all states.  */

	protected StateList parentStateList= null; // Our parent state.
	  ///opt It might be worthwhile combining this field 
	  // with DataNode.parentNamedList, but we would need a way to
	  // identify State children during state operations.
	  //
	  ///opt To eliminate null checks, make this be be a Sentinel state.

	protected Color theColor= UIColor.initializerStateColor;

  protected List<StateList> theListOfSubStateLists= // Our sub-states.
      new ArrayList<StateList>(); // Initially empty list.

  private String discreteInputString; /* Temporarily stores an input event.
    It is the one place this state checks for discrete input.
    Adiscrete input can appear in any number of other variables.
    This variable is special and is set according to the following protocol:
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

	/* Variables used only for OrState behavior.. */

	// Sentinel states which can simplify other code by eliminating null checks.
  // Sentinel states are used by OrState machines. 
	protected static final SentinelState initialSentinelState;
	///elim protected static SentinelState finalSentinelState= new SentinelState();

	static { 
		initialSentinelState= new SentinelState(); 
		initialSentinelState.initializeV();
		}
	
  protected StateList presentSubStateList= null; /* Machine's qualitative state.
        This is used to select between and-state and or-state behavior
        This will be null when the state is behaving as an and-state.
        When it is not null then is the or-state's active sub-state. 
        See AndOrState.
        */

  protected StateList requestedSubStateList= null; /* Becomes non-null when 
  	machine requests a new qualitative state.  
    It could become the present state, 
    which would cause state exit and re-entry.
    */

  private Boolean isActiveBoolean= null; // Activity value, Initially invalid. 

	/* Methods used to build state objects. */

  public StateList initializeWithIOExceptionStateList() throws IOException
    /* This method can also be used to initialize this state object.
      It calls initializeV() and it also returns a reference to this StateList.
      This method difference from ordinary initialization methods because:
      * It throws an IOException and makes this clear in its name.
      * It returns a reference to this object which can be used to
        simplifier code in the caller.

      ///elim? This method throws IOException.
        It was originally done for compatibility with its superclass
        or because it called a method that threw IOException.
        Now this is apparently no longer true, and
        many of its callers also do not throw IOException.
        If this continues to be true as this app matures,
        remove the throwing of IOException and the inclusion in 
        the method names. 
     	*/
	  {
  	  initializeV(); // Use the other initialization method to do the work.
  	  return this;
	  	}

  public void initializeV()
    /* This method initializes this state object and its superclasses.
      It does actions needed when this state object is being built.
      This is not the same as the entryV() method, which
      does actions needed when the associated state-machine state is entered.

			Like constructors, this method should be called first
			from the sub-class versions of this method.
  		*/
    {
  	  super.initializeV();
  	  theColor= UIColor.initialStateColor;
    	}

  public void initAndAddStateListV(StateList theSubStateList) 
      throws IOException
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
  	  addStateListV( theSubStateList.initializeWithIOExceptionStateList() );
  	  }

  public void addStateListV(StateList theSubStateList)
    /* This method adds one sub-state to this state.
			It is part of the StateList building process.  
      It adds theSubState to the state's sub-state list,
      including setting the parent of the sub-state to be this state.
      */
  	{ 
  	  theListOfSubStateLists.add( theSubStateList ); // Add theSubState to
  	    // this state's list of sub-states.
  	  theSubStateList.setParentStateListV( this ); // Store this state as
  	  	// the sub-state's parent state.

  	  addAtEndB( theSubStateList ); // Add to this DataNode's list of DataNodes.
  	  }

  public synchronized void finalizeV() throws IOException
    /* This method processes any pending loose ends before shutdown.
      In this case it finalizes each of its sub-states.
      This is not the same as the onExitV() method, which
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

  public StateList getpresentSubStateList()
    /* This method returns the present machine substate.
      It is meaningful only for OrStates.
      */
  	{ 
  		return presentSubStateList; 
  		}

	
	/*  Methods for AndState behavior.  */

	public synchronized boolean andStateOnInputsB() throws IOException
	  /* This method handles AndState and 
	    state-machines that want to behave like an AndState machine 
	    by cycling all of their sub-machines
	    until none of them makes any computational progress.
	    It scans the sub-machines in order until one makes computational progress.
	    Then it restarts the scan.
	    It is done this way to prioritized the sub-machines.
	    If one sub-machine produces something for which an earlier machine waits,
	    then that earlier machine can be run next.
      It keeps scanning until none of the sub-machines in a scan makes progress.
	    This method returns true if any computational progress was made
	    by any sub-machine, false otherwise.
	    */
	  { 
			throwDelayedExceptionV(); // Throw exception if one was saved.
		  boolean progressMadeB= false;
  	  substateScansUntilNoProgress: while(true) {
 	  		for (StateList subStateList : theListOfSubStateLists) 
	  	  	if (doOnInputsToSubstateB(subStateList))
		  	  	{
		  	  		progressMadeB= true;
		  	  		continue substateScansUntilNoProgress; // Restart scan.
		  	  		}
		  	  	else
		  				setBackgroundColorV( UIColor.runnableStateColor );
 	  		break substateScansUntilNoProgress; // No progress in this, final scan.
  	  	} // substateScansUntilNoProgress:
			return progressMadeB; 
			}


	/* Methods for implementing OrState behavior.. */

  public synchronized boolean orStateOnInputsB() throws IOException
	  /* This handles the OrState by cycling it's machine.
	    It does this by calling the handler method 
	    associated with the present state-machine's sub-state.
      The sub-state might change with each of these calls.
      It calls sub-state handlers until no computational progress is made.
      It returns true if at least one sub-state made computational progress.
	    It returns false otherwise.
	    Each sub-state handler gets a chance to process the discrete input,
	    if one is available, until it is consumed. 
	    
	    ///enh Must check for sub-state validity vs. super-state 
	      when and if behavioral inheritance is added.
      */
	  { 
			throwDelayedExceptionV(); // Throw exception if one was saved.
	  	boolean stateProgressB= false;
			while (true) { // Cycle sub-states until done.
	  	    boolean substateProgressB= 
	  	    		doOnInputsToSubstateB(presentSubStateList);
		  		if (requestedSubStateList != null) // Handling state change request.
		  		  { presentSubStateList.onExitV();
		  		  	presentSubStateList.invalidateActiveV();
	    	  		presentSubStateList= requestedSubStateList; // Change sub-state.
		  		  	presentSubStateList.invalidateActiveV();
		  				requestedSubStateList= null;
		  			  ///dbg  appLogger.debug(
			  			///dbg  		"StateList.orStateOnInputsB() entering"
			  			///dbg  	+ presentSubStateList.getFormattedStatePathString()
			  			///dbg  	);
		  			  presentSubStateList.doOnEntryV();
							substateProgressB= true; // Count this as progress.
			  			}
		  	  if (!substateProgressB) // Exiting loop if no sub-state progress made.
			  	  { presentSubStateList.setBackgroundColorV( 
			  	  		UIColor.waitingStateColor 
			  	  		);
			  	  	break;
			  	  	}
	  	  	stateProgressB= true; // Accumulate sub-state progress in this state.
	  	  	} // while (true)
			return stateProgressB; // Returning accumulated state progress result.
			}

	protected void requestSiblingStateListV(StateList requestedStateList)
		/* This is called to change the state of a the state machine
		  that contains this state to the sibling state requestedStateList.  
		  It does this by calling the parent state's 
		  setNextSubStateListV(..) method. 
		  It does not fully take affect until the present state handler exits.
		  */
		{
			parentStateList.requestSubStateListV(requestedStateList);
			}

  protected boolean requestSubStateListB(StateList requestedStateList)
	  /* This method does the same as requestSubStateListV(..) except that
	    if will reject the state=change request and return false if 
	    the present sub-state is the same as requestedStateList. 
	    In this case the sub-state will not be exited and reentered.
	    This method will return true if the request is accepted.
	    */
	  {
  	  boolean progressB= // Calculate whether the state will actually change. 
  	  		( presentSubStateList != requestedStateList );
	  	if ( progressB ) // If it will
	  		requestSubStateListV(requestedStateList); // call this to do the work.
	  	return progressB; // Return whether we accomplished anything.
	  }	

  protected void requestSubStateListV(StateList requestedStateList)
	  /* This method requests the next state-machine state,
	    which is the same thing as this state's next sub-state.
	    
	    Though the requestedSubStateList variable changes immediately,
	    control is not transfered to the new sub-state until
	    the handler of the present sub-state exits.
	    
	    Requesting a state changing is considered progress
	    for purposes of deciding whether to retry a states doOnInputsB().
	    
	    If the machine is already in the requested sub-state,
	    the sub-state will be exited and reentered.
	    */
	  {
	  	if  // Report excess state change request.
				( (requestedSubStateList != null)
					&& (requestedSubStateList != StateList.initialSentinelState)
					)
        appLogger.error(
        		"StateList.requestSubStateListV(..), next state already requested."
        	  );

	  	requestedSubStateList= requestedStateList;
	  	}	

	/*  Methods for entry and exit of OrState or their sub-states.  */

	public final void doOnEntryV() throws IOException
	  /* This method is called when 
		  the state associated with this object is entered.
		  It is final so it can not be overridden.
		  It calls onEntryV() which may be overridden.
		  This version does nothing, but it should be overridden 
		  by state subclasses that require entry actions.
		  This is not the same as initialize*V(),
		  which does actions needed when the StateList object is being built
		  and is being prepared for providing its services.
		  
		  //enh: Presently this is called from orStateOnInputsB() only.
		  	Maybe it should be called when andStateOnInputsB() is called?
		  */
		{ 
		  ////appLogger.debug(
			if ( logB(TRACE)) logV( 
					TRACE, "StateList.doOnEntryV() to"+ getFormattedStatePathString() );
			setBackgroundColorV( UIColor.runningStateColor );
			onEntryV(); 
			}

	public void onEntryV() throws IOException
	  /* This method is called when 
	    the state associated with this object is entered.
	    This version does nothing, but it should be overridden 
	    by state subclasses that require entry actions.
	    This is not the same as initialize*V(),
	    which does actions needed when the StateList object is being built
	    and is being prepared for providing its services.
	    
	    //enh: Presently this is called from orStateOnInputsB() only.
	    	Maybe it should be called when andStateOnInputsB() is called?
	    */
	  { 
			}

	public void onExitV() throws IOException
	  /* This method is called when a state is exited.
	    It does actions needed when a state is exited.
	    This version does nothing.
	    It should be overridden in state subclasses that need exit actions.
	    This is not the same as finalizeV(),
	    called when the StateList object has finished providing its services.
	    and is about to be destroyed or freed
	    */
	  { 
			setBackgroundColorV( UIColor.inactiveStateColor );
			if ( logB(TRACE)) logV( 
					TRACE, "StateList.onExitV() from"+ getFormattedStatePathString() );
			}


	/* Methods containing general state handler code. */
	
	public synchronized void onInputsForLeafStatesV() throws IOException
	  /* This is a code-saving method.
	    A state class overrides either this method or onInputsB(), 
	    but not both, as part of how it controls its behavior.
	    Overriding this method instead of onInputsB() can result in
	    more compact code.  An override like this:

				public synchronized void onInputsForLeafStatesV() throws IOException
				  { 
				    some-code;
				    }

			is a more compact version of, but equivalent to, this:

				public synchronized boolean onInputsB() throws IOException
				  { 
				    some-code;
				    return false;
				    }

			WARNING: This method should only be used in state which extend StateList,
			or other states which are leaves, to avoid confusion
			with states which have an onInputsB().

			As with onInputsB(), because this method can be called from
			multiple threads, such as timer threads, it and all sub-class overrides
			should be synchronized.

		  See onInputsB() .
	    */
	  { 
		  // This default version does nothing.
	    }
	
	public synchronized boolean onInputsB() throws IOException
	  /* A state class overrides either this method or onInputsForLeafStatesV(),
	    but not both, as part of how it controls its behavior.

	    This method does nothing except return false unless 
	    it or onInputsForLeafStatesV() is overridden.
	    All overridden versions of this method should return 
	    * true to indicate that some computational progress was made, including:
		    * one or more sub-state's onInputsB() returned true,
		    * a discrete input String was processed,
		    * requestSubStateListV(StateList nextState) was called
		      to request a new qualitative sub-state.
	    * false if no computational progress is made, 
	      or progress made was indicated in some other way.
	    To return false without needing to code a return statement,
	    override the onInputsForLeafStatesV() method instead.
	    
	    A onInputsB() method does not return until
	    everything that can possibly be done has been done, meaning:
	    * All available inputs that it can process have been processed.
	    * All outputs that it can produce have been been produced.
	    * All changes to extended state variables have been made.
	    * A request for a state transition to the qualitative state 
	      has been made if it is possible.

			Because this method can be called from multiple threads, 
			such as timer threads, it and all sub-class overrides
			should be synchronized.
	    
	    */
	  { 
			onInputsForLeafStatesV(); // Call this in case it is overridden instead.
			return false; // This default version returns false.
		  }
	
	public final synchronized boolean doOnInputsB() throws IOException
	  /* This is the method that should be called to invoke a state's handler.
	    It can not be overridden.  
	    It calls override-able handler methods which 
	    state sub-classes may override.
	    */
	  { 
		  ///dbg  appLogger.debug(
			///dbg  	"StateList.doOnInputsB() of "
		  ///dbg  	+ discreteInputString 
			///dbg  + " to"
		  ///dbg  	+ getFormattedStatePathString() );
		  setBackgroundColorV( UIColor.runningStateColor );
			boolean successB= onInputsB();
			colorBySuccessV( successB );
			return successB;
		  }


	/* Methods for delaying and re-throwing exceptions.  */

	public void delayExceptionV( IOException theIOException )
		/* 	This method is used to save an IOException that 
		  occurred in a thread that couldn't handle it,
		  such as a Timer thread.
		  It can be re-thrown later on a thread that can handle it,
		  by calling throwDelayedExceptionV().
		  If there is already a saved exception then it is logged first. 
		  */
		{ 
		  if  // Log any previously saved exception.
	    	(delayedIOException != null) 
		  	appLogger.exception(
		  			"StateList.delayExceptionV(..), previously saved exception: ",
		  			theIOException
		  			);
			delayedIOException= theIOException;  // Save the new exception.
		  }

	public void throwDelayedExceptionV() throws IOException
		/* 	This method re-throws any IOException 
		  that occurred and was saved earlier
		  in a thread which could not fully process it, 
		  such as a Timer thread.
		  If there is no such saved exception then it simply returns. 
		  */
		{ 
		  if  (delayedIOException != null) // There is a saved exception. 
		  	throw delayedIOException; // Re-throw it.
		  else
		  	return;
		  }


	/* Methods for dealing with discrete input to state-machines.

	  Discrete input is input that arrives as discrete packets,
	  though not packets in the network sense,
	  as opposed to level-type inputs, represented by values in variables
	  which can change at any time.

	  Presently a discrete input is a String which identifies a message, 
	  parsed from the network input stream,
	  followed by additional data associated that that message
	  that follows the String in the same input stream.

	  The arrival of a discrete input is signaled 
	  by the variable discreteInputString being set 
	  by the handler's caller to the String input value.
	  If the state machine processes the input then 
	  it sets the variable to null, thereby consuming that input.
	  If it does not process it then the caller withdraws the input
	  by setting the variable to null so that the input
	  may be presented to other sub-state machines,
	  or discarded if no machine processes it.
	  */
	

	protected final synchronized boolean doOnInputsToSubstateB( 
			  StateList subStateList )
		  throws IOException
		/* This method calls subStateList's handler doOnInputsB().
		  Because it is sub-state processing,
		  it copies any discrete input to the sub-state first.
		  It returns true if the sub-state's handler returned true 
		    or a discrete input was consumed by the sub-state.
			It returns false otherwise.
			If a discrete input was consumed by the sub-state,
			then it is erased from this StateList also so that 
			it will not be processed by any other states.
			If an existing discrete input was not consumed,
			then it will remain stored in this state machine,
			and it is the responsibility of the caller to deal with it,
			either by removing it, trying to process it in a sibling state, 
			or by calling this method until the input is consumed.
			*/
		{
			boolean madeProgressB;
			String inputString= getDiscreteInputString();
			if ( inputString == null ) // Discrete input not present.
				madeProgressB= // Process only non-discrete inputs.
					subStateList.doOnInputsB();
				else // Discrete input is present.
				{
					subStateList.setDiscreteInputV(inputString);
				  	// Store copy of discrete input, if any, in sub-state.
					madeProgressB= subStateList.doOnInputsB();
					if // Process consumption of discrete input, if it happened.
						(subStateList.getDiscreteInputString() == null) // Consumed.
						{ resetDiscreteInputV(); // Remove input from this state also.
							madeProgressB= true; // Treat input consumption as progress.
							}
						else // Discrete input was not consumed by sub-state.
						subStateList.resetDiscreteInputV(); // Remove it from sub-state.
					}
			colorBySuccessV( madeProgressB );
			return madeProgressB;
			}


	public boolean tryInputB(String testString) throws IOException
	  /* This method tries to process a specific discrete input string.
	    If testString equals the stored input string 
	    then it is consumed and true is returned, 
	    otherwise the stored input string
	    is not consumed and false is returned.
	    If true is returned then it is the responsibility of the caller
	    to process other data associated with testString 
	    which follows it in the input stream.
	   	*/
	  {
			boolean successB= // Comparing requested discrete input to test input. 
					(testString.equals(discreteInputString));
		  if (successB) // Consuming stored input if it matched.
			  {
					if ( logB(DEBUG)) logV( 
							DEBUG,
							"StateList.tryInputB(..), \""
					  	+ this.discreteInputString
			  			+ "\" consumed by"
			  			+ getFormattedStatePathString()
			  			);
			  	discreteInputString= null;
			  	}
			return successB; // Returning result of the comparison.
		  }

	protected String getDiscreteInputString()
	  // This method returns the discrete input string stored in this state.
	  {
			return discreteInputString;
		  }

	public void setDiscreteInputV(String discreteInputString)
		/* This method stores discreteInputString within this state
		  for possible input by the state.
		 	*/
	  {
			{ // Log anomalous behavior first.
				String anomalyString= null;
			  if ( discreteInputString == null )
			  	anomalyString= 
			  	  discreteInputString + " value is ILLEGAL input to";
			  else if ( this.discreteInputString != null ) 
				  	anomalyString= 
				  		this.discreteInputString + " was NOT consumed by";
			  ///dbg else
				///dbg 	detailString= 
				///dbg discreteInputString + " input to";
			  if ( anomalyString != null ) // Log if anomaly produced.
			  	appLogger.warning(
			  			"StateList.setDiscreteInputV..), "
					  	+ anomalyString
			  			+ getFormattedStatePathString()
			  			);
				}

			this.discreteInputString= discreteInputString; // Store new input.
		  }

	public void resetDiscreteInputV()
		/* This method clears the discreteInputString within this state,
		  by setting it to null.
		  It should be used on a state and all its ancestors
		  when the input is processed and consumed,
		  and on a single state that processes the input but doesn't consume it.  
		 	*/
		{
		  if ( this.discreteInputString == null )
		  	appLogger.error(
		  			"StateList.resetDiscreteInputV(), input already consumed in"
		  			+ getFormattedStatePathString()
		  			);
		  
			this.discreteInputString= null;
		  }

	protected String getFormattedStatePathString()
	  /* Returns string with "state:" on first line, 
	    and "  (state path)" on the second.
	   */
	  {
		  return " state:\n  " + getStatePathString();
	    }

	private String getStatePathString()
	  /* Recursively calculates and returns a comma-separated list of state names
	    from the root of the hierarchical state machine to this state.
	   */
	  {
		  String resultString;
		  
		  if ( parentStateList == null )
		  	resultString= getNameString();
		  else
		    resultString= 
		  		parentStateList.getStatePathString()
		  		+ ", "
		  		+ getNameString(); 

		  Nulls.fastFailNullCheckT(resultString);
		  return resultString;
	  	}
	

	/* Method for dealing with timer input.  */

	public void run()
	  /* This method is run by TimerInput to run 
	    the doOnInputsB() method when the timer is triggered.
	    If an IOException occurs then it is saved for processing later by
	    a non-Timer thread.
	    */
		{
			appLogger.debug(
  			"StateList.run() beginning of Timer tick to"+ getFormattedStatePathString() );
			try { 
				doOnInputsB(); // Try to process timer event with handler. 
				}
		  catch ( IOException theIOException) { 
		    delayExceptionV( // Postpone exception processing to other thread.
		    		theIOException
		    		); 
		    }
			appLogger.debug(
	  			"StateList.run() end of Timer tick to"+ getFormattedStatePathString() );
		}


  /* Methods for UI cell rendering.  */

	public String processedNameString()
    /* This method returns a decorated name String.
      The decorations indicate that this node is a state,
      and whether the state is active.
      */
		{
			String resultString;
			if ( (parentStateList != null)
					&& (parentStateList.getpresentSubStateList() == this)
					)
				resultString= "*-" + getNameString();
				else
				resultString= " -" + getNameString();
	  	return resultString;
	  	}

  /* Methods for UI coloring.  */

	private void colorBySuccessV( boolean successB )
		{ if ( successB )
				setBackgroundColorV( UIColor.runnableStateColor );
				else
				setBackgroundColorV( UIColor.waitingStateColor );
		  }
	
  void setBackgroundColorV( Color theColor )
    /* This method sets the background color 
      which should be used to display this State.
      If the color changes then it also displays the State cell.
     */
    {
		  boolean colorChangingB= ( this.theColor != theColor );
		  if ( colorChangingB ) {
			  this.theColor= theColor; // Change color.
		  	reportChangeOfSelfV(); // Display change.
		  	}
  	  }
	
  Color getBackgroundColor( Color defaultBackgroundColor )
    /* This method returns the background color 
      which should be used to display this State.
      Presently it is simply the value stored in variable theColor.
      This is being changed.
      
      This is being changed to display
      * pink for inactive states
      * light green for active states
      
     */
    {
  		//// return theColor;  ///elim theColor?
  	  return ( getActiveB() ? Color.GREEN : Color.PINK);
  	  }

  private void invalidateActiveV()
    /* This method recursively invalidates the active status of 
      this state and any descendant states that depend on it.
      It doesn't check descendants if state is already invalidated.
      This direction of invalidation matches the down-propagation
      of the isActiveBoolean attribute.
      This method should be called whenever the state,
      or one of its ancestors,
      becomes an active sub-state or stops being an active sub-state.
      */
	  { 
			if ( isActiveBoolean != null ) // Invalidate if a value is now valid. 
				{ // Invalidate the value in this state and its descendants.
					for // Invalidate descendants first.
						(StateList subStateList : theListOfSubStateLists) // All of them. 
						{
			  	  	subStateList.invalidateActiveV(); // Invalidate in one subtree.
			  	  	}
					isActiveBoolean= null; // Invalidate in this state by assigning null.
					reportChangeOfSelfV(); // Trigger eventual GUI redisplay.
					}
			}

  private boolean getActiveB()
    /* This method returns true if this state is active, false otherwise.
      A state is considered active if:
      * it is a root state, or
      * it is any sub-state of an active AndState, or
      * it is the single active sub-state of an active OrState.
      The calculation done implements the down-propagation
      of the isActiveBoolean attribute.
      It returns the cached value of this calculation if it is present.
      */
  	{ 
			if ( isActiveBoolean != null ) // Reevaluate activity value if needed.
				; // Do nothing because we can return the cached evaluation.
	  		else
	  		{ // Evaluate activity value and cache it.
		  			boolean activeB;
					evaluation: {
		  		  StateList parentStateList= this.parentStateList;
						if // There is no parent state so this is a root state.
							(parentStateList == null)
							{ activeB= true; break evaluation; } // Roots are always active.
						if // Parent is an AndState and it is active.
						  ( parentStateList.isAndStateB()
								&& parentStateList.getActiveB() )
							{ activeB= true; break evaluation; } // So this state is active.
					  if // Parent is an OrState, active, and this is its sub-state.
					    ( this == (parentStateList.getpresentSubStateList() )
					    	&& parentStateList.getActiveB() )
					  	{ activeB= true; break evaluation; } // So this state is active.
					  activeB= false; // Anything else means this state is not active.
			  		} // evaluation:
		  		isActiveBoolean= Boolean.valueOf(activeB); // Cache the evaluation.
		  		}
			return isActiveBoolean; 
		  }

  protected boolean isAndStateB()
    /* This method returns true if this state is behaving as an AndState,
      false otherwise.
      */
  	{ 
  		return ( presentSubStateList == null) ; 
  		}
  
	}  // StateList class 


class SentinelState extends StateList {

	/* This class overrides enough non-no-op methods in StateList
	  that need to be no-ops so that this class can be used
	  as an initial sentinel-state for OrState state machines.
	  By doing this the actual desired first state 
	  can be requested in normal way,
	  after being requested with requestStateListV(..),
	  and null checks can be avoided.
	 	*/

  protected void reportChangeOfSelfV()
		/* This method does nothing because SentinelStates
		  should never be part of display-able DAG.
		 	*/
  	{
  		}

	} // class SentinelState

class AndOrState extends StateList {

	/* The methods in this class use the value of 
	  the variable presentSubStateList to determine at run-time whether 
	  this state should act as an AndState or an OrState.
	  It acts as an AndState until and unless setAsOrStateV(..) is called.

	  This includes the following methods:
	    onEntryV(), onInputsB(), and onExitV().
    */
  
  protected void setFirstOrSubStateV(StateList firstSubStateList)
	  /* This method sets this state to behave as an OrState
	    and requests the first state-machine state to be firstSubStateList. 
	    */
	  {
  		requestSubStateListV( firstSubStateList );
  	  presentSubStateList= StateList.initialSentinelState;
	  }

	public void onEntryV() throws IOException
	  /* This method recursively enters each of the concurrent sub-states
	    if we are acting as an AndState.
	    */
	  { 
			super.onEntryV();
			if ( isAndStateB() )
				for (StateList subStateList : theListOfSubStateLists) 
					{
	  			  ///dbg  appLogger.debug(
						///dbg  		"AndOrState.onEntryV() entering sub"
						///dbg    	+ subStateList.getFormattedStatePathString()
						///dbg  	);
		  	  	subStateList.doOnEntryV();
						}
			}
	
	public synchronized boolean onInputsB() throws IOException
	  /* This method acts as an AndState handler if presentSubStateList == null,
	    otherwise it aces as an OrState handler.
	    */
	  { 
		  boolean successB;
			if ( isAndStateB() )
		  	successB= andStateOnInputsB();
		  	else
		  	successB= orStateOnInputsB();
			return successB;
		  }

	public void onExitV() throws IOException
	  /* This method recursively exits each of the concurrent sub-states
	    if this state is acting as an AndState.
	    */
	  { 
			if ( isAndStateB() )
	  		for (StateList subStateList : theListOfSubStateLists) 
	  	  	subStateList.onExitV();
			super.onExitV();
			}
	
		}

class OrState extends AndOrState { //? StateList {

	/* There are two ways to get "or" state-machine behavior.
	  * Extend this class.
	  * Extend StateList but call the "or" state handler method.
	  
	  OrState machines have sub-states, but unlike AndStates,
	  only one sub-state can be active at a time.

	  There is no concurrency in an OrState machine, at least not at this level.
	  Its sub-states are active one at a time.
		*/

  public StateList initializeWithIOExceptionStateList() throws IOException
    {
  		super.initializeV();
  	  setFirstOrSubStateV( StateList.initialSentinelState );
  	  theColor= UIColor.initialOrStateColor;
  	  return this;
    	}

  /*  //?
  public synchronized boolean onInputsB() throws IOException
	  /* This method calls the default OrState handler.  */
  /*  //?
	  { 
  		return orStateOnInputsB(); 
  		}
  */  //?

	}  // OrState 

class AndState extends AndOrState { //? StateList {

	/* There are two ways to get "and" state-machine behavior.
	  * Extend this class.
	  * Extend StateList but call the "and" state handler method.

	  AndState machines have sub-states, but unlike OrStates,
	  all AndState sub-states are active at the same time.
	  There is concurrency in an AndState machine, at least at this level.
	  */

  public StateList initializeWithIOExceptionStateList() throws IOException
    {
  		super.initializeV();
  	  theColor= UIColor.initialAndStateColor;
  	  return this;
    	}

  /*  //?
	public synchronized boolean onInputsB() throws IOException
		{ 
		  return andStateOnInputsB(); 
		  }
  */  //?

	}  // AndState 
