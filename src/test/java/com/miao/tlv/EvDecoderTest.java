package com.miao.tlv;

import cn.hutool.core.exceptions.ExceptionUtil;
import com.miao.tlv.nni.Nni;
import com.google.common.collect.Lists;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

import static com.miao.tlv.Strings.hexToByteArray;

@Slf4j
public class EvDecoderTest {
	private EvDecoder<EvdTestTarget> evDecoder;

	@Before
	public void before() {
		evDecoder = new EvDecoder<EvdTestTarget>("A0", 0xA0);

		// input: T1-L1-[T11-L11-V11, T12-L12-[ TL13-L13-V13]]
		// nest 相当于把其他的 EvDecoder 对象的规则嵌入到当前的 EvDecoder
		evDecoder.add(0xA1, (t, tlv) -> { // order（161） = 200 , required = false
					t.setA1(t.getA1() + 1);
					// 会导致原来的 position 发生变化，从3 到 5，相当于解析了 A1
					tlv.getDecoder().decode(new A1());
				}).add(0xA4, (t, tlv) -> { // order（164） = 300 , required = false
					t.setA4(t.getA4() + 1);
				}).add(0xA6, (t, tlv) -> { // order（166） = 400 , required = true
					t.setA6(t.getA6() + 1);
				}, RuleOptions.of(true))
				.add(0xA9, (t, tlv) -> { // order（169） = 500 , required = false
					t.setA9(t.getA9() + 1);
				}).add(0xC0, new EvDecoder<EvdTestTarget>("C0").add(0xC1, (t, tlv) -> { // order（192） = 600 , required = false
					// 适合复杂类型的TLV结构的解码
					t.setC1(Nni.decode(ByteBuffer.wrap(tlv.getValue())).longValue());
				}));
	}

	@Test
	public void testDecodeNormal() {
		// input: [160, 17, 161, 1, 16, 164, 0, 166, 0, 166, 0, 169, 0, 192, 4, 193, 2, 1, 4]
		byte[] input = hexToByteArray(new int[]{
				0xA0, 0x11,
				0xA1, 0x01, 0x10,
				0xA4, 0x00,
				0xA6, 0x00,
				0xA6, 0x00,
				0xA9, 0x00,
				0xC0, 0x04, 0xC1, 0x02, 0x01, 0x04,
		});

		// byte array: [-96, 17, -95, 1, 16, -92, 0, -90, 0, -90, 0, -87, 0, -64, 4, -63, 2, 1, 4]
		System.out.println("byte array: " + Arrays.toString(input));

		EvdTestTarget target = new EvdTestTarget();
		// add before/after observer
		target.setCallbacks(evDecoder);

		evDecoder.decode(target, new Decoder(ByteBuffer.wrap(input)));

		// target expect:               a1=1，a4=1, a6=2, a9=1, c1=260, c2=0 , sum: 1121
		// target actual: EvdTestTarget{a1=1, a4=1, a6=2, a9=1, c1=260, c2=0}, sum: 1121
		// target.sum = this.a1 * 1000 + this.a4 * 100 + this.a6 * 10 + this.a9;
		System.out.println("target: " + target + ", sum: " + target.sum());
		Assert.assertTrue("sum == 1121", target.sum().intValue() == 1121);
		Assert.assertTrue("c1 == 0x0104", target.c1 == 0x0104);

		// TODO
		EvdTestTarget target2 = new EvdTestTarget();
		target2.setCallbacks(evDecoder);
		EvdTestTarget target3 = evDecoder.decodeValue(target2, new Decoder(Arrays.copyOfRange(input, 2, input.length)));
		Assert.assertTrue("sum == 1121", target3.sum().intValue() == 1121);
		Assert.assertTrue("c1 == 0x0104", target3.c1 == 0x0104);
	}

	@Test
	public void testDecodeUnknownNonCritical() {
		Decoder decoder = new Decoder(hexToByteArray(new int[]{
				0xA0, 0x02,
				0xA2, 0x00, // non-critical
		}));
		EvdTestTarget target = new EvdTestTarget();

		evDecoder.decode(target, decoder);
		Assert.assertTrue(target.sum().intValue() == 0);
	}

