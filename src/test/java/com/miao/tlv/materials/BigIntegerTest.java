package com.miao.tlv.materials;

import org.junit.Test;

import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import static com.miao.tlv.Commons._NUMBER_FORMAT;

/**
 * 1. BigInteger也是不可变的，在进行每一步运算时，都会产生一个新的对象。都会产生一个新的对象。发生异常算术条件时，会抛出ArithmeticException异常。例如，一个整数除以“0”，会抛出一个这个类的实例；
 * 2. 假设计算一个int数据平方与另一个大小的问题，很可能会内存溢出。除了使用二分法外，利用BigInteger的compareTo方法也是一个好选择，简单易懂，而且不需要算法支持；
 * 3. 本章作为笔记使用，内容比较全面，但常用的只有：构造函数，基本运算以及compareTo()，intValue()，setBit()，testBit()方法；
 * 4. setBit()和testBit()方法可用于菜单的权限控制，小编在开发中多次尝试，非常好用。很多微博有相关介绍，在这里我不做项目演示了。
 */
public class BigIntegerTest {
    private DecimalFormat decimalFormat = new DecimalFormat("#,##0.00");// 格式化设置

    static NumberFormat nf = NumberFormat.getInstance();

    static {
        // 是否以逗号隔开, 默认true以逗号隔开,如[123,456,789.128]
        nf.setGroupingUsed(false);
    }


    @Test
    public void testNumber() {
        // Integer max: 2147483647, min: -2147483648
        System.out.println("Integer max: " + Integer.MAX_VALUE + ", min: " + Integer.MIN_VALUE);
        // Long max: 9223372036854775807, min: -9223372036854775808
        System.out.println("Long max: " + Long.MAX_VALUE + ", min: " + Long.MIN_VALUE);
        // Double max: 1.7976931348623157E308, min: 4.9E-324
        System.out.println("Double max: " + nf.format(Double.MAX_VALUE).toString() + ", min: " + nf.format(Double.MIN_VALUE).toString());

        Double aa = (double) 18446744073709552000.5213413d;
        BigInteger bigInteger = new BigInteger(_NUMBER_FORMAT.format(aa));
        // aa: 1.8446744073709552E19, aa.intValue: 2147483647, aa.longValue: 9223372036854775807 , bigInteger: 18446744073709552000
        System.out.println("aa: " + aa + ", aa.intValue: " + aa.intValue() + ", aa.longValue: " + aa.longValue() + " , bigInteger: " + bigInteger.toString());

        // 0xFFFFFFFFFFFFFFFFL:-1
        System.out.println("0xFFFFFFFFFFFFFFFFL:" + 0xFFFFFFFFFFFFFFFFL);
        // 0xFFFFFFFFFFFFFFFFL & 0xFFFFFFFFFFFFFFFFL: -1
        System.out.println("0xFFFFFFFFFFFFFFFFL & 0xFFFFFFFFFFFFFFFFL: " + (0xFFFFFFFFFFFFFFFFL & 0xFFFFFFFFFFFFFFFFL));
    }

    @Test
    public void testInteger() {
        // Integer.MAX_VALUE = 2147483647
        System.out.println("Integer.MAX_VALUE: " + Integer.MAX_VALUE);
        // BigInteger.TEN.longValue(): 10
        System.out.println("BigInteger.TEN.longValue(): " + BigInteger.TEN.longValue());
        System.out.println("\n");

        // 10位
        BigInteger bigInteger = new BigInteger("4594967296");
        // 三进制：bigInteger toString: 102212020020112010221, 4594967296
        System.out.println("bigInteger toString: " + bigInteger.toString(3) + ", " + bigInteger.toString());
        // bigInteger intValue: 300000000
        System.out.println("bigInteger intValue: " + bigInteger.intValue());
        // bigInteger longValue: 4594967296
        System.out.println("bigInteger longValue: " + bigInteger.longValue());
        System.out.println("\n");

//        // 不支持十六进制的构造函数
//        bigInteger = new BigInteger("0x100000000", 16);
//        // 三进制：bigInteger toString: 102212020020112010221
//        System.out.println("bigInteger toString: " + bigInteger.toString());
//        // bigInteger intValue: 300000000
//        System.out.println("bigInteger intValue: " + bigInteger.intValue());
//        // bigInteger longValue: 4594967296
//        System.out.println("bigInteger longValue: " + bigInteger.longValue());
//        System.out.println("\n");

        //在构造将函数时，把radix进制的字符串转化为BigInteger
        String str = "1011100111";
        int radix = 2;
        BigInteger interNum1 = new BigInteger(str, radix); //743

        //我们通常不写，则是默认成10进制转换，如下：
        BigInteger interNum2 = new BigInteger(str); //1011100111
        // interNum1: 743, interNum2: 1011100111
        System.out.println("interNum1: " + interNum1 + ", interNum2: " + interNum2);

    }

