import os
import zipfile

yarn_sources = None
for root, dirs, files in os.walk('/home/waster/.gradle/caches/fabric-loom'):
    for f in files:
        if f.endswith('-sources.jar') and 'minecraft' in f and '1.21' in f:
            yarn_sources = os.path.join(root, f)
            break
    if yarn_sources:
        break

if yarn_sources:
    with zipfile.ZipFile(yarn_sources, 'r') as z:
        for name in z.namelist():
            if 'EntityRenderer.java' in name and 'client/render/entity' in name:
                content = z.read(name).decode('utf-8')
                lines = content.split('\n')
                in_method = False
                for i, line in enumerate(lines):
                    if 'protected void renderLabelIfPresent(' in line:
                        in_method = True
                    if in_method:
                        print(line)
                        if line.strip() == '}':
                            in_method = False
                            break
