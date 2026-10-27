#!/usr/bin/env python3
"""
Builds the offline data bundled in the APK:
  app/src/main/assets/players.json  every active roster player (fallback when there is no network)
  app/src/main/assets/rings.json    ESPN ids of every player who won an NBA title since 2004

Data source: ESPN's public (free, key-less) API — the same source the app uses at runtime
(see data/EspnSource.kt):
  rosters     https://site.api.espn.com/apis/site/v2/sports/basketball/nba/teams/{id}/roster
  stats       https://site.web.api.espn.com/apis/common/v3/sports/basketball/nba/statistics/byathlete
  draft year  https://sports.core.api.espn.com/v2/sports/basketball/leagues/nba/athletes/{id}  ("draft"; absent = undrafted)
  champions   NBA Finals games from the daily scoreboard; ring holders = every player listed in the
              champion's Finals box scores (played or DNP).

Re-run after each NBA Finals (to add the new champions) and before a release:
  python3 tools/fetch_players.py
"""
import json, os, sys, datetime, urllib.request, concurrent.futures as cf
from datetime import timezone

ESPN_TEAM_IDS = [1,2,17,30,4,5,6,7,8,9,10,11,12,13,29,14,15,16,3,18,25,19,20,21,22,23,24,28,26,27]
SITE = "https://site.api.espn.com/apis/site/v2/sports/basketball/nba"
CORE = "https://sports.core.api.espn.com/v2/sports/basketball/leagues/nba"
ASSETS = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "assets")
FIRST_FINALS = 2004
# ESPN team id -> is Eastern Conference
EAST = {str(t): t in (2, 17, 18, 20, 28, 4, 5, 8, 11, 15, 1, 30, 14, 19, 27) for t in ESPN_TEAM_IDS}  # oldest active players debuted 2003

# Hand corrections, checked against Wikidata (linked by ESPN id) and the players' national teams.
# Written to assets/corrections.json, which the app also applies after every refresh, so a wrong
# ESPN value can never come back. "country" is the country the player represents.
CORRECTIONS = {
    # ESPN has no birthplace for these
    "4711294": {"country": "USA"},        # Matas Buzelis
    "4870562": {"country": "USA"},        # Dominick Barlow
    "4871145": {"country": "Australia"},  # Josh Giddey
    "4997528": {"country": "Serbia"},     # Nikola Jovic
    "4997538": {"country": "Croatia"},    # Karlo Matkovic
    "5104155": {"country": "France"},     # Bilal Coulibaly
    "5157587": {"country": "Australia"},  # Rocco Zikarsky
    "5159925": {"country": "Serbia"},     # Nikola Topic
    "5160992": {"country": "France"},     # Alex Sarr
    "5175647": {"country": "Spain"},      # Hugo Gonzalez
    "5211175": {"country": "France"},     # Zaccharie Risacher
    "5211176": {"country": "France"},     # Tidjane Salaun
    "5214637": {"country": "France"},     # Noah Penda
    "5214989": {"country": "Serbia"},     # Bogoljub Markovic
    "5217746": {"country": "China"},      # Yang Hansen
    "5242496": {"country": "France"},     # Noa Essengue
    "5242502": {"country": "Israel"},     # Ben Saraf
    "5279133": {"country": "France"},     # Joan Beringer
}

def load_corrections():
    """CORRECTIONS + the ones found by the Wikidata check, keyed by ESPN id."""
    out = {k: dict(v) for k, v in CORRECTIONS.items()}
    for k, v in WIKIDATA_FIXES.items():
        out.setdefault(k, {}).update(v)
    return out