    //基本运算:add(),subtract(),multiply(),divide(),mod(),remainder(),pow(),abs(),negate()
    @Test
    public void testBasic() {
        BigInteger a = new BigInteger("13");
        BigInteger b = new BigInteger("4");
        int n = 3;

        //1.加
        BigInteger bigNum1 = a.add(b);            //17
        //2.减
        BigInteger bigNum2 = a.subtract(b);        //9
        //3.乘
        BigInteger bigNum3 = a.multiply(b);        //52
        //4.除
        BigInteger bigNum4 = a.divide(b);        //3
        //5.取模(需 b > 0，否则出现异常：ArithmeticException("BigInteger: modulus not positive"))
        BigInteger bigNum5 = a.mod(b);            //1
        //6.求余
        BigInteger bigNum6 = a.remainder(b);    //1
        //7.平方(需 n >= 0，否则出现异常：ArithmeticException("Negative exponent"))
        BigInteger bigNum7 = a.pow(n);            //2197
        //8.取绝对值
        BigInteger bigNum8 = a.abs();            //13
        //9.取相反数
        BigInteger bigNum9 = a.negate();        //-13
    }

    //比较大小:compareTo(),max(),min()
    @Test
    public void testCompare() {
        BigInteger bigNum1 = new BigInteger("52");
        BigInteger bigNum2 = new BigInteger("27");

        //1.compareTo()：返回一个int型数据（1 大于； 0 等于； -1 小于）
        int num = bigNum1.compareTo(bigNum2);            //1

        //2.max()：直接返回大的那个数，类型为BigInteger
        //	原理：return (compareTo(val) > 0 ? this : val);
        BigInteger compareMax = bigNum1.max(bigNum2);    //52

        //3.min()：直接返回小的那个数，类型为BigInteger
        //	原理：return (compareTo(val) < 0 ? this : val);
        BigInteger compareMin = bigNum1.min(bigNum2);    //27
    }

    //类型转换(返回类型如下)
    @Test
    public void testToAnother() {
        BigInteger bigNum = new BigInteger("52");
        int radix = 2;

        //1.转换为bigNum的二进制补码形式 [52]
        byte[] num1 = bigNum.toByteArray();
        //2.转换为bigNum的十进制字符串形式 52
        String num2 = bigNum.toString();        //52
        //3.转换为bigNum的radix进制字符串形式  110100
        String num3 = bigNum.toString(radix);    //110100
        //4.将bigNum转换为int  52
        int num4 = bigNum.intValue();
        //5.将bigNum转换为long 52
        long num5 = bigNum.longValue();
        //6.将bigNum转换为float 52.0
        float num6 = bigNum.floatValue();
        //7.将bigNum转换为double  52.0
        double num7 = bigNum.doubleValue();

        System.out.println(num1);
    }

    //二进制运算(返回类型都为BigInteger，不常用，但有备无患)
    @Test
    public void testBinaryOperation() {
        BigInteger a = new BigInteger("13");
        BigInteger b = new BigInteger("2");
        int n = 1;

        //1.与：a&b
        BigInteger bigNum1 = a.and(b);            //0
        //2.或：a|b
        BigInteger bigNum2 = a.or(b);            //15
        //3.异或：a^b
        BigInteger bigNum3 = a.xor(b);            //15
        //4.取反：~a
        BigInteger bigNum4 = a.not();            //-14
        //5.左移n位： (a << n)
        BigInteger bigNum5 = a.shiftLeft(n);    //26
        //6.右移n位： (a >> n)
        BigInteger bigNum6 = a.shiftRight(n);    //6
    }

    //权限控制：setBit(),testBit()
    @Test
    public void testSetAndTest() {
        //1.封装数据(setBit的值需 >= 0，否则出现异常：ArithmeticException("Negative bit address"))
        BigInteger permission = new BigInteger("0");
        BigInteger numBig = permission.setBit(2);
        numBig = numBig.setBit(5);
        numBig = numBig.setBit(13);
        numBig = numBig.setBit(66);
        System.out.println("原理：" + numBig);
        // 原理：73786976294838214692 = 2^2+2^5+2^13+2^66 次方的和；
        // 看！！即使这么大的数也不会溢出，而int最大值只有2147483647；

        //2.取值验证（返回Boolean型）
        boolean flag1 = numBig.testBit(2);        //true
        boolean flag2 = numBig.testBit(5);        //true
        boolean flag3 = numBig.testBit(13);        //true
        boolean flag4 = numBig.testBit(66);        //true
        boolean flag5 = numBig.testBit(27);        //false
    }

    @Test
    public void testBigInteger() {
        BigInteger bigInteger = new BigInteger("9007199254740991");
        BigInteger dividedResult = bigInteger.divide(new BigInteger("4294967296"));
        System.out.println(dividedResult.toString());

        BigInteger bigInteger1 = BigInteger.valueOf(0x100000000L);
        log(bigInteger1);
    }

    private void log(Object o) {
        System.out.println("log: " + o);
    }

}
