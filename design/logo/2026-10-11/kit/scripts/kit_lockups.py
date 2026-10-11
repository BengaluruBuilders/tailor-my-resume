import os, re, subprocess, sys

HERE = os.path.dirname(os.path.abspath(__file__))
FONT = os.path.join(HERE, "../../../../../core/designsystem/src/main/res/font/space_mono_bold.ttf")
MASTER = sys.argv[1]
WORD = "TailorMyResume"
TRACK, NARROW = -0.02, {}
INK, LIME, WHITE = "#0A0A0A", "#A3F43F", "#FFFFFF"


def glyph(ch, x, size, baseline):
    return subprocess.run([sys.executable, "-I", os.path.join(HERE, "ttf2svg.py"), FONT, ch, "--size", str(size),
                           "--x", str(x), "--baseline", str(baseline)], capture_output=True, text=True, check=True).stdout.strip()


def wordmark(size, x, baseline):
    pen, d = x, []
    for ch in WORD:
        pull = NARROW.get(ch, 0) * size
        d.append(glyph(ch, pen - (pull if ch in "il" else 0), size, baseline))
        pen += 0.612 * size + TRACK * size - pull * (2 if ch in "il" else 1)
    return "".join(d)


def bbox(d):
    n = list(map(float, re.findall(r"-?\d+\.?\d*", re.sub(r"A[^A-Z]*", " ", d))))
    return min(n[0::2]), min(n[1::2]), max(n[0::2]), max(n[1::2])


def symbol(file):
    return re.search(r"<svg[^>]*>(?:<title>.*?</title>)?(.*)</svg>", open(os.path.join(MASTER, file)).read(), re.S).group(1)


def save(name, w, h, body, fill, bg=None):
    tile = f'<rect width="{w:g}" height="{h:g}" fill="{bg}"/>' if bg else ""
    with open(os.path.join(MASTER, name + ".svg"), "w") as fh:
        fh.write(f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {w:g} {h:g}"><title>TailorMyResume</title>'
                 f'{tile}{body.replace("#0A0A0A", fill).replace("#FFFFFF", fill)}</svg>\n')


if __name__ == "__main__":
    cap = 0.4 * 188
    size = round(cap / 0.7, 1)
    wd = wordmark(size, 0, 0)
    x0, y0, x1, y1 = bbox(wd)
    wx = 202 + 0.6 * cap - x0
    horiz = wordmark(size, wx, 128 + cap / 2)
    hw = wx + x1 + 54
    for sym, fill, bg, suffix in [("paige-symbol.svg", INK, None, ""), ("paige-symbol-reversed.svg", WHITE, None, "-reversed"),
                                  ("paige-symbol-reversed.svg", LIME, INK, "-lime-on-ink")]:
        save("lockup-horizontal" + suffix, hw, 256, symbol(sym) + f'<path fill="#0A0A0A" d="{horiz}"/>', fill, bg)
    ssize = round(size * 0.62, 1)
    sx0, sy0, sx1, sy1 = bbox(wordmark(ssize, 0, 0))
    sw = sx1 - sx0 + 2 * 54
    stack = wordmark(ssize, 54 - sx0, 222 + 48 + 0.7 * ssize)
    sh = 222 + 48 + 0.7 * ssize + 0.2 * ssize + 34
    sym_g = f'<g transform="translate({(sw - 256) / 2:g} 0)">{symbol("paige-symbol.svg")}</g>'
    save("lockup-stacked", sw, sh, sym_g + f'<path fill="#0A0A0A" d="{stack}"/>', INK)
    ox0, oy0, ox1, oy1 = bbox(wd)
    save("wordmark", ox1 - ox0 + 16, oy1 - oy0 + 16, f'<path fill="#0A0A0A" transform="translate({8 - ox0:g} {8 - oy0:g})" d="{wd}"/>', INK)
    print("cap", cap, "size", size, "horizontal width", round(hw), "stacked", round(sw), round(sh))
