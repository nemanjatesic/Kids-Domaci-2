package servent.message.snapshot;

import app.ServentInfo;
import servent.message.BasicMessage;
import servent.message.MessageType;

public class CLMarkerMessage extends BasicMessage {

	private static final long serialVersionUID = -3114137381491356339L;

	public CLMarkerMessage(ServentInfo sender, ServentInfo receiver, int collectorId) {
		super(MessageType.CL_MARKER, sender, receiver, String.valueOf(collectorId));
	}
}
