const { app, BrowserWindow, dialog } = require('electron');
const { spawn } = require('child_process');
const path = require('path');
const fs = require('fs');
const waitOn = require('wait-on');

let mainWindow;
let splashWindow;
let javaProcess;

function createSplash() {
  splashWindow = new BrowserWindow({
    width: 420,
    height: 320,
    transparent: true,
    frame: false,
    resizable: false,
    alwaysOnTop: true,
    icon: path.join(__dirname, 'resources', 'icon.ico'),
    webPreferences: {
      nodeIntegration: true,
      contextIsolation: false
    }
  });
  splashWindow.loadFile(path.join(__dirname, 'resources', 'splash.html'));
  splashWindow.center();
}

function createWindow() {
  mainWindow = new BrowserWindow({
    width: 1280,
    height: 860,
    minWidth: 900,
    minHeight: 600,
    icon: path.join(__dirname, 'resources', 'icon.ico'),
    autoHideMenuBar: true,
    show: false,
    webPreferences: {
      nodeIntegration: false,
      contextIsolation: true
    }
  });

  // Handle downloads cleanly
  mainWindow.webContents.session.on('will-download', (event, item) => {
    const defaultPath = path.join(app.getPath('downloads'), item.getFilename());
    item.setSaveDialogOptions({ defaultPath });

    item.on('updated', (event, state) => {
      if (state === 'progressing' && !item.isPaused()) {
        console.log(`Received bytes: ${item.getReceivedBytes()}`);
      }
    });
    item.once('done', (event, state) => {
      console.log(state === 'completed' ? 'Download complete' : `Download failed: ${state}`);
    });
  });

  mainWindow.on('closed', () => { mainWindow = null; });

  // Show window only after content has loaded to prevent flash
  mainWindow.once('ready-to-show', () => {
    mainWindow.show();
    if (splashWindow && !splashWindow.isDestroyed()) {
      splashWindow.destroy();
      splashWindow = null;
    }
  });
}

function updateSplashStatus(message) {
  if (splashWindow && !splashWindow.isDestroyed()) {
    splashWindow.webContents.executeJavaScript(
      `document.getElementById('status') && (document.getElementById('status').textContent = '${message}')`
    ).catch(() => {});
  }
}

app.on('ready', () => {
  createSplash();

  const isDev = !app.isPackaged;
  const basePath = isDev ? __dirname : process.resourcesPath;
  const PORT = 8081;

  const javaBinaryName = process.platform === 'win32' ? 'java.exe' : 'java';
  const bundledJava = path.join(basePath, 'jre', 'bin', javaBinaryName);
  const javaExe = fs.existsSync(bundledJava) ? bundledJava : javaBinaryName;
  const warFile = path.join(basePath, 'qpss.war');

  console.log(`Starting Java: ${javaExe} -jar ${warFile} --server.port=${PORT}`);
  updateSplashStatus('Starting server...');

  javaProcess = spawn(javaExe, ['-jar', warFile, `--server.port=${PORT}`], { cwd: basePath });

  javaProcess.stdout.on('data', (data) => {
    const line = data.toString();
    console.log(`[Java]: ${line}`);
    if (line.includes('Starting')) updateSplashStatus('Initializing application...');
    if (line.includes('HikariPool') || line.includes('H2')) updateSplashStatus('Connecting to database...');
    if (line.includes('Tomcat started')) updateSplashStatus('Almost ready...');
  });

  javaProcess.stderr.on('data', (data) => {
    console.error(`[Java Error]: ${data}`);
  });

  javaProcess.on('exit', (code) => {
    console.log(`Java process exited with code ${code}`);
    if (!mainWindow || mainWindow.isDestroyed()) {
      if (splashWindow && !splashWindow.isDestroyed()) {
        updateSplashStatus('Server failed to start');
      }
      dialog.showErrorBox('Server Error', 'The internal server stopped unexpectedly. Please restart the application.');
      app.quit();
    }
  });

  // Wait for Spring Boot with generous timeout and progress updates
  const opts = {
    resources: [`http://127.0.0.1:${PORT}`],
    delay: 500,
    interval: 800,
    timeout: 120000,
    window: 1000,
  };

  let dotCount = 0;
  const progressInterval = setInterval(() => {
    dotCount = (dotCount + 1) % 4;
    const dots = '.'.repeat(dotCount + 1);
    updateSplashStatus(`Starting server${dots}`);
  }, 1500);

  waitOn(opts)
    .then(() => {
      clearInterval(progressInterval);
      updateSplashStatus('Loading interface...');
      console.log('Server is up, opening window...');
      createWindow();
      mainWindow.loadURL(`http://127.0.0.1:${PORT}`);
    })
    .catch((err) => {
      clearInterval(progressInterval);
      console.error('Error waiting for server:', err);
      if (splashWindow && !splashWindow.isDestroyed()) {
        splashWindow.destroy();
        splashWindow = null;
      }
      dialog.showErrorBox(
        'Server Timeout',
        'The server did not start within 2 minutes.\n\nPlease check that Java is installed and try restarting the application.'
      );
      app.quit();
    });
});

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    app.quit();
  }
});

app.on('will-quit', () => {
  if (javaProcess && !javaProcess.killed) {
    console.log('Killing Java process...');
    try {
      spawn('taskkill', ['/pid', javaProcess.pid.toString(), '/f', '/t']);
    } catch (e) {
      javaProcess.kill('SIGTERM');
    }
  }
});
