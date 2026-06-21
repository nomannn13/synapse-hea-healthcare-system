const test = require('node:test');
const assert = require('node:assert/strict');
const path = require('path');

const projectRoot = path.resolve(__dirname, '..');
const dbPath = path.join(projectRoot, 'backend', 'config', 'db.js');

test('public registration always creates a patient role', async () => {
  const executed = [];
  const connection = {
    beginTransaction: async () => {},
    commit: async () => {},
    rollback: async () => {},
    release: () => {},
    execute: async (sql, params) => {
      executed.push({ sql, params });
      if (sql.includes('INSERT INTO users')) return [{ insertId: 42 }, []];
      return [[], []];
    }
  };

  const fakeDb = {
    execute: async (sql, params) => {
      executed.push({ sql, params });
      return [[], []];
    },
    getConnection: async () => connection
  };

  require.cache[dbPath] = {
    id: dbPath,
    filename: dbPath,
    loaded: true,
    exports: fakeDb
  };

  const controllerPath = path.join(projectRoot, 'backend', 'controllers', 'authController.js');
  delete require.cache[controllerPath];
  const { register } = require(controllerPath);

  let statusCode;
  let response;
  await register(
    {
      body: {
        first_name: 'Test',
        last_name: 'User',
        email: 'test@example.com',
        password: 'SecurePass123!',
        role: 'admin'
      },
      ip: '127.0.0.1',
      get: () => 'test'
    },
    {
      status(code) {
        statusCode = code;
        return this;
      },
      json(value) {
        response = value;
        return value;
      }
    }
  );

  assert.equal(statusCode, 201);
  assert.equal(response.success, true);
  const insert = executed.find((item) => item.sql.includes('INSERT INTO users'));
  assert.ok(insert.sql.includes("'patient'"));
  assert.equal(insert.params.includes('admin'), false);
});
