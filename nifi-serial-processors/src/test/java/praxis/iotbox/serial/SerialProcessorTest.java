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

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.processor.Processor;
import org.apache.nifi.processor.exception.FlowFileHandlingException;
import org.apache.nifi.stream.io.StreamUtils;
import org.apache.nifi.util.MockFlowFile;
import org.apache.nifi.util.MockProcessSession;
import org.apache.nifi.util.SharedSessionState;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class SerialProcessorTest {

	private TestRunner testRunner;

	@Before
	public void init() {
		testRunner = TestRunners.newTestRunner(SerialProcessor.class);
	}

	@Test
	public void testProcessor() {

		final Processor processor = new SerialProcessor();
		final MockProcessSession session = new MockProcessSession(new SharedSessionState(processor, new AtomicLong(0L)),
				processor);
		FlowFile flowFile = session.createFlowFile("HELLO    WORLD".getBytes());

		final InputStream in = session.read(flowFile);
		final byte[] buffer = new byte[14];
		try {
			StreamUtils.fillBuffer(in, buffer);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		assertEquals("HELLO    WORLD", new String(buffer));

		session.remove(flowFile);

		try {
			session.commit();
			Assert.fail("Was able to commit session without closing InputStream");
		} catch (final FlowFileHandlingException ffhe) {
			System.out.println(ffhe.toString());
		}

	}

}
