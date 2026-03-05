import json
import time
import urllib.request

OUTPUT_FILE = "pokemon_heights.json"
TOTAL = 493
DELAY = 0.2  # seconds between requests — be polite to the API

data = {}

for pokemon_id in range(1, TOTAL + 1):
    url = f"https://pokeapi.co/api/v2/pokemon/{pokemon_id}"
    req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0"})
    try:
        with urllib.request.urlopen(req, timeout=10) as response:
            pokemon = json.loads(response.read().decode())
            name   = pokemon["name"]
            height = pokemon["height"] / 10  # decimeters → meters
            data[pokemon_id] = {
                "id":     pokemon_id,
                "name":   name,
                "height": height  # in meters
            }
            print(f"[{pokemon_id:>3}/493] {name:<20} {height} m")
    except Exception as e:
        print(f"[{pokemon_id:>3}/493] ERROR — {e}")
        data[pokemon_id] = {
            "id":     pokemon_id,
            "name":   "unknown",
            "height": None
        }

    time.sleep(DELAY)

with open(OUTPUT_FILE, "w", encoding="utf-8") as f:
    json.dump(data, f, indent=2, ensure_ascii=False)

print(f"\nDone! Saved to {OUTPUT_FILE}")