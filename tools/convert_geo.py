#!/usr/bin/env python3
"""Convert the Skyfront Shahed Bedrock/GeckoLib geo model into a vanilla 1.21.11 item model.

The vanilla element format supports euler rotation applied as rotationZYX, exactly like
GeckoLib/Bedrock cube rotation, so cube rotations are copied 1:1.
The model is scaled by SCALE (to fit the -16..32 element bounds) and centred on (8, 8, 8);
the mod scales it back up with the display transformation.

usage: python3 tools/convert_geo.py
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
GEO = ROOT / "tools/source/shahed.geo.json"
OUT = ROOT / "src/main/resources/assets/shahedkill/models/item/shahed.json"
TEX_W = TEX_H = 128
SCALE = 0.5
CENTER = (0.0, 3.0, -8.0)  # centre of the model in Bedrock units -> (8, 8, 8) in the vanilla model
FACES = ("north", "east", "south", "west", "up", "down")


def tr(p):
    return [round((p[i] - CENTER[i]) * SCALE + 8.0, 4) for i in range(3)]


def uv_rect(f):
    u, v = f["uv"]
    w, h = f["uv_size"]
    k = 16.0 / TEX_W
    return [round(u * k, 4), round(v * k, 4), round((u + w) * k, 4), round((v + h) * k, 4)]


def main():
    geo = json.loads(GEO.read_text())["minecraft:geometry"][0]
    elements = []
    for bone in geo["bones"]:
        for c in bone.get("cubes", []):
            o, s = c["origin"], c["size"]
            frm = tr(o)
            to = tr([o[0] + s[0], o[1] + s[1], o[2] + s[2]])
            faces = {}
            uv = c.get("uv")
            if isinstance(uv, dict):
                for name in FACES:
                    if name in uv:
                        faces[name] = {"uv": uv_rect(uv[name]), "texture": "#0"}
            if not faces:
                continue
            el = {"name": bone["name"], "from": frm, "to": to, "faces": faces}
            rot = c.get("rotation")
            if rot and any(abs(a) > 1e-6 for a in rot):
                el["rotation"] = {"origin": tr(c.get("pivot", bone.get("pivot", [0, 0, 0]))),
                                  "x": rot[0], "y": rot[1], "z": rot[2]}
            elements.append(el)
    for el in elements:
        for p in (el["from"], el["to"]):
            assert all(-16 <= a <= 32 for a in p), (el["name"], p)
    model = {"textures": {"0": "shahedkill:item/shahed", "particle": "shahedkill:item/shahed"},
             "elements": elements}
    OUT.write_text(json.dumps(model, indent=1))
    print(f"wrote {OUT} with {len(elements)} elements")


if __name__ == "__main__":
    main()
