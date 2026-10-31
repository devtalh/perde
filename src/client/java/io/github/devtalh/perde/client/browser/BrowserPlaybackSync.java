package io.github.devtalh.perde.client.browser;

import io.github.devtalh.perde.Perde;
import io.github.devtalh.perde.screen.PlaybackClock;
import org.cef.CefClient;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.browser.CefMessageRouter;
import org.cef.callback.CefQueryCallback;
import org.cef.handler.CefMessageRouterHandlerAdapter;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Client tick owns decisions; CEF's UI thread only submits bounded observations. */
final class BrowserPlaybackSync implements AutoCloseable {
    private static final int MAX_FRAMES = 256;
    private final String token;
    private final CefBrowser browser;
    private final CefClient client;
    private final CefMessageRouter router;
    private final Set<String> pending = new HashSet<>();
    private final List<Reply> replies = new ArrayList<>();
    private final Map<String, Long> seeks = new LinkedHashMap<>();
    private long poll, nextPoll, collectAt, pageStarted, nextSummary;
    private boolean closed, collecting, warnedMissing, warnedPlay;
    private record Reply(String frame, PlaybackSyncMessage message, CefQueryCallback callback) { }

    BrowserPlaybackSync(CefClient client, CefBrowser browser) {
        this.client = client;
        this.browser = browser;
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        token = HexFormat.of().formatHex(bytes);
        router = CefMessageRouter.create(new CefMessageRouterHandlerAdapter() {
            @Override public boolean onQuery(CefBrowser source, CefFrame frame, long queryId, String request,
                                             boolean persistent, CefQueryCallback callback) {
                synchronized (BrowserPlaybackSync.this) {
                    if (closed || source != browser || frame == null || persistent || !collecting) return false;
                    String id = frame.getIdentifier();
                    if (!pending.contains(id)) return false;
                    PlaybackSyncMessage message = PlaybackSyncMessage.parse(request, token, poll);
                    if (message == null) return false;
                    pending.remove(id);
                    replies.add(new Reply(id, message, callback));
                    return true;
                }
            }
        });
        if (router == null) throw new IllegalStateException("Could not create private playback router");
        client.addMessageRouter(router);
        newPage();
    }

    synchronized void newPage() {
        cancelReplies();
        seeks.clear();
        nextPoll = 0;
        pageStarted = monotonicMs();
        nextSummary = pageStarted;
        warnedMissing = warnedPlay = false;
    }

    synchronized void tick(PlaybackClock clock, long serverNowMs) {
        if (closed) return;
        long now = monotonicMs();
        if (collecting && now >= collectAt) finish(clock, serverNowMs, now);
        if (now < nextPoll) return;
        nextPoll = now + 500;
        collecting = true;
        collectAt = now + 100;
        poll++;
        var ids = browser.getFrameIdentifiers();
        if (ids == null) return;
        for (String id : ids) {
            if (pending.size() >= MAX_FRAMES) break;
            CefFrame frame = browser.getFrameByIdentifier(id);
            if (frame == null) continue;
            try {
                if (!frame.isValid()) continue;
                pending.add(id);
                frame.executeJavaScript(PlaybackSyncScript.sample(token, poll), frame.getURL(), 0);
            } finally { frame.dispose(); }
        }
    }

    private void finish(PlaybackClock clock, long serverNowMs, long now) {
        collecting = false;
        pending.clear();
        Reply selected = replies.stream().filter(reply -> reply.message().element() != 0)
                .min(Comparator.<Reply, Boolean>comparing(reply -> reply.message().media().paused())
                        .thenComparing(Comparator.comparingDouble((Reply reply) -> reply.message().area()).reversed())).orElse(null);
        for (Reply reply : replies) {
            PlaybackSyncDecision.Action action = PlaybackSyncDecision.Action.NONE;
            if (reply == selected) {
                var message = reply.message();
                String element = reply.frame() + ":" + message.element();
                long target = clock.current(serverNowMs);
                action = PlaybackSyncDecision.decide(target, clock.playing(), message.media(), now, seeks.get(element), false);
                if (action.seekMs() != null) {
                    if (seeks.size() >= 256 && !seeks.containsKey(element)) seeks.remove(seeks.keySet().iterator().next());
                    seeks.put(element, now);
                    Perde.LOGGER.info("Perde sync driftMs={} action=seek targetMs={}", message.media().positionMs() - target, action.seekMs());
                }
                if (action.transport() != PlaybackSyncDecision.Transport.NONE) Perde.LOGGER.info("Perde sync driftMs={} action={}",
                        message.media().positionMs() - target, action.transport());
                if (message.playRejected() && !warnedPlay) {
                    warnedPlay = true;
                    Perde.LOGGER.warn("Perde sync: page rejected play(); retrying on the next loop.");
                }
                if (now >= nextSummary) {
                    nextSummary = now + 10_000;
                    Perde.LOGGER.debug("Perde sync driftMs={} targetMs={} readyState={} live={}", message.media().positionMs() - target,
                            target, message.media().readyState(), message.media().live());
                }
            }
            respond(reply, action);
        }
        if (selected == null && !warnedMissing && now - pageStarted >= 10_000) {
            warnedMissing = true;
            Perde.LOGGER.warn("Perde sync: no visible video on this page; URL guard remains active.");
        }
        replies.clear();
    }

    private void respond(Reply reply, PlaybackSyncDecision.Action action) {
        reply.callback().success("{\"seek\":" + action.seekMs() + ",\"transport\":\"" + action.transport() + "\"}");
    }

    private void cancelReplies() {
        collecting = false;
        pending.clear();
        for (Reply reply : replies) reply.callback().failure(-1, "Page changed");
        replies.clear();
    }

    @Override public synchronized void close() {
        if (closed) return;
        closed = true;
        cancelReplies();
        client.removeMessageRouter(router);
        router.dispose();
    }
    private static long monotonicMs() { return System.nanoTime() / 1_000_000; }
}
