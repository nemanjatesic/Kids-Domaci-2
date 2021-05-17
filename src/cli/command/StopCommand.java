package cli.command;

import java.util.List;

import app.AppConfig;
import app.snapshot_bitcake.SnapshotCollector;
import cli.CLIParser;
import servent.SimpleServentListener;
import servent.message.util.FifoSendWorker;

public class StopCommand implements CLICommand {

	private CLIParser parser;
	private SimpleServentListener listener;
	private List<FifoSendWorker> senderWorkers;
	private SnapshotCollector snapshotCollector;
	
	public StopCommand(CLIParser parser, SimpleServentListener listener,
			List<FifoSendWorker> senderWorkers, SnapshotCollector snapshotCollector) {
		this.parser = parser;
		this.listener = listener;
		this.senderWorkers = senderWorkers;
		this.snapshotCollector = snapshotCollector;
	}
	
	@Override
	public String commandName() {
		return "stop";
	}

	@Override
	public void execute(String args) {
		AppConfig.timestampedStandardPrint("Stopping...");
		parser.stop();
		listener.stop();
		for (FifoSendWorker senderWorker : senderWorkers) {
			senderWorker.stop();
		}
		snapshotCollector.stop();
	}

}
