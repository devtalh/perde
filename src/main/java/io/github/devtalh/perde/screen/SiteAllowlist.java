package io.github.devtalh.perde.screen;

import java.net.IDN;
import java.net.URI;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

/** Immutable trusted-site policy. It is not a network sandbox for code served by trusted sites. */
public record SiteAllowlist(List<String> domains) {
    public static final int MAX_DOMAINS = 4096;
    public static final List<String> DEFAULT_DOMAINS = List.of("youtube.com", "youtu.be", "youtube-nocookie.com",
            "twitch.tv", "kick.com", "w2g.tv", "vimeo.com", "dailymotion.com");

    public SiteAllowlist {
        var normalized = new LinkedHashSet<String>();
        for (String domain : domains) normalized.add(normalize(domain));
        if (normalized.size() > MAX_DOMAINS) throw new IllegalArgumentException("Too many allowed domains");
        domains = List.copyOf(normalized);
    }

    public static SiteAllowlist empty() { return new SiteAllowlist(List.of()); }
    public static SiteAllowlist defaults() { return new SiteAllowlist(DEFAULT_DOMAINS); }

    public static String normalize(String input) {
        if (input == null) throw new IllegalArgumentException("Missing domain");
        String domain = input.strip();
        if (domain.contains("://")) {
            if (!UrlPolicy.allowsPage(domain)) throw new IllegalArgumentException("Invalid public URL");
            domain = URI.create(domain).getRawAuthority();
            domain = domain.substring(domain.lastIndexOf('@') + 1);
            int port = domain.indexOf(':');
            if (port >= 0) domain = domain.substring(0, port);
        }
        domain = IDN.toASCII(domain, IDN.USE_STD3_ASCII_RULES).toLowerCase(Locale.ROOT);
        if (domain.endsWith(".")) domain = domain.substring(0, domain.length() - 1);
        if (domain.startsWith("www.")) domain = domain.substring(4);
        String last = domain.substring(domain.lastIndexOf('.') + 1);
        if (domain.length() > 253 || !domain.contains(".") || last.matches("[0-9]+|0x[0-9a-f]*")
                || !UrlPolicy.allowsPage("https://" + domain)) {
            throw new IllegalArgumentException("Expected a public domain name, not an IP or local name");
        }
        return domain;
    }

    public boolean allowsPage(String url) {
        if (!UrlPolicy.allowsPage(url)) return false;
        try {
            String host = normalize(url);
            return domains.stream().anyMatch(domain -> host.equals(domain) || host.endsWith("." + domain));
        } catch (IllegalArgumentException exception) { return false; }
    }

    public boolean allowsMainNavigation(String url) {
        return "about:blank".equalsIgnoreCase(url) || allowsPage(url);
    }

    public SiteAllowlist adding(String input) {
        String domain = normalize(input);
        var next = new LinkedHashSet<>(domains);
        next.add(domain);
        return new SiteAllowlist(List.copyOf(next));
    }

    public SiteAllowlist removing(String input) {
        String domain = normalize(input);
        return new SiteAllowlist(domains.stream().filter(value -> !value.equals(domain)).toList());
    }
}
