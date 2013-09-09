package com.mickare.xserver.listener;

import java.io.IOException;

import com.mickare.xserver.AbstractXServerManager;
import com.mickare.xserver.XServerListener;
import com.mickare.xserver.annotations.XEventHandler;
import com.mickare.xserver.events.XServerMessageIncomingEvent;
import com.mickare.xserver.exceptions.NotConnectedException;
import com.mickare.xserver.stresstest.StressTest;

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
		} catch (NotConnectedException | IOException e) {

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
			event.getServer().sendMessage(manager.createMessage(StressTest.STRESSTEST_CHANNEL_PONG_SYNC, event
					.getMessage().getContent()));
		} catch (NotConnectedException | IOException e) {

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
