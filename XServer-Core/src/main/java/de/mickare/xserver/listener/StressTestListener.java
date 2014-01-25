package de.mickare.xserver.listener;

import java.io.IOException;

import de.mickare.xserver.AbstractXServerManager;
import de.mickare.xserver.XServerListener;
import de.mickare.xserver.annotations.XEventHandler;
import de.mickare.xserver.events.XServerMessageIncomingEvent;
import de.mickare.xserver.stresstest.StressTest;

public class StressTestListener implements XServerListener {

	private final AbstractXServerManager manager;

	public StressTestListener(AbstractXServerManager manager) {
		this.manager = manager;
	}

	@XEventHandler(sync = true, channel = StressTest.STRESSTEST_CHANNEL_PING_SYNC)
	public void onStressTestPingSync(XServerMessageIncomingEvent event) {
		// Do Pong
		try {
			event.getServer().sendMessage(manager.createMessage(StressTest.STRESSTEST_CHANNEL_PONG_SYNC, event
					.getMessage().getContent()));
		} catch (IOException e) {

		}
	}

	@XEventHandler(sync = true, channel = StressTest.STRESSTEST_CHANNEL_PONG_SYNC)
	public void onStressTestPongSync(XServerMessageIncomingEvent event) {
		try {
			StressTest.receive(event.getMessage());
		} catch (IOException e) {

		}
	}

	@XEventHandler(sync = false, channel = StressTest.STRESSTEST_CHANNEL_PING_ASYNC)
	public void onStressTestPingAsync(XServerMessageIncomingEvent event) {
		// Do Pong
		try {
			event.getServer().sendMessage(manager.createMessage(StressTest.STRESSTEST_CHANNEL_PONG_ASYNC, event
					.getMessage().getContent()));
		} catch (IOException e) {

		}
	}

	@XEventHandler(sync = false, channel = StressTest.STRESSTEST_CHANNEL_PONG_ASYNC)
	public void onStressTestPongAsync(XServerMessageIncomingEvent event) {
		try {
			StressTest.receive(event.getMessage());
		} catch (IOException e) {

		}
	}

}
