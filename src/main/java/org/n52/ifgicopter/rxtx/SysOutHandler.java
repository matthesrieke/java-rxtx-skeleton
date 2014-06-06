package org.n52.ifgicopter.rxtx;

public class SysOutHandler implements DataHandler {

	@Override
	public void processData(byte[] recv) {
		System.out.println(new String(recv).trim());
	}

}
