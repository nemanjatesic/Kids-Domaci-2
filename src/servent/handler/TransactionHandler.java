package servent.handler;

import app.AppConfig;
import app.CausalBroadcastShared;
import app.ServentInfo;
import app.snapshot_bitcake.BitcakeManager;
import servent.message.CausalBroadcastMessage;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.TransactionMessage;
import servent.message.util.MessageUtil;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class TransactionHandler implements MessageHandler{

    private Message clientMessage;
    private BitcakeManager bitcakeManager;

    private static Set<TransactionMessage> recieved = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public TransactionHandler(Message clientMessage, BitcakeManager bitcakeManager) {
        this.clientMessage = clientMessage;
        this.bitcakeManager = bitcakeManager;
    }

    @Override
    public void run() {
        if (clientMessage.getMessageType() == MessageType.TRANSACTION) {
            //Uvek radimo rebroadcast
            TransactionMessage transactionMessage = (TransactionMessage) clientMessage;
            ServentInfo senderInfo = transactionMessage.getOriginalSenderInfo();
            if(senderInfo.getId() == AppConfig.myServentInfo.getId())
            {
                AppConfig.timestampedStandardPrint("Got own message back. No rebroadcast.");
                return;
            }
            boolean didPut = recieved.add(transactionMessage);

            if (didPut){
                CausalBroadcastShared.addPendingMessage(transactionMessage);
                CausalBroadcastShared.checkPendingMessages();

                for (Integer neighbor : AppConfig.myServentInfo.getNeighbors()) {
                    MessageUtil.sendMessage(transactionMessage.changeReceiver(neighbor).makeMeASender());
                }

            }else {
                AppConfig.timestampedStandardPrint("Already had this. No rebroadcast.");
            }
        } else {
            AppConfig.timestampedErrorPrint("Transaction handler got: " + clientMessage);
        }
    }
}
