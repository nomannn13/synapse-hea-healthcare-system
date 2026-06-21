const bcrypt = require('bcryptjs');
const db = require('../config/db');

async function upsertUser(connection, user) {
  const passwordHash = await bcrypt.hash(user.password, 12);
  const [existing] = await connection.execute(
    'SELECT user_id FROM users WHERE email = ?',
    [user.email]
  );

  if (existing.length > 0) {
    await connection.execute(
      `UPDATE users
       SET first_name = ?, last_name = ?, password = ?, role = ?, is_active = TRUE
       WHERE user_id = ?`,
      [user.firstName, user.lastName, passwordHash, user.role, existing[0].user_id]
    );
    return existing[0].user_id;
  }

  const [result] = await connection.execute(
    `INSERT INTO users
     (first_name, last_name, email, password, role, is_active)
     VALUES (?, ?, ?, ?, ?, TRUE)`,
    [user.firstName, user.lastName, user.email, passwordHash, user.role]
  );
  return result.insertId;
}

async function seed() {
  const connection = await db.getConnection();
  try {
    await connection.beginTransaction();

    const adminId = await upsertUser(connection, {
      firstName: 'System',
      lastName: 'Administrator',
      email: 'admin@hea.local',
      password: process.env.DEMO_ADMIN_PASSWORD || 'AdminDemo123!',
      role: 'admin'
    });

    const doctorUserId = await upsertUser(connection, {
      firstName: 'Amina',
      lastName: 'Rossi',
      email: 'doctor@hea.local',
      password: process.env.DEMO_DOCTOR_PASSWORD || 'DoctorDemo123!',
      role: 'doctor'
    });

    const patientUserId = await upsertUser(connection, {
      firstName: 'Demo',
      lastName: 'Patient',
      email: 'patient@hea.local',
      password: process.env.DEMO_PATIENT_PASSWORD || 'PatientDemo123!',
      role: 'patient'
    });

    const [[department]] = await connection.execute(
      "SELECT department_id FROM departments WHERE name = 'General Medicine'"
    );

    await connection.execute(
      `INSERT INTO doctors (user_id, department_id, specialization, license_number, years_experience)
       VALUES (?, ?, 'General Practice', 'DEMO-GP-001', 8)
       ON DUPLICATE KEY UPDATE
         department_id = VALUES(department_id),
         specialization = VALUES(specialization),
         license_number = VALUES(license_number),
         years_experience = VALUES(years_experience)`,
      [doctorUserId, department.department_id]
    );

    await connection.execute(
      `INSERT INTO patients (user_id, blood_type)
       VALUES (?, 'O+')
       ON DUPLICATE KEY UPDATE blood_type = VALUES(blood_type)`,
      [patientUserId]
    );

    const [[doctor]] = await connection.execute(
      'SELECT doctor_id FROM doctors WHERE user_id = ?',
      [doctorUserId]
    );

    for (const day of ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday']) {
      await connection.execute(
        `INSERT INTO doctor_schedules
         (doctor_id, day_of_week, start_time, end_time, is_available)
         VALUES (?, ?, '09:00:00', '17:00:00', TRUE)
         ON DUPLICATE KEY UPDATE
           start_time = VALUES(start_time),
           end_time = VALUES(end_time),
           is_available = TRUE`,
        [doctor.doctor_id, day]
      );
    }

    await connection.commit();
    console.log('Demo users created:');
    console.log('  admin@hea.local / AdminDemo123!');
    console.log('  doctor@hea.local / DoctorDemo123!');
    console.log('  patient@hea.local / PatientDemo123!');
    console.log('Admin user id:', adminId);
  } catch (error) {
    await connection.rollback();
    console.error(error);
    process.exitCode = 1;
  } finally {
    connection.release();
    await db.end();
  }
}

seed();
