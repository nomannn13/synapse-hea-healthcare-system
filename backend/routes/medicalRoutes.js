const express = require('express');
const controller = require('../controllers/medicalRecordController');
const { protect } = require('../middleware/authMiddleware');
const { medicalAccess, doctorOnly } = require('../middleware/roleMiddleware');

const router = express.Router();

router.use(protect);
router.get('/', medicalAccess, controller.getRecords);
router.post('/', doctorOnly, controller.createRecord);
router.get('/:id', medicalAccess, controller.getRecordById);
router.put('/:id', doctorOnly, controller.updateRecord);

module.exports = router;
