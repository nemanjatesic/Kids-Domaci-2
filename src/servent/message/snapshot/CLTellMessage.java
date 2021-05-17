package servent.message.snapshot;

import app.ServentInfo;
import app.snapshot_bitcake.CLSnapshotResult;
import servent.message.BasicMessage;
import servent.message.MessageType;

public class CLTellMessage extends BasicMessage {

	private static final long serialVersionUID = 8224274653159843559L;

	private CLSnapshotResult clSnapshotResult;
	
	public CLTellMessage(ServentInfo sender, ServentInfo receiver, CLSnapshotResult clSnapshotResult) {
		super(MessageType.CL_TELL, sender, receiver);
		
		this.clSnapshotResult = clSnapshotResult;
	}

	public CLSnapshotResult getCLSnapshotResult() {
		return clSnapshotResult;
	}
	
}
