package com.mickare.xserver.events;

public class XServerConnectionDenied extends XServerEvent {

	private final String xserverName;
	private final String xserverPassword;
	private final String host;
	private final int port;
	
	public XServerConnectionDenied(String xserverName, String xserverPassword, String host, int port) {
		super("The login request from " + xserverName + " from " + host + ":" + port + " with the password " + xserverPassword + " was denied!");
		this.xserverName = xserverName;
		this.xserverPassword = xserverPassword;
		this.host = host;	
		this.port = port;	
	}

	@Override
	public void postCall() {
		// TODO Auto-generated method stub
		
	}

	public int getPort()
	{
		return port;
	}

	public String getXserverPassword()
	{
		return xserverPassword;
	}

	public String getXserverName()
	{
		return xserverName;
	}

	public String getHost()
	{
		return host;
	}
	
}
