const crypto = require('crypto');

const ALGORITHM = 'aes-256-cbc';
const SECRET_KEY = process.env.JWT_SECRET || 'hea_super_secret_key_2025_synapse';

// Create a 32-byte key from the secret
const KEY = crypto.scryptSync(SECRET_KEY, 'salt', 32);

// ============================================
// ENCRYPT DATA (AES-256)
// ============================================
const encrypt = (text) => {
  try {
    if (!text) return null;
    const iv = crypto.randomBytes(16);
    const cipher = crypto.createCipheriv(ALGORITHM, KEY, iv);
    let encrypted = cipher.update(String(text), 'utf8', 'hex');
    encrypted += cipher.final('hex');
    return iv.toString('hex') + ':' + encrypted;
  } catch (error) {
    console.error('Encryption error:', error);
    return text;
  }
};

// ============================================
// DECRYPT DATA
// ============================================
const decrypt = (encryptedText) => {
  try {
    if (!encryptedText) return null;
    if (!encryptedText.includes(':')) return encryptedText;
    const [ivHex, encrypted] = encryptedText.split(':');
    const iv = Buffer.from(ivHex, 'hex');
    const decipher = crypto.createDecipheriv(ALGORITHM, KEY, iv);
    let decrypted = decipher.update(encrypted, 'hex', 'utf8');
    decrypted += decipher.final('utf8');
    return decrypted;
  } catch (error) {
    console.error('Decryption error:', error);
    return encryptedText;
  }
};

// ============================================
// HASH DATA (one-way, for sensitive fields)
// ============================================
const hash = (data) => {
  return crypto
    .createHmac('sha256', SECRET_KEY)
    .update(String(data))
    .digest('hex');
};

// ============================================
// GENERATE SECURE TOKEN
// ============================================
const generateToken = (length = 32) => {
  return crypto.randomBytes(length).toString('hex');
};

module.exports = { encrypt, decrypt, hash, generateToken };