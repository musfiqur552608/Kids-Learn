#!/usr/bin/env python3
"""
Generates the two Lottie animations bundled in app/src/main/res/raw.

Hand-authoring Lottie JSON is error-prone (Bezier easing handles, layer ordering,
shape-group nesting). Generating it keeps the motion maths reviewable and lets us
tune the "feel" in one place.

Animations produced:
  * confetti.json   - celebration burst when a module / lesson is finished
  * star_burst.json - small pop when a single star is earned

Run:  python3 tools/generate_lottie.py
Both are pure vector shape animations: no images, no network, tiny payload.
"""
import json
import os
import random

OUT_DIR = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "res", "raw")

# Kid-friendly party palette (matches ui/theme/Color.kt PartyPalette).
COLORS = [
    (1.000, 0.420, 0.420),  # coral
    (1.000, 0.710, 0.290),  # tangerine
    (1.000, 0.890, 0.310),  # sunshine
    (0.400, 0.820, 0.420),  # grass
    (0.290, 0.710, 0.910),  # sky
    (0.580, 0.490, 0.910),  # grape
    (0.980, 0.510, 0.780),  # bubblegum
]


def rgba(c, alpha=1.0):
    return {"a": 0, "k": [c[0], c[1], c[2], alpha]}


def transform(pos=(0, 0), scale=100, rotation=0, opacity=100, anchor=(0, 0)):
    return {
        "ty": "tr",
        "p": {"a": 0, "k": list(pos)},
        "a": {"a": 0, "k": list(anchor)},
        "s": {"a": 0, "k": [scale, scale]},
        "r": {"a": 0, "k": rotation},
        "o": {"a": 0, "k": opacity},
    }


def fill(color):
    return {"ty": "fl", "c": rgba(color), "o": {"a": 0, "k": 100}, "r": 1, "bm": 0, "nm": "fill"}


def rect_group(color, w, h, radius=3, rotation=0):
    """One rectangular confetti chip, wrapped in a shape group."""
    return {
        "ty": "gr",
        "nm": "chip",
        "it": [
            {"ty": "rc", "d": 1, "s": {"a": 0, "k": [w, h]},
             "p": {"a": 0, "k": [0, 0]}, "r": {"a": 0, "k": radius}, "nm": "rect"},
            fill(color),
            transform(rotation=rotation),
        ],
        "bm": 0,
    }


def star_group(color, size):
    """A 5-pointed star via a bezier path."""
    outer = size
    inner = size * 0.42
    k = 0.5523  # circle-to-bezier constant, gives near-circular control points
    pts = []
    import math
    for i in range(10):
        r = outer if i % 2 == 0 else inner
        a = -math.pi / 2 + i * math.pi / 5
        pts.append((r * math.cos(a), r * math.sin(a)))
    vertices, in_vec, out_vec = [], [], []
    n = len(pts)
    for i in range(n):
        px, py = pts[i]
        vx, vy = pts[(i + 1) % n]
        vertices.append([round(px, 2), round(py, 2)])
        out_vec.append([round(px + (vx - px) * k, 2), round(py + (vy - py) * k, 2)])
        bx, by = pts[(i - 1) % n]
        in_vec.append([round(px + (bx - px) * k, 2), round(py + (by - py) * k, 2)])
    return {
        "ty": "gr",
        "nm": "star",
        "it": [
            {"ty": "sh", "d": 1, "ks": {"a": 0, "k": {
                "c": True, "v": vertices, "i": in_vec, "o": out_vec}},
             "nm": "star-path"},
            fill(color),
            transform(),
        ],
        "bm": 0,
    }


def kf(frames_values, ease_in=(0.35, 1.0), ease_out=(0.65, 0.0)):
    """Builds an Lottie animated property from (frame, [values]) pairs.

    `ease_out` is the outgoing bezier of keyframe N (how it leaves),
    `ease_in` is the incoming bezier of keyframe N+1 (how it arrives).
    Gravity reads as a slow start then an accelerating fall.
    """
    keys = []
    for idx, (t, v) in enumerate(frames_values):
        if idx == len(frames_values) - 1:
            keys.append({"t": t, "s": v})
        else:
            keys.append({
                "t": t,
                "s": v,
                "o": {"x": [ease_out[0]], "y": [ease_out[1]]},
                "i": {"x": [ease_in[0]], "y": [ease_in[1]]},
            })
    return {"a": 1, "k": keys}


def static(v):
    return {"a": 0, "k": v}


