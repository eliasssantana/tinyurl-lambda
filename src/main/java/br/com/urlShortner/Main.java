package br.com.urlShortner;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Main implements RequestHandler<Map<String, Object>, Map<String, String>> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final S3Client s3Client = S3Client.builder().build();

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, String> handleRequest(Map<String, Object> event, Context context) {

        System.out.println(event.get("body"));

        String body = null;

        try {
            body = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error during event object data binding: " + e.getMessage(), e);
        }

        Map<String, String> bodyMap;
        try{
            bodyMap = objectMapper.readValue(body, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Error parsing JSON body: " + e.getMessage(), e);
        }

        String originalUrl = bodyMap.get("originalUrl");
        String expirationTime = bodyMap.get("expirationTime");

        if (expirationTime == null || expirationTime.isEmpty()){
            throw new IllegalArgumentException("ExpirationTime field not informed.");
        }

        long expirationTimeInSeconds = Long.parseLong(expirationTime);

        String shortUrlCode = UUID.randomUUID().toString().substring(0, 8);

        UrlData urlData = UrlData().builder()
                .originalUrl(originalUrl)
                .expirationTime(expirationTimeInSeconds)
                .build();

        try {
            String urlDataJson = objectMapper.writeValueAsString(urlData);

            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket("url-shortened-storage-area")
                    .key(shortUrlCode + ".json")
                    .build();

            s3Client.putObject(request, RequestBody.fromString(urlDataJson));

        } catch (Exception e){
            throw new RuntimeException("Error saving URL data to S3: " + e.getMessage(), e);
        }

        Map<String, String> response = new HashMap<>();

        response.put("code", shortUrlCode);

        return response;
    }
}