	@Test
	public void testDecodeUnknownCritical() {
		Decoder decoder = new Decoder(hexToByteArray(new int[]{
				0xA0, 0x02,
				0xA3, 0x00,
		}));
		EvdTestTarget target = new EvdTestTarget();

		try {
			evDecoder.decode(target, decoder);
		} catch (Throwable e) {
			System.out.println(e.getMessage() + "\n" + ExceptionUtil.getMessage(e));
			Assert.assertTrue(e.getMessage().equals("TLV-TYPE 163 is unknown in A0"));
			Assert.assertTrue("Error should be thrown out", true);
		}
	}


	@Test
	public void testDecodeUnknownCriticalInGrandfatheredRange() {
		Decoder decoder = new Decoder(hexToByteArray(new int[]{
				0xA0, 0x02,
				0x10, 0x00, // 0x10
		}));
		EvdTestTarget target = new EvdTestTarget();

		try {
			evDecoder.decode(target, decoder);
		} catch (Throwable e) {
			System.out.println(e.getMessage() + "\n" + ExceptionUtil.getMessage(e));
			Assert.assertTrue(e.getMessage().equals("TLV-TYPE 16 is unknown in A0"));
			Assert.assertTrue("Error should be thrown out", true);
		}
	}

	@Test
	public void testDecodeNonRepeatable() {
		Decoder decoder = new Decoder(hexToByteArray(new int[]{
				0xA0, 0x05,
				0xA1, 0x01, 0x10,
				0xA1, 0x00, // cannot repeat , 0xA1=16
		}));
		EvdTestTarget target = new EvdTestTarget();

		try {
			evDecoder.decode(target, decoder);
		} catch (Throwable e) {
			System.out.println(e.getMessage() + "\n" + ExceptionUtil.getMessage(e));
			Assert.assertTrue(e.getMessage().equals("TLV-TYPE 161 cannot repeat in A0"));
			Assert.assertTrue("Error should be thrown out", true);
		}
	}

	@Test
	public void testDecodeOutOfOrderCritical() {
		Decoder decoder = new Decoder(hexToByteArray(new int[]{
				0xA0, 0x04,
				0xA4, 0x00,
				0xA1, 0x00,  // is critical : 0xA1 % 2 == 1
		}));
		EvdTestTarget target = new EvdTestTarget();

		try {
			evDecoder.decode(target, decoder);
		} catch (Throwable e) {
			System.out.println(e.getMessage() + "\n" + ExceptionUtil.getMessage(e));
			Assert.assertTrue(e.getMessage().equals("TLV-TYPE 161 is out of order in A0"));
			Assert.assertTrue("Error should be thrown out", true);
		}

	}


	@Test
	public void testDecodeOutOfOrderNonCritical() {
		Decoder decoder = new Decoder(hexToByteArray(new int[]{
				0xA0, 0x06,
				0xA6, 0x00,
				0xA9, 0x00,
				0xA6, 0x00,  // non critical : 0xA6 % 2 ==0
		}));
		EvdTestTarget target = new EvdTestTarget();

		try {
			evDecoder.decode(target, decoder);
			Assert.assertTrue("no Error should be thrown out", true);
			System.out.println("target: " + target);

			// {a1=0, a4=0, a6=1, a9=1, c1=0, c2=0}
			Assert.assertTrue(target.getA1() == 0
					&& target.getA4() == 0
					&& target.getA6() == 1
					&& target.getA9() == 1
					&& target.getC1() == 0
					&& target.getC2() == 0
			);
		} catch (Throwable e) {
			System.out.println(e.getMessage() + "\n" + ExceptionUtil.getMessage(e));
			Assert.assertTrue("Error should be thrown out", false);
		}
	}

