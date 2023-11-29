package com.miao.tlv;

import cn.hutool.core.exceptions.ExceptionUtil;
import com.miao.tlv.nni.Nni;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.Arrays;

import static com.miao.tlv.Strings.hexToByteArray;

@Slf4j
public class ExtensibleTest {

	private ExtensionRegistry<ExtTestTarget> extensionRegistry;

	private EvDecoder<ExtTestTarget> evdDecoder;

	@Before
	public void before() {
		extensionRegistry = new ExtensionRegistry<ExtTestTarget>();
		extensionRegistry.registerExtension(new A1Extension());
		extensionRegistry.registerExtension(new A2Extension());

		evdDecoder = new EvDecoder<ExtTestTarget>("ExtTestTarget")
				.setUnknown((ExtTestTarget target, TLV tlv, Number order) -> extensionRegistry.decodeUnknown(target, tlv, order));
	}

	@Test
	public void testEncode() {
		// actual: 0xA0, 0x00 -> [-96, 0]
		ExtTestTarget target1 = new ExtTestTarget(extensionRegistry);
		byte[] testTargetBytes = Encoder.encode(target1);
		Assert.assertTrue(String.format("target1 encoded bytes should be [-96, 0], current %s", Arrays.toString(testTargetBytes)), "[-96, 0]".equals(Arrays.toString(testTargetBytes)));

		// 添加数据
		ExtensionRegistry.set(target1, 0xA1, 5);
		ExtensionRegistry.set(target1, 0xA2, true);
		byte[] target1Bytes = Encoder.encode(target1);

		// [0xA0, 0x05, 0xA2, 0x00, 0xA1, 0x01, 0x05] -> (hexToByteArray): [-96, 5, -94, 0, -95, 1, 5]
		Assert.assertTrue(String.format("target1 encoded bytes should be [-96, 5, -94, 0, -95, 1, 5], current %s", Arrays.toString(target1Bytes)), "[-96, 5, -94, 0, -95, 1, 5]".equals(Arrays.toString(target1Bytes)));

		// clear
		ExtensionRegistry.clear(target1);
	}

	@Test
	public void testCloneRecordAndClear() {
		ExtTestTarget target1 = new ExtTestTarget(extensionRegistry);
		ExtensionRegistry.set(target1, 0xA1, 5);
		ExtensionRegistry.set(target1, 0xA2, true);

		// clone
		ExtTestTarget target2 = new ExtTestTarget(extensionRegistry);
		ExtensionRegistry.cloneRecord(target2, target1);

		byte[] target2Bytes = Encoder.encode(target2);

		// [0xA0, 0x05, 0xA2, 0x00, 0xA1, 0x01, 0x05] -> (hexToByteArray): [-96, 5, -94, 0, -95, 1, 5]
		Assert.assertTrue(String.format("target1 encoded bytes should be [-96, 5, -94, 0, -95, 1, 5], current %s", Arrays.toString(target2Bytes)),
				"[-96, 5, -94, 0, -95, 1, 5]".equals(Arrays.toString(target2Bytes)));

		ExtensionRegistry.clear(target1, 0xA1);
		ExtensionRegistry.set(target1, 0xA2, false);
		byte[] target1Bytes = Encoder.encode(target1);
		// [0xA0, 0x00] -> (hexToByteArray): [-96, 0]
		Assert.assertTrue(String.format("target1 encoded bytes should be [-96, 0], current %s", Arrays.toString(target1Bytes)),
				"[-96, 0]".equals(Arrays.toString(target1Bytes)));

		// clear
		ExtensionRegistry.clear(target1);
		ExtensionRegistry.clear(target2);
	}

	@Test
	public void testThrowUnknownExtension() {
		// EXTENSIONS 中只有 [0xA1, 0xA2]
		ExtTestTarget target = new ExtTestTarget(extensionRegistry);
		ExtensionRegistry.set(target, 0xA1, 5);
		ExtensionRegistry.set(target, 0xA3, 1);

		try {
			byte[] target1Bytes = Encoder.encode(target);
			// Assert.assertTrue("Error should be thrown out", false);
		} catch (Throwable e) {
			System.out.println(e.getMessage() + "\n" + ExceptionUtil.getMessage(e));
			Assert.assertTrue(e.getMessage().equals("unknown extension type " + 0xA3));
			Assert.assertTrue("Error should be thrown out", true);
		}

		// clear
		ExtensionRegistry.clear(target);
	}

