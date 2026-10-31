package io.github.devtalh.perde.client.browser;

import com.mojang.blaze3d.textures.GpuTextureView;

/** Keeps optional MCEF classes behind a loader-guarded implementation. All calls use the client thread. */
interface BrowserBackend {
    default void checkHealth() { }
    default boolean currentPageAllowed() { return true; }
    default void sync(io.github.devtalh.perde.screen.PlaybackClock playback, Long serverNowMs) { }
    boolean ready();
    void open(String url);
    void loadUrl(String url);
    GpuTextureView textureView();
    void close();
}
