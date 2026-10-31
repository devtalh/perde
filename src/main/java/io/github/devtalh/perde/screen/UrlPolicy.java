package io.github.devtalh.perde.screen;

import java.net.IDN;
import java.net.InetAddress;
import java.net.URI;
import java.util.Locale;
import java.util.Set;

/** Literal-only policy. No DNS lookup: DNS rebinding and public names resolving privately are out of scope. */
public final class UrlPolicy {
    private static final Set<String> NETWORK = Set.of("http", "https", "ws", "wss");
    private UrlPolicy() { }

    public static boolean allowsNavigation(String url) {
        return "about:blank".equalsIgnoreCase(url) || allowsNetwork(url, false);
    }

    public static boolean allowsResource(String url) {
        if (url == null) return false;
        int colon = url.indexOf(':');
        if (colon < 1) return false;
        String scheme = url.substring(0, colon).toLowerCase(Locale.ROOT);
        if (scheme.equals("data")) return true;
        if (scheme.equals("blob")) {
            String origin = url.substring(colon + 1);
            return origin.startsWith("null/") || allowsNetwork(origin, false);
        }
        return allowsNetwork(url, true);
    }

    public static boolean allowsPage(String url) { return allowsNetwork(url, false); }

    private static boolean allowsNetwork(String url, boolean webSocket) {
        if (url == null) return false;
        try {
            URI uri = new URI(url);
            String scheme = uri.getScheme();
            if (scheme == null || !NETWORK.contains(scheme.toLowerCase(Locale.ROOT))
                    || (!webSocket && !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https")))) return false;
            // Fail closed for ambiguous authorities instead of disagreeing with Chromium's URL parser.
            String authority = uri.getRawAuthority();
            if (authority == null || authority.isEmpty()) return false;
            int at = authority.lastIndexOf('@');
            String hostPort = authority.substring(at + 1);
            String host;
            String port = "";
            if (hostPort.startsWith("[")) {
                int end = hostPort.indexOf(']');
                if (end < 0) return false;
                host = hostPort.substring(1, end);
                port = hostPort.substring(end + 1);
                if (host.contains("%")) return false; // No scoped addresses or escaped hosts.
                if (!publicLiteral(InetAddress.ofLiteral(host).getAddress())) return false;
            } else {
                int separator = hostPort.indexOf(':');
                host = separator < 0 ? hostPort : hostPort.substring(0, separator);
                port = separator < 0 ? "" : hostPort.substring(separator);
                if (host.contains("%") || host.isEmpty()) return false;
                host = IDN.toASCII(host, IDN.USE_STD3_ASCII_RULES).toLowerCase(Locale.ROOT);
                if (host.endsWith(".")) host = host.substring(0, host.length() - 1);
                if (host.isEmpty() || host.endsWith(".") || host.equals("localhost") || host.endsWith(".localhost")) return false;
                for (String suffix : new String[]{".local", ".lan", ".home", ".internal"}) {
                    if (host.endsWith(suffix)) return false;
                }
                String last = host.substring(host.lastIndexOf('.') + 1);
                // WHATWG IPv4 supports hex/octal, fewer than four components and a single integer.
                if (last.matches("[0-9]+") || last.matches("0[xX][0-9a-fA-F]*")) {
                    if (!publicIpv4(parseIpv4(host))) return false;
                }
            }
            return port.isEmpty() || (port.matches(":[0-9]{1,5}") && Integer.parseInt(port.substring(1)) <= 65535);
        } catch (IllegalArgumentException | java.net.URISyntaxException exception) {
            return false;
        }
    }

    private static long parseIpv4(String host) {
        String[] parts = host.split("\\.", -1);
        if (parts.length > 4) throw new IllegalArgumentException("Invalid IPv4");
        long address = 0;
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            int radix = 10;
            if (part.startsWith("0x") || part.startsWith("0X")) { radix = 16; part = part.substring(2); }
            else if (part.length() > 1 && part.startsWith("0")) { radix = 8; part = part.substring(1); }
            if (part.isEmpty()) part = "0";
            if (!part.matches(radix == 16 ? "[0-9a-fA-F]+" : radix == 8 ? "[0-7]+" : "[0-9]+")) {
                throw new IllegalArgumentException("Invalid IPv4 component");
            }
            long value = Long.parseLong(part, radix);
            long limit = i == parts.length - 1 ? 1L << (8 * (5 - parts.length)) : 256;
            if (value >= limit) throw new IllegalArgumentException("IPv4 component overflow");
            address = i == parts.length - 1 ? (address << (8 * (5 - parts.length))) + value : (address << 8) + value;
        }
        return address;
    }

    private static boolean publicIpv4(long ip) {
        long a = ip >>> 24, b = (ip >>> 16) & 255;
        return !(a == 0 || a == 10 || a == 127 || a >= 224 || (a == 172 && b >= 16 && b <= 31)
                || (a == 192 && b == 168) || (a == 169 && b == 254) || (a == 100 && b >= 64 && b <= 127));
    }

    private static boolean publicLiteral(byte[] bytes) {
        if (bytes.length == 4) {
            long ip = 0;
            for (byte value : bytes) ip = (ip << 8) | (value & 255);
            return publicIpv4(ip);
        }
        int a = bytes[0] & 255, b = bytes[1] & 255;
        boolean zeroPrefix = true;
        for (int i = 0; i < 15; i++) zeroPrefix &= bytes[i] == 0;
        return !(zeroPrefix && (bytes[15] == 0 || bytes[15] == 1)) && (a & 254) != 252
                && !(a == 254 && (b & 192) == 128) && a != 255;
    }
}
