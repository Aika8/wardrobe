#!/usr/bin/env python3
"""Drawn clothes and the paper doll for the mockups (docs/design/DESIGN.md, «Clothes and the doll»).

Clothes are fashion flats: the real cut of a garment (collar, placket, cuffs, seams, pockets, folds) with soft
shading, in the item's colour with an ink outline — not icons. Each garment sits in a 0 0 100 100 box.

    python3 docs/design/mockups/garments.py          # rewrites the drawings inside mockups.html in place
    python3 docs/design/mockups/garments.py --json   # SVG markup per garment and the doll, for the app (ADR-0003)

The mockup marks each drawing `<svg class="cl" data-g="<garment>" data-c="<colour>">`; this script fills it.
"""
from __future__ import annotations

import re
import sys
from pathlib import Path

INK = "#3F3630"
CARD = "#FCF8F1"
HARDWARE = "#D7A955"  # mustard: buckles, clasps


def base(d):            # the garment's cloth, in the item colour
    return ("base", d)


def shade(d, o=0.12):   # shadow on the cloth
    return ("shade", d, o)


def light(d, o=0.28):   # light catching the cloth
    return ("light", d, o)


def line(d, o=0.45, w=0.9):   # seams, folds, creases
    return ("line", d, o, w)


def stitch(d, o=0.5):   # top-stitching
    return ("stitch", d, o)


def dot(cx, cy, r, fill=CARD):  # buttons, eyelets
    return ("dot", cx, cy, r, fill)


def part(d, fill=None, o=1.0):  # a separate piece drawn over the base (collar, flap); fill None = item colour
    return ("part", d, fill, o)


def ribs(x0, x1, y0, y1, step=2.2):
    return [line(f"M{x:.1f} {y0} L{x:.1f} {y1}", 0.3, 0.6) for x in frange(x0, x1, step)]


def frange(a, b, s):
    x = a
    while x <= b + 1e-9:
        yield x
        x += s


