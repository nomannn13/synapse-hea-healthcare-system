const express = require('express');
const controller = require('../controllers/authController');
const { protect } = require('../middleware/authMiddleware');

const router = express.Router();

router.post('/register', controller.register);
router.post('/login', controller.login);
router.post('/verify-login-otp', controller.verifyLoginOtp);
router.post('/logout', protect, controller.logout);
router.get('/me', protect, controller.getMe);
router.post('/change-password', protect, controller.changePassword);

module.exports = router;
