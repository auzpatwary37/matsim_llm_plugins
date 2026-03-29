package chatcommons;
import java.time.Duration;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class OkHttpLmStudioEmbedTest {

    public static void main(String[] args) throws Exception {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofSeconds(60))
                .writeTimeout(Duration.ofSeconds(60))
                .build();

        String json = """
        {
          "model": "text-embedding-granite-embedding-278m-multilingual",
          "input": "This is a test embedding!!!"
        }
        """;

        RequestBody body = RequestBody.create(
                json,
                MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
                .url("http://127.0.0.1:1234/v1/embeddings")
                .addHeader("Authorization", "Bearer lm-studio")
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            System.out.println("HTTP " + response.code());
            System.out.println(response.body() != null ? response.body().string() : "<no body>");
        }
    }
}