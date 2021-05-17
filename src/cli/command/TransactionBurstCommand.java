package cli.command;

import app.AppConfig;
import app.CausalBroadcastShared;
import app.ServentInfo;
import app.snapshot_bitcake.BitcakeManager;
import servent.message.Message;
import servent.message.TransactionMessage;
import servent.message.util.MessageUtil;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TransactionBurstCommand implements CLICommand{

    private static final int TRANSACTION_COUNT = 1;
    private static final int BURST_WORKERS = 1;
    private static final int MAX_TRANSFER_AMOUNT = 10;

    private BitcakeManager bitcakeManager;

    public TransactionBurstCommand(BitcakeManager bitcakeManager) {
        this.bitcakeManager = bitcakeManager;
    }

    private class TransactionBurstWorker implements Runnable {

        @Override
        public void run() {
            for (int i = 0; i < TRANSACTION_COUNT; i++) {
                for (int neighbor : AppConfig.myServentInfo.getNeighbors()) {
                    ServentInfo neighborInfo = AppConfig.getInfoById(neighbor);

                    int amount = 1 + (int)(Math.random() * MAX_TRANSFER_AMOUNT);

                    Message transactionMessage = new TransactionMessage(
                            AppConfig.myServentInfo, neighborInfo, neighborInfo, amount, bitcakeManager, new ConcurrentHashMap<>(CausalBroadcastShared.getVectorClock()));

                    MessageUtil.sendMessage(transactionMessage);

                    for(int n2: AppConfig.myServentInfo.getNeighbors())
                    {
                        if(n2!=neighbor)
                        {
                            transactionMessage.changeReceiver(n2);
                            MessageUtil.sendMessage(transactionMessage);
                        }
                    }

                    bitcakeManager.takeSomeBitcakes(amount);
                    transactionMessage.changeReceiver(AppConfig.myServentInfo.getId());
                    CausalBroadcastShared.commitCausalMessage(transactionMessage);
                }
            }
        }
    }

    @Override
    public String commandName() {
        return "transaction_burst";
    }

    @Override
    public void execute(String args) {
        for (int i = 0; i < BURST_WORKERS; i++) {
            Thread t = new Thread(new TransactionBurstWorker());

            t.start();
        }
    }


}
