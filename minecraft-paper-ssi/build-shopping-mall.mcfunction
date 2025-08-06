# SSI Metaverse Shopping Mall Builder
# Run this in Minecraft server console or with command blocks

# Clear the area first (100x100x50 area)
fill ~-50 ~-5 ~-50 ~50 ~45 ~50 air

# === FOUNDATION & GROUND FLOOR ===
# Main foundation (100x100)
fill ~-50 ~-1 ~-50 ~50 ~-1 ~50 quartz_block

# Floor pattern (checkered with polished quartz)
fill ~-50 ~ ~-50 ~50 ~ ~50 polished_quartz_block
fill ~-48 ~ ~-48 ~-46 ~ ~-46 white_concrete
fill ~-44 ~ ~-48 ~-42 ~ ~-46 white_concrete
fill ~-40 ~ ~-48 ~-38 ~ ~-46 white_concrete

# === EXTERIOR WALLS ===
# Main walls (4 sides)
fill ~-50 ~ ~-50 ~50 ~30 ~-50 quartz_block_slab  # North wall
fill ~-50 ~ ~50 ~50 ~30 ~50 quartz_block_slab    # South wall
fill ~-50 ~ ~-50 ~-50 ~30 ~50 quartz_block_slab  # West wall
fill ~50 ~ ~-50 ~50 ~30 ~50 quartz_block_slab    # East wall

# === MAIN ENTRANCE ===
# Grand entrance (15 blocks wide, 8 blocks tall)
fill ~-7 ~1 ~-50 ~7 ~8 ~-50 air
fill ~-8 ~1 ~-50 ~-8 ~8 ~-50 quartz_pillar
fill ~8 ~1 ~-50 ~8 ~8 ~-50 quartz_pillar

# Entrance archway
fill ~-7 ~8 ~-50 ~7 ~10 ~-50 quartz_stairs[facing=north]

# === GROUND FLOOR SHOPS ===
# Shop 1 - Electronics Store (Left side)
fill ~-45 ~1 ~-45 ~-25 ~5 ~-25 glass
fill ~-45 ~1 ~-45 ~-45 ~5 ~-25 white_concrete
fill ~-45 ~1 ~-25 ~-25 ~5 ~-25 white_concrete
fill ~-25 ~1 ~-45 ~-25 ~5 ~-25 white_concrete
fill ~-44 ~1 ~-44 ~-26 ~1 ~-26 polished_blackstone

# Shop 2 - Fashion Store
fill ~-20 ~1 ~-45 ~0 ~5 ~-25 glass
fill ~-20 ~1 ~-45 ~-20 ~5 ~-25 pink_concrete
fill ~-20 ~1 ~-25 ~0 ~5 ~-25 pink_concrete
fill ~0 ~1 ~-45 ~0 ~5 ~-25 pink_concrete
fill ~-19 ~1 ~-44 ~-1 ~1 ~-26 pink_wool

# Shop 3 - Bookstore/Digital Goods
fill ~5 ~1 ~-45 ~25 ~5 ~-25 glass
fill ~5 ~1 ~-45 ~5 ~5 ~-25 brown_concrete
fill ~5 ~1 ~-25 ~25 ~5 ~-25 brown_concrete
fill ~25 ~1 ~-45 ~25 ~5 ~-25 brown_concrete
fill ~6 ~1 ~-44 ~24 ~1 ~-26 dark_oak_planks

# Shop 4 - Jewelry/Luxury
fill ~30 ~1 ~-45 ~45 ~5 ~-25 glass
fill ~30 ~1 ~-45 ~30 ~5 ~-25 gold_block
fill ~30 ~1 ~-25 ~45 ~5 ~-25 gold_block
fill ~45 ~1 ~-45 ~45 ~5 ~-25 gold_block
fill ~31 ~1 ~-44 ~44 ~1 ~-26 yellow_carpet

# === CENTRAL ATRIUM ===
# Open area in center
fill ~-15 ~1 ~-15 ~15 ~25 ~15 air

# Central fountain base
fill ~-5 ~1 ~-5 ~5 ~3 ~5 prismarine_bricks
fill ~-3 ~3 ~-3 ~3 ~5 ~3 water

# Fountain decoration
setblock ~ ~5 ~ sea_lantern

# === SECOND FLOOR ===
# Second floor platform
fill ~-45 ~10 ~-45 ~45 ~10 ~45 quartz_block