	@Test
	public void testDecodeBadTlvType() {
		Decoder decoder = new Decoder(hexToByteArray(new int[]{
				0xAF, 0x00,
		}));
		EvdTestTarget target = new EvdTestTarget();
		target.setCallbacks(evDecoder);

		try {
			evDecoder.decode(target, decoder);
		} catch (Throwable e) {
			e.printStackTrace();
			System.out.println(e.getMessage() + "\n" + ExceptionUtil.getMessage(e));
			Assert.assertTrue("Error should be thrown out", true);
			Assert.assertTrue(e.getMessage().equals("TLV-TYPE 175 is not A0"));
		}
	}

	@Test
	public void testDecodeRequired() {
		EvDecoder evDecoder = new EvDecoder<EvdTestTarget>("A0", 0xA0)
				.add(0xA9, (t, tlv) -> {
					t.setA9(t.getA9() + 1);
				}, RuleOptions.of(3, false, true))
				.add(0xA4, (t, tlv) -> {
					t.setA4(t.getA4() + 1);
				}, RuleOptions.of(1))
				.add(0xA1, (t, tlv) -> {
					t.setA1(t.getA1() + 1);
				}, RuleOptions.of(2, false, true));


		Decoder decoder = new Decoder(hexToByteArray(new int[]{
				// first object, OK
				0xA0, 0x06,
				0xA4, 0x00,
				0xA1, 0x00,
				0xA9, 0x00,
				// second object, missing 0xA1
				0xA0, 0x04,
				0xA4, 0x00,
				0xA9, 0x00,
				// third object, missing 0xA1 and 0xA9
				0xA0, 0x00,
		}));


		EvdTestTarget target = new EvdTestTarget();
		evDecoder.decode(target, decoder);

		// first object, OK
		// target: EvdTestTarget{a1=1, a4=1, a6=0, a9=1, c1=0, c2=0}
		System.out.println("target: " + target);
		Assert.assertTrue(target.getA1() == 1
				&& target.getA4() == 1
				&& target.getA6() == 0
				&& target.getA9() == 1
				&& target.getC1() == 0
				&& target.getC2() == 0
		);

		try {
			evDecoder.decode(new EvdTestTarget(), decoder);
		} catch (Throwable e) {
			System.out.println(e.getMessage() + "\n" + ExceptionUtil.getMessage(e));
			Assert.assertTrue("Error should be thrown out", true);
			Assert.assertTrue(e.getMessage().equals("TLV-TYPE 161 missing in A0"));
		}

		try {
			evDecoder.decode(new EvdTestTarget(), decoder);
		} catch (Throwable e) {
			System.out.println(e.getMessage() + "\n" + ExceptionUtil.getMessage(e));
			Assert.assertTrue("Error should be thrown out", true);
			Assert.assertTrue(e.getMessage().equals("TLV-TYPE 161,169 missing in A0"));
		}
	}

	@Test
	public void testAddDuplicateRule() {
		try {
			evDecoder.add(0xA1, (t, tlv) -> {
				System.out.println("target :" + t.getClass().getName());
			});
		} catch (Throwable e) {
			System.out.println(e.getMessage() + "\n" + ExceptionUtil.getMessage(e));
			Assert.assertTrue("Error should be thrown out", true);
			Assert.assertTrue(e.getMessage().equals("duplicate rule for same TLV-TYPE"));
		}
	}

	@Test
	public void testSetIsCritical() {
		EvDecoder<EvdTestTarget> evDecoder = new EvDecoder<EvdTestTarget>("A0", 0xA0)
				.add(0xA1, (t, tlv) -> {
					t.a1++;
				}).setIsCritical(new IsCritical() {
					@Override
					public boolean apply(Number tt) {
						return tt.intValue() == 0xA2;
					}
				});

		Decoder decoder = new Decoder<EvdTestTarget>(hexToByteArray(new int[]{
				// first object
				0xA0, 0x04,
				0xA1, 0x00, // recognized
				0xA3, 0x00, // non-critical in cb
				// second object
				0xA0, 0x04,
				0xA1, 0x00, // recognized
				0xA2, 0x00, // critical in cb
		}));

		EvdTestTarget target = evDecoder.decode(new EvdTestTarget(), decoder);

		// target: EvdTestTarget{a1=1, a4=0, a6=0, a9=0, c1=0, c2=0}
		System.out.println("target: " + target);
		Assert.assertTrue(target.getA1() == 1
				&& target.getA4() == 0
				&& target.getA6() == 0
				&& target.getA9() == 0
				&& target.getC1() == 0
				&& target.getC2() == 0);

		try {
			target = evDecoder.decode(new EvdTestTarget(), decoder);
		} catch (Throwable e) {
			System.out.println(e.getMessage() + "\n" + ExceptionUtil.getMessage(e));
			Assert.assertTrue("Error should be thrown out", true);
			Assert.assertTrue(e.getMessage().equals("TLV-TYPE 162 is unknown in A0"));
		}
	}

