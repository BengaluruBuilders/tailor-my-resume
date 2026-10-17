import struct
import sys
import argparse


def u16(b, o):
    return struct.unpack_from(">H", b, o)[0]


def i16(b, o):
    return struct.unpack_from(">h", b, o)[0]


def u32(b, o):
    return struct.unpack_from(">I", b, o)[0]


def f2dot14(b, o):
    return i16(b, o) / 16384.0


def fmt(v):
    s = "%.2f" % v
    if "." in s:
        s = s.rstrip("0").rstrip(".")
    return "0" if s in ("", "-", "-0") else s


def ceil(v):
    i = int(v)
    return i + 1 if v > i else i


def cmap4(b, o, c):
    seg = u16(b, o + 6) // 2
    ends = o + 14
    starts = ends + seg * 2 + 2
    deltas = starts + seg * 2
    ranges = deltas + seg * 2
    for i in range(seg):
        if u16(b, starts + i * 2) <= c <= u16(b, ends + i * 2):
            ro = u16(b, ranges + i * 2)
            if ro == 0:
                return (c + u16(b, deltas + i * 2)) & 0xFFFF
            g = u16(b, ranges + i * 2 + ro + 2 * (c - u16(b, starts + i * 2)))
            return ((g + u16(b, deltas + i * 2)) & 0xFFFF) if g else 0
    return None


def cmap12(b, o, c):
    for i in range(u32(b, o + 12)):
        p = o + 16 + i * 12
        if u32(b, p) <= c <= u32(b, p + 4):
            return u32(b, p + 8) + c - u32(b, p)
    return None


def cmap_subtable(b, o):
    best = None
    for i in range(u16(b, o + 2)):
        p = o + 4 + i * 8
        pid, eid, so = u16(b, p), u16(b, p + 2), o + u32(b, p + 4)
        if u16(b, so) not in (4, 12):
            continue
        if pid == 3 and eid == 10:
            s = 0
        elif pid == 3 and eid == 1:
            s = 1
        elif pid == 0:
            s = 2
        elif pid == 3:
            s = 5
        else:
            s = 9
        if best is None or s < best[0]:
            best = (s, so)
    return best[1] if best else None


def contours(b, glyf, loca, gid):
    if loca[gid] == loca[gid + 1] or gid + 1 >= len(loca):
        return []
    o = glyf + loca[gid]
    nc = i16(b, o)
    if nc < 0:
        return composite(b, o, glyf, loca)
    ends = [u16(b, o + 10 + 2 * i) for i in range(nc)]
    p = o + 10 + nc * 2
    p += 2 + u16(b, p)
    flags = []
    while len(flags) < ends[-1] + 1:
        f = b[p]
        p += 1
        flags += [f] * (1 + (b[p] if f & 8 else 0))
        if f & 8:
            p += 1
    xs = []
    v = 0
    for f in flags:
        if f & 2:
            v += b[p] if f & 16 else -b[p]
            p += 1
        elif not f & 16:
            v += i16(b, p)
            p += 2
        xs.append(v)
    ys = []
    v = 0
    for f in flags:
        if f & 4:
            v += b[p] if f & 32 else -b[p]
            p += 1
        elif not f & 32:
            v += i16(b, p)
            p += 2
        ys.append(v)
    pts = [(xs[i], ys[i], bool(flags[i] & 1)) for i in range(ends[-1] + 1)]
    out = []
    s = 0
    for e in ends:
        out.append(pts[s:e + 1])
        s = e + 1
    return out


def composite(b, o, glyf, loca):
    out = []
    while True:
        flags = u16(b, o)
        gid = u16(b, o + 2)
        o += 4
        if flags & 1:
            a1, a2 = i16(b, o), i16(b, o + 2)
            o += 4
        else:
            a1, a2 = struct.unpack_from(">bb", b, o)
            o += 2
        xx, xy, yx, yy = 1.0, 0.0, 0.0, 1.0
        if flags & 8:
            xx = yy = f2dot14(b, o)
            o += 2
        elif flags & 0x40:
            xx, yy = f2dot14(b, o), f2dot14(b, o + 2)
            o += 4
        elif flags & 0x80:
            xx, xy, yx, yy = (f2dot14(b, o), f2dot14(b, o + 2),
 f2dot14(b, o + 4), f2dot14(b, o + 6))
            o += 8
        dx, dy = (a1, a2) if flags & 2 else (0, 0)
        for ct in contours(b, glyf, loca, gid):
            out.append([(xx * x + xy * y + dx, yx * x + yy * y + dy, on)
                        for x, y, on in ct])
        if not flags & 0x20:
            return out


