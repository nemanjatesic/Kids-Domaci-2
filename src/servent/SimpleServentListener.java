package servent;

import app.AppConfig;
import app.Cancellable;
import app.snapshot_bitcake.SnapshotCollector;
import servent.handler.*;
import servent.handler.snapshot.NaiveAskAmountHandler;
import servent.handler.snapshot.NaiveTellAmountHandler;
import servent.message.Message;
import servent.message.util.MessageUtil;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SimpleServentListener implements Runnable, Cancellable {

    private volatile boolean working = true;
    private SnapshotCollector snapshotCollector;

    public SimpleServentListener(SnapshotCollector snapshotCollector) {
        this.snapshotCollector = snapshotCollector;
    }

    /*
     * Thread pool for executing the handlers. Each client will get it's own handler thread.
     */
    private final ExecutorService threadPool = Executors.newWorkStealingPool();

    @Override
    public void run() {
        ServerSocket listenerSocket = null;
        try {
            listenerSocket = new ServerSocket(AppConfig.myServentInfo.getListenerPort());
            /*
             * If there is no connection after 1s, wake up and see if we should terminate.
             */
            listenerSocket.setSoTimeout(1000);
        } catch (IOException e) {
            AppConfig.timestampedErrorPrint("Couldn't open listener socket on: " + AppConfig.myServentInfo.getListenerPort());
            System.exit(0);
        }

        while (working) {
            try {
                /*
                 * This blocks for up to 1s, after which SocketTimeoutException is thrown.
                 */
                Socket clientSocket = listenerSocket.accept();

                //GOT A MESSAGE! <3
                Message clientMessage = MessageUtil.readMessage(clientSocket);

                MessageHandler messageHandler = switch (clientMessage.getMessageType()) {
                    case PING -> new PingHandler(clientMessage);
                    case PONG -> new PongHandler(clientMessage);
                    case CAUSAL_BROADCAST -> new CausalBroadcastHandler(clientMessage, !AppConfig.IS_CLIQUE);
                    case TRANSACTION -> new TransactionHandler(clientMessage, snapshotCollector.getBitcakeManager());
                    case NAIVE_ASK_AMOUNT -> new NaiveAskAmountHandler(clientMessage, snapshotCollector.getBitcakeManager());
                    case NAIVE_TELL_AMOUNT -> new NaiveTellAmountHandler(clientMessage, snapshotCollector);
                    default -> new NullHandler(clientMessage);
                };

                /*
                 * Each message type has it's own handler.
                 * If we can get away with stateless handlers, we will,
                 * because that way is much simpler and less error prone.
                 */

                threadPool.submit(messageHandler);
            } catch (SocketTimeoutException timeoutEx) {
                //Uncomment the next line to see that we are waking up every second.
//				AppConfig.timedStandardPrint("Waiting...");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void stop() {
        this.working = false;
    }

}
