const test = require('node:test');
const assert = require('node:assert/strict');
const path = require('path');

const projectRoot = path.resolve(__dirname, '..');
const dbPath = path.join(projectRoot, 'backend', 'config', 'db.js');

const fakeConnection = {
  execute: async () => [[], []],
  beginTransaction: async () => {},
  commit: async () => {},
  rollback: async () => {},
  release: () => {},
  ping: async () => {}
};

require.cache[dbPath] = {
  id: dbPath,
  filename: dbPath,
  loaded: true,
  exports: {
    execute: fakeConnection.execute,
    getConnection: async () => fakeConnection,
    testConnection: async () => true
  }
};

function routeMap(routeFile) {
  const router = require(path.join(projectRoot, 'backend', 'routes', routeFile));
  return router.stack
    .filter((layer) => layer.route)
    .map((layer) => ({
      path: layer.route.path,
      method: Object.keys(layer.route.methods)[0].toUpperCase()
    }));
}

test('all expected route modules expose their API contracts', () => {
  assert.deepEqual(routeMap('authRoutes.js'), [
    { method: 'POST', path: '/register' },
    { method: 'POST', path: '/login' },
    { method: 'POST', path: '/verify-login-otp' },
    { method: 'POST', path: '/logout' },
    { method: 'GET', path: '/me' },
    { method: 'POST', path: '/change-password' }
  ]);

  assert.equal(routeMap('patientRoutes.js').length, 7);
  assert.equal(routeMap('doctorRoutes.js').length, 9);
  assert.equal(routeMap('adminRoutes.js').length, 6);
  assert.equal(routeMap('appointmentRoutes.js').length, 5);
  assert.equal(routeMap('medicalRoutes.js').length, 4);
});

test('role middleware rejects a user with the wrong role', () => {
  const { authorize } = require(path.join(projectRoot, 'backend', 'middleware', 'roleMiddleware.js'));
  let statusCode = null;
  let body = null;
  let nextCalled = false;

  authorize('admin')(
    { user: { role: 'patient' } },
    {
      status(code) {
        statusCode = code;
        return this;
      },
      json(value) {
        body = value;
        return value;
      }
    },
    () => { nextCalled = true; }
  );

  assert.equal(statusCode, 403);
  assert.equal(body.success, false);
  assert.equal(nextCalled, false);
});
