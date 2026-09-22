const { app, BrowserWindow, shell } = require('electron');
const path = require('path');

function createWindow() {
  const mainWindow = new BrowserWindow({
    width: 1366,
    height: 868,
    minWidth: 1000,
    minHeight: 680,
    title: 'Trace Learning System',
    webPreferences: {
      nodeIntegration: false,
      contextIsolation: true
    }
  });

  const devUrl = process.env.TRACE_APP_URL || 'http://localhost:3000';
  mainWindow.loadURL(devUrl).catch(() => {
    // Fallback to cloud HF Space if local port 3000 is not running
    mainWindow.loadURL('https://huggingface.co/spaces/Agent606/Edu-AI');
  });

  mainWindow.webContents.setWindowOpenHandler(({ url }) => {
    shell.openExternal(url);
    return { action: 'deny' };
  });
}

app.whenReady().then(() => {
  createWindow();

  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) createWindow();
  });
});

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') app.quit();
});
