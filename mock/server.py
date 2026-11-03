#!/usr/bin/env python3
"""Faux panneau Xtream pour tester IPTVPlayer hors ligne.

Identifiants : demo / demo
L'émulateur Android accède au serveur via http://10.0.2.2:8099
"""
from __future__ import annotations

import argparse
import json
import os
from datetime import datetime, timezone
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import parse_qs, unquote, urlparse

MEDIA = Path(__file__).resolve().parent / "media"
EXP_DATE = int(datetime(2026, 12, 31, 12, 0, 0, tzinfo=timezone.utc).timestamp())
# MOCK_HLS=0 force le repli de l'application vers le flux MPEG-TS.
HLS_ENABLED = os.environ.get("MOCK_HLS", "1") != "0"

LIVE_CATEGORIES = [
    {"category_id": "1", "category_name": "Généralistes", "parent_id": 0},
    {"category_id": "2", "category_name": "Info", "parent_id": 0},
    {"category_id": "3", "category_name": "Sport", "parent_id": 0},
    {"category_id": "4", "category_name": "Cinéma", "parent_id": 0},
    {"category_id": "5", "category_name": "Jeunesse", "parent_id": 0},
]

LIVE_STREAMS = [
    ("1", "1", "France 2 HD", "1"),
    ("2", "2", "France 3", "1"),
    ("3", "3", "TF1", "1"),
    ("4", "4", "Arte", "1"),
    ("5", "5", "TV5 Monde", "1"),
    ("10", "10", "BFM TV", "2"),
    ("11", "11", "France Info", "2"),
    ("12", "12", "LCI", "2"),
    ("20", "20", "Eurosport 1", "3"),
    ("21", "21", "beIN Sports 1", "3"),
    ("22", "22", "Canal+ Sport", "3"),
    ("30", "30", "Canal+", "4"),
    ("31", "31", "Ciné+ Premier", "4"),
    ("32", "32", "Paramount Channel", "4"),
    ("40", "40", "Disney Channel", "5"),
    ("41", "41", "Gulli", "5"),
    ("42", "42", "Nickelodeon", "5"),
]

VOD_CATEGORIES = [
    {"category_id": "101", "category_name": "Films récents", "parent_id": 0},
    {"category_id": "102", "category_name": "Classiques", "parent_id": 0},
    {"category_id": "103", "category_name": "Documentaires", "parent_id": 0},
]

VOD_STREAMS = [
    ("201", "Le Grand Bleu", "1988", "102"),
    ("202", "Intouchables", "2011", "101"),
    ("203", "Océans", "2009", "103"),
    ("204", "Kirikou et la sorcière", "1998", "102"),
    ("205", "Astérix et Obélix : Mission Cléopâtre", "2002", "101"),
    ("206", "Le Petit Prince", "2015", "101"),
    ("207", "La Planète bleue", "2003", "103"),
    ("208", "Les Choristes", "2004", "102"),
]


