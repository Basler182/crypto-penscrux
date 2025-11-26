package com.basler182.cryptopenscrux.service;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CryptoServiceTest {

    private final CryptoService cryptoService = new CryptoService();

    @Test
    void givenWordCount_whenGenerateMnemonic_thenReturnsListWithExpectedSizeAndNonEmptyWords() {
        // given
        int count = 12;

        // when
        List<String> mnemonic = cryptoService.generateMnemonic(count);

        // then
        assertNotNull(mnemonic, "mnemonic must not be null");
        assertEquals(count, mnemonic.size(), "mnemonic must contain the requested number of words");
        assertTrue(mnemonic.stream().allMatch(w -> w != null && !w.trim().isEmpty()),
                "all mnemonic words must be non-empty");
        // optional: words should be alphabetic (bip-39 wordlist)
        assertTrue(mnemonic.stream().allMatch(w -> w.matches("^[a-z]+$")),
                "mnemonic words should be lowercase alphabetic");
    }

    @Test
    void givenSecret_whenSplitWithThreshold_thenProducesExpectedNumberOfSharesAndCombineRecovers() {
        // given
        List<String> mnemonic = cryptoService.generateMnemonic(12);
        String secret = String.join(" ", mnemonic);
        int k = 2;
        int n = 3;

        // when
        Map<Integer, String> shares = cryptoService.splitSecret(secret, k, n);

        // then
        assertNotNull(shares, "shares map must not be null");
        assertEquals(n, shares.size(), "should produce the requested number of shares");
        List<String> shareValues = new ArrayList<>(shares.values());
        assertTrue(shareValues.stream().allMatch(s -> s != null && !s.trim().isEmpty()),
                "each share must be non-empty");

        // when: combine any k shares
        List<String> subset = shareValues.subList(0, k);
        String recovered = cryptoService.combineShares(subset);

        // then
        assertEquals(secret, recovered, "recovered secret must match original secret");
    }

    @Test
    void givenInsufficientShares_whenCombine_thenThrowsException() {
        // given
        List<String> mnemonic = cryptoService.generateMnemonic(12);
        String secret = String.join(" ", mnemonic);
        Map<Integer, String> shares = cryptoService.splitSecret(secret, 2, 3);
        List<String> singleShare = Collections.singletonList(shares.values().iterator().next());

        // when / then
        assertThrows(Exception.class, () -> cryptoService.combineShares(singleShare),
                "combining fewer than threshold shares should fail");
    }

    @Test
    void givenInvalidParameters_whenSplit_thenThrowsIllegalArgumentException() {
        // given
        String sampleSecret = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about";

        // when / then: threshold greater than total shares
        assertThrows(IllegalArgumentException.class, () -> cryptoService.splitSecret(sampleSecret, 4, 3),
                "k > n should throw IllegalArgumentException");

        // when / then: negative values
        assertThrows(IllegalArgumentException.class, () -> cryptoService.splitSecret(sampleSecret, -1, 3),
                "negative threshold should throw IllegalArgumentException");
        assertThrows(IllegalArgumentException.class, () -> cryptoService.splitSecret(sampleSecret, 2, 0),
                "non-positive total shares should throw IllegalArgumentException");
    }
}