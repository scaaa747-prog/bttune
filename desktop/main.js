const { app, BrowserWindow, ipcMain, protocol } = require('electron');
const path = require('path');
const fs = require('fs');
const https = require('https');

let mainWindow;

function createWindow() {
  mainWindow = new BrowserWindow({
    width: 1280,
    height: 800,
    minWidth: 900,
    minHeight: 600,
    title: 'BTTUNE',
    backgroundColor: '#0D0D11',
    frame: true,
    autoHideMenuBar: true,
    webPreferences: {
      nodeIntegration: true,
      contextIsolation: false,
      webSecurity: false
    }
  });

  mainWindow.loadFile(path.join(__dirname, 'index.html'));
}

// In-Memory & Persistent Audio Cache directory
const CACHE_DIR = path.join(app.getPath('userData'), 'BTTUNECache');
if (!fs.existsSync(CACHE_DIR)) {
  fs.mkdirSync(CACHE_DIR, { recursive: true });
}

ipcMain.handle('get-cache-path', async (event, songId) => {
  const filePath = path.join(CACHE_DIR, `${songId}.mp3`);
  if (fs.existsSync(filePath)) {
    return filePath;
  }
  return null;
});

ipcMain.handle('save-to-cache', async (event, { songId, buffer }) => {
  try {
    const filePath = path.join(CACHE_DIR, `${songId}.mp3`);
    fs.writeFileSync(filePath, Buffer.from(buffer));
    return true;
  } catch (err) {
    console.error('Cache save error:', err);
    return false;
  }
});

app.whenReady().then(() => {
  createWindow();

  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) createWindow();
  });
});

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') app.quit();
});
