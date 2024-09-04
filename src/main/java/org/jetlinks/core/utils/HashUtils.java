package org.jetlinks.core.utils;

import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;

@SuppressWarnings("all")
public class HashUtils {

    public static long murmur3_128(Object first, Object... another) {
        return HashUtils
            .putHash(Hashing.murmur3_128().newHasher(), first, another)
            .hash()
            .asLong();
    }

    public static Hasher putHash(Hasher hasher, Object key) {
        if (key instanceof String) {
            return hasher.putUnencodedChars(((String) key));
        } else if (key instanceof BigDecimal) {
            return hasher.putBytes(((BigDecimal) key).toBigInteger().toByteArray());
        } else if (key instanceof BigInteger) {
            return hasher.putBytes(((BigInteger) key).toByteArray());
        } else if (key instanceof Number) {
            return hasher.putDouble(((Number) key).doubleValue());
        } else if (key.getClass().isArray()) {
            if (key instanceof byte[]) {
                return hasher.putBytes(((byte[]) key));
            }
            int len = Array.getLength(key);
            for (int i = 0; i < len; i++) {
                putHash(hasher, Array.get(key, i));
            }
            return hasher;
        } else if (key instanceof Iterable) {
            for (Object ele : (Iterable) key) {
                putHash(hasher, ele);
            }
            return hasher;
        } else {
            return hasher.putInt(key == null ? 0 : key.hashCode());
        }
    }

    public static Hasher putHash(Hasher hasher, Object first, Object... objects) {
        putHash(hasher, first);
        for (Object object : objects) {
            putHash(hasher, object);
        }
        return hasher;
    }
}
