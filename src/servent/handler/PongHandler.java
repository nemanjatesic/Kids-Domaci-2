package servent.handler;

import app.AppConfig;
import servent.message.Message;
import servent.message.MessageType;

/**
 * Pong handler. Here for completeness. Could have left it out.
 * 
 * @author bmilojkovic
 */
public class PongHandler implements MessageHandler {

	private Message clientMessage;
	
	public PongHandler(Message clientMessage) {
		this.clientMessage = clientMessage;
	}
	
	@Override
	public void run() {
		/*
		 * The receive function is gonna print out that we got this message anyway,
		 * so idk what we are doing here tbh.
		 * All of this code is quite useless, and we even made a thread for it,
		 * I mean it's rather divorced from reality if you ask me.
		 * Obviously we needed this long comment here so we can feel important.
		 * k tnx bye.
		 */
		if (clientMessage.getMessageType() == MessageType.PONG) {
			; //whatevs
		}  else {
			AppConfig.timestampedErrorPrint("PONG handler got: " + clientMessage);
		}
	}

}
