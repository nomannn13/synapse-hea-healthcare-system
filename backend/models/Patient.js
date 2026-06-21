// backend/models/Patient.js
const pool = require('../config/db');

class Patient {
    static async findByUserId(userId) {
        const [rows] = await pool.execute(
            `SELECT p.*, u.first_name, u.last_name, u.email, u.phone
             FROM patients p
             JOIN users u ON p.user_id = u.user_id
             WHERE p.user_id = ?`,
            [userId]
        );
        return rows[0] || null;
    }

    static async findById(patientId) {
        const [rows] = await pool.execute(
            `SELECT p.*, u.first_name, u.last_name, u.email, u.phone
             FROM patients p
             JOIN users u ON p.user_id = u.user_id
             WHERE p.patient_id = ?`,
            [patientId]
        );
        return rows[0] || null;
    }

    static async getAll() {
        const [rows] = await pool.execute(
            `SELECT p.*, u.first_name, u.last_name, u.email, u.phone, u.is_active
             FROM patients p
             JOIN users u ON p.user_id = u.user_id
             WHERE u.is_active = TRUE
             ORDER BY u.first_name`
        );
        return rows;
    }

    static async update(patientId, updates) {
        const fields = Object.keys(updates).map(k => `${k} = ?`).join(', ');
        const values = [...Object.values(updates), patientId];
        await pool.execute(
            `UPDATE patients SET ${fields} WHERE patient_id = ?`, 
            values
        );
    }
}

module.exports = Patient;