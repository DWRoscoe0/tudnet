package allClasses;

///org Scan for all uses of "signal", previously called "progress",
//   and make it make sense.

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static allClasses.AppLog.theAppLog;
import static allClasses.AppLog.LogLevel.*;
import static allClasses.SystemSettings.NL;


@SuppressWarnings("unused") ////
public class StateList extends MutableList implements Runnable {

	/*  This class is the base class for all state objects and state-machines.

	  States are hierarchical.  A state can be both:
	  * a sub-state of a larger state-machine 
	  * a state-machine with its own sub-states

    Here are the classes in this file and their relationships:
      * StateList
		    * SentinelState
		    * LeafState   ///org to be added?
		    * AndOrState
			    * OrState 
			    * AndState
	    
    Here are descriptions of the classes in this file.
    * StateList: This is the base class of all states.
      It is also typically used as machine states with no children.
      ///org Add LeafState for leaf states so run-time code can be replaced be
        faster code organized by subclasses.
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


		Machine Initialization:

    Many, maybe most, state-machine states are simple leaf states.
    Many have few, if any, dependency inputs at construction time.
		To reduce boilerplate code in these low-level states, 
		constructor source code has been eliminated.  
		There are constructors, but they are the default constructors.
		
		For the states which require dependency inputs, setter injection is used.
		Instance variables are initialized using various initialization methods,
		such as initializeV(..) and initializeWithIOExceptionStateList(..).
		This avoid the difficulties of exceptions thrown within constructors.
		
		Initialization uses a combination of eager-evaluation and lazy-evaluation.
		For each [root?] state machine, initialization should proceed as follows:
		
		* The state-machine's default 0-argument constructor is called.

	  * The state machine's initialization method is called.  
	    It does the following.
	    * It assigns all instance variables that need assigning.
	    * If the state-machine has sub-states then 
	      it constructs, initializes, and adds them to the state-machine.
			* If the machine is an OrState machine subclass,
			  the machine's initial sub-state is set.
			* If a sub-state needs references to another sub-state
			  which wasn't created until later, the reference is injected now. 

	  At this point the state-machine is fully constructed and ready to run.
    Machine activation is all that remains.
    The doOnEntry(..) method of the state-machine is called.
    This recursively calls the doOnEntry(..) method of sub-machines 
    that should be active.
    doOnEntry(..) is called manually only in 
    the root machine of a state-machine hierarchy.


		Threads:

		Most state machines don't have threads of their own. 
		Their handler code is called by external threads.  
		The exception is the single root state of a hierarchical state machine.
		
		There are 2 types of state machine threads:
		* The thread that drives the root machine of a hierarchical state machine.
		  It is similar to an event dispatch loop.
		  It waits for and receives one or more 
		  different types of ordinary inputs 
      and passes them to the root state-machine handler.
      The handler returns when those inputs, 
      and possibly some newly arriving ones, have been handled.
    * There can be zero or more additional threads 
      that are used to deliver inputs triggered by timers.
      These timers are created or called-upon by states as needed.

    State-machine handler methods are synchronized,
		so only one thread at one time may execute a state's handler code.

		State machine handler code should not do busy-waits, 
		or contain long loops, because doing so could disable 
		other parts of the hierarchical state machine of which it is a part.


    Handler Methods:

		State machines run when their handler methods are called.
		A handler's main job is to process events, usually inputs.
		In doing so they might change the machine's [sub]state
		or produce some outputs, or both. 
   	
   	There are two sets of state-machine handler methods in this file.
   	* Final handler methods are the methods called to execute 
   	  a state's code.  These may not be overridden.
   	* Override-able handler methods may be overridden by state subclasses.
   	  These methods are called by the 
   	  above-mentioned non-override-able final handler methods.


    Hierarchical state machine major states: 

    A hierarchical state machine has two major states,
    not to be confused with their own sub-states.  The major states are:
    * Waiting for the next input event.  
      In this major state, no handler methods are active.
      All threads that provide inputs to the machine are waiting
      or busy doing other things. 
    * Processing input events.  
      In this major state, one or more handler methods are active.
      They are processing one or more input events.
      During processing, handlers might cause one or more events
      which are inputs to other parts of the hierarchical state machine.
      Processing inputs should be quick.
      When all processing is finished, all handlers will return,
      and the machine will return to the other major state: 
      waiting for the next input event.


		Signals, also known as events:

    Signals carry information between parts of 
    the hierarchical state machine, and between 
    the state machine and the external world.
    Signals can be of several types.

	  * Continuous time vs. discrete time:
	  	* A continuous time signal can change at any time,
	  	  like the value of a Java variable.
	  	  The only way to know it has changed is to examine it.
	  	  A continuous time signal can change at any time.
	  	  It's possible not all changes will be seen,
	  	  depending on when and how often it is examined.
	  	  An example of a continuous time signal is a thermometer reading.
	  	* A discrete time signal is a sequence of data values
	  	  that become available at known times.
	  	  Every datum is accompanied by a notification of its arrival.
	  	  If makes no sense to read it more than once per notification.
	  	  An example of a discrete time signal is 
	  	  a stream of email messages arriving in an In-Box.

    * Input signals vs. output signals: 
    	* An input signal is one that is produced outside of a machine,
    	  and read or consumed inside of the machine.
	 		* An output signal is one that is produced inside of a machine,
    	  and read or consumed outside of the machine.
    	This property is contextual.  A signal which is
    	an output of one machine might be an input to another machine.

    The following is a list of some signals of interest to these state machines,
    signals that are checked by the base state-machine infrastructure, 
    and how these signals should be handled:
  	* A state change request: 
  	  A state machine can make this request to its ancestor state machines.
  	  In most cases, the ancestor state that should respond is the parent.
  	  The appropriate ancestor state responds by changing its state 
  	  to the sub-state requested, and calling its handler.
    * A timer being triggered.  
      The affected state handler will take an appropriate action.
    * Discrete data objects inputs.  These can be:
      * queued objects such as received network packets,
    	* strings or other objects parsed from those packets, 
    	* bytes in those strings.
    	An affected state handler will take an appropriate action.
    	Very possibly it will read additional data from the same input stream
    	and act on the entire sequence as a whole.
    * Handler method return codes.
      When a return code is true, it means that 
      some other otherwise undocumented signal has been produced.  
      See the additional information below about 
      handler methods and return codes.


		Handler methods and their protocol (return status values):

		To review, the purpose of a state machine handler method 
		is to process one or more inputs to its state machine.
		A state machine's handler method is called whenever it is possible that 
		one or more of the state machine's inputs has changed or appeared.
		The processing might include internal state transitions.
		passing of signals between internal sub-machines,
		and/or the production of one or more outputs.

		A state handler method may return at any time.
		The way the caller responds to a return depends on
		the handler method's return code.  Details follow.

		There are several ways that a state can pass information/signals.
		* state [change] request: This is a signal requesting that
		  the state machine activate a particular state.
		  Usually it is a request from one state to its parent
		  to change the parent's sub-state to be
		  a sibling of the requesting state,
		  but other requests are possible.
		* discrete object inputs:
		  * A discrete object input is passed by 
		    setting a state input variable for that purpose to contain
		    a reference to that object, and calling the state's input handler.
      * The state may act on the input, or may ignore it, 
        as appropriate to the application.
      * The state may consume the input, or not consume it, 
        as appropriate to the application.
      * Input being consumed means that the input was fully processed 
        by the state that consumes it and no further action is needed.
      * Input NOT being consumed means either
        * that the input was not processed by 
          the state that did not consume it, or
        * that the input was processed by the state that did not consume it but
          this is a broadcast type of input that may be processed by 
          multiple states.
		  * Generally a state will offer the input to one or more of its children
		    before trying to process the input itself.
		    This makes it possible for sub-states to override 
		    the default behavior of their parent. 
		  * If the state's handler processes and consumes the discrete input, 
		    it sets the input variable to null to indicate this.
		    The setter of the variable interprets this to mean
		    that the input was consumed and no further processing of it needed.
		  * If the state's handler does not consume a discrete input then 
		    the input datum remains in the state's input register variable.
		    The setter interprets this to mean that the input was not consumed.
		    In this case the setter will set the variable to null
		    and one-at-a-time store the value to the input variable of 
		    other sub-states that should receive it and call their handlers.
		    The setter might also try to process the input itself.
		* handler return code:  State handers methods return a boolean value. 
		  This value has the following meanings.
      * false: This means that no progress was possible.
        The current state's handler and the handlers of 
        the current state's descendant states, if any, 
        examined all available state machine inputs of interest to them,
        and found no input that could be processed in any way.
        Previously false meant that there might or might not have been progress,
        but no further progress was possible.
      * true: This means that some computational progress was made 
        Previously true meant that that at least some input-processing work 
        remains to be done.
      * processing inputs.  In this case, the following should happen.
        * The handlers of other states, probably concurrent sibling states, 
          should be called to process the remaining inputs,
          and any output signals that might have been produced by 
          the state handler that just returned. 
        * The state handler that just returned should be called again
          to try to finish up its work.
          If it returns true again, this sequence should be repeated,
          until it finally returns false indicating no further work is possible.


		Exception Handling:

		It is common for a state-machine to receive input from InputStreams,
		and InputStreams produce IOExceptions.  These exceptions must be handled.
		* Most state-machine handler methods handle IOExceptions by 
			declaring that they may throw them.  But not all threads 
			that execute state-machine code can handle exceptions.
			An example of this is the Timer thread.
			To accommodate such threads, methods are provided that,
			instead of throwing an IOException, they record the IOException,
			so it can be re-thrown later by a different thread that can handle it.
		* Originally it was thought that IOExceptions must be handled
		  when a state object is constructed, but this is difficult to do.
		  So initialization was moved from constructors to initialization methods.
		  initializeWithIOExceptionStateList() was created for this purpose.
		  However it might be possible to eliminate this eventually.


		Other Notes:

	  When writing state-machine code it is important to distinguish between
	  * the current State object, designated by "this", and
	  * its current sub-state object, or child state, 
	    designated by "this.presentSubStateList".

		///enh At first, StateList and its subclasses AndState and OrState
		  do not [yet] provide behavioral inheritance, which is
		  the most important benefit of hierarchical state machines.
		  Note, this is now being added.

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

	private int pathLevelI; // Distance from root node.
	  // This, along with the link to the parent node,
	  // was added to speed the finding of Lowest Common Ancestors (LCAs),
	  // without needing to traverse the tree from the root.
	  // But so far it has not been used for that yet.
	  // Presently this variable is only globally down-propagated.
	  ///opt This could be cached, but it wouldn't save much.
	  //  If it was, down-invalidation would be setting it to -MAXINT.

	/// elim protected Color theColor= UIColor.initializerStateColor;

  protected List<StateList> theListOfSubStateLists= // Our sub-states.
      new ArrayList<StateList>(); // Initially empty list.

  private MapEpiNode offeredInputMapEpiNode= null; ////// New input.
  private String offeredInputString; 
    /* Temporarily stores a discrete input event.
    It is the one place this state checks for discrete input.
    This variable is set according to the following protocol:
    * set to a non-null reference to an input String by 
      the state handler's caller immediately before 
      the handler is called to try to process the input
    * set to null.
  	  * This is done by the handler immediately after processing the input if
  	    the input was fully processed.  Setting to null in this case
  	    indicates to the caller that the input was consumed.
      * This is done by the handler's caller after a handler returns
        without setting it to null before returning.
        This means either that the handler failed to process the input 
        or succeeded processing it but the message is a broadcast-type message
        meant to be processed by multiple states.
        In this case the caller may offer the input to other sub-states
        and might process the input itself.
    */

