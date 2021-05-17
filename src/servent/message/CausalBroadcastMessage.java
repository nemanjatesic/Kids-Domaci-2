package servent.message;

import app.AppConfig;
import app.ServentInfo;

import java.util.*;

/**
 * Has all the fancy stuff from {@link BasicMessage}, with an
 * added vector clock.
 *
 * Think about the repercussions of invoking <code>changeReceiver</code> or
 * <code>makeMeASender</code> on this without overriding it.
 * @author bmilojkovic
 *
 */
public class CausalBroadcastMessage extends BasicMessage {

    private static final long serialVersionUID = 7952273798396080816L;
    private Map<Integer, Integer> senderVectorClock;

    public CausalBroadcastMessage(ServentInfo senderInfo, ServentInfo receiverInfo, String messageText,
                                  Map<Integer, Integer> senderVectorClock) {
        super(MessageType.CAUSAL_BROADCAST, senderInfo, receiverInfo, messageText);

        this.senderVectorClock = senderVectorClock;
    }

    private CausalBroadcastMessage(ServentInfo originalSenderInfo, ServentInfo receiverInfo,
                                   List<ServentInfo> routeList, String messageText, int messageId,
                                   Map<Integer, Integer> senderVectorClock)
    {
        super(MessageType.CAUSAL_BROADCAST, originalSenderInfo,receiverInfo, routeList,
               messageText, messageId);
        this.senderVectorClock = senderVectorClock;
    }

    public Map<Integer, Integer> getSenderVectorClock() {
        return senderVectorClock;
    }

    @Override
    public Message changeReceiver(Integer newReceiverId) {
        if (AppConfig.myServentInfo.getNeighbors().contains(newReceiverId) || AppConfig.myServentInfo.getId()==newReceiverId) {
            ServentInfo newReceiverInfo = AppConfig.getInfoById(newReceiverId);


            return new CausalBroadcastMessage(getOriginalSenderInfo(),
                    newReceiverInfo, getRoute(), getMessageText(), getMessageId(), getSenderVectorClock());
        } else {
            AppConfig.timestampedErrorPrint("Trying to make a message for " + newReceiverId + " who is not a neighbor.");

            return null;
        }
    }

    @Override
    public Message makeMeASender() {
        ServentInfo newRouteItem = AppConfig.myServentInfo;

        List<ServentInfo> newRouteList = new ArrayList<>(getRoute());
        newRouteList.add(newRouteItem);

        return new CausalBroadcastMessage(getOriginalSenderInfo(),
                getReceiverInfo(), newRouteList, getMessageText(), getMessageId(), getSenderVectorClock());
    }

}
