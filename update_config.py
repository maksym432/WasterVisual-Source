import re

with open("src/main/java/com/example/glassmenu/GlassMenuConfigModel.java", "r") as f:
    content = f.read()

enum_code = """
    public MenuLanguage menuLanguage = MenuLanguage.AUTO;

    public enum MenuLanguage {
        AUTO, ENGLISH, RUSSIAN
    }
"""

if "enum MenuLanguage" not in content:
    content = content.replace("public boolean enableDynamicIsland = true;", enum_code + "\n    public boolean enableDynamicIsland = true;")
    with open("src/main/java/com/example/glassmenu/GlassMenuConfigModel.java", "w") as f:
        f.write(content)
    print("Added MenuLanguage to GlassMenuConfigModel")
else:
    print("MenuLanguage already exists")
