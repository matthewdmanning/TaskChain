"""Replace the digit glyphs in cyberpunkAndroid's Fastup fonts with the digits from Oare Sans.

Fastup 1.236 FFP draws all ten digits with one placeholder outline. The patched files go into
app/src/main/res/font/ under the library's file names, so they override the library resources.

Usage: uv run --with fonttools python scripts/patch_fastup_digits.py <dir with release fonts>
The directory must hold fastup_regular.ttf, fastup_bold.ttf, and oare_sans_black_oblique.otf
from the cyberpunkAndroid release that app/build.gradle.kts uses.
"""

import math
import sys
from pathlib import Path

from fontTools.pens.cu2quPen import Cu2QuPen
from fontTools.pens.transformPen import TransformPen
from fontTools.pens.ttGlyphPen import TTGlyphPen
from fontTools.ttLib import TTFont

DIGITS = "0123456789"
OUTPUT_DIR = Path(__file__).resolve().parent.parent / "app/src/main/res/font"


def transplant_digits(target_path: Path, donor: TTFont, output_path: Path) -> None:
    target = TTFont(target_path)
    scale = target["OS/2"].sCapHeight / donor["OS/2"].sCapHeight
    # Add slant so the donor digits match the target's italic angle.
    shear = math.tan(math.radians(-target["post"].italicAngle)) - math.tan(math.radians(-donor["post"].italicAngle))
    donor_cmap, target_cmap = donor.getBestCmap(), target.getBestCmap()
    donor_glyphs, glyf, hmtx = donor.getGlyphSet(), target["glyf"], target["hmtx"]
    for digit in DIGITS:
        donor_name, target_name = donor_cmap[ord(digit)], target_cmap[ord(digit)]
        glyph_pen = TTGlyphPen(None)
        # CFF contours run counterclockwise and TrueType contours run clockwise, so reverse them.
        quadratic_pen = Cu2QuPen(glyph_pen, max_err=1.0, reverse_direction=True)
        donor_glyphs[donor_name].draw(TransformPen(quadratic_pen, (scale, 0, shear * scale, scale, 0, 0)))
        glyph = glyph_pen.glyph()
        glyph.recalcBounds(glyf)
        glyf[target_name] = glyph
        hmtx[target_name] = (round(donor["hmtx"][donor_name][0] * scale), getattr(glyph, "xMin", 0))
    target.save(output_path)


def main() -> None:
    source_dir = Path(sys.argv[1])
    donor = TTFont(source_dir / "oare_sans_black_oblique.otf")
    for name in ("fastup_regular.ttf", "fastup_bold.ttf"):
        transplant_digits(source_dir / name, donor, OUTPUT_DIR / name)
        print(f"wrote {OUTPUT_DIR / name}")


if __name__ == "__main__":
    main()
