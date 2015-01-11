package com.nightscout.core.utils;

import org.junit.Test;

import java.net.URI;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class RestUriUtilsTest {

    @Test
    public void testIsV1Uri_withV1Uri() {
        assertThat(RestUriUtils.isV1Uri(URI.create("http://example.com/v1")), is(true));
    }

    @Test
    public void testIsV1Uri_withV1UriTrailingSlash() {
        assertThat(RestUriUtils.isV1Uri(URI.create("http://example.com/v1/")), is(true));
    }

    @Test
    public void testIsV1Uri_withV1InPathButNotEnding() {
        assertThat(RestUriUtils.isV1Uri(URI.create("http://example.com/v1/test")), is(false));
    }

    @Test
    public void testIsV1Uri_withV1NotInPath() {
        assertThat(RestUriUtils.isV1Uri(URI.create("http://example.v1/")), is(false));
    }

    @Test
    public void testIsV1Uri_withLegacyUri() {
        assertThat(RestUriUtils.isV1Uri(URI.create("http://example.com/foo")), is(false));
    }

    @Test
    public void testIsV1Uri_withNull() {
        assertThat(RestUriUtils.isV1Uri(null), is(false));
    }

    @Test
    public void testHasToken_withNone() {
        assertThat(RestUriUtils.hasToken(URI.create("http://example.com")), is(false));
    }

    @Test
    public void testHasToken_withOne() {
        assertThat(RestUriUtils.hasToken(URI.create("http://token@example.com")), is(true));
    }

    @Test
    public void testRemoveToken_withToken() {
        assertThat(RestUriUtils.removeToken(URI.create("http://token@example.com")),
                is(URI.create("http://example.com")));
    }

    @Test
    public void testRemoveToken_withoutToken() {
        assertThat(RestUriUtils.removeToken(URI.create("http://example.com")),
                is(URI.create("http://example.com")));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGenerateSecret_withNull() {
        RestUriUtils.generateSecret(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGenerateSecret_withEmpty() {
        RestUriUtils.generateSecret("");
    }

    @Test
    public void testGenerateSecret_withString() {
        assertThat(RestUriUtils.generateSecret("testingtesting"), is("b0212be2cc6081fba3e0b6f3dc6e0109d6f7b4cb"));
    }

    @Test
    public void testSplitIntoMultipleUris_Empty() {
        assertThat(RestUriUtils.splitIntoMultipleUris("").size(), is(0));
    }

    @Test
    public void testSplitIntoMultipleUris_One() {
        List<String> urls = RestUriUtils.splitIntoMultipleUris("one");
        assertThat(urls.size(), is(1));
        assertThat(urls.get(0), is("one"));
    }

    @Test
    public void testSplitIntoMultipleUris_ExtraWhitespace() {
        List<String> urls = RestUriUtils.splitIntoMultipleUris("one \t\n");
        assertThat(urls.size(), is(1));
        assertThat(urls.get(0), is("one"));
    }

    @Test
    public void testSplitIntoMultipleUris_Multiple() {
        List<String> urls = RestUriUtils.splitIntoMultipleUris("one two");
        assertThat(urls.size(), is(2));
        assertThat(urls.get(0), is("one"));
        assertThat(urls.get(1), is("two"));
    }

    @Test
    public void testSplitIntoMultipleUris_Whitespace() {
        List<String> urls = RestUriUtils.splitIntoMultipleUris("one \t\ntwo");
        assertThat(urls.size(), is(2));
        assertThat(urls.get(0), is("one"));
        assertThat(urls.get(1), is("two"));
    }
}
