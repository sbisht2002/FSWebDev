#!/usr/bin/env python3
"""Content QA: must pass before every release (build.sh runs it).

Checks
  * every article has front matter (id, title, summary, category, version, last_reviewed),
    600-1,500 words, the disclaimer, a "## Sources" list with at least two linked sources
    and a "Last reviewed" line matching its front matter
  * every audio script has front matter and valid timing markers
  * no banned phrases anywhere in /content or the coach system prompt:
    "cure", "guaranteed", "#1", "clinically proven", or "sex therapist" describing the app/coach
Usage: tools/content_qa.py content [extra files...]
"""
import json, os, re, sys

DISCLAIMER = "Education, not medical advice"
BANNED = [
    (re.compile(r"\bcur(e|es|ed|ing)\b", re.I), '"cure"'),
    (re.compile(r"\bguarantee(d|s)?\b", re.I), '"guaranteed"'),
    (re.compile(r"#1\b"), '"#1"'),
    (re.compile(r"clinically[- ]proven", re.I), '"clinically proven"'),
    (re.compile(r"\b(this app|the app|our app|the coach|i am|i'm|we are|kai is|coach is)\b[^.\n]{0,40}\bsex therapist", re.I),
     '"sex therapist" describing the app or coach'),
]
MARKER = re.compile(r"^\[(pause|in|out|hold|holdout|squeeze|release|rest|drop|flick|repeat|/repeat|chime)(\s+\d+(\.\d+)?)?\]")
ART_KEYS = ["id", "title", "summary", "category", "version", "last_reviewed"]
AUDIO_KEYS = ["id", "title", "category", "level", "minutes", "version", "last_reviewed"]

def front(text):
    if not text.startswith("---"):
        return None, text
    end = text.find("\n---", 3)
    if end < 0:
        return None, text
    meta = {}
    for line in text[3:end].splitlines():
        if ":" in line:
            k, v = line.split(":", 1)
            meta[k.strip()] = v.strip().strip('"')
    return meta, text[text.find("\n", end + 4) + 1:]

def main():
    root = sys.argv[1] if len(sys.argv) > 1 else "content"
    extra = sys.argv[2:]
    errors = []
    def err(path, msg):
        errors.append(f"{path}: {msg}")
    files = []
    for d, _, names in os.walk(root):
        for n in names:
            if n.endswith((".md", ".json")):
                files.append(os.path.join(d, n))
    files += [f for f in extra if os.path.exists(f)]
    articles = audio = 0
    for path in sorted(files):
        text = open(path, encoding="utf-8").read()
        for rx, label in BANNED:
            m = rx.search(text)
            if m:
                line = text[:m.start()].count("\n") + 1
                err(path, f"banned phrase {label} at line {line}: ...{text[max(0,m.start()-30):m.end()+30]!r}")
        if path.endswith(".json"):
            try:
                json.loads(text)
            except ValueError as e:
                err(path, f"invalid JSON: {e}")
            continue
        parts = path.replace("\\", "/").split("/")
        if "articles" in parts:
            articles += 1
            meta, body = front(text)
            if meta is None:
                err(path, "missing front matter"); continue
            for k in ART_KEYS:
                if not meta.get(k):
                    err(path, f"front matter missing '{k}'")
            words = len(re.findall(r"[A-Za-z0-9’']+", body.split("## Sources")[0]))
            if not 600 <= words <= 1500:
                err(path, f"{words} words (articles must be 600-1,500)")
            if DISCLAIMER not in body:
                err(path, "missing disclaimer")
            if "## Sources" not in body:
                err(path, "missing '## Sources'")
            else:
                src = body.split("## Sources", 1)[1]
                items = [l for l in src.splitlines() if l.strip().startswith("- ")]
                linked = [l for l in items if "http" in l and re.search(r"\b(19|20)\d\d\b", l)]
                if len(linked) < 2:
                    err(path, "needs at least two sources with a year and a link")
            m = re.search(r"Last reviewed:\s*(\d{4}-\d{2}-\d{2})", body)
            if not m:
                err(path, "missing 'Last reviewed: YYYY-MM-DD'")
            elif meta.get("last_reviewed") and m.group(1) != meta["last_reviewed"]:
                err(path, "Last reviewed date differs from front matter")
        elif "audio" in parts:
            audio += 1
            meta, body = front(text)
            if meta is None:
                err(path, "missing front matter"); continue
            for k in AUDIO_KEYS:
                if not meta.get(k):
                    err(path, f"front matter missing '{k}'")
            depth = 0
            for i, line in enumerate(body.splitlines(), 1):
                s = line.strip()
                if s.startswith("["):
                    if not MARKER.match(s):
                        err(path, f"line {i}: unknown timing marker {s[:30]!r}")
                    if s.startswith("[repeat"): depth += 1
                    if s.startswith("[/repeat"): depth -= 1
            if depth != 0:
                err(path, "unbalanced [repeat] blocks")
    for e in errors:
        print("QA:", e)
    print(f"Content QA: {articles} articles, {audio} audio scripts, {len(files)} files checked, {len(errors)} problems")
    sys.exit(1 if errors else 0)

if __name__ == "__main__":
    main()
