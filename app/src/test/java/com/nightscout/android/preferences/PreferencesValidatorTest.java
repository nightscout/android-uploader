package com.nightscout.android.preferences;

import com.nightscout.android.R;
import com.nightscout.android.test.RobolectricTestBase;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class PreferencesValidatorTest extends RobolectricTestBase {
    @Test
    public void testValidateMongoUriSyntax_Empty() {
        assertThat(PreferencesValidator.validateMongoUriSyntax(getContext(), "").get(),
                is(getContext().getString(R.string.illegal_mongo_uri, "")));
    }

    @Test
    public void testValidateMongoUriSyntax_Invalid() {
        assertThat(PreferencesValidator.validateMongoUriSyntax(getContext(), "test/db").get(),
                is(getContext().getString(R.string.illegal_mongo_uri, "test/db")));
    }

    @Test
    public void testValidateMongoUriSyntax_Valid() {
        assertThat(PreferencesValidator.validateMongoUriSyntax(getContext(), "mongodb://test/db")
                        .isPresent(),
                is(false));
    }

    @Test
    public void testValidateRestApiUriSyntax_Empty() {
        assertThat(PreferencesValidator.validateRestApiUriSyntax(getContext(), "").get(),
                is(getContext().getString(R.string.invalid_rest_uri, "")));
    }

    @Test
    public void testValidateRestApiUriSyntax_Invalid() {
        assertThat(PreferencesValidator.validateRestApiUriSyntax(getContext(), "\\invalid").get(),
                is(getContext().getString(R.string.invalid_rest_uri, "\\invalid")));
    }

    @Test
    public void testValidateRestApiUriSyntax_Valid() {
        assertThat(PreferencesValidator.validateRestApiUriSyntax(getContext(), "http://test.com")
                        .isPresent(),
                is(false));
    }

    @Test
    public void testValidateRestApiUriSyntax_V1NoToken() {
        assertThat(PreferencesValidator.validateRestApiUriSyntax(getContext(), "http://test.com/v1")
                        .get(),
                is(getContext().getString(R.string.rest_uri_missing_token, "http://test.com/v1")));
    }
}
