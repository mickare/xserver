package com.mickare.xserver.events;

public class XServerConnectionDenied extends XServerEvent {

	public final String sender;
	public final String t_name;
	public final String t_password;
	
	public XServerConnectionDenied(String sender, String t_name, String t_password) {
		super("The login request from " + sender + " to " + t_name + " with the md5-password " + t_password + " was denied!");
		this.sender = sender;
		this.t_name = t_name;
		this.t_password = t_password;	
	}

	@Override
	public void postCall() {
		// TODO Auto-generated method stub
		
	}
	
}
