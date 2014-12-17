package com.nightscout.core.utils;

import org.junit.Test;

import java.net.URL;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class RestUrlUtilsTest {
    @Test
    public void testIsV1Url_withV1Url() throws Exception {
        assertThat(RestUrlUtils.isV1Url(new URL("http://example.com/v1")), is(true));
    }

    @Test
    public void testIsV1Url_withV1UriTrailingSlash() throws Exception {
        assertThat(RestUrlUtils.isV1Url(new URL("http://example.com/v1/")), is(true));
    }

    @Test
    public void testIsV1Url_withV1InPathButNotEnding() throws Exception {
        assertThat(RestUrlUtils.isV1Url(new URL("http://example.com/v1/test")), is(false));
    }

    @Test
    public void testIsV1Url_withV1NotInPath() throws Exception {
        assertThat(RestUrlUtils.isV1Url(new URL("http://example.v1/")), is(false));
    }

    @Test
    public void testIsV1Url_withLegacyUrl() throws Exception {
        assertThat(RestUrlUtils.isV1Url(new URL("http://example.com/foo")), is(false));
    }

    @Test
    public void testIsV1Url_withNull() {
        assertThat(RestUrlUtils.isV1Url(null), is(false));
    }

    @Test
    public void testHasToken_withNone() throws Exception {
        assertThat(RestUrlUtils.hasToken(new URL("http://example.com")), is(false));
    }

    @Test
    public void testHasToken_withOne() throws Exception {
        assertThat(RestUrlUtils.hasToken(new URL("http://token@example.com")), is(true));
    }

    @Test
    public void testRemoveToken_withToken() throws Exception {
        assertThat(RestUrlUtils.removeToken(new URL("http://token@example.com")),
                is(new URL("http://example.com")));
    }

    @Test
    public void testRemoveToken_withoutToken() throws Exception {
        assertThat(RestUrlUtils.removeToken(new URL("http://example.com")),
                is(new URL("http://example.com")));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGenerateSecret_withNull() {
        RestUrlUtils.generateToken(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGenerateSecret_withEmpty() {
        RestUrlUtils.generateToken("");
    }

    @Test
    public void testGenerateSecret_withString() {
        assertThat(RestUrlUtils.generateToken("thisismyapisecret"),
                is("1674bb2ed584ce8a73f7d938922beb4f6198b1e0"));
    }

    @Test
    public void testSplitIntoMultipleUris_Empty() {
        assertThat(RestUrlUtils.splitIntoMultipleUris("").size(), is(0));
    }

    @Test
    public void testSplitIntoMultipleUris_One() {
        List<String> urls = RestUrlUtils.splitIntoMultipleUris("one");
        assertThat(urls.size(), is(1));
        assertThat(urls.get(0), is("one"));
    }

    @Test
    public void testSplitIntoMultipleUris_ExtraWhitespace() {
        List<String> urls = RestUrlUtils.splitIntoMultipleUris("one \t\n");
        assertThat(urls.size(), is(1));
        assertThat(urls.get(0), is("one"));
    }

    @Test
    public void testSplitIntoMultipleUris_Multiple() {
        List<String> urls = RestUrlUtils.splitIntoMultipleUris("one two");
        assertThat(urls.size(), is(2));
        assertThat(urls.get(0), is("one"));
        assertThat(urls.get(1), is("two"));
    }

    @Test
    public void testSplitIntoMultipleUris_Whitespace() {
        List<String> urls = RestUrlUtils.splitIntoMultipleUris("one \t\ntwo");
        assertThat(urls.size(), is(2));
        assertThat(urls.get(0), is("one"));
        assertThat(urls.get(1), is("two"));
    }
}
