"""
image_svg_tool.py

Two operations:
  1. revert_assets  – Extract the base64-encoded raster images back out of the
                      assets/*.svg wrappers and restore the original PNG/JPG files.
  2. sprites_to_svg – Wrap every PNG inside sprites/front/ and sprites/back/
                      as a base64-embedded SVG (originals deleted).

Usage (run from project root):
    python convert_assets_to_svg.py revert_assets
    python convert_assets_to_svg.py sprites_to_svg

Dependencies: Pillow  (pip install Pillow)
"""

import base64
import re
import struct
import sys
from pathlib import Path

SCRIPT_DIR  = Path(__file__).resolve().parent
RES_BASE    = SCRIPT_DIR / "src/main/resources/com/example/pokemonbattle"
ASSETS_DIR  = RES_BASE / "assets"
SPRITE_DIRS = [
    RES_BASE / "sprites/front",
    RES_BASE / "sprites/back",
]

try:
    from PIL import Image as PILImage
    HAS_PILLOW = True
except ImportError:
    HAS_PILLOW = False


# ── shared helpers ────────────────────────────────────────────────────────────

def _png_dimensions(path: Path):
    with open(path, "rb") as f:
        if f.read(8) != b"\x89PNG\r\n\x1a\n":
            return None, None
        f.read(8)  # length + "IHDR"
        w = struct.unpack(">I", f.read(4))[0]
        h = struct.unpack(">I", f.read(4))[0]
    return w, h


def get_dimensions(path: Path):
    if HAS_PILLOW:
        try:
            with PILImage.open(path) as img:
                return img.size
        except Exception:
            pass
    if path.suffix.lower() == ".png":
        return _png_dimensions(path)
    return None, None


def raster_to_svg(src: Path) -> Path:
    """Wrap *src* (PNG/JPG) as a base64-embedded SVG alongside it; return the SVG path."""
    w, h = get_dimensions(src)
    mime = "image/png" if src.suffix.lower() == ".png" else "image/jpeg"

    with open(src, "rb") as f:
        b64 = base64.b64encode(f.read()).decode("ascii")

    if w and h:
        dim   = f'width="{w}" height="{h}"'
        vb    = f'viewBox="0 0 {w} {h}"'
    else:
        dim   = 'width="100%" height="100%"'
        vb    = ''

    svg = (
        f'<svg xmlns="http://www.w3.org/2000/svg" '
        f'xmlns:xlink="http://www.w3.org/1999/xlink" '
        f'{dim} {vb} preserveAspectRatio="none">\n'
        f'  <image {dim} href="data:{mime};base64,{b64}" preserveAspectRatio="none"/>\n'
        f'</svg>\n'
    )
    dest = src.with_suffix(".svg")
    dest.write_text(svg, encoding="utf-8")
    return dest


# ── operation 1 : revert assets ───────────────────────────────────────────────

_HREF_RE = re.compile(r'href="data:(image/(?:png|jpeg));base64,([A-Za-z0-9+/=]+)"')
_EXT_MAP  = {"image/png": ".png", "image/jpeg": ".jpg"}


def svg_to_raster(svg_path: Path) -> Path:
    """Extract the embedded raster bytes from *svg_path*, write the original
    file (PNG/JPG) next to it and return the restored path."""
    content = svg_path.read_text(encoding="utf-8")
    m = _HREF_RE.search(content)
    if not m:
        raise ValueError("No base64 image href found in SVG")
    mime, b64 = m.group(1), m.group(2)
    ext  = _EXT_MAP[mime]
    dest = svg_path.with_suffix(ext)
    dest.write_bytes(base64.b64decode(b64))
    return dest


def revert_assets():
    print("── Reverting assets SVGs → original rasters ──────────────────────\n")
    svgs = sorted(ASSETS_DIR.glob("*.svg"))
    if not svgs:
        print("[INFO] No SVG files found in assets – nothing to revert.")
        return
    ok, fail = [], []
    for svg in svgs:
        print(f"  Extracting  {svg.name:<45}", end="")
        try:
            restored = svg_to_raster(svg)
            svg.unlink()
            print(f"→  {restored.name}")
            ok.append(restored.name)
        except Exception as exc:
            print(f"  FAILED: {exc}")
            fail.append(svg.name)
    print(f"\n✓ Restored {len(ok)} file(s).")
    if fail:
        print(f"✗ Failed   {len(fail)} file(s): {fail}")


# ── operation 2 : sprites → SVG ───────────────────────────────────────────────

def sprites_to_svg():
    print("── Converting sprite PNGs → SVG ──────────────────────────────────\n")
    total_ok, total_fail = 0, 0
    for sdir in SPRITE_DIRS:
        pngs = sorted(sdir.glob("*.png"))
        print(f"  [{sdir.name}/]  {len(pngs)} PNG(s) found")
        for src in pngs:
            print(f"    {src.name:<20}", end="")
            try:
                raster_to_svg(src)
                src.unlink()
                print(f"→  {src.stem}.svg")
                total_ok += 1
            except Exception as exc:
                print(f"  FAILED: {exc}")
                total_fail += 1
    print(f"\n✓ Converted {total_ok} sprite(s).")
    if total_fail:
        print(f"✗ Failed    {total_fail} sprite(s).")


def revert_sprites():
    print("── Reverting sprite SVGs → original PNGs ─────────────────────────\n")
    total_ok, total_fail = 0, 0
    for sdir in SPRITE_DIRS:
        svgs = sorted(sdir.glob("*.svg"))
        print(f"  [{sdir.name}/]  {len(svgs)} SVG(s) found")
        for svg in svgs:
            print(f"    {svg.name:<20}", end="")
            try:
                restored = svg_to_raster(svg)
                svg.unlink()
                print(f"→  {restored.name}")
                total_ok += 1
            except Exception as exc:
                print(f"  FAILED: {exc}")
                total_fail += 1
    print(f"\n✓ Restored {total_ok} sprite(s).")
    if total_fail:
        print(f"✗ Failed    {total_fail} sprite(s).")


# ── entry point ───────────────────────────────────────────────────────────────

def main():
    cmds = {"revert_assets": revert_assets, "sprites_to_svg": sprites_to_svg, "revert_sprites": revert_sprites}
    args = sys.argv[1:]
    if not args or args[0] not in cmds:
        print(f"Usage: python {Path(__file__).name} <command>")
        print(f"Commands: {', '.join(cmds)}")
        sys.exit(1)
    cmds[args[0]]()


if __name__ == "__main__":
    main()
