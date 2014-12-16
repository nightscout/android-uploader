package com.nightscout.android.sms;

public class SMSMessage {
	private String sender;
	private String message;
	SMSMessage(){}
	SMSMessage(String Sender, String Message)
	{
		sender = Sender;
		message = Message;
	}
	public String getSender() {
		return sender;
	}
	public void setSender(String sender) {
		this.sender = sender;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
}
