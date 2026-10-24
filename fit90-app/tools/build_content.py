#!/usr/bin/env python3
"""Copies /content into the APK assets and writes index.json for each language
(article and audio metadata, so the app can list content without reading every file)."""
import json, math, os, re, shutil, sys

sys.path.insert(0, os.path.dirname(__file__))
from content_qa import front

def main(src, dst):
    shutil.rmtree(dst, ignore_errors=True)
    meta_all = json.load(open(os.path.join(src, "content.json"), encoding="utf-8"))
    for lang in sorted(os.listdir(src)):
        ldir = os.path.join(src, lang)
        if not os.path.isdir(ldir) or lang == "templates":
            continue
        shutil.copytree(ldir, os.path.join(dst, lang))
        index = {"version": meta_all.get("version", ""), "last_reviewed": meta_all.get("last_reviewed", ""),
                 "articles": [], "audio": []}
        for kind in ("articles", "audio"):
            d = os.path.join(ldir, kind)
            if not os.path.isdir(d):
                continue
            for name in sorted(os.listdir(d)):
                if not name.endswith(".md"):
                    continue
                meta, body = front(open(os.path.join(d, name), encoding="utf-8").read())
                meta = dict(meta or {})
                meta["file"] = f"{kind}/{name}"
                if kind == "articles":
                    words = len(re.findall(r"[A-Za-z0-9’']+", body.split("## Sources")[0]))
                    meta["words"] = words
                    meta["minutes"] = max(2, math.ceil(words / 200))
                    meta["tags"] = [t.strip() for t in meta.get("tags", "").strip("[]").split(",") if t.strip()]
                    meta["order"] = int(meta.get("order", 0))
                else:
                    meta["level"] = int(meta.get("level", 1))
                    meta["minutes"] = int(meta.get("minutes", 5))
                index[kind].append(meta)
        json.dump(index, open(os.path.join(dst, lang, "index.json"), "w", encoding="utf-8"), ensure_ascii=False)
        print(f"  {lang}: {len(index['articles'])} articles, {len(index['audio'])} audio scripts")

if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
