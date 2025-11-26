package com.basler182.cryptopenscrux.util;

public final class Gf256Util {

    private Gf256Util() {  }

    public static byte add(byte a, byte b) {
        return (byte) (a ^ b);
    }

    public static byte sub(byte a, byte b) {
        return (byte) (a ^ b);
    }

    public static byte mul(byte a, byte b) {
        int p = 0;
        int aa = Byte.toUnsignedInt(a);
        int bb = Byte.toUnsignedInt(b);
        for (int i = 0; i < 8; i++) {
            if ((bb & 1) != 0) p ^= aa;
            boolean carry = (aa & 0x80) != 0;
            aa <<= 1;
            if (carry) aa ^= 0x11B;
            bb >>= 1;
        }
        return (byte) p;
    }

    public static byte div(byte a, byte b) {
        if (b == 0) throw new ArithmeticException("Division of 0 in GF(256)");
        return mul(a, inv(b));
    }

    public static byte inv(byte a) {
        if (a == 0) throw new ArithmeticException("Inverse of 0 not defined");
        byte result = 1;
        byte base = a;
        int exp = 254; // a^(2^8-2)
        while (exp > 0) {
            if ((exp & 1) != 0) result = mul(result, base);
            base = mul(base, base);
            exp >>= 1;
        }
        return result;
    }
}
