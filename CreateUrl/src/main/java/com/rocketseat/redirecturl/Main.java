package com.rocketseat.redirecturl;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.Map;
import java.util.UUID;

public class Main implements RequestHandler<Map<String, Object>, Map<String, Object>> {
    private final ObjectMapper mapper = new ObjectMapper();
    private final S3Client s3Client = S3Client.builder().build();

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
        var body = (String) input.get("body");

        Map<String, String> bodyMap;
        try {
            bodyMap = mapper.readValue(body, Map.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("error in parsing Json body:" + e.getMessage(), e);
        }

        String originalUrl = bodyMap.get("originalUrl");
        String expirationTime = bodyMap.get("expirationTime");


        UUID id = UUID.randomUUID();

        UrlData urlData = new UrlData(originalUrl, Long.parseLong(expirationTime));

        try {
            String json = mapper.writeValueAsString(urlData);

            PutObjectRequest request = PutObjectRequest
                    .builder()
                    .bucket("example-bucket")
                    .key(id.toString())
                    .build();

            s3Client.putObject(request, RequestBody.fromString(json));
        } catch (Exception ex) {
            throw new RuntimeException("ERROR IN SAVING URL DATA TO S3: " + ex.getMessage(), ex);

        }

        return Map.of("code", id.toString());
    }
}