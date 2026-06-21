function authorize(...roles) {
  return (req, res, next) => {
    if (!req.user) {
      return res.status(401).json({ success: false, message: 'Authentication required' });
    }
    if (!roles.includes(req.user.role)) {
      return res.status(403).json({
        success: false,
        message: `This operation requires one of these roles: ${roles.join(', ')}`
      });
    }
    return next();
  };
}

const patientOnly = authorize('patient');
const doctorOnly = authorize('doctor');
const adminOnly = authorize('admin');
const doctorOrAdmin = authorize('doctor', 'admin');
const medicalAccess = authorize('patient', 'doctor', 'admin');

module.exports = {
  authorize,
  patientOnly,
  doctorOnly,
  adminOnly,
  doctorOrAdmin,
  medicalAccess
};
