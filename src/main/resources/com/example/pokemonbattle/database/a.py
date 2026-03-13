import json
import os
import requests
import time

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
INPUT_FILE = os.path.join(BASE_DIR, "moves_gen4.json")
OUTPUT_FILE = os.path.join(BASE_DIR, "moves_gen4_with_desc.json")

MOVE_URL = "https://pokeapi.co/api/v2/move/{}/"


def get_move_flavor(session, move_name, retries=3):
    url = MOVE_URL.format(move_name)
    for attempt in range(retries):
        try:
            r = session.get(url, timeout=10)
            if r.status_code != 200:
                return move_name, "No description available."
            data = r.json()
            flavor = "No description available."
            for entry in data["flavor_text_entries"]:
                if entry["language"]["name"] == "en":
                    flavor = entry["flavor_text"].replace("\n", " ").replace("\f", " ")
                    break
            return move_name, flavor
        except requests.RequestException as e:
            if attempt < retries - 1:
                time.sleep(1)
            else:
                print(f"  Failed move {move_name} after {retries} attempts: {e}")
                return move_name, "No description available."


def get_move_flavors_for_names(move_names):
    move_flavors = {}
    session = requests.Session()
    total = len(move_names)
    for i, move_name in enumerate(move_names, 1):
        name, flavor = get_move_flavor(session, move_name)
        move_flavors[name] = flavor
        print(f"Fetched move {move_name} ({i}/{total})")
        time.sleep(0.05)
    session.close()
    return move_flavors


# load your existing JSON
with open(INPUT_FILE, "r", encoding="utf-8") as f:
    moves = json.load(f)

move_names = [move["name"] for move in moves]
print(f"Fetching {len(move_names)} move descriptions...")

# download descriptions only for moves in the gen4 list
move_flavor_dict = get_move_flavors_for_names(move_names)


# add description field
for move in moves:

    power = move["power"] if move["power"] else "-"
    accuracy = move["accuracy"] if move["accuracy"] else "-"
    pp = move["pp"]

    flavor = move_flavor_dict.get(move["name"], "No description available.")

    move["description"] = f"Power: {power}\nAccuracy: {accuracy}\nPP: {pp}\n\n{flavor}"


# save new file
with open(OUTPUT_FILE, "w", encoding="utf-8") as f:
    json.dump(moves, f, indent=2)

print("Done! File saved as", OUTPUT_FILE)