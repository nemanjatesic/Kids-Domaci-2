package servent.handler;

import app.AppConfig;
import app.CausalBroadcastShared;
import app.ServentInfo;
import servent.message.CausalBroadcastMessage;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.util.MessageUtil;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles the CAUSAL_BROADCAST message. Fairly simple, as we assume that we are
 * in a clique. We add the message to a pending queue, and let the check on the queue
 * take care of the rest.
 * @author bmilojkovic
 *
 */
public class CausalBroadcastHandler implements MessageHandler {

    private Message clientMessage;
    private boolean doRebroadcast = false;

    private static Set<CausalBroadcastMessage> recieved = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public CausalBroadcastHandler(Message clientMessage, boolean doRebroadcast) {
        this.clientMessage = clientMessage;
        this.doRebroadcast = doRebroadcast;
    }

    @Override
    public void run() {
        //Sanity check.
        if (clientMessage.getMessageType() == MessageType.CAUSAL_BROADCAST) {

            if(doRebroadcast)
            {
                CausalBroadcastMessage causalMessage = (CausalBroadcastMessage)clientMessage;
                ServentInfo senderInfo = causalMessage.getOriginalSenderInfo();
                if(senderInfo.getId() == AppConfig.myServentInfo.getId())
                {
                    AppConfig.timestampedStandardPrint("Got own message back. No rebroadcast.");
                    return;
                }
                boolean didPut = recieved.add(causalMessage);
                if (didPut) {
                    CausalBroadcastShared.addPendingMessage(causalMessage);
                    CausalBroadcastShared.checkPendingMessages();

                    for (Integer neighbor : AppConfig.myServentInfo.getNeighbors()) {
                        MessageUtil.sendMessage(causalMessage.changeReceiver(neighbor).makeMeASender());
                    }
                } else {
                    AppConfig.timestampedStandardPrint("Already had this. No rebroadcast.");
                }
            }else
            {
                CausalBroadcastShared.addPendingMessage(clientMessage);
                CausalBroadcastShared.checkPendingMessages();
            }
        }
    }

}