	private IOException delayedIOException= null; /* Storage for an exception
	  thrown in a thread, such as a Timer thread which can't handle it,
	  for re-throwing later in a thread that can handle it. 
	  */

	/* Variables used only for OrState behavior.. */

	// Sentinel states which can simplify other code by eliminating null checks.
  // Sentinel states are used by OrState machines. 
	protected static final SentinelState initialSentinelState;

	static { // Static initialization.
		initialSentinelState= new SentinelState(); 
		initialSentinelState.initializeV();
		}
	
  protected StateList presentSubStateList= null; /* Machine's qualitative state.
        This is used to select between and-state and or-state behavior
        This will be null when the state is behaving as an and-state.
        When it is not null then is the or-state's active sub-state. 
        See AndOrState.
        */

  protected StateList nextSubStateList= null; /* Becomes non-null when 
  	machine requests a new qualitative state.  
    It will eventually become the present state also, 
    which would cause state exit and re-entry when that happens.
    */

  private Boolean cachedActiveBoolean= null; // Initially invalid.
  
	/* Methods used to build state objects. */

  public StateList initializeWithIOExceptionStateList() throws IOException
    /* This method can also be used to initialize this state object.
      It calls initializeV() and it also returns a reference to this StateList.
      This method difference from ordinary initialization methods because:
      * It throws an IOException and makes this clear in its name.
      * It returns a reference to this object which can be used to
        simplify code in the caller.

      ///org? This method throws IOException.
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
    /* This method initializes this state object and its super-classes.
      It does actions needed when this state object is being built.
      This is not the same as the entryV() method, which
      does actions needed when the associated state-machine state is entered.

			Like constructors, this method should be called first
			from the sub-class versions of this method.
  		*/
    {
  	  super.initializeV();
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

      It also adds this state as a child DataNode to 
      this state's DataNode subclass's list of child DataNodes.
      */
  	{ 
      /// appLogger.debug( 
      ///    "addStateListV(StateList theSubStateList) begins with "+pathLevelI);
      { // Add as StateList child.
    	  theListOfSubStateLists.add( theSubStateList ); // Add theSubState to
    	    // this state's list of sub-states.
        theSubStateList.propagateIntoSubtreeB( pathLevelI );
    	  theSubStateList.setParentStateListV( this ); // Store this state as
    	  	// the sub-state's parent state.
        } // Add as StateList child.
  	  
  	  addAtEndV( theSubStateList ); // Add as DataNode child.
  	  }

  protected boolean propagateIntoSubtreeB( int pathLevelI )
    /* This method propagates pathLevelI into 
      this node and any of its descendants which need it,
      incrementing its value with each additional descendant level. 
      It acts only if the present level is not
      one more than the input pathLevel.
      
      ///opt  return value might not be needed.
      */
    {
      pathLevelI++; // Increment because we have gone down 1 level. 
      boolean changeB= ( this.pathLevelI != pathLevelI ); 
      if ( changeB ) // Store pathLevelI if its value is not already correct.
        {
          for (StateList subStateList : theListOfSubStateLists)
            subStateList.propagateIntoSubtreeB( // recursively propagate  
              pathLevelI ); // the new level into child subtree.
          this.pathLevelI= pathLevelI; // Update this node.  This eliminates
              // the inequality which triggered this storing.
          }
      return changeB;
      }

  public synchronized void finalizeV() throws IOException
    /* This method processes any pending loose ends 
      before shutdown and object destruction.
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
    /* This method returns the present machine sub-state.
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
	    until none of them makes progress as indicated by its return code.
	    It scans the sub-machines in order until one does.
	    Then it restarts the scan.
	    It is done this way to prioritized the sub-machines.
	    If one sub-machine produces an output signal 
	    which is an input signal to other sub-machines,
	    then earlier machines have the first change to process it.
	    It keeps scanning until none of the sub-machines' handlers return true. 
	    This method returns true if any sub-machine returned true during any scan, 
	    false otherwise.
	    */
	  { 
			throwDelayedExceptionV(); // Throw exception if one was saved earlier.
		  boolean stateProgressB= false;
  	  substateScansUntilNoProgress: while(true) {
 	  		for (StateList subStateList : theListOfSubStateLists)
   	  		{
   	  		  boolean substateProgressB= doOnInputsToSubstateB(subStateList); 
  	  	  	if (substateProgressB) 
  		  	  	{
  		  	  		stateProgressB= true;
  		  	  		continue substateScansUntilNoProgress; // Restart scan.
  		  	  		}
   	  		  }
 	  		break substateScansUntilNoProgress; // No progress was made in this final scan.
  	  	} // substateScansUntilNoProgress:
			return stateProgressB; 
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
	    
	    //////fix? State changes should keep loop going
	      and count as progress?
	    ///enh Must check for sub-state validity vs. super-state 
	      when and if behavioral inheritance is added.
      */
	  { 
			throwDelayedExceptionV(); // Throw exception if one was saved.
	  	boolean stateProgressB= false;
			while (true) { // Process sub-states until no new next one.
	  	    boolean substateReturnB= // Call sub-state handler.
	  	    		doOnInputsToSubstateB(presentSubStateList);
          if (substateReturnB) // Accumulate its return code
            stateProgressB= true; // in state return code.
          boolean stateChangedB= tryPreparingNextStateB();
          if (!stateChangedB) // If no next state
            break; // exit, we're done.
	  	  	} // while (true)
			return stateProgressB; // Returning accumulated state return code.
			}

  private boolean tryPreparingNextStateB() throws IOException
    /* This method prepares to processes the next sibling state,
      if there is one.
      * It returns true if there is a requested next sibling state,
        and preparations for it, such as exiting the present state,
        and entering the new state, have been done.
      * It returns false if either there is no requested next state,
        or there is but it is not a sibling state.
        In this case the request must be handled elsewhere.
     */
  {
    boolean stateIsChangingB= false; // Assume not changing.
    toReturn: {
      if ((nextSubStateList == null)) // No change is requested.
        break toReturn;
      if ( // Present state and desired state are not siblings. 
          presentSubStateList.parentStateList 
          != nextSubStateList.parentStateList )
        break toReturn; // Present state-machine's state can not change.
      { // Changing state is okay.  Make the change.
        presentSubStateList.doOnExitV(); // Exit old sub-state.
        presentSubStateList.invalidateActiveV();
        presentSubStateList= nextSubStateList; // Set new sub-state.
        presentSubStateList.invalidateActiveV();
        presentSubStateList.doOnEntryV(); // Enter new sub-state.
        nextSubStateList= null; // Consume state-change request.
        }
      stateIsChangingB= true;
      } // toReturn:
    return stateIsChangingB;
    }

  protected boolean requestSubStateChangeIfNeededB(
      StateList requestedStateList)
	  /* This method does the same as requestSubStateListV(..) except that
	    if will reject the state-change request and return false if 
	    the present sub-state is the same as the requested state. 
	    In this case the sub-state will not be exited and reentered.
	    This method will return true if the request actually occurs, false otherwise.
	    */
	  {
  	  boolean changeIsNeededB= // Calculate whether the state needs changing. 
  	  		( presentSubStateList != requestedStateList );
	  	if ( changeIsNeededB )
	  		requestSubStateListV(requestedStateList); // Request the state change.
      theAppLog.debug( "requestSubStateChangeIfNeededB(..) "+changeIsNeededB);
	  	return changeIsNeededB; // Return whether request occurred.
	  }	

  protected void requestAncestorSubStateV(StateList requestedStateList)
    /* This method requests a state change to requestedStateList,
      which must be a sub-state of an ancestor of this StateList.
      If it is not, it logs an error, and does nothing else.
      A StateList is not considered an ancestor of itself.
      */
  {  
    parentStateList.requestSubStateListV(requestedStateList);
      // Request a sub-state change in parent.
      // Validity of request will be tested later.
    }

  private Throwable oldThrowable= null;

  protected void requestSubStateListV(StateList requestedStateList)
	  /* This method requests the next state-machine state,
	    which is the same thing as this state's next sub-state.
	    
	    Though the requestedSubStateList variable changes immediately,
	    control is not transfered to the new sub-state until
	    the handler of the present sub-state exits.
	    
	    Requesting a state change is considered making progress
	    for purposes of deciding whether to retry a state's doOnInputsB().
	    
	    If the machine is already in the requested sub-state,
	    the sub-state will be exited and reentered.
	    ///org Is this a good idea?
	    
	    ///enh Being modified to request state changes 
	      in ancestor state machines.
	       
	    */
	  {
      Throwable newThrowable= new Throwable("stack trace at call");
      if  // Detect and report if this is an excessive state change request.
				( (nextSubStateList != null) // There is an old request, it's,
					&& (nextSubStateList != StateList.initialSentinelState) // not the sentinel,
		      && (nextSubStateList != requestedStateList) // and it's not a duplicate request.
					)
        synchronized(theAppLog) { // Must synchronize all output on AppLogger object. 
          theAppLog.debug(  // Log entry header, first line.
              "StateList.requestSubStateListV(..), already requested,");
          theAppLog.appendToOpenFileV(
              nextSubStateList.getFormattedStatePathString()+NL);
          theAppLog.doStackTraceV(oldThrowable);
          theAppLog.appendToOpenFileV(NL + "new request is");
          theAppLog.appendToOpenFileV(
              requestedStateList.getFormattedStatePathString()+NL);
          theAppLog.doStackTraceV(newThrowable);
          }
      oldThrowable= newThrowable; // Save new Throwable as old for next time.

      nextSubStateList= requestedStateList; // Now, finally, actually record the request.
	  	}	

	/*  Methods for entry and exit of OrState or their sub-states.  */

	public final void doOnEntryV() throws IOException
	  /* This method is called when 
		  the state associated with this object is entered.
		  It is final so it can not be overridden.
		  It calls onEntryV() which may be overridden.
		  
		  //enh: Presently this is called from orStateOnInputsB() only.
		  	Maybe it should be called when andStateOnInputsB() is called?
		  */
		{ 
      /// appLogger.debug( "StateList.doOnEntryV() to"+ getFormattedStatePathString() );
			//// if ( logB(TRACE)) logV( 
			//// 		TRACE, "StateList.doOnEntryV() to"+ getFormattedStatePathString() );
      theAppLog.debug("StateList",
          "StateList.doOnEntryV() to"+ getFormattedStatePathString() );

			onEntryV(); 
			}

	public void onEntryV() throws IOException
	  /* This method recursively enters each of its relevant sub-states.

	    It may be overridden in subclasses, but if it is, 
	    those subclass methods should probably begin with a call to 
	      super.onEntryV();
			  
			///opt  This method probably belongs in class AndOrState,
			  and this method should be an empty method.
			  Maybe there should be a class LeafState for use by leaf state subclasses.
	    */
	  { 
			/// super.onEntryV();
			
			if ( ! isAndStateB() ) // This is an OrState.
				presentSubStateList.doOnEntryV(); // Enter its only active sub-state.
			else // This is an AndState.
				for  // Enter all of its sub-states.
				  (StateList subStateList : theListOfSubStateLists) 
					{
		  	  	subStateList.doOnEntryV();
						}
			}

  public boolean logOrSubstatesB(String callerString)
    /* This method recursively logs any descendant OrState active sub-states.
       It returns true if any states were logged, false otherwise.
       callerString is made part of the log entry to identify the source.
       
       ///opt Don't log ancestors of OrStates already logged.
     */
    { 
      boolean anyStateLoggedB= false;
      if ( ! isAndStateB() ) // This is an OrState.
        {
          if (presentSubStateList.logOrSubstatesB(callerString)) anyStateLoggedB= true;
          ///opt Don't log this state if we logged any sub-states.
          theAppLog.debug( // Log this OrState's sub-state
              callerString
              + ": StateList.logOrSubstatesB()(), "
              + presentSubStateList.getFormattedStatePathString());
          }
      else // This is an AndState.
        for  // Recurse into all sub-states looking for active OrState sub-states to log.
          (StateList subStateList : theListOfSubStateLists) 
          {
            if (subStateList.logOrSubstatesB(callerString)) anyStateLoggedB= true;
            }
      return anyStateLoggedB;
      }

	public final void doOnExitV() throws IOException
	  /* This method is called when a state is exited.
	    It does actions needed when a state is exited.
		  It is final so it can not be overridden.
		  It calls onExitV() which may be overridden.
	    */
	  { 
			onExitV();
			//// if ( logB(TRACE)) logV( // Log that exit took place. 
			//// 		TRACE, "StateList.doOnExitV() from"+ getFormattedStatePathString() );
      theAppLog.debug("StateList",
          "StateList.doOnExitV() from"+ getFormattedStatePathString() );
			}

	public void onExitV() throws IOException
	  /* This method recursively exits each of this states active sub-states.
	    It handles both AndState and OrState cases.
	    */
	  { 
			if ( ! isAndStateB() ) // this is an OrState.
				presentSubStateList.doOnExitV(); // Exit its single active sub-state.
			else // this is an AndState.
	  		for // Exit all its active concurrent sub-states. 
	  		  (StateList subStateList : theListOfSubStateLists) 
	  	  	subStateList.doOnExitV();
			
			/// super.onExitV();
			}


	/* Methods containing general state handler code. */
	
	public synchronized void onInputsToReturnFalseV() throws IOException
	  /* This is a code-saving method.
	    A state class overrides either this method or onInputsB(), 
	    but not both, as part of how it controls its handler's behavior.
	    Overriding this method instead of onInputsB() can result in
	    more compact code.  An override like this:

				public synchronized void onInputsReturnsFalseV() throws IOException
				  { 
				    some-code;
				    }

			is a more compact version of, but equivalent to, this:

				public synchronized boolean onInputsB() throws IOException
				  { 
				    some-code;
				    return false;
				    }

			WARNING: Some states such as AndState, OrState, and their subclasses
			already override the onInputsB() method.
			In these cases, overriding the onInputsToReturnFalseV() method
			will have no effect.
			This method should only be overridden in state 
			which extend the class StateList,
			or other state classes which are leaves.

			As with onInputsB(), because this method can be called from
			multiple threads, such as timer threads, it and all sub-class overrides
			should be synchronized.

		  See onInputsB() .
	    */
	  { 
		  // This default version does nothing.
	    }
	
	public synchronized boolean onInputsB() throws IOException
	  /* A state class overrides either this method or onInputsToReturnFalseV(),
	    but not both, as part of how it controls its behavior.

	    This method does nothing except return false unless 
	    it or onInputsToReturnFalseV() is overridden.
	    All overridden versions of this method should return 
	    * true if any computational progress was made.
	    * false otherwise.
	    To return false without needing to code a return statement,
	    override the onInputsToReturnFalseV() method instead.

	    An onInputsB() method should not return false until
	    everything that can possibly be done has been done, meaning:
      * It was unable to process any new inputs or input changes.
      * It was unable to produce any new outputs or output changes.
      * It was unable to make a state transition in the qualitative state.
      * It was unable to change any extended state variables.

			Because this method can be called from multiple threads, 
			such as timer threads, it and all sub-class overrides
			should be synchronized.
	    
	    */
	  { 
			onInputsToReturnFalseV(); // Call this in case it is overridden instead.
			return false; // This default version returns false.
		  }
	
	public final synchronized boolean doOnInputsB() throws IOException
	  /* This is the method that should be called to invoke a state's handler.
	    It can not be overridden.  
	    It calls the override-able handler methods which 
	    StateList sub-classes may override.
	    */
	  { 
      helloCheckV();
			boolean signalB= onInputsB();
      stateChange: { // Detect and prepare left-over state change request.
        if ((nextSubStateList == null)) // Not requested.
          break stateChange;
        {
          requestAncestorSubStateV(nextSubStateList); // Pass request
            // to our parent.
          nextSubStateList= null; // Consume our state-change request.
          }
        } // stateChange: 
      helloCheckV();
			return signalB;
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
		  	theAppLog.exception(
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
	  by the variable offeredInputString being set 
	  by the handler's caller to the String input value.
	  If the state machine processes the input then 
	  it sets the variable to null, thereby consuming that input.
	  If it does not process it then the caller withdraws the input
	  by setting the variable to null so that the input
	  may be presented to other sub-state machines,
	  or discarded if no machine processes it.
	  
	  ///enh Create a better way of doing discrete input 
	  so less copying is needed and kludges such as 
	  passing sub-states into common state methods, such as
	  LinkedMachineState.tryReceivingHelloB(StateList subStateList).
	  Make it work with current state and controlling ancestor states?
	  Maybe tie to InputStream?
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
			or by calling this method again until the input is consumed.
			*/
		{
			boolean signalB;
			if ( ! hasOfferedInputB(this) ) // Discrete input not present.
				signalB= // Process without passing the offered input.
					subStateList.doOnInputsB();
				else // Offered input is present.
				{ // Process with passed input.
				  moveOfferedInputV(this, subStateList);
            // Move offered input from this state to sub-state.
					signalB= subStateList.doOnInputsB(); // Process with offered input.
          if // Process consumption of input by sub-state, if it happened.
            (! hasOfferedInputB(subStateList)) // It was consumed.
            { signalB= true; // Treat input consumption as progress.
              }
            else // Discrete input was not consumed by sub-state.
            moveOfferedInputV( // Move it back to this state.
                subStateList, this);
					}
			return signalB;
			}

  public synchronized boolean processInputB(String inputString) 
      throws IOException
    /* This method processes a specific discrete input string, inputString.
      It was created for responding to outputs from other states,
      not inputs from the network, by injecting them into a machine.
      Processing is done by offering the string and cycling the state machine.
      If the string is processed by the machine, true is returned,
      otherwise false is returned.
      In either case, the offered input variables are reset before returning.
      */
    {
      setOfferedInputV(inputString); // Store string in input variable.
      while (doOnInputsB()) ; // Cycle state machine as many times as needed.
      boolean successB= // Calculate whether input was consumed.
          !hasOfferedInputB(this);
      if (!successB) { // Handle not-consumed input.
        theAppLog.debug( "StateList processInputB(..) unprocessed: "+inputString);
        resetOfferedInputV(); // Consume the input.
        }
      return successB; // Returning result of the attempt.
      }

  public boolean tryInputB(String testString) throws IOException
    /* This method tries to process a specific discrete input string.
      If testString equals the offered input string then 
      the offered input string is consumed and true is returned, 
      otherwise the offered input string
      is not consumed and false is returned.
      If true is returned then it is the responsibility of the caller
      to process any other data that might be associated with testString 
      which follows it in the input stream.
      */
    {
      boolean successB= // Comparing requested discrete input to test input. 
          (testString.equals(offeredInputString));
      if (successB) // Consuming offered input if it matched.
        {
          /*  ///dbg
          if ( logB(DEBUG)) logV( 
              DEBUG,
              "StateList.tryInputB(..), \""
              + this.offeredInputString
              + "\" consumed by"
              + getFormattedStatePathString()
              );
          */  ///dbg
          //// offeredInputString= null; // Consume input string.
          resetOfferedInputV(); // Consume the input.
          }
      return successB; // Returning result of the comparison.
      }

	protected String getOfferedInputString()
	  /* This method returns the discrete input String 
     * stored in this state.
     */
	  {
			return offeredInputString;
		  }

  public MapEpiNode tryInputMapEpiNode(String keyString)
    /* This method tries to process a specific discrete input MapEpiNode.
     * If keyString is a key in the next input MapEpiNode,
     * the offered input MapEpiNode is consumed and 
     * the value associated with the key String is returned.
     * Otherwise the offered input MapEpiNode is not consumed 
     * and null is returned.
     */
    {
        MapEpiNode valueMapEpiNode= null; // Assume not found.
      toReturn: {
        valueMapEpiNode= // Test for value for key from input map.  
          testInputMapEpiNode(keyString);
        if (null != valueMapEpiNode) // If input contained a value map
          resetOfferedInputV(); // consume that input.
      } // toReturn:
        return valueMapEpiNode; // Return value map as result.
      }

  public MapEpiNode testInputMapEpiNode(String keyString)
    /* This method tests for a specific discrete input MapEpiNode.
     * If keyString is a key in the next input MapEpiNode then
     * the value associated with the key String is returned.
     * Otherwise null is returned.
     */
    {
        MapEpiNode valueMapEpiNode= null; // Assume not found.
      toReturn: {
        if (null == offeredInputMapEpiNode) // If no input
          break toReturn; // exit with null result.
        valueMapEpiNode= // Try getting value from input map.  
            offeredInputMapEpiNode.getMapEpiNode(keyString);
      } // toReturn:
        return valueMapEpiNode; // Return value map as result.
      }

  protected MapEpiNode getOfferedInputMapEpiNode()
    /* This method returns the discrete input MapEpiNode
     * stored in this state.
     */
    {
      return offeredInputMapEpiNode;
      }

  public void setOfferedInputV(String newOfferedInputString)
    /* This method stores offeredInputString within this state
      for possible input by the state.
      */
    {
      { // Log anomalous behavior first.
        String anomalyString= null;
        if ( newOfferedInputString == null )
          anomalyString= 
            newOfferedInputString + " value is ILLEGAL input to";
        else if ( this.offeredInputString != null ) 
            anomalyString= 
              "Old input '" + this.offeredInputString + "' was NOT consumed by";
        if ( anomalyString != null ) // Log if anomaly produced.
          theAppLog.warning(
              "StateList.setOfferedInputV(String), "
              + anomalyString
              + getFormattedStatePathString()
              );
        }

      // theAppLog.debug("StateList.setOfferedInputV(..) with "+newOfferedInputString);
      this.offeredInputString= newOfferedInputString; // Store new input.
      }

  public void setOfferedInputV(MapEpiNode newOfferedInputMapEpiNode)
    /* This method stores offeredInputString within this state
      for possible input by the state.
      What this method does, resetOfferedInputV() is used to undo
      if the input is not consumed.
      */
    {
      { // Log anomalous behavior first.
        String anomalyString= null;
        if ( newOfferedInputMapEpiNode == null )
          anomalyString= 
            newOfferedInputMapEpiNode + " value is ILLEGAL input to";
        else if ( this.offeredInputMapEpiNode != null ) 
            anomalyString= 
              "Old input '" + this.offeredInputMapEpiNode + "' was NOT consumed by";
        if ( anomalyString != null ) // Log if anomaly produced.
          theAppLog.warning(
              "StateList.setOfferedInputV(MapEpiNode), "
              + anomalyString
              + getFormattedStatePathString()
              );
        }

      // theAppLog.debug("StateList.setOfferedInputV(MapEpiNode) with "+newOfferedInputString);
      this.offeredInputMapEpiNode= newOfferedInputMapEpiNode; // Store new input.
      }

	public void resetOfferedInputV()
		/* This method resets the offered input areas.
		  It does this by setting the input variables to null.
		  This method is needed only for reseting input that has been consumed.
		  Non-consumed input is reset by moving it back to its parent state. 
		 	*/
		{
      if ( ! hasOfferedInputB(this)) // Error check that there is input.
        theAppLog.error(
    			"StateList.resetOfferedInputV(), input already consumed in"
    			+ getFormattedStatePathString()
    			);

      // theAppLog.debug(
      //  "offeredInputCheckReset from" + getFormattedStatePathString() );
      staticOfferedInputMapEpiNode= null;
      // Reset both types of inputs.
      this.offeredInputString= null;
      this.offeredInputMapEpiNode= null;
  	  }

  private void moveOfferedInputV(
      StateList sourceStateList, StateList destinationStateList)
    /* This method moves offered input from sourceStateList
     * to destinationStateList.
     */
    {
      if (! hasOfferedInputB(sourceStateList))
        theAppLog.warning(
          "StateList.moveOfferedInputV(.), source has no input.");
      if (hasOfferedInputB(destinationStateList))
        theAppLog.warning(
          "StateList.moveOfferedInputV(.), destination has unconsumed input.");

      destinationStateList.offeredInputString= // Copy to destination.
          sourceStateList.offeredInputString;
      sourceStateList.offeredInputString= null; // Remove from source.
      destinationStateList.offeredInputMapEpiNode= // Copy to destination.
          sourceStateList.offeredInputMapEpiNode;
      sourceStateList.offeredInputMapEpiNode= null; // Remove from source.
      }
      
  private static MapEpiNode staticOfferedInputMapEpiNode= null;

  private void offeredInputCheckV()
    {
      if 
        ( 
          (staticOfferedInputMapEpiNode != offeredInputMapEpiNode)
          && (null != offeredInputMapEpiNode)
          )
        
      {
        theAppLog.debug(  ///////////
            "offeredInputCheckSet: "+offeredInputMapEpiNode
            +" from"+ getFormattedStatePathString() );
        staticOfferedInputMapEpiNode= offeredInputMapEpiNode;
        }
      }

  protected boolean hasOfferedInputB(StateList theStateList)
    /* This method returns true if theStateList has any offered input,
     * false otherwise.
     */
      { 
        // offeredInputCheckV();
        boolean resultB= 
            (null != theStateList.offeredInputString) ||
            (null != theStateList.offeredInputMapEpiNode)
            ;
        return resultB; 
        }

  /////////////////// ?
  private StateList savedStateList= null;
  private int helloCountI= 0;

  public void helloResetV()
    {
      // theAppLog.debug( "helloResetV() called.");
      savedStateList= null;
      }
  
  public void helloCheckV() throws IOException
    {
      goReturn: {
        MapEpiNode valueMapEpiNode= 
            testInputMapEpiNode("HELLO");
        if (null == valueMapEpiNode)
          break goReturn;
        if (null == savedStateList)
          helloCountI++;
        savedStateList= this;
      } // goReturn: 
        return;
      }
  
  protected String getFormattedStatePathString()
	  /* Returns string with "state:" on first line, 
	    and "  (state path)" on the second.
	   */
	  {
		  return " state:" + NL + "  " + getStatePathString();
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
			// appLogger.debug(  ///dbg
  		//	"StateList.run() beginning of Timer tick to"
  		//	+ getFormattedStatePathString() );
			try { 
				downAndUpDoOnInputsB(); // Try to process timer event with handler. 
				}
		  catch ( IOException theIOException) { 
		    delayExceptionV( // Postpone exception processing to other thread.
		    		theIOException
		    		); 
		    }
			// appLogger.debug( ///dbg
	  	//		"StateList.run() end of Timer tick to"+ getFormattedStatePathString() );
		}

	
	/* Method for logging conditionals.  */
	
  public boolean testAndLogIfTrueB(boolean theB, String messageString)
    /* This method logs a message and the current state if theB is true.
      In either case it also returns theB.
      */
    { 
      if (theB) // Log message with state path if condition true.
        theAppLog.debug( messageString + getFormattedStatePathString() );
      return theB; 
      }


  private synchronized boolean downAndUpDoOnInputsB() throws IOException
    /* This method is functionally similar to doOnInputs(),
      processing inputs in the current state and its descendants, but has,
      ///enh or will have, the additional ability 
      to process particular signals passed to 
      the handler methods of ancestors.
      
      This method is presently called only when a Timer is triggered.
      ///enh It might eventually be called by 
        the state-machine driver threads also.
      */
    { 
      boolean signalB= false;
      StateList scanStateList= this;
      while (true)  // Process inputs and left-over to ancestors. 
        { // Process one level.
          boolean thereAreLeftOversB= false;
          if (scanStateList.doOnInputsB()) // First, process inputs normally.
            signalB= true; 
          StateList parentStateList= scanStateList.parentStateList;
          if (parentStateList == null) break;
          stateChange: { // Detect and prepare left-over state change request.
            if ((parentStateList.nextSubStateList == null)) // Not requested.
              break stateChange;
            {
              thereAreLeftOversB= true;
              }
            } // stateChange:
          if (! thereAreLeftOversB) // No signals to pass to our parent.
            break;
          scanStateList= scanStateList.parentStateList; // Go to parent.
          } // while (therAreLeftOverSignalsB);
      return signalB; 
      } 


  /* Methods for UI cell rendering.  */

  public boolean isDecoratingB()
    /* Enables DataNode String decoration because we use it to indicate
      that we are a state node and whether or not we are active.
      */
    {
      return true;
      }
  
	public String decoratedCellString()
    /* This method returns a decorated name String.
      The decoration prefixes indicate that this node is a state,
      and whether the state is active.
      */
		{
			String resultString;
			if ( (parentStateList != null)
					&& (parentStateList.getpresentSubStateList() == this)
					)
				resultString= "*-" + getNameString(); // Active state.
				else
				resultString= " -" + getNameString(); // Inactive state.
	  	return resultString;
	  	}

  /* Methods for UI coloring.  */

  Color getBackgroundColor( Color defaultBackgroundColor )
    /* This method returns the background color 
      which should be used to display this State.
      It overrides the superclass DataNode default.
      Unless this method is overridden,
      this method will return one of two colors,
      depending on whether the state is active or not.
      
      This is being changed to display different colors depending on states
      and sub-states.
      
     */
    {
      return activityBasedBackgroundColor( UIColor.activeStateColor );
      }

  Color activityBasedBackgroundColor( Color activeColor )
    /* This method returns activeColor if this state is active,
      the inactive color otherwise.
      */
    {
      return ( getActiveB() 
          ? activeColor 
          : UIColor.inactiveStateColor
          );
      }

  private void invalidateActiveV()
    /* This method recursively invalidates the cached active status of 
      this state and any descendant states that depend on it.
      It doesn't check descendants if state is already invalidated.
      This direction of invalidation matches the down-propagation
      of the cachedActiveBoolean attribute.
      This method should be called whenever the state,
      or one of its ancestors,
      becomes an active sub-state or stops being an active sub-state.
      */
	  { 
			if ( cachedActiveBoolean != null ) // Invalidate if a value is now valid. 
				{ // Invalidate the value in this state and its descendants.
					for // Invalidate descendants first.
						(StateList subStateList : theListOfSubStateLists) // All of them. 
						{
			  	  	subStateList.invalidateActiveV(); // Invalidate in one subtree.
			  	  	}
					cachedActiveBoolean= null; // Invalidate here by assigning null.
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
      of the cachedActiveBoolean attribute.
      It caches the value if it was not cached already.
      */
  	{ 
			if ( cachedActiveBoolean != null ) // Reevaluate value if needed.
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
		  		cachedActiveBoolean= Boolean.valueOf(activeB); // Cache evaluation.
		  		}
			return cachedActiveBoolean; 
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

	  This class, or the superclass StateList, includes the following methods:
	    onEntryV(), onInputsB(), and onExitV().
	    
	  ///org, ///opt: Eliminate this class by eliminating references to it,
	  replacing subclass references to either OrState or AndState,
	  and moving code to those classes, until there is no code remaining. 
    */
  
  protected void setFirstOrSubStateV(StateList firstSubStateList)
	  /* This method sets this state to behave as an OrState
	    and requests the first state-machine state to be firstSubStateList. 
	    */
	  {
			//requestSubStateListV( firstSubStateList );
		  //presentSubStateList= StateList.initialSentinelState;
  	
		  presentSubStateList= firstSubStateList; // Assign present state.
	  	}
	
	public synchronized boolean onInputsB() throws IOException
	  /* This method acts as an AndState handler if presentSubStateList == null,
	    otherwise it aces as an OrState handler.
	    */
	  { 
		  boolean signalB;
			if ( isAndStateB() )
		  	signalB= andStateOnInputsB();
		  	else
		  	signalB= orStateOnInputsB();
			return signalB;
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
  	  /// elim theColor= UIColor.initialOrStateColor;
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

  Color getBackgroundColor( Color defaultBackgroundColor )
    /* This method returns the background color from the selected sub-state. 
      In other words, this state takes on the color of its active sub-state.
     */
    {
      return presentSubStateList.getBackgroundColor( defaultBackgroundColor );
      }

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
  	  return this;
    	}

  /*  //?
	public synchronized boolean onInputsB() throws IOException
		{ 
		  return andStateOnInputsB(); 
		  }
  */  //?

	}  // AndState 
