package com.miao.tlv;

import com.miao.tlv.enums.Len;
import com.miao.tlv.nni.Nni;
import com.miao.tlv.nni.Options;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;

@Slf4j
public class NniTest {

	@Before
	public void before() {

	}

	@Test
	public void testMove() {
		int n = 256;

		String.valueOf(1213);
		new StringBuffer();


		// actual：65535
		// expect：[-1, -1]
		Encodable encodable = Nni.NNI(65537);
		byte[] output = Encoder.encode(encodable, 10);
		String outputStr = Arrays.toString(output);
		Assert.assertTrue(String.format("expected: %s", outputStr), "[0, 1, 0, 1]".equals(outputStr));


		encodable = Nni.NNI(4294967296l);
		output = Encoder.encode(encodable, 10);

		for (int i = 0; i < output.length; i++) {
			// System.out.println(output[i] + " --> " + Strings.printTT(output[i]));
			System.out.print(Strings.printTT(output[i]) + ",");
		}
		Assert.assertTrue(String.format("expected: %s", outputStr), "[-1, -1, -1, -1]".equals(outputStr));

	}


	@Test
	public void testNniEncode() {
		// actual：255
		// expect：[-3]
		Encodable encodable = Nni.NNI(255);
		byte[] output = Encoder.encode(encodable, 10);
		String outputStr = Arrays.toString(output);
		Assert.assertTrue(String.format("expected: %s", outputStr), "[-1]".equals(outputStr));

		// actual：256
		// expect：[-1, 0]
		encodable = Nni.NNI(256);
		output = Encoder.encode(encodable, 10);
		outputStr = Arrays.toString(output);
		Assert.assertTrue(String.format("expected: %s", outputStr), "[1, 0]".equals(outputStr));


		// actual：65535
		// expect：[-1, -1]
		encodable = Nni.NNI(65535);
		output = Encoder.encode(encodable, 10);
		outputStr = Arrays.toString(output);
		Assert.assertTrue(String.format("expected: %s", outputStr), "[-1, -1]".equals(outputStr));

		encodable = Nni.NNI(4294967295l);
		output = Encoder.encode(encodable, 10);
		outputStr = Arrays.toString(output);
		Assert.assertTrue(String.format("expected: %s", outputStr), "[-1, -1, -1, -1]".equals(outputStr));

		encodable = Nni.NNI(655356);
		output = Encoder.encode(encodable, 10);
		outputStr = Arrays.toString(output);
		Assert.assertTrue(String.format("expected: %s", outputStr), "[0, 9, -1, -4]".equals(outputStr));
	}

	@Test
	public void testNniDecode() {
		byte[] encodedBytes = getNniEncodedBytes(255);
		Number number = Nni.decode(ByteBuffer.wrap(encodedBytes));
		Assert.assertTrue(String.format("expected: 255, actual: %s", number), 255 == number.intValue());

		encodedBytes = getNniEncodedBytes(65535);
		number = Nni.decode(ByteBuffer.wrap(encodedBytes));
		Assert.assertTrue(String.format("expected: 65535, actual: %s", number), 65535 == number.intValue());

		encodedBytes = getNniEncodedBytes(4294967295l);
		number = Nni.decode(ByteBuffer.wrap(encodedBytes));
		Assert.assertTrue(String.format("expected: 4294967295, actual: %s", number), 4294967295l == number.longValue());

		encodedBytes = getNniEncodedBytes(655356);
		number = Nni.decode(ByteBuffer.wrap(encodedBytes));
		Assert.assertTrue(String.format("expected: 655356, actual: %s", number), 655356 == number.longValue());
	}


