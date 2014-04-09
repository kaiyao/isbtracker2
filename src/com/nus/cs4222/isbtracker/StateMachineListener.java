package com.nus.cs4222.isbtracker;

public interface StateMachineListener {
	public void onStateMachineChanged();
	public void onLogMessage(String message);
}
