import os
from PIL import Image

SRC = r"F:\fopws2526projectfop-qqqcha\assets_raw\font"
OUT_IMG = "font.png"
OUT_FNT = "font.fnt"

images = []
chars = []

x = 0
max_h = 0

files = sorted(os.listdir(SRC))

for f in files:
    if not f.lower().endswith(".png"):
        continue

    ch = os.path.splitext(f)[0]
    if len(ch) != 1:
        continue

    code = ord(ch)

    img = Image.open(os.path.join(SRC, f)).convert("RGBA")
    w, h = img.size

    images.append((img, x, 0))
    chars.append(
        f"char id={code} x={x} y=0 width={w} height={h} "
        f"xoffset=0 yoffset=0 xadvance={w} page=0 chnl=15"
    )

    x += w
    max_h = max(max_h, h)

# 合成图集
atlas = Image.new("RGBA", (x, max_h), (0, 0, 0, 0))
for img, px, py in images:
    atlas.paste(img, (px, py))

atlas.save(OUT_IMG)

# 写 fnt
with open(OUT_FNT, "w", encoding="utf-8") as f:
    # ① info 行（必须有 padding 和 spacing）
    f.write('info face="custom" size=64 padding=0,0,0,0 spacing=0,0\n')

    # ② common 行（base 不要等于 lineHeight）
    f.write(
        f"common lineHeight={max_h} base={int(max_h * 0.75)} "
        f"scaleW={x} scaleH={max_h} pages=1 packed=0\n"
    )

    # ③ page 行（文件名一定要和实际 png 一致）
    f.write('page id=0 file="character.png"\n')

    # ④ chars
    f.write(f"chars count={len(chars)}\n")
    for c in chars:
        f.write(c + "\n")

print("DONE: font.fnt + font.png")
