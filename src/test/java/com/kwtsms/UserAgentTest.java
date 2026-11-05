/*
 * FILE:      src/test/java/com/kwtsms/UserAgentTest.java
 * CREATED:   2026-11-05 14:21 +03
 * TASK:      Vikunja #939 (int-measure-signups)
 * AUTHOR:    M Alhassoun
 * WHAT:      Unit tests for the User-Agent kwtsms-java/VERSION and the optional appName token.
 * WHY:       kwtSMS counts accounts per integration from the User-Agent; it must be exact and header-safe.
 * USAGE:     bash ./gradlew test --tests com.kwtsms.UserAgentTest
 * DEPENDS:   JUnit 5; JDK com.sun.net.httpserver for a local server on 127.0.0.1; no internet.
 */
package com.kwtsms;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class UserAgentTest {

    private static final String LIB = "kwtsms-java/" + KwtSMS.VERSION;

    @Test
    void versionEqualsBuildGradle() throws Exception {
        String gradle = new String(Files.readAllBytes(Paths.get("build.gradle")), StandardCharsets.UTF_8);
        Matcher m = Pattern.compile("(?m)^version = '([^']+)'").matcher(gradle);
        assertTrue(m.find(), "version line not found in build.gradle");
        assertEquals(m.group(1), KwtSMS.VERSION);
    }

    @Test
    void defaultIsLibraryToken() {
        String ua = new KwtSMS("java_your_api_user", "java_your_api_pass", "TEST", true, "").getUserAgent();
        assertEquals(LIB, ua);
        assertTrue(ua.matches("^kwtsms-java/[0-9][0-9A-Za-z.+-]*$"), ua);
    }

    @Test
    void appNameIsPrepended() {
        KwtSMS c = new KwtSMS("java_your_api_user", "java_your_api_pass", "TEST", true, "", "kwtsms-wordpress/3.5.2");
        assertEquals("kwtsms-wordpress/3.5.2 " + LIB, c.getUserAgent());
    }

    @Test
    void appNameIsSanitized() {
        String ua = KwtSMS.buildUserAgent("my app\r\nX-Injected: 1");
        assertFalse(ua.contains("\r"));
        assertFalse(ua.contains("\n"));
        assertFalse(ua.contains(" X-Injected"));
        assertTrue(ua.endsWith(" " + LIB));
        assertEquals(LIB, KwtSMS.buildUserAgent("  - -  "));
        assertEquals(LIB, KwtSMS.buildUserAgent(null));
        assertEquals(64, KwtSMS.buildUserAgent(new String(new char[200]).replace('\0', 'a')).split(" ")[0].length());
    }

    @Test
    void headerIsSentOnRequest() throws Exception {
        AtomicReference<String> got = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/API/balance/", ex -> {
            got.set(ex.getRequestHeaders().getFirst("User-Agent"));
            byte[] body = "{\"result\":\"OK\",\"available\":1,\"purchased\":1}".getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(200, body.length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(body);
            }
        });
        server.start();
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("username", "java_your_api_user");
            payload.put("password", "java_your_api_pass");
            String base = "http://127.0.0.1:" + server.getAddress().getPort() + "/API";
            Map<String, Object> r = ApiRequest.post(base, "balance", payload, "", KwtSMS.buildUserAgent("my-shop/2.1.0"));
            assertEquals("OK", r.get("result"));
            assertEquals("my-shop/2.1.0 " + LIB, got.get());
        } finally {
            server.stop(0);
        }
    }
}
