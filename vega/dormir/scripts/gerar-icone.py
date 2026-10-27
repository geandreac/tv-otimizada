# Draws the Dormir icon: a crescent moon on black. 512x512 PNG, as the Vega manifest requires.
# Usage: python gerar-icone.py <output.png>
import sys
from PIL import Image, ImageDraw

S = 4  # supersampling for smooth edges
N = 512 * S
MOON = (244, 208, 88)  # soft yellow

img = Image.new('RGB', (N, N), (0, 0, 0))
mask = Image.new('L', (N, N), 0)
d = ImageDraw.Draw(mask)
cx, cy, r = N * 0.517, N * 0.494, N * 0.17  # off-centre so the crescent's visible mass sits in the middle
d.ellipse((cx - r, cy - r, cx + r, cy + r), fill=255)
ox, oy = cx + r * 0.42, cy - r * 0.30  # the bite that leaves a crescent opening to the upper right
d.ellipse((ox - r, oy - r, ox + r, oy + r), fill=0)
img.paste(MOON, mask=mask)

# One small star in the opening, dimmer than the moon.
star = ImageDraw.Draw(img)
sx, sy, sr = N * 0.636, N * 0.398, N * 0.011
star.ellipse((sx - sr, sy - sr, sx + sr, sy + sr), fill=(150, 128, 60))

img.resize((512, 512), Image.LANCZOS).save(sys.argv[1], optimize=True)
