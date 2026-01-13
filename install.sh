#!/bin/bash

APP_NAME="TerminalTodo"
INSTALL_DIR="$HOME/.local/share/terminal-todo"
DESKTOP_DIR="$HOME/.local/share/applications"
ICON_PATH="$INSTALL_DIR/icon.svg"
JAR_NAME="TerminalTodo.jar"

echo "🚧 Building $APP_NAME..."
chmod +x gradlew
./gradlew shadowJar

if [ $? -ne 0 ]; then
    echo "❌ Build failed."
    exit 1
fi

echo "📂 Creating install directories..."
mkdir -p "$INSTALL_DIR"
cp build/libs/TerminalTodo.jar "$INSTALL_DIR/$JAR_NAME"

echo "🎨 Generating Icon..."
cat <<EOF > "$ICON_PATH"
<svg width="128" height="128" viewBox="0 0 128 128" xmlns="http://www.w3.org/2000/svg">
  <rect x="10" y="10" width="108" height="108" rx="15" fill="#1d2021" stroke="#928374" stroke-width="4"/>
  <rect x="10" y="10" width="108" height="25" rx="15" fill="#928374"/>
  <circle cx="25" cy="22" r="5" fill="#cc241d"/>
  <circle cx="40" cy="22" r="5" fill="#d79921"/>
  <circle cx="55" cy="22" r="5" fill="#98971a"/>
  <text x="20" y="70" font-family="monospace" font-size="24" fill="#ebdbb2" font-weight="bold">&gt;_</text>
  <rect x="55" y="55" width="40" height="4" fill="#ebdbb2"/>
  <rect x="55" y="70" width="25" height="4" fill="#ebdbb2"/>
</svg>
EOF

echo "🚀 Creating Desktop Launcher..."
cat <<EOF > "$DESKTOP_DIR/terminal-todo.desktop"
[Desktop Entry]
Version=1.0
Type=Application
Name=Terminal Todo
Comment=Minimalist Daily Planner
Exec=java -jar $INSTALL_DIR/$JAR_NAME
Icon=$ICON_PATH
Categories=Utility;Office;Java;
Terminal=false
StartupNotify=true
EOF

update-desktop-database "$DESKTOP_DIR" 2>/dev/null
echo "✅ Installation Complete! Check your app launcher."