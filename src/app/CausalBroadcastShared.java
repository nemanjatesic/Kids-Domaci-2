package app;

import servent.message.CausalBroadcastMessage;
import servent.message.Message;
import servent.message.TransactionMessage;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.function.BiFunction;

/**
 * This class contains shared data for the Causal Broadcast implementation:
 * <ul>
 * <li> Vector clock for current instance
 * <li> Commited message list
 * <li> Pending queue
 * </ul>
 * As well as operations for working with all of the above.
 *
 * @author bmilojkovic
 *
 */
public class CausalBroadcastShared {

    private static Map<Integer, Integer> vectorClock = new ConcurrentHashMap<>();
    private static List<Message> commitedCausalMessageList = new CopyOnWriteArrayList<>();
    private static Queue<Message> pendingMessages = new ConcurrentLinkedQueue<>();
    private static Object pendingMessagesLock = new Object();
    private static ExecutorService threadPool = Executors.newWorkStealingPool();

    public static void initializeVectorClock(int serventCount) {
        for(int i = 0; i < serventCount; i++) {
            vectorClock.put(i, 0);
        }
    }

    public static void incrementClock(int serventId) {
        vectorClock.computeIfPresent(serventId, (key, oldValue) -> oldValue+1);
    }

    public static Map<Integer, Integer> getVectorClock() {
        return vectorClock;
    }

    public static List<Message> getCommitedCausalMessages() {
        List<Message> toReturn = new CopyOnWriteArrayList<>(commitedCausalMessageList);

        return toReturn;
    }

    public static void addPendingMessage(Message msg) {
        pendingMessages.add(msg);
    }

    public static void commitCausalMessage(Message newMessage) {
        AppConfig.timestampedStandardPrint("Committing " + newMessage);
        commitedCausalMessageList.add(newMessage);
        incrementClock(newMessage.getOriginalSenderInfo().getId());

        checkPendingMessages();
    }

    private static boolean otherClockGreater(Map<Integer, Integer> clock1, Map<Integer, Integer> clock2) {
        if (clock1.size() != clock2.size()) {
            throw new IllegalArgumentException("Clocks are not same size how why");
        }

        for(int i = 0; i < clock1.size(); i++) {
            if (clock2.get(i) > clock1.get(i)) {
                return true;
            }
        }

        return false;
    }

    public static void checkPendingMessages() {
        boolean gotWork = true;

        while (gotWork) {
            gotWork = false;

            synchronized (pendingMessagesLock) {
                Iterator<Message> iterator = pendingMessages.iterator();

                Map<Integer, Integer> myVectorClock = getVectorClock();
                while (iterator.hasNext()) {
                    Message pendingMessage = iterator.next();
                    TransactionMessage causalPendingMessage = (TransactionMessage) pendingMessage;

                    if (!otherClockGreater(myVectorClock, causalPendingMessage.getSenderVectorClock())) {
                        gotWork = true;

                        AppConfig.timestampedStandardPrint("Committing " + pendingMessage);
                        commitedCausalMessageList.add(pendingMessage);
                        incrementClock(pendingMessage.getOriginalSenderInfo().getId());

                        TransactionMessage transactionMessage = (TransactionMessage) pendingMessage;
                        if(AppConfig.myServentInfo.getId() == transactionMessage.getTargetReciever().getId())
                        {
                            String amountString = transactionMessage.getMessageText();
                            int amountNumber = 0;
                            try {
                                amountNumber = Integer.parseInt(amountString);
                            } catch (NumberFormatException e) {
                                AppConfig.timestampedErrorPrint("Couldn't parse amount: " + amountString);
                                return;
                            }
                            if(amountNumber!=0)
                            {
                                AppConfig.timestampedErrorPrint(AppConfig.myBitcakeManager.getCurrentBitcakeAmount() + " -1-");
                                AppConfig.myBitcakeManager.addSomeBitcakes(amountNumber);
                                AppConfig.timestampedErrorPrint(AppConfig.myBitcakeManager.getCurrentBitcakeAmount() + " -2-");
                            }

                        }
                        iterator.remove();

                        break;
                    }
                }
            }
        }
    }
}