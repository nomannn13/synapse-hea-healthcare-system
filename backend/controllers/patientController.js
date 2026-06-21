const db = require('../config/db');
const { writeAudit } = require('../utils/audit');

async function patientIdForUser(userId) {
  const [rows] = await db.execute(
    'SELECT patient_id FROM patients WHERE user_id = ?',
    [userId]
  );
  return rows[0]?.patient_id || null;
}

async function getDashboard(req, res) {
  try {
    const patientId = await patientIdForUser(req.user.id);
    if (!patientId) {
      return res.status(404).json({ success: false, message: 'Patient profile not found' });
    }

    const [[stats], [upcomingAppointments], [recentRecords], [notificationCount]] =
      await Promise.all([
        db.execute(
          `SELECT
             COUNT(*) AS total,
             SUM(status = 'completed') AS completed,
             SUM(status IN ('pending', 'confirmed', 'rescheduled')) AS upcoming,
             SUM(status = 'cancelled') AS cancelled
           FROM appointments
           WHERE patient_id = ?`,
          [patientId]
        ),
        db.execute(
          `SELECT a.appointment_id, a.appointment_date, a.appointment_time,
                  a.status, a.reason,
                  CONCAT(u.first_name, ' ', u.last_name) AS doctor_name,
                  d.specialization
           FROM appointments a
           JOIN doctors d ON d.doctor_id = a.doctor_id
           JOIN users u ON u.user_id = d.user_id
           WHERE a.patient_id = ?
             AND a.appointment_date >= CURDATE()
             AND a.status <> 'cancelled'
           ORDER BY a.appointment_date, a.appointment_time
           LIMIT 5`,
          [patientId]
        ),
        db.execute(
          `SELECT mr.record_id, mr.diagnosis, mr.prescription, mr.created_at,
                  CONCAT(u.first_name, ' ', u.last_name) AS doctor_name
           FROM medical_records mr
           JOIN doctors d ON d.doctor_id = mr.doctor_id
           JOIN users u ON u.user_id = d.user_id
           WHERE mr.patient_id = ?
           ORDER BY mr.created_at DESC
           LIMIT 3`,
          [patientId]
        ),
        db.execute(
          'SELECT COUNT(*) AS unread FROM notifications WHERE user_id = ? AND is_read = FALSE',
          [req.user.id]
        )
      ]);

    return res.status(200).json({
      success: true,
      data: {
        stats: {
          total: Number(stats[0]?.total || 0),
          completed: Number(stats[0]?.completed || 0),
          upcoming: Number(stats[0]?.upcoming || 0),
          cancelled: Number(stats[0]?.cancelled || 0)
        },
        upcomingAppointments,
        recentRecords,
        unreadNotifications: Number(notificationCount[0]?.unread || 0)
      }
    });
  } catch (error) {
    console.error('Patient dashboard error:', error);
    return res.status(500).json({ success: false, message: 'Could not load patient dashboard' });
  }
}

async function getProfile(req, res) {
  try {
    const [rows] = await db.execute(
      `SELECT u.user_id, u.first_name, u.last_name, u.email, u.phone, u.role,
              p.date_of_birth, p.gender, p.blood_type, p.address,
              p.emergency_contact, p.emergency_phone, p.insurance_number
       FROM users u
       JOIN patients p ON p.user_id = u.user_id
       WHERE u.user_id = ?`,
      [req.user.id]
    );
    if (rows.length === 0) {
      return res.status(404).json({ success: false, message: 'Patient profile not found' });
    }
    return res.status(200).json({ success: true, data: rows[0] });
  } catch (error) {
    return res.status(500).json({ success: false, message: 'Could not load profile' });
  }
}