	@Test
	public void test8NumberIntegerEncodeDecode() {
		// [0, 0, 0, 1, 0, 0, 0]
		byte[] encodedBytes = getNniEncodedBytes(4294967296l);
		Number number = Nni.decode(ByteBuffer.wrap(encodedBytes));
		Assert.assertTrue(String.format("expected: 4294967296, actual: %s", number), 4294967296l == number.longValue());

		// 0x7fffffffffffffffL
		long lValue = Long.MAX_VALUE;

		// [0, 0, 0, 1, 0, 0, 0]
		encodedBytes = getNniEncodedBytes(9007199254740991L);
		number = Nni.decode(ByteBuffer.wrap(encodedBytes));
		Assert.assertTrue(String.format("expected: 9007199254740991L, actual: %s", number), 9007199254740991L == number.longValue());


		long time = System.currentTimeMillis();
		// 1691474477251 (1 6914 7447 7251) -> [0, 0, 1, -119, -45, -68, 64, -61]
		encodedBytes = getNniEncodedBytes(time);
		number = Nni.decode(ByteBuffer.wrap(encodedBytes));
		Assert.assertTrue(String.format("expected: %s, actual: %s", time, number), time == number.longValue());

		try {
			lValue = Commons.MAX_SAFE_INTEGER + 2;
			encodedBytes = getNniEncodedBytes(lValue);
			number = Nni.decode(ByteBuffer.wrap(encodedBytes));
			Assert.assertTrue(false);
		} catch (IllegalArgumentException e) {
			Assert.assertTrue(true);
		} catch (Exception e) {
			Assert.assertTrue(false);
		}

		// 9007199254740993  -> [0, 32, 0, 0, 0, 0, 0, 1]
		lValue = Commons.MAX_SAFE_INTEGER + 2;
		encodedBytes = getNniEncodedBytes(lValue, Options.of(null, true));
		number = Nni.decode(ByteBuffer.wrap(encodedBytes));
		Assert.assertTrue(String.format("expected: %s, actual: %s", lValue, number), lValue == number.longValue());
	}


	@Test
	public void testBigIntegerEncodeDecode() {
		// [0, 0, 0, 0, 1, 0, 0, 0]
		byte[] encodedBytes = getNniEncodedBytes(new BigInteger("4294967296"));
		Number number = Nni.decode(ByteBuffer.wrap(encodedBytes));
		Assert.assertTrue(String.format("expected: 4294967296, actual: %s", number), 4294967296l == number.longValue());

		// 1691475010521 -> [0, 0, 1, -119, -45, -60, 99, -39]
		long time = System.currentTimeMillis();
		encodedBytes = getNniEncodedBytes(BigInteger.valueOf(time));
		number = Nni.decode(ByteBuffer.wrap(encodedBytes));
		Assert.assertTrue(String.format("expected: %s, actual: %s", time, number), time == number.longValue());


		long lValue = Commons.MAX_SAFE_INTEGER + 2;
		encodedBytes = getNniEncodedBytes(BigInteger.valueOf(lValue));
		number = Nni.decode(ByteBuffer.wrap(encodedBytes));
		Assert.assertTrue(String.format("expected: %s, actual: %s", lValue, number), lValue == number.longValue());
	}

	@Test
	public void testLenEncodeDecode() {
		// 4294967296 - > [0, 0, 0, 0, 1, 0, 0, 0]
		Options options = Options.of(Len.LEN_8);
		byte[] encodedBytes = getNniEncodedBytes(new BigInteger("4294967296"), options);
		Number number = Nni.decode(ByteBuffer.wrap(encodedBytes), options);
		Assert.assertTrue(String.format("expected: 4294967296, actual: %s", number), 4294967296l == number.longValue());
	}

	private byte[] getNniEncodedBytes(Number n) {
		Encodable encodable = Nni.NNI(n);
		return Encoder.encode(encodable, 10);
	}

	private byte[] getNniEncodedBytes(Number n, Options options) {
		Encodable encodable = Nni.NNI(n, options);
		return Encoder.encode(encodable, 10);
	}

	private void log(Object o) {
		System.out.println("log: " + o);
	}

}
