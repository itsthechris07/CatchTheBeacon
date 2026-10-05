# Branding

Images for the plugin pages on SpigotMC, Modrinth and Hangar.

| File | Size | Use |
|---|---|---|
| `icon.png` | 512×512 | Project icon (Modrinth, Hangar, SpigotMC) |
| `banner.png` | 1600×500 | Header at the top of the description |
| `how-it-works.png` | 1600×900 | Gallery / description: map schematic + the three steps of a round |
| `features.png` | 1600×900 | Gallery / description: feature overview |

## Editing

The sources are in `design/` (one `.dc.html` per image, `canvas.json` holds the sizes). They were made on a
Claude design canvas and are plain HTML with inline styles (fonts: Chakra Petch + Barlow from Google Fonts).

After a change, render the PNGs again (needs Chrome or Edge):

```powershell
.\branding\render.ps1
```

Colors: background `#0D1120`, panels `#151B2E`, red `#F0505A`, blue `#4B95FF`, beacon `#7FF5E8`,
fatigue/highlight `#F5B83D`.