	@Test
	public void testSetUnknown() {
		EvDecoder<EvdTestTarget> evDecoder = new EvDecoder<EvdTestTarget>("0xAA", 0xAA)
				.add(0xA4, (t, tlv) -> {
					t.a4++;
				}, RuleOptions.of(7))
				.setUnknown((EvdTestTarget target, TLV tlv, Number order) -> {
					if (tlv.getType() == 0xA1) {
						target.a1++;
						return true;
					}
					return false;
				});

		Decoder decoder = new Decoder<EvdTestTarget>(hexToByteArray(new int[]{
				0xAA, 0x0A,
				0xA2, 0x00, // ignored - non critical  0xA2 % 2 == 0
				0xA1, 0x00, // handled by cb
				0xA4, 0x00, // handled by rule
				0xA1, 0x00, // handled by cb
				0xA6, 0x00, // ignored - non critical 0xA6 % 2 == 0
		}));

		EvdTestTarget target = evDecoder.decode(new EvdTestTarget(), decoder);

		// target: EvdTestTarget{a1=2, a4=1, a6=1, a9=0, c1=0, c2=0}
		System.out.println("target: " + target);
		log.info("target: " + target);
		Assert.assertTrue(target.getA1() == 2
				&& target.getA4() == 1
				&& target.getA6() == 0
				&& target.getA9() == 0
				&& target.getC1() == 0
				&& target.getC2() == 0);
	}

}

@AllArgsConstructor
@NoArgsConstructor
class A1 implements Decodable<A1> {
	private long type;
	private int length;
	private byte[] value;

	@Override
	public A1 decodeFrom(Decoder decoder) {
		TLV tlv = decoder.read();
		return new A1(tlv.getType(), tlv.getLength(), tlv.getValue());
	}
}

@Getter
@Setter
class EvdTestTarget {
	public long a1 = 0;
	public long a4 = 0;
	public long a6 = 0;
	public long a9 = 0;
	public long c1 = 0;
	public long c2 = 0;

	public Number sum() {
		return this.a1 * 1000 + this.a4 * 100 + this.a6 * 10 + this.a9;
	}

	public List<TlvObserver<EvdTestTarget>> observeBefore = Lists.newArrayList(new TlvObserver<EvdTestTarget>() {
		@Override
		public void apply(EvdTestTarget target, TLV topTlv) {
			System.out.println("observeBefore target = " + target + ", topTlv = " + topTlv);
		}
	});

	public List<TlvObserver<EvdTestTarget>> observeAfter = Lists.newArrayList(new TlvObserver<EvdTestTarget>() {
		@Override
		public void apply(EvdTestTarget target, TLV topTlv) {
			System.out.println("observeAfter1 target = " + target + ", topTlv = " + topTlv);
		}
	}, new TlvObserver<EvdTestTarget>() {
		@Override
		public void apply(EvdTestTarget target, TLV topTlv) {
			System.out.println("observeAfter2 target = " + target + ", topTlv = " + topTlv);
		}
	});

	public void setCallbacks(EvDecoder<EvdTestTarget> evd) {
		evd.beforeObservers.addAll(0, this.observeBefore);
		evd.afterObservers.addAll(0, this.observeAfter);
	}

	@Override
	public String toString() {
		return "EvdTestTarget{" +
				"a1=" + a1 +
				", a4=" + a4 +
				", a6=" + a6 +
				", a9=" + a9 +
				", c1=" + c1 +
				", c2=" + c2 +
				'}';
	}
}

