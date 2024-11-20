package com.rocketseat.redirecturl;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Main implements RequestHandler<Map<String, Object>, Map<String, Object>> {
    private final ObjectMapper mapper = new ObjectMapper();
    private final S3Client s3Client = S3Client.builder().build();

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
        var pathParameters = (String) input.get("rawPath");
        String shortUrlCode = pathParameters.replace("/", "");

        if (Objects.isNull(shortUrlCode) || shortUrlCode.isEmpty()) {
            throw new IllegalArgumentException("Invalid input: 'shortUrlCode' is required.");
        }

        GetObjectRequest request = GetObjectRequest
                .builder()
                .bucket("example-bucket")
                .key(shortUrlCode + ".json")
                .build();
        InputStream s3ObjectStream;
        try {
            s3ObjectStream = s3Client.getObject(request);
        } catch (Exception ex) {
            throw new RuntimeException("Error in get object in s3" + ex.getMessage(), ex);
        }

        UrlData urlData;
        try {
            urlData = mapper.readValue(s3ObjectStream, UrlData.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        long currentTime = System.currentTimeMillis() / 1000;

        Map<String, Object> response = new HashMap<>();

        if(urlData.getExpirationTime() < currentTime) {
            response.put("statusCode", 410);
            response.put("body", "URL EXPIRED");
            return response;
        }

        response.put("statusCode", 302);
        Map<String, String> headers = new HashMap<>();
        headers.put("Location", urlData.getOriginalUrl());
        response.put("headers", headers);

        return response;
    }
}