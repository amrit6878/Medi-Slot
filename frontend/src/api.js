const API_BASE = import.meta.env.VITE_API_URL || '/api';

function authHeaders() {
  const token = sessionStorage.getItem('medi-slot-token');
  return token ? { Authorization: `Bearer ${token}` } : {};
}

export function clearSession() {
  Object.keys(sessionStorage)
    .filter((key) => key.startsWith('medi-slot-'))
    .forEach((key) => sessionStorage.removeItem(key));
}

export async function request(path, options = {}) {
  const response = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...authHeaders(),
      ...options.headers,
    },
  });
  const body = await response.json().catch(() => null);
  if (response.status === 401 && path !== '/auth/login') {
    // Token missing/expired: drop the session and return to the landing page.
    clearSession();
    window.location.reload();
  }
  if (!response.ok) throw new Error(body?.message || 'Unable to complete the request.');
  return body;
}

export const api = {
  login: (identifier, password) => request('/auth/login', { method: 'POST', body: JSON.stringify({ identifier, password }) }),
  hospitals: (params = {}) => request(`/hospitals?${new URLSearchParams(Object.entries(params).filter(([, value]) => value !== undefined && value !== null && value !== '')).toString()}`),
  hospital: (id) => request(`/hospitals/${id}`),
  departments: () => request('/departments'),
  doctors: (params = {}) => request(`/doctors?${new URLSearchParams(Object.entries(params).filter(([, value]) => value !== undefined && value !== null && value !== '')).toString()}`),
  doctor: (id) => request(`/doctors/${id}`),
  slots: (doctorId, date) => request(`/doctors/${doctorId}/slots${date ? `?date=${date}` : ''}`),
  myAppointments: (status = 'upcoming') => request(`/appointments/my?status=${status}`),
  appointment: (id) => request(`/appointments/${id}`),
  bookAppointment: (slotId, reason = '') => request('/appointments', { method: 'POST', body: JSON.stringify({ slotId, reason }) }),
  cancelAppointment: id => request(`/appointments/${id}/cancel`, { method: 'PUT' }),
  employeeMe: () => request('/employees/me'),
  updateEmployeeMe: (profile) => request('/employees/me', { method: 'PUT', body: JSON.stringify(profile) }),
  adminDashboard: () => request('/admin/dashboard'),
  adminDoctors: (params = {}) => request(`/admin/doctors?${new URLSearchParams(params).toString()}`),
  adminDoctor: (id) => request(`/admin/doctors/${id}`),
  createAdminDoctor: (doctor) => request('/admin/doctors', { method: 'POST', body: JSON.stringify(doctor) }),
  adminDepartments: () => request('/admin/departments'),
  updateDoctorStatus: (id, isActive) => request(`/admin/doctors/${id}/status`, { method: 'PUT', body: JSON.stringify({ isActive }) }),
  replaceDoctorSchedule: (id, schedules) => request(`/admin/doctors/${id}/schedule`, { method: 'PUT', body: JSON.stringify({ schedules }) }),
  adminHospital: () => request('/admin/hospital'),
  updateAdminHospital: (hospital) => request('/admin/hospital', { method: 'PUT', body: JSON.stringify(hospital) }),
  adminReport: (params = {}) => request(`/admin/reports?${new URLSearchParams(Object.entries(params).filter(([, value]) => value)).toString()}`),
  adminSettings: () => request('/admin/settings'),
  updateAdminProfile: (name) => request('/admin/settings/profile', { method: 'PUT', body: JSON.stringify({ name }) }),
  changeAdminPassword: (currentPassword, newPassword) => request('/admin/settings/password', { method: 'PUT', body: JSON.stringify({ currentPassword, newPassword }) }),
  adminAppointments: (params = {}) => request(`/admin/appointments?${new URLSearchParams(params).toString()}`),
  updateAdminAppointment: (id, action) => request(`/admin/appointments/${id}/${action}`, { method: 'PUT' }),
  superHospitals: () => request('/super-admin/hospitals'),
  superDashboard: () => request('/super-admin/hospitals/dashboard'),
  createHospital: (hospital) => request('/super-admin/hospitals', { method: 'POST', body: JSON.stringify(hospital) }),
  updateHospital: (id, hospital) => request(`/super-admin/hospitals/${id}`, { method: 'PUT', body: JSON.stringify(hospital) }),
  archiveHospital: (id) => request(`/super-admin/hospitals/${id}`, { method: 'DELETE' }),
  superAdmins: (params = {}) => request(`/super-admin/admins?${new URLSearchParams(Object.entries(params).filter(([, value]) => value !== undefined && value !== null && value !== '')).toString()}`),
  createSuperAdmin: (admin) => request('/super-admin/admins', { method: 'POST', body: JSON.stringify(admin) }),
  updateSuperAdmin: (id, admin) => request(`/super-admin/admins/${id}`, { method: 'PUT', body: JSON.stringify(admin) }),
};

export function saveSession(response) {
  const role = response.role === 'SUPER_ADMIN' ? 'superAdmin' : response.role === 'ADMIN' ? 'admin' : 'employee';
  sessionStorage.setItem('medi-slot-token', response.token);
  sessionStorage.setItem('medi-slot-role', role);
  sessionStorage.setItem('medi-slot-user', JSON.stringify(response.user || response.employee || response.admin));
}
