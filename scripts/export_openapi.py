"""Export the running application's OpenAPI contract as a reviewable JSON file."""
import argparse
import json
from pathlib import Path
from urllib.request import urlopen

parser = argparse.ArgumentParser()
parser.add_argument("--base-url", default="http://localhost:8080")
parser.add_argument("--output", type=Path, default=Path("docs/openapi.json"))
args = parser.parse_args()
with urlopen(args.base_url.rstrip("/") + "/v3/api-docs", timeout=30) as response:
    spec = json.load(response)
assert spec["openapi"].startswith("3.")
spec["servers"] = [{"url": "http://localhost:8080", "description": "Local / Docker"}]
args.output.parent.mkdir(parents=True, exist_ok=True)
args.output.write_text(json.dumps(spec, indent=2, ensure_ascii=False, sort_keys=True) + "\n", encoding="utf-8")
print(args.output)
