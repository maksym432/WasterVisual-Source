import zipfile
import subprocess
import os

jar_path = os.path.expanduser("~/.gradle/caches/fabric-loom/1.21.1/minecraft-merged.jar")
if not os.path.exists(jar_path):
    print("Jar not found")
    exit(1)

with zipfile.ZipFile(jar_path, 'r') as z:
    for name in z.namelist():
        if name.endswith("EntityRenderer.class"):
            print("Extracting " + name)
            z.extract(name, "/tmp")
            subprocess.run(["javap", "-c", "-p", "/tmp/" + name])
            break