GARMENTS = {
    "tee": [
        base("M35 14 C40 17 45 18.5 50 18.5 C55 18.5 60 17 65 14 L80 20 C83 21.5 85 24 86 27 L91 40 C88 42.5 84 44 80 44.5 "
             "L74 35 C73.5 50 73.5 70 74.5 88 C66 90.5 34 90.5 25.5 88 C26.5 70 26.5 50 26 35 L20 44.5 C16 44 12 42.5 9 40 "
             "L14 27 C15 24 17 21.5 20 20 Z"),
        shade("M37 14 C42 18.5 58 18.5 63 14 C58 15.6 42 15.6 37 14 Z", 0.3),
        shade("M26 35 C27 55 27 72 25.5 88 C28.5 88.8 30.5 89.1 32.5 89.3 C31 72 30.5 52 30 38 Z"),
        shade("M74 35 L80 44.5 C78 44.8 76.5 44.6 75 44 Z", 0.18),
        light("M58 24 C62 34 63 52 61 70 C64 58 65 38 62 24 Z"),
        line("M35 14 C40 21 60 21 65 14", 0.6, 1.1),
        line("M44 40 C46 52 45 64 42 76", 0.22, 0.8),
        line("M26.5 36 C28 33 29 29 28 24", 0.3, 0.8),
        line("M73.5 36 C72 33 71 29 72 24", 0.3, 0.8),
        stitch("M27 85.2 C40 87.2 60 87.2 73.5 85.2"),
        stitch("M11 37.5 C14 39.5 17.5 40.8 21 41.5"),
        stitch("M89 37.5 C86 39.5 82.5 40.8 79 41.5"),
    ],
    "shirt": [
        base("M36 12 L44 9.5 L50 16 L56 9.5 L64 12 L77 17.5 C80 19 82 22 83 26 L90 70 C90.5 72 89.5 73.5 87.5 74 L82 75 "
             "C80 75.3 78.6 74.4 78.2 72.6 L72.5 42 C72.8 58 73 75 73.8 92 C66 96 58 93 50 94.5 C42 93 34 96 26.2 92 "
             "C27 75 27.2 58 27.5 42 L21.8 72.6 C21.4 74.4 20 75.3 18 75 L12.5 74 C10.5 73.5 9.5 72 10 70 L17 26 "
             "C18 22 20 19 23 17.5 Z"),
        shade("M27.5 42 C27.2 58 27 75 26.2 92 C28.5 93.2 30.5 93.8 32 94 C31 76 30.8 58 31 45 Z"),
        shade("M11 66 L21 67.8 L21.8 72.6 C21.4 74.4 20 75.3 18 75 L12.5 74 C10.5 73.5 9.5 72 10 70 Z", 0.1),
        shade("M89 66 L79 67.8 L78.2 72.6 C78.6 74.4 80 75.3 82 75 L87.5 74 C89.5 73.5 90.5 72 90 70 Z", 0.1),
        shade("M44 9.5 C47 8.4 53 8.4 56 9.5 L55 11.2 C52 10.4 48 10.4 45 11.2 Z", 0.3),
        light("M60 26 C64 40 65 60 63 84 C67 68 67 42 63 26 Z"),
        part("M44 9.5 L50 16 L45.8 21.5 L39.5 13.2 Z"),
        part("M56 9.5 L50 16 L54.2 21.5 L60.5 13.2 Z"),
        line("M50 16 L50 94", 0.5, 0.8),
        line("M52.6 21 L52.6 93.6", 0.35, 0.7),
        line("M23 17.5 C35 22.5 65 22.5 77 17.5", 0.3, 0.7),
        line("M33 30 L42 30 L42 38.5 C39 40 36 40 33 38.5 Z", 0.4, 0.8),
        line("M18 30 C19.5 42 20.5 55 20 66", 0.2, 0.7),
        line("M82 30 C80.5 42 79.5 55 80 66", 0.2, 0.7),
        line("M40 50 C42 62 41 74 38 86", 0.18, 0.7),
        dot(51.3, 27, 1.05), dot(51.3, 39, 1.05), dot(51.3, 51, 1.05), dot(51.3, 63, 1.05), dot(51.3, 75, 1.05),
        dot(51.3, 87, 1.05), dot(15.5, 70.5, 0.9), dot(84.5, 70.5, 0.9),
    ],
    "knit": [
        base("M36 13 C41 17 59 17 64 13 L78 18.5 C82 20 84.5 23 85.5 27 L91.5 72 C91.8 74.5 90 76 88 76.3 L82.5 77 "
             "C80.5 77.2 79 76 78.6 74 L73 44 C73.2 58 73.3 72 74 84 C74.2 86 73 87.5 71 87.8 C58 89.5 42 89.5 29 87.8 "
             "C27 87.5 25.8 86 26 84 C26.7 72 26.8 58 27 44 L21.4 74 C21 76 19.5 77.2 17.5 77 L12 76.3 C10 76 8.2 74.5 "
             "8.5 72 L14.5 27 C15.5 23 18 20 22 18.5 Z"),
        shade("M36 13 C41 17 59 17 64 13 C60 20.5 40 20.5 36 13 Z", 0.16),
        shade("M27 44 C26.8 58 26.7 72 26 84 C26 86 27 87.5 29 87.8 L32 88.1 C30.8 72 30.6 58 30.6 47 Z"),
        shade("M9 70 L21.9 71.6 L21.4 74 C21 76 19.5 77.2 17.5 77 L12 76.3 C10 76 8.2 74.5 8.5 72 Z", 0.12),
        shade("M91 70 L78.1 71.6 L78.6 74 C79 76 80.5 77.2 82.5 77 L88 76.3 C90 76 91.8 74.5 91.5 72 Z", 0.12),
        shade("M26.2 81 C42 83 58 83 73.8 81 L74 84 C74.2 86 73 87.5 71 87.8 C58 89.5 42 89.5 29 87.8 C27 87.5 "
              "25.8 86 26 84 Z", 0.1),
        light("M58 26 C61 40 62 60 60 78 C64 62 64 40 61 26 Z", 0.22),
        line("M46 24 C44 30 48 34 46 40 C44 46 48 50 46 56 C44 62 48 66 46 72 C44 76 46 78 46 80", 0.3, 0.8),
        line("M54 24 C56 30 52 34 54 40 C56 46 52 50 54 56 C56 62 52 66 54 72 C56 76 54 78 54 80", 0.3, 0.8),
        line("M27 44 C27 36 24 28 22 18.5", 0.25, 0.7),
        line("M73 44 C73 36 76 28 78 18.5", 0.25, 0.7),
        *ribs(28, 72, 81.6, 87.6),
        line("M9 70 L21.9 71.6", 0.4, 0.8),
        line("M91 70 L78.1 71.6", 0.4, 0.8),
    ],
    "trousers": [
        base("M30 8 L70 8 C71 25 72.5 55 74 92 C73.5 93.5 72 94 70.5 94 L57.5 94 C56 94 55 93 54.9 91.5 L51.2 36 "
             "C51 34.5 49 34.5 48.8 36 L45.1 91.5 C45 93 44 94 42.5 94 L29.5 94 C28 94 26.5 93.5 26 92 C27.5 55 29 25 30 8 Z"),
        shade("M30 8 L70 8 L70.3 14 L29.7 14 Z", 0.1),
        shade("M48.8 36 L45.1 91.5 C45 93 44 94 42.5 94 L40.8 94 L45.6 40 Z", 0.16),
        shade("M30 15 C29 40 28 66 26.5 91 C28 93 29 93.5 31 93.8 C31.5 66 32.5 40 33.5 18 Z", 0.1),
        light("M64 20 C65.5 44 67 68 68.5 92 L70.5 92 C69 68 67.5 44 66.4 20 Z", 0.3),
        line("M29.7 14 L70.3 14", 0.5, 0.8),
        *[line(f"M{x} 7.6 L{x} 15.2", 0.55, 1.4) for x in (34, 45, 55, 66)],
        line("M50 14 L50 30 C50 32 52.4 33.4 54 32", 0.5, 0.8),
        stitch("M53.2 14.5 L53.2 29.5"),
        line("M31.5 15 C36 18 38.5 22 39 27", 0.5, 0.8),
        line("M68.5 15 C64 18 61.5 22 61 27", 0.5, 0.8),
        line("M37.5 20 L36.5 92.5", 0.22, 0.7),
        line("M62.5 20 L63.5 92.5", 0.22, 0.7),
        stitch("M27 89.5 L44.6 89.5"),
        stitch("M55.4 89.5 L73 89.5"),
        line("M40 54 C41 56 43 57 45.5 57", 0.2, 0.6),
        line("M60 54 C59 56 57 57 54.5 57", 0.2, 0.6),
    ],
    "skirt": [
        base("M36 16 L64 16 L65 22 C72 40 78 62 82 86 C72 90.5 28 90.5 18 86 C22 62 28 40 35 22 Z"),
        shade("M36 16 L64 16 L65 22 L35 22 Z", 0.12),
        shade("M42 30 C40 50 37 70 34 89 C31 88.7 28.5 88.3 26 87.8 C30 70 36 48 42 30 Z", 0.1),
        shade("M58 30 C60 50 63 70 66 89 C68 88.8 70 88.4 72 88 C68 70 63 48 58 30 Z", 0.12),
        light("M50 34 C52 54 53 72 54 89 C56 89 57.5 89 59 88.9 C57 72 54 52 50 34 Z", 0.22),
        line("M35 22 L65 22", 0.5, 0.8),
        line("M42 30 C40 50 37 70 34 88.6", 0.28, 0.7),
        line("M58 30 C60 50 63 70 66 88.6", 0.28, 0.7),
        line("M50 34 C50 55 50 72 50 89.2", 0.2, 0.7),
        line("M41 22 L42 30", 0.35, 0.7),
        line("M59 22 L58 30", 0.35, 0.7),
        stitch("M20 83.4 C35 87.4 65 87.4 80 83.4"),
        dot(62, 19, 0.9),
    ],
    "dress": [
        base("M38 6 L43 6 C44 13 47 18 50 18 C53 18 56 13 57 6 L62 6 C63 11 64 17 66 22 C67 27 66 33 64 38 "
             "C63.5 41 63 43 63.5 45 C70 58 77 74 82 92 C70 97 30 97 18 92 C23 74 30 58 36.5 45 C37 43 36.5 41 36 38 "
             "C34 33 33 27 34 22 C36 17 37 11 38 6 Z"),
        shade("M43 6 C44 13 47 18 50 18 C53 18 56 13 57 6 C54 9.5 46 9.5 43 6 Z", 0.3),
        shade("M36.5 45 C37 43 36.5 41 36 38 C34 33 33 27 34 22 L37 22 C36.5 28 37.5 34 39.5 40 C40 42 40 44 39.8 46 Z", 0.1),
        shade("M44 50 C40 64 36 78 33 95.2 C29 94.8 25 94.1 21.5 93.2 C27 76 35 60 44 50 Z", 0.1),
        shade("M57 50 C61 64 65 78 69 95 C72 94.6 75 94 78 93.2 C73 76 65 60 57 50 Z", 0.12),
        light("M50 50 C51 66 52 80 52.5 96 C55 96 57 95.9 59 95.6 C57 80 54 64 50 50 Z", 0.22),
        line("M36.5 44.5 C45 47 55 47 63.5 44.5", 0.5, 0.8),
        line("M42 24 L43.6 40", 0.3, 0.7),
        line("M58 24 L56.4 40", 0.3, 0.7),
        line("M44 50 C40 64 36 78 33 95", 0.25, 0.7),
        line("M57 50 C61 64 65 78 69 95", 0.25, 0.7),
        line("M50.5 48 C51 64 52 80 52.5 96", 0.18, 0.7),
        stitch("M21 90 C35 94.4 65 94.4 79 90"),
    ],
    "coat": [
        base("M34 7 L43 5 L50 30 L57 5 L66 7 L79 13 C83 15 85 18 86 23 L91 78 C91.3 80.5 89.6 82 87.5 82.2 L82 82.5 "
             "C80 82.6 78.7 81.3 78.5 79.5 L73.5 40 C73.8 60 74.5 80 75.5 96 C62 98.5 38 98.5 24.5 96 C25.5 80 26.2 60 "
             "26.5 40 L21.5 79.5 C21.3 81.3 20 82.6 18 82.5 L12.5 82.2 C10.4 82 8.7 80.5 9 78 L14 23 C15 18 17 15 21 13 Z"),
        shade("M43 5 C47 3.8 53 3.8 57 5 L55.5 8 C52 7 48 7 44.5 8 Z", 0.3),
        shade("M26.5 40 C26.2 60 25.5 80 24.5 96 C27 96.6 29 96.9 31 97.1 C30 80 30 60 30.3 43 Z", 0.12),
        shade("M9.6 74 L21.9 75.8 L21.5 79.5 C21.3 81.3 20 82.6 18 82.5 L12.5 82.2 C10.4 82 8.7 80.5 9 78 Z", 0.12),
        shade("M90.4 74 L78.1 75.8 L78.5 79.5 C78.7 81.3 80 82.6 82 82.5 L87.5 82.2 C89.6 82 91.3 80.5 91 78 Z", 0.12),
        shade("M26.3 52 C42 54 58 54 73.7 52 L73.8 57 C58 59 42 59 26.2 57 Z", 0.14),
        light("M62 40 C65 56 66 74 66 94 C69 78 69 56 65 40 Z", 0.22),
        part("M43 5 L50 30 L44.5 40 L39.5 22 L35.5 16 L40 12.5 Z"),
        part("M57 5 L50 30 L55.5 40 L60.5 22 L64.5 16 L60 12.5 Z"),
        line("M39.5 22 L35.5 16", 0.5, 0.8),
        line("M60.5 22 L64.5 16", 0.5, 0.8),
        line("M52.5 36 L52.5 97.6", 0.5, 0.8),
        line("M30 66 L42 66 L42 70.5 L30 70.5 Z", 0.45, 0.8),
        line("M58 66 L70 66 L70 70.5 L58 70.5 Z", 0.45, 0.8),
        line("M21 13 C21.5 22 23 32 26.5 40", 0.25, 0.7),
        line("M79 13 C78.5 22 77 32 73.5 40", 0.25, 0.7),
        line("M15.5 36 C16.5 50 17 62 16.5 74", 0.2, 0.7),
        line("M84.5 36 C83.5 50 83 62 83.5 74", 0.2, 0.7),
        ("rect", 47.5, 52.4, 9, 5.2, HARDWARE),
        dot(46, 46, 1.4, INK), dot(57, 46, 1.4, INK), dot(46, 63, 1.4, INK), dot(57, 63, 1.4, INK),
        stitch("M25.2 93 C38 95.4 62 95.4 74.8 93"),
    ],
    "shoes": [
        ("group", "translate(-27 -5)", [
            base("M40 66 C40 58 44 52 50 50 L60 49 C63 49 64.5 51 66 53 C70 58 78 60 86 62 C91 63.5 93 66.5 92.5 70 "
                 "L92 72 L40 72 Z"),
            shade("M40 66 C40 58 44 52 50 50 L52 50 C47 53 44 58 44 66 L44 72 L40 72 Z", 0.16),
            part("M39 72 L93 72 C93.5 75 92 77 89 77 L42 77 C40 77 38.8 75 39 72 Z", CARD),
        ]),
        base("M40 66 C40 58 44 52 50 50 L60 49 C63 49 64.5 51 66 53 C70 58 78 60 86 62 C91 63.5 93 66.5 92.5 70 "
             "L92 72 L40 72 Z"),
        shade("M40 66 C40 58 44 52 50 50 L52 50 C47 53 44 58 44 66 L44 72 L40 72 Z", 0.14),
        shade("M78 61 C84 62.5 90 64.5 92.3 69 L92 72 L80 72 C80 68 79.5 64 78 61 Z", 0.12),
        light("M52 52.5 C56 51.5 60 51.5 62 52.5 C58 53.5 55 54 52 55 Z", 0.4),
        part("M39 72 L93 72 C93.5 75 92 77 89 77 L42 77 C40 77 38.8 75 39 72 Z", CARD),
        line("M39.4 74.5 L92.9 74.5", 0.3, 0.6),
        line("M78 61 C84 62.5 90 64.5 92.3 69", 0.45, 0.8),
        line("M44 54 C42.5 57 42 61 43 64", 0.4, 0.8),
        line("M61 52 L66.5 56.8", 0.6, 0.9),
        line("M63.5 51 L69 55.6", 0.6, 0.9),
        line("M58.5 53.6 L64 58.2", 0.6, 0.9),
        dot(61, 52, 0.6, INK), dot(58.5, 53.6, 0.6, INK), dot(63.5, 51, 0.6, INK),
        stitch("M46 68.6 L88 68.6"),
    ],
    "bag": [
        base("M22 42 C22 39 24 37.5 27 37.5 L73 37.5 C76 37.5 78 39 78 42 L81 82 C81.3 86 79 88 75 88 L25 88 "
             "C21 88 18.7 86 19 82 Z"),
        ("handle", "M35 38 C35 20 65 20 65 38"),
        shade("M19.5 76 C40 79 60 79 80.6 76 L81 82 C81.3 86 79 88 75 88 L25 88 C21 88 18.7 86 19 82 Z", 0.12),
        part("M22.5 41.5 C22.5 39 24.5 38 27 38 L73 38 C75.5 38 77.5 39 77.5 41.5 L76.5 58 C67 63 33 63 23.5 58 Z"),
        shade("M23.5 58 C33 63 67 63 76.5 58 L76.4 60 C67 65 33 65 23.6 60 Z", 0.2),
        light("M27 41 L73 41 L72.6 45 C60 46.5 40 46.5 27.4 45 Z", 0.25),
        stitch("M25.5 42 L74.5 42 L73.6 56 C65 60.5 35 60.5 26.4 56 Z"),
        ("rect", 46.5, 56.5, 7, 6.5, HARDWARE),
        line("M48.5 59.8 L51.5 59.8", 0.6, 0.8),
        stitch("M21.5 83 C40 84.6 60 84.6 78.5 83"),
    ],
    "scarf": [
        base("M33 18 C29 40 29.5 62 31.5 84 L44.5 84 C43.5 62 43.5 42 46.5 26 Z"),
        base("M67 18 C71 40 70.5 62 68.5 86 L55.5 86 C56.5 62 56.5 42 53.5 26 Z"),
        shade("M33 18 C29 40 29.5 62 31.5 84 L35.5 84 C34 62 34 40 37 22 Z", 0.12),
        shade("M67 18 C71 40 70.5 62 68.5 86 L64.6 86 C66.2 62 66.4 40 63.6 22 Z", 0.16),
        base("M33 18 C38 9 62 9 67 18 C64 24 58 27 50 27 C42 27 36 24 33 18 Z"),
        shade("M37 18.5 C42 14 58 14 63 18.5 C58 16.8 42 16.8 37 18.5 Z", 0.3),
        base("M42 22 C45 19 55 19 58 22 C59 28 56 33 50 33 C44 33 41 28 42 22 Z"),
        shade("M42 22 C41 28 44 33 50 33 C46 31 43.5 27.5 43.6 22.6 Z", 0.14),
        light("M39 38 C38 52 38 66 39 80 L41 80 C40 66 40 52 41 38 Z", 0.25),
        light("M60 40 C61 54 61 68 60 82 L62 82 C63 68 63 54 62 40 Z", 0.2),
        line("M30.6 62 L44 62.4", 0.3, 1.4),
        line("M30.4 70 L43.9 70.2", 0.3, 1.4),
        line("M56.2 64 L70.2 63.6", 0.3, 1.4),
        line("M56 72 L69.8 71.8", 0.3, 1.4),
        line("M46 22 C47 25 47 28 46 31", 0.3, 0.7),
        line("M54 22 C53 25 53 28 54 31", 0.3, 0.7),
        *[line(f"M{x:.1f} 84 L{x - 0.3:.1f} 91", 0.65, 0.8) for x in frange(32.5, 44.2, 1.9)],
        *[line(f"M{x:.1f} 86 L{x + 0.3:.1f} 93", 0.65, 0.8) for x in frange(56.5, 68.2, 1.9)],
    ],
}


