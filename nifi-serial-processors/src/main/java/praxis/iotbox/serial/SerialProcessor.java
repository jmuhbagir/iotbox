/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package praxis.iotbox.serial;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.nifi.annotation.behavior.ReadsAttribute;
import org.apache.nifi.annotation.behavior.ReadsAttributes;
import org.apache.nifi.annotation.behavior.WritesAttribute;
import org.apache.nifi.annotation.behavior.WritesAttributes;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.SeeAlso;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.annotation.lifecycle.OnScheduled;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.processor.AbstractProcessor;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.ProcessorInitializationContext;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.util.StandardValidators;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortInvalidPortException;

@Tags({ "example" })
@CapabilityDescription("Provide a description")
@SeeAlso({})
@ReadsAttributes({ @ReadsAttribute(attribute = "", description = "") })
@WritesAttributes({ @WritesAttribute(attribute = "", description = "") })
public class SerialProcessor extends AbstractProcessor {

	public PropertyDescriptor PORT;

	public static final PropertyDescriptor BAUDRATE = new PropertyDescriptor.Builder().name("BAUDRATE")
			.displayName("baud rate").description("Device serial baud rate").defaultValue("9600").required(true)
			.addValidator(StandardValidators.NON_EMPTY_VALIDATOR).build();

	public static final PropertyDescriptor DATABITS = new PropertyDescriptor.Builder().name("DATABITS")
			.displayName("Data Bits").description("Device serial data bits (7/8)").defaultValue("7").required(true)
			.addValidator(StandardValidators.NON_EMPTY_VALIDATOR).build();

	public static final PropertyDescriptor PARITY = new PropertyDescriptor.Builder().name("PARITY")
			.displayName("parity").description("Device serial parity (NONE, ODD, EVEN, MARK)").defaultValue("EVEN")
			.required(true).addValidator(StandardValidators.NON_EMPTY_VALIDATOR).build();

	public static final PropertyDescriptor STOPBITS = new PropertyDescriptor.Builder().name("STOPBITS")
			.displayName("Stop Bits").description("Device serial stop bits (1/2)").defaultValue("1").required(true)
			.addValidator(StandardValidators.NON_EMPTY_VALIDATOR).build();

	public static final Relationship DATA = new Relationship.Builder().name("data").description("On new data updated")
			.build();

	public static final Relationship ERROR = new Relationship.Builder().name("error")
			.description("Error on listen serial device").build();

	public static final Relationship IDLE = new Relationship.Builder().name("idle")
			.description("Idle on waiting new data").build();

	private List<PropertyDescriptor> descriptors;

	private Set<Relationship> relationships;

	SerialPort port;
	FlowFile flowFile;

	@Override
	protected void init(final ProcessorInitializationContext context) {

		SerialPort[] ports = SerialPort.getCommPorts();
		Set<String> values = new HashSet<String>();
		int i = 0;
		for (SerialPort p : ports) {
			values.add(i + "," + p.getSystemPortName());
		}

		PORT = new PropertyDescriptor.Builder().name("PORT").displayName("port").description("Device serial ports")
				.allowableValues(values).required(true).addValidator(StandardValidators.NON_EMPTY_VALIDATOR).build();

		final List<PropertyDescriptor> descriptors = new ArrayList<PropertyDescriptor>();
		descriptors.add(PORT);
		descriptors.add(BAUDRATE);
		descriptors.add(DATABITS);
		descriptors.add(STOPBITS);
		descriptors.add(PARITY);
		this.descriptors = Collections.unmodifiableList(descriptors);

		final Set<Relationship> relationships = new HashSet<Relationship>();
		relationships.add(DATA);
		relationships.add(ERROR);
		relationships.add(IDLE);
		this.relationships = Collections.unmodifiableSet(relationships);

	}

	@Override
	public Set<Relationship> getRelationships() {
		return this.relationships;
	}

	@Override
	public final List<PropertyDescriptor> getSupportedPropertyDescriptors() {
		return descriptors;
	}

	@OnScheduled
	public void onScheduled(final ProcessContext context) {

	}

	@OnScheduled
	public void onStop(final ProcessContext context) {
		if (port != null && !port.isOpen()) {
			port.closePort();
		}
	}

	@OnScheduled
	public void onShutdown(final ProcessContext context) {
		if (port != null && !port.isOpen()) {
			port.closePort();
		}
	}

	@Override
	public void onTrigger(final ProcessContext context, final ProcessSession session) throws ProcessException {
		this.getLogger().info("Welcome to Serial Processor by jmuhbagir");
		int parity = getParity(context.getProperty(PARITY).getValue());
		int baudrate = context.getProperty(BAUDRATE).asInteger();
		int databits = context.getProperty(DATABITS).asInteger();
		int stopbits = context.getProperty(STOPBITS).asInteger();

		try {
			flowFile = session.create();
			port = SerialPort.getCommPorts()[Integer.parseInt(context.getProperty(PORT).getValue().split(",")[0])];
			port.setComPortParameters(baudrate, databits, stopbits, parity);
			if (port.openPort()) {
				this.getLogger().info("Serial Device Connection Established");
				int len = 0;
				StringBuffer sb = new StringBuffer();
				while (true) {
					while ((len = port.bytesAvailable()) > 0) {
						byte[] readBuffer = new byte[len];
						int numbytes = port.readBytes(readBuffer, readBuffer.length);
						if (numbytes < 0)
							break;
						sb.append(new String(readBuffer, 0, numbytes));
					}
					
					if (sb.toString().endsWith("END OF WT SUMMARY")) {
						byte[] buff = sb.toString().getBytes();
						flowFile = session.write(flowFile,
								outputStream -> outputStream.write(buff));
						session.getProvenanceReporter().create(flowFile);
						session.transfer(flowFile, DATA);
						break;
					}
					
				}
			} else {
				this.getLogger().info("Failed To Connect To Serial Device");
				session.transfer(flowFile, IDLE);
			}

			port.closePort();

		} catch (SerialPortInvalidPortException e1) {
			port.closePort();
			session.transfer(flowFile, ERROR);
			throw new ProcessException(e1.getMessage());
		}
	}

	private int getParity(String value) {
		if (value.equalsIgnoreCase("EVEN")) {
			return SerialPort.EVEN_PARITY;
		} else if (value.equalsIgnoreCase("ODD")) {
			return SerialPort.ODD_PARITY;
		} else if (value.equalsIgnoreCase("MARK")) {
			return SerialPort.MARK_PARITY;
		} else {
			return SerialPort.NO_PARITY;
		}
	}
}
