const express = require('express');
const controller = require('../controllers/appointmentController');
const { protect } = require('../middleware/authMiddleware');
const { authorize, patientOnly } = require('../middleware/roleMiddleware');

const router = express.Router();

router.use(protect);
router.get('/availability', authorize('patient', 'doctor', 'admin'), controller.getAvailability);
router.post('/book', patientOnly, controller.bookAppointment);
router.put(
  '/:appointment_id/cancel',
  authorize('patient', 'doctor', 'admin'),
  controller.cancelAppointment
);
router.put(
  '/:appointment_id/reschedule',
  authorize('patient', 'doctor', 'admin'),
  controller.rescheduleAppointment
);
router.get('/', authorize('patient', 'doctor', 'admin'), controller.getAppointments);

module.exports = router;