	@Test
	public void testDecode() {
		// first
		Decoder decoder = new Decoder(hexToByteArray(
				new int[]{
						0xA0, 0x06,
						0xA1, 0x01, 0x01,
						0xA1, 0x01, 0x03,
				}
		));

		// 不能再按照 unknown 中进行解码
		ExtTestTarget target = evdDecoder.decode(new ExtTestTarget(extensionRegistry), decoder);
		Object o1 = ExtensionRegistry.get(target, 0xA1);
		Assert.assertTrue("o1 should be 4", o1 instanceof Number && ((Number) o1).intValue() == 4);
		Object o2 = ExtensionRegistry.get(target, 0xA2);
		Assert.assertTrue("o2 should be null", o2 == null);


		// another
		decoder = new Decoder(hexToByteArray(
				new int[]{
						0xA0, 0x02,
						0xA2, 0x00,
				}
		));
		target = evdDecoder.decode(new ExtTestTarget(extensionRegistry), decoder);
		// A1Extension: encoder.prependTlv(this.tt.longValue(), Nni.NNI(value));
		o1 = ExtensionRegistry.get(target, 0xA1);
		Assert.assertTrue("o1 should be null", o1 == null);
		// A2Extension.decode return false
		o2 = ExtensionRegistry.get(target, 0xA2);
		Assert.assertTrue("o2 should be 1", o2 instanceof Boolean && !((Boolean) o2).booleanValue());

		// third
		decoder = new Decoder(hexToByteArray(
				new int[]{
						0xA0, 0x02,
						0xA3, 0x00, // not matching any extension, critical
				}
		));
		try {
			target = evdDecoder.decode(new ExtTestTarget(extensionRegistry), decoder);
			Assert.assertTrue("Error should be thrown out", false);
		} catch (Throwable e) {
			// TLV-TYPE 163 is unknown in ExtTestTarget
			System.out.println(e.getMessage() + "\n" + ExceptionUtil.getMessage(e));
			Assert.assertTrue("Error should be thrown out", true);
		}


		// clear
		ExtensionRegistry.clear(target);
	}


	@After
	public void after() {
		extensionRegistry.unregisterExtension(0xA1);
		extensionRegistry.unregisterExtension(0xA2);
	}

}

class A1Extension<ExtTestTarget> extends Extension<ExtTestTarget, Number> {

	@Getter
	private Number tt = 0xA1;
	@Getter
	private Number order = 0xA3;

	@Override
	Encodable encode(ExtTestTarget obj, Number value) {
		return (encoder) -> {
			encoder.prependTlv(this.tt.longValue(), Nni.NNI(value));
		};
	}

	@Override
	Number decode(ExtTestTarget obj, TLV tlv, Number accumulator) {
		Number value = Nni.decode(ByteBuffer.wrap(tlv.getValue()));
		Number accumulateNumber = accumulator == null ? 0 : accumulator;
		return value.longValue() + accumulateNumber.longValue();
	}

}

class A2Extension<ExtTestTarget> extends Extension<ExtTestTarget, Boolean> {
	@Getter
	private Number tt = 0xA2;

	@Override
	Boolean decode(ExtTestTarget obj, TLV tlv, Boolean accumulator) {
		return false;
	}

	@Override
	Encodable encode(ExtTestTarget obj, Boolean value) {
		if (value) {
			return encoder -> encoder.prependTlv(tt.longValue(), null);
		}

		return null;
	}
}

class ExtTestTarget extends Extensible implements Encodable {

	public ExtTestTarget(ExtensionRegistry<ExtTestTarget> extensibleRegistry) {
		super.extensibleRegistry = extensibleRegistry;
	}

	@Override
	public void encodeTo(Encoder encoder) {
		encoder.prependTlv(0xA0, super.extensibleRegistry.encode(this));
	}

}