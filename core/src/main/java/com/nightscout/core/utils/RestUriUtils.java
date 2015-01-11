package com.nightscout.core.utils;

import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.hash.Hashing;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class RestUriUtils {
    public static boolean isV1Uri(URI uri) {
        return uri != null && (uri.getPath().endsWith("v1") || uri.getPath().endsWith("v1/"));
    }

    public static boolean hasToken(URI uri) {
        return !Strings.isNullOrEmpty(uri.getUserInfo());
    }

    /**
     * Removes the token from the uri.
     * @param uri Non-null uri to strip the token from.
     * @return uri without token.
     */
    public static URI removeToken(URI uri) {
        checkNotNull(uri);
        // This is gross, but I don't know a better way to do it.
        return URI.create(uri.toString().replaceFirst("//[^@]+@", "//"));
    }

    /**
     * Generates a secret from the given token.
     * @param secret Non-null, non-empty secret to generate the token from.
     * @return The generated token.
     */
    public static String generateSecret(String secret) {
        checkArgument(!Strings.isNullOrEmpty(secret));
        return Hashing.sha1().hashBytes(secret.getBytes(Charsets.UTF_8)).toString();
    }

    public static List<String> splitIntoMultipleUris(String combinedUris) {
        if (Strings.isNullOrEmpty(combinedUris)) {
            return new ArrayList<>();
        }
        return Splitter.onPattern("\\s+").splitToList(combinedUris.trim());
    }
}
