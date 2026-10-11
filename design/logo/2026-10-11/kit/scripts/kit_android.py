import os, sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from kit_symbol import paige_paths, CUTS, INK, LIME

KIT = sys.argv[1]
SCALE = 50 / 188
SHIFT = 54 - 128 * SCALE


def vector(fill, body, flap):
    return f'''<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <group
        android:scaleX="{SCALE:.5f}"
        android:scaleY="{SCALE:.5f}"
        android:translateX="{SHIFT:.4f}"
        android:translateY="{SHIFT:.4f}">
        <path
            android:fillColor="{fill}"
            android:fillType="evenOdd"
            android:pathData="{body}" />
        <path
            android:fillColor="{fill}"
            android:pathData="{flap}" />
    </group>
</vector>
'''


def play_svg(body, flap, side=512):
    s = side * SCALE / 108
    t = side / 2 - 128 * s
    return (f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {side} {side}"><rect width="{side}" height="{side}" fill="{LIME}"/>'
            f'<g transform="translate({t:.3f} {t:.3f}) scale({s:.5f})"><path fill="{INK}" fill-rule="evenodd" d="{body}"/>'
            f'<path fill="{INK}" d="{flap}"/></g></svg>\n')


if __name__ == "__main__":
    out = os.path.join(KIT, "android")
    os.makedirs(out, exist_ok=True)
    body, flap = paige_paths(**CUTS["paige-symbol"])
    files = {
        "ic_launcher_foreground.xml": vector(INK, body, flap),
        "ic_launcher_monochrome.xml": vector("#000000", body, flap),
        "colors-snippet.xml": f'<color name="ic_launcher_background">{LIME}</color>\n',
        "play-store-icon.svg": play_svg(body, flap),
    }
    for name, text in files.items():
        with open(os.path.join(out, name), "w") as fh:
            fh.write(text)
    print("wrote", ", ".join(files), "scale", round(SCALE, 4), "mark", round(188 * SCALE, 1), "x", round(148 * SCALE, 1), "dp")
