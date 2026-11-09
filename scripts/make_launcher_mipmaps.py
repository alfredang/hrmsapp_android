"""Writes the launcher mipmaps and the Play icon from the TIA HRMS brand icon (v1.10).

Usage: python3 scripts/make_launcher_mipmaps.py <fg-1024.png> [icon-1024.png]
  fg-1024.png   glyph-only transparent foreground from scripts/make_adaptive_foreground.swift
  icon-1024.png the iOS AppIcon master (default ~/projects/tertiary/tia-hrms-brand/icon-1024.png)
Outputs per density: ic_launcher_foreground, ic_launcher_monochrome (white glyph, check punched
out), legacy rounded-square ic_launcher and circular ic_launcher_round; plus
store/play_store_icon_512.png. The background is the vector drawable/ic_launcher_background.xml.
"""
import os
import sys

from PIL import Image, ImageDraw

fg = Image.open(sys.argv[1]).convert("RGBA")
icon_path = sys.argv[2] if len(sys.argv) > 2 else os.path.expanduser("~/projects/tertiary/tia-hrms-brand/icon-1024.png")
icon = Image.open(icon_path).convert("RGBA")
res = "app/src/main/res"
densities = {"mdpi": 1, "hdpi": 1.5, "xhdpi": 2, "xxhdpi": 3, "xxxhdpi": 4}

mono = Image.new("RGBA", fg.size, (255, 255, 255, 0))
src, dst = fg.load(), mono.load()
for y in range(fg.size[1]):
    for x in range(fg.size[0]):
        r, g, b, a = src[x, y]
        if a:
            blue = b > r + 60 and b > g + 30  # the premier-blue check stroke
            dst[x, y] = (255, 255, 255, 0 if blue else a)

for d, m in densities.items():
    fsz, lsz = int(108 * m), int(48 * m)
    fg.resize((fsz, fsz), Image.LANCZOS).save(f"{res}/mipmap-{d}/ic_launcher_foreground.png")
    mono.resize((fsz, fsz), Image.LANCZOS).save(f"{res}/mipmap-{d}/ic_launcher_monochrome.png")
    big = icon.resize((lsz * 4, lsz * 4), Image.LANCZOS)
    for name, shape in (("ic_launcher", "rounded"), ("ic_launcher_round", "circle")):
        mask = Image.new("L", big.size, 0)
        box = (0, 0, big.size[0] - 1, big.size[1] - 1)
        if shape == "circle":
            ImageDraw.Draw(mask).ellipse(box, fill=255)
        else:
            ImageDraw.Draw(mask).rounded_rectangle(box, radius=int(big.size[0] * 0.18), fill=255)
        out = Image.new("RGBA", big.size, (0, 0, 0, 0))
        out.paste(big, (0, 0), mask)
        out.resize((lsz, lsz), Image.LANCZOS).save(f"{res}/mipmap-{d}/{name}.png")

icon.resize((512, 512), Image.LANCZOS).save("store/play_store_icon_512.png")
print("launcher mipmaps + Play icon written")
