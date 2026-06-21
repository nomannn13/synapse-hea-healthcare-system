const jwt = require('jsonwebtoken');
const db = require('../config/db');

function jwtSecret() {
  const secret = process.env.JWT_SECRET;
  if (!secret && process.env.NODE_ENV === 'production') {
    throw new Error('JWT_SECRET is required in production');
  }
  return secret || 'development-only-change-me';
}

async function protect(req, res, next) {
  try {
    const header = req.headers.authorization;
    if (!header?.startsWith('Bearer ')) {
      return res.status(401).json({ success: false, message: 'Authentication required' });
    }

    const token = header.slice(7);
    const decoded = jwt.verify(token, jwtSecret());

    const [users] = await db.execute(
      `SELECT user_id, first_name, last_name, email, role, is_active
       FROM users WHERE user_id = ?`,
      [decoded.id]
    );

    if (users.length === 0 || !users[0].is_active) {
      return res.status(401).json({ success: false, message: 'Account is unavailable' });
    }

    const user = users[0];
    req.user = {
      id: user.user_id,
      name: `${user.first_name} ${user.last_name}`.trim(),
      firstName: user.first_name,
      lastName: user.last_name,
      email: user.email,
      role: user.role
    };

    return next();
  } catch (error) {
    const message = error.name === 'TokenExpiredError'
      ? 'Session expired. Please sign in again.'
      : 'Invalid authentication token';
    return res.status(401).json({ success: false, message });
  }
}

async function optionalAuth(req, res, next) {
  const header = req.headers.authorization;
  if (!header?.startsWith('Bearer ')) {
    req.user = null;
    return next();
  }
  return protect(req, res, next);
}

module.exports = { protect, optionalAuth, jwtSecret };
