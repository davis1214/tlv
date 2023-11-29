package com.miao.tlv;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class EvDecoder<T> implements ElementDecoder {
    private static final Integer AUTO_ORDER_SKIP = 100;
    @Getter
    @Setter
    public final List<TlvObserver<T>> beforeObservers = new ArrayList<>();
    @Getter
    @Setter
    public final List<TlvObserver<T>> afterObservers = new ArrayList<>();
    private String typeName;
    private List<Integer> topTT;
    private Map<Integer, Rule<T>> rules = new HashMap<>();
    private Set<Integer> requiredTT = new HashSet<>();
    private int nextOrder = AUTO_ORDER_SKIP;
    private IsCritical isCritical = this::isCritical;
    private UnknownElementHandler<T> unknownHandler;

    public EvDecoder(String typeName) {
        this(typeName, new int[]{});
    }

    public EvDecoder(String typeName, int topTT) {
        this(typeName, new int[]{topTT});
    }

    public EvDecoder(String typeName, int[] topTT) {
        this.typeName = typeName;
        this.topTT = new ArrayList<>();
         for (int tt : topTT) {
            this.topTT.add(tt);
        }
    }

    private boolean isCritical(Number tt) {
        int t = tt.intValue();
        // 0x1F = 31 (31个)
        // TLV-TYPE 编号 0-31（闭区间）被认为是“祖父级”的类型，这些类型都被认为是“关键”的
        return t <= 0x1F || t % 2 == 1;
    }

    private ElementDecoder<T> nest(EvDecoder<T> evd) {
        return new ElementDecoder<T>() {
            @Override
            public void apply(T target, TLV tlv) {
                evd.decode(target, tlv.getDecoder());
            }
        };
    }

    public EvDecoder<T> setIsCritical(IsCritical cb) {
        this.isCritical = cb;
        return this;
    }

    public EvDecoder<T> setUnknown(UnknownElementHandler<T> cb) {
        this.unknownHandler = cb;
        return this;
    }

    public EvDecoder<T> add(int tt, ElementDecoder<T> cb) {
        return this.add(tt, cb, null);
    }

    public EvDecoder<T> add(int tt, ElementDecoder<T> cb, RuleOptions options) {
        assert !rules.containsKey(tt) : "duplicate rule for same TLV-TYPE";

        if (options == null) {
            options = new RuleOptions();
        }

        Rule<T> rule = new Rule<>();
        // if cb is typeOf EvDecoder would execute force-conversion
        rule.cb = cb instanceof EvDecoder ? nest((EvDecoder) cb) : cb;
        rule.order = options.order != null ? options.order : (nextOrder += AUTO_ORDER_SKIP);
        rule.required = options.required;
        rule.repeat = options.repeat;

        rules.put(tt, rule);
        if (rule.required) {
            requiredTT.add(tt);
        }
        return this;
    }

    @Override
    public void apply(Object target, TLV tlv) {
        // TODO 为了处理: rule.cb = cb instanceof EvDecoder ? nest(cb) : cb;
        log.info("target = " + target + ", tlv = " + tlv);
    }

    public <R extends T> R decode(R target, Decoder decoder) {
        TLV topTlv = decoder.read();
        if (topTlv == null) {
            return target;
        }

        int type = (int) topTlv.getType();
        if (topTT.size() > 0 && !topTT.contains(type)) {
            throw new Error(String.format("TLV-TYPE %s is not %s", printTT(type), typeName));
        }

        Decoder vd = topTlv.vd();
        return decodeV(target, vd, topTlv);
    }

    public <R extends T> R decodeValue(R target, Decoder vd) {
        return decodeV(target, vd, null);
    }

    private <R extends T> R decodeV(R target, Decoder vd, TLV topTlv) {
        for (TlvObserver<T> cb : beforeObservers) {
            cb.apply(target, topTlv);
        }

        int currentOrder = 0;
        Set<Integer> foundTT = new HashSet<>();
        Set<Integer> missingTT = new HashSet<>(requiredTT);

        while (!vd.eof()) {
            TLV tlv = vd.read();
            int tt = (int) tlv.getType();

            Rule<T> rule = rules.get(tt);
            if (rule == null) {
                // rule 不存在，判断是否关键属性，是关键属性也要抛出异常
                if (unknownHandler != null && unknownHandler.apply(target, tlv, currentOrder)) {
                    continue;
                }
                handleUnrecognized(tt, "unknown");
                continue;
            }

            // 如果顺序不一致，是关键的type，则抛出异常；如果不是关键的属性，忽略此次的处理
            if (currentOrder > rule.order.intValue()) {
                handleUnrecognized(tt, "out of order");
                continue;
            }

            currentOrder = rule.order.intValue();

            if (!rule.repeat && foundTT.contains(tt)) {
                throw new Error("TLV-TYPE " + printTT(tt) + " cannot repeat in " + typeName);
            }

            foundTT.add(tt);
            missingTT.remove(tt);
            rule.cb.apply(target, tlv);
        }

        // 个数：不能缺少，必须一致
        if (!missingTT.isEmpty()) {
            String missingTTString = missingTT.stream().map(this::printTT).collect(Collectors.joining(","));
            throw new Error("TLV-TYPE " + missingTTString + " missing in " + typeName);
        }

        for (TlvObserver<T> cb : afterObservers) {
            cb.apply(target, topTlv);
        }

        return target;
    }

    private void handleUnrecognized(int tt, String reason) {
        if (isCritical.apply(tt)) {
            throw new Error("TLV-TYPE " + printTT(tt) + " is " + reason + " in " + typeName);
        }
    }

    private String printTT(long tt) {
        return String.format("%s", tt);
    }

    private String printTT(int tt) {
        return String.format("%s", tt);
    }

}


