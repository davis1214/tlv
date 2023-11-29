package com.miao.tlv;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;
import java.util.List;


/**
 * encoder 编码
 */
@Slf4j
@Getter
public class Encoder {

	private final static int _DEFAULT_BUFFER_SIZE = 1024;

	private int _init_buffer_size = 1024;
	private volatile ByteBuffer writeBuf;

	public Encoder() {
		initial();
	}

	public Encoder(int initSize) {
		this._init_buffer_size = initSize;
		initial(initSize);
	}

	private void initial() {
		_init_buffer_size = _DEFAULT_BUFFER_SIZE;
		writeBuf = ByteBuffer.allocate(_init_buffer_size);
		writeBuf.position(writeBuf.limit());
	}

	private void initial(int initSize) {
		writeBuf = ByteBuffer.allocate(initSize);
		writeBuf.position(writeBuf.limit());
	}

	public int getSize() {
		return writeBuf.remaining();
	}

	public byte[] getOutput() {
		return writeBuf.array();
	}

	public int getWritePosition(int sizeofObject) {
		grow(writeBuf.remaining() + sizeofObject);
		writeBuf.position(writeBuf.limit() - writeBuf.remaining() - sizeofObject);
		return writeBuf.position();
	}

	/**
	 * 添加数据（tlv-type、tlv-length）
	 *
	 * @param tlvType
	 * @param tlvLength
	 */
	private void prependTypeLength(long tlvType, long tlvLength) {
		// length
		int sizeofL = Strings.sizeofVarNum(tlvLength);
		int tlvLengthPosition = getWritePosition(sizeofL);
		writeVarNum(tlvLength, tlvLengthPosition);

		// type
		int sizeofT = Strings.sizeofVarNum(tlvType);
		int tlvTypePosition = getWritePosition(sizeofT);
		writeVarNum(tlvType, tlvTypePosition);
	}

	private void prependValue(byte[] bytes) {
		if (bytes == null || bytes.length == 0) {
			return;
		}
		prependValue(ByteBuffer.wrap(bytes));
	}

	private void prependValue(ByteBuffer buffer) {
		if (buffer == null || buffer.capacity() == 0) {
			return;
		}

		int writePosition = getWritePosition(buffer.remaining());
		int bufferPosition = buffer.position();
		this.writeBuf.put(buffer);
		this.writeBuf.position(writePosition);
		buffer.position(bufferPosition);
	}


	public void prependTlv(long type, Encodable... encodables) {
		if (type <= 0) {
			return;
		}

		if (encodables == null || encodables.length == 0) {
			prependTypeLength(type, 0);
			return;
		}

		int position = this.writeBuf.position();
		for (int i = encodables.length - 1; i >= 0; i--) {
			Encodable encodable = encodables[i];
			if (encodable == null) {
				continue;
			}
			encodables[i].encodeTo(this);
		}

		long length = this.writeBuf.position() == position ? 0 : position - this.writeBuf.position();
		prependTypeLength(type, length);
	}

	public void prependTlv(TLV tlv) {
		if (tlv == null) {
			return;
		}

		// T1-L1-[T2-L2-V2, T3-L3-[TL4-L4-V4]]
		// [T2-L2-V2, TL4-L4-V5]

		if (tlv.getTlvList() == null || tlv.getTlvList().size() == 0) {
			prependValue(tlv.getValueBuf());
		} else {
			for (int index = tlv.getTlvList().size() - 1; index >= 0; index--) {
				prependTlv(tlv.getTlvList().get(index));
			}
		}
		prependTypeLength(tlv.getType(), tlv.getLength());
	}

	/**
	 * 对象编码，对外暴漏的接口
	 *
	 * @param obj
	 * @return
	 */
	public static byte[] encode(Object obj) {
		return encode(obj, _DEFAULT_BUFFER_SIZE);
	}


	/**
	 * 对象编码，对外暴漏的接口
	 *
	 * @param obj         待编码对象
	 * @param initBufSize ByteBuffer 初始化大小
	 * @return
	 */
	public static byte[] encode(Object obj, int initBufSize) {
		Encoder encoder = new Encoder(initBufSize);
		encoder.encoding(obj);
		return Strings.getRemainBytes(encoder.getWriteBuf());
	}

	/**
	 * @param obj
	 */
	private void encoding(Object obj) {
		if (obj == null) {
			throw new IllegalArgumentException("obj should not be null");
		}

		if (obj instanceof ByteBuffer) {
			ByteBuffer buf = (ByteBuffer) obj;
			prependValue(buf);
		} else if (obj instanceof TLV) {
			TLV tlv = (TLV) obj;
			prependTlv(tlv);
		} else if (obj instanceof List) {
			List objList = (List) obj;
			for (int i = objList.size() - 1; i >= 0; i--) {
				Object o = objList.get(i);
				if (o instanceof TLV) {
					TLV tlv = (TLV) o;
					prependTlv(tlv);
				} else {
					log.error("element in obj(list) should be instance of TLV, but got ", o.getClass().getName());
				}
			}
		} else if (obj instanceof Encodable) {
			((Encodable) obj).encodeTo(this);
		} else {
			throw new IllegalArgumentException(String.format("obj is not Encodable - %s", obj.getClass().getName()));
		}
	}

	/**
	 * Invariants: mark <= position <= limit <= capacity
	 * int mark = -1;     // 标记当前位置，可以使用 reset 方法重新回到 mark 标记的位置
	 * int limit;         // 逻辑大小，描述这个缓冲区目前有多大
	 * int capacity;      // 容量，初始化大小，缓冲区 byte 数组大小
	 * int remaining      // 数组中元素个数（limit - position）
	 * int position = 0;  // 下一个字节写到byte数组的哪一个下标，从左往右开始
	 */
	private void grow(int expandCapacity) {
		if (writeBuf.capacity() >= expandCapacity) {
			return;
		}

		int newCapacity = expandCapacity + _init_buffer_size;
		ByteBuffer expandBuffer = ByteBuffer.allocate(newCapacity);
		int position = expandBuffer.limit() - writeBuf.limit();
		// 从 position 开始读
		writeBuf.flip();
		// expandBuffer.mark();
		expandBuffer.position(position);
		expandBuffer.put(writeBuf.array());
		expandBuffer.limit(expandBuffer.capacity());
		expandBuffer.position(position);
		// expandBuffer.reset();
		writeBuf = expandBuffer;
	}


	private final void writeVarNum(long varNumber, int position) {
		if (varNumber < 253) {
			// todo buffer中已有元素个数和length，再与实际大小做对比
			// 位与计算
			writeBuf.put(position, (byte) (varNumber & 0xff));
		} else if (varNumber <= 0xffff) {
			writeBuf.put(position, (byte) 253);
			writeBuf.put(position + 1, (byte) ((varNumber >> 8) & 0xff));
			writeBuf.put(position + 2, (byte) (varNumber & 0xff));
		} else {
			writeBuf.put(position, (byte) 254);
			writeBuf.put(position + 1, (byte) ((varNumber >> 24) & 0xff));
			writeBuf.put(position + 2, (byte) ((varNumber >> 16) & 0xff));
			writeBuf.put(position + 3, (byte) ((varNumber >> 8) & 0xff));
			writeBuf.put(position + 4, (byte) (varNumber & 0xff));
		}
	}
}
