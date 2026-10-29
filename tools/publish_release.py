#!/usr/bin/env python3
"""
Publishes a built release so installed apps get the "new version" notification.

  ./gradlew :app:assembleRelease && python3 tools/publish_release.py

Copies the signed APK to release/DNBA.apk and writes release/latest.json from the newest entry in
data/Changelog.kt (versionCode, name, Hebrew/English notes). Commit and push both files: apps read
https://raw.githubusercontent.com/elroitouati/NBECK/HEAD/release/latest.json (see data/Updates.kt).
"""
import json, os, re, shutil, sys

ROOT = os.path.join(os.path.dirname(__file__), "..")
APK_URL = "https://raw.githubusercontent.com/elroitouati/NBECK/HEAD/release/DNBA.apk"

src = open(os.path.join(ROOT, "app/src/main/java/com/nbeck/app/data/Changelog.kt"), encoding="utf-8").read()
m = re.search(r'Release\(\s*(\d+),\s*"([^"]+)",\s*"[^"]+",\s*he = listOf\((.*?)\),\s*en = listOf\((.*?)\),\s*\)', src, re.S)
code, name, he, en = int(m[1]), m[2], m[3], m[4]
strings = lambda block: [bytes(s, "utf-8").decode("unicode_escape").encode("latin-1").decode("utf-8") for s in re.findall(r'"((?:[^"\\]|\\.)*)"', block)]

gradle = open(os.path.join(ROOT, "app/build.gradle.kts"), encoding="utf-8").read()
built = int(re.search(r"versionCode = (\d+)", gradle)[1])
if built != code:
    sys.exit(f"Changelog's newest entry is {code} but build.gradle.kts has versionCode {built}")

os.makedirs(os.path.join(ROOT, "release"), exist_ok=True)
shutil.copy(os.path.join(ROOT, "app/build/outputs/apk/release/app-release.apk"), os.path.join(ROOT, "release/DNBA.apk"))
with open(os.path.join(ROOT, "release/latest.json"), "w", encoding="utf-8") as fh:
    json.dump({"versionCode": code, "versionName": name, "apk": APK_URL, "notesHe": strings(he), "notesEn": strings(en)},
              fh, ensure_ascii=False, indent=1)
print(f"published {name} ({code})")
