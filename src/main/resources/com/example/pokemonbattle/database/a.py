import json
import requests
import time

INPUT_FILE = "moves_gen4.json"
OUTPUT_FILE = "moves_gen4_with_desc.json"

MOVE_LIST_URL = "https://pokeapi.co/api/v2/move?limit=1000"


def get_all_move_flavors():
    move_flavors = {}

    print("Fetching move list...")
    r = requests.get(MOVE_LIST_URL)
    moves = r.json()["results"]

    for m in moves:
        url = m["url"]
        r2 = requests.get(url)

        if r2.status_code != 200:
            continue

        data = r2.json()

        move_id = data["id"]

        flavor = "No description available."

        for entry in data["flavor_text_entries"]:
            if entry["language"]["name"] == "en":
                flavor = entry["flavor_text"].replace("\n", " ").replace("\f", " ")
                break

        move_flavors[move_id] = flavor

        print("Fetched move", move_id)

        time.sleep(0.05)

    return move_flavors


# download all move descriptions
move_flavor_dict = get_all_move_flavors()


# load your existing JSON
with open(INPUT_FILE, "r", encoding="utf-8") as f:
    moves = json.load(f)


# add description field
for move in moves:

    power = move["power"] if move["power"] else "-"
    accuracy = move["accuracy"] if move["accuracy"] else "-"
    pp = move["pp"]

    flavor = move_flavor_dict.get(move["id"], "No description available.")

    move["description"] = f"Power: {power}\nAccuracy: {accuracy}\nPP: {pp}\n\n{flavor}"


# save new file
with open(OUTPUT_FILE, "w", encoding="utf-8") as f:
    json.dump(moves, f, indent=2)

print("Done! File saved as", OUTPUT_FILE)