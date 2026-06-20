import re

with open("src/main/java/com/example/glassmenu/util/MenuTranslator.java", "r") as f:
    code = f.read()

code = code.replace("ru.put(\"Radius: \", \"Радиус: \");", "ru.put(\"Radius: \", \"Радиус: \");\n        ru.put(\"Icon Size\", \"Размер указателя\");")

with open("src/main/java/com/example/glassmenu/util/MenuTranslator.java", "w") as f:
    f.write(code)
