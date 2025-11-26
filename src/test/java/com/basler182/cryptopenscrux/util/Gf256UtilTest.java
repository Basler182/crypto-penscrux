package com.basler182.cryptopenscrux.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class Gf256UtilTest {

    @Test
    void givenTwoBytes_whenAddAndSub_thenResultIsXor() {
        // given
        byte a = (byte) 0xAB;
        byte b = (byte) 0x34;

        // when
        byte add = Gf256Util.add(a, b);
        byte sub = Gf256Util.sub(a, b);

        // then
        int expected = Byte.toUnsignedInt((byte) (a ^ b));
        assertEquals(expected, Byte.toUnsignedInt(add), "add should be XOR");
        assertEquals(expected, Byte.toUnsignedInt(sub), "sub should be identical to add (XOR)");
    }

    @Test
    void givenKnownVector_whenMul_thenMatchesExpected() {
        // AES example: 0x57 * 0x83 = 0xC1 in GF(256) with 0x11B polynomial
        // given
        byte a = (byte) 0x57;
        byte b = (byte) 0x83;

        // when
        byte result = Gf256Util.mul(a, b);

        // then
        assertEquals(0xC1, Byte.toUnsignedInt(result), "Multiplication should match known AES example");
    }

    @Test
    void givenByte_whenMulWithOneOrZero_thenIdentityOrZero() {
        // given
        byte a = (byte) 0x3A;
        byte one = (byte) 0x01;
        byte zero = (byte) 0x00;

        // when
        byte mulOne = Gf256Util.mul(a, one);
        byte mulZero = Gf256Util.mul(a, zero);

        // then
        assertEquals(Byte.toUnsignedInt(a), Byte.toUnsignedInt(mulOne), "a * 1 == a");
        assertEquals(0, Byte.toUnsignedInt(mulZero), "a * 0 == 0");
    }

    @Test
    void givenNonZeroByte_whenInv_thenMultiplyGivesOne() {
        // given
        byte a = (byte) 0x57; // non-zero

        // when
        byte inv = Gf256Util.inv(a);
        byte product = Gf256Util.mul(a, inv);

        // then
        assertEquals(1, Byte.toUnsignedInt(product), "a * inv(a) == 1");
    }

    @Test
    void givenValues_whenDiv_thenReversesMultiplication() {
        // given
        byte a = (byte) 0x7F;
        byte b = (byte) 0x13; // non-zero

        // when
        byte mul = Gf256Util.mul(a, b);
        byte div = Gf256Util.div(mul, b);

        // then
        assertEquals(Byte.toUnsignedInt(a), Byte.toUnsignedInt(div), "div(mul(a,b), b) == a");
    }

    @Test
    void givenZero_whenInv_thenThrows() {
        // given
        byte zero = (byte) 0x00;

        // when / then
        assertThrows(ArithmeticException.class, () -> Gf256Util.inv(zero), "Inverse of 0 should throw");
    }

    @Test
    void givenZeroDivisor_whenDiv_thenThrows() {
        // given
        byte a = (byte) 0x11;
        byte zero = (byte) 0x00;

        // when / then
        assertThrows(ArithmeticException.class, () -> Gf256Util.div(a, zero), "Division by 0 should throw");
    }
}