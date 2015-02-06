package com.nightscout.android.preferences;

import android.content.Context;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.mongodb.MongoClientURI;
import com.nightscout.android.R;
import com.nightscout.core.utils.RestUriUtils;

import java.net.URI;

public class PreferencesValidator {
    /**
     * Validate the syntax of the given mongo uri.
     * @param mongoUriString String to validated.
     * @return Optional localized validation error, if one occurs.
     */
    public static Optional<String> validateMongoUriSyntax(Context context, String mongoUriString) {
        if (Strings.isNullOrEmpty(mongoUriString)) {
            return Optional.of(context.getString(R.string.value_must_not_be_empty));
        }
        try {
            new MongoClientURI(mongoUriString);
        } catch (IllegalArgumentException e) {
            return Optional.of(context.getString(R.string.illegal_mongo_uri));
        } catch (Exception e) {
            // TODO(trhodeos): report these to acra, or something
            return Optional.of(context.getString(R.string.general_error_parsing));
        }
        return Optional.absent();
    }

    /**
     * Validate the syntax of a single rest api uri. Can be either legacy or v1 format.
     * @param restApiUri Uri to validate.
     * @return Localized validation error, if one occurs.
     */
    public static Optional<String> validateRestApiUriSyntax(Context context, String restApiUri) {
        if (Strings.isNullOrEmpty(restApiUri)) {
            return Optional.of(context.getString(R.string.invalid_rest_uri, restApiUri));
        }
        URI uri;
        try {
            uri = URI.create(restApiUri);
        } catch (NullPointerException e) {
            return Optional.of(context.getString(R.string.invalid_rest_uri, restApiUri));
        } catch (IllegalArgumentException e) {
            return Optional.of(context.getString(R.string.invalid_rest_uri, restApiUri));
        } catch (Exception e) {
            return Optional.of(context.getString(R.string.general_error_parsing));
        }
        if (RestUriUtils.isV1Uri(uri)) {
            if (!RestUriUtils.hasToken(uri)) {
                return Optional.of(context.getString(R.string.rest_uri_missing_token, restApiUri));
            }
        }
        return Optional.absent();
    }
}
