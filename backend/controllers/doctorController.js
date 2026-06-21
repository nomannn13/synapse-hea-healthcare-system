const db = require('../config/db');
const { writeAudit } = require('../utils/audit');

async function doctorIdForUser(userId) {
  const [rows] = await db.execute(
    'SELECT doctor_id FROM doctors WHERE user_id = ?',
    [userId]
  );
  return rows[0]?.doctor_id || null;
}

async function getDashboard(req, res) {
  try {
    const doctorId = await doctorIdForUser(req.user.id);
    if (!doctorId) {
      return res.status(404).json({ success: false, message: 'Doctor profile not found' });
    }

    const [[stats], [upcoming]] = await Promise.all([
      db.execute(
        `SELECT
           SUM(appointment_date = CURDATE() AND status <> 'cancelled') AS today,
           SUM(appointment_date >= CURDATE() AND status IN ('pending','confirmed','rescheduled')) AS upcoming,
           SUM(status = 'completed') AS completed,
           COUNT(DISTINCT patient_id) AS patients
         FROM appointments
         WHERE doctor_id = ?`,
        [doctorId]
      ),
      db.execute(
        `SELECT a.appointment_id, a.appointment_date, a.appointment_time,
                a.status, a.reason,
                CONCAT(u.first_name, ' ', u.last_name) AS patient_name
         FROM appointments a
         JOIN patients p ON p.patient_id = a.patient_id
         JOIN users u ON u.user_id = p.user_id
         WHERE a.doctor_id = ?
           AND a.appointment_date >= CURDATE()
           AND a.status <> 'cancelled'
         ORDER BY a.appointment_date, a.appointment_time
         LIMIT 8`,
        [doctorId]
      )
    ]);

    return res.status(200).json({
      success: true,
      data: {
        stats: {
          today: Number(stats[0]?.today || 0),
          upcoming: Number(stats[0]?.upcoming || 0),
          completed: Number(stats[0]?.completed || 0),
          patients: Number(stats[0]?.patients || 0)
        },
        upcomingAppointments: upcoming
      }
    });
  } catch (error) {
    console.error('Doctor dashboard error:', error);
    return res.status(500).json({ success: false, message: 'Could not load doctor dashboard' });
  }
}

async function getProfile(req, res) {
  try {
    const [rows] = await db.execute(
      `SELECT u.user_id, u.first_name, u.last_name, u.email, u.phone,
              d.doctor_id, d.specialization, d.license_number,
              d.years_experience, d.is_available, d.consultation_fee,
              dep.department_id, dep.name AS department_name
       FROM users u
       JOIN doctors d ON d.user_id = u.user_id
       LEFT JOIN departments dep ON dep.department_id = d.department_id
       WHERE u.user_id = ?`,
      [req.user.id]
    );
    if (rows.length === 0) {
      return res.status(404).json({ success: false, message: 'Doctor profile not found' });
    }
    return res.status(200).json({ success: true, data: rows[0] });
  } catch (error) {
    return res.status(500).json({ success: false, message: 'Could not load doctor profile' });
  }
}

async function updateProfile(req, res) {
  const connection = await db.getConnection();
  try {
    await connection.beginTransaction();
    await connection.execute(
      `UPDATE users
       SET first_name = COALESCE(?, first_name),
           last_name = COALESCE(?, last_name),
           phone = COALESCE(?, phone)
       WHERE user_id = ?`,
      [req.body.first_name || null, req.body.last_name || null, req.body.phone || null, req.user.id]
    );
    await connection.execute(
      `UPDATE doctors
       SET specialization = COALESCE(?, specialization),
           department_id = COALESCE(?, department_id),
           license_number = COALESCE(?, license_number),
           years_experience = COALESCE(?, years_experience),
           consultation_fee = COALESCE(?, consultation_fee),
           is_available = COALESCE(?, is_available)
       WHERE user_id = ?`,
      [
        req.body.specialization || null,
        req.body.department_id || null,
        req.body.license_number || null,
        req.body.years_experience ?? null,
        req.body.consultation_fee ?? null,
        req.body.is_available ?? null,
        req.user.id
      ]
    );
    await connection.commit();

    await writeAudit({
      userId: req.user.id,
      action: 'DOCTOR_PROFILE_UPDATE',
      entityType: 'doctors',
      req
    });

    return res.status(200).json({ success: true, message: 'Doctor profile updated' });
  } catch (error) {
    await connection.rollback();
    return res.status(500).json({ success: false, message: 'Could not update doctor profile' });
  } finally {
    connection.release();
  }
}

async function getSchedule(req, res) {
  try {
    const doctorId = await doctorIdForUser(req.user.id);
    const [schedule] = await db.execute(
      `SELECT schedule_id, day_of_week, start_time, end_time, is_available
       FROM doctor_schedules
       WHERE doctor_id = ?
       ORDER BY FIELD(day_of_week,'Monday','Tuesday','Wednesday','Thursday','Friday','Saturday','Sunday')`,
      [doctorId]
    );
    return res.status(200).json({ success: true, data: schedule });
  } catch (error) {
    return res.status(500).json({ success: false, message: 'Could not load schedule' });
  }
}

