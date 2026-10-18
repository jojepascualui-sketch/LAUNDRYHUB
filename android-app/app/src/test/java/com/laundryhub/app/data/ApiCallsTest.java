package com.laundryhub.app.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class ApiCallsTest {

    @Test
    public void readsProblemDetails() throws Exception {
        ApiException e = ApiCalls.toException(400,
                "{\"status\":400,\"detail\":\"Some fields are not valid.\",\"errors\":{\"email\":\"must be a well-formed email address\"}}");

        assertEquals(400, e.getStatus());
        assertEquals("Some fields are not valid.", e.getMessage());
        assertEquals("must be a well-formed email address", e.getFieldErrors().get("email"));
    }

    @Test
    public void nonJsonBodiesStillGiveTheStatus() throws Exception {
        ApiException e = ApiCalls.toException(502, "<html>Bad gateway</html>");

        assertEquals(502, e.getStatus());
        assertNull(e.getMessage());
    }
}
