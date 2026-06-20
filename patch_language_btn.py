import re

with open("src/main/java/com/example/glassmenu/screen/LiquidGlassScreen.java", "r") as f:
    content = f.read()

btn_code = """
        // Language Toggle
        String langText = "Language / Язык: " + GlassMenuClient.CONFIG.menuLanguage().name();
        LiquidGlassButton langBtn = new LiquidGlassButton((int)x + 280, (int)y + 150, 150, 22, Text.literal(langText), b -> {
            com.example.glassmenu.GlassMenuConfigModel.MenuLanguage currentLang = GlassMenuClient.CONFIG.menuLanguage();
            com.example.glassmenu.GlassMenuConfigModel.MenuLanguage nextLang;
            if (currentLang == com.example.glassmenu.GlassMenuConfigModel.MenuLanguage.AUTO) nextLang = com.example.glassmenu.GlassMenuConfigModel.MenuLanguage.ENGLISH;
            else if (currentLang == com.example.glassmenu.GlassMenuConfigModel.MenuLanguage.ENGLISH) nextLang = com.example.glassmenu.GlassMenuConfigModel.MenuLanguage.RUSSIAN;
            else nextLang = com.example.glassmenu.GlassMenuConfigModel.MenuLanguage.AUTO;
            
            GlassMenuClient.CONFIG.menuLanguage(nextLang);
            GlassMenuClient.CONFIG.save();
            b.setMessage(Text.literal("Language / Язык: " + nextLang.name()));
        });
        generalWidgets.add(langBtn);
"""

# Inject before the end of initGeneralTab
match = re.search(r'(private void initGeneralTab[^}]+?this\.effectView\.disableBlur\(\);\n\s*\}\n\s*\});\n\s*generalWidgets\.add\(menuGlassBtn\);\n)', content)
if match:
    content = content.replace(match.group(1), match.group(1) + btn_code)
    with open("src/main/java/com/example/glassmenu/screen/LiquidGlassScreen.java", "w") as f:
        f.write(content)
    print("Added Language button")
else:
    print("Could not find initGeneralTab end")
