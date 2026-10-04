#!/usr/bin/env python3
"""
Generates the bundled narration clips shipped in app/src/main/assets/narration.

Why bundle narration instead of relying on the system text-to-speech engine?
  * Many devices (and most emulators) ship no Bangla or Arabic voice data. The
    app declares no INTERNET permission, so it cannot download voices at
    runtime - every Bangla/Arabic "Listen" tap then fell back to the same
    generic chime instead of speech. Bundled clips speak correctly on any
    device, on a plane, with no permission and no download.
  * Nothing is fetched at runtime: the only network use is *here*, on the
    developer's machine, when (re)generating the clips. The APK stays offline.

What is covered: every static Listen/quiz/praise line the app can speak in
Bangla, Arabic, or Maths/English-lesson context (~250 short clips):
  * lesson cards:  "A. A for Apple" / "খ, খরগোশ" / "ب, بَطَّة"
  * quiz prompts:  word-only "খরগোশ" (picture -> letter never reveals the letter)
  * numbers:       "One" .. "Twenty"; shapes: "Red" .. "Heart"; "How many?"
  * encouragements: the 5 English + 5 Bangla mascot lines.
Dynamic English-only prompts ("3 plus 2", badge names) stay on system TTS:
en-US is present on virtually every device, and pre-generating every
arithmetic combination would bloat the APK for no benefit.

Sources of truth: the Kotlin catalogs themselves - this script *parses*
EnglishCatalog.kt / BanglaCatalog.kt / ArabicCatalog.kt / MathsCatalog.kt and
FeedbackPlayer.kt, so a catalog edit without re-running shows up as a
`--check` failure and as a BundledNarrationTest failure, never as a silent
wrong-voice regression.

Run:  python3 tools/generate_narration.py            # fetch + write (needs net)
      python3 tools/generate_narration.py --check    # offline verify, for CI

Output: MP3 clips + manifest.json under app/src/main/assets/narration/.
Filenames are ASCII slugs; the (text, locale) -> file mapping lives in
manifest.json, which BundledSpeechPlayer reads at runtime.
"""

import json
import os
import re
import sys
import time
import unicodedata
import urllib.parse
import urllib.request

ROOT = os.path.join(os.path.dirname(__file__), "..")
MAIN = os.path.join(ROOT, "app", "src", "main", "java", "com", "freedu", "kidslearn")
OUT_DIR = os.path.join(ROOT, "app", "src", "main", "assets", "narration")
MANIFEST = os.path.join(OUT_DIR, "manifest.json")

STR = r'"((?:[^"\\]|\\.)*)"'
CALL = re.compile(r"(?:vowel|consonant|letter|colour|color|shape|count)\((.*)\)")


def read(name):
    with open(os.path.join(MAIN, name), encoding="utf-8") as f:
        return f.read()


def split_args(body):
    parts, depth, cur, instr, esc = [], 0, "", False, False
    for ch in body:
        if instr:
            cur += ch
            if esc:
                esc = False
            elif ch == "\\":
                esc = True
            elif ch == '"':
                instr = False
        else:
            if ch == '"':
                instr = True
                cur += ch
            elif ch == "(":
                depth += 1
                cur += ch
            elif ch == ")":
                depth -= 1
                cur += ch
            elif ch == "," and depth == 0:
                parts.append(cur.strip())
                cur = ""
            else:
                cur += ch
    parts.append(cur.strip())
    return parts


def unquote(token):
    m = re.fullmatch(STR, token.strip())
    if not m:
        raise ValueError(f"not a string literal: {token!r}")
    # Kotlin escapes only; never touch the raw UTF-8 text itself (a
    # unicode_escape round-trip would mangle every Bangla/Arabic glyph).
    return m.group(1).replace('\\"', '"').replace("\\\\", "\\")


def entries(src, kinds):
    found = []
    for line in src.splitlines():
        s = line.strip().rstrip(",")
        for kind in kinds:
            if s.startswith(kind + "(") and s.endswith(")"):
                tokens = split_args(s[len(kind) + 1:-1])
                # `count()` takes a bare Int first arg; everything else is quoted.
                found.append([t if re.fullmatch(r"-?\d+", t) else unquote(t) for t in tokens])
    return found


def nfc(s):
    return unicodedata.normalize("NFC", s)


