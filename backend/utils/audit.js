const db = require('../config/db');

async function writeAudit({
  userId = null,
  action,
  entityType = null,
  entityId = null,
  oldValue = null,
  newValue = null,
  status = 'success',
  req = null
}) {
  try {
    await db.execute(
      `INSERT INTO audit_logs
       (user_id, action, entity_type, entity_id, old_value, new_value,
        ip_address, user_agent, status)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)`,
      [
        userId,
        action,
        entityType,
        entityId,
        oldValue ? JSON.stringify(oldValue) : null,
        newValue ? JSON.stringify(newValue) : null,
        req?.ip || null,
        req?.get?.('user-agent') || null,
        status
      ]
    );
  } catch (error) {
    console.error('Audit log failed:', error.message);
  }
}

module.exports = { writeAudit };
