package dev.invtools;

public final class Theme {
	private Theme() {}

	public static int hsv(float h, float s, float v) {
		h = h - (float) Math.floor(h);
		float c = v * s;
		float hp = h * 6f;
		float x = c * (1f - Math.abs(hp % 2f - 1f));
		float r = 0, g = 0, b = 0;
		switch ((int) hp) {
			case 0: r = c; g = x; break;
			case 1: r = x; g = c; break;
			case 2: g = c; b = x; break;
			case 3: g = x; b = c; break;
			case 4: r = x; b = c; break;
			default: r = c; b = x; break;
		}
		float m = v - c;
		int ri = (int) ((r + m) * 255f), gi = (int) ((g + m) * 255f), bi = (int) ((b + m) * 255f);
		return (ri << 16) | (gi << 8) | bi;
	}

	public static int lerp(int a, int b, float t) {
		int aa = (a >>> 24) & 255, ar = (a >> 16) & 255, ag = (a >> 8) & 255, ab = a & 255;
		int ba = (b >>> 24) & 255, br = (b >> 16) & 255, bg = (b >> 8) & 255, bb = b & 255;
		int ra = (int) (aa + (ba - aa) * t), rr = (int) (ar + (br - ar) * t);
		int rg = (int) (ag + (bg - ag) * t), rb = (int) (ab + (bb - ab) * t);
		return (ra << 24) | (rr << 16) | (rg << 8) | rb;
	}

	public static int top(ModConfig c) { return 0xD8000000 | hsv(c.color1Hue, c.color1Sat, 0.42f); }
	public static int bottom(ModConfig c) { return 0xD8000000 | hsv(c.color2Hue, c.color2Sat, 0.30f); }
	public static int accent(ModConfig c) { return 0xFF000000 | hsv(c.color1Hue, Math.max(0.35f, c.color1Sat), 1f); }
}
