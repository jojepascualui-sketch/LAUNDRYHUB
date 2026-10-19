package com.laundryhub.admin.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.laundryhub.admin.api.dto.Dashboard;
import com.laundryhub.admin.api.dto.OrderUpdate;
import com.laundryhub.admin.model.OrderStatus;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** The HTTP client against a local server: token header, JSON records, query strings and problem errors. */
class HttpAdminApiTest {

    private static final int WAIT_SECONDS = 5;

    private HttpServer server;
    private HttpAdminApi api;
    private final AtomicReference<String> lastPath = new AtomicReference<>();
    private final AtomicReference<String> lastAuth = new AtomicReference<>();
    private final AtomicReference<String> lastBody = new AtomicReference<>();
    private volatile int status = 200;
    private volatile String response = "{}";

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            lastPath.set(exchange.getRequestMethod() + " " + exchange.getRequestURI());
            lastAuth.set(exchange.getRequestHeaders().getFirst("Authorization"));
            lastBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, status == 204 ? -1 : bytes.length);
            if (status != 204) {
                try (OutputStream out = exchange.getResponseBody()) {
                    out.write(bytes);
                }
            }
            exchange.close();
        });
        server.start();
        String base = "http://127.0.0.1:" + server.getAddress().getPort() + "/api/";
        api = new HttpAdminApi(new ApiClient(base, () -> "token-1"));
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    @Test
    void dashboardSendsTheTokenAndParsesRecords() throws Exception {
        response = "{\"ordersToday\":6,\"waitingToProcess\":2,\"kilosToday\":62.00,\"salesToday\":2375.00,"
                + "\"pipeline\":[{\"status\":\"PENDING\",\"label\":\"Pending\",\"count\":2}],"
                + "\"recentOrders\":[],\"somethingNew\":true}";

        Dashboard dashboard = api.dashboard().get(WAIT_SECONDS, TimeUnit.SECONDS);

        assertEquals("GET /api/admin/dashboard", lastPath.get());
        assertEquals("Bearer token-1", lastAuth.get());
        assertEquals(6, dashboard.ordersToday());
        assertEquals(new BigDecimal("2375.00"), dashboard.salesToday());
        assertEquals(2, dashboard.pipeline().get(0).count());
    }

    @Test
    void orderSearchIsEncoded() throws Exception {
        response = "{\"totalCount\":0,\"counts\":[],\"orders\":[]}";

        api.orders(OrderStatus.OUT_FOR_DELIVERY, "Dela Cruz").get(WAIT_SECONDS, TimeUnit.SECONDS);

        assertEquals("GET /api/admin/orders?status=OUT_FOR_DELIVERY&q=Dela+Cruz", lastPath.get());
    }

    @Test
    void patchLeavesOutNullFields() throws Exception {
        response = "{}";

        api.updateOrder(7, new OrderUpdate(null, 3L)).get(WAIT_SECONDS, TimeUnit.SECONDS);

        assertEquals("PATCH /api/admin/orders/7", lastPath.get());
        assertEquals("{\"riderId\":3}", lastBody.get());
    }

    @Test
    void problemDetailsBecomeTheMessage() {
        status = 409;
        response = "{\"title\":\"Conflict\",\"status\":409,\"detail\":\"Walk-in orders have no rider jobs.\"}";

        ExecutionException error = assertThrows(ExecutionException.class,
                () -> api.updateOrder(1, new OrderUpdate(null, 2L)).get(WAIT_SECONDS, TimeUnit.SECONDS));

        ApiException cause = assertInstanceOf(ApiException.class, error.getCause());
        assertEquals(409, cause.getStatus());
        assertEquals("Walk-in orders have no rider jobs.", cause.getMessage());
    }

    @Test
    void expiredSessionIsUnauthorized() {
        status = 401;
        response = "";

        ExecutionException error = assertThrows(ExecutionException.class,
                () -> api.pricing().get(WAIT_SECONDS, TimeUnit.SECONDS));

        assertEquals(true, ((ApiException) error.getCause()).isUnauthorized());
    }

    @Test
    void emptyBodyIsNull() throws Exception {
        status = 204;

        assertNull(api.updateOrder(1, new OrderUpdate("READY", null)).get(WAIT_SECONDS, TimeUnit.SECONDS));
    }

    @Test
    void feedbackPollsWithAfterAndRepliesWithABody() throws Exception {
        response = "{\"threadId\":4,\"messages\":[{\"id\":9,\"side\":\"USER\",\"senderName\":\"Ana Reyes\","
                + "\"mine\":false,\"body\":\"Hi\",\"sentAt\":\"2026-10-19T14:05:00\"}]}";

        var chat = api.feedbackMessages(4, 8L).get(WAIT_SECONDS, TimeUnit.SECONDS);

        assertEquals("GET /api/admin/feedback/threads/4/messages?after=8", lastPath.get());
        assertEquals("Ana Reyes", chat.messages().get(0).senderName());

        status = 201;
        response = "{\"id\":10,\"side\":\"SHOP\",\"senderName\":\"Admin\",\"mine\":true,\"body\":\"Hello\","
                + "\"sentAt\":\"2026-10-19T14:06:00\"}";
        assertEquals(true, api.reply(4, "Hello").get(WAIT_SECONDS, TimeUnit.SECONDS).mine());
        assertEquals("POST /api/admin/feedback/threads/4/messages", lastPath.get());
        assertEquals("{\"body\":\"Hello\"}", lastBody.get());
    }
}
