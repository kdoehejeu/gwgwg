package dev.invtools;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class ModConfig {
	private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("invtools.json");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static ModConfig instance;

	// Disconnect-Verzögerung
	public boolean disconnectDelayEnabled = true;
	public int disconnectDelaySeconds = 3; // 1..10

	// Invsee-Hotkey
	public boolean invseeEnabled = true;
	public int invseeRange = 15;

	// Invsee-Counter
	public boolean counterEnabled = true;
	public boolean showTotems = true;
	public boolean showCrystals = true;
	public boolean showPearls = true;
	public boolean showWindCharges = true;

	// Anti-Leak (Erze + Spawner sehen aus wie normaler Deepslate)
	public boolean antiLeak = false;

	// Werkzeugschutz
	public boolean toolProtect = false;
	public int toolProtectPercent = 10; // 1..50

	// Hotkeys (GLFW-Tastencodes, -1 = keine Taste)
	public int invseeKey = 82; // R
	public int menuKey = 76;   // L

	// Menü-Design
	public int menuSizePercent = 70; // 50..100
	public float color1Hue = 0.97f;
	public float color1Sat = 0.45f;
	public float color2Hue = 0.60f;
	public float color2Sat = 0.25f;

	// Farben (ARGB)
	public int colorPanelBg = 0xC0101010;
	public int colorPanelBorder = 0xFF555555;
	public int colorTitle = 0xFFFFFFFF;
	public int colorTotem = 0xFFFFD700;
	public int colorCrystal = 0xFFFF55FF;
	public int colorPearl = 0xFF55FFFF;
	public int colorWind = 0xFFAAFFFF;
	public int colorCountdown = 0xFFFF5555;

	public static ModConfig get() {
		if (instance == null) load();
		return instance;
	}

	public static void load() {
		try {
			if (Files.exists(PATH)) {
				try (Reader r = Files.newBufferedReader(PATH)) {
					instance = GSON.fromJson(r, ModConfig.class);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (instance == null) instance = new ModConfig();
		instance.disconnectDelaySeconds = Math.max(1, Math.min(10, instance.disconnectDelaySeconds));
	}

	public static void save() {
		try (Writer w = Files.newBufferedWriter(PATH)) {
			GSON.toJson(get(), w);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void resetColors() {
		ModConfig d = new ModConfig();
		colorPanelBg = d.colorPanelBg;
		colorPanelBorder = d.colorPanelBorder;
		colorTitle = d.colorTitle;
		colorTotem = d.colorTotem;
		colorCrystal = d.colorCrystal;
		colorPearl = d.colorPearl;
		colorWind = d.colorWind;
		colorCountdown = d.colorCountdown;
	}
}