def collect():
    """Returns [(text, tts_lang, locale_tag, filename)]."""
    phrases = []

    en = entries(read("data/content/EnglishCatalog.kt"), ["letter"])
    assert len(en) == 26, f"English letters: {len(en)}"
    for i, (upper, lower, sound, word, meaning, visual) in enumerate(en, 1):
        phrases.append((nfc(f"{upper}. {upper} for {word}"), "en", "en-US", f"en_l{i:02d}.mp3"))
        phrases.append((nfc(word), "en", "en-US", f"en_w{i:02d}.mp3"))

    bn_src = read("data/content/BanglaCatalog.kt")
    bn_v = entries(bn_src, ["vowel"])
    bn_c = entries(bn_src, ["consonant"])
    assert len(bn_v) == 11, f"Bangla vowels: {len(bn_v)}"
    assert len(bn_c) == 35, f"Bangla consonants: {len(bn_c)}"
    for i, (glyph, sound, word, meaning, visual) in enumerate(bn_v + bn_c, 1):
        phrases.append((nfc(f"{glyph}, {word}"), "bn", "bn-BD", f"bn_l{i:02d}.mp3"))
        phrases.append((nfc(word), "bn", "bn-BD", f"bn_w{i:02d}.mp3"))

    ar_src = read("data/content/ArabicCatalog.kt")
    ar_v = entries(ar_src, ["vowel"])
    ar_c = entries(ar_src, ["consonant"])
    assert len(ar_v) == 3, f"Arabic vowels: {len(ar_v)}"
    assert len(ar_c) == 25, f"Arabic consonants: {len(ar_c)}"
    for i, (glyph, sound, word, meaning, visual) in enumerate(ar_v + ar_c, 1):
        phrases.append((nfc(f"{glyph}, {word}"), "ar", "ar-SA", f"ar_l{i:02d}.mp3"))
        phrases.append((nfc(word), "ar", "ar-SA", f"ar_w{i:02d}.mp3"))

    maths = read("data/content/MathsCatalog.kt")
    counts = entries(maths, ["count"])
    assert len(counts) == 20, f"counting: {len(counts)}"
    for i, (number, emoji, word) in enumerate(counts, 1):
        phrases.append((nfc(word), "en", "en-US", f"math_n{i:02d}.mp3"))
    shapes = entries(maths, ["colour", "color", "shape"])
    assert len(shapes) == 13, f"shapes: {len(shapes)}"
    for i, (name, *_) in enumerate(shapes, 1):
        phrases.append((nfc(name), "en", "en-US", f"math_s{i:02d}.mp3"))
    phrases.append(("How many?", "en", "en-US", "prompt_howmany.mp3"))

    fb = read("core/audio/FeedbackPlayer.kt")
    enc = re.findall(r"(\w+)\(" + STR + r",\s*" + STR + r"\)", fb)
    enc = [(n, a, b) for n, a, b in enc
           if n not in ("vowel", "consonant", "letter", "colour", "color", "shape", "count")]
    # Keep only the Encouragement enum entries (5).
    names = ["GreatJob", "WellDone", "Awesome", "YouGotIt", "TryAgain"]
    enc = [(n, a, b) for n, a, b in enc if n in names]
    assert len(enc) == 5, f"encouragements: {enc}"
    for i, (name, english, bangla) in enumerate(enc, 1):
        phrases.append((nfc(english), "en", "en-US", f"enc_en_{i:02d}.mp3"))
        phrases.append((nfc(bangla), "bn", "bn-BD", f"enc_bn_{i:02d}.mp3"))

    # Identical (locale, text) pairs (e.g. "Orange" as an English word and as a
    # shape name) need only one clip: the runtime lookup is exact text, so the
    # first filename wins and later repeats are dropped.
    seen, unique = set(), []
    for t, lang, loc, fn in phrases:
        if (loc, t) not in seen:
            seen.add((loc, t))
            unique.append((t, lang, loc, fn))
    return unique


def fetch(text, lang, tries=5):
    params = urllib.parse.urlencode({"ie": "UTF-8", "q": text, "tl": lang, "client": "tw-ob"})
    req = urllib.request.Request(
        "https://translate.google.com/translate_tts?" + params,
        headers={"User-Agent": "Mozilla/5.0"},
    )
    last = None
    for attempt in range(tries):
        try:
            with urllib.request.urlopen(req, timeout=20) as r:
                data = r.read()
            if len(data) < 1000:
                raise IOError(f"suspiciously small reply ({len(data)} bytes)")
            return data
        except Exception as e:  # noqa: BLE001 - retry transient net errors
            last = e
            time.sleep(2 ** attempt)
    raise IOError(f"TTS fetch failed for {text!r} ({lang}): {last}")


def main():
    check_only = "--check" in sys.argv
    force = "--force" in sys.argv
    phrases = collect()
    print(f"{len(phrases)} phrases parsed from catalogs.")

    if check_only:
        with open(MANIFEST, encoding="utf-8") as f:
            manifest = json.load(f)
        have = {(e["locale"], e["text"]): e["file"] for e in manifest["entries"]}
        missing = [(loc, t) for t, lang, loc, fn in phrases if (loc, t) not in have]
        gone = [k for k in have if k not in set((loc, t) for t, lang, loc, fn in phrases)]
        bad_files = [e for e in manifest["entries"]
                     if not os.path.exists(os.path.join(OUT_DIR, e["file"]))]
        if missing or gone or bad_files:
            print(f"FAIL: {len(missing)} phrases missing from manifest, "
                  f"{len(gone)} stale, {len(bad_files)} files missing.")
            print("Run: python3 tools/generate_narration.py")
            sys.exit(1)
        print(f"OK: manifest covers all {len(phrases)} phrases, all files present.")
        return

    os.makedirs(OUT_DIR, exist_ok=True)
    total, done = 0, 0
    for text, lang, locale, fn in phrases:
        path = os.path.join(OUT_DIR, fn)
        if os.path.exists(path) and not force:
            total += os.path.getsize(path)
            done += 1
            continue
        data = fetch(text, lang)
        with open(path, "wb") as f:
            f.write(data)
        total += len(data)
        done += 1
        print(f"  [{done:3d}/{len(phrases)}] {fn:22s} {len(data):>6d} bytes  {locale} {text[:28]}")
        time.sleep(0.3)
    manifest = {
        "version": 1,
        "entries": [
            {"text": t, "locale": loc, "file": fn} for t, lang, loc, fn in phrases
        ],
    }
    with open(MANIFEST, "w", encoding="utf-8") as f:
        json.dump(manifest, f, ensure_ascii=False, indent=1)
    print(f"Wrote {done} clips + manifest.json ({total / 1024:.0f} KB total).")


if __name__ == "__main__":
    main()
