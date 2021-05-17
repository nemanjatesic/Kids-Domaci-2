package app.snapshot_bitcake;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import app.AppConfig;
import servent.message.Message;
import servent.message.snapshot.NaiveAskAmountMessage;
import servent.message.util.MessageUtil;

/**
 * Main snapshot collector class. Has support for Naive, Chandy-Lamport
 * and Lai-Yang snapshot algorithms.
 * 
 * @author bmilojkovic
 *
 */
public class SnapshotCollectorWorker implements SnapshotCollector {

	private volatile boolean working = true;
	
	private AtomicBoolean collecting = new AtomicBoolean(false);
	
	private Map<String, Integer> collectedNaiveValues = new ConcurrentHashMap<>();
	private Map<Integer, CLSnapshotResult> collectedCLValues = new ConcurrentHashMap<>();
	private Map<Integer, LYSnapshotResult> collectedLYValues = new ConcurrentHashMap<>();
	
	private SnapshotType snapshotType = SnapshotType.NAIVE;
	
	private BitcakeManager bitcakeManager;

	public SnapshotCollectorWorker(SnapshotType snapshotType) {
		this.snapshotType = snapshotType;
		
		switch(snapshotType) {
		case NAIVE:
			bitcakeManager = new NaiveBitcakeManager();
			break;
		case CHANDY_LAMPORT:
			bitcakeManager = new ChandyLamportBitcakeManager();
			break;
		case LAI_YANG:
			bitcakeManager = new LaiYangBitcakeManager();
			break;
		case NONE:
			AppConfig.timestampedErrorPrint("Making snapshot collector without specifying type. Exiting...");
			System.exit(0);
		}
	}
	
	@Override
	public BitcakeManager getBitcakeManager() {
		return bitcakeManager;
	}
	
	@Override
	public void run() {
		while(working) {
			
			/*
			 * Not collecting yet - just sleep until we start actual work, or finish
			 */
			while (collecting.get() == false) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				if (working == false) {
					return;
				}
			}
			
			/*
			 * Collecting is done in three stages:
			 * 1. Send messages asking for values
			 * 2. Wait for all the responses
			 * 3. Print result
			 */
			
			//1 send asks
			switch (snapshotType) {
			case NAIVE:
				Message askMessage = new NaiveAskAmountMessage(AppConfig.myServentInfo, null);
				
				for (Integer neighbor : AppConfig.myServentInfo.getNeighbors()) {
					askMessage = askMessage.changeReceiver(neighbor);
					
					MessageUtil.sendMessage(askMessage);
				}
				collectedNaiveValues.put("node"+AppConfig.myServentInfo.getId(), bitcakeManager.getCurrentBitcakeAmount());
				break;
			case CHANDY_LAMPORT:
				((ChandyLamportBitcakeManager)bitcakeManager).markerEvent(AppConfig.myServentInfo.getId());
				break;
			case LAI_YANG:
				((LaiYangBitcakeManager)bitcakeManager).markerEvent(AppConfig.myServentInfo.getId(), this);
				break;
			case NONE:
				//Shouldn't be able to come here. See constructor. 
				break;
			}
			
			//2 wait for responses or finish
			boolean waiting = true;
			while (waiting) {
				switch (snapshotType) {
				case NAIVE:
					if (collectedNaiveValues.size() == AppConfig.getServentCount()) {
						waiting = false;
					}
					break;
				case CHANDY_LAMPORT:
					if (collectedCLValues.size() == AppConfig.getServentCount()) {
						waiting = false;
					}
					break;
				case LAI_YANG:
					if (collectedLYValues.size() == AppConfig.getServentCount()) {
						waiting = false;
					}
					break;
				case NONE:
					//Shouldn't be able to come here. See constructor. 
					break;
				}
				
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
				if (working == false) {
					return;
				}
			}
			
			//print
			int sum;
			switch (snapshotType) {
			case NAIVE:
				sum = 0;
				for (Entry<String, Integer> itemAmount : collectedNaiveValues.entrySet()) {
					sum += itemAmount.getValue();
					AppConfig.timestampedStandardPrint(
							"Info for " + itemAmount.getKey() + " = " + itemAmount.getValue() + " bitcake");
				}
				
				AppConfig.timestampedStandardPrint("System bitcake count: " + sum);
				
				collectedNaiveValues.clear(); //reset for next invocation
				break;
			case CHANDY_LAMPORT:
				sum = 0;
				for (Entry<Integer, CLSnapshotResult> nodeResult : collectedCLValues.entrySet()) {
					sum += nodeResult.getValue().getRecordedAmount();
					AppConfig.timestampedStandardPrint(
							"Recorded bitcake amount for " + nodeResult.getKey() + " = " + nodeResult.getValue().getRecordedAmount());
					if (nodeResult.getValue().getAllChannelMessages().size() == 0) {
						AppConfig.timestampedStandardPrint("No channel bitcake for " + nodeResult.getKey());
					} else {
						for (Entry<String, List<Integer>> channelMessages : nodeResult.getValue().getAllChannelMessages().entrySet()) {
							int channelSum = 0;
							for (Integer val : channelMessages.getValue()) {
								channelSum += val;
							}
							AppConfig.timestampedStandardPrint("Channel bitcake for " + channelMessages.getKey() +
									": " + channelMessages.getValue() + " with channel bitcake sum: " + channelSum);
							
							sum += channelSum;
						}
					}
				}
				
				AppConfig.timestampedStandardPrint("System bitcake count: " + sum);
				
				collectedCLValues.clear(); //reset for next invocation
				break;
			case LAI_YANG:
				sum = 0;
				for (Entry<Integer, LYSnapshotResult> nodeResult : collectedLYValues.entrySet()) {
					sum += nodeResult.getValue().getRecordedAmount();
					AppConfig.timestampedStandardPrint(
							"Recorded bitcake amount for " + nodeResult.getKey() + " = " + nodeResult.getValue().getRecordedAmount());
				}
				for(int i = 0; i < AppConfig.getServentCount(); i++) {
					for (int j = 0; j < AppConfig.getServentCount(); j++) {
						if (i != j) {
							if (AppConfig.getInfoById(i).getNeighbors().contains(j) &&
								AppConfig.getInfoById(j).getNeighbors().contains(i)) {
								int ijAmount = collectedLYValues.get(i).getGiveHistory().get(j);
								int jiAmount = collectedLYValues.get(j).getGetHistory().get(i);
								
								if (ijAmount != jiAmount) {
									String outputString = String.format(
											"Unreceived bitcake amount: %d from servent %d to servent %d",
											ijAmount - jiAmount, i, j);
									AppConfig.timestampedStandardPrint(outputString);
									sum += ijAmount - jiAmount;
								}
							}
						}
					}
				}
				
				AppConfig.timestampedStandardPrint("System bitcake count: " + sum);
				
				collectedLYValues.clear(); //reset for next invocation
				break;
			case NONE:
				//Shouldn't be able to come here. See constructor. 
				break;
			}
			collecting.set(false);
		}

	}
	
	@Override
	public void addNaiveSnapshotInfo(String snapshotSubject, int amount) {
		collectedNaiveValues.put(snapshotSubject, amount);
	}

	@Override
	public void addCLSnapshotInfo(int id, CLSnapshotResult clSnapshotResult) {
		collectedCLValues.put(id, clSnapshotResult);
	}
	
	@Override
	public void addLYSnapshotInfo(int id, LYSnapshotResult lySnapshotResult) {
		collectedLYValues.put(id, lySnapshotResult);
	}
	
	@Override
	public void startCollecting() {
		boolean oldValue = this.collecting.getAndSet(true);
		
		if (oldValue == true) {
			AppConfig.timestampedErrorPrint("Tried to start collecting before finished with previous.");
		}
	}
	
	@Override
	public void stop() {
		working = false;
	}

}