def contour_path(ct, ox, base, sc, out):
    if not ct:
        return
    start = next((i for i, q in enumerate(ct) if q[2]), None)
    if start is None:
        p = ct
        ct = [((p[-1][0] + p[0][0]) / 2.0, (p[-1][1] + p[0][1]) / 2.0, True)] + p[1:]
    else:
        ct = ct[start:] + ct[:start]
    out.append("M%s %s" % (fmt(ox + ct[0][0] * sc), fmt(base - ct[0][1] * sc)))
    seq = ct[1:] + [ct[0]]
    for i, p in enumerate(seq):
        if p[2]:
            out.append("L%s %s" % (fmt(ox + p[0] * sc), fmt(base - p[1] * sc)))
            continue
        nx = seq[i + 1]
        if not nx[2]:
            nx = ((p[0] + nx[0]) / 2.0, (p[1] + nx[1]) / 2.0, True)
        out.append("Q%s %s %s %s" % (fmt(ox + p[0] * sc), fmt(base - p[1] * sc),
 fmt(ox + nx[0] * sc), fmt(base - nx[1] * sc)))
    out.append("Z")


def main():
    ap = argparse.ArgumentParser(description="TrueType text to SVG path data")
    ap.add_argument("font")
    ap.add_argument("text")
    ap.add_argument("--size", type=float, default=100)
    ap.add_argument("--tracking", type=float, default=0)
    ap.add_argument("--x", type=float, default=0)
    ap.add_argument("--baseline", type=float, default=100)
    ap.add_argument("--svg", action="store_true")
    a = ap.parse_args()

    try:
        b = open(a.font, "rb").read()
    except OSError as e:
        raise SystemExit("error: cannot read font: %s" % e)
    tables = {}
    for i in range(u16(b, 4)):
        o = 12 + 16 * i
        tables[b[o:o + 4].decode("latin-1")] = (u32(b, o + 8), u32(b, o + 12))
    for t in ("head", "maxp", "hhea", "hmtx", "loca", "cmap", "glyf"):
        if t not in tables:
            raise SystemExit("error: font has no '%s' table (requires TrueType glyf outlines)" % t)
    o, _ = tables["head"]
    upem = u16(b, o + 18)
    ilf = i16(b, o + 50)
    numGlyphs = u16(b, tables["maxp"][0] + 4)
    numHMetrics = u16(b, tables["hhea"][0] + 34)
    hmtx = tables["hmtx"][0]
    lo, ll = tables["loca"]
    loca = ([u16(b, lo + 2 * i) * 2 for i in range(numGlyphs + 1)] if ilf == 0
            else [u32(b, lo + 4 * i) for i in range(numGlyphs + 1)])
    glyf = tables["glyf"][0]
    sub = cmap_subtable(b, tables["cmap"][0])
    if sub is None:
        raise SystemExit("error: no usable cmap subtable (formats 4 or 12)")

    def lookup(cp):
        if u16(b, sub) == 4:
            return cmap4(b, sub, cp)
        return cmap12(b, sub, cp)

    def advance(gid):
        if gid < numHMetrics:
            return u16(b, hmtx + 4 * gid)
        return u16(b, hmtx + 4 * (numHMetrics - 1))

    sc = a.size / float(upem)
    pen =0.0
    units = 0.0
    d = []
    for ch in a.text:
        gid = lookup(ord(ch))
        if gid is None:
            raise SystemExit("error: character U+%04X ('%s') not in cmap" % (ord(ch), ch))
        for ct in contours(b, glyf, loca, gid):
            contour_path(ct, a.x + pen, a.baseline, sc, d)
        aw = advance(gid)
        units += aw + a.tracking / sc
        pen += aw * sc + a.tracking
    path = "".join(d)
    if a.svg:
        w = ceil(pen + a.x)
        h = ceil(a.baseline + a.size * 0.3)
        print('<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 %d %d">'
              '<path fill="#000" d="%s"/></svg>' % (w, h, path))
    else:
        print(path)
    sys.stderr.write("total advance: %s font units, unitsPerEm: %d\n"
                     % (fmt(units), upem))


main()
