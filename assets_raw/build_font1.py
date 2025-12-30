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

    img = Image.open(os.path.join(SRC, f)).convert("RGBA")
    w, h = img.size

    # 记录图片在图集中的位置
    images.append((img, x, 0))

    # ===== 核心：字符码映射 =====
    codes = set()

    # 基础字符
    codes.add(ord(ch))

    # 如果是字母，同时注册大小写（共用同一张 png）
    if ch.isalpha():
        codes.add(ord(ch.lower()))
        codes.add(ord(ch.upper()))

    # 为同一张图生成多个 char id
    for code in codes:
        chars.append(
            f"char id={code} x={x} y=0 width={w} height={h} "
            f"xoffset=0 yoffset=0 xadvance={w} page=0 chnl=15"
        )

    x += w
    max_h = max(max_h, h)

# ===== 强制补 space（空格）=====
chars.insert(
    0,
    "char id=32 x=0 y=0 width=0 height=0 xoffset=0 yoffset=0 xadvance=20 page=0 chnl=15"
)

# ===== 合成图集 =====
atlas = Image.new("RGBA", (x, max_h), (0, 0, 0, 0))
for img, px, py in images:
    atlas.paste(img, (px, py))

atlas.save(OUT_IMG)

# ===== 写 fnt 文件 =====
with open(OUT_FNT, "w", encoding="utf-8") as f:
    # info 行
    f.write('info face="custom" size=64 padding=0,0,0,0 spacing=0,0\n')

    # common 行
    f.write(
        f"common lineHeight={max_h} base={int(max_h * 0.75)} "
        f"scaleW={x} scaleH={max_h} pages=1 packed=0\n"
    )

    # page 行（注意：这里名字要和 OUT_IMG 一致）
    f.write(f'page id=0 file="{OUT_IMG}"\n')

    # chars
    f.write(f"chars count={len(chars)}\n")
    for c in chars:
        f.write(c + "\n")

print("DONE: font.fnt + font.png,All Caps")