# Shop 5 - Gaming/VR Store
fill ~-45 ~11 ~-45 ~-25 ~15 ~-25 glass
fill ~-45 ~11 ~-45 ~-45 ~15 ~-25 purple_concrete
fill ~-45 ~11 ~-25 ~-25 ~15 ~-25 purple_concrete
fill ~-25 ~11 ~-45 ~-25 ~15 ~-25 purple_concrete
fill ~-44 ~11 ~-44 ~-26 ~11 ~-26 purple_carpet

# Shop 6 - Sports Equipment
fill ~-20 ~11 ~-45 ~0 ~15 ~-25 glass
fill ~-20 ~11 ~-45 ~-20 ~15 ~-25 lime_concrete
fill ~-20 ~11 ~-25 ~0 ~15 ~-25 lime_concrete
fill ~0 ~11 ~-45 ~0 ~15 ~-25 lime_concrete
fill ~-19 ~11 ~-44 ~-1 ~11 ~-26 lime_wool

# Shop 7 - Home & Garden
fill ~5 ~11 ~-45 ~25 ~15 ~-25 glass
fill ~5 ~11 ~-45 ~5 ~15 ~-25 green_concrete
fill ~5 ~11 ~-25 ~25 ~15 ~-25 green_concrete
fill ~25 ~11 ~-45 ~25 ~15 ~-25 green_concrete
fill ~6 ~11 ~-44 ~24 ~11 ~-26 grass_block

# Shop 8 - Art Gallery
fill ~30 ~11 ~-45 ~45 ~15 ~-25 glass
fill ~30 ~11 ~-45 ~30 ~15 ~-25 white_concrete
fill ~30 ~11 ~-25 ~45 ~15 ~-25 white_concrete
fill ~45 ~11 ~-45 ~45 ~15 ~-25 white_concrete
fill ~31 ~11 ~-44 ~44 ~11 ~-26 white_carpet

# === SOUTH SIDE SHOPS (Ground Floor) ===
# Shop 9 - Food Court
fill ~-45 ~1 ~25 ~-25 ~5 ~45 glass
fill ~-45 ~1 ~25 ~-45 ~5 ~45 orange_concrete
fill ~-45 ~1 ~45 ~-25 ~5 ~45 orange_concrete
fill ~-25 ~1 ~25 ~-25 ~5 ~45 orange_concrete
fill ~-44 ~1 ~26 ~-26 ~1 ~44 orange_wool

# Shop 10 - Pharmacy/Health
fill ~-20 ~1 ~25 ~0 ~5 ~45 glass
fill ~-20 ~1 ~25 ~-20 ~5 ~45 light_blue_concrete
fill ~-20 ~1 ~45 ~0 ~5 ~45 light_blue_concrete
fill ~0 ~1 ~25 ~0 ~5 ~45 light_blue_concrete
fill ~-19 ~1 ~26 ~-1 ~1 ~44 light_blue_wool

# Shop 11 - Music Store
fill ~5 ~1 ~25 ~25 ~5 ~45 glass
fill ~5 ~1 ~25 ~5 ~5 ~45 note_block
fill ~5 ~1 ~45 ~25 ~5 ~45 note_block
fill ~25 ~1 ~25 ~25 ~5 ~45 note_block
fill ~6 ~1 ~26 ~24 ~1 ~44 red_carpet

# Shop 12 - Travel Agency
fill ~30 ~1 ~25 ~45 ~5 ~45 glass
fill ~30 ~1 ~25 ~30 ~5 ~45 cyan_concrete
fill ~30 ~1 ~45 ~45 ~5 ~45 cyan_concrete
fill ~45 ~1 ~25 ~45 ~5 ~45 cyan_concrete
fill ~31 ~1 ~26 ~44 ~1 ~44 cyan_wool

# === ESCALATORS/STAIRS ===
# Main staircase (West side)
fill ~-48 ~1 ~-20 ~-46 ~1 ~-10 quartz_stairs[facing=north]
fill ~-48 ~2 ~-19 ~-46 ~2 ~-11 quartz_stairs[facing=north]
fill ~-48 ~3 ~-18 ~-46 ~3 ~-12 quartz_stairs[facing=north]
fill ~-48 ~4 ~-17 ~-46 ~4 ~-13 quartz_stairs[facing=north]
fill ~-48 ~5 ~-16 ~-46 ~5 ~-14 quartz_stairs[facing=north]
fill ~-48 ~6 ~-15 ~-46 ~6 ~-15 quartz_block
fill ~-48 ~7 ~-15 ~-46 ~10 ~-15 quartz_block

