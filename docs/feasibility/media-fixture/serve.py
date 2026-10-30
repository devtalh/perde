"""Repository-local media fixture server with HTTP byte-range support.

Python 3.12's SimpleHTTPRequestHandler ignores Range requests. Chromium may
therefore treat a WebM served by that handler as unseekable. This server only
serves the explicit fixture files and binds to loopback.
"""

import argparse
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
import re
from urllib.parse import urlsplit


ROOT = Path(__file__).resolve().parent
FILES = {
    "/": ("index.html", "text/html; charset=utf-8"),
    "/index.html": ("index.html", "text/html; charset=utf-8"),
    "/empty.html": ("empty.html", "text/html; charset=utf-8"),
    "/sample.webm": ("sample.webm", "video/webm"),
}


class FixtureHandler(BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.1"

    def do_HEAD(self):
        self.serve_file(head_only=True)

    def do_GET(self):
        self.serve_file(head_only=False)

    def serve_file(self, head_only):
        entry = FILES.get(urlsplit(self.path).path)
        if entry is None:
            self.send_error(404)
            return
        file_path = ROOT / entry[0]
        if not file_path.is_file():
            self.send_error(404)
            return
        total = file_path.stat().st_size
        start, end = 0, total - 1
        range_header = self.headers.get("Range")
        if range_header is not None:
            match = re.fullmatch(r"bytes=(\d*)-(\d*)", range_header.strip())
            try:
                if match is None or not any(match.groups()):
                    raise ValueError("Only a single byte range is supported")
                first, last = match.groups()
                if first:
                    start = int(first)
                    end = min(int(last), total - 1) if last else total - 1
                else:
                    suffix_length = int(last)
                    if suffix_length <= 0:
                        raise ValueError("Invalid suffix length")
                    start = max(0, total - suffix_length)
                if start >= total or start > end:
                    raise ValueError("Unsatisfiable byte range")
            except ValueError:
                self.send_response(416)
                self.send_header("Content-Range", f"bytes */{total}")
                self.send_header("Content-Length", "0")
                self.send_header("Accept-Ranges", "bytes")
                self.end_headers()
                return
        length = end - start + 1
        self.send_response(206 if range_header is not None else 200)
        self.send_header("Content-Type", entry[1])
        self.send_header("Content-Length", str(length))
        self.send_header("Accept-Ranges", "bytes")
        self.send_header("Cache-Control", "no-store")
        if range_header is not None:
            self.send_header("Content-Range", f"bytes {start}-{end}/{total}")
        self.end_headers()
        if head_only:
            return
        try:
            with file_path.open("rb") as media:
                media.seek(start)
                remaining = length
                while remaining:
                    chunk = media.read(min(64 * 1024, remaining))
                    if not chunk:
                        break
                    self.wfile.write(chunk)
                    remaining -= len(chunk)
        except (BrokenPipeError, ConnectionResetError):
            pass  # Browser may cancel a previous range after a seek.


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--port", type=int, default=8765)
    args = parser.parse_args()
    server = ThreadingHTTPServer(("127.0.0.1", args.port), FixtureHandler)
    print(f"Serving fixture with byte ranges at http://127.0.0.1:{args.port}/", flush=True)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass
    finally:
        server.server_close()
