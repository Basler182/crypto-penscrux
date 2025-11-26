package com.basler182.cryptopenscrux.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HexUtilTest {

    @Test
    void givenBytes_whenBytesToHex_thenProducesUppercaseHex() {
        // given
        byte[] input = new byte[]{0x0F, (byte) 0xA0, (byte) 0xFF};

        // when
        String hex = HexUtil.bytesToHex(input);

        // then
        assertEquals("0FA0FF", hex, "bytesToHex should produce uppercase hex string");
    }

    @Test
    void givenHexUppercase_whenHexToBytes_thenMatchesBytes() {
        // given
        String hex = "0FA0FF";
        byte[] expected = new byte[]{0x0F, (byte) 0xA0, (byte) 0xFF};

        // when
        byte[] result = HexUtil.hexToBytes(hex);

        // then
        assertArrayEquals(expected, result, "hexToBytes should convert uppercase hex to original bytes");
    }

    @Test
    void givenHexLowercase_whenHexToBytes_thenMatchesBytes() {
        // given
        String hex = "0fa0ff";
        byte[] expected = new byte[]{0x0F, (byte) 0xA0, (byte) 0xFF};

        // when
        byte[] result = HexUtil.hexToBytes(hex);

        // then
        assertArrayEquals(expected, result, "hexToBytes should accept lowercase hex as well");
    }

    @Test
    void givenEmpty_whenConvert_thenEmptyResults() {
        // given
        byte[] emptyBytes = new byte[0];
        String emptyHex = "";

        // when
        String hexFromEmpty = HexUtil.bytesToHex(emptyBytes);
        byte[] bytesFromEmptyHex = HexUtil.hexToBytes(emptyHex);

        // then
        assertEquals("", hexFromEmpty, "bytesToHex of empty array should be empty string");
        assertArrayEquals(emptyBytes, bytesFromEmptyHex, "hexToBytes of empty string should be empty array");
    }

    @Test
    void givenOddLengthHex_whenHexToBytes_thenThrowsIllegalArgumentException() {
        // given
        String oddHex = "ABC"; // length 3

        // when / then
        assertThrows(IllegalArgumentException.class, () -> HexUtil.hexToBytes(oddHex),
                "hexToBytes should throw IllegalArgumentException for odd length strings");
    }
}