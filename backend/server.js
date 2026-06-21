const express = require('express');
const cors = require('cors');
const dotenv = require('dotenv');
const helmet = require('helmet');
const morgan = require('morgan');
const path = require('path');

dotenv.config();

const db = require('./config/db');
const authRoutes = require('./routes/authRoutes');
const appointmentRoutes = require('./routes/appointmentRoutes');
const medicalRoutes = require('./routes/medicalRoutes');
const adminRoutes = require('./routes/adminRoutes');
const patientRoutes = require('./routes/patientRoutes');
const doctorRoutes = require('./routes/doctorRoutes');

const app = express();

const allowedOrigins = (process.env.CORS_ORIGINS || 'http://localhost:4000')
  .split(',')
  .map((origin) => origin.trim())
  .filter(Boolean);

app.use(helmet({
  contentSecurityPolicy: false
}));
app.use(cors({
  origin(origin, callback) {
    if (!origin || allowedOrigins.includes('*') || allowedOrigins.includes(origin)) {
      return callback(null, true);
    }
    return callback(new Error('Origin is not allowed by CORS'));
  },
  methods: ['GET', 'POST', 'PUT', 'PATCH', 'DELETE', 'OPTIONS'],
  allowedHeaders: ['Content-Type', 'Authorization']
}));
app.use(morgan(process.env.NODE_ENV === 'production' ? 'combined' : 'dev'));
app.use(express.json({ limit: '1mb' }));
app.use(express.urlencoded({ extended: true, limit: '1mb' }));

app.use(express.static(path.join(__dirname, '../frontend')));

app.use('/api/auth', authRoutes);
app.use('/api/appointments', appointmentRoutes);
app.use('/api/medical', medicalRoutes);
app.use('/api/admin', adminRoutes);
app.use('/api/patient', patientRoutes);
app.use('/api/doctor', doctorRoutes);

app.get('/api/health', async (req, res) => {
  try {
    await db.execute('SELECT 1');
    return res.status(200).json({
      success: true,
      service: 'Synapse HEA API',
      database: 'connected',
      timestamp: new Date().toISOString()
    });
  } catch (error) {
    return res.status(503).json({
      success: false,
      service: 'Synapse HEA API',
      database: 'unavailable',
      timestamp: new Date().toISOString()
    });
  }
});

app.get('/', (req, res) => {
  res.sendFile(path.join(__dirname, '../frontend/pages/index.html'));
});

app.use('/api', (req, res) => {
  res.status(404).json({ success: false, message: 'API route not found' });
});

app.use((err, req, res, next) => {
  console.error(err);
  const status = Number(err.status || 500);
  res.status(status).json({
    success: false,
    message: status >= 500 ? 'Internal server error' : err.message
  });
});

async function startServer() {
  const port = Number(process.env.PORT || 4000);
  try {
    await db.testConnection();
    app.listen(port, () => {
      console.log(`Synapse HEA API listening on http://localhost:${port}`);
    });
  } catch (error) {
    console.error('Database connection failed:', error.message);
    process.exitCode = 1;
  }
}

if (require.main === module) {
  startServer();
}

module.exports = { app, startServer };
