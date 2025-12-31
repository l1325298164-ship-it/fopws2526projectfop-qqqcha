from PIL import Image, ImageFilter
import numpy as np
import os
import random

# ===== 配置区 =====
INPUT_IMAGE = "decoration/menu_background.png"
OUTPUT_DIR = "tiles_stage_2"
TILE = 64               # 推荐 48 / 64
NOISE_STRENGTH = 0.5   # 越大越破碎（0.3~0.6）
BLUR_RADIUS = 2.5       # 越大边缘越柔
# ==================

os.makedirs(OUTPUT_DIR, exist_ok=True)

img = Image.open(INPUT_IMAGE).convert("RGBA")
width, height = img.size
tile_id = 0

for y in range(0, height, TILE):
    for x in range(0, width, TILE):
        tile = img.crop((x, y, x + TILE, y + TILE))

        # 1. 生成随机噪声
        noise = np.random.rand(TILE, TILE)

        # 2. 转成图片并模糊（关键）
        noise_img = Image.fromarray((noise * 255).astype("uint8"))
        noise_img = noise_img.filter(ImageFilter.GaussianBlur(BLUR_RADIUS))

        # 3. 阈值生成 mask
        mask = noise_img.point(lambda p: 255 if p / 255 > NOISE_STRENGTH else 0)

        # 4. 应用 alpha
        tile.putalpha(mask)

        tile.save(f"{OUTPUT_DIR}/tile_{tile_id}_{x}_{y}.png")
        tile_id += 1

print(f"done：{tile_id} pieces")