# ESPN values that Wikidata (birth date, citizenship) and the national team contradict.
WIKIDATA_FIXES = {
    "4600663": {"birthDate": "2002-05-14"},                          # Zach Edey
    "4683860": {"birthDate": "2003-09-04"},                          # Quadir Copeland
    "5105337": {"birthDate": "2003-11-01"},                          # Maliq Brown
    "4431679": {"country": "Nigeria"},                               # Precious Achiuwa (born Port Harcourt)
    "4432827": {"country": "Democratic Republic of Congo"},          # Oscar Tshiebwe (born Lubumbashi)
    "6478":    {"country": "Montenegro"},                            # Nikola Vucevic (Montenegro national team)
    "4017837": {"country": "Croatia"},                               # Ivica Zubac (Croatia national team)
    "4997537": {"country": "Serbia"},                                # Tristan Vukcevic (Serbia national team)
}

def get(url):
    err = None
    for _ in range(3):
        try:
            req = urllib.request.Request(url, headers={"Accept": "application/json"})
            with urllib.request.urlopen(req, timeout=40) as r:
                return json.load(r)
        except Exception as e:
            err = e
    raise err

def position(abbr):
    a = (abbr or "").upper()
    if a in ("G", "PG", "SG"): return "G"
    if a == "C": return "C"
    return "F"

def country_of(bp):
    if not bp: return None
    c = bp.get("country")
    if c: return {"Türkiye": "Turkey"}.get(c, c)
    return "USA" if bp.get("state") else None

def team_players(tid):
    roster = get(f"{SITE}/teams/{tid}/roster")
    out = []
    for a in roster.get("athletes", []):
        dob = (a.get("dateOfBirth") or "")[:10] or None
        jersey = a.get("jersey")
        out.append({
            "id": str(a["id"]),
            "name": a.get("fullName") or a.get("displayName"),
            "firstName": a.get("firstName", ""),
            "lastName": a.get("lastName", ""),
            "teamId": tid,
            "position": position(a.get("position", {}).get("abbreviation")),
            "birthDate": dob,
            "jersey": jersey if jersey not in (None, "") else None,
            "country": country_of(a.get("birthPlace")),
            "headshot": (a.get("headshot") or {}).get("href"),
        })
    return out

def live_jerseys():
    """{player id: jersey} from each team's most recent game (what was actually worn).

    ESPN's player profiles lag behind number changes (Jamal Shead: profile 23, wears 1); the
    game's lineup is right. Scoreboards from today backwards until every team has a game (max 7 days).
    """
    latest, day = {}, datetime.date.today()
    for _ in range(7):
        if len(latest) >= 30: break
        for e in get(f"{SITE}/scoreboard?dates={day.strftime('%Y%m%d')}").get("events", []):
            if not e["status"]["type"].get("completed"): continue
            for c in e["competitions"][0]["competitors"]:
                latest.setdefault(c["team"]["id"], e["id"])
        day -= datetime.timedelta(1)
    def lineup(item):
        team, eid = item
        r = get(f"{CORE}/events/{eid}/competitions/{eid}/competitors/{team}/roster")
        return {(team, str(x["playerId"])): x.get("jersey") for x in r.get("entries", []) if x.get("jersey")}
    out = {}
    with cf.ThreadPoolExecutor(8) as ex:
        for m in ex.map(lineup, latest.items()): out.update(m)
    return out

def draft_year(pid):
    d = get(f"{CORE}/athletes/{pid}").get("draft")
    return d.get("year") if d else None

def stats():
    res, page = {}, 1
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

def playoff_games_on(day):
    out = []
    for e in get(f"{SITE}/scoreboard?dates={day}").get("events", []):
        if e.get("season", {}).get("type") != 3 or not e["status"]["type"].get("completed"):
            continue
        comp = e["competitions"][0]
        teams = frozenset(c["team"]["id"] for c in comp["competitors"])
        winner = next((c["team"]["id"] for c in comp["competitors"] if c.get("winner")), None)
        out.append((e["date"], e["id"], teams, winner))
    return out

