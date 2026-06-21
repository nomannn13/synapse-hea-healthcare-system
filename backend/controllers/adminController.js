const db = require('../config/db');
const { writeAudit } = require('../utils/audit');

async function getDashboard(req, res) {
  try {
    const [[users], [patients], [doctors], [appointments], [unreadLogs]] = await Promise.all([
      db.execute('SELECT COUNT(*) AS total FROM users'),
      db.execute('SELECT COUNT(*) AS total FROM patients'),
      db.execute('SELECT COUNT(*) AS total FROM doctors'),
      db.execute('SELECT COUNT(*) AS total FROM appointments'),
      db.execute("SELECT COUNT(*) AS total FROM audit_logs WHERE status IN ('failed','suspicious')")
    ]);

    return res.status(200).json({
      success: true,
      data: {
        totalUsers: Number(users[0].total),
        totalPatients: Number(patients[0].total),
        totalDoctors: Number(doctors[0].total),
        totalAppointments: Number(appointments[0].total),
        flaggedAuditEvents: Number(unreadLogs[0].total)
      }
    });
  } catch (error) {
    return res.status(500).json({ success: false, message: 'Could not load admin dashboard' });
  }
}

async function getReports(req, res) {
  try {
    const [[appointments], [resources], [departments]] = await Promise.all([
      db.execute(
        `SELECT status, COUNT(*) AS total
         FROM appointments GROUP BY status ORDER BY status`
      ),
      db.execute(
        `SELECT type, status, COUNT(*) AS total
         FROM resources GROUP BY type, status ORDER BY type, status`
      ),
      db.execute(
        `SELECT dep.name, COUNT(d.doctor_id) AS doctors
         FROM departments dep
         LEFT JOIN doctors d ON d.department_id = dep.department_id
         GROUP BY dep.department_id, dep.name
         ORDER BY dep.name`
      )
    ]);

    return res.status(200).json({
      success: true,
      data: { appointments, resources, departments }
    });
  } catch (error) {
    return res.status(500).json({ success: false, message: 'Could not generate reports' });
  }
}

async function getResources(req, res) {
  try {
    const [resources] = await db.execute(
      `SELECT r.*, dep.name AS department_name
       FROM resources r
       LEFT JOIN departments dep ON dep.department_id = r.department_id
       ORDER BY r.type, r.name`
    );
    return res.status(200).json({ success: true, data: resources });
  } catch (error) {
    return res.status(500).json({ success: false, message: 'Could not load resources' });
  }
}

async function getAuditLogs(req, res) {
  try {
    const limit = Math.min(Number(req.query.limit || 100), 200);
    const [logs] = await db.execute(
      `SELECT al.*, CONCAT(u.first_name, ' ', u.last_name) AS user_name, u.email
       FROM audit_logs al
       LEFT JOIN users u ON u.user_id = al.user_id
       ORDER BY al.created_at DESC
       LIMIT ?`,
      [limit]
    );
    return res.status(200).json({ success: true, data: logs });
  } catch (error) {
    return res.status(500).json({ success: false, message: 'Could not load audit logs' });
  }
}

async function assignRoom(req, res) {
  const connection = await db.getConnection();
  try {
    const appointmentId = Number(req.body.appointment_id);
    const roomId = Number(req.body.room_id);
    if (!appointmentId || !roomId) {
      return res.status(400).json({ success: false, message: 'Appointment and room are required' });
    }

    await connection.beginTransaction();
    const [rooms] = await connection.execute(
      `SELECT resource_id, status FROM resources
       WHERE resource_id = ? AND type = 'room'
       FOR UPDATE`,
      [roomId]
    );
    if (rooms.length === 0 || rooms[0].status !== 'available') {
      await connection.rollback();
      return res.status(409).json({ success: false, message: 'Room is not available' });
    }

    const [result] = await connection.execute(
      'UPDATE appointments SET room_id = ? WHERE appointment_id = ?',
      [roomId, appointmentId]
    );
    if (result.affectedRows === 0) {
      await connection.rollback();
      return res.status(404).json({ success: false, message: 'Appointment not found' });
    }
    await connection.execute(
      "UPDATE resources SET status = 'occupied' WHERE resource_id = ?",
      [roomId]
    );
    await connection.commit();

    await writeAudit({
      userId: req.user.id,
      action: 'ROOM_ASSIGN',
      entityType: 'appointments',
      entityId: appointmentId,
      newValue: { room_id: roomId },
      req
    });

    return res.status(200).json({ success: true, message: 'Room assigned successfully' });
  } catch (error) {
    await connection.rollback();
    return res.status(500).json({ success: false, message: 'Could not assign room' });
  } finally {
    connection.release();
  }
}

async function manageUser(req, res) {
  try {
    const userId = Number(req.body.user_id);
    const action = String(req.body.action || '');
    const role = req.body.role;
    const validRoles = new Set(['patient', 'doctor', 'admin']);

    if (!userId || !['activate', 'deactivate', 'update_role'].includes(action)) {
      return res.status(400).json({ success: false, message: 'Valid user and action are required' });
    }
    if (userId === req.user.id && action === 'deactivate') {
      return res.status(409).json({ success: false, message: 'You cannot deactivate your own account' });
    }
    if (action === 'update_role' && !validRoles.has(role)) {
      return res.status(400).json({ success: false, message: 'Invalid role' });
    }

    const [result] = action === 'update_role'
      ? await db.execute('UPDATE users SET role = ? WHERE user_id = ?', [role, userId])
      : await db.execute('UPDATE users SET is_active = ? WHERE user_id = ?', [action === 'activate', userId]);

    if (result.affectedRows === 0) {
      return res.status(404).json({ success: false, message: 'User not found' });
    }

    await writeAudit({
      userId: req.user.id,
      action: `USER_${action.toUpperCase()}`,
      entityType: 'users',
      entityId: userId,
      newValue: role ? { role } : { is_active: action === 'activate' },
      req
    });

    return res.status(200).json({ success: true, message: 'User updated successfully' });
  } catch (error) {
    return res.status(500).json({ success: false, message: 'Could not update user' });
  }
}

module.exports = {
  getDashboard,
  getReports,
  getResources,
  getAuditLogs,
  assignRoom,
  manageUser
};