def layer(ind, name, shapes, ks, op):
    return {
        "ddd": 0, "ind": ind, "ty": 4, "nm": name, "sr": 1,
        "ks": ks, "ao": 0, "shapes": shapes,
        "ip": 0, "op": op, "st": 0, "bm": 0,
    }


def build_confetti(seed=7, width=400, height=400, count=26, frames=100):
    rng = random.Random(seed)
    layers = []
    for i in range(count):
        color = COLORS[i % len(COLORS)]
        # Start spread across the top, staggered so the burst feels organic.
        start_x = rng.uniform(30, width - 30)
        start_frame = rng.randint(0, 18)
        drift = rng.uniform(-70, 70)
        spin = rng.uniform(-540, 540)
        chip_w = rng.uniform(9, 15)
        chip_h = rng.uniform(13, 22)
        end_frame = start_frame + int((frames - start_frame) * rng.uniform(0.72, 1.0))

        mid_frame = start_frame + int((end_frame - start_frame) * 0.45)
        # Quadratic-ish path: straight-ish drop, then a stronger final fall.
        y0, y1, y2 = -30, height * 0.52, height + 40
        x0, x1, x2 = start_x, start_x + drift * 0.35, start_x + drift

        pos = kf([
            (start_frame, [round(x0, 1), round(y0, 1), 0]),
            (mid_frame, [round(x1, 1), round(y1, 1), 0]),
            (end_frame, [round(x2, 1), round(y2, 1), 0]),
        ])
        rot = kf([(start_frame, [0]), (end_frame, [round(spin, 1)])])
        # Fade in fast, hold, fade out at the very end.
        op_anim = kf([
            (start_frame, [0]),
            (start_frame + 4, [100]),
            (max(start_frame + 5, end_frame - 14), [100]),
            (end_frame, [0]),
        ], ease_in=(0.3, 0.0), ease_out=(0.7, 1.0))

        layers.append(layer(
            i + 1, f"confetti-{i}",
            [rect_group(color, round(chip_w, 1), round(chip_h, 1),
                        radius=2, rotation=rng.randint(-25, 25))],
            {
                "o": op_anim,
                "r": rot,
                "p": pos,
                "a": static([0, 0, 0]),
                "s": static([100, 100, 100]),
            },
            op=frames,
        ))
    return {
        "v": "5.9.0", "fr": 60, "ip": 0, "op": frames,
        "w": width, "h": height, "nm": "confetti", "ddd": 0,
        "assets": [], "layers": layers,
        "markers": [],
    }


def build_star_burst(seed=11, size=200, count=9):
    rng = random.Random(seed)
    layers = []
    frames = 46
    for i in range(count):
        color = COLORS[i % len(COLORS)]
        angle = 2 * math.pi * i / count
        dist = 62
        start_frame = rng.randint(0, 5)
        end_frame = start_frame + rng.randint(26, 34)
        x0, y0 = size / 2, size / 2
        x1 = x0 + dist * math.cos(angle)
        y1 = y0 + dist * math.sin(angle)
        scale = kf([
            (start_frame, [0, 0]),
            (end_frame - 8, [100, 100]),
            (end_frame, [40, 40]),
        ])
        pos = kf([(start_frame, [round(x0, 1), round(y0, 1), 0]),
                  (end_frame, [round(x1, 1), round(y1, 1), 0])])
        op_anim = kf([
            (start_frame, [0]),
            (start_frame + 3, [100]),
            (end_frame - 10, [100]),
            (end_frame, [0]),
        ], ease_in=(0.3, 0.0), ease_out=(0.7, 1.0))
        layers.append(layer(
            i + 1, f"ray-{i}",
            [star_group(color, 7.0)],
            {"o": op_anim, "r": static(0), "p": pos,
             "a": static([0, 0, 0]), "s": scale},
            op=frames,
        ))
    return {
        "v": "5.9.0", "fr": 60, "ip": 0, "op": frames,
        "w": size, "h": size, "nm": "star_burst", "ddd": 0,
        "assets": [], "layers": layers, "markers": [],
    }


import math  # noqa: E402  (used inside build_star_burst)


def main():
    os.makedirs(OUT_DIR, exist_ok=True)
    for name, doc in (("confetti.json", build_confetti()),
                      ("star_burst.json", build_star_burst())):
        path = os.path.join(OUT_DIR, name)
        with open(path, "w", encoding="utf-8") as f:
            json.dump(doc, f, separators=(",", ":"))
        print(f"  {name:20s} {os.path.getsize(path):>6d} bytes, "
              f"{len(doc['layers'])} layers")


if __name__ == "__main__":
    main()
