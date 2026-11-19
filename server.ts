import express from 'express';
import {spawn, spawnSync, type ChildProcess} from 'node:child_process';
import fs from 'node:fs';
import path from 'node:path';
import {createServer as createViteServer} from 'vite';

const PORT = 3000;
const JAVA_BRIDGE_PORT = Number(process.env.JAVA_BRIDGE_PORT || 8085);
const JAVA_BRIDGE_URL = `http://127.0.0.1:${JAVA_BRIDGE_PORT}`;
const SHADED_JAR = path.resolve(process.cwd(), 'target/gaming-gear-management-1.0.0.jar');
const PORTABLE_JAVA = path.resolve(process.cwd(), '.jre17/bin/java');

let javaProcess: ChildProcess | null = null;
let javaBootError: string | null = null;

const JRE_MODULES_FILE = path.resolve(process.cwd(), '.jre17/lib/modules');
const TEMURIN_JRE17_URL =
  'https://api.adoptium.net/v3/binary/latest/17/ga/linux/x64/jre/hotspot/normal/eclipse?project=jdk';

function hasWorkingJava(bin: string): boolean {
  const probe = spawnSync(bin, ['-version'], {stdio: 'ignore'});
  return !probe.error && probe.status === 0;
}

function resolveJavaBinary(): string {
  if (process.env.JAVA_BIN && fs.existsSync(process.env.JAVA_BIN)) {
    return process.env.JAVA_BIN;
  }

  if (fs.existsSync(PORTABLE_JAVA) && fs.existsSync(JRE_MODULES_FILE)) {
    try {
      fs.chmodSync(PORTABLE_JAVA, 0o755);
      const jspawnhelper = path.resolve(process.cwd(), '.jre17/lib/jspawnhelper');
      if (fs.existsSync(jspawnhelper)) {
        fs.chmodSync(jspawnhelper, 0o755);
      }
    } catch {
      // ignore chmod errors if owned by another user
    }
    if (hasWorkingJava(PORTABLE_JAVA)) {
      return PORTABLE_JAVA;
    }
  }

  if (hasWorkingJava('java')) {
    return 'java';
  }

  // Self-heal portable JRE 17 if workspace snapshot stripped large binary files like lib/modules
  console.log('[server] Restoring portable Temurin JRE 17...');
  const jreDir = path.resolve(process.cwd(), '.jre17');
  const tmpTar = '/tmp/temurin-jre17.tar.gz';
  const restore = spawnSync(
    'sh',
    [
      '-c',
      `rm -rf "${jreDir}" && mkdir -p "${jreDir}" && curl -fsSL "${TEMURIN_JRE17_URL}" -o "${tmpTar}" && tar -xzf "${tmpTar}" -C "${jreDir}" --strip-components=1 && rm -f "${tmpTar}" && chmod +x "${jreDir}/bin/"* "${jreDir}/lib/jspawnhelper"`,
    ],
    {cwd: process.cwd(), stdio: 'inherit'},
  );
  if (restore.status === 0 && hasWorkingJava(PORTABLE_JAVA)) {
    return PORTABLE_JAVA;
  }

  return 'java';
}

function ensureJavaJarBuilt(): void {
  if (fs.existsSync(SHADED_JAR)) {
    return;
  }
  console.log('[server] Building Java 17 shaded JAR via Maven...');
  const res = spawnSync('mvn', ['-q', 'package', '-DskipTests'], {
    cwd: process.cwd(),
    stdio: 'inherit',
  });
  if (res.error || res.status !== 0 || !fs.existsSync(SHADED_JAR)) {
    throw new Error(
      res.error
        ? `Maven unavailable to build JAR (${res.error.message})`
        : 'Failed to build Java JAR with Maven',
    );
  }
}

async function waitForJavaHealth(timeoutMs = 20000): Promise<void> {
  const start = Date.now();
  while (Date.now() - start < timeoutMs) {
    if (javaBootError) {
      throw new Error(javaBootError);
    }
    try {
      const resp = await fetch(`${JAVA_BRIDGE_URL}/api/java/health`);
      if (resp.ok) {
        return;
      }
    } catch {
      // retry until ready
    }
    await new Promise((r) => setTimeout(r, 200));
  }
  throw new Error(`JavaApiBridgeServer did not respond on ${JAVA_BRIDGE_URL} within ${timeoutMs}ms`);
}

