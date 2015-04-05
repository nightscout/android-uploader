package com.nightscout.android.preferences;

import com.nightscout.android.BuildConfig;
import com.nightscout.android.R;
import com.nightscout.android.test.RobolectricTestBase;

import org.junit.Test;
import org.robolectric.annotation.Config;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

@Config(constants = BuildConfig.class)
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
    public void testValidateMongoUriSyntax_EmptyPassword() {
        assertThat(PreferencesValidator.validateMongoUriSyntax(getContext(), "mongodb://a@b.com/db").get(),
                is(getContext().getString(R.string.illegal_mongo_uri, "mongodb://a@b.com/db")));
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

    @Test
    public void testValidateMqttEndpointSyntax_Valid() {
        assertThat(PreferencesValidator.validateMqttEndpointSyntax(getContext(),
                "mqtt://m10.cloudmqtt.com:23966").isPresent(), is(false));
    }

    @Test
    public void testValidateMqttUriSyntax_Invalid() {
        assertThat(PreferencesValidator.validateMqttEndpointSyntax(getContext(), "\\invalid").get(),
                is(getContext().getString(R.string.invalid_mqtt_endpoint, "\\invalid")));
    }

}
