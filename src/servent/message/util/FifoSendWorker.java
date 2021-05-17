package servent.message.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

import app.AppConfig;
import app.Cancellable;
import app.ServentInfo;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.PoisonMessage;

/**
 * We will have as many instances of these workers as we have neighbors. Each of them
 * reads messages from a queue (two queues, actually, for Chandy-Lamport) and sends them
 * via a simple socket. The thread waits for an ACK on the same socket before sending another
 * message to the same servent.
 * 
 * These threads are stopped via {@link PoisonMessage}.
 * @author bmilojkovic
 *
 */
public class FifoSendWorker implements Runnable, Cancellable {

	private int neighbor;
	
	public FifoSendWorker(int neighbor) {
		this.neighbor = neighbor;
	}
	
	@Override
	public void run() {
		while (true) {
			try {
				Message messageToSend = MessageUtil.pendingMarkers.get(neighbor).poll(200, TimeUnit.MILLISECONDS);
				
				if (messageToSend == null) {
					if (AppConfig.isWhite.get()) {
						messageToSend = MessageUtil.pendingMessages.get(neighbor).poll(200, TimeUnit.MILLISECONDS);
					}
				}
				
				if (messageToSend == null) {
					continue;
				}
				
				if (messageToSend.getMessageType() == MessageType.POISON) {
					break;
				}
				
				Socket sendSocket = null;
				
				/*
				 * We are doing a compound operation on our color. In Chandy-Lamport, we want to be sure
				 * that we are white when we are taking away our bitcakes.
				 * The bitcake reducing is done in the sendEffect() method of the
				 * Transaction message, which is at the bottom of this sync.
				 */
				synchronized (AppConfig.colorLock) {
					if (AppConfig.isWhite.get() == false && messageToSend.getMessageType() != MessageType.CL_MARKER) {
						MessageUtil.pendingMessages.get(neighbor).put(messageToSend);
						continue;
					}
				
					if (MessageUtil.MESSAGE_UTIL_PRINTING) {
						AppConfig.timestampedStandardPrint("Sending message " + messageToSend);
					}
					
					ServentInfo receiverInfo = messageToSend.getReceiverInfo();
					
					sendSocket = new Socket(receiverInfo.getIpAddress(), receiverInfo.getListenerPort());
					ObjectOutputStream oos = new ObjectOutputStream(sendSocket.getOutputStream());
					oos.writeObject(messageToSend);
					oos.flush();
					
					messageToSend.sendEffect();
				}
				
				ObjectInputStream ois = new ObjectInputStream(sendSocket.getInputStream());
				String ackString = (String)ois.readObject();
				if (!ackString.equals("ACK")) {
					AppConfig.timestampedErrorPrint("Got response which is not an ACK");
				}
				
				sendSocket.close();


			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
	}
	
	@Override
	public void stop() {
		try {
			MessageUtil.pendingMessages.get(neighbor).put(new PoisonMessage());
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}
