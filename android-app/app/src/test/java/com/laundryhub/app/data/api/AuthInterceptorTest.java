package com.laundryhub.app.data.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

public class AuthInterceptorTest {

    private MockWebServer server;
    private String token;
    private boolean unauthorized;

    @Before
    public void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
    }

    @After
    public void tearDown() throws Exception {
        server.shutdown();
    }

    private OkHttpClient client() {
        return new OkHttpClient.Builder().addInterceptor(new AuthInterceptor(new TokenProvider() {
            @Override
            public String getToken() {
                return token;
            }

            @Override
            public void onUnauthorized() {
                unauthorized = true;
            }
        })).build();
    }

    private RecordedRequest call(int status) throws Exception {
        server.enqueue(new MockResponse().setResponseCode(status));
        client().newCall(new Request.Builder().url(server.url("/api/orders/mine")).build()).execute().close();
        return server.takeRequest();
    }

    @Test
    public void addsTheBearerTokenWhenSignedIn() throws Exception {
        token = "jwt-123";
        assertEquals("Bearer jwt-123", call(200).getHeader("Authorization"));
    }

    @Test
    public void sendsNoHeaderWhenSignedOut() throws Exception {
        token = null;
        assertNull(call(200).getHeader("Authorization"));
    }

    @Test
    public void a401EndsTheSession() throws Exception {
        token = "expired";
        call(401);
        assertTrue(unauthorized);
    }
}
