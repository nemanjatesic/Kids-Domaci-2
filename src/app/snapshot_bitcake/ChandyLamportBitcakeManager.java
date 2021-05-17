package app.snapshot_bitcake;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import app.AppConfig;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.snapshot.CLMarkerMessage;
import servent.message.snapshot.CLTellMessage;
import servent.message.util.MessageUtil;

public class ChandyLamportBitcakeManager implements BitcakeManager {

	private final AtomicInteger currentAmount = new AtomicInteger(1000);
	
	public void takeSomeBitcakes(int amount) {
		currentAmount.getAndAdd(-amount);
	}
	
	public void addSomeBitcakes(int amount) {
		currentAmount.getAndAdd(amount);
	}
	
	public int getCurrentBitcakeAmount() {
		return currentAmount.get();
	}
	
	/*
	 * This value is protected by AppConfig.colorLock.
	 * Access it only if you have the blessing.
	 */
	public int recordedAmount = 0;
	
	private Map<Integer, Boolean> closedChannels = new ConcurrentHashMap<>();
	private Map<String, List<Integer>> allChannelTransactions = new ConcurrentHashMap<>();
	private Object allChannelTransactionsLock = new Object();
	
	/**
	 * This is invoked when we are white and get a marker. Basically,
	 * we or someone alse have started recording a snapshot.
	 * This method does the following:
	 * <ul>
	 * <li>Makes us red</li>
	 * <li>Records our bitcakes</li>
	 * <li>Sets all channels to not closed</li>
	 * <li>Sends markers to all neighbors</li>
	 * </ul>
	 * @param collectorId - id of collector node, to be put into marker messages for others.
	 */
	public void markerEvent(int collectorId) {
		synchronized (AppConfig.colorLock) {
			AppConfig.timestampedStandardPrint("Going red");
			AppConfig.isWhite.set(false);
			recordedAmount = getCurrentBitcakeAmount();
			
			for (Integer neighbor : AppConfig.myServentInfo.getNeighbors()) {
				closedChannels.put(neighbor, false);
				Message clMarker = new CLMarkerMessage(AppConfig.myServentInfo, AppConfig.getInfoById(neighbor), collectorId);
				MessageUtil.sendMessage(clMarker);
				try {
					/**
					 * This sleep is here to artificially produce some white node -> red node messages
					 */
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		
	}
	
	/**
	 * This is invoked whenever we get a marker from another node. We do the following:
	 * <ul>
	 * <li>If we are white, we do markerEvent()</li>
	 * <li>We mark the channel of the person that sent the marker as closed</li>
	 * <li>If we are done, we report our snapshot result to the collector</li>
	 * </ul>
	 */
	public void handleMarker(Message clientMessage, SnapshotCollector snapshotCollector) {
		synchronized (AppConfig.colorLock) {
			int collectorId = Integer.parseInt(clientMessage.getMessageText());
			
			if (AppConfig.isWhite.get()) {
				markerEvent(collectorId);
			}
			
			closedChannels.put(clientMessage.getOriginalSenderInfo().getId(), true);
			
			if (isDone()) {
				CLSnapshotResult snapshotResult = new CLSnapshotResult(
						AppConfig.myServentInfo.getId(), recordedAmount, allChannelTransactions);
				
				if (AppConfig.myServentInfo.getId() == collectorId) {
					snapshotCollector.addCLSnapshotInfo(collectorId, snapshotResult);
				} else {
					Message clTellMessage = new CLTellMessage(
							AppConfig.myServentInfo, AppConfig.getInfoById(collectorId),
							snapshotResult);
					
					MessageUtil.sendMessage(clTellMessage);
				}
				
				recordedAmount = 0;
				allChannelTransactions.clear();
				AppConfig.timestampedStandardPrint("Going white");
				AppConfig.isWhite.set(true);
			}
		}
	}
	
	/**
	 * Checks if we are done being red. This happens when all channels are closed.
	 * @return
	 */
	private boolean isDone() {
		if (AppConfig.isWhite.get()) {
			return false;
		}
		
		AppConfig.timestampedStandardPrint(closedChannels.toString());
		
		for (Entry<Integer, Boolean> closedChannel : closedChannels.entrySet()) {
			if (closedChannel.getValue() == false) {
				return false;
			}
		}
		
		return true;
	}

	/**
	 * Records a channel message. This will be invoked if we are red and
	 * get a message that is not a marker.
	 * @param clientMessage
	 */
	public void addChannelMessage(Message clientMessage) {
		if (clientMessage.getMessageType() == MessageType.TRANSACTION) {
			synchronized (allChannelTransactionsLock) {
				String channelName = "channel " + AppConfig.myServentInfo.getId() + "<-" + clientMessage.getOriginalSenderInfo().getId();
				
				List<Integer> channelMessages = allChannelTransactions.getOrDefault(channelName, new ArrayList<>());
				channelMessages.add(Integer.parseInt(clientMessage.getMessageText()));
				allChannelTransactions.put(channelName, channelMessages);
			}
		}
	}
}
