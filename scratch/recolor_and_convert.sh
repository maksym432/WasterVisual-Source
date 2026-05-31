#!/bin/bash
set -e

DEST_DIR="src/main/resources/assets/glassmenu/textures/gui/symbolic_icons"
mkdir -p "$DEST_DIR"

declare -A ICONS
ICONS[play]="Adwaita/symbolic/actions/media-playback-start-symbolic.svg"
ICONS[pause]="Adwaita/symbolic/actions/media-playback-pause-symbolic.svg"
ICONS[prev]="Adwaita/symbolic/actions/media-seek-backward-symbolic.svg"
ICONS[next]="Adwaita/symbolic/actions/media-seek-forward-symbolic.svg"
ICONS[shuffle]="Adwaita/symbolic/status/media-playlist-shuffle-symbolic.svg"
ICONS[consecutive]="Adwaita/symbolic/status/media-playlist-consecutive-symbolic.svg"
ICONS[repeat]="Adwaita/symbolic/status/media-playlist-repeat-symbolic.svg"
ICONS[repeat_song]="Adwaita/symbolic/status/media-playlist-repeat-song-symbolic.svg"
ICONS[mail_forward]="Adwaita/symbolic/actions/mail-forward-symbolic.svg"

for key in "${!ICONS[@]}"; do
    path="${ICONS[$key]}"
    url="https://gitlab.gnome.org/GNOME/adwaita-icon-theme/-/raw/master/$path"
    echo "Processing $key..."
    # Download
    curl -s -f -o temp.svg "$url"
    # Recolor: replace fill="#2e3436" or fill="#2E3436" or black fills with white
    sed -i 's/fill="#2e3436"/fill="#ffffff"/g' temp.svg
    sed -i 's/fill="#2E3436"/fill="#ffffff"/g' temp.svg
    # Convert to 128x128 PNG
    rsvg-convert -w 128 -h 128 temp.svg -o "$DEST_DIR/$key.png"
done

rm -f temp.svg
echo "Successfully downloaded, recolored, and converted all icons!"
