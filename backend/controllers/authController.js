const bcrypt = require('bcryptjs');
const crypto = require('crypto');
const jwt = require('jsonwebtoken');
const db = require('../config/db');
const { jwtSecret } = require('../middleware/authMiddleware');
const { writeAudit } = require('../utils/audit');

const OTP_TTL_MS = Number(process.env.OTP_TTL_SECONDS || 300) * 1000;
const OTP_MAX_ATTEMPTS = Number(process.env.OTP_MAX_ATTEMPTS || 5);
const OTP_ENABLED = String(process.env.OTP_ENABLED || 'true') === 'true';
const DEMO_OTP = String(process.env.DEMO_OTP || 'false') === 'true';
const otpStore = new Map();

function generateToken(userId) {
  return jwt.sign({ id: userId }, jwtSecret(), {
    expiresIn: process.env.JWT_EXPIRES_IN || '24h'
  });
}

function normalizeEmail(email) {
  return String(email || '').trim().toLowerCase();
}

function createOtpSession(user) {
  const now = Date.now();
  for (const [id, record] of otpStore.entries()) {
    if (record.expiresAt <= now) otpStore.delete(id);
  }
  const tempId = crypto.randomUUID();
  const otp = String(crypto.randomInt(100000, 1000000));
  otpStore.set(tempId, {
    otp,
    attempts: 0,
    expiresAt: Date.now() + OTP_TTL_MS,
    user
  });
  return { tempId, otp };
}

async function register(req, res) {
  try {
    const firstName = String(req.body.first_name || req.body.name || '').trim().split(/\s+/)[0];
    const suppliedLastName = String(req.body.last_name || '').trim();
    const nameParts = String(req.body.name || '').trim().split(/\s+/);
    const lastName = suppliedLastName || nameParts.slice(1).join(' ');
    const email = normalizeEmail(req.body.email);
    const password = String(req.body.password || '');
    const phone = String(req.body.phone || '').trim() || null;

    if (!firstName || !lastName || !email || !password) {
      return res.status(400).json({
        success: false,
        message: 'First name, last name, email and password are required'
      });
    }
    if (password.length < 8) {
      return res.status(400).json({
        success: false,
        message: 'Password must be at least 8 characters'
      });
    }

    const [existing] = await db.execute(
      'SELECT user_id FROM users WHERE email = ?',
      [email]
    );
    if (existing.length > 0) {
      return res.status(409).json({ success: false, message: 'Email is already registered' });
    }

    const connection = await db.getConnection();
    try {
      await connection.beginTransaction();
      const hashedPassword = await bcrypt.hash(password, 12);
      const [result] = await connection.execute(
        `INSERT INTO users
         (first_name, last_name, email, password, phone, role, is_active)
         VALUES (?, ?, ?, ?, ?, 'patient', TRUE)`,
        [firstName, lastName, email, hashedPassword, phone]
      );
      await connection.execute(
        'INSERT INTO patients (user_id) VALUES (?)',
        [result.insertId]
      );
      await connection.commit();

      await writeAudit({
        userId: result.insertId,
        action: 'REGISTER',
        entityType: 'users',
        entityId: result.insertId,
        req
      });

      return res.status(201).json({
        success: true,
        message: 'Patient account created successfully. Please sign in.'
      });
    } catch (error) {
      await connection.rollback();
      throw error;
    } finally {
      connection.release();
    }
  } catch (error) {
    console.error('Register error:', error);
    return res.status(500).json({ success: false, message: 'Registration failed' });
  }
}

