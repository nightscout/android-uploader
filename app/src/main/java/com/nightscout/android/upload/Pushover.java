package com.nightscout.android.upload;

import org.json.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;

import javax.net.ssl.HttpsURLConnection;

public class Pushover {

    /**
     * The application token from Pushover.net's application section.
     */
    private String appToken;

    /**
     * The user token, from a user's user key page.
     */
    private String userToken;

    /**
     * Initializes a Pushover object for talking to Pushover.
     * @param appToken Your application token, generated from Pushover.net
     * @param userToken A user's usertoken, found on the user page from Pushover.net
     */
    public Pushover(String appToken, String userToken) {
        this.appToken = appToken;
        this.userToken = userToken;
    }

    /**
     * Gets the application token associated with this object
     * @return appToken The application token
     */
    public String getAppToken() {
        return appToken;
    }

    /**
     * Gets the user token associated with this object
     * @return userToken
     */
    public String getUserToken() {
        return userToken;
    }

    /**
     * Sends a Pushover message.
     * @param message Message to send.
     * @param title Title of the message.
     * @param priority Priority of the message.
     * @return Result containing the status and any errors that occurred.
     * @throws JSONException
     * @throws UnsupportedEncodingException
     * @throws IOException
     */
    public PushoverResponse sendMessage(String message, String title, int priority) throws JSONException, IOException {
        String result = sendToPushoverRaw(getAuthenticationTokens() +
                "&message=" + URLEncoder.encode(message, "UTF-8") +
                "&title=" + URLEncoder.encode(title, "UTF-8") +
                "&priority=" + String.valueOf(priority) +
                "&retry=30" + // 24 hours
                "&expire=14400" + // 4 hours
                "&sound=persistent"
        );

        JSONObject jsonResult = new JSONObject(result);
        int status = jsonResult.optInt("status");
        ArrayList<String> errorStrings = new ArrayList<String>();

        if (status != 1) {
            JSONArray pushErrors = jsonResult.getJSONArray("errors");

            for (int i = 0, n = pushErrors.length(); i < n; i++) {
                errorStrings.add(pushErrors.optString(i));
            }
        }

        return new PushoverResponse(status, errorStrings.toArray(new String[errorStrings.size()]));
    }

    /**
     * Gets a string with the auth tokens already made.
     * @return String of auth tokens
     * @throws UnsupportedEncodingException
     */
    private String getAuthenticationTokens() throws UnsupportedEncodingException{
        return "token=" + getAppToken() + "&user=" + getUserToken();
    }

    /**
     * Sends a raw bit of text via POST to Pushover.
     * @param rawMessage The URL-encoded message to send.
     * @return JSON reply from Pushover.
     * @throws IOException
     */
    private String sendToPushoverRaw(String rawMessage) throws IOException {
        URL pushoverUrl = new URL("https://api.pushover.net/1/messages.json");

        HttpsURLConnection connection = (HttpsURLConnection) pushoverUrl.openConnection();
        connection.setDoOutput(true);
        connection.setDoInput(true);

        OutputStream outputStream = connection.getOutputStream();
        outputStream.write(rawMessage.getBytes(Charset.forName("UTF-8")));
        outputStream.close();

        BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));

        String output = "";
        String outputCache;
        while ((outputCache = br.readLine()) != null) {
            output += outputCache;
        }
        br.close();
        return output;
    }

}