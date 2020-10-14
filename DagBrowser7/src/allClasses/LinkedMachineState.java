package allClasses;

import java.awt.Color;
import java.io.IOException;
import java.util.Timer;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import static allClasses.AppLog.theAppLog;

public class LinkedMachineState

    extends OrState

/*
 * This class contains a [hierarchical] state machine that processes the HELLO
 * handshake that is supposed to happen at the beginning of a Unicaster
 * connection, and the GOODBYE message that ends a Unicaster connection.
 * 
 * This state machine needs redesign to be general enough to handle all of the
 * following inputs: HELLO GOODBYE Any unicaster packet from the remote peer.
 * Any multicaster packet from the remote peer. interrupt() request used to
 * terminate it.
 * 
 */

{
  // Injected dependencies.
  @SuppressWarnings("unused")
  private NetcasterInputStream theNetcasterInputStream;
  private NetcasterOutputStream theNetcasterOutputStream;
  private NamedLong initialRetryTimeOutMsNamedLong;
  private TCPCopier theTCPCopier;
  @SuppressWarnings("unused") /// opt
  private Timer theTimer; /// opt. use function parameter only.
  private ScheduledThreadPoolExecutor theScheduledThreadPoolExecutor;
  private Unicaster theUnicaster;
  private Persistent thePersistent;
  private PeersCursor thePeersCursor;
  @SuppressWarnings("unused") /// opt Remove eventually.
  private NotifyingQueue<MapEpiNode> toConnectionManagerNotifyingQueueOfMapEpiNodes;
  // For inputs in the form of MapEpiNodes.
  private ConnectionManager theConnectionManager;

  // Other variables: none.
  private MapEpiNode theMapEpiNode;
  private String theUserIdString;

  // Sub-state-machine instances.
  private DisconnectedState theDisconnectedState;
  private ExponentialRetryConnectingState theExponentialRetryConnectingState;
  private SlowPeriodicRetryConnectingState theSlowPeriodicRetryConnectingState;
  private ConnectedState theConnectedState;

  LinkedMachineState( // Constructor.
  ) throws IOException {
  }

  public synchronized LinkedMachineState initializeWithIOExceptionLinkedMachineState(Timer theTimer,
      ScheduledThreadPoolExecutor theScheduledThreadPoolExecutor, NetcasterInputStream theNetcasterInputStream,
      NetcasterOutputStream theNetcasterOutputStream, NamedLong initialRetryTimeOutMsNamedLong, TCPCopier theTCPCopier,
      Unicaster theUnicaster, Persistent thePersistent, PeersCursor thePeersCursor,
      LinkMeasurementState theLinkMeasurementState,
      NotifyingQueue<MapEpiNode> toConnectionManagerNotifyingQueueOfMapEpiNodes, ConnectionManager theConnectionManager)
      throws IOException {
    super.initializeWithIOExceptionStateList();

    // Injected dependencies.
    this.theTimer = theTimer;
    this.theScheduledThreadPoolExecutor = theScheduledThreadPoolExecutor;
    this.theNetcasterInputStream = theNetcasterInputStream;
    this.theNetcasterOutputStream = theNetcasterOutputStream;
    this.initialRetryTimeOutMsNamedLong = initialRetryTimeOutMsNamedLong;
    this.theTCPCopier = theTCPCopier;
    this.theUnicaster = theUnicaster;
    this.thePersistent = thePersistent;
    this.thePeersCursor = thePeersCursor;
    this.toConnectionManagerNotifyingQueueOfMapEpiNodes = toConnectionManagerNotifyingQueueOfMapEpiNodes;
    this.theConnectionManager = theConnectionManager;
    // Adding measurement count.

    // Initialize other variables.
    theMapEpiNode = thePeersCursor.getSelectedMapEpiNode();
    theUserIdString = theMapEpiNode.getEmptyOrString(Config.userIdString);

    // Construct all sub-states of this state machine.
    theDisconnectedState = new DisconnectedState();
    theExponentialRetryConnectingState = new ExponentialRetryConnectingState();
    theSlowPeriodicRetryConnectingState = new SlowPeriodicRetryConnectingState();
    theConnectedState = new ConnectedState();

    // Initialize add to DAG all sub-states of this state machine.
    // Not all sub-states get default initialization.
    initAndAddStateListV(theDisconnectedState);
    initAndAddStateListV(theExponentialRetryConnectingState);
    initAndAddStateListV(theSlowPeriodicRetryConnectingState);
    addStateListV(theConnectedState.initializeWithIOExceptionStateList(theLinkMeasurementState));

    theConnectedState.setTargetDisconnectStateV(theSlowPeriodicRetryConnectingState);

    setFirstOrSubStateV(theDisconnectedState); // Initial state is Disconnected.

    theTimerInput = // Creating our timer and linking it to this state.
        new TimerInput(
            /// opt this.theTimer,
            this.theScheduledThreadPoolExecutor, this);

    /*
     * ///dbg { ///dbg adjust logging for debugging. propagateIntoSubtreeB(
     * LogLevel.TRACE ); theConnectedState.propagateIntoDescendantsV(LogLevel.INFO);
     * }
     */ /// dbg

    return this;
  }

  public synchronized void finalizeV() throws IOException
  // This method processes any pending loose ends before app shutdown.
  {
    super.finalizeV();
    theTimerInput.cancelingV(); // To stop our timer.
  }

  private long debugMessageCountL = 0;

  @SuppressWarnings("unused")
  private void sendDebugCountV() throws IOException
  /*
   * This method sends a DEBUG message followed by the debug message count to the
   * remote peer. Its purpose is to make logs of packets easier to interpret
   * during debugging. It does not flush the buffer to send a packet. It should be
   * called before a regular message which does flush the buffer. The receiver
   * should silently ignore this message and its argument.
   */
  {
    theNetcasterOutputStream.writeV("{DEBUG:{N:" + debugMessageCountL + "}}");
    debugMessageCountL++;
  }

  public void onEntryV() throws IOException {
    super.onEntryV();
    retryTimeOutMsL = // Initializing retry time-out.
        initialRetryTimeOutMsNamedLong.getValueL();
  }

  public boolean onInputsB() throws IOException
  /*
   * This input handler method is mainly concerned with disconnecting its
   * Unicaster from the one running on the remote peer node. It lets its
   * descendant states handle everything else. It interprets a thread interrupt as
   * a termination request and responds by requesting a sub-state change to
   * theDisconnectedState if that state is not already active.
   */
  {
    if (tryInputB("Shutdown")) // Process any local termination/shutdown request.
      if (isConnectedB()) { // Disconnect if connected.
        theAppLog.debug("UCLinkedMachine",
            "LinkedMachineState.onInputsB() disconnecting for shutdown.");
        processInputB("Disconnect"); // Now cause disconnect.
      }
    boolean returnB = // Try processing in OrState machine of superclass.
        super.onInputsB();
    return returnB;
  }

  public void onExitV() throws IOException
  // Cancels acknowledgement timer.
  {
    theTimerInput.cancelingV();
    super.onExitV();
  }

  // Other variables.
  private TimerInput theTimerInput;
  private long retryTimeOutMsL;

  public boolean isConnectedB()
  /*
   * This method returns true if this Unicaster is connected to its peer, 
   * false otherwise.
   */
  {
    return (getpresentSubStateList() == theConnectedState);
  }

  public boolean isDisconnectedB()
  /*
   * This method returns true if this Unicaster is Disconnected from its peer, 
   * false otherwise.
   */
  {
    return (getpresentSubStateList() == theDisconnectedState);
  }

  /*
   * The following are the states used for managing Unicaster connecting. There
   * are 2 states worthy of note InitiatingConnectState: The machine is put into
   * this state when a Unicaster is created for the first time, in response to a
   * multicast message from the peer. It makes use of an aggressive exponential
   * back-off of HELLO messages in an effort to connect to the other peer.
   */

  private class DisconnectedState extends StateList

  /*
   * This is an important state. It is the first state after app startup. It is
   * the last state before app shutdown.
   * 
   * This state does not use a timer, so it make no attempt on its own to end the
   * state.
   * 
   * This state will end only when it receives a request to do so, which could be:
   * A HELLO message received from the peer. A local Connect request received from
   * ConnectionManager.
   * 
   * This state will silently ignore any received GOODBYE message.
   */

  {

    public void onEntryV() throws IOException {
      super.onEntryV();
      }

    public void onInputsToReturnFalseV() throws IOException
    /*
     * This method does a connect if either a HELLO message or a Connect request is
     * received. It also absorbs and ignores GOODBYE messages.
     */
    {
      goReturn: {
        if (tryReceivingHelloB(this)) { // Connect requested from remote peer.
          if (theMapEpiNode.isTrueB("ignorePeer"))
            theAppLog.debug("UCLinkedMachine",
                "LinkedMachineState.onInputsToReturnFalseV() ignorePeer:true.");
          else {
            sendHelloV(this); // Send a response HELLO.
            requestAncestorSubStateV(theConnectedState); // Become connected.
            }
          break goReturn;
          }
        if (tryInputB("ExponentialRetryConnect")) { // Local connect requested.
          theAppLog.debug("UCLinkedMachine",
              "DisconnectedState.onInputsToReturnFalseV() ExponentialRetryConnect.");
          sendHelloV(this); // Send initial HELLO.
          requestAncestorSubStateV(theExponentialRetryConnectingState);
          break goReturn;
          } 
        if (tryInputB("SlowPeriodicRetryConnect")) { // Local connect requested.
          theAppLog.debug("UCLinkedMachine",
              "DisconnectedState.onInputsToReturnFalseV() SlowPeriodicRetryConnect.");
          requestAncestorSubStateV(theSlowPeriodicRetryConnectingState);
          break goReturn;
          }
        if (tryInputB("GOODBYE")) { // Ignore any redundant GOODBYE message.
          // appLogger.info("LinkedMachineState.onInputsToReturnFalseV() GOODBYE received
          // and ignored while in DisconnectedState.");
          break goReturn;
          }
    } // goReturn:
      return;
    }

    Color getBackgroundColor(Color defaultBackgroundColor) {
      return activityBasedBackgroundColor(UIColor.activeWaitingWithoutLimitStateColor);
    }

  } // class DisconnectedState

  private class ExponentialRetryConnectingState extends StateList

  /*
   * This class assumes that a HELLO message has already been sent to the remote
   * peer and tries to receive one from the remote peer. It retransmits HELLOs if
   * a HELLO is not received, using exponential back-off. It will give up trying
   * if no HELLO is received.
   */

  {

    public void onEntryV() throws IOException {
      super.onEntryV();
      theTimerInput.scheduleV(retryTimeOutMsL);
    }

    public void onInputsToReturnFalseV() throws IOException {
      if (tryReceivingHelloB(this)) // Try to process HELLO.
        requestAncestorSubStateV( // Success. Request connected state.
            theConnectedState);
      else if // Try handling time-out?
      (theTimerInput.testInputArrivedB()) {
        final long timeOutLimitMsL = initialRetryTimeOutMsNamedLong.getValueL() * 40;
        // was Config.maxTimeOutMsL;
        theAppLog.appendToFileV("[" + theTimerInput.getLastDelayMsL() + "ms time-out after HELLO]");
        boolean limitReachedB = // Reschedule time-out with exponential back-off
            (theTimerInput.rescheduleB(timeOutLimitMsL)); // up to this limit.
        if (!limitReachedB) // Not at max time-out so
          sendHelloV(this); // re-send hello and stay in this state.
        else // Time-out limit was reached.
        { // End exponential back off by reporting it and changing state.
          Anomalies.displayDialogV(
              "Time-out limit of " + timeOutLimitMsL + " ms was reached, while in" 
              + getFormattedStatePathString());
          requestAncestorSubStateV( // Switch to different type of retrying.
              theSlowPeriodicRetryConnectingState);
        }
      }
    }

    Color getBackgroundColor(Color defaultBackgroundColor) {
      return activityBasedBackgroundColor(UIColor.activeAfterTimeOutStateColor);
    }

  } // class ExponentialRetryConnectingState

  private class SlowPeriodicRetryConnectingState extends StateList

  /*
   * This class assumes that a HELLO message has already been sent to the remote
   * peer and tries to receive one from the remote peer. It does not retransmit
   * HELLOs if a HELLO is not received. It will give up trying if no HELLO is
   * received.
   */

  {

    public void onEntryV() throws IOException {
      super.onEntryV();

      sendHelloV(this); // Initial HELLO for this state. Done here because
      // some predecessor states (ConnectedState sub-states) can't do it.
      
      AppMapEpiNode.updateFieldV(theMapEpiNode, "reconnectType", "periodicRetry");

      theTimerInput.scheduleV(Config.slowPeriodicRetryTimeOutMsL);
    }

    public void onInputsToReturnFalseV() throws IOException {
      if (tryReceivingHelloB(this)) // Try to process HELLO.
        { // Success. Move to ConnectedState.
          sendHelloV(this); // Send one back in case HELLO came from peer in same state.
          Anomalies.displayDialogV("A broken connection has been reestablished, from" 
              + getFormattedStatePathString());
          requestAncestorSubStateV(theConnectedState); // Request connected state.
          }
      else if (theTimerInput.testInputArrivedB()) // Time-out?
        { // Send another HELLO and keep waiting.
          sendHelloV(this);
          theTimerInput.scheduleV( // Restart timer.
              Config.slowPeriodicRetryTimeOutMsL);
          }
      else if (tryInputB("ExponentialRetryConnect")) 
        { // Local connect requested.
          theAppLog.debug("UCLinkedMachine",
              "SlowPeriodicRetryConnectingState.onInputsToReturnFalseV() ExponentialRetryConnect.");
          sendHelloV(this); // Send initial HELLO.
          requestAncestorSubStateV(theExponentialRetryConnectingState);
          }
    }

    Color getBackgroundColor(Color defaultBackgroundColor) {
      return activityBasedBackgroundColor(UIColor.activeInErrorStateColor);
    }

  } // class SlowPeriodicRetryConnectingState

  private class ConnectedState extends AndState

  /*
   * This is a special state. Being in this state means we are connected to the
   * peer node. This is the state in which inter-peer communication happens.
   * 
   * This state handles the reception of any extra HELLO messages, which are HELLO
   * messages received after the first one. Receiving HELLO messages here means
   * that the remote peer did not receive the HELLO or HELLOs sent by us earlier.
   * 
   * This state also contains the sub-state LinkedMeasurementState. This is used
   * to monitor the health of the link to the peer, measuring packet losses and
   * round-trip-time.
   * 
   * Finally this state handles several conditions which can cause the state
   * machine to exit this state.
   */

  {
    private LinkMeasurementState theLinkMeasurementState;

    public StateList initializeWithIOExceptionStateList(LinkMeasurementState theLinkMeasurementState) throws IOException
    /*
     * This method initializes the sub-states, building the sub-state-machine.
     * Presently there is only one.
     */
    {
      this.theLinkMeasurementState = theLinkMeasurementState;

      super.initializeWithIOExceptionStateList();

      addStateListV(this.theLinkMeasurementState);

      return this;
    }

    public void setTargetDisconnectStateV(StateList theBrokenConnectionState)
    /*
     * This method tells the LinkMeasurementState what state to request if it
     * appears that the communication link to the peer is broken.
     */
    {
      theLinkMeasurementState.setTargetDisconnectStateV(
          theSlowPeriodicRetryConnectingState);
      }

    public void onEntryV() throws IOException
    /*
     * Informs TCPCopier about this new connection, which means a possible TCPServer
     * to try. It also records peer information in the Persistent storage for faster
     * connecting after app restart.
     */
    {
      super.onEntryV();

      AppMapEpiNode.updateFieldV(theMapEpiNode, "isConnected", true); // Record connection.
      AppMapEpiNode.updateFieldV(theMapEpiNode, "reconnectType", "exponentialRetry");
      theAppLog.info("ConnectedState.onEntryV() "
          + "Connecting, notifying ConnectionManager with: \n  "
          + thePeersCursor.getSelectedMapEpiNode());
      notifyConnectionManagerOfPeerConnectionChangeV();

      IPAndPort remoteIPAndPort = theUnicaster.getKeyK();
      theTCPCopier.queuePeerConnectionV(remoteIPAndPort);
    }

    private boolean sentHelloB = true;
    // True means previous HELLO was sent by us, not received by us.
    // It is used by onInputsB() to prevent HELLO message storms.

    public boolean onInputsB() throws IOException
    /*
     * This method sends HELLO messages in response to extra received HELLO
     * messages. It calls the sub-state handler for message processing. It also
     * decodes various messages such as GOODBYE that can cause the state machine to
     * exit this state. To prevent HELLO storms a response is made to only every
     * other received HELLO. This 1/2 ratio might be overkill. 1/3 or 1/4 might be
     * better.
     */
    {
      boolean signalB = true; // Assume signal will be produced.
      goReturn: {
        if (super.onInputsB()) // Try processing in sub-state machine.
          break goReturn; // Return with signal true.
        if (tryReceivingHelloB(this)) { // Try to process an extra HELLO.
          theAppLog.debug("UCLinkedMachine","Extra HELLO received.");
          if // If we received a HELLO and
          (sentHelloB ^= true) { // we didn't send one last time,
            sendHelloV(this); // send a HELLO this time.
            requestAncestorSubStateV(this); // Reenter this to reset sub-states.
          }
          break goReturn; // Return with signal true.
          }
        if (tryInputB("GOODBYE")) { // Peer disconnected itself by saying goodbye?
          sayGoodbyesV(); // Respond to peer with our own goodbyes.
          notifyConnectionManagerOfPeerConnectionChangeV();
          AppMapEpiNode.updateFieldV(theMapEpiNode, "reconnectType", null);
          requestAncestorSubStateV(theDisconnectedState); // Disconnect ourselves.
          break goReturn; // Return with signal true.
          }
        if (tryInputB("Disconnect")) { // Disconnect requested (for our shutdown)?
          sayGoodbyesV(); // Inform peer.
          requestAncestorSubStateV(theDisconnectedState); // Disconnect ourselves.
          break goReturn; // Return with signal true.
          }
        if (tryInputB("Skipped-Time")) { // We just woke up from sleep.
          sendHelloV(this); // send a HELLO to reestablish connection.
          requestAncestorSubStateV( // Yes, so assume connection broke
              theExponentialRetryConnectingState); // and try to reestablish.
          break goReturn; // Return with signal true.
          }
        signalB = false; // Everything failed. Set no signal.
      } // goReturn:
      return signalB;
    }

    public void onExitV() throws IOException {
      theAppLog.info("ConnectedState.onExitV() Disconnecting");
      theMapEpiNode.removeV("isConnected"); // Record disconnection.
      super.onExitV();
      }

    Color getBackgroundColor(Color defaultBackgroundColor) {
      return activityBasedBackgroundColor(UIColor.activeBeforeTimeOutStateColor);
      }

  } // class ConnectedState

  private boolean tryReceivingHelloB(StateList subStateList) throws IOException
  /*
   * This method tries to receive and process the Hello message, including its
   * arguments, IP and UserId. This method is part of this state class, but is not
   * called by its own code. It is called by one of its sub-states, which must be
   * specified with the parameter subStateList.
   * 
   * If the next input to the sub-state is "HELLO" then it processes it, which
   * means parsing the IP address which follows and determining which peer, the
   * local or remote, will be leader, by comparing the IP addresses, and setting
   * the value for leadingDefaultBooleanLike. Note, the particular ordering of IP
   * address Strings doesn't matter. What matters is that the ordering is
   * consistent. One peer will decide it is the leader; the other peer, the
   * follower.
   * 
   * This method does not send a reply "HELLO". Either a "HELLO" must be sent
   * after this method returns, or a "HELLO" should be sent before this method is
   * called.
   * 
   * This method returns true if HELLO is received and processed, false otherwise.
   * 
   * ///pos This method could also select a leader based on UserId instead of IP
   * address.
   */
  {
    boolean gotGoodHelloB = false; // Assume default of failure.
    toReturn: {
      /*  ////
      if (!subStateList.tryInputB("HELLO"))
        break toReturn; // Fail if not HELLO.
      String localIpString = theNetcasterInputStream.readAString();
      String useIdString = theNetcasterInputStream.readAString();
      */  ////
      MapEpiNode valueMapEpiNode= subStateList.tryInputMapEpiNode("HELLO");
      if (null == valueMapEpiNode) // If offered input not a HELLO message
        break toReturn; // exit with failure.
      subStateList.helloResetV(); //// debug.
      // theAppLog.debug("tryReceivingHelloB((.), fields: "+valueMapEpiNode
      //    + "from" + subStateList.getFormattedStatePathString() );
      String localIpString = valueMapEpiNode.getString("IP");
      String userIdString = valueMapEpiNode.getString("UserId");
      if (!processUserIdB(userIdString)) // Process Id.
        break toReturn; // or exit if fail.
      String remoteIpString = theUnicaster.getKeyK().getInetAddress().getHostAddress();
      theUnicaster.leadingDefaultBooleanLike.setValueB( // Decide who leads.
          localIpString.compareTo(remoteIpString) > 0);
      /// dbg leadingDefaultBooleanLike= !leadingDefaultBooleanLike; // Reverse roles
      /// for debug test.
      theAppLog.debug("UCLinkedMachine",
          "HELLO received.  Setting or overriding role to be: "
          + (theUnicaster.leadingDefaultBooleanLike.getValueB() ? "LEADER" : "FOLLOWER"));
      gotGoodHelloB = true; // Tests passed. Exit with success.
    } // toReturn:
    return gotGoodHelloB;
  }

  private boolean processUserIdB(String inUserIdString)
  /*
   * This method is a kludge to handle the problem of Unicasters being created
   * before the UserId of the remote peer is known. The ConnectionManager thread
   * does this when it creates Unicasters.
   * 
   * If this Unicaster's RootId has not yet been defined entries exist with and
   * without peer identity, This method searches the Persistent storage cache for
   * entries matching this Unicaster. If entries exist with and without peer
   * identity, then it combines them into a single entry.
   * 
   * It returns true if an acceptable RootId was processed, false otherwise.
   */
  {
    boolean successB = false; /// This is always overridden!
    goReturn: {
      if // Same IDs, so subject peer is actually the local peer,
      (testForOurUserIdB(inUserIdString)) { // so ignore this HELLO.
        theAppLog.warning("LinkedMachineState.processRootIdB(String) " + "HELLO came from me, ignoring.");
        theMapEpiNode.putV("ignorePeer", "true");
        theMapEpiNode.putV("ID_WARNING", "This is local peer");
        break goReturn; // so exit with failure.
      }
      IPAndPort remoteIPAndPort = theUnicaster.getKeyK();
      String ipString = remoteIPAndPort.getIPString(); // Extract IP.
      String portString = String.valueOf(remoteIPAndPort.getPortI()); // Extract port.
      PeersCursor anotherPeersCursor = PeersCursor.makeOnNoEntryPeersCursor(thePersistent);
      anotherPeersCursor.findPeerV( // Find match using all 3 values.
          ipString, portString, inUserIdString);
      if (anotherPeersCursor.getEntryKeyString().isEmpty()) // Not found.
      { // So store identity in this Unicaster's data entry and in easy-access copy.
        theAppLog.info("LinkedMachineState.processUserIdB(String) Saving identity.");
        theMapEpiNode.putV(Config.userIdString, inUserIdString);
        successB = true;
        break goReturn;
      }
      // Found entry with identity. No need to store it in entry.

      if // The found entry is not this Unicaster's entry.
      (!thePeersCursor.getEntryKeyString().equals(anotherPeersCursor.getEntryKeyString())) { // Eliminate one entry, the
        theAppLog.info(
            "LinkedMachineState.processUserIdB(String) " + "replacing Unicasters new peer entry with old one with ID.");
        thePeersCursor.removeEntryV(); // Delete new entry without ID.
        thePeersCursor = // Replace this Unicaster's PeersCursor with
            anotherPeersCursor; // the one that found the entry with the identity.
        successB = true;
        break goReturn;
      }
      theAppLog.debug("UCLinkedMachine",
          "LinkedMachineState.processUserIdB(String) "
          + "Found entry is Unicaster's entry.  Using  it.");
      successB = true;
    } // goReturn:
    return successB;
  }

  private boolean testForOurUserIdB(String inIdentityString) {
    boolean resultB = thePersistent.getEmptyOrString(Config.userIdString).equals(inIdentityString);
    if (resultB)
      theAppLog.warning("LinkedMachineState.thisIsOurUserIdB(.) UNICASTER IS US!");
    return resultB;
  }

  private int helloCountI= 0;  ////
  
  private void sendHelloV(StateList subStateList) throws IOException
  /*
   * This method sends a HELLO message to the remote peer from state subStateList,
   * and logs that it has done so. The HELLO message includes the IP address of
   * the remote peer and the ID of the local peer.
   */
  {
    synchronized (theAppLog) { ////
      // theAppLog.debug(
      //    "sendHelloV() from" + subStateList.getFormattedStatePathString() );
      // theAppLog.doStackTraceV(null);
      }
    
    //// sendDebugCountV(); // This is 1st block.

    MapEpiNode fieldsMapEpiNode= new MapEpiNode(); // Make empty map.
    fieldsMapEpiNode.putV( // Add IP.
        "IP", theUnicaster.getKeyK().getInetAddress().getHostAddress());
    fieldsMapEpiNode.putV( // //// for debugging.
        "HelloCount", helloCountI); ////
    fieldsMapEpiNode.putV( // Add UserId.
        Config.userIdString, thePersistent.getEmptyOrString(Config.userIdString));
    MapEpiNode helloMapEpiNode= MapEpiNode.makeSingleEntryMapEpiNode( // Wrap in HELLO map.
        "HELLO", fieldsMapEpiNode);
    helloMapEpiNode.writeV(theNetcasterOutputStream); // Write complete map as 2nd block.
    
    theNetcasterOutputStream.endBlockAndSendPacketV(); // Forcing send of both blocks.
  }

  private void sayGoodbyesV() throws IOException
  /* This method sends 3 GOODBYEs, each in a separate packet. */
  {
    for (int i = 0; i < 3; i++) { // Send 3 GOODBYE packets.
      //// theNetcasterOutputStream.writeV("{GOODBYE}");
      //// theNetcasterOutputStream.flush();
      MapEpiNode messageMapEpiNode= 
          MapEpiNode.makeSingleEntryEmptyMapEpiNode("GOODBYE");
      messageMapEpiNode.writeV(theNetcasterOutputStream);
        theNetcasterOutputStream.endBlockAndSendPacketV();
      }
  }

  private void notifyConnectionManagerOfPeerConnectionChangeV()
  /*
   * This method notifies the ConnectionManager about a change in the
   * connectedness status of this Unicaster. It does this by creating a
   * single-entry MapEpiNode wrapping the MapEpiNode data of the peer connection.
   */
  {
    MapEpiNode messageMapEpiNode = MapEpiNode.makeSingleEntryMapEpiNode("LocalNewState",
        thePeersCursor.getSelectedMapEpiNode());
    theConnectionManager.decodePeerMapEpiNodeV(messageMapEpiNode, theUserIdString // Unicaster UserId as context.
    ); // Decode it.
  }

} // class LinkedMachineState
