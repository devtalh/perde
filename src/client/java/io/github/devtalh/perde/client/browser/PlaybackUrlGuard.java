package io.github.devtalh.perde.client.browser;

/** Initial HTTP redirects may commit; subsequent document and History API changes restore the server URL. */
final class PlaybackUrlGuard {
    private String committed;
    private boolean restore;

    synchronized void reset() { committed = null; restore = false; }
    synchronized void committed(String url) {
        if (committed == null && io.github.devtalh.perde.screen.UrlPolicy.allowsPage(url)) committed = url;
        else address(url);
    }
    synchronized void address(String url) {
        if (committed != null && !committed.equals(url)) restore = true;
    }
    synchronized boolean takeRestore() {
        if (!restore) return false;
        reset();
        return true;
    }
}