def ring_holders():
    """{season year: [player ids]} for each champion since FIRST_FINALS.

    The Finals are the two teams of the season's last playoff game; East and West teams only ever
    meet in the Finals, so every playoff game between them is a Finals game. The champion is the
    winner of the last one.
    """
    today = datetime.date.today()
    days = []
    for y in range(FIRST_FINALS, today.year + 1):
        start, end = datetime.date(y, 5, 25), datetime.date(y, 6, 30)
        if y == 2020: start, end = datetime.date(2020, 9, 25), datetime.date(2020, 10, 15)  # bubble Finals
        if y == 2021: end = datetime.date(2021, 7, 25)                                        # delayed season
        d = start
        while d <= min(end, today):
            days.append(d.strftime("%Y%m%d")); d += datetime.timedelta(1)
    with cf.ThreadPoolExecutor(12) as ex:
        games = sorted((g for r in ex.map(playoff_games_on, days) for g in r), key=lambda g: g[0])
    by_year = {}
    for g in games: by_year.setdefault(g[0][:4], []).append(g)
    rings = {}
    for year, gs in by_year.items():
        finalists = gs[-1][2]
        finals = [g for g in gs if g[2] == finalists]
        if len({EAST.get(t) for t in finalists}) != 2 or len(finals) < 4:
            continue  # Finals not reached yet
        champ = finals[-1][3]
        ids = set()
        for _, eid, _, _ in finals:
            box = get(f"{SITE}/summary?event={eid}").get("boxscore", {})
            for t in box.get("players", []):
                if t["team"]["id"] == champ:
                    ids |= {str(a["athlete"]["id"]) for a in t["statistics"][0].get("athletes", [])}
        rings[year] = sorted(ids)
        print(f"  {year}: team {champ}, {len(finals)} games, {len(ids)} players", file=sys.stderr)
    return rings

def main():
    with cf.ThreadPoolExecutor(8) as ex:
        players = [p for lst in ex.map(team_players, ESPN_TEAM_IDS) for p in lst]
    players = [p for p in players if p["birthDate"]]
    with cf.ThreadPoolExecutor(12) as ex:
        drafts = dict(zip([p["id"] for p in players], ex.map(draft_year, [p["id"] for p in players])))
    st = stats()
    worn = live_jerseys()
    fixes = load_corrections()
    changed = []
    for p in players:
        j = worn.get((str(p["teamId"]), p["id"]))
        if j and j != p["jersey"]:
            changed.append(f'{p["name"]}: {p["jersey"]} -> {j}')
            p["jersey"] = j
        for k, v in fixes.get(p["id"], {}).items():
            if p.get(k) != v: changed.append(f'{p["name"]}: {k} {p.get(k)} -> {v}')
            p[k] = v
    print("corrected:", *changed, sep="\n  ", file=sys.stderr)
    rings = ring_holders()
    ring_ids = {i for ids in rings.values() for i in ids}
    for p in players:
        p["draftYear"] = drafts[p["id"]]
        p["ring"] = p["id"] in ring_ids
        p.update(st.get(p["id"], {"ppg": None, "rpg": None, "apg": None, "gp": 0}))
    players.sort(key=lambda p: p["id"])
    if len(players) < 300:
        sys.exit(f"only {len(players)} players — refusing to write")
    now = datetime.datetime.now(timezone.utc).isoformat(timespec="seconds")
    with open(os.path.join(ASSETS, "players.json"), "w", encoding="utf-8") as fh:
        json.dump({"schema": 4, "source": "ESPN public API", "fetchedAt": now, "players": players}, fh, ensure_ascii=False, separators=(",", ":"))
    with open(os.path.join(ASSETS, "corrections.json"), "w", encoding="utf-8") as fh:
        json.dump(fixes, fh, ensure_ascii=False, indent=1, sort_keys=True)
    with open(os.path.join(ASSETS, "rings.json"), "w", encoding="utf-8") as fh:
        json.dump({"fetchedAt": now, "champions": rings}, fh, separators=(",", ":"))
    from collections import Counter
    print(len(players), "players;", Counter(p["position"] for p in players),
          "undrafted:", sum(1 for p in players if p["draftYear"] is None),
          "with ring:", sum(1 for p in players if p["ring"]))

if __name__ == "__main__":
    main()