async function login(req, res) {
  try {
    const email = normalizeEmail(req.body.email);
    const password = String(req.body.password || '');

    if (!email || !password) {
      return res.status(400).json({ success: false, message: 'Email and password are required' });
    }

    const [users] = await db.execute('SELECT * FROM users WHERE email = ?', [email]);
    const user = users[0];

    if (!user || !(await bcrypt.compare(password, user.password))) {
      await writeAudit({ action: 'LOGIN', status: 'failed', req });
      return res.status(401).json({ success: false, message: 'Invalid email or password' });
    }
    if (!user.is_active) {
      return res.status(403).json({ success: false, message: 'Account is deactivated' });
    }

    const safeUser = {
      id: user.user_id,
      name: `${user.first_name} ${user.last_name}`.trim(),
      first_name: user.first_name,
      last_name: user.last_name,
      email: user.email,
      role: user.role
    };

    if (!OTP_ENABLED) {
      const token = generateToken(user.user_id);
      await writeAudit({
        userId: user.user_id,
        action: 'LOGIN',
        entityType: 'users',
        entityId: user.user_id,
        req
      });
      return res.status(200).json({ success: true, token, user: safeUser });
    }

    const { tempId, otp } = createOtpSession(safeUser);
    if (DEMO_OTP) {
      console.log(`Demo OTP for ${user.email}: ${otp}`);
    }

    const response = {
      success: true,
      otpRequired: true,
      tempId,
      expiresInSeconds: Math.floor(OTP_TTL_MS / 1000),
      message: 'Password verified. Enter the one-time code to continue.'
    };
    if (DEMO_OTP) response.demoOtp = otp;

    return res.status(200).json(response);
  } catch (error) {
    console.error('Login error:', error);
    return res.status(500).json({ success: false, message: 'Login failed' });
  }
}

async function verifyLoginOtp(req, res) {
  const tempId = String(req.body.tempId || '');
  const otp = String(req.body.otp || '').trim();
  const record = otpStore.get(tempId);

  if (!record) {
    return res.status(401).json({ success: false, message: 'OTP session is invalid or expired' });
  }
  if (Date.now() > record.expiresAt) {
    otpStore.delete(tempId);
    return res.status(401).json({ success: false, message: 'OTP expired. Please sign in again.' });
  }

  record.attempts += 1;
  if (record.attempts > OTP_MAX_ATTEMPTS) {
    otpStore.delete(tempId);
    return res.status(429).json({ success: false, message: 'Too many OTP attempts' });
  }
  if (!/^\d{6}$/.test(otp) || !crypto.timingSafeEqual(Buffer.from(otp), Buffer.from(record.otp))) {
    return res.status(401).json({ success: false, message: 'Invalid OTP' });
  }

  otpStore.delete(tempId);
  const token = generateToken(record.user.id);

  await writeAudit({
    userId: record.user.id,
    action: 'LOGIN',
    entityType: 'users',
    entityId: record.user.id,
    req
  });

  return res.status(200).json({
    success: true,
    message: 'Login successful',
    token,
    user: record.user
  });
}

async function logout(req, res) {
  await writeAudit({
    userId: req.user.id,
    action: 'LOGOUT',
    entityType: 'users',
    entityId: req.user.id,
    req
  });
  return res.status(200).json({
    success: true,
    message: 'Logged out. Remove the token from the client.'
  });
}

async function getMe(req, res) {
  return res.status(200).json({ success: true, user: req.user });
}

async function changePassword(req, res) {
  try {
    const currentPassword = String(req.body.currentPassword || '');
    const newPassword = String(req.body.newPassword || '');

    if (!currentPassword || newPassword.length < 8) {
      return res.status(400).json({
        success: false,
        message: 'Current password and a new password of at least 8 characters are required'
      });
    }

    const [users] = await db.execute('SELECT password FROM users WHERE user_id = ?', [req.user.id]);
    if (users.length === 0 || !(await bcrypt.compare(currentPassword, users[0].password))) {
      return res.status(400).json({ success: false, message: 'Current password is incorrect' });
    }

    const hashedPassword = await bcrypt.hash(newPassword, 12);
    await db.execute(
      'UPDATE users SET password = ?, updated_at = NOW() WHERE user_id = ?',
      [hashedPassword, req.user.id]
    );

    await writeAudit({
      userId: req.user.id,
      action: 'PASSWORD_CHANGE',
      entityType: 'users',
      entityId: req.user.id,
      req
    });

    return res.status(200).json({ success: true, message: 'Password changed successfully' });
  } catch (error) {
    console.error('Change password error:', error);
    return res.status(500).json({ success: false, message: 'Password change failed' });
  }
}

module.exports = {
  register,
  login,
  verifyLoginOtp,
  logout,
  getMe,
  changePassword
};
