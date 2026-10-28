"""Query OSV using only Maven coordinates/versions; never upload application files."""
import argparse
import concurrent.futures
import datetime
import json
import re
import sys
import urllib.request
from pathlib import Path

def request(url, body=None):
    encoded = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(url, encoded, headers={"Content-Type": "application/json", "User-Agent": "Coelho-Music-dependency-review/1.0"})
    with urllib.request.urlopen(req, timeout=60) as response:
        return json.load(response)

def main():
    sys.stdout.reconfigure(encoding="utf-8")
    parser = argparse.ArgumentParser()
    parser.add_argument("--before", action="store_true")
    args = parser.parse_args()
    directory = Path(__file__).resolve().parent.parent / "security/evidence"
    if args.before:
        modules = {}
        for line in (directory / "dependencies-before.txt").read_text(encoding="utf-8-sig").splitlines():
            found = re.search(r"([\w.-]+):([\w.-]+):([\w.-]+)(?: -> ([\w.-]+))?", line)
            if found:
                group, name, version, selected = found.groups()
                version = selected or version
                modules[(group, name, version)] = dict(group=group, name=name, version=version)
        dependencies = list(modules.values())
    else:
        dependencies = json.loads((directory / "dependencies.json").read_text())
    results = []
    for start in range(0, len(dependencies), 100):
        batch = dependencies[start:start+100]
        response = request("https://api.osv.dev/v1/querybatch", {"queries": [
            {"package": {"ecosystem": "Maven", "name": f'{item["group"]}:{item["name"]}'}, "version": item["version"]}
            for item in batch]})
        if len(response.get("results", [])) != len(batch):
            raise RuntimeError("Incomplete OSV response")
        results.extend(dict(dependency=dep, response=res) for dep, res in zip(batch, response["results"]))
    ids = sorted({v["id"] for r in results for v in r["response"].get("vulns", [])})
    with concurrent.futures.ThreadPoolExecutor(max_workers=4) as pool:
        details = list(pool.map(lambda vid: request("https://api.osv.dev/v1/vulns/" + vid), ids))
    report = dict(queried_at=datetime.datetime.now(datetime.timezone.utc).isoformat(), endpoint="https://api.osv.dev/v1/querybatch",
                  query_count=len(results), results=results, vulnerability_details=details,
                  limitation="An empty result means no indexed match returned, not proof of safety or complete ecosystem coverage.")
    filename = "osv-before.json" if args.before else "osv-after.json"
    (directory / filename).write_text(json.dumps(report, indent=2), encoding="utf-8")
    print(f"{len(results)} module versions queried; {len(ids)} advisory IDs returned. Evidence: {filename}")
    for item in details:
        print(item["id"], item.get("summary", ""), "withdrawn=" + str(item.get("withdrawn")))

if __name__ == "__main__":
    main()
