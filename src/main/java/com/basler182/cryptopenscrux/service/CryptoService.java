package com.basler182.cryptopenscrux.service;

import com.basler182.cryptopenscrux.util.HexUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;

import static com.basler182.cryptopenscrux.util.Gf256Util.*;

@Service
public class CryptoService {

    private static final Logger LOG = LoggerFactory.getLogger(CryptoService.class);

    private static final String WORDLIST_RESOURCE = "/bip-39/english.txt";

    private final SecureRandom random = new SecureRandom();

    private final List<String> WORD_LIST;

    public CryptoService() {
        List<String> tmp = new ArrayList<>();
        try (InputStream is = getClass().getResourceAsStream(WORDLIST_RESOURCE)) {
            if (is != null) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        line = line.trim();
                        if (!line.isEmpty()) tmp.add(line);
                    }
                }
            }
        } catch (Exception e) {
            LOG.warn("Error when loading word list: {}.", e.getMessage(), e);
            throw new RuntimeException(e);
        }
        WORD_LIST = Collections.unmodifiableList(tmp);
        LOG.debug("Wordlist initialized ({} words).", WORD_LIST.size());
    }

    public List<String> generateMnemonic(int count) {
        List<String> words = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            words.add(WORD_LIST.get(random.nextInt(WORD_LIST.size())));
        }
        return words;
    }

    public Map<Integer, String> splitSecret(String secret, int k, int n) {
        Objects.requireNonNull(secret, "secret must not be null");
        if (k < 2) throw new IllegalArgumentException("k must be >= 2");
        if (n < k) throw new IllegalArgumentException("n must be >= k");
        if (n > 255) throw new IllegalArgumentException("n must be <= 255 (index fits in one byte)");

        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length == 0) throw new IllegalArgumentException("secret must not be empty");

        int secretLen = secretBytes.length;
        byte[][] sharesData = new byte[n][secretLen];

        for (int i = 0; i < secretLen; i++) {
            byte[] coeffs = new byte[k];
            coeffs[0] = secretBytes[i];
            for (int j = 1; j < k; j++) {
                coeffs[j] = (byte) random.nextInt(256);
            }
            for (int x = 1; x <= n; x++) {
                sharesData[x - 1][i] = evaluatePolynomial(coeffs, (byte) x);
            }
        }

        Map<Integer, String> result = new LinkedHashMap<>();
        for (int x = 1; x <= n; x++) {
            byte[] combined = new byte[secretLen + 1];
            combined[0] = (byte) x;
            System.arraycopy(sharesData[x - 1], 0, combined, 1, secretLen);
            result.put(x, HexUtil.bytesToHex(combined));
        }
        return result;
    }

    public String combineShares(List<String> shareStrings) {
        Objects.requireNonNull(shareStrings, "shareStrings must not be null");
        if (shareStrings.isEmpty() || shareStrings.size() < 2) throw new IllegalArgumentException("No shares provided");

        List<byte[]> shares = shareStrings.stream()
                .map(HexUtil::hexToBytes)
                .toList();

        // Validierungen
        int shareLen = shares.getFirst().length;
        if (shareLen < 2) throw new IllegalArgumentException("Invalid share length");
        for (byte[] s : shares) {
            if (s.length != shareLen) throw new IllegalArgumentException("Inconsistent share lengths detected");
        }

        int k = shares.size();
        byte[] xCoords = new byte[k];
        Set<Integer> seenIndexes = new HashSet<>();
        for (int i = 0; i < k; i++) {
            int idx = Byte.toUnsignedInt(shares.get(i)[0]);
            if (idx == 0) throw new IllegalArgumentException("Share index cannot be zero");
            if (!seenIndexes.add(idx)) throw new IllegalArgumentException("Duplicate share index detected: " + idx);
            xCoords[i] = shares.get(i)[0];
        }

        int secretLen = shareLen - 1;
        byte[] secretBytes = new byte[secretLen];

        for (int i = 0; i < secretLen; i++) {
            byte[] yCoords = new byte[k];
            for (int j = 0; j < k; j++) {
                yCoords[j] = shares.get(j)[i + 1];
            }
            secretBytes[i] = interpolate(xCoords, yCoords);
        }

        return new String(secretBytes, StandardCharsets.UTF_8).trim();
    }

    private byte evaluatePolynomial(byte[] coeffs, byte x) {
        byte result = 0;
        for (int i = coeffs.length - 1; i >= 0; i--) {
            result = add(mul(result, x), coeffs[i]);
        }
        return result;
    }

    private byte interpolate(byte[] xCoords, byte[] yCoords) {
        byte result = 0;
        for (int i = 0; i < xCoords.length; i++) {
            byte term = yCoords[i];
            for (int j = 0; j < xCoords.length; j++) {
                if (i == j) continue;
                byte numerator = sub((byte) 0, xCoords[j]);
                byte denominator = sub(xCoords[i], xCoords[j]);
                term = mul(term, div(numerator, denominator));
            }
            result = add(result, term);
        }
        return result;
    }
}
