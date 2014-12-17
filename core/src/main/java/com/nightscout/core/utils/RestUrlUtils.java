package com.nightscout.core.utils;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.nightscout.core.dexcom.Utils;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class RestUrlUtils {
    public static boolean isV1Url(URL url) {
        return url != null && (url.getPath().endsWith("v1") || url.getPath().endsWith("v1/"));
    }

    public static boolean hasToken(URL url) {
        return !Strings.isNullOrEmpty(url.getUserInfo());
    }

    /**
     * Removes the token from the url.
     * @param url Non-null url to strip the token from.
     * @return url without token.
     */
    public static URL removeToken(URL url) {
        checkNotNull(url);
        // This is gross, but I don't know a better way to do it.
        try {
            return new URL(url.toString().replaceFirst("//[^@]+@", "//"));
        } catch(MalformedURLException e) {
            return url;
        }
    }

    /**
     * Generates a secret from the given token.
     * @param secret Non-null, non-empty secret to generate the token from.
     * @return The generated token.
     */
    public static String generateToken(String secret) {
        checkArgument(!Strings.isNullOrEmpty(secret));
        String sha1 = null;
        try {
            MessageDigest crypt = MessageDigest.getInstance("SHA-1");
            crypt.reset();
            crypt.update(secret.getBytes("UTF-8"));
            sha1 = Utils.bytesToHex(crypt.digest()).toLowerCase();
        } catch(NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch(UnsupportedEncodingException e) {
            return null;
        }
        return sha1;
    }

    public static List<String> splitIntoMultipleUris(String combinedUris) {
        if (Strings.isNullOrEmpty(combinedUris)) {
            return Lists.newArrayList();
        }
        return Splitter.onPattern("\\s+").splitToList(combinedUris.trim());
    }

    public static String getSecret(URL baseUrl) {
        String protocolLessUrl = baseUrl.toString().replace("http[s]?://", "");
        int index = protocolLessUrl.indexOf('@');
        if (index < 0) {
            return null;
        } else {
            return protocolLessUrl.substring(0, index);
        }
    }
}
