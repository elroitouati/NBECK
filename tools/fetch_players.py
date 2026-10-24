#!/usr/bin/env python3
"""
Builds app/src/main/assets/players.json — the offline fallback roster bundled in the APK.

Data source: ESPN's public (free, key-less) site API — the same source the app uses at
runtime (see data/EspnSource.kt). Endpoints:
  rosters     https://site.api.espn.com/apis/site/v2/sports/basketball/nba/teams/{id}/roster
  depth chart https://site.api.espn.com/apis/site/v2/sports/basketball/nba/teams/{id}/depthcharts
  stats       https://site.web.api.espn.com/apis/common/v3/sports/basketball/nba/statistics/byathlete

Positions: ESPN rosters list only G/F/C. Combo positions (G-F, F-G, F-C, C-F) are derived from
the depth chart: a player who sits in the top 3 of an adjacent position group gets it as a
secondary position. This logic is mirrored exactly in EspnSource.kt.

Usage:  python3 tools/fetch_players.py
"""
import json, os, sys, urllib.request, concurrent.futures as cf
from datetime import datetime, timezone

ESPN_TEAM_IDS = [1,2,17,30,4,5,6,7,8,9,10,11,12,13,29,14,15,16,3,18,25,19,20,21,22,23,24,28,26,27]
SLOT_GROUP = {"pg": "G", "sg": "G", "sf": "F", "pf": "F", "c": "C"}
ADJ = {"G": ["F"], "F": ["G", "C"], "C": ["F"]}
DEPTH_TOP = 3

def get(url):
    req = urllib.request.Request(url, headers={"Accept": "application/json"})
    with urllib.request.urlopen(req, timeout=40) as r:
        return json.load(r)

def primary_group(abbr):
    a = (abbr or "").upper()
    if a in ("G", "PG", "SG"): return "G"
    if a in ("F", "SF", "PF"): return "F"
    if a == "C": return "C"
    return "F"

def country_of(bp):
    if not bp: return None
    c = bp.get("country")
    if c: return {"USA": "USA", "Türkiye": "Turkey"}.get(c, c)
    return "USA" if bp.get("state") else None

def team_data(tid):
    roster = get(f"https://site.api.espn.com/apis/site/v2/sports/basketball/nba/teams/{tid}/roster")
    try:
        depth = get(f"https://site.api.espn.com/apis/site/v2/sports/basketball/nba/teams/{tid}/depthcharts")
    except Exception:
        depth = {}
    best = {}  # athlete id -> {group: best rank}
    for chart in depth.get("depthchart", []):
        for slot, v in chart.get("positions", {}).items():
            g = SLOT_GROUP.get(slot.lower())
            if not g: continue
            for rank, a in enumerate(v.get("athletes", [])):
                d = best.setdefault(str(a.get("id")), {})
                d[g] = min(d.get(g, 99), rank)
    out = []
    for a in roster.get("athletes", []):
        pid = str(a["id"])
        p = primary_group(a.get("position", {}).get("abbreviation"))
        ranks = best.get(pid, {})
        sec = [(ranks[g], g) for g in ADJ[p] if ranks.get(g, 99) < DEPTH_TOP]
        pos = f"{p}-{min(sec)[1]}" if sec else p
        dob = (a.get("dateOfBirth") or "")[:10] or None
        jersey = a.get("jersey")
        out.append({
            "id": pid,
            "name": a.get("fullName") or a.get("displayName"),
            "firstName": a.get("firstName", ""),
            "lastName": a.get("lastName", ""),
            "teamId": tid,
            "position": pos,
            "birthDate": dob,
            "jersey": jersey if jersey not in (None, "") else None,
            "country": country_of(a.get("birthPlace")),
            "headshot": (a.get("headshot") or {}).get("href"),
        })
    return out

def stats():
    res = {}
    page = 1
    while True:
        d = get(f"https://site.web.api.espn.com/apis/common/v3/sports/basketball/nba/statistics/byathlete?limit=200&page={page}")
        names = {c["name"]: c["names"] for c in d.get("categories", [])}
        for row in d.get("athletes", []):
            vals = {}
            for c in row.get("categories", []):
                for n, v in zip(names.get(c["name"], []), c.get("totals", [])):
                    vals.setdefault(n, v)
            def f(k):
                try: return float(vals.get(k))
                except (TypeError, ValueError): return None
            res[str(row["athlete"]["id"])] = {"ppg": f("avgPoints"), "rpg": f("avgRebounds"), "apg": f("avgAssists"), "gp": int(f("gamesPlayed") or 0)}
        if page >= d.get("pagination", {}).get("pages", 1): break
        page += 1
    return res

def main():
    with cf.ThreadPoolExecutor(8) as ex:
        players = [p for lst in ex.map(team_data, ESPN_TEAM_IDS) for p in lst]
    st = stats()
    for p in players:
        p.update(st.get(p["id"], {"ppg": None, "rpg": None, "apg": None, "gp": 0}))
    players = [p for p in players if p["birthDate"]]
    players.sort(key=lambda p: p["id"])
    if len(players) < 300:
        sys.exit(f"only {len(players)} players — refusing to write")
    out = {"source": "ESPN public site API", "fetchedAt": datetime.now(timezone.utc).isoformat(timespec="seconds"), "players": players}
    path = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "assets", "players.json")
    with open(path, "w", encoding="utf-8") as fh:
        json.dump(out, fh, ensure_ascii=False, separators=(",", ":"))
    from collections import Counter
    print(len(players), "players;", Counter(p["position"] for p in players), "no jersey:", sum(1 for p in players if not p["jersey"]), "no country:", sum(1 for p in players if not p["country"]))

if __name__ == "__main__":
    main()
