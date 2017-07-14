package com.github.phiz71.vertx.swagger.router.extractors;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.github.phiz71.vertx.swagger.router.SwaggerRouter;

import io.swagger.models.Swagger;
import io.swagger.parser.SwaggerParser;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.file.FileSystem;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.Router;

@RunWith(VertxUnitRunner.class)
public class HeaderParameterExtractorTest {

    private static final int TEST_PORT = 9292;
    private static final String TEST_HOST = "localhost";
    private static Vertx vertx;
    private static EventBus eventBus;
    private static HttpClient httpClient;
    private static HttpServer httpServer;

    @BeforeClass
    public static void beforeClass(TestContext context) {
        Async before = context.async();
        vertx = Vertx.vertx();
        eventBus = vertx.eventBus();

        // init Router
        FileSystem vertxFileSystem = vertx.fileSystem();
        vertxFileSystem.readFile("extractors/swaggerHeaderExtractor.json", readFile -> {
            if (readFile.succeeded()) {
                Swagger swagger = new SwaggerParser().parse(readFile.result().toString(Charset.forName("utf-8")));
                Router swaggerRouter = SwaggerRouter.swaggerRouter(Router.router(vertx), swagger, eventBus);

                httpServer = vertx.createHttpServer().requestHandler(swaggerRouter::accept).listen(TEST_PORT, TEST_HOST, listen -> {
                    if (listen.succeeded()) {
                        before.complete();
                    } else {
                        context.fail(listen.cause());
                    }
                });
            } else {
                context.fail(readFile.cause());
            }
        });

        // init consumers
        eventBus.<JsonObject> consumer("GET_header_required").handler(message -> {
            String header = message.body().getString("headerRequired");
            message.reply(null, new DeliveryOptions().addHeader("headerRequiredResp", header));
        });
        eventBus.<JsonObject> consumer("GET_header_not_required").handler(message -> {
            String header = message.body().getString("headerNotRequired");
            if(header != null) {
                message.reply(null, new DeliveryOptions().addHeader("headerNotRequiredResp", header));
            } else {
                message.reply(null);
            }
        });
        eventBus.<JsonObject> consumer("GET_header_array").handler(message -> {
            JsonArray headers = message.body().getJsonArray("arrayHeader");
            DeliveryOptions options = new DeliveryOptions();
            headers.forEach(head-> {options.addHeader("arrayHeaderResp", head.toString());});

            message.reply(null, options);
        });

        // init http client
        httpClient = Vertx.vertx().createHttpClient();

    }

    @Test()
    public void testOkHeaderRequired(TestContext context) {
        Async async = context.async();
        HttpClientRequest req = httpClient.get(TEST_PORT, TEST_HOST, "/header/required");
        req.handler(response -> {
            context.assertEquals(response.statusCode(), 200);
            context.assertEquals("toto", response.headers().get("headerRequiredResp"));
            async.complete();
        }).putHeader("headerRequired", "toto").end();
    }

    @Test()
    public void testKoHeaderRequired(TestContext context) {
        Async async = context.async();
        HttpClientRequest req = httpClient.get(TEST_PORT, TEST_HOST, "/header/required");
        req.handler(response -> {
            context.assertEquals(response.statusCode(), 400);
            async.complete();
        }).end();
    }

    @Test()
    public void testOkHeaderNotRequiredWithHeader(TestContext context) {
        Async async = context.async();
        HttpClientRequest req = httpClient.get(TEST_PORT, TEST_HOST, "/header/not/required");
        req.handler(response -> {
            context.assertEquals(response.statusCode(), 200);
            context.assertEquals("toto", response.headers().get("headerNotRequiredResp"));
            async.complete();
        }).putHeader("headerNotRequired", "toto").end();
    }

    @Test()
    public void testOkHeaderNotRequiredWithoutHeader(TestContext context) {
        Async async = context.async();
        HttpClientRequest req = httpClient.get(TEST_PORT, TEST_HOST, "/header/not/required");
        req.handler(response -> {
            context.assertEquals(response.statusCode(), 200);
            context.assertEquals(null, response.headers().get("headerNotRequiredResp"));
            async.complete();
        }).end();
    }

    @Test()
    public void testOkHeaderArrayWithOneHeader(TestContext context) {
        Async async = context.async();
        HttpClientRequest req = httpClient.get(TEST_PORT, TEST_HOST, "/header/array");
        req.handler(response -> {
            context.assertEquals(response.statusCode(), 200);
            context.assertEquals("[toto]", response.headers().getAll("arrayHeaderResp").toString());
            async.complete();
        })
        .putHeader("arrayHeader", "toto")
        .end();
    }

    @Test()
    public void testOkHeaderArrayWithTwoHeader(TestContext context) {
        Async async = context.async();
        HttpClientRequest req = httpClient.get(TEST_PORT, TEST_HOST, "/header/array");
        List<String> headers = new ArrayList<>();
        headers.add("toto1");
        headers.add("toto2");
        req.handler(response -> {
            context.assertEquals(response.statusCode(), 200);
            context.assertEquals("[toto1, toto2]", response.headers().getAll("arrayHeaderResp").toString());
            async.complete();
        }).putHeader("arrayHeader", headers).end();
    }

    @AfterClass
    public static void afterClass(TestContext context) {
        Async after = context.async();
        httpServer.close(completionHandler -> {
            if (completionHandler.succeeded()) {
                FileSystem vertxFileSystem = vertx.fileSystem();
                vertxFileSystem.deleteRecursive(".vertx", true, vertxDir -> {
                    if (vertxDir.succeeded()) {
                        after.complete();
                    } else {
                        context.fail(vertxDir.cause());
                    }
                });
            }
        });

    }

}