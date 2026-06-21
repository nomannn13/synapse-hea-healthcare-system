const db = require('../config/db');
const { writeAudit } = require('../utils/audit');

async function patientIdForUser(connection, userId) {
  const [rows] = await connection.execute(
    'SELECT patient_id FROM patients WHERE user_id = ?',
    [userId]
  );
  return rows[0]?.patient_id || null;
}

async function doctorIdForUser(connection, userId) {
  const [rows] = await connection.execute(
    'SELECT doctor_id FROM doctors WHERE user_id = ?',
    [userId]
  );
  return rows[0]?.doctor_id || null;
}

async function createNotification(connection, userId, title, message, type = 'system') {
  await connection.execute(
    `INSERT INTO notifications (user_id, title, message, type)
     VALUES (?, ?, ?, ?)`,
    [userId, title, message, type]
  );
}

async function getAvailability(req, res) {
  try {
    const doctorId = Number(req.query.doctor_id);
    const date = String(req.query.date || '');
    if (!doctorId || !/^\d{4}-\d{2}-\d{2}$/.test(date)) {
      return res.status(400).json({ success: false, message: 'Doctor and valid date are required' });
    }

    const [schedules] = await db.execute(
      `SELECT start_time, end_time
       FROM doctor_schedules
       WHERE doctor_id = ?
         AND day_of_week = DAYNAME(?)
         AND is_available = TRUE`,
      [doctorId, date]
    );
    if (schedules.length === 0) {
      return res.status(200).json({ success: true, data: { date, doctor_id: doctorId, slots: [] } });
    }

    const [booked] = await db.execute(
      `SELECT TIME_FORMAT(appointment_time, '%H:%i') AS appointment_time
       FROM appointments
       WHERE doctor_id = ? AND appointment_date = ?
         AND status NOT IN ('cancelled', 'rejected')`,
      [doctorId, date]
    );
    const bookedTimes = new Set(booked.map((row) => row.appointment_time));
    const slots = [];

    for (const schedule of schedules) {
      let current = new Date(`1970-01-01T${String(schedule.start_time)}`);
      const end = new Date(`1970-01-01T${String(schedule.end_time)}`);

      while (current < end) {
        const time = current.toTimeString().slice(0, 5);
        if (!bookedTimes.has(time)) slots.push({ time, available: true });
        current = new Date(current.getTime() + 30 * 60 * 1000);
      }
    }

    return res.status(200).json({
      success: true,
      data: { date, doctor_id: doctorId, slots }
    });
  } catch (error) {
    console.error('Availability error:', error);
    return res.status(500).json({ success: false, message: 'Could not load availability' });
  }
}

