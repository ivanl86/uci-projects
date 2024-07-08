import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;


public class ReCaptchaVerifyUtils {

    public static final String SITE_VERIFY_URL = "https://www.google.com/recaptcha/api/siteverify";

    private ReCaptchaVerifyUtils() {
    }

    public static boolean verify(String gRecaptchaResponse, String secretKey) {

        try {
            HttpURLConnection httpConnection = (HttpURLConnection) new URL(SITE_VERIFY_URL).openConnection();
            String postParams = "secret=" + URLEncoder.encode(secretKey, StandardCharsets.UTF_8)
                    + "&response=" + URLEncoder.encode(gRecaptchaResponse, StandardCharsets.UTF_8);

            httpConnection.setDoOutput(true);
            httpConnection.setRequestMethod("POST");
            httpConnection.setRequestProperty("User-Agent", "Mozilla/5.0");
            httpConnection.setRequestProperty("Accept-Language", "en-US,en;q=0.5");

            try (DataOutputStream dataWriter = new DataOutputStream(httpConnection.getOutputStream())) {

                dataWriter.writeBytes(postParams);
                dataWriter.flush();
            }

            if (httpConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {

                try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(httpConnection.getInputStream()))) {

                    JsonObject jsonObject = new Gson().fromJson(bufferedReader, JsonObject.class);

                    return jsonObject.has("success") && jsonObject.get("success").getAsBoolean();
                }
            } else {
                return false;
            }
        } catch (Exception e) {

            e.printStackTrace();
            return false;
        }
    }
}
