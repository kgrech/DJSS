package com.github.kgrech.djss.controller;

import static com.google.common.net.HttpHeaders.CONTENT_TYPE;
import static org.eclipse.jetty.http.MimeTypes.Type.APPLICATION_JSON;

import java.io.IOException;
import spark.Route;
import spark.Spark;


public abstract class JsonController {

    private final JsonTransformer transformer = new JsonTransformer();

    protected void get(String path, Route route) {
        Spark.get(path, header(route), transformer);
    }

    protected void post(String path, Route route) {
        Spark.post(path, APPLICATION_JSON.asString(),
                header(route), transformer);
    }

    protected void put(String path, Route route) {
        Spark.put(path, APPLICATION_JSON.asString(),
                header(route), transformer);
    }

    private Route header(Route route) {
        return (request, response) -> {
            response.header(CONTENT_TYPE, APPLICATION_JSON.asString());
            return route.handle(request, response);
        };
    }

    public <T> T transform(String body, Class<T> clazz) throws IOException {
        return transformer.transform(body, clazz);
    }
}