async function bookAppointment(req, res) {
  const connection = await db.getConnection();
  try {
    const doctorId = Number(req.body.doctor_id);
    const date = String(req.body.appointment_date || '');
    const time = String(req.body.appointment_time || '').slice(0, 5);
    const reason = String(req.body.reason || '').trim() || null;

    if (!doctorId || !/^\d{4}-\d{2}-\d{2}$/.test(date) || !/^\d{2}:\d{2}$/.test(time)) {
      return res.status(400).json({ success: false, message: 'Doctor, date and time are required' });
    }
    if (new Date(`${date}T${time}:00`) <= new Date()) {
      return res.status(400).json({ success: false, message: 'Appointment must be in the future' });
    }

    await connection.beginTransaction();
    const patientId = await patientIdForUser(connection, req.user.id);
    if (!patientId) {
      await connection.rollback();
      return res.status(404).json({ success: false, message: 'Patient profile not found' });
    }

    const [doctorRows] = await connection.execute(
      `SELECT d.doctor_id, d.user_id
       FROM doctors d
       JOIN users u ON u.user_id = d.user_id
       WHERE d.doctor_id = ? AND d.is_available = TRUE AND u.is_active = TRUE`,
      [doctorId]
    );
    if (doctorRows.length === 0) {
      await connection.rollback();
      return res.status(404).json({ success: false, message: 'Doctor is unavailable' });
    }

    const [scheduleRows] = await connection.execute(
      `SELECT schedule_id
       FROM doctor_schedules
       WHERE doctor_id = ? AND day_of_week = DAYNAME(?)
         AND is_available = TRUE
         AND ? >= start_time AND ADDTIME(?, '00:30:00') <= end_time`,
      [doctorId, date, time, time]
    );
    if (scheduleRows.length === 0) {
      await connection.rollback();
      return res.status(409).json({ success: false, message: 'Selected time is outside the doctor schedule' });
    }

    const [conflicts] = await connection.execute(
      `SELECT appointment_id
       FROM appointments
       WHERE doctor_id = ? AND appointment_date = ? AND appointment_time = ?
         AND status NOT IN ('cancelled', 'rejected')
       FOR UPDATE`,
      [doctorId, date, time]
    );
    if (conflicts.length > 0) {
      await connection.rollback();
      return res.status(409).json({ success: false, message: 'Time slot is already booked' });
    }

    const [result] = await connection.execute(
      `INSERT INTO appointments
       (patient_id, doctor_id, appointment_date, appointment_time, reason, status)
       VALUES (?, ?, ?, ?, ?, 'pending')`,
      [patientId, doctorId, date, time, reason]
    );

    await createNotification(
      connection,
      req.user.id,
      'Appointment request submitted',
      `Your request for ${date} at ${time} is awaiting doctor confirmation.`,
      'appointment'
    );
    await createNotification(
      connection,
      doctorRows[0].user_id,
      'New appointment request',
      `A patient requested an appointment on ${date} at ${time}.`,
      'appointment'
    );

    await connection.commit();

    await writeAudit({
      userId: req.user.id,
      action: 'APPOINTMENT_BOOK',
      entityType: 'appointments',
      entityId: result.insertId,
      req
    });

    return res.status(201).json({
      success: true,
      message: 'Appointment request submitted',
      data: {
        appointment_id: result.insertId,
        appointment_date: date,
        appointment_time: time,
        status: 'pending'
      }
    });
  } catch (error) {
    await connection.rollback();
    console.error('Book appointment error:', error);
    return res.status(500).json({ success: false, message: 'Could not book appointment' });
  } finally {
    connection.release();
  }
}

async function loadAppointmentForUpdate(connection, appointmentId) {
  const [rows] = await connection.execute(
    `SELECT a.*, p.user_id AS patient_user_id, d.user_id AS doctor_user_id
     FROM appointments a
     JOIN patients p ON p.patient_id = a.patient_id
     JOIN doctors d ON d.doctor_id = a.doctor_id
     WHERE a.appointment_id = ?
     FOR UPDATE`,
    [appointmentId]
  );
  return rows[0] || null;
}

function canModifyAppointment(user, appointment) {
  return user.role === 'admin'
    || (user.role === 'patient' && appointment.patient_user_id === user.id)
    || (user.role === 'doctor' && appointment.doctor_user_id === user.id);
}

async function cancelAppointment(req, res) {
  const connection = await db.getConnection();
  try {
    await connection.beginTransaction();
    const appointment = await loadAppointmentForUpdate(connection, req.params.appointment_id);
    if (!appointment) {
      await connection.rollback();
      return res.status(404).json({ success: false, message: 'Appointment not found' });
    }
    if (!canModifyAppointment(req.user, appointment)) {
      await connection.rollback();
      return res.status(403).json({ success: false, message: 'Access denied' });
    }
    if (['cancelled', 'completed', 'rejected'].includes(appointment.status)) {
      await connection.rollback();
      return res.status(409).json({ success: false, message: 'Appointment can no longer be cancelled' });
    }

    await connection.execute(
      `UPDATE appointments
       SET status = 'cancelled', notes = COALESCE(?, notes)
       WHERE appointment_id = ?`,
      [req.body.reason || null, appointment.appointment_id]
    );
    await createNotification(
      connection,
      appointment.patient_user_id,
      'Appointment cancelled',
      `The appointment on ${appointment.appointment_date} was cancelled.`,
      'cancellation'
    );
    await createNotification(
      connection,
      appointment.doctor_user_id,
      'Appointment cancelled',
      `The appointment on ${appointment.appointment_date} was cancelled.`,
      'cancellation'
    );
    await connection.commit();

    await writeAudit({
      userId: req.user.id,
      action: 'APPOINTMENT_CANCEL',
      entityType: 'appointments',
      entityId: appointment.appointment_id,
      req
    });

    return res.status(200).json({ success: true, message: 'Appointment cancelled' });
  } catch (error) {
    await connection.rollback();
    return res.status(500).json({ success: false, message: 'Could not cancel appointment' });
  } finally {
    connection.release();
  }
}

