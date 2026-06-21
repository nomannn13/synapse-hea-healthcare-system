// backend/models/Doctor.js
const pool = require('../config/db');

class Doctor {
    static async findByUserId(userId) {
        const [rows] = await pool.execute(
            `SELECT d.*, u.first_name, u.last_name, u.email, u.phone
             FROM doctors d
             JOIN users u ON d.user_id = u.user_id
             WHERE d.user_id = ?`,
            [userId]
        );
        return rows[0] || null;
    }

    static async findById(doctorId) {
        const [rows] = await pool.execute(
            `SELECT d.*, u.first_name, u.last_name, u.email, u.phone
             FROM doctors d
             JOIN users u ON d.user_id = u.user_id
             WHERE d.doctor_id = ?`,
            [doctorId]
        );
        return rows[0] || null;
    }

    static async getAll() {
        const [rows] = await pool.execute(
            `SELECT d.*, u.first_name, u.last_name, u.email, u.phone
             FROM doctors d
             JOIN users u ON d.user_id = u.user_id
             WHERE u.is_active = TRUE
             ORDER BY u.first_name`
        );
        return rows;
    }

    static async getSchedule(doctorId) {
        const [rows] = await pool.execute(
            `SELECT * FROM doctor_schedules 
             WHERE doctor_id = ? AND is_available = TRUE
             ORDER BY FIELD(day_of_week, 
             'Monday','Tuesday','Wednesday','Thursday','Friday','Saturday','Sunday')`,
            [doctorId]
        );
        return rows;
    }
}

module.exports = Doctor;