const express = require('express');
const controller = require('../controllers/doctorController');
const { protect } = require('../middleware/authMiddleware');
const { doctorOnly } = require('../middleware/roleMiddleware');

const router = express.Router();

router.use(protect, doctorOnly);
router.get('/dashboard', controller.getDashboard);
router.get('/profile', controller.getProfile);
router.put('/profile', controller.updateProfile);
router.get('/schedule', controller.getSchedule);
router.put('/schedule', controller.updateSchedule);
router.get('/appointments', controller.getAppointments);
router.put('/appointments/:id/status', controller.updateAppointmentStatus);
router.put('/appointments/:id/complete', controller.completeAppointment);
router.get('/patients', controller.getMyPatients);

module.exports = router;
