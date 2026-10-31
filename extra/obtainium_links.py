#!/usr/bin/env python3
"""Rebuilds the Obtainium badge links in README.md from extra/obtainium.json.

Usage, from the repository root:
    python3 extra/obtainium_links.py           # update README.md
    python3 extra/obtainium_links.py --print   # only print the links
"""

import json
import re
import sys
import urllib.parse
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
CONFIG = ROOT / "extra" / "obtainium.json"
README = ROOT / "README.md"
REDIRECT = "https://apps.obtainium.imranr.dev/redirect?r=obtainium://app/"


def build_link(config: dict) -> str:
    # Obtainium expects additionalSettings as a JSON string inside the config
    payload = dict(config, additionalSettings=json.dumps(config["additionalSettings"], separators=(",", ":")))
    return REDIRECT + urllib.parse.quote(json.dumps(payload, separators=(",", ":")), safe="")


def main() -> None:
    apps = json.loads(CONFIG.read_text())["apps"]
    links = {app["badgeAlt"]: build_link(app["config"]) for app in apps}

    if "--print" in sys.argv:
        for alt, link in links.items():
            print(f"{alt}:\n{link}\n")
        return

    readme = README.read_text()
    for alt, link in links.items():
        # the badge is <a href="LINK"><img ... alt="ALT" ...></a>
        pattern = re.compile(r'(<a href=")' + re.escape(REDIRECT) + r'[^"]*("><img[^>]*alt="' + re.escape(alt) + r'")')
        readme, count = pattern.subn(lambda m: m.group(1) + link + m.group(2), readme)
        if count != 1:
            sys.exit(f'Expected one badge with alt="{alt}" in README.md, found {count}')
    README.write_text(readme)
    print(f"Updated {len(links)} Obtainium links in README.md")


if __name__ == "__main__":
    main()