# East side staircase
fill ~46 ~1 ~-20 ~48 ~1 ~-10 quartz_stairs[facing=north]
fill ~46 ~2 ~-19 ~48 ~2 ~-11 quartz_stairs[facing=north]
fill ~46 ~3 ~-18 ~48 ~3 ~-12 quartz_stairs[facing=north]
fill ~46 ~4 ~-17 ~48 ~4 ~-13 quartz_stairs[facing=north]
fill ~46 ~5 ~-16 ~48 ~5 ~-14 quartz_stairs[facing=north]
fill ~46 ~6 ~-15 ~48 ~6 ~-15 quartz_block
fill ~46 ~7 ~-15 ~48 ~10 ~-15 quartz_block

# === CEILING & ROOF ===
# Ground floor ceiling
fill ~-50 ~6 ~-50 ~50 ~6 ~50 quartz_slab

# Second floor ceiling  
fill ~-50 ~16 ~-50 ~50 ~16 ~50 quartz_slab

# Roof structure
fill ~-52 ~30 ~-52 ~52 ~32 ~52 quartz_block

# === LIGHTING ===
# Ground floor lighting
setblock ~-40 ~4 ~-35 glowstone
setblock ~-15 ~4 ~-35 glowstone
setblock ~10 ~4 ~-35 glowstone
setblock ~35 ~4 ~-35 glowstone

setblock ~-40 ~4 ~35 glowstone
setblock ~-15 ~4 ~35 glowstone
setblock ~10 ~4 ~35 glowstone
setblock ~35 ~4 ~35 glowstone

# Central atrium lighting
setblock ~0 ~8 ~0 chandelier
setblock ~-10 ~8 ~0 sea_lantern
setblock ~10 ~8 ~0 sea_lantern
setblock ~0 ~8 ~-10 sea_lantern
setblock ~0 ~8 ~10 sea_lantern

# Second floor lighting
setblock ~-40 ~14 ~-35 glowstone
setblock ~-15 ~14 ~-35 glowstone
setblock ~10 ~14 ~-35 glowstone
setblock ~35 ~14 ~-35 glowstone

# === SHOP SIGNS ===
setblock ~-35 ~3 ~-50 oak_wall_sign[facing=south]{Text1:'{"text":"ELECTRONICS","color":"blue","bold":true}',Text2:'{"text":"Latest Tech","color":"dark_blue"}',Text3:'{"text":"Gadgets & More","color":"dark_blue"}'}

setblock ~-10 ~3 ~-50 oak_wall_sign[facing=south]{Text1:'{"text":"FASHION","color":"light_purple","bold":true}',Text2:'{"text":"Trendy Clothes","color":"purple"}',Text3:'{"text":"Style & Beauty","color":"purple"}'}

setblock ~15 ~3 ~-50 oak_wall_sign[facing=south]{Text1:'{"text":"BOOKSTORE","color":"brown","bold":true}',Text2:'{"text":"Digital Books","color":"dark_red"}',Text3:'{"text":"Knowledge Hub","color":"dark_red"}'}

setblock ~37 ~3 ~-50 oak_wall_sign[facing=south]{Text1:'{"text":"LUXURY","color":"gold","bold":true}',Text2:'{"text":"Fine Jewelry","color":"yellow"}',Text3:'{"text":"Premium Items","color":"yellow"}'}

# === DIRECTORY BOARD ===
setblock ~0 ~3 ~-10 oak_sign{Text1:'{"text":"MALL DIRECTORY","color":"dark_green","bold":true}',Text2:'{"text":"Ground Floor:","color":"green"}',Text3:'{"text":"Tech, Fashion,","color":"green"}',Text4:'{"text":"Books, Luxury","color":"green"}'}

setblock ~0 ~4 ~-10 oak_sign{Text1:'{"text":"Second Floor:","color":"green"}',Text2:'{"text":"Gaming, Sports,","color":"green"}',Text3:'{"text":"Home, Art","color":"green"}',Text4:'{"text":"","color":"green"}'}

# === WELCOME MESSAGE ===
say Mall construction complete! Welcome to the SSI Metaverse Shopping Mall!
say Features: 12 shops across 2 floors, central atrium, escalators, and directory
say Enjoy exploring the virtual shopping experience!