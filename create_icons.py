import struct
import zlib
import os

def make_png(size, bg_r, bg_g, bg_b, fg_r, fg_g, fg_b):
    """Create a simple PNG with a colored circle on background."""
    
    def make_chunk(chunk_type, data):
        chunk_len = struct.pack('>I', len(data))
        chunk_data = chunk_type + data
        chunk_crc = struct.pack('>I', zlib.crc32(chunk_data) & 0xffffffff)
        return chunk_len + chunk_data + chunk_crc
    
    # PNG signature
    signature = b'\x89PNG\r\n\x1a\n'
    
    # IHDR chunk
    ihdr_data = struct.pack('>IIBBBBB', size, size, 8, 2, 0, 0, 0)
    ihdr = make_chunk(b'IHDR', ihdr_data)
    
    # Image data
    raw_data = bytearray()
    cx = size // 2
    cy = size // 2
    radius = int(size * 0.38)
    
    for y in range(size):
        raw_data.append(0)  # filter type: None
        for x in range(size):
            dist_sq = (x - cx) ** 2 + (y - cy) ** 2
            if dist_sq <= radius * radius:
                raw_data.extend([fg_r, fg_g, fg_b])
            else:
                raw_data.extend([bg_r, bg_g, bg_b])
    
    compressed = zlib.compress(bytes(raw_data), 9)
    idat = make_chunk(b'IDAT', compressed)
    
    # IEND chunk
    iend = make_chunk(b'IEND', b'')
    
    return signature + ihdr + idat + iend


# Icon sizes for each density
sizes = {
    'mipmap-mdpi': 48,
    'mipmap-hdpi': 72,
    'mipmap-xhdpi': 96,
    'mipmap-xxhdpi': 144,
    'mipmap-xxxhdpi': 192,
}

base = r'c:\Users\User\Desktop\Anzeigen\app\src\main\res'

for folder, size in sizes.items():
    path = os.path.join(base, folder)
    os.makedirs(path, exist_ok=True)
    
    # ic_launcher.png - purple background with white circle
    launcher_png = make_png(size, 103, 80, 164, 255, 255, 255)
    with open(os.path.join(path, 'ic_launcher.png'), 'wb') as f:
        f.write(launcher_png)
    
    # ic_launcher_round.png - same
    with open(os.path.join(path, 'ic_launcher_round.png'), 'wb') as f:
        f.write(launcher_png)
    
    # ic_launcher_foreground.png - white on transparent (use white bg)
    fg_png = make_png(size, 255, 255, 255, 103, 80, 164)
    with open(os.path.join(path, 'ic_launcher_foreground.png'), 'wb') as f:
        f.write(fg_png)
    
    print(f'OK: {folder} ({size}x{size}px) -> {path}')

print('\nAll icons created successfully!')
