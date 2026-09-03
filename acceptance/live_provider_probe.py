#!/usr/bin/env python3
"""Live acceptance probe for bundled zero-cost catalog providers.

The generated media experience must not claim useful built-in discovery merely
because provider classes compile. This probe exercises the same reviewed public
catalog endpoints used by AIDao-generated apps and requires at least one bundled
provider to return a meaningful result. It intentionally validates metadata
catalog discovery only; it does not add or test unauthorized playback sources.
"""

import json
import sys
import urllib.error
import urllib.parse
import urllib.request

UA = "AIDao-V1-Acceptance/1.0 (+https://github.com/IcyKokane/AIDaoPublic)"
QUERY = "Naruto"


def request_json(url, *, data=None, headers=None, timeout=20):
    base_headers = {
        "User-Agent": UA,
        "Accept": "application/json",
    }
    if headers:
        base_headers.update(headers)
    req = urllib.request.Request(url, data=data, headers=base_headers)
    with urllib.request.urlopen(req, timeout=timeout) as response:
        if response.status < 200 or response.status >= 300:
            raise RuntimeError(f"HTTP {response.status}")
        return json.loads(response.read().decode("utf-8"))


def probe_jikan():
    url = "https://api.jikan.moe/v4/anime?" + urllib.parse.urlencode(
        {"q": QUERY, "limit": 5}
    )
    payload = request_json(url)
    rows = payload.get("data") or []
    titles = [str(row.get("title") or "").strip() for row in rows if isinstance(row, dict)]
    titles = [title for title in titles if title]
    if not titles:
        raise RuntimeError("reachable but returned no usable catalog titles")
    return titles


def probe_anilist():
    query = """
    query ($search: String) {
      Page(page: 1, perPage: 5) {
        media(search: $search, type: ANIME) { id title { romaji english } }
      }
    }
    """
    body = json.dumps({"query": query, "variables": {"search": QUERY}}).encode("utf-8")
    payload = request_json(
        "https://graphql.anilist.co",
        data=body,
        headers={"Content-Type": "application/json"},
    )
    rows = (((payload.get("data") or {}).get("Page") or {}).get("media") or [])
    titles = []
    for row in rows:
        if not isinstance(row, dict):
            continue
        title = row.get("title") or {}
        value = str(title.get("english") or title.get("romaji") or "").strip()
        if value:
            titles.append(value)
    if not titles:
        raise RuntimeError("reachable but returned no usable catalog titles")
    return titles


def main():
    probes = [("Jikan", probe_jikan), ("AniList", probe_anilist)]
    successes = []
    failures = []
    for name, probe in probes:
        try:
            titles = probe()
            successes.append((name, titles))
            print(f"PASS {name}: {len(titles)} usable titles; sample={titles[0]!r}")
        except (urllib.error.URLError, urllib.error.HTTPError, TimeoutError, RuntimeError, ValueError, json.JSONDecodeError) as exc:
            failures.append((name, str(exc)))
            print(f"FAIL {name}: {exc}")

    if not successes:
        print("FAIL bundled-provider discovery: no reviewed built-in provider returned usable data")
        for name, error in failures:
            print(f"  {name}: {error}")
        return 1

    print("PASS bundled-provider discovery: at least one authorized zero-cost catalog source is live")
    return 0


if __name__ == "__main__":
    sys.exit(main())