async function updateSchedule(req, res) {
  try {
    const doctorId = await doctorIdForUser(req.user.id);
    const { day_of_week, start_time, end_time, is_available = true } = req.body;
    const days = new Set(['Monday','Tuesday','Wednesday','Thursday','Friday','Saturday','Sunday']);

    if (!days.has(day_of_week) || !start_time || !end_time || start_time >= end_time) {
      return res.status(400).json({ success: false, message: 'Valid day and time range are required' });
    }

    await db.execute(
      `INSERT INTO doctor_schedules
       (doctor_id, day_of_week, start_time, end_time, is_available)
       VALUES (?, ?, ?, ?, ?)
       ON DUPLICATE KEY UPDATE
         start_time = VALUES(start_time),
         end_time = VALUES(end_time),
         is_available = VALUES(is_available)`,
      [doctorId, day_of_week, start_time, end_time, Boolean(is_available)]
    );

    await writeAudit({
      userId: req.user.id,
      action: 'DOCTOR_SCHEDULE_UPDATE',
      entityType: 'doctor_schedules',
      req
    });

    return res.status(200).json({ success: true, message: 'Schedule updated' });
  } catch (error) {
    return res.status(500).json({ success: false, message: 'Could not update schedule' });
  }
}

async function getAppointments(req, res) {
  try {
    const doctorId = await doctorIdForUser(req.user.id);
    const [appointments] = await db.execute(
      `SELECT a.*, CONCAT(u.first_name, ' ', u.last_name) AS patient_name,
              p.date_of_birth, p.blood_type
       FROM appointments a
       JOIN patients p ON p.patient_id = a.patient_id
       JOIN users u ON u.user_id = p.user_id
       WHERE a.doctor_id = ?
       ORDER BY a.appointment_date DESC, a.appointment_time DESC`,
      [doctorId]
    );
    return res.status(200).json({ success: true, data: appointments });
  } catch (error) {
    return res.status(500).json({ success: false, message: 'Could not load appointments' });
  }
}


async function updateAppointmentStatus(req, res) {
  const connection = await db.getConnection();
  try {
    const status = String(req.body.status || '');
    if (!['confirmed', 'rejected'].includes(status)) {
      return res.status(400).json({ success: false, message: 'Status must be confirmed or rejected' });
    }

    const doctorId = await doctorIdForUser(req.user.id);
    await connection.beginTransaction();
    const [rows] = await connection.execute(
      `SELECT a.appointment_id, a.status, p.user_id AS patient_user_id,
              a.appointment_date, a.appointment_time
       FROM appointments a
       JOIN patients p ON p.patient_id = a.patient_id
       WHERE a.appointment_id = ? AND a.doctor_id = ?
       FOR UPDATE`,
      [req.params.id, doctorId]
    );
    if (rows.length === 0) {
      await connection.rollback();
      return res.status(404).json({ success: false, message: 'Appointment not found' });
    }
    if (!['pending', 'rescheduled'].includes(rows[0].status)) {
      await connection.rollback();
      return res.status(409).json({ success: false, message: 'Appointment has already been processed' });
    }

    await connection.execute(
      'UPDATE appointments SET status = ? WHERE appointment_id = ?',
      [status, req.params.id]
    );
    await connection.execute(
      `INSERT INTO notifications (user_id, title, message, type)
       VALUES (?, ?, ?, 'appointment')`,
      [
        rows[0].patient_user_id,
        status === 'confirmed' ? 'Appointment confirmed' : 'Appointment rejected',
        `Your appointment on ${rows[0].appointment_date} at ${rows[0].appointment_time} was ${status}.`
      ]
    );
    await connection.commit();

    await writeAudit({
      userId: req.user.id,
      action: `APPOINTMENT_${status.toUpperCase()}`,
      entityType: 'appointments',
      entityId: Number(req.params.id),
      req
    });

    return res.status(200).json({ success: true, message: `Appointment ${status}` });
  } catch (error) {
    await connection.rollback();
    return res.status(500).json({ success: false, message: 'Could not update appointment status' });
  } finally {
    connection.release();
  }
}

async function completeAppointment(req, res) {
  const connection = await db.getConnection();
  try {
    const doctorId = await doctorIdForUser(req.user.id);
    await connection.beginTransaction();

    const [appointments] = await connection.execute(
      `SELECT appointment_id, patient_id, status
       FROM appointments
       WHERE appointment_id = ? AND doctor_id = ?
       FOR UPDATE`,
      [req.params.id, doctorId]
    );
    if (appointments.length === 0) {
      await connection.rollback();
      return res.status(404).json({ success: false, message: 'Appointment not found' });
    }
    if (appointments[0].status === 'cancelled') {
      await connection.rollback();
      return res.status(409).json({ success: false, message: 'Cancelled appointment cannot be completed' });
    }

    await connection.execute(
      `UPDATE appointments
       SET status = 'completed', notes = COALESCE(?, notes)
       WHERE appointment_id = ?`,
      [req.body.notes || null, req.params.id]
    );
    await connection.commit();

    await writeAudit({
      userId: req.user.id,
      action: 'APPOINTMENT_COMPLETED',
      entityType: 'appointments',
      entityId: Number(req.params.id),
      req
    });

    return res.status(200).json({ success: true, message: 'Appointment completed' });
  } catch (error) {
    await connection.rollback();
    return res.status(500).json({ success: false, message: 'Could not complete appointment' });
  } finally {
    connection.release();
  }
}

async function getMyPatients(req, res) {
  try {
    const doctorId = await doctorIdForUser(req.user.id);
    const [patients] = await db.execute(
      `SELECT DISTINCT p.patient_id, u.first_name, u.last_name, u.email,
              p.date_of_birth, p.gender, p.blood_type
       FROM appointments a
       JOIN patients p ON p.patient_id = a.patient_id
       JOIN users u ON u.user_id = p.user_id
       WHERE a.doctor_id = ?
       ORDER BY u.first_name, u.last_name`,
      [doctorId]
    );
    return res.status(200).json({ success: true, data: patients });
  } catch (error) {
    return res.status(500).json({ success: false, message: 'Could not load patients' });
  }
}

module.exports = {
  getDashboard,
  getProfile,
  updateProfile,
  getSchedule,
  updateSchedule,
  getAppointments,
  updateAppointmentStatus,
  completeAppointment,
  getMyPatients
};
