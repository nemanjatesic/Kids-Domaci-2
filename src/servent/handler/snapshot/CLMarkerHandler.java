package servent.handler.snapshot;

import app.snapshot_bitcake.ChandyLamportBitcakeManager;
import app.snapshot_bitcake.SnapshotCollector;
import servent.handler.MessageHandler;
import servent.message.Message;

public class CLMarkerHandler implements MessageHandler {

	private Message clientMessage;
	private ChandyLamportBitcakeManager bitcakeManager;
	private SnapshotCollector snapshotCollector;

	public CLMarkerHandler(Message clientMessage, SnapshotCollector snapshotCollector) {
		this.clientMessage = clientMessage;
		this.bitcakeManager = (ChandyLamportBitcakeManager)snapshotCollector.getBitcakeManager();
		this.snapshotCollector = snapshotCollector;
	}

	@Override
	public void run() {
		bitcakeManager.handleMarker(clientMessage, snapshotCollector);

	}

}
