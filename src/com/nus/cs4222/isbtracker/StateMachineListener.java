package com.nus.cs4222.isbtracker;

public interface StateMachineListener {
	public void onStateMachineChanged(StateMachine.State state);
	public void onLogMessage(String message);
}
