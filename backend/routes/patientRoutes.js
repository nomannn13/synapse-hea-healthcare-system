const express = require('express');
const controller = require('../controllers/patientController');
const { protect } = require('../middleware/authMiddleware');
const { patientOnly } = require('../middleware/roleMiddleware');

const router = express.Router();

router.use(protect, patientOnly);
router.get('/dashboard', controller.getDashboard);
router.get('/profile', controller.getProfile);
router.put('/profile', controller.updateProfile);
router.get('/appointments', controller.getAppointmentHistory);
router.get('/doctors', controller.getDoctors);
router.get('/notifications', controller.getNotifications);
router.patch('/notifications/:id/read', controller.markNotificationRead);

module.exports = router;
