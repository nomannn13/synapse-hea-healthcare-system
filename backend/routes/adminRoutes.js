const express = require('express');
const controller = require('../controllers/adminController');
const { protect } = require('../middleware/authMiddleware');
const { adminOnly } = require('../middleware/roleMiddleware');

const router = express.Router();

router.use(protect, adminOnly);
router.get('/dashboard', controller.getDashboard);
router.get('/reports', controller.getReports);
router.get('/resources', controller.getResources);
router.get('/audit-logs', controller.getAuditLogs);
router.post('/assign-room', controller.assignRoom);
router.put('/users/manage', controller.manageUser);

module.exports = router;
