package cli.command;

import app.AppConfig;
import app.ServentInfo;
import servent.message.Message;
import servent.message.PingMessage;
import servent.message.util.MessageUtil;

public class PingCommand implements CLICommand {

	@Override
	public String commandName() {
		return "ping";
	}

	@Override
	public void execute(String args) {
		int serventToPing = -1;
		
		try {
			serventToPing = Integer.parseInt(args);
			
			if (serventToPing < 0 || serventToPing >= AppConfig.getServentCount()) {
				throw new NumberFormatException();
			}
			
			if (!AppConfig.myServentInfo.getNeighbors().contains(serventToPing)) {
				AppConfig.timestampedErrorPrint("PINGing a servent that is not our neighbor :( " + serventToPing);
				return;
			}
			ServentInfo serventInfo = AppConfig.getInfoById(serventToPing);
			
			Message pingMessage = new PingMessage(AppConfig.myServentInfo, serventInfo);
			
			MessageUtil.sendMessage(pingMessage);
			
		} catch (NumberFormatException e) {
			AppConfig.timestampedErrorPrint("Ping command should have one int argument, which is id.");
		}
	}

}
