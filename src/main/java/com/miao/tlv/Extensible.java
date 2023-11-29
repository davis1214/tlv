package com.miao.tlv;

import com.google.common.collect.Maps;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.*;

public abstract class Extensible {

	ExtensionRegistry extensibleRegistry;

	/**
	 * es6 中可以直接把变量或者表达式作为属性名
	 * <p>
	 * let objAddname = 'name'
	 * let firstName = 'name'
	 * const objAdd = {
	 * name: 'lisi',
	 * [objAddname]: 'xoapming',
	 * [`my${firstName}is`]: 'zhangsan'
	 * }
	 * <p>
	 * // { name: 'xoapming', myfirstNameis: 'zhangsan' }
	 * console.log(objAdd)
	 */
}

@Data
abstract class Extension<T, R> {

	/**
	 * TLV-TYPE.
	 */
	Number tt;

	/**
	 * Order relative to other extensions, used on encoding only
	 */
	Number order;

	/**
	 * Decode extension element.
	 *
	 * @param obj         parent object.
	 * @param tlv         TLV of sub element; its TLV-TYPE would be this.tt .
	 * @param accumulator previous decoded value, if extension element appears more than once.
	 */
	abstract R decode(T obj, TLV tlv, R accumulator);

	/**
	 * Encode extension element.
	 *
	 * @param obj   parent object.
	 * @param value decoded value.
	 * @returns encoding of sub element; its TLV-TYPE should be this.tt .
	 */
	abstract Encodable encode(T obj, R value);

}

class ExtensionRegistry<T extends Extensible> {
	/**
	 * 可扩展的 TLV 编解码
	 */
	private static final Map<Extensible, Map<Number, Object>> RECORDS = Maps.newHashMap();

	private final Map<Number, Extension<T, Object>> table = new HashMap<>();

	public void registerExtension(Extension<T, Object> ext) {
		table.put(ext.getTt(), ext);
	}

	public void unregisterExtension(Number tt) {
		table.remove(tt);
	}

	public boolean decodeUnknown(T target, TLV tlv, Number order) {
		int tt = (int) tlv.getType();
		Extension<T, Object> ext = table.get(tt);
		if (ext == null) {
			return false;
		}

		Map<Number, Object> record = RECORDS.computeIfAbsent(target, key -> Maps.newHashMap());
		record.put(tt, ext.decode(target, tlv, record.get(tt)));
		return true;
	}

	public Encodable[] encode(T source) {
		Map<Number, Object> record = RECORDS.get(source);
		if (record == null || record.size() == 0) {
			return new Encodable[0];
		}

		List<RegistryField> fields = new ArrayList<>();
		for (Map.Entry<Number, Object> entry : record.entrySet()) {
			Extension<T, Object> ext = table.get(entry.getKey());
			if (ext == null) {
				throw new Error("unknown extension type " + entry.getKey());
			}
			fields.add(new RegistryField(entry.getKey(), entry.getValue(), ext));
		}

		// 编码顺序：order 从小到大，如果order为空按 type 从小到大编码
		fields.sort(Comparator.comparingInt(f -> f.ext.getOrder() != null ? f.ext.getOrder().intValue() : f.tt.intValue()));
		return fields.stream().map(f -> f.ext.encode(source, f.value)).toArray(Encodable[]::new);
	}


	//    // ts 中的 Symbol 一种基础类型。ts 的属性名称可以是变量名或者表达式
	//    private final static String TAG = new String("Extensible");
	public static void cloneRecord(Extensible dst, Extensible src) {
		RECORDS.put(dst, new HashMap<>(RECORDS.get(src)));
	}

	public static Object get(Extensible obj, int tt) {
		return RECORDS.computeIfAbsent(obj, key -> Maps.newHashMap()).get(tt);
	}

	public static void set(Extensible obj, int tt, Object value) {
		RECORDS.computeIfAbsent(obj, key -> Maps.newHashMap()).put(tt, value);
	}

	public static void clear(Extensible obj, int tt) {
		RECORDS.computeIfAbsent(obj, key -> Maps.newHashMap()).remove(tt);
	}

	public static void clear(Extensible obj) {
		RECORDS.remove(obj);
	}

	@Data
	@AllArgsConstructor
	class RegistryField {
		private final Number tt;
		private final Object value;
		private final Extension<T, Object> ext;
	}
}

