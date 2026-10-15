"""Render app/build/anim/frames.json (from MotionsTest.dumpFrames) into PNG contact sheets."""
import json, sys
from PIL import Image, ImageDraw

src = sys.argv[1] if len(sys.argv) > 1 else "app/build/anim/frames.json"
out = sys.argv[2] if len(sys.argv) > 2 else "app/build/anim"
data = json.load(open(src))
CELL, PH, PER_ROW, ROWS = 150, 4, 2, 8
NEAR, FAR = (30, 110, 76), (150, 195, 172)

for sheet in range(0, len(data), PER_ROW * ROWS):
    chunk = data[sheet:sheet + PER_ROW * ROWS]
    img = Image.new("RGB", (CELL * PH * PER_ROW, CELL * ROWS), "white")
    d = ImageDraw.Draw(img)
    for i, m in enumerate(chunk):
        minx, maxx, miny, maxy = m["b"]
        ua, fa, th, sh, to = m["w"]
        ox0, oy0 = (i % PER_ROW) * CELL * PH, (i // PER_ROW) * CELL
        for ph, f in enumerate(m["frames"]):
            w, h = CELL, CELL - 16
            sc = min(w * 0.9 / (maxx - minx), h * 0.88 / (maxy - miny))
            gx = ox0 + ph * CELL + w / 2 - sc * (minx + maxx) / 2
            gy = oy0 + 16 + h * 0.94 - sc * maxy
            P = lambda p: (gx + p[0] * sc, gy + p[1] * sc)
            def line(a, b, wd, col):
                r = wd * sc / 2
                d.line([P(a), P(b)], fill=col, width=max(1, int(wd * sc)))
                for q in (P(a), P(b)):
                    d.ellipse([q[0] - r, q[1] - r, q[0] + r, q[1] + r], fill=col)
            d.line([(ox0 + ph * CELL + 4, gy), (ox0 + (ph + 1) * CELL - 4, gy)], fill=(210, 210, 210), width=2)
            if "bench" in m:
                bx, bw, bh = m["bench"]
                d.rectangle([gx + bx * sc, gy - bh * sc, gx + (bx + bw) * sc, gy], fill=(205, 205, 205))
            if "bar" in m:
                bx, by = m["bar"]
                d.line([P((bx - 12, by)), P((bx + 12, by))], fill=(120, 120, 120), width=3)
            if "wall" in m:
                d.line([P((m["wall"], 0)), P((m["wall"], -85))], fill=(190, 190, 190), width=3)
            hip, hip1, hip2, neck, sh1, sh2, head, el1, ha1, el2, ha2, kn1, an1, kn2, an2 = f
            c2 = NEAR if m["front"] else FAR
            line(sh2, el2, ua, c2); line(el2, ha2, fa, c2)
            line(hip2, kn2, th, c2); line(kn2, an2, sh, c2)
            line(hip, neck, to, NEAR)
            hx, hy = P(head); r = 6.5 * sc
            d.ellipse([hx - r, hy - r, hx + r, hy + r], fill=NEAR)
            line(hip1, kn1, th, NEAR); line(kn1, an1, sh, NEAR)
            line(sh1, el1, ua, NEAR); line(el1, ha1, fa, NEAR)
        d.text((ox0 + 4, oy0 + 2), m["id"], fill="black")
        d.rectangle([ox0, oy0, ox0 + CELL * PH - 1, oy0 + CELL - 1], outline=(225, 225, 225))
    img.save(f"{out}/sheet-{sheet // (PER_ROW * ROWS)}.png")
print("ok", len(data))
