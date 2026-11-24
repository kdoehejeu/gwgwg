package dev.invtools;

import org.lwjgl.glfw.GLFW;

public final class Keys {
	private Keys() {}

	public static boolean down(long window, int key) {
		return key > 0 && GLFW.glfwGetKey(window, key) == GLFW.GLFW_PRESS;
	}

	public static String name(int key) {
		if (key <= 0) return "keine";
		switch (key) {
			case 32: return "Leertaste";
			case 256: return "Esc";
			case 257: return "Enter";
			case 258: return "Tab";
			case 259: return "Rücktaste";
			case 260: return "Einfg";
			case 261: return "Entf";
			case 262: return "Pfeil rechts";
			case 263: return "Pfeil links";
			case 264: return "Pfeil runter";
			case 265: return "Pfeil hoch";
			case 280: return "Feststelltaste";
			case 340: case 344: return "Shift";
			case 341: case 345: return "Strg";
			case 342: case 346: return "Alt";
			default: break;
		}
		if (key >= 290 && key <= 314) return "F" + (key - 289);
		String n = GLFW.glfwGetKeyName(key, 0);
		if (n != null && !n.isEmpty()) return n.toUpperCase();
		return "Taste " + key;
	}
}
