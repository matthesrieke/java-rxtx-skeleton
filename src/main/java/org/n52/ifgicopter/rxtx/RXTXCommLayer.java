package org.n52.ifgicopter.rxtx;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.TooManyListenersException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import gnu.io.CommPortIdentifier;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import gnu.io.UnsupportedCommOperationException;

/**
 * RXTX implementation of the Comm Layer.
 * 
 * @author Matthes Rieke <m.rieke@uni-muenster.de>
 * 
 */
public class RXTXCommLayer implements SerialPortEventListener {

	private InputStream inputStream;
	private SerialPort serialPort;
	private String preferredPort;
	private OutputStream outputStream;
	private ByteArrayOutputStream byteBuffer;
	private final Log log = LogFactory.getLog(RXTXCommLayer.class);
	private DataHandler handler;

	public static void main(String[] args) throws Exception {
		new RXTXCommLayer("COM3", new SysOutHandler());
	}

	/**
	 * @param handler
	 *            the main handler
	 * @param port
	 *            the preferred com port
	 * @throws Exception
	 *             if init failes.
	 */
	public RXTXCommLayer(String port, DataHandler h) throws Exception {
		this.preferredPort = port;
		this.handler = h;
		initialize();
	}

	private void initialize() throws UnsupportedCommOperationException, IOException, TooManyListenersException {
		findPort();
		
		if (this.serialPort == null) throw new IOException("Could not open port "+ this.preferredPort);
		
		this.serialPort.setSerialPortParams(9600, SerialPort.DATABITS_8,
				SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
		this.serialPort.notifyOnDataAvailable(true);
		this.serialPort.notifyOnOutputEmpty(false);
		this.inputStream = this.serialPort.getInputStream();
		this.outputStream = this.serialPort.getOutputStream();
		this.byteBuffer = new ByteArrayOutputStream();
		this.serialPort.addEventListener(this);
	}

	private void findPort() {
		Enumeration<?> portList = CommPortIdentifier.getPortIdentifiers();
		CommPortIdentifier portId = null;
		while (portList.hasMoreElements()) {
			portId = (CommPortIdentifier) portList.nextElement();
			if (portId.getPortType() == CommPortIdentifier.PORT_SERIAL
					&& portId.getName().equals(this.preferredPort)) {
				try {
					this.serialPort = (SerialPort) portId.open("Com Handler "
							+ portId.getName(), 2000);
				} catch (PortInUseException e) {
				}
			}
		}
	}

	/**
	 * Shuts down the COM port.
	 * 
	 * @throws Exception
	 *             if failed
	 */
	public void shutdown() throws Exception {
		if (this.serialPort == null)
			return;

		this.inputStream.close();
		this.outputStream.close();

		/*
		 * close the port
		 */
		this.serialPort.close();
		if (this.log.isInfoEnabled()) {
			this.log.info("COM Port module shutdown. Port "
					+ this.preferredPort + " freed.");
		}
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see gnu.io.SerialPortEventListener#serialEvent(gnu.io.SerialPortEvent)
	 */
	public void serialEvent(SerialPortEvent event) {
		if (event.getEventType() == SerialPortEvent.DATA_AVAILABLE) {

			try {
				int nextByte, previousByte = -1;
				while (this.inputStream.available() > 0) {
					nextByte = this.inputStream.read();
					if (nextByte == (int) '\n') {
						
						/*
						 * last byte of cmd received
						 */
						this.byteBuffer.flush();

						if (this.byteBuffer.size() > 0  && previousByte == (int) '\r') {
							byte[] recv = RXTXCommLayer.this.byteBuffer
									.toByteArray();

							this.handler.processData(recv);
						}
						this.byteBuffer.reset();
					} else {
						/*
						 * write to tmp buffer
						 */
						this.byteBuffer.write(nextByte);
					}
					previousByte = nextByte;
				}
			} catch (IOException e) {
				this.log.warn(e.getMessage(), e);
			}

		} else {
			if (this.log.isInfoEnabled()) {
				this.log.info("Event type with no handling: " + event.getEventType());
			}
		}
	}


}