async function startJavaBridge(): Promise<void> {
  javaBootError = null;
  try {
    const alreadyUp = await fetch(`${JAVA_BRIDGE_URL}/api/java/health`);
    if (alreadyUp.ok) {
      console.log(`[server] JavaApiBridgeServer already listening on ${JAVA_BRIDGE_URL}`);
      return;
    }
  } catch {
    // not running yet
  }

  ensureJavaJarBuilt();

  const javaBin = resolveJavaBinary();
  console.log(`[server] Starting JavaApiBridgeServer using ${javaBin}`);

  javaProcess = spawn(
    javaBin,
    ['-Djava.awt.headless=true', '-cp', SHADED_JAR, 'com.gaminggear.web.JavaApiBridgeServer'],
    {
      cwd: process.cwd(),
      env: {...process.env, JAVA_BRIDGE_PORT: String(JAVA_BRIDGE_PORT)},
      stdio: ['ignore', 'pipe', 'pipe'],
    },
  );

  javaProcess.on('error', (err) => {
    javaBootError = `Failed to spawn Java process (${javaBin}): ${err.message}`;
    console.error(`[server] ${javaBootError}`);
    javaProcess = null;
  });

  javaProcess.stdout?.on('data', (chunk) => {
    process.stdout.write(`[java] ${chunk}`);
  });
  javaProcess.stderr?.on('data', (chunk) => {
    process.stderr.write(`[java-err] ${chunk}`);
  });
  javaProcess.on('exit', (code) => {
    console.log(`[server] JavaApiBridgeServer exited with code ${code}`);
    if (code !== 0 && code !== null && !javaBootError) {
      javaBootError = `JavaApiBridgeServer exited prematurely with code ${code}`;
    }
    javaProcess = null;
  });

  await waitForJavaHealth();
  console.log(`[server] JavaApiBridgeServer connected at ${JAVA_BRIDGE_URL}`);
}

function cleanupJava(): void {
  if (javaProcess && !javaProcess.killed) {
    javaProcess.kill('SIGTERM');
    javaProcess = null;
  }
}

process.on('SIGINT', () => {
  cleanupJava();
  process.exit(0);
});
process.on('SIGTERM', () => {
  cleanupJava();
  process.exit(0);
});
process.on('exit', cleanupJava);

async function bootstrap(): Promise<void> {
  try {
    await startJavaBridge();
  } catch (err) {
    console.error('[server] Warning starting Java bridge:', err);
  }

  const app = express();
  app.use(express.json({limit: '2mb'}));

  // Thin proxy forwarding all /api/java/* requests directly to Java 17 backend
  app.use('/api/java', async (req, res) => {
    try {
      const targetUrl = `${JAVA_BRIDGE_URL}${req.originalUrl}`;
      const method = req.method.toUpperCase();
      const headers: Record<string, string> = {};
      if (req.headers['content-type']) {
        headers['Content-Type'] = String(req.headers['content-type']);
      }
      const hasBody = !['GET', 'HEAD'].includes(method);
      const body = hasBody ? JSON.stringify(req.body ?? {}) : undefined;

      const upstream = await fetch(targetUrl, {
        method,
        headers,
        body,
      });

      res.status(upstream.status);
      const contentType = upstream.headers.get('content-type');
      if (contentType) {
        res.setHeader('Content-Type', contentType);
      }
      const disposition = upstream.headers.get('content-disposition');
      if (disposition) {
        res.setHeader('Content-Disposition', disposition);
      }

      const arrayBuffer = await upstream.arrayBuffer();
      res.send(Buffer.from(arrayBuffer));
    } catch (error) {
      const msg = error instanceof Error ? error.message : 'Java bridge proxy error';
      res.status(502).json({error: `Không kết nối được tới máy chủ Java: ${msg}`});
    }
  });

  if (process.env.NODE_ENV === 'production') {
    const distDir = path.resolve(process.cwd(), 'dist');
    app.use(express.static(distDir));
    app.get('*', (_req, res) => {
      res.sendFile(path.join(distDir, 'index.html'));
    });
  } else {
    const vite = await createViteServer({
      server: {middlewareMode: true},
      appType: 'spa',
    });
    app.use(vite.middlewares);
  }

  app.listen(PORT, '0.0.0.0', () => {
    console.log(`[server] Express + Vite running at http://0.0.0.0:${PORT}`);
  });
}

bootstrap().catch((err) => {
  console.error('[server] Fatal startup error:', err);
  cleanupJava();
  process.exit(1);
});
