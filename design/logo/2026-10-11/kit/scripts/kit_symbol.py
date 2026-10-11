import os, sys

OUT = sys.argv[1]
INK, LIME, WHITE, BLACK = "#0A0A0A", "#A3F43F", "#FFFFFF", "#000000"
TILE_FIT = ' transform="translate(128 128) scale(0.7) translate(-128 -128)"'


def pill(cx, cy, w, h):
    r, top, bot = w / 2, cy - h / 2 + w / 2, cy + h / 2 - w / 2
    return (f"M{cx - r:g} {top:g}A{r:g} {r:g} 0 0 1 {cx + r:g} {top:g}V{bot:g}"
            f"A{r:g} {r:g} 0 0 1 {cx - r:g} {bot:g}Z")


def paige_paths(gap=12, rf=8, eye_w=24, eye_h=40, eye_dx=24, eye_y=114, mouth_y=150, mouth_r=22):
    fx, fy = 166, 70
    page = (f"M78 34H{fx - gap}V{fy - rf}A{rf + gap} {rf + gap} 0 0 0 {fx + rf} {fy + gap}H202V198"
            f"A24 24 0 0 1 178 222H78A24 24 0 0 1 54 198V58A24 24 0 0 1 78 34Z")
    eyes = pill(128 - eye_dx, eye_y, eye_w, eye_h) + pill(128 + eye_dx, eye_y, eye_w, eye_h)
    mouth = f"M{128 - mouth_r} {mouth_y}H{128 + mouth_r}A{mouth_r} {mouth_r} 0 0 1 {128 - mouth_r} {mouth_y}Z"
    flap = f"M{fx} 34L202 {fy}H{fx + rf}A{rf} {rf} 0 0 1 {fx} {fy - rf}Z"
    return page + eyes + mouth, flap


CUTS = {
    "paige-symbol": {},
    "paige-symbol-small": dict(gap=18, rf=6, eye_w=28, eye_h=44, eye_dx=26, eye_y=116, mouth_y=154, mouth_r=24),
    "paige-symbol-reversed": dict(gap=14, eye_w=26, eye_h=42, mouth_r=23),
}


def symbol_svg(name, fill, bg=None, **cut):
    body, flap = paige_paths(**cut)
    tile = f'<rect width="256" height="256" rx="56" fill="{bg}"/>' if bg else ""
    return (f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 256 256"><title>TailorMyResume</title>{tile}'
            f'<g{TILE_FIT if bg else ""}><path fill="{fill}" fill-rule="evenodd" d="{body}"/><path fill="{fill}" d="{flap}"/></g></svg>\n')


if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    variants = [
        ("paige-symbol", "paige-symbol", INK, None),
        ("paige-symbol-black", "paige-symbol", BLACK, None),
        ("paige-symbol-small", "paige-symbol-small", INK, None),
        ("paige-symbol-reversed", "paige-symbol-reversed", WHITE, None),
        ("paige-symbol-lime-on-ink", "paige-symbol-reversed", LIME, INK),
        ("paige-icon-ink-on-lime", "paige-symbol", INK, LIME),
    ]
    for file, cut, fill, bg in variants:
        with open(os.path.join(OUT, file + ".svg"), "w") as fh:
            fh.write(symbol_svg(file, fill, bg, **CUTS[cut]))
    print("wrote", len(variants))
