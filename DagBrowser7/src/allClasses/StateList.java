package allClasses;

import static allClasses.Globals.appLogger;

import java.awt.Color;
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

	
	/* Variables used for all states.  */

	protected StateList parentStateList= null; // Our parent state.

	protected Color theColor= UIColor.initializerStateColor;

  protected List<StateList> theListOfSubStateLists= // Our sub-states.
      new ArrayList<StateList>(); // Initially empty list.

  private String synchronousInputString; /* Temporarily stores an input event.
    It is the one place this state checks for synchronous input.
    Asynchronous input can appear in any number of other variables.
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

	/* Variables used only for OrState behavior.. */

	private static SentinelState theSentinelState= new SentinelState();
	  // This is used as an initial sentinel state which simplifies other code. 

  protected StateList presentSubStateList= // Machine's qualitative state.
  		StateList.theSentinelState; // Initial default no-op state.
      //// This could be null in AndStates.

  protected StateList requestedSubStateList= null; // Becomes non-null when 
  	// machine requests a new qualitative state, 
    // even if it is the same as the present state.


	/* Methods used to build state objects. */
  protected void initializeWithIOExceptionV() throws IOException
	  {
	  	initializeWithIOExceptionStateList();
		  }

  public StateList initializeWithIOExceptionStateList() throws IOException
    /* This method initializes this state object, 
      It does actions needed when this state object is being built.
      This is not the same as the entryV() method, which
      does actions needed when the associated state-machine state is entered.

			Like constructors, this method should be called first
			in the sub-class versions of this method.

      This version only calls 
      the no-IOException superclass initializeV() version.
  		*/
    {
  	  super.initializeV(); // IOExceptions are not thrown in super-classes.
  	  theColor= UIColor.initialStateColor;
  	  return this;
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

	
	/*  Methods for AndState behavior.  */ ////// Maybe add initialization.

	public synchronized boolean andStateHandlerB() throws IOException
	  /* This method handles AndState and 
	    state-machines that want to behave like AndState 
	    by cycling all of their sub-machines
	    until none of them makes any computational progress.
	    It scans the sub-machine in order until one makes computational progress.
	    Then it restarts the scan.
	    It is done this way to prioritized the sub-machines.
	    If one sub-machine produces something for which an earlier machine waits,
	    that earlier machine will be run next.
      It keeps scanning until none of the sub-machines in a scan make progress.
	    This method returns true if any computational progress was made
	    by any sub-machine, false otherwise.
	    */
	  { 
			throwDelayedExceptionV(); // Throw exception if one was saved.
		  boolean progressMadeB= false;
  	  substateScansUntilNoProgress: while(true) {
 	  		for (StateList subStateList : theListOfSubStateLists) 
	  	  	if (handleWithSynchronousInputB(subStateList))
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


	/* Methods for implementing OrState behavior.. */ ////// Maybe add initialization.

  public synchronized boolean orStateHandlerB() throws IOException
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
			throwDelayedExceptionV(); // Throw exception if one was saved.
	  	boolean stateProgressB= false;
			while (true) { // Cycle sub-states until done.
	  	    boolean substateProgressB= 
	  	    		handleWithSynchronousInputB(presentSubStateList);
		  		if (requestedSubStateList != null) // Handling state change request.
		  		  { presentSubStateList.exitV(); 
	    	  		presentSubStateList= requestedSubStateList;
		  			  presentSubStateList.enterV(); 
		  				requestedSubStateList= null;
							substateProgressB= true; // Count this as progress.
			  			}
		  	  if (!substateProgressB) // Exiting loop if no sub-state progress made.
			  	  { presentSubStateList.setBackgroundColorV( 
			  	  		UIColor.waitingStateColor 
			  	  		);
			  	  	break;
			  	  	}
	  	  	stateProgressB= true; // Accumulate sub-state progress in this state.
	  	  	} // while
			return stateProgressB; // Returning accumulated state progress result.
			}

	protected void requestStateListV(StateList nextStateList)
		/* This is called to change the state of a the state machine
		  that contains this state to the sibling state nextStateList.  
		  It does this by calling the parent state's 
		  setNextSubStateListV(..) method. 
		  It does not fully take affect until the present state handler exits.
		  */
		{
			parentStateList.requestSubStateListV(nextStateList);
			}

  protected void requestSubStateListV(StateList nextStateList)
	  /* This method requests the next state-machine state,
	    which is the same thing as this state's next sub-state.
	    
	    Note, though the requestedSubStateList variable changes immediately,
	    control is not transfered to the new sub-state until
	    the handler of the present sub-state exits.
	    
	    ////// Change to use presentSubStateList instead of requestedSubStateList. 
	    */
	  {
			if (requestedSubStateList != null) // Report excess request.
        appLogger.error(
        		"StateList.requestSubStateListV(..), excess state change request"
        	  );
  	  requestedSubStateList= nextStateList;
			}

	/*  Methods for entry and exit of OrState or their sub-states.  */
	
	public void enterV() throws IOException
	  /* This method is called when 
	    the state associated with this object is entered.
	    This version does nothing, but it should be overridden 
	    by state subclasses that require entry actions.
	    This is not the same as initialize*V(),
	    which does actions needed when the StateList object is being built
	    and is being prepared for providing its services.
	    */
	  { 
			setBackgroundColorV( UIColor.runningStateColor );
			}

	public void exitV() throws IOException
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
			}


	/* Methods containing general state handler code. */
	
	public synchronized void stateHandlerV() throws IOException
	  /* This is a code-saving method.
	    A state class overrides either this method or stateHandlerB()
	    as part of how it controls its behavior.
	    Overriding this method instead of stateHandlerB() can result in
	    more compact code.  An override like this:

				public synchronized void stateHandlerV() throws IOException
				  { 
				    some-code;
				    }

			is a more compact version of, but equivalent to, this:

				public synchronized boolean stateHandlerB() throws IOException
				  { 
				    some-code;
				    return false;
				    }

			As with stateHandlerB(), because this method can be called from
			multiple threads, such as timer threads, it and all sub-class overrides
			should be synchronized.

		  See stateHandlerB() .
	    */
	  { 
		  // This default version does nothing.
	    }
	
	public synchronized boolean overrideStateHandlerB() throws IOException
	  /* A state class overrides either this method or stateHandlerV()
	    to control how its state is handled.

	    This method does nothing except return false unless 
	    it or stateHandlerV() is overridden.
	    All overridden versions of this method should return 
	    * true to indicate that some computational progress was made, including:
		    * one or more sub-state's stateHandlerB() returned true,
		    * a synchronous input String was processed,
		    * requestSubStateListV(StateList nextState) was called
		      to request a new qualitative sub-state.
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

			Because this method can be called from multiple threads, 
			such as timer threads, it and all sub-class overrides
			should be synchronized.
	    
	    */
	  { 
			stateHandlerV(); // Call this in case it is overridden instead.
			return false; // This default version returns false.
		  }
	
	public synchronized final boolean finalStateHandlerB() throws IOException
	  /* This is the method that should be called to invoke a state's handler.
	    It can not be overridden.  
	    It calls override-able handler methods which state sub-classes override.
	    */
	  { 
			setBackgroundColorV( UIColor.runningStateColor );
			boolean successB= overrideStateHandlerB();
			colorBySuccessV( successB );
			return successB;
		  }


	/* Methods for exception handling.  */

	public void delayExceptionV( IOException theIOException )
		/* 	This method records an IOException that 
		  occurred in a handler that couldn't handle it,
		  such as a Timer thread.
		  It can be re-thrown later on a thread that can handle it,
		  by calling delayedExceptionCheckV(). 
		  */
		{ 
		  delayedIOException= theIOException;
		  }

	public void throwDelayedExceptionV() throws IOException
		/* 	This method re-throws any delayedIOException that
			might have occurred in a handler running on an earlier Timer thread. 
		  */
		{ 
		  if  // Re-throw any previously saved exception from timer thread.
		    (delayedIOException != null) 
		  	throw delayedIOException;
		  }


	/* Methods for dealing with synchronous input to state-machines.  */

	protected synchronized boolean handleWithSynchronousInputB( 
					StateList subStateList
					) 
		  throws IOException
		/* This method calls subStateList's handler while passing
		  any available synchronous input to it.
		  It returns true if the sub-state's handler returned true 
		    or a synchronous input was consumed by the sub-state.
			It returns false otherwise.
			If a synchronous input was consumed by the sub-state,
			then it is erased from this StateList also so that 
			it will not be processed by any other states.
			*/
		{
			boolean madeProgressB;
			String inputString= getSynchronousInputString();
			if ( inputString == null ) // Synchronous input not present.
				madeProgressB= subStateList.finalStateHandlerB();
				else // Synchronous input is present.
				{
					subStateList.setSynchronousInputV(inputString);
				  	// Store copy of synchronous input, if any, in sub-state.
					madeProgressB= subStateList.finalStateHandlerB();
					if // Process consumption of synchronous input, if it happened.
						(subStateList.getSynchronousInputString() == null)
						{ setSynchronousInputV(null); // Remove input from this state.
							madeProgressB= true; // Treat input consumption as progress.
							}
					}
			if ( madeProgressB )
				setBackgroundColorV( UIColor.runnableStateColor );
				else
				setBackgroundColorV( UIColor.waitingStateColor );
			return madeProgressB;
			}
	
	public final synchronized boolean finalHandleSynchronousInputB(
				String inputString
				) 
			throws IOException
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
			setBackgroundColorV( UIColor.runningStateColor );
			boolean successB=  // Call regular handler to process it.  Return value
					overrideStateHandlerB(); 
			  // is ignored because we are interested in String processing.
			successB|= // Combine success with result of synchronous input processing. 
					(synchronousInputString == null);
			synchronousInputString= null; // Remove input to from field variable.
			colorBySuccessV( successB );
			return successB; // Returning whether input was processed.
		  }

	private void colorBySuccessV( boolean successB )
		{ if ( successB )
			setBackgroundColorV( UIColor.runnableStateColor );
			else
			setBackgroundColorV( UIColor.waitingStateColor );
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
	

	/* Method for dealing with timer input.  */

	public void run()
	  /* This method is run by TimerInput to run 
	    the stateHandlerB() method when the timer is triggered.
	    If an IOException occurs then it is saved for processing later by
	    a non-Timer thread.
	    */
		{
			try { 
				finalStateHandlerB(); // Try to process timer event with handler. 
				}
		  catch ( IOException theIOException) { 
		    delayExceptionV( // Postpone exception processing to other thread.
		    		theIOException
		    		); 
		    }
		}

	/* Methods for UI coloring.  */
	
  void setBackgroundColorV( Color theColor )
    /* This method sets the background color 
      which should be used to display this State.
      If the color changes then it also displays it.
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
      Presently it is the value stored in variable theColor.
     */
    {
  		return theColor;
  	  }

	}  // StateList class 


	class SentinelState extends StateList {
		
		/* This class overrides enough non-no-op methods in StateList
		  that need to be no-ops so that this class can be used
		  as an initial sentinel-state for OrState state machines.
		  By doing this the actual desired first state 
		  can be requested in normal way,
		  after being requested with requestStateListV(..).
		 	*/

    protected void reportChangeOfSelfV()
	  	{
	  		// Do nothing because this state is not part of display-able DAG.
	  		}

		} // class SentinelState

	class OrState extends StateList {

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
  	  super.initializeWithIOExceptionStateList();
  	  theColor= UIColor.initialOrStateColor;
  	  return this;
    	}

  public synchronized boolean overrideStateHandlerB() throws IOException
	  /* This method calls the default OrState handler.  */
	  { 
  		return orStateHandlerB(); 
  		}

	}  // OrState 

class AndState extends StateList {

	/* There are two ways to get "and" state-machine behavior.
	  * Extend this class.
	  * Extend StateList but call the "and" state handler method.

	  AndState machines have sub-states, but unlike OrStates,
	  all AndState sub-states are active at the same time.
	  There is concurrency in an AndState machine, at least at this level.
	  */

  public StateList initializeWithIOExceptionStateList() throws IOException
    {
  	  super.initializeWithIOExceptionStateList();
  	  theColor= UIColor.initialAndStateColor;
  	  return this;
    	}

	public synchronized boolean overrideStateHandlerB() throws IOException
		{ 
		  return andStateHandlerB(); 
		  }

	}  // AndState 
