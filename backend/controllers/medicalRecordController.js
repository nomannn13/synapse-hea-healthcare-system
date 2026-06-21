const db = require('../config/db');
const { writeAudit } = require('../utils/audit');

async function getDoctorId(userId) {
  const [rows] = await db.execute('SELECT doctor_id FROM doctors WHERE user_id = ?', [userId]);
  return rows[0]?.doctor_id || null;
}

async function getPatientId(userId) {
  const [rows] = await db.execute('SELECT patient_id FROM patients WHERE user_id = ?', [userId]);
  return rows[0]?.patient_id || null;
}

async function getRecords(req, res) {
  try {
    let where = '';
    let params = [];

    if (req.user.role === 'patient') {
      const patientId = await getPatientId(req.user.id);
      where = 'WHERE mr.patient_id = ?';
      params = [patientId];
    } else if (req.user.role === 'doctor') {
      const doctorId = await getDoctorId(req.user.id);
      where = 'WHERE mr.doctor_id = ?';
      params = [doctorId];
    }

    const [records] = await db.execute(
      `SELECT mr.*,
              CONCAT(pu.first_name, ' ', pu.last_name) AS patient_name,
              CONCAT(du.first_name, ' ', du.last_name) AS doctor_name
       FROM medical_records mr
       JOIN patients p ON p.patient_id = mr.patient_id
       JOIN users pu ON pu.user_id = p.user_id
       JOIN doctors d ON d.doctor_id = mr.doctor_id
       JOIN users du ON du.user_id = d.user_id
       ${where}
       ORDER BY mr.created_at DESC`,
      params
    );

    await writeAudit({
      userId: req.user.id,
      action: 'MEDICAL_RECORDS_VIEW',
      entityType: 'medical_records',
      req
    });

    return res.status(200).json({ success: true, data: records });
  } catch (error) {
    return res.status(500).json({ success: false, message: 'Could not load medical records' });
  }
}

async function createRecord(req, res) {
  try {
    const doctorId = await getDoctorId(req.user.id);
    const { patient_id, appointment_id, diagnosis, symptoms, treatment_plan,
      prescription, test_results, blood_pressure, heart_rate, temperature,
      weight, height, notes, triage_category } = req.body;

    if (!patient_id || !diagnosis) {
      return res.status(400).json({ success: false, message: 'Patient and diagnosis are required' });
    }

    const [result] = await db.execute(
      `INSERT INTO medical_records
       (patient_id, doctor_id, appointment_id, diagnosis, symptoms, treatment_plan,
        prescription, test_results, blood_pressure, heart_rate, temperature,
        weight, height, notes, triage_category)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
      [
        patient_id, doctorId, appointment_id || null, diagnosis, symptoms || null,
        treatment_plan || null, prescription || null, test_results || null,
        blood_pressure || null, heart_rate || null, temperature || null,
        weight || null, height || null, notes || null, triage_category || 'non-urgent'
      ]
    );

    await writeAudit({
      userId: req.user.id,
      action: 'MEDICAL_RECORD_CREATE',
      entityType: 'medical_records',
      entityId: result.insertId,
      req
    });

    return res.status(201).json({
      success: true,
      message: 'Medical record created',
      data: { record_id: result.insertId }
    });
  } catch (error) {
    return res.status(500).json({ success: false, message: 'Could not create medical record' });
  }
}

async function getRecordById(req, res) {
  try {
    const [records] = await db.execute(
      `SELECT mr.*, p.user_id AS patient_user_id, d.user_id AS doctor_user_id,
              CONCAT(pu.first_name, ' ', pu.last_name) AS patient_name,
              CONCAT(du.first_name, ' ', du.last_name) AS doctor_name
       FROM medical_records mr
       JOIN patients p ON p.patient_id = mr.patient_id
       JOIN users pu ON pu.user_id = p.user_id
       JOIN doctors d ON d.doctor_id = mr.doctor_id
       JOIN users du ON du.user_id = d.user_id
       WHERE mr.record_id = ?`,
      [req.params.id]
    );

    const record = records[0];
    if (!record) {
      return res.status(404).json({ success: false, message: 'Medical record not found' });
    }
    const allowed = req.user.role === 'admin'
      || record.patient_user_id === req.user.id
      || record.doctor_user_id === req.user.id;
    if (!allowed) {
      return res.status(403).json({ success: false, message: 'Access denied' });
    }

    await writeAudit({
      userId: req.user.id,
      action: 'MEDICAL_RECORD_VIEW',
      entityType: 'medical_records',
      entityId: Number(req.params.id),
      req
    });

    return res.status(200).json({ success: true, data: record });
  } catch (error) {
    return res.status(500).json({ success: false, message: 'Could not load medical record' });
  }
}

async function updateRecord(req, res) {
  try {
    const doctorId = await getDoctorId(req.user.id);
    const [result] = await db.execute(
      `UPDATE medical_records
       SET diagnosis = COALESCE(?, diagnosis),
           symptoms = COALESCE(?, symptoms),
           treatment_plan = COALESCE(?, treatment_plan),
           prescription = COALESCE(?, prescription),
           test_results = COALESCE(?, test_results),
           notes = COALESCE(?, notes),
           triage_category = COALESCE(?, triage_category)
       WHERE record_id = ? AND doctor_id = ?`,
      [
        req.body.diagnosis || null,
        req.body.symptoms || null,
        req.body.treatment_plan || null,
        req.body.prescription || null,
        req.body.test_results || null,
        req.body.notes || null,
        req.body.triage_category || null,
        req.params.id,
        doctorId
      ]
    );

    if (result.affectedRows === 0) {
      return res.status(404).json({ success: false, message: 'Record not found or not owned by doctor' });
    }

    await writeAudit({
      userId: req.user.id,
      action: 'MEDICAL_RECORD_UPDATE',
      entityType: 'medical_records',
      entityId: Number(req.params.id),
      req
    });

    return res.status(200).json({ success: true, message: 'Medical record updated' });
  } catch (error) {
    return res.status(500).json({ success: false, message: 'Could not update medical record' });
  }
}

module.exports = { getRecords, createRecord, getRecordById, updateRecord };