def render(gid: str, colour: str) -> str:
    out = []

    def emit(layer):
        kind = layer[0]
        if kind == "base":
            out.append(f'<path d="{layer[1]}" fill="{colour}" stroke="{INK}" stroke-width="1.5" stroke-linejoin="round"/>')
        elif kind == "part":
            _, d, fill, o = layer
            out.append(f'<path d="{d}" fill="{fill or colour}" stroke="{INK}" stroke-width="1.2" stroke-linejoin="round"/>')
        elif kind == "shade":
            out.append(f'<path d="{layer[1]}" fill="{INK}" fill-opacity="{layer[2]}"/>')
        elif kind == "light":
            out.append(f'<path d="{layer[1]}" fill="{CARD}" fill-opacity="{layer[2]}"/>')
        elif kind == "line":
            _, d, o, w = layer
            out.append(f'<path d="{d}" fill="none" stroke="{INK}" stroke-opacity="{o}" stroke-width="{w}" stroke-linecap="round"/>')
        elif kind == "stitch":
            out.append(f'<path d="{layer[1]}" fill="none" stroke="{INK}" stroke-opacity="{layer[2]}" stroke-width="0.6" '
                       f'stroke-dasharray="1.4 1.2"/>')
        elif kind == "dot":
            _, cx, cy, r, fill = layer
            out.append(f'<circle cx="{cx}" cy="{cy}" r="{r}" fill="{fill}" stroke="{INK}" stroke-width="0.6"/>')
        elif kind == "rect":
            _, x, y, w, h, fill = layer
            out.append(f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="1.2" fill="{fill}" stroke="{INK}" stroke-width="0.7"/>')
        elif kind == "handle":
            out.append(f'<path d="{layer[1]}" fill="none" stroke="{INK}" stroke-width="5.2" stroke-linecap="round"/>')
            out.append(f'<path d="{layer[1]}" fill="none" stroke="{colour}" stroke-width="3.4" stroke-linecap="round"/>')
        elif kind == "group":
            out.append(f'<g transform="{layer[1]}">')
            for sub in layer[2]:
                emit(sub)
            out.append("</g>")

    for layer in GARMENTS[gid]:
        emit(layer)
    return "".join(out)


# ---------------------------------------------------------------- the paper doll

def mirror(d: str, axis: float = 89.0) -> str:
    """Mirror an absolute M/L/C/Z path across x = axis (the doll is symmetric)."""
    toks = re.findall(r"[MLCZ]|-?\d+(?:\.\d+)?", d)
    i, res = 0, []
    while i < len(toks):
        t = toks[i]
        if t in "MLCZ":
            res.append(t)
            i += 1
            continue
        x, y = float(toks[i]), float(toks[i + 1])
        res.append(f"{2 * axis - x:g} {y:g}")
        i += 2
    return " ".join(res)


SKIN, HAIR = "var(--doll-skin)", "var(--doll-hair)"


def doll(shirt: str, trousers: str) -> str:
    """Front view, 178 × 360, from the invented figure of the mockup (168 cm). Faceless on purpose."""
    leg_l = ("M64 172 C66 186 66 196 66.5 206 C67.5 222 69.5 232 70 241 C70 262 71 290 72.6 318 C73 324 71.6 328 "
             "69.8 332 C68 336 70 339.5 75 339.5 L84 339.5 C86.2 339.5 86.8 336 85.6 332 C84.6 326 84.6 320 85 316 "
             "C86 290 86 262 85.6 244 C85.6 230 87 210 88 188 L89 183")
    torso_l = "M89 59 L80 59 C70 60.5 63 62.5 60.5 66.5 L63.5 84 C66 98 70.5 112 70.8 122 C70.8 132 63.4 141 62.2 152 C61.6 160 62.4 166 64 172"
    arm_l = ("M60.5 65.5 C54 67 50.6 72.5 49.6 80 L46 116 C45 122 43.6 128 42.3 136 L38.6 166 C37 172 36.3 178 37.6 "
             "184 C38.6 188.4 41.4 189.4 42.8 186 C43.3 181 43.8 176 44.8 172 L47.6 166.5 C50 150 52 138 53.2 130 "
             "C54.7 120 56.2 106 58.2 92 C59 88 61 85.5 63.5 84 Z")
    left = f"{torso_l} {leg_l[leg_l.index('C'):]}"
    right = mirror(left)
    # one closed outline: left half down to the crotch, right half back up
    body_path = left + " " + reverse_path(right) + " Z"
    neck = "M83 47 C83 53 82 56.5 79.5 59.5 L98.5 59.5 C96 56.5 95 53 95 47 Z"
    hair = ("M73.5 33 C72 18 79.5 10.5 89 10.5 C98.5 10.5 106 18 104.5 33 C104 27 101 22.5 96 20.6 C92 22.6 84 22.8 "
            "79.6 21.2 C76.2 23.6 74.2 27.8 73.5 33 Z")
    bun = "M83 9.5 C83 5 86 3 89 3 C92 3 95 5 95 9.5 C93 11.2 85 11.2 83 9.5 Z"

    shirt_body = ("M78.5 57.5 L99.5 57.5 C109.5 59 116.5 62 119.5 66.5 L116.6 84.5 C114 97 109.6 109 109.4 122 "
                  "C109.4 134 114 142 115.6 156.5 C103 160.6 75 160.6 62.4 156.5 C64 142 68.6 134 68.6 122 C68.4 109 "
                  "64 97 61.4 84.5 L58.5 66.5 C61.5 62 68.5 59 78.5 57.5 Z")
    sleeve_l = ("M59.2 64.6 C52.6 66.2 48.6 71.6 47.6 79.6 L43.8 115.6 C42.8 121.6 41.2 127.8 40 135.8 L36.6 158.8 "
                "L47.2 160.4 L50.8 134.8 C51.8 126.8 53.4 120.6 54.6 114.4 L57.6 92.4 C58.6 88 60.6 85.4 62.8 84 Z")
    sleeve_r = mirror(sleeve_l)
    cuff_l = "M37.6 152 L48 153.6 L47.2 160.4 L36.6 158.8 Z"
    cuff_r = mirror(cuff_l)
    trousers_p = ("M69.6 119 L108.4 119 C109.6 133 115.6 141.6 116.8 152 C117.4 160 116.4 166 114.9 172 L112.6 206 "
                  "C111.6 222 109.6 232 109.1 241 C108.6 262 107.6 290 106.2 326 L92.4 326 C91.9 296 91.9 262 92 244 "
                  "C92 230 90.8 210 89.8 187 L89 183 L88.2 187 C87.2 210 86 230 86 244 C86.1 262 86.1 296 85.6 326 "
                  "L71.8 326 C70.4 290 69.4 262 68.9 241 C68.4 232 66.4 222 65.4 206 L63.1 172 C61.6 166 60.6 160 "
                  "61.2 152 C62.4 141.6 68.4 133 69.6 119 Z")
    p = []
    a = p.append
    a(f'<path d="{body_path}" style="fill:{SKIN}" stroke="{INK}" stroke-width="1.3" stroke-linejoin="round"/>')
    a(f'<path d="{arm_l}" style="fill:{SKIN}" stroke="{INK}" stroke-width="1.3" stroke-linejoin="round"/>')
    a(f'<path d="{mirror(arm_l)}" style="fill:{SKIN}" stroke="{INK}" stroke-width="1.3" stroke-linejoin="round"/>')
    a(f'<path d="{neck}" style="fill:{SKIN}" stroke="{INK}" stroke-width="1.3"/>')
    a(f'<ellipse cx="89" cy="31" rx="15.2" ry="19" style="fill:{SKIN}" stroke="{INK}" stroke-width="1.3"/>')
    a(f'<path d="M95 47 C94 51 92 53 89 53.6 C92.5 55 95.5 54 97 50 Z" fill="{INK}" fill-opacity=".12"/>')  # under the chin
    a(f'<path d="{hair}" style="fill:{HAIR}" stroke="{INK}" stroke-width="1.1"/>')
    a(f'<path d="{bun}" style="fill:{HAIR}" stroke="{INK}" stroke-width="1.1"/>')
    # trousers (lower layer)
    a(f'<path d="{trousers_p}" fill="{trousers}" stroke="{INK}" stroke-width="1.3" stroke-linejoin="round"/>')
    a(f'<path d="M89 183 L88.2 187 C87.2 210 86 230 86 244 C86.1 262 86.1 296 85.6 326 L81 326 C81.6 296 82 262 '
      f'82.2 244 C82.6 228 84.6 208 87 188 Z" fill="{INK}" fill-opacity=".16"/>')
    a(f'<path d="M108.4 119 C109.6 133 115.6 141.6 116.8 152 C117.4 160 116.4 166 114.9 172 L112.6 206 C111.6 222 '
      f'109.6 232 109.1 241 C108.6 262 107.6 290 106.2 326 L102.6 326 C104 290 105 262 105.6 240 C106.4 226 108.8 210 '
      f'110 190 C111 172 110 150 105 128 Z" fill="{INK}" fill-opacity=".1"/>')
    for d in ("M77.5 170 L76.5 325", "M100.5 170 L101.5 325"):
        a(f'<path d="{d}" fill="none" stroke="{INK}" stroke-opacity=".25" stroke-width=".8"/>')
    a(f'<path d="M71 240 C74 243 80 243.6 84 242" fill="none" stroke="{INK}" stroke-opacity=".2" stroke-width=".8"/>')
    a(f'<path d="M107 240 C104 243 98 243.6 94 242" fill="none" stroke="{INK}" stroke-opacity=".2" stroke-width=".8"/>')
    a(f'<path d="M72.4 320.5 L85.8 320.5 M92.2 320.5 L105.6 320.5" fill="none" stroke="{INK}" stroke-opacity=".45" '
      f'stroke-width=".6" stroke-dasharray="1.4 1.2"/>')
    # shirt (upper layer): sleeves, body, collar, placket, buttons, cuffs
    for d in (sleeve_l, sleeve_r):
        a(f'<path d="{d}" fill="{shirt}" stroke="{INK}" stroke-width="1.3" stroke-linejoin="round"/>')
    for d in (cuff_l, cuff_r):
        a(f'<path d="{d}" fill="{INK}" fill-opacity=".08" stroke="{INK}" stroke-opacity=".5" stroke-width=".8"/>')
    a(f'<path d="{shirt_body}" fill="{shirt}" stroke="{INK}" stroke-width="1.3" stroke-linejoin="round"/>')
    a(f'<path d="M109.4 122 C109.4 134 114 142 115.6 156.5 C112 157.8 108.6 158.6 105.4 159.2 C106 146 104.6 134 '
      f'104.4 122 C104.4 110 108 98 112.4 86 L116.6 84.5 C114 97 109.6 109 109.4 122 Z" fill="{INK}" fill-opacity=".1"/>')
    a(f'<path d="M78.5 57.5 C82 61 85.5 65 89 70.5 C92.5 65 96 61 99.5 57.5 C95 59.6 83 59.6 78.5 57.5 Z" '
      f'style="fill:{SKIN}" stroke="{INK}" stroke-width="1"/>')
    a(f'<path d="M78.5 57.5 L89 70.5 L84 74 L76.5 61.5 Z" fill="{shirt}" stroke="{INK}" stroke-width="1"/>')
    a(f'<path d="M99.5 57.5 L89 70.5 L94 74 L101.5 61.5 Z" fill="{shirt}" stroke="{INK}" stroke-width="1"/>')
    a(f'<path d="M89 70.5 L89 159.6" fill="none" stroke="{INK}" stroke-opacity=".5" stroke-width=".8"/>')
    a(f'<path d="M91.4 74 L91.4 159.4" fill="none" stroke="{INK}" stroke-opacity=".3" stroke-width=".7"/>')
    for y in (82, 96, 110, 124, 138, 152):
        a(f'<circle cx="90.2" cy="{y}" r="1.1" fill="{CARD}" stroke="{INK}" stroke-width=".6"/>')
    for d in ("M72 100 C74 112 74 128 71.5 146", "M106 100 C104 112 104 128 106.5 146",
              "M52 96 C51 108 49.5 122 47 134", "M126 96 C127 108 128.5 122 131 134"):
        a(f'<path d="{d}" fill="none" stroke="{INK}" stroke-opacity=".2" stroke-width=".8"/>')
    # fit markers (FitNote 1: the sleeve, 2: the trouser length)
    for (cx, cy, colour, n) in ((40, 166, "var(--mustard)", 1), (66, 326, "var(--sage)", 2)):
        a(f'<circle cx="{cx}" cy="{cy}" r="6.5" style="fill:{colour}" stroke="{CARD}" stroke-width="1.5"/>')
        a(f'<text x="{cx}" y="{cy + 3.4}" font-size="9.5" font-weight="800" text-anchor="middle" '
          f'style="fill:var(--on-accent);font-family:var(--body)">{n}</text>')
    return "".join(p)


def reverse_path(d: str) -> str:
    """Reverse an absolute M/C path (no L/Z inside) so it can continue another outline."""
    toks = re.findall(r"[MLC]|-?\d+(?:\.\d+)?", d)
    segs, i, cur = [], 0, None
    while i < len(toks):
        t = toks[i]
        if t == "M":
            cur = (toks[i + 1], toks[i + 2]); i += 3; continue
        if t == "L":
            p1 = (toks[i + 1], toks[i + 2]); segs.append(("L", cur, [p1])); cur = p1; i += 3; continue
        if t == "C":
            c1 = (toks[i + 1], toks[i + 2]); c2 = (toks[i + 3], toks[i + 4]); p = (toks[i + 5], toks[i + 6])
            segs.append(("C", cur, [c1, c2, p])); cur = p; i += 7; continue
        i += 1
    out = []
    for kind, frm, rest in reversed(segs):
        if kind == "L":
            out.append(f"L {frm[0]} {frm[1]}")
        else:
            c1, c2, _ = rest
            out.append(f"C {c2[0]} {c2[1]} {c1[0]} {c1[1]} {frm[0]} {frm[1]}")
    return " ".join(out)


# Token values for --json (the app has no CSS variables); DESIGN.md is their source.
TOKENS = {"--doll-skin": "#EBD9C4", "--doll-hair": "#6E5A4C", "--mustard": "#D7A955", "--sage": "#8DA48A",
          "--on-accent": "#FFF8EE", "--body": "Nunito"}


def as_app_svg(markup: str) -> str:
    """Resolve the CSS variables of the mockup page into plain attributes an SVG renderer understands."""
    markup = re.sub(r'style="fill:var\((--[a-z-]+)\)"', lambda m: f'fill="{TOKENS[m.group(1)]}"', markup)
    markup = re.sub(r'style="fill:var\((--[a-z-]+)\);font-family:var\(--body\)"',
                    lambda m: f'fill="{TOKENS[m.group(1)]}" font-family="Nunito"', markup)
    assert "var(" not in markup, markup[markup.index("var("):][:60]
    return markup


def main() -> int:
    if "--json" in sys.argv[1:]:
        import json
        print(json.dumps({
            "viewBox": {"garment": "0 0 100 100", "doll": "0 0 178 360"},
            "garments": {k: as_app_svg(render(k, "{colour}")) for k in GARMENTS},
            "doll": as_app_svg(doll("{shirt}", "{trousers}")),
        }, ensure_ascii=False, indent=1, sort_keys=True))
        return 0
    page = Path(__file__).with_name("mockups.html")
    s = page.read_text(encoding="utf-8")

    def fill(m):
        attrs, gid, colour = m.group(1), m.group(2), m.group(3)
        return f'<svg class="cl"{attrs}>{render(gid, colour)}</svg>'

    s, n = re.subn(r'<svg class="cl"((?:(?!>).)*?data-g="([a-z]+)" data-c="(#[0-9A-Fa-f]{6})"[^>]*)>.*?</svg>', fill, s,
                   flags=re.S)
    s, m = re.subn(r'(<svg data-doll="1"[^>]*data-shirt="(#[0-9A-Fa-f]{6})" data-trousers="(#[0-9A-Fa-f]{6})"[^>]*>)'
                   r'.*?(<!-- /doll -->)',
                   lambda mm: mm.group(1) + doll(mm.group(2), mm.group(3)) + mm.group(4), s, flags=re.S)
    page.write_text(s, encoding="utf-8")
    print(f"garments drawn: {n}, dolls drawn: {m}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