async function updateProfile(req, res) {
  const connection = await db.getConnection();
  try {
    const allowedGender = new Set(['male', 'female', 'other']);
    const gender = req.body.gender || null;
    if (gender && !allowedGender.has(gender)) {
      return res.status(400).json({ success: false, message: 'Invalid gender value' });
    }

    await connection.beginTransaction();
    await connection.execute(
      `UPDATE users
       SET first_name = COALESCE(?, first_name),
           last_name = COALESCE(?, last_name),
           phone = COALESCE(?, phone)
       WHERE user_id = ?`,
      [
        req.body.first_name || null,
        req.body.last_name || null,
        req.body.phone || null,
        req.user.id
      ]
    );
    await connection.execute(
      `UPDATE patients
       SET address = COALESCE(?, address),
           date_of_birth = COALESCE(?, date_of_birth),
           gender = COALESCE(?, gender),
           blood_type = COALESCE(?, blood_type),
           emergency_contact = COALESCE(?, emergency_contact),
           emergency_phone = COALESCE(?, emergency_phone),
           insurance_number = COALESCE(?, insurance_number)
       WHERE user_id = ?`,
      [
        req.body.address || null,
        req.body.date_of_birth || null,
        gender,
        req.body.blood_type || null,
        req.body.emergency_contact || null,
        req.body.emergency_phone || null,
        req.body.insurance_number || null,
        req.user.id
      ]
    );
    await connection.commit();

    await writeAudit({
      userId: req.user.id,
      action: 'PATIENT_PROFILE_UPDATE',
      entityType: 'patients',
      req
    });

    return res.status(200).json({ success: true, message: 'Profile updated successfully' });
  } catch (error) {
    await connection.rollback();
    console.error('Update profile error:', error);
    return res.status(500).json({ success: false, message: 'Could not update profile' });
  } finally {
    connection.release();
  }
}

async function getAppointmentHistory(req, res) {
  try {
    const patientId = await patientIdForUser(req.user.id);
    if (!patientId) {
      return res.status(404).json({ success: false, message: 'Patient profile not found' });
    }
    const [appointments] = await db.execute(
      `SELECT a.*, CONCAT(u.first_name, ' ', u.last_name) AS doctor_name,
              d.specialization, dep.name AS department_name
       FROM appointments a
       JOIN doctors d ON d.doctor_id = a.doctor_id
       JOIN users u ON u.user_id = d.user_id
       LEFT JOIN departments dep ON dep.department_id = d.department_id
       WHERE a.patient_id = ?
       ORDER BY a.appointment_date DESC, a.appointment_time DESC`,
      [patientId]
    );
    return res.status(200).json({ success: true, data: appointments });
  } catch (error) {
    return res.status(500).json({ success: false, message: 'Could not load appointment history' });
  }
}

async function getDoctors(req, res) {
  try {
    const [doctors] = await db.execute(
      `SELECT d.doctor_id, d.specialization, d.consultation_fee,
              CONCAT(u.first_name, ' ', u.last_name) AS name,
              dep.name AS department_name
       FROM doctors d
       JOIN users u ON u.user_id = d.user_id
       LEFT JOIN departments dep ON dep.department_id = d.department_id
       WHERE u.is_active = TRUE AND d.is_available = TRUE
       ORDER BY u.first_name, u.last_name`
    );
    return res.status(200).json({ success: true, data: doctors });
  } catch (error) {
    return res.status(500).json({ success: false, message: 'Could not load doctors' });
  }
}

async function getNotifications(req, res) {
  try {
    const [notifications] = await db.execute(
      `SELECT notification_id, title, message, type, is_read, created_at
       FROM notifications
       WHERE user_id = ?
       ORDER BY created_at DESC
       LIMIT 50`,
      [req.user.id]
    );
    return res.status(200).json({ success: true, data: notifications });
  } catch (error) {
    return res.status(500).json({ success: false, message: 'Could not load notifications' });
  }
}

async function markNotificationRead(req, res) {
  try {
    const [result] = await db.execute(
      `UPDATE notifications SET is_read = TRUE
       WHERE notification_id = ? AND user_id = ?`,
      [req.params.id, req.user.id]
    );
    if (result.affectedRows === 0) {
      return res.status(404).json({ success: false, message: 'Notification not found' });
    }
    return res.status(200).json({ success: true, message: 'Notification marked as read' });
  } catch (error) {
    return res.status(500).json({ success: false, message: 'Could not update notification' });
  }
}

module.exports = {
  getDashboard,
  getProfile,
  updateProfile,
  getAppointmentHistory,
  getDoctors,
  getNotifications,
  markNotificationRead
};
