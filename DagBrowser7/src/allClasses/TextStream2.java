package allClasses;


import static allClasses.AppLog.theAppLog;
import static allClasses.SystemSettings.NL;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.PlainDocument;
import javax.swing.text.Segment;
import javax.swing.tree.TreePath;

public class TextStream2

  extends KeyedStateList< String > 

  /* This class represents a single text stream.
   * 
   * A user node can take on one or more of the following roles:
   * 
   * * subscribee: This is a user node that is a producer of content.  
   *   It receives requests for text and text acknowledgments from others.
   *   It sends text to others.  All TextStream messages contain
   *   an explicit subscribee UserId.
   *   
   * * subscriber; This is a user node that is a consumer of content.  
   *   It sends requests for text and text acknowledgments to others.
   *   It receives text from others.  
   *   A subscriber can also act like a subscribee if it already has
   *   some of the subscribee's content text, thereby functioning as
   *   an alternative source of that text.  Ideally, most content
   *   provided to subscribers will be provided by fellow subscribers.
   *   
   * * starter, [also/previously known as source, requester, or initiator]:
   *   This is a user node that initiates a subscription operation, either by:
   *   * being the source of a message which triggers an operation,
   *     meaning the remote node at the other end of the Unicaster link
   *     on which the message was received, or
   *   * being the caller of a method that performs an operation,
   *     meaning the local node.
   *   The identity of the starter is important because it's used to determine 
   *   whether a subscriber should receive text or a text acknowledgment
   *   
   *   A subscription operation always involves:
   *   * 1 subscribee
   *   * 1 or more subscribers
   *   * 1 starter
   *   The starter is either the subscribee or 
   *   one of the subscribee's subscribers.
   */
  
  {
  
    // Variables.
  
      // Injected variables:
      private Persistent thePersistent;
      private TextStreams2 theTextStreams2;

      // Other variables:
      private PlainDocument thePlainDocument= null; // Internal document store.
        // This has some EDT (Event Dispatch Thread) restrictions.
      private File streamFile; // File name of external document store. 
      private MapEpiNode subscribeeUserIdsMapEpiNode; 
        // Map of subscribee UserIds.
      private String subscribeeUserIdString;
      private MapEpiNode subscribeeMapEpiNode;
        // Map of subscribee fields.
      private MapEpiNode subscriberUserIdsMapEpiNode;
        // Map of subscriber UserIds.
      ///opt Some of the above maps might be created even though
        // they contain no data.  Can this be avoided?
        // Maybe just remove map entries with empty map values on exit?

    // Constructors.

      TextStream2( 
          String peerIdentityString,
          Persistent thePersistent,
          TextStreams2 theTextStreams2
          )
        { 
          super( // Superclass KeyedStateList<String> constructor injections.
              "TextStream", // Type name but not entire name.
              peerIdentityString // key
              );
          theAppLog.debug("TextStream2.TextStream(.) called.");
          this.thePersistent= thePersistent;
          Nulls.fastFailNullCheckT(theTextStreams2);
          this.theTextStreams2= theTextStreams2;
          
          // Calculate and cache some values we might need later.
          subscribeeUserIdsMapEpiNode= // Calculate map of subscribees.
              thePersistent.getOrMakeMapEpiNode("SubscribeeUserIds");
          subscribeeUserIdString= getKeyK(); // Get our UserId.
          subscribeeMapEpiNode= // Calculate map of our subscribee data.
              subscribeeUserIdsMapEpiNode.getOrMakeMapEpiNode(
                  subscribeeUserIdString);
          subscriberUserIdsMapEpiNode= // Calculate map of our subscribers.
              subscribeeMapEpiNode.getOrMakeMapEpiNode("SubscriberUserIds");
              
          streamFile= FileOps.makePathRelativeToAppFolderFile(
              Config.textStream2FolderString,getKeyK(),"textStreamFile.txt"
              ); // Create file path name.
          loadDocumentV(streamFile); // Load document from file.
          }

    // theFile pass-through methods.

    // A subset of delegated DataTreeModel methods.

      public boolean isLeaf( )
        {
          return true;
          }

      // Methods about returning Strings about the node.

      public boolean isDecoratingB()
        /* Disables DataNode String decoration because we 
          don't want that StateList feature.
          */
        {
          return false;
          }

    // other interface DataNode methods.

      public JComponent getDataJComponent( 
          TreePath inTreePath, 
          DataTreeModel inDataTreeModel 
          )
        {
          theAppLog.debug("TextStream2.getDataJComponent(.) called.");
          JComponent resultJComponent= 
            new TextStream2Viewer( 
              inTreePath, 
              inDataTreeModel,
              thePlainDocument,
              this,
              subscribeeUserIdString,
              thePersistent
              );
          return resultJComponent;  // return the final result.
          }
          
    // other methods.
      
      private void loadDocumentV( File theFile )
        /* This method loads the stream document Area with
          the contents of the external text file whose name is fileString.
          */
        {
          theAppLog.info("TextStream2.loadStreamV(..) begins.");
          BufferedReader theBufferedReader= null; 
          try {
              thePlainDocument= new PlainDocument();
              FileInputStream theFileInputStream= 
                new FileInputStream( theFile );
              theBufferedReader = 
                new BufferedReader(new InputStreamReader(theFileInputStream));
              String lineString;
              while ((lineString = theBufferedReader.readLine()) != null) {
                thePlainDocument.insertString( // Append line to document.
                    thePlainDocument.getLength(),lineString + "\n",null);
                }
              }
            catch (BadLocationException theBadLocationException) { 
              theAppLog.exception("TextStream2.loadStreamV(..) ",theBadLocationException);
              }
            catch (FileNotFoundException theFileNotFoundException) { 
              theAppLog.info("TextStream2.loadStreamV(..) "
                + theFileNotFoundException);
              }
            catch (IOException theIOException) { 
              theAppLog.exception("TextStream2.loadStreamV(..)", theIOException);
              }
            finally {
              try {
                if ( theBufferedReader != null ) theBufferedReader.close();
                }
              catch ( IOException theIOException ) { 
                theAppLog.exception("TextStream2.loadStreamV(..)", theIOException);
                }
              }
          theAppLog.info("TextStream2.loadStreamV(..) ends.");
          }

      public void requestNextTextFromAllSubscribersV()
        /* Notifies connected subscribers that 
         * we would like to receive the next text available
         * for this subscribee, unless this is the local subscribee.
         * It does this by sending an acknowledgement of 
         * the text it has received so far.
         * This includes the subscribee, which is considered to be
         * a subscriber to itself.
         * This method is called when a viewer opens any non-local text stream.
         */
        {
          theAppLog.debug(
              "TextStream2.requestNextTextFromAllSubscribersV() called.");

          // Notifiy all the regular subscribers.
          Set<EpiNode> subscriberUserIdsSetOfEpiNodes= 
              subscriberUserIdsMapEpiNode.getLinkedHashMap().keySet();
          Iterator<EpiNode> subscriberUserIdsIterator= 
              subscriberUserIdsSetOfEpiNodes.iterator();
          while // For each subscriber of this subscribee
            (subscriberUserIdsIterator.hasNext())
            { // send an acknowledgement to the subscriber.
              String subscriberUserIdString=
                  subscriberUserIdsIterator.next().toString();
              subscriberSendAckV(subscriberUserIdString);
              }

          // Notify the subscribee, which technically is also a subscriber.
          // It is part of the overlay network that can source the content.
          //// subscriberSendAckV(subscribeeUserIdString);
          subscriberSendAckV(subscribeeMapEpiNode,subscribeeUserIdString);
          }

      public void subscriberSendAckV(String subscriberUserIdString)
        /* Notifies the subscriber identified by subscriberUserIdString
         * that we would like to receive the next text available
         * for this subscribee, unless this is the local subscribee.
         * It does this by sending an acknowledgement of 
         * the text it has received so far.
         * This method is called when a viewer opens any non-local text stream.
         */
        {
          MapEpiNode subscriberMapEpiNode=
              subscriberUserIdsMapEpiNode.getMapEpiNode(
                  subscriberUserIdString);
          subscriberSendAckV(
              subscriberMapEpiNode,subscriberUserIdString);
          }

      protected int finalizeDataNodesI()
        /* This override method finalizes all the children and then the base class. */
        {
          storeDocumentV( streamFile );
          return super.finalizeDataNodesI(); // Finalize base class
          }

      private void storeDocumentV( File theFile )
        /* This method stores the stream data that is in main memory to 
          the external text file whose name is fileString.
          */
        {
          theAppLog.info("TextStream2.storeDocumentV(..) begins.");
          FileWriter theFileWriter= null;
          thePlainDocument.readLock();
          try {
              FileOps.makeDirectoryAndAncestorsWithLoggingV(
                theFile.getParentFile()); // Create directory if needed.
              theFileWriter= new FileWriter( theFile ); // Open file.
              writeAllLineElementsV(theFileWriter);
              }
            catch (BadLocationException theBadLocationException) { 
              theAppLog.exception( "TextStream2.storeDocumentV(..)",
                  theBadLocationException);
              }
            catch (IOException theIOException) { 
              theAppLog.exception("TextStream2.storeDocumentV(..)",theIOException);
              }
            finally {
              try {
                if ( theFileWriter != null ) theFileWriter.close();
                }
              catch ( IOException theIOException ) { 
                theAppLog.exception("TextStream2.storeDocumentV(..)", theIOException);
                }
              thePlainDocument.readUnlock();
              }
          thePlainDocument= null; // Indicate document is no longer being viewed.
          theAppLog.info("TextStream2.storeDocumentV(..) ends.");
          }

      private void writeAllLineElementsV(FileWriter theFileWriter)
        throws BadLocationException, IOException 
      {
        Element rootElement= thePlainDocument.getDefaultRootElement();
        for // For all line elements in document...
          ( int elementI=0; 
            elementI<rootElement.getElementCount()-1; // Last element is 
            elementI++ )  // ignored because it's always just a newline.
          { // Write the line to file.
            Element lineElement= rootElement.getElement(elementI);
            int startOffset= lineElement.getStartOffset();
            int endOffset= lineElement.getEndOffset();
            String lineString= thePlainDocument.getText(
                startOffset,endOffset-startOffset-1);
            theAppLog.debug(
                "TextStream2.writeAllLineElementsV(.) String=" + lineString);
            theFileWriter.write(lineString); // Output one line of text.
            theFileWriter.write(NL); // Write line terminator.
            }
        }

      public boolean tryProcessingUserDataMapEpiNodeB(
          MapEpiNode userIdDataMapEpiNode,String senderUserIdString) 
        /* This method tries to process TextStream message userIdDataMapEpiNode.
          starterUserIdString identifies the source of the message.
          */
        {
            boolean successB= true; // Assume one alternative will succeed.
          goReturn: {
            if (tryProcessingTextB(
                userIdDataMapEpiNode,senderUserIdString))
              break goReturn;
            if (tryProcessingOffsetOnlyB(
                userIdDataMapEpiNode,senderUserIdString))
              break goReturn;
            successB= false; // Show that everything failed in returned value.
          } // goReturn:
            return successB;
          }

      public boolean tryProcessingOffsetOnlyB(
          MapEpiNode fieldsMapEpiNode,String starterUserIdString)
        /* This method tries to process "HaveToOffset" 
          in fieldsMapEpiNode in a received message.
          If present, it is recorded as subscriber information.
          starterUserIdString identifies the user node 
          which is the source of the message and 
          to which text might be sent in response.
          This method returns true if processing succeeds, false otherwise.
          */
        {
            boolean gotOffsetB; 
          goReturn: {
            String messageOffsetString= 
                fieldsMapEpiNode.getString("HaveToOffset");
            gotOffsetB= (null != messageOffsetString); 
            if (! gotOffsetB) break goReturn; // No offset present, so exit.
            MapEpiNode subscriberUserFieldsMapEpiNode= 
              subscriberUserIdsMapEpiNode.getOrMakeMapEpiNode(
                starterUserIdString);
            subscriberUserFieldsMapEpiNode.putV(
              "HaveToOffset",messageOffsetString);
            subscriberSendTextV(starterUserIdString);
          } // goReturn:
            return gotOffsetB;
          }

      public boolean tryProcessingTextB(
          MapEpiNode fieldsMapEpiNode, String starterUserIdString)
          ///opt starterUserIdString not needed?
        /* This method tries to process "Text" in fieldsMapEpiNode.
          starterUserIdString is context as the source of the message.
          It returns true if successful, false otherwise.
          It switches to the Event Dispatch Thread (EDT) if needed,
          so this method can be called from the EDT or another thread.
          */
        {
            boolean gotTextB= false;
          toReturn: {
            EpiNode textEpiNode= fieldsMapEpiNode.getEpiNode("Text");
            if (null == textEpiNode)break toReturn; // Exit if no Text.
            String senderTextString= textEpiNode.toRawString();
            int senderTextOffsetI= fieldsMapEpiNode.getZeroOrI("TextAtOffset");
            int subscribeeHaveToOffsetI=
                subscribeeMapEpiNode.getZeroOrI("HaveToOffset");
            if (senderTextOffsetI != subscribeeHaveToOffsetI)
              break toReturn; // Exit because text is not at expected offset.
            { // Process the received text.
              EDTUtilities.runOrInvokeAndWaitV( // Switch to EDT thread if needed. 
                  new Runnable() {
                    @Override  
                    public void run() {
                      synchronized(this) { //// Needed?
                        processNewTextStringV(
                          senderTextString,senderTextOffsetI,starterUserIdString);
                        }
                      }
                    } 
                  );
              }
            gotTextB= true; // Success.
          } // toReturn: {
            return gotTextB;
          }

      public void processNewTextStringV(
          String textString, int offsetI, String starterUserIdString)
        /* This method processes textString as new TextStream text.
          Text is replaced starting at offset offsetI.
          If the textString length exceeds the text that follows the offset,
          then the excess text is appended to the end of the document.
          starterUserIdString identifies the source of the text.
          It either could be the sender of a message containing the text,
          or it could be the local node's user keyboard.
          The text may contain '\n' to represent newlines, 
          independent of platform's operating system.
          The text is processed by appending it to 
          the subscribees stream document,
          then doing any acknowledgment or forwarding operations that 
          might have been made possible by receipt of the new text.

          This method should be called from the EDT (Event Dispatch Thread)
          only because it modifies a PlainDocument which
          calls listeners in GUI Swing classes.
           *   
           * ///fix  Because TextStream2Viewer listens to thePlainDocument,
           * operations which mutate its contents might need to switch 
           * to the EDT, at least after loading and before saving.
           * Maybe this appending method should be changed to do this,
           * even though it isn't necessary when it's called on the EDT.
          */
        {
          theAppLog.debug(
              "TextStream2.processNewStreamStringV(.) String=" + textString);

          try { // Put text into document.
              //// thePlainDocument.insertString( // Add to end of document.
              ////   thePlainDocument.getLength(),textString,null);
              thePlainDocument.replace(
                  offsetI,
                  Math.min( thePlainDocument.getLength()-offsetI,
                      textString.length()),
                  textString, // The new text.
                  null // No AttributeSet.
                  );
              subscribeeMapEpiNode.putV( // Record offset of new document end.
                  "HaveToOffset",thePlainDocument.getLength());
            } catch (BadLocationException theBadLocationException) { 
              theAppLog.exception( "TextStream2.processNewStreamStringV(.) ",
                theBadLocationException);
            }

          subscriberUserIdsMapEpiNode.getOrMakeMapEpiNode( // Make certain that
            starterUserIdString); // starter is also a subscriber.
          subscribersSendAckOrTextV(starterUserIdString);
          }

      public void subscribersSendAckOrTextV(
          String starterUserIdString)
        /* This method sends an acknowledgement or sends text
         * to all active subscriber Unicasters of this subscribee,
         * but only if it is appropriate to do so.
         * starterUserIdString identifies the trigger of these actions.
         */
        {
          Set<EpiNode> subscriberUserIdsSetOfEpiNodes= 
              subscriberUserIdsMapEpiNode.getLinkedHashMap().keySet();
          Iterator<EpiNode> subscriberUserIdsIterator= 
              subscriberUserIdsSetOfEpiNodes.iterator();
          while // For all subscriber UserIds for this subscribee
            (subscriberUserIdsIterator.hasNext())
            { // process a subscriber UserId.
              String subscriberUserIdString=
                  subscriberUserIdsIterator.next().toString();
              subscriberSendAckOrTextV(
                  subscriberUserIdString,starterUserIdString);
              }
          }

      private void subscriberSendAckOrTextV(
              String subscriberUserIdString,
              String starterUserIdString
              )
        /* This method is called when this subscribee receives new text.
         * This method sends an acknowledgement or sends text
         * to the subscriber Unicaster identified by subscriberUserIdString,
         * the following conditions are met:
         * * The subscriber is not the local user.
         * * The subscriber Unicaster exists and is connected.
         * What gets sent depends on circumstances:
         * * If the subscriber is also the user node 
         *   that started this operation, meaning was the text source,
         *   identified by starterUserIdString, then an acknowledgement is sent.
         * * Any other subscriber receives the new text if it wants it.
         */
        {
          subscriberProcessing: {
            // Exit if map entry value is invalid.
            MapEpiNode subscriberMapEpiNode=
                subscriberUserIdsMapEpiNode.getMapEpiNode(
                    subscriberUserIdString);
            if (null == subscriberMapEpiNode) // Entry invalid?
              break subscriberProcessing; // Yes, so exit.

            // Send acknowledgment if subscriber is starter of operation.
            if (starterUserIdString.equals(subscriberUserIdString)) {
              subscriberSendAckV(subscriberMapEpiNode,starterUserIdString);
              break subscriberProcessing; // And we are done.
              }

            // Send text because this subscriber is not starter of operation.
            subscriberSendTextV(subscriberUserIdString);
          } // subscriberProcessing:
            return;
          }

      private void subscriberSendTextV( 
          String subscriberUserIdString)
        /* This method sends at least some of the most recently received text
         * to the subscriber identified by subscriberUserIdString,
         * unless 
         * * the subscriber doesn't want any of the new text, or
         * * the subscriber is the local User.
         */
        {
          goReturn: {
            // If subscriber is local, exit.  Text already stored locally.
            if (theTextStreams2.isLocalB(subscriberUserIdString))
              break goReturn; // Yes, so exit.  Don't send text to ourselves.

            // Get MapEntry value but exit if it is invalid.
            MapEpiNode subscriberMapEpiNode=
                subscriberUserIdsMapEpiNode.getMapEpiNode(
                    subscriberUserIdString);
            if (null == subscriberMapEpiNode) // Entry invalid?
              break goReturn; // Yes, so exit.

            // Exit if there is no text to send.
            int subscribeeHaveToOffsetI= 
                subscribeeMapEpiNode.getZeroOrI("HaveToOffset");
            int subscriberHaveToOffsetI= 
                subscriberMapEpiNode.getZeroOrI("HaveToOffset");
            if // Any unsent text for this subscriber?
              (subscriberHaveToOffsetI >= subscribeeHaveToOffsetI)
              break goReturn; // No, so exit.
            
            // Send the next, possibly partial, piece of text to subscriber.
            String nextTextString= getDocumentString(subscriberHaveToOffsetI);
            MapEpiNode subscribeeFieldsMapEpiNode= new MapEpiNode();
            subscribeeFieldsMapEpiNode.putV("Text", nextTextString);
            subscribeeFieldsMapEpiNode.putV( // Include starting offset of text.
                "TextAtOffset",subscriberHaveToOffsetI);
            sendToSubscriberUnicasterV(
                subscribeeFieldsMapEpiNode,subscriberUserIdString);
  
            // Update state of subscription data to include text that was sent.
            subscriberMapEpiNode.putV( // Calculate and record sent-end offset.
              "SentToOffset",subscriberHaveToOffsetI+nextTextString.length());
            subscriberMapEpiNode.putV( // Record ack time-out, 100ms in future.
              "AckTime", System.currentTimeMillis()+100);
          } // goReturn:
        }

      private void subscriberSendAckV(
          MapEpiNode subscriberMapEpiNode,String subscriberUserIdString)
        /* This method sends acknowledgment of the most recently processed text
         * to the subscriber whose data is in subscriberMapEpiNode
         * and is identified by subscriberUserIdString,
         * unless the subscriber is the local User.
         * Note, a text acknowledgment is also considered to be
         * a request for the next available text. 
         */
        {
          goReturn: {
        
            if // Is subscriber local?
              (theTextStreams2.isLocalB(subscriberUserIdString))
              break goReturn; // Yes, so exit.  Don't send ack to ourselves.

            // Send acknowledgement to remote subscriber.
            MapEpiNode subscribeeSendMapEpiNode= new MapEpiNode();
            int subscribeeHaveToOffsetI= // Get offset from subscribee data.
              subscribeeMapEpiNode.getZeroOrI("HaveToOffset");
            subscribeeSendMapEpiNode.putV( // Store offset in message map.
              "HaveToOffset",subscribeeHaveToOffsetI);
            sendToSubscriberUnicasterV( 
              subscribeeSendMapEpiNode, subscriberUserIdString);
      
          } // goReturn:
      
            return;
          }

      private void sendToSubscriberUnicasterV(
          MapEpiNode subscribeeFieldsMapEpiNode, String subscriberUserIdString)
        /* This method sends the subscribeeFieldsMapEpiNode 
         * to the Unicaster whose UserId is subscriberUserIdString.
         * First it wraps the map in a map whose key is
         * the subscribee's UserId, and then passes it to theTextStreams2.
         */
        {
          MapEpiNode userIdMapEpiNode= // Wrap parameters map in UserId map. 
            MapEpiNode.makeSingleEntryMapEpiNode(
              getKeyK(), // This is our/subscribee UserId
              subscribeeFieldsMapEpiNode
              );
          
          // Pass on up to TextStreams2 manager.
          theTextStreams2.sendToSubscriberUnicasterV( 
            userIdMapEpiNode,
            subscriberUserIdString
            );
          }

      public String getDocumentString(int wantFromOffsetI)
        /* Returns a String containing text from the Document, 
         * starting at offset wantFromOffsetI.
         * The String is limited to a maximum of one line, 
         * including the trailing newline, if present.
         */
        {
          Segment textSegment= new Segment();
          textSegment.setPartialReturn(true);
          int remainingI= thePlainDocument.getLength() - wantFromOffsetI; 
          try { // Get a segment of text at desired offset.
              thePlainDocument.getText(wantFromOffsetI,remainingI,textSegment);
            } catch (BadLocationException theBadLocationException) {
              theAppLog.debug(
                "TextStream2.getDocumentString(.) "+theBadLocationException);
            }
          int scanIndexI= textSegment.getBeginIndex();
          while (true) { // Find end of sub-segment to return.
            if (scanIndexI >= textSegment.length()) break; // At end, so exit.
            char theChar= textSegment.charAt(scanIndexI);
            scanIndexI++;
            if ('\n' == theChar) break; // After newline, so exit.
            }
          CharSequence resultCharSequence= 
            textSegment.subSequence(textSegment.getBeginIndex(),scanIndexI);
          return resultCharSequence.toString();
          }

      public boolean isLocalB()
        /* Returns true if this text stream is local, false otherwise.
         * It is equivalent to isLocalB(subscribeeUserIdString).
         */
        {
          return theTextStreams2.isLocalB(subscribeeUserIdString);
          }

      }  
