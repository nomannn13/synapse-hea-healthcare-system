const test = require('node:test');
const assert = require('node:assert/strict');
const path = require('path');

const projectRoot = path.resolve(__dirname, '..');
const dbPath = path.join(projectRoot, 'backend', 'config', 'db.js');

const fakeConnection = {
  execute: async () => [[], []],
  getConnection: async () => ({
    execute: async () => [[], []],
    beginTransaction: async () => {},
    commit: async () => {},
    rollback: async () => {},
    release: () => {},
    ping: async () => {}
  }),
  testConnection: async () => true
};

require.cache[dbPath] = {
  id: dbPath,
  filename: dbPath,
  loaded: true,
  exports: fakeConnection
};

const { app } = require(path.join(projectRoot, 'backend', 'server.js'));

test('server exposes health and JSON 404 responses', async (t) => {
  const server = app.listen(0);
  t.after(() => server.close());
  await new Promise((resolve) => server.once('listening', resolve));
  const port = server.address().port;

  const health = await fetch(`http://127.0.0.1:${port}/api/health`);
  assert.equal(health.status, 200);
  assert.equal((await health.json()).success, true);

  const missing = await fetch(`http://127.0.0.1:${port}/api/does-not-exist`);
  assert.equal(missing.status, 404);
  assert.equal((await missing.json()).success, false);
});
