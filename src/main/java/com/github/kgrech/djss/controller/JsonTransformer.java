package com.github.kgrech.djss.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import spark.ResponseTransformer;

public class JsonTransformer implements ResponseTransformer {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String render(Object model) throws JsonProcessingException {
        return mapper.writeValueAsString(model);
    }

    public <T> T transform(String body, Class<T> clazz) throws IOException {
        return mapper.readValue(body, clazz);
    }

}