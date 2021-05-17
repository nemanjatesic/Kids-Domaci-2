package app.snapshot_bitcake;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Snapshot result for servent with id serventId.
 * The amount of bitcakes on that servent is recordedAmount,
 * and messages from others to that servent are recorded in
 * allChannelMessages.
 * 
 * @author bmilojkovic
 *
 */
public class CLSnapshotResult implements Serializable {

	private static final long serialVersionUID = -1443515806440079979L;

	private final int serventId;
	private final int recordedAmount;
	private final Map<String, List<Integer>> allChannelMessages;
	
	public CLSnapshotResult(int serventId, int recordedAmount, Map<String, List<Integer>> allChannelMessages) {
		this.serventId = serventId;
		this.recordedAmount = recordedAmount;
		this.allChannelMessages = new ConcurrentHashMap<>(allChannelMessages);
	}
	public int getServentId() {
		return serventId;
	}
	public int getRecordedAmount() {
		return recordedAmount;
	}
	public Map<String, List<Integer>> getAllChannelMessages() {
		return allChannelMessages;
	}
	
	
}