class Handler(BaseHTTPRequestHandler):
    server_version = "IPTVPlayerMock/1.0"

    def do_GET(self) -> None:  # noqa: N802
        parsed = urlparse(self.path)
        path = unquote(parsed.path)
        query = parse_qs(parsed.query)

        if path == "/health":
            self.send_json({"status": "ok"})
            return

        if path == "/player_api.php":
            self.handle_api(query)
            return

        if path.startswith("/live/"):
            self.handle_live(path)
            return

        if path.startswith("/movie/"):
            self.serve_file(MEDIA / "sample.mp4", "video/mp4")
            return

        self.send_error(404, "Not found")

    def do_HEAD(self) -> None:  # noqa: N802
        self.do_GET()

    def handle_api(self, query: dict[str, list[str]]) -> None:
        username = query.get("username", [""])[0]
        password = query.get("password", [""])[0]
        if username != "demo" or password != "demo":
            self.send_json({"user_info": {"auth": 0}})
            return

        action = query.get("action", [""])[0]
        category = query.get("category_id", [None])[0]

        if action == "":
            self.send_json(
                {
                    "user_info": {
                        "username": "demo",
                        "password": "demo",
                        "message": "Bienvenue sur IPTVPlayer",
                        "auth": 1,
                        "status": "Active",
                        "exp_date": str(EXP_DATE),
                        "is_trial": "0",
                        "active_cons": "1",
                        "created_at": "1700000000",
                        "max_connections": "1",
                        "allowed_output_formats": ["m3u8", "ts"],
                    },
                    "server_info": {
                        "url": "10.0.2.2",
                        "port": str(self.server.server_port),
                        "https_port": "",
                        "server_protocol": "http",
                        "timezone": "Europe/Paris",
                    },
                }
            )
            return

        if action == "get_live_categories":
            self.send_json(LIVE_CATEGORIES)
            return

        if action == "get_live_streams":
            stream_ids = {item[0] for item in LIVE_STREAMS}
            streams = [
                {
                    "num": int(num),
                    "name": name,
                    "stream_type": "live",
                    "stream_id": int(stream_id),
                    "stream_icon": "",
                    "epg_channel_id": None,
                    "added": "1700000000",
                    "category_id": cat,
                    "custom_sid": "",
                    "tv_archive": 0,
                    "direct_source": "",
                    "tv_archive_duration": 0,
                }
                for stream_id, num, name, cat in LIVE_STREAMS
                if stream_id in stream_ids and (category is None or cat == category)
            ]
            self.send_json(streams)
            return

        if action == "get_vod_categories":
            self.send_json(VOD_CATEGORIES)
            return

        if action == "get_vod_streams":
            movies = [
                {
                    "num": index + 1,
                    "name": name,
                    "stream_type": "movie",
                    "stream_id": int(stream_id),
                    "stream_icon": "",
                    "rating": "7.5",
                    "rating_5based": 3.8,
                    "added": "1700000000",
                    "category_id": cat,
                    "container_extension": "mp4",
                    "year": year,
                    "custom_sid": "",
                    "direct_source": "",
                }
                for index, (stream_id, name, year, cat) in enumerate(VOD_STREAMS)
                if category is None or cat == category
            ]
            self.send_json(movies)
            return

        self.send_json([])

    def handle_live(self, path: str) -> None:
        name = path.rsplit("/", 1)[-1].lower()
        if name.endswith(".m3u8"):
            if not HLS_ENABLED:
                self.send_error(404, "HLS disabled for fallback test")
                return
            self.serve_file(MEDIA / "live.m3u8", "application/vnd.apple.mpegurl")
            return
        if name.endswith(".ts"):
            if name.startswith("seg"):
                candidate = MEDIA / name
                if candidate.exists():
                    self.serve_file(candidate, "video/mp2t")
                    return
            self.serve_file(MEDIA / "live.ts", "video/mp2t")
            return
        self.send_error(404, "Not found")

    def serve_file(self, file_path: Path, content_type: str) -> None:
        if not file_path.exists():
            self.send_error(404, "Not found")
            return
        size = file_path.stat().st_size
        start, end = 0, size - 1
        status = 200
        range_header = self.headers.get("Range", "")
        if range_header.startswith("bytes="):
            spec = range_header[6:].split(",", 1)[0]
            first, _, last = spec.partition("-")
            try:
                if first:
                    start = int(first)
                if last:
                    end = min(int(last), size - 1)
                elif not first:
                    start = 0
                if first and not last:
                    end = size - 1
            except ValueError:
                self.send_error(400, "Bad range")
                return
            if start >= size or start > end:
                self.send_response(416)
                self.send_header("Content-Range", f"bytes */{size}")
                self.end_headers()
                return
            status = 206

        length = end - start + 1
        self.send_response(status)
        self.send_header("Content-Type", content_type or "application/octet-stream")
        self.send_header("Content-Length", str(length))
        self.send_header("Accept-Ranges", "bytes")
        self.send_header("Cache-Control", "no-store")
        if status == 206:
            self.send_header("Content-Range", f"bytes {start}-{end}/{size}")
        self.end_headers()
        if self.command == "HEAD":
            return
        with file_path.open("rb") as handle:
            handle.seek(start)
            remaining = length
            while remaining > 0:
                chunk = handle.read(min(64 * 1024, remaining))
                if not chunk:
                    break
                self.wfile.write(chunk)
                remaining -= len(chunk)

    def send_json(self, payload: object, status: int = 200) -> None:
        data = json.dumps(payload, ensure_ascii=False).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(data)))
        self.send_header("Cache-Control", "no-store")
        self.end_headers()
        if self.command != "HEAD":
            self.wfile.write(data)

    def log_message(self, fmt: str, *args: object) -> None:
        print(f"[mock] {self.address_string()} - {fmt % args}")


def main() -> None:
    parser = argparse.ArgumentParser(description="Faux panneau Xtream pour IPTVPlayer")
    parser.add_argument("--host", default="0.0.0.0")
    parser.add_argument("--port", type=int, default=8099)
    args = parser.parse_args()

    server = ThreadingHTTPServer((args.host, args.port), Handler)
    print(f"[mock] Xtream prêt : http://{args.host}:{args.port}")
    print("[mock] Identifiants : demo / demo")
    print("[mock] Depuis l'émulateur : http://10.0.2.2:%d" % args.port)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass
    finally:
        server.server_close()


if __name__ == "__main__":
    main()
