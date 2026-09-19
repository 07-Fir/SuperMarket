package all_class;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** 密码单向哈希：算法、迭代次数、随机盐和派生值一起保存，不需要解密密钥。 */
final class PasswordHash {
    private static final String PREFIX = "$pbkdf2-sha256$";
    private static final int ITERATIONS = 600_000;
    private static final int SALT_BYTES = 16;
    private static final int HASH_BYTES = 32;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordHash() {}

    static String hash(String plaintext) {
        Objects.requireNonNull(plaintext, "密码不能为空");
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        byte[] derived = derive(plaintext, salt, ITERATIONS);
        try {
            return PREFIX + ITERATIONS + "$" + Base64.getEncoder().encodeToString(salt)
                    + "$" + Base64.getEncoder().encodeToString(derived);
        } finally { Arrays.fill(derived, (byte) 0); }
    }

    static String requireEncoded(String stored) {
        parse(stored);
        return stored;
    }

    static boolean verify(String plaintext, String stored) {
        if (plaintext == null) return false;
        try {
            String[] parts = parse(stored);
            byte[] salt = Base64.getDecoder().decode(parts[3]);
            byte[] expected = Base64.getDecoder().decode(parts[4]);
            byte[] actual = derive(plaintext, salt, Integer.parseInt(parts[2]));
            try { return MessageDigest.isEqual(expected, actual); }
            finally { Arrays.fill(actual, (byte) 0); Arrays.fill(expected, (byte) 0); }
        } catch (IllegalArgumentException e) {
            return false; // 损坏的哈希不能被当作明文密码登录。
        }
    }

    // 仅用于旧 TXT 导入；新密码输入始终调用 hash，即使输入看起来像哈希。
    static String importText(String stored) {
        Objects.requireNonNull(stored, "密码缺失");
        return stored.startsWith(PREFIX) ? requireEncoded(stored) : hash(stored);
    }

    private static String[] parse(String stored) {
        if (stored == null || stored.length() > 256) throw new IllegalArgumentException("密码哈希格式错误");
        String[] parts = stored.split("\\$", -1);
        if (parts.length != 5 || !parts[0].isEmpty() || !parts[1].equals("pbkdf2-sha256"))
            throw new IllegalArgumentException("密码哈希格式错误");
        int iterations = Integer.parseInt(parts[2]);
        // 限制读取参数，避免损坏的文件引起无上限的计算。
        if (iterations < ITERATIONS || iterations > 2_000_000
                || Base64.getDecoder().decode(parts[3]).length != SALT_BYTES
                || Base64.getDecoder().decode(parts[4]).length != HASH_BYTES)
            throw new IllegalArgumentException("密码哈希参数错误");
        return parts;
    }

    private static byte[] derive(String plaintext, byte[] salt, int iterations) {
        char[] chars = plaintext.toCharArray();
        PBEKeySpec spec = new PBEKeySpec(chars, salt, iterations, HASH_BYTES * 8);
        Arrays.fill(chars, '\0');
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("当前 Java 环境不支持密码哈希算法", e);
        } finally { spec.clearPassword(); }
    }
}
