package com.laundryhub.app;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SanityTest {

    @Test
    public void apiBaseUrlTargetsHost() {
        assertTrue(BuildConfig.API_BASE_URL.startsWith("http://10.0.2.2"));
    }
}
