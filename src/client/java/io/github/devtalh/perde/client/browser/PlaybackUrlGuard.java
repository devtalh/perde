package io.github.devtalh.perde.client.browser;

/** Initial redirects may commit; subsequent media identity changes restore the server URL. */
final class PlaybackUrlGuard {
    private String committed;
    private boolean restore;

    synchronized void reset() { committed = null; restore = false; }
    synchronized void committed(String url) {
        String identity = PlaybackMediaIdentity.key(url);
        if (committed == null && identity != null) committed = identity;
        else address(url);
    }
    synchronized void address(String url) {
        if (committed != null && !committed.equals(PlaybackMediaIdentity.key(url))) restore = true;
    }
    synchronized boolean takeRestore() {
        if (!restore) return false;
        reset();
        return true;
    }
}
