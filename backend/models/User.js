// backend/models/User.js
const pool = require('../config/db');

class User {
    static async findByEmail(email) {
        const [rows] = await pool.execute(
            'SELECT * FROM users WHERE email = ? AND is_active = TRUE',
            [email]
        );
        return rows[0] || null;
    }

    static async findById(id) {
        const [rows] = await pool.execute(
            `SELECT u.*, p.patient_id, p.date_of_birth, p.gender, p.blood_type,
                    d.doctor_id, d.specialization, d.department
             FROM users u
             LEFT JOIN patients p ON u.user_id = p.user_id
             LEFT JOIN doctors d ON u.user_id = d.user_id
             WHERE u.user_id = ?`,
            [id]
        );
        return rows[0] || null;
    }

    static async create(userData) {
        const { first_name, last_name, email, password, phone, national_id, role } = userData;
        const [result] = await pool.execute(
            `INSERT INTO users (first_name, last_name, email, password, phone, national_id, role)
             VALUES (?, ?, ?, ?, ?, ?, ?)`,
            [first_name, last_name, email, password, phone, national_id, role]
        );
        return result.insertId;
    }

    static async update(id, updates) {
        const fields = Object.keys(updates).map(k => `${k} = ?`).join(', ');
        const values = [...Object.values(updates), id];
        await pool.execute(`UPDATE users SET ${fields} WHERE user_id = ?`, values);
    }

    static async deactivate(id) {
        await pool.execute(
            'UPDATE users SET is_active = FALSE WHERE user_id = ?', 
            [id]
        );
    }
}

module.exports = User;