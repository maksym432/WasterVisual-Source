import re

with open("src/main/java/com/example/glassmenu/util/MenuTranslator.java", "r") as f:
    code = f.read()

# Add translation keys
translations = """        ru.put("Player Radar", "Радар Игроков");
        ru.put("Player Radar Settings", "Настройки Радара Игроков");
        ru.put("RGB Arrows", "RGB Стрелки");
        ru.put("Arrow Color", "Цвет Стрелки");
        ru.put("Radius: ", "Радиус: ");
        ru.put("Radius", "Радиус");
"""

code = code.replace("        ru.put(\"Red\", \"Красный\");", "        ru.put(\"Red\", \"Красный\");\n" + translations)

with open("src/main/java/com/example/glassmenu/util/MenuTranslator.java", "w") as f:
    f.write(code)
