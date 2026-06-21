// Reusable authentication helpers for pages that prefer external scripts.
window.HEA = window.HEA || {};

HEA.API_BASE = '/api';

HEA.getSession = function getSession() {
  const token = localStorage.getItem('hea_token');
  let user = null;
  try {
    user = JSON.parse(localStorage.getItem('hea_user') || 'null');
  } catch {
    user = null;
  }
  return { token, user };
};

HEA.clearSession = function clearSession() {
  localStorage.removeItem('hea_token');
  localStorage.removeItem('hea_user');
};

HEA.saveSession = function saveSession(token, user) {
  localStorage.setItem('hea_token', token);
  localStorage.setItem('hea_user', JSON.stringify(user));
};

HEA.dashboardForRole = function dashboardForRole(role) {
  return {
    admin: 'admin-dashboard.html',
    doctor: 'doctor-dashboard.html',
    patient: 'patient-dashboard.html'
  }[role] || 'login.html';
};

HEA.requireRole = function requireRole(role) {
  const { token, user } = HEA.getSession();
  if (!token || !user || user.role !== role) {
    location.href = token && user ? HEA.dashboardForRole(user.role) : 'login.html';
    return null;
  }
  return { token, user };
};

HEA.request = async function request(path, options = {}) {
  const { token } = HEA.getSession();
  const response = await fetch(`${HEA.API_BASE}${path}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(options.headers || {})
    }
  });
  const data = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(data.message || `Request failed (${response.status})`);
  return data;
};

HEA.escapeHtml = function escapeHtml(value) {
  return String(value ?? '').replace(/[&<>"']/g, (character) => ({
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    '"': '&quot;',
    "'": '&#039;'
  })[character]);
};
