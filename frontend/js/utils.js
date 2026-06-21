// ============================================
// HEA HEALTHCARE - UTILITY FUNCTIONS
// ============================================

const API_BASE = '/api';

// ============================================
// API CALL HELPER
// ============================================
const api = {
  async request(method, endpoint, data = null) {
    const token = localStorage.getItem('hea_token');
    const options = {
      method,
      headers: {
        'Content-Type': 'application/json',
      }
    };

    if (token) {
      options.headers['Authorization'] = `Bearer ${token}`;
    }

    if (data) {
      options.body = JSON.stringify(data);
    }

    try {
      const response = await fetch(`${API_BASE}${endpoint}`, options);
      const result = await response.json();

      if (!response.ok) {
        throw new Error(result.message || 'Request failed');
      }

      return result;
    } catch (error) {
      throw error;
    }
  },

  get(endpoint)         { return this.request('GET', endpoint); },
  post(endpoint, data)  { return this.request('POST', endpoint, data); },
  put(endpoint, data)   { return this.request('PUT', endpoint, data); },
  delete(endpoint)      { return this.request('DELETE', endpoint); }
};

// ============================================
// AUTH HELPERS
// ============================================
const auth = {
  setSession(token, user) {
    localStorage.setItem('hea_token', token);
    localStorage.setItem('hea_user', JSON.stringify(user));
  },

  getToken() {
    return localStorage.getItem('hea_token');
  },

  getUser() {
    const user = localStorage.getItem('hea_user');
    return user ? JSON.parse(user) : null;
  },

  isLoggedIn() {
    return !!this.getToken();
  },

  logout() {
    localStorage.removeItem('hea_token');
    localStorage.removeItem('hea_user');
    window.location.href = '/pages/login.html';
  },

  requireAuth() {
    if (!this.isLoggedIn()) {
      window.location.href = '/pages/login.html';
      return false;
    }
    return true;
  },

  redirectIfLoggedIn() {
    if (this.isLoggedIn()) {
      const user = this.getUser();
      this.redirectByRole(user?.role);
      return true;
    }
    return false;
  },

  redirectByRole(role) {
    switch(role) {
      case 'patient':
        window.location.href = '/pages/patient-dashboard.html';
        break;
      case 'doctor':
        window.location.href = '/pages/doctor-dashboard.html';
        break;
      case 'admin':
        window.location.href = '/pages/admin-dashboard.html';
        break;
      default:
        window.location.href = '/pages/login.html';
    }
  }
};

// ============================================
// UI HELPERS
// ============================================
const ui = {
  showAlert(message, type = 'info', containerId = 'alert-container') {
    const container = document.getElementById(containerId);
    if (!container) return;

    const icons = {
      success: '✅',
      error: '❌',
      warning: '⚠️',
      info: 'ℹ️'
    };

    container.innerHTML = `
      
        ${icons[type] || 'ℹ️'}
        ${message}
      
    `;

    // Auto clear after 5 seconds
    setTimeout(() => { container.innerHTML = ''; }, 5000);
  },

  clearAlert(containerId = 'alert-container') {
    const container = document.getElementById(containerId);
    if (container) container.innerHTML = '';
  },

  setLoading(buttonId, loading = true) {
    const btn = document.getElementById(buttonId);
    if (!btn) return;

    if (loading) {
      btn.classList.add('loading');
      btn.disabled = true;
      btn._originalText = btn.innerHTML;
    } else {
      btn.classList.remove('loading');
      btn.disabled = false;
      if (btn._originalText) btn.innerHTML = btn._originalText;
    }
  },

  showSpinner(containerId) {
    const el = document.getElementById(containerId);
    if (el) el.innerHTML = '';
  },

  setUserInfo(user) {
    if (!user) return;
    const nameEl = document.getElementById('user-name');
    const roleEl = document.getElementById('user-role');
    const avatarEl = document.getElementById('user-avatar');
    const initials = user.name ? user.name.split(' ').map(n => n[0]).join('').toUpperCase() : 'U';

    if (nameEl) nameEl.textContent = user.name || 'User';
    if (roleEl) roleEl.textContent = user.role ? capitalize(user.role) : '';
    if (avatarEl) avatarEl.textContent = initials;
  }
};

// ============================================
// FORMAT HELPERS
// ============================================
const format = {
  date(dateStr) {
    if (!dateStr) return 'N/A';
    const date = new Date(dateStr);
    return date.toLocaleDateString('en-US', {
      year: 'numeric', month: 'short', day: 'numeric'
    });
  },

  time(timeStr) {
    if (!timeStr) return 'N/A';
    const [hours, minutes] = timeStr.split(':');
    const hour = parseInt(hours);
    const ampm = hour >= 12 ? 'PM' : 'AM';
    const hour12 = hour % 12 || 12;
    return `${hour12}:${minutes} ${ampm}`;
  },

  datetime(dateStr) {
    if (!dateStr) return 'N/A';
    const date = new Date(dateStr);
    return date.toLocaleDateString('en-US', {
      year: 'numeric', month: 'short', day: 'numeric',
      hour: '2-digit', minute: '2-digit'
    });
  },

  currency(amount) {
    if (amount == null) return '$0.00';
    return new Intl.NumberFormat('en-US', {
      style: 'currency', currency: 'USD'
    }).format(amount);
  },

  relativeTime(dateStr) {
    if (!dateStr) return '';
    const now = new Date();
    const date = new Date(dateStr);
    const diff = Math.floor((now - date) / 1000);

    if (diff < 60)    return 'Just now';
    if (diff < 3600)  return `${Math.floor(diff / 60)}m ago`;
    if (diff < 86400) return `${Math.floor(diff / 3600)}h ago`;
    return `${Math.floor(diff / 86400)}d ago`;
  },

  statusBadge(status) {
    const map = {
      'scheduled': 'pill-blue',
      'completed': 'pill-green',
      'cancelled': 'pill-red',
      'pending':   'pill-yellow',
      'confirmed': 'pill-green',
      'rescheduled': 'pill-purple'
    };
    const cls = map[status] || 'pill-gray';
    return `${capitalize(status || 'unknown')}`;
  },

  age(dob) {
    if (!dob) return 'N/A';
    const today = new Date();
    const birth = new Date(dob);
    let age = today.getFullYear() - birth.getFullYear();
    const m = today.getMonth() - birth.getMonth();
    if (m < 0 || (m === 0 && today.getDate() < birth.getDate())) age--;
    return age;
  }
};

// ============================================
// HELPER FUNCTIONS
// ============================================
function capitalize(str) {
  if (!str) return '';
  return str.charAt(0).toUpperCase() + str.slice(1).toLowerCase();
}

function debounce(fn, delay = 300) {
  let timer;
  return (...args) => {
    clearTimeout(timer);
    timer = setTimeout(() => fn(...args), delay);
  };
}

function getGreeting() {
  const hour = new Date().getHours();
  if (hour < 12) return 'Good Morning';
  if (hour < 17) return 'Good Afternoon';
  return 'Good Evening';
}

function getTodayDate() {
  return new Date().toISOString().split('T')[0];
}

function validateEmail(email) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
}

function validatePassword(password) {
  return password && password.length >= 6;
}

// Export for use in other files
window.api    = api;
window.auth   = auth;
window.ui     = ui;
window.format = format;
window.capitalize  = capitalize;
window.debounce    = debounce;
window.getGreeting = getGreeting;
window.getTodayDate = getTodayDate;