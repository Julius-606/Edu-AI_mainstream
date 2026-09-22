# 💻 Trace Desktop Suite (Windows & Linux)

## Overview
The `desktop/` directory contains complete desktop launchers and packagers for Windows and Linux.

## Running in Development

### Windows
```cmd
cd desktop
run_windows.bat
```

### Linux
```bash
cd desktop
chmod +x run_linux.sh
./run_linux.sh
```

## Packaging as Standalone Executable (PyInstaller)

To build a standalone `.exe` (Windows) or binary (Linux) with no Python installation required by end users:
```bash
cd desktop
pip install -r requirements.txt
pyinstaller --noconfirm --onedir --windowed --name "Trace_Learning_System" main.py
```
The output executable will be placed in `desktop/dist/Trace_Learning_System/`.

## Packaging with Electron
If preferred, you can build cross-platform native installers via Electron:
```bash
cd desktop
npm install
npm run pack:win    # Builds Windows .exe installer
npm run pack:linux  # Builds Linux AppImage & .deb
```
