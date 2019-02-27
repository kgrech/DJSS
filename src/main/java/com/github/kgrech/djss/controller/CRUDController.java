package com.github.kgrech.djss.controller;

import static spark.Spark.path;

import com.github.kgrech.djss.view.Page;
import lombok.extern.slf4j.Slf4j;
import spark.Request;
import spark.Spark;

@Slf4j
public abstract class CRUDController<T> extends JsonController {

    public static final String PAGE = "page";
    public static final String PAGE_SZE = "pageSize";
    public static final String ID = ":id";

    private final Class<T> tClass;

    protected CRUDController(Class<T> tClass) {
        this.tClass = tClass;
    }

    public void init() {
        path(getPath(), () -> {
            get("/", (req, res) ->
                    getPage(
                        intParam(req, PAGE, 0),
                        intParam(req, PAGE_SZE, 10)
                    )
            );
            get("/" + ID, (req, res) ->
                    get(intPathParam(req, ID))
            );
            post("/", (req, res) -> {
                        T value = transform(req.body(), tClass);
                        T created = create(value);
                        res.status(201);
                        return created;
                    }
            );
            put("/" + ID, (req, res) -> {
                        T value = transform(req.body(), tClass);
                        return update(intPathParam(req, ID), value);
                    }
            );
            Spark.delete("/" + ID, (req, res) -> {
                delete(intPathParam(req, ID));
                return "";
            });
        });
    }

    protected abstract String getPath();

    protected abstract T get(int id);

    protected abstract Page<T> getPage(int page, int pageSize);

    protected abstract T create(T newInstance);

    protected abstract T update(int id, T updateInstance);

    protected abstract void delete(int id);

    private int intParam(Request req, String param, int defaultValue) {
        String value = req.queryParams(param);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException e) {
            log.debug("Incorrect value of {} param: ", param, value);
            return defaultValue;
        }
    }

    private int intPathParam(Request req, String param) {
        String value = req.params(param);
        if (value == null) {
            throw new IllegalArgumentException("Missing mandatory param " + param);
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Missing mandatory param " + param, e);

        }
    }
}
