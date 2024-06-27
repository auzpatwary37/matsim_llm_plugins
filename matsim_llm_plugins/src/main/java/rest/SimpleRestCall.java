package rest;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SimpleRestCall {
	public static void main(String[] args) {
		OkHttpClient client = new OkHttpClient();
				MediaType mediaType = MediaType.parse("application/json");
				RequestBody body = RequestBody.create(mediaType, "{ \n    \"model\": \"LM Studio Community/Meta-Llama-3-8B-Instruct-GGUF\",\n    \"messages\": [ \n      { \"role\": \"system\", \"content\": \"Always answer in rhymes.\" },\n      { \"role\": \"user\", \"content\": \"Introduce yourself.\" }\n    ], \n    \"temperature\": 0.7, \n    \"max_tokens\": -1,\n    \"stream\": false\n}");
				Request request = new Request.Builder()
				   .url("http://localhost:1234/v1/chat/completions")
				   .method("POST", body)
				   .addHeader("Content-Type", "application/json")
				   .build();
				try {
					Response response = client.newCall(request).execute();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	}

}
