package servent.message;

import app.AppConfig;
import app.ServentInfo;
import app.snapshot_bitcake.BitcakeManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TransactionMessage extends BasicMessage{
    private static final long serialVersionUID = -333251402058492901L;

    private int amount;
    private transient BitcakeManager bitcakeManager;
    private Map<Integer, Integer> senderVectorClock;
    private final ServentInfo targetReciever;

    public TransactionMessage(ServentInfo sender, ServentInfo receiver, ServentInfo targetReciever,
                              int amount, BitcakeManager bitcakeManager
                        ,Map<Integer, Integer> senderVectorClock) {
        super(MessageType.TRANSACTION, sender, receiver, String.valueOf(amount));
        this.amount = amount;
        this.bitcakeManager = bitcakeManager;
        this.senderVectorClock = senderVectorClock;
        this.targetReciever = targetReciever;
    }

    private TransactionMessage(ServentInfo sender, ServentInfo receiver,ServentInfo targetReciever,
                               List<ServentInfo> routeList,
                               int amount, BitcakeManager bitcakeManager,
                               Map<Integer, Integer> senderVectorClock, int messageId) {
        super(MessageType.TRANSACTION, sender, receiver, routeList, String.valueOf(amount), messageId);
        this.targetReciever = targetReciever;
        this.amount = amount;
        this.bitcakeManager = bitcakeManager;
        this.senderVectorClock = senderVectorClock;
    }
    /**
     * We want to take away our amount exactly as we are sending, so our snapshots don't mess up.
     * This method is invoked by the sender just before sending, and with a lock that guarantees
     * that we are white when we are doing this in Chandy-Lamport.
     */
    @Override
    public void sendEffect() {
//        //Oduzimamo samo original senderu
//        if((this.getRoute()==null || this.getRoute().size()==0) && (this.targetReciever.equals(this.getReceiverInfo()))){
//            int amount = Integer.parseInt(getMessageText());
//            bitcakeManager.takeSomeBitcakes(amount);
//        }
    }

    public Map<Integer, Integer> getSenderVectorClock() {
        return senderVectorClock;
    }
    public int getAmount()
    {
        return this.amount;
    }
    private BitcakeManager getBitcakeManager()
    {
        return this.bitcakeManager;
    }

    public ServentInfo getTargetReciever()
    {
        return this.targetReciever;
    }

    @Override
    public Message changeReceiver(Integer newReceiverId) {
        if (AppConfig.myServentInfo.getNeighbors().contains(newReceiverId) || AppConfig.myServentInfo.getId()==newReceiverId) {
            ServentInfo newReceiverInfo = AppConfig.getInfoById(newReceiverId);

            return new TransactionMessage(getOriginalSenderInfo(),
                    newReceiverInfo, getTargetReciever(),
                    getRoute(), getAmount(), getBitcakeManager(), getSenderVectorClock(), getMessageId());
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

        return new TransactionMessage(getOriginalSenderInfo(),
                getReceiverInfo(), getTargetReciever(), newRouteList, getAmount(), getBitcakeManager(),
                getSenderVectorClock(), getMessageId() );
    }

}