interface ElementDecoder<T> {
    /**
     * Invoked when a matching TLV element is found
     */
    void apply(T target, TLV tlv);
}

interface UnknownElementHandler<T> {
    /**
     * Invoked when a TLV element does not match any rule.
     * 'order' denotes the order number of last recognized TLV element.
     * Return true if this TLV element is accepted, or false to follow evolvability guidelines.
     */
    boolean apply(T target, TLV tlv, Number order);
}

interface IsCritical<T> {
    /**
     * Function to determine whether a TLV-TYPE number is "critical".
     * Unrecognized or out-of-order TLV element with a critical TLV-TYPE number causes decoding error.
     */
    boolean apply(Number tt);
}

interface TlvObserver<T> {
    /**
     * Callback before or after decoding TLV-VALUE.
     *
     * @param target target object.
     * @param topTlv top-level TLV element, available in EVD.decode but unavailable in EVD.decodeValue.
     */
    void apply(T target, TLV topTlv);
}

class Rule<T> extends RuleOptions {
    ElementDecoder cb = null;
}

@NoArgsConstructor
@AllArgsConstructor
class RuleOptions {
    /**
     * Expected order of appearance.
     * When using this option, it should be specified for all rules in a EvDecoder.
     * Default to the order in which rules were added to EvDecoder.
     */
    Number order = null;

    /**
     * Whether TLV element may appear more than once.
     * Default is false.
     */
    Boolean repeat = false;

    /**
     * Whether TLV element must appear at least once.
     * Default is false.
     */
    Boolean required = false;

    static RuleOptions of(Number order) {
        return new RuleOptions(order, false, false);
    }

    static RuleOptions of(Boolean repeat) {
        return new RuleOptions(null, repeat, false);
    }

    static RuleOptions of(Number order, Boolean repeat) {
        return new RuleOptions(order, repeat, false);
    }

    static RuleOptions of(Number order, Boolean repeat, Boolean required) {
        return new RuleOptions(order, repeat, required);
    }

}