async function rescheduleAppointment(req, res) {
  const connection = await db.getConnection();
  try {
    const date = String(req.body.new_date || '');
    const time = String(req.body.new_time || '').slice(0, 5);
    if (!/^\d{4}-\d{2}-\d{2}$/.test(date) || !/^\d{2}:\d{2}$/.test(time)) {
      return res.status(400).json({ success: false, message: 'Valid new date and time are required' });
    }

    await connection.beginTransaction();
    const appointment = await loadAppointmentForUpdate(connection, req.params.appointment_id);
    if (!appointment) {
      await connection.rollback();
      return res.status(404).json({ success: false, message: 'Appointment not found' });
    }
    if (!canModifyAppointment(req.user, appointment)) {
      await connection.rollback();
      return res.status(403).json({ success: false, message: 'Access denied' });
    }

    const [conflicts] = await connection.execute(
      `SELECT appointment_id
       FROM appointments
       WHERE doctor_id = ? AND appointment_date = ? AND appointment_time = ?
         AND appointment_id <> ?
         AND status NOT IN ('cancelled', 'rejected')
       FOR UPDATE`,
      [appointment.doctor_id, date, time, appointment.appointment_id]
    );
    if (conflicts.length > 0) {
      await connection.rollback();
      return res.status(409).json({ success: false, message: 'New time slot is unavailable' });
    }

    await connection.execute(
      `UPDATE appointments
       SET appointment_date = ?, appointment_time = ?, status = 'rescheduled'
       WHERE appointment_id = ?`,
      [date, time, appointment.appointment_id]
    );
    await createNotification(
      connection,
      appointment.patient_user_id,
      'Appointment rescheduled',
      `The appointment moved to ${date} at ${time}.`,
      'appointment'
    );
    await createNotification(
      connection,
      appointment.doctor_user_id,
      'Appointment rescheduled',
      `The appointment moved to ${date} at ${time}.`,
      'appointment'
    );
    await connection.commit();

    await writeAudit({
      userId: req.user.id,
      action: 'APPOINTMENT_RESCHEDULE',
      entityType: 'appointments',
      entityId: appointment.appointment_id,
      newValue: { appointment_date: date, appointment_time: time },
      req
    });

    return res.status(200).json({ success: true, message: 'Appointment rescheduled' });
  } catch (error) {
    await connection.rollback();
    return res.status(500).json({ success: false, message: 'Could not reschedule appointment' });
  } finally {
    connection.release();
  }
}

async function getAppointments(req, res) {
  try {
    let where = '';
    let params = [];

    if (req.user.role === 'patient') {
      where = 'WHERE p.user_id = ?';
      params = [req.user.id];
    } else if (req.user.role === 'doctor') {
      where = 'WHERE d.user_id = ?';
      params = [req.user.id];
    }

    const [appointments] = await db.execute(
      `SELECT a.*,
              CONCAT(pu.first_name, ' ', pu.last_name) AS patient_name,
              CONCAT(du.first_name, ' ', du.last_name) AS doctor_name,
              d.specialization
       FROM appointments a
       JOIN patients p ON p.patient_id = a.patient_id
       JOIN users pu ON pu.user_id = p.user_id
       JOIN doctors d ON d.doctor_id = a.doctor_id
       JOIN users du ON du.user_id = d.user_id
       ${where}
       ORDER BY a.appointment_date DESC, a.appointment_time DESC`,
      params
    );

    return res.status(200).json({ success: true, data: appointments });
  } catch (error) {
    return res.status(500).json({ success: false, message: 'Could not load appointments' });
  }
}

module.exports = {
  getAvailability,
  bookAppointment,
  cancelAppointment,
  rescheduleAppointment,
  getAppointments
};
