package io.github.devtalh.perde.client.browser;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class McefCompatibilityTest {
    @Test void detectsMissingImplementationWithoutInitializingNatives() {
        ClassLoader missing = new ClassLoader(getClass().getClassLoader()) {
            @Override public Class<?> loadClass(String name) throws ClassNotFoundException {
                if (name.equals("net.dimaskama.mcef.impl.MCEFBrowserImpl")) throw new ClassNotFoundException(name);
                return super.loadClass(name);
            }
        };
        assertThrows(ClassNotFoundException.class, () -> McefCompatibility.verify(missing));
    }

    @Test void detectsMissingConstructorAndNamesExpectedVersionForLinkageErrors() {
        assertThrows(NoSuchMethodException.class, () -> McefCompatibility.verifyImplementation(Object.class, getClass().getClassLoader()));
        assertThrows(NoSuchMethodException.class, () -> McefCompatibility.verifyImplementation(ConstructorOnly.class, getClass().getClassLoader()));
        var cause = new NoSuchMethodError("changed MCEF signature");
        var failure = McefCompatibility.incompatible(cause);
        assertSame(cause, failure.getCause());
        assertTrue(failure.getMessage().contains(McefCompatibility.EXPECTED_VERSION));
    }

    public static class ConstructorOnly {
        public ConstructorOnly(org.cef.CefClient client, String url, boolean transparent,
                               org.cef.browser.CefRequestContext context, org.cef.CefBrowserSettings settings) { }
    }
}
