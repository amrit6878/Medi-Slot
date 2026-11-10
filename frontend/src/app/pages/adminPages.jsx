import React, { useEffect, useState } from "react";
import {
  CalendarDays,
  MapPin,
  Search,
  ArrowRight,
  Building2,
  HeartPulse,
  ShieldCheck,
  Clock3,
  Star,
  ChevronLeft,
  Check,
  X,
  UserRound,
  LayoutDashboard,
  Hospital,
  Stethoscope,
  ClipboardList,
  Bell,
  Settings,
  FileText,
  Menu,
  Download,
  Plus,
  MoreHorizontal,
  Pencil,
  Trash2,
} from "lucide-react";
import { api, saveSession } from "../../api";
import { hospitals, doctors, mapHospital, mapDoctor } from "../data/mockData";
import { Logo, Avatar, Btn } from "../components/ui";
import { EmployeeLayout, AdminLayout, Top } from "../components/navigation";
import {
  Stat,
  Chart,
  Department,
  Today,
  Available,
  DoctorTable,
  FormGroup,
} from "../components/adminWidgets";
function LegacySuperAdminDashboard({ setPage }) {
  const [items, setItems] = useState([]), [metrics, setMetrics] = useState({}), [error, setError] = useState(""), [form, setForm] = useState({name:"",address:"",city:"",phone:"",email:"",openingTime:"09:00",closingTime:"17:00"}), [editing, setEditing] = useState(null);
  const load = () => Promise.all([api.superHospitals(),api.superDashboard()]).then(([h,m])=>{setItems(h);setMetrics(m)}).catch(()=>setError("Unable to load network data."));
  useEffect(()=>{load()},[]);
  const save = async e => { e.preventDefault(); try { editing ? await api.updateHospital(editing,form) : await api.createHospital(form); setEditing(null);setForm({name:"",address:"",city:"",phone:"",email:"",openingTime:"09:00",closingTime:"17:00"});load(); } catch(e){setError(e.message)} };
  return <><Top admin setPage={setPage}/><main className="portal-main page-enter" style={{marginLeft:0,maxWidth:1200}}><section className="admin-head"><div><h1>Motherson Network Control</h1><p>Manage partner hospitals across Medi-Slot.</p></div></section><div className="stats"><Stat label="Partner Hospitals" num={metrics.hospitals||0} type="today"/><Stat label="Active Hospitals" num={metrics.activeHospitals||0} type="confirmed"/><Stat label="Doctors" num={metrics.doctors||0} type="pending"/><Stat label="Appointments" num={metrics.appointments||0} type="cancelled"/></div><form className="panel" style={{marginTop:22,display:"grid",gridTemplateColumns:"repeat(3,1fr)",gap:12}} onSubmit={save}><h3 style={{gridColumn:"1/-1"}}>{editing?"Edit Hospital":"Add Hospital"}</h3>{["name","address","city","phone","email"].map(k=><input key={k} required type={k==='email'?'email':'text'} placeholder={k} value={form[k]} onChange={e=>setForm({...form,[k]:e.target.value})}/>) }<input type="time" value={form.openingTime} onChange={e=>setForm({...form,openingTime:e.target.value})}/><input type="time" value={form.closingTime} onChange={e=>setForm({...form,closingTime:e.target.value})}/><Btn>{editing?"Save changes":"Add hospital"}</Btn></form>{error&&<p className="login-error">{error}</p>}<section className="panel" style={{marginTop:22}}><h3>All Partner Hospitals</h3>{items.map(h=><div className="hospital-card" key={h.id}><div><h3>{h.name}</h3><p>{h.city} · {h.address}</p><p>{h.email}</p></div><span className={'pill '+(h.active?'green':'red')}>{h.active?'Active':'Archived'}</span>{h.active&&<><Btn kind="outline" onClick={()=>{setEditing(h.id);setForm({...h,openingTime:"09:00",closingTime:"17:00"})}}>Edit</Btn><Btn kind="danger-outline" onClick={()=>api.archiveHospital(h.id).then(load)}>Archive</Btn></>}</div>)}</section></main></>;
}
export function SuperAdminDashboard({ setPage }) {
  const emptyHospital = { name: "", address: "", city: "", phone: "", email: "", openingTime: "09:00", closingTime: "17:00" };
  const emptyAdmin = { hospitalId: "", name: "", email: "", password: "" };
  const [hospitals, setHospitals] = useState([]);
  const [admins, setAdmins] = useState([]);
  const [metrics, setMetrics] = useState({});
  const [hospitalForm, setHospitalForm] = useState(emptyHospital);
  const [adminForm, setAdminForm] = useState(emptyAdmin);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const load = async () => {
    try {
      const [hospitalRows, dashboard, adminRows] = await Promise.all([api.superHospitals(), api.superDashboard(), api.superAdmins()]);
      setHospitals(hospitalRows); setMetrics(dashboard); setAdmins(adminRows); setError("");
    } catch (e) { setError(e.message); }
  };
  useEffect(() => { load(); }, []);
  const createHospital = async (event) => {
    event.preventDefault(); setError("");
    try { await api.createHospital(hospitalForm); setHospitalForm(emptyHospital); setNotice("Partner hospital created. You can now assign its administrator."); await load(); }
    catch (e) { setError(e.message); }
  };
  const createAdmin = async (event) => {
    event.preventDefault(); setError("");
    try { await api.createSuperAdmin({ ...adminForm, hospitalId: Number(adminForm.hospitalId) }); setAdminForm(emptyAdmin); setNotice("Hospital administrator credentials created."); await load(); }
    catch (e) { setError(e.message); }
  };
  const toggleAdmin = async (admin) => {
    try { await api.updateSuperAdmin(admin.id, { hospitalId: admin.hospitalId, name: admin.name, email: admin.email, active: !admin.active }); setNotice(`Administrator ${admin.active ? "suspended" : "reactivated"}.`); await load(); }
    catch (e) { setError(e.message); }
  };
  return <><Top admin setPage={setPage}/><main className="portal-main page-enter" style={{ marginLeft: 0, maxWidth: 1200 }}>
    <section className="admin-head"><div><h1>Motherson Network Control</h1><p>Create partner hospitals and securely assign their hospital administrators.</p></div></section>
    <div className="stats"><Stat label="Partner Hospitals" num={metrics.hospitals || 0} type="today"/><Stat label="Active Hospitals" num={metrics.activeHospitals || 0} type="confirmed"/><Stat label="Hospital Admins" num={metrics.hospitalAdmins || 0} type="pending"/><Stat label="Appointments" num={metrics.appointments || 0} type="cancelled"/></div>
    {notice && <p className="saved-note"><Check size={14}/>{notice}</p>}{error && <p className="login-error">{error}</p>}
    <div className="super-admin-forms">
      <form className="panel" onSubmit={createHospital}><h3>Add Partner Hospital</h3>{["name", "address", "city", "phone", "email"].map((field) => <input key={field} required type={field === "email" ? "email" : "text"} placeholder={field === "name" ? "Hospital name" : field[0].toUpperCase() + field.slice(1)} value={hospitalForm[field]} onChange={(e) => setHospitalForm({ ...hospitalForm, [field]: e.target.value })}/>)}<div className="form-time-row"><input required type="time" value={hospitalForm.openingTime} onChange={(e) => setHospitalForm({ ...hospitalForm, openingTime: e.target.value })}/><input required type="time" value={hospitalForm.closingTime} onChange={(e) => setHospitalForm({ ...hospitalForm, closingTime: e.target.value })}/></div><Btn>Add Hospital</Btn></form>
      <form className="panel" onSubmit={createAdmin}><h3>Create Hospital Admin</h3><select required value={adminForm.hospitalId} onChange={(e) => setAdminForm({ ...adminForm, hospitalId: e.target.value })}><option value="">Select active hospital</option>{hospitals.filter((hospital) => hospital.active).map((hospital) => <option key={hospital.id} value={hospital.id}>{hospital.name} · {hospital.city}</option>)}</select><input required placeholder="Administrator name" value={adminForm.name} onChange={(e) => setAdminForm({ ...adminForm, name: e.target.value })}/><input required type="email" placeholder="Official email" value={adminForm.email} onChange={(e) => setAdminForm({ ...adminForm, email: e.target.value })}/><input required minLength="6" type="password" placeholder="Temporary password" value={adminForm.password} onChange={(e) => setAdminForm({ ...adminForm, password: e.target.value })}/><Btn>Create Credentials</Btn></form>
    </div>
    <section className="panel" style={{ marginTop: 22 }}><h3>Hospital Administrators</h3><table><thead><tr><th>Name</th><th>Official email</th><th>Hospital</th><th>Status</th><th>Action</th></tr></thead><tbody>{admins.map((admin) => <tr key={admin.id}><td>{admin.name}</td><td>{admin.email}</td><td>{admin.hospitalName}</td><td><span className={'pill ' + (admin.active ? 'green' : 'red')}>{admin.active ? 'Active' : 'Suspended'}</span></td><td><Btn kind="outline" onClick={() => toggleAdmin(admin)}>{admin.active ? 'Suspend' : 'Reactivate'}</Btn></td></tr>)}{!admins.length && <tr><td colSpan="5" className="empty-state">No hospital administrators created yet.</td></tr>}</tbody></table></section>
    <section className="panel" style={{ marginTop: 22 }}><h3>Partner Hospitals</h3>{hospitals.map((hospital) => <div className="hospital-card" key={hospital.id}><div><h3>{hospital.name}</h3><p>{hospital.address}, {hospital.city}</p><p>{hospital.email}</p></div><span className={'pill ' + (hospital.active ? 'green' : 'red')}>{hospital.active ? 'Active' : 'Archived'}</span>{hospital.active && <Btn kind="danger-outline" onClick={() => api.archiveHospital(hospital.id).then(load).catch((e) => setError(e.message))}>Archive</Btn>}</div>)}</section>
  </main></>;
}
function greeting() {
  const hour = Number(new Intl.DateTimeFormat("en-GB", { hour: "numeric", hour12: false, timeZone: "Asia/Kolkata" }).format(new Date()));
  return hour < 12 ? "Good Morning" : hour < 17 ? "Good Afternoon" : "Good Evening";
}
function adminName() {
  try { return (JSON.parse(sessionStorage.getItem("medi-slot-user") || "{}").name || "Admin").split(" ")[0]; } catch { return "Admin"; }
}
export function AdminDashboard({ setPage }) {
  const [dashboard, setDashboard] = useState(null);
  const [appointments, setAppointments] = useState([]);
  const [error, setError] = useState("");
  useEffect(() => { Promise.all([api.adminDashboard(), api.adminAppointments({ size: 5 })]).then(([summary, page]) => { setDashboard(summary); setAppointments(page.content || []); }).catch((e) => setError(e.message)); }, []);
  const stats = dashboard ? [
    ["Today’s Appointments", dashboard.todayAppointments, "today"],
    ["Awaiting Confirmation", dashboard.pendingAppointments, "pending"],
    ["Completed Today", dashboard.completedToday, "confirmed"],
    ["Available Doctors", dashboard.doctorsAvailableToday, "cancelled"],
  ] : [];
  return (
    <AdminLayout page="admin" setPage={setPage}>
      <section className="admin-head">
        <div>
          <h1>{greeting()}, {adminName()}!</h1>
          <p>{dashboard ? `Here’s what’s happening at ${dashboard.hospitalName} today.` : "Loading your hospital dashboard…"}</p>
        </div>
        <button>
          <CalendarDays size={16} />
          {new Intl.DateTimeFormat("en-GB", { weekday: "short", day: "numeric", month: "short", year: "numeric", timeZone: "Asia/Kolkata" }).format(new Date())}
        </button>
      </section>
      <div className="stats">
        {stats.map(([l, n, t], i) => (
          <div
            className="dash-reveal"
            style={{ animationDelay: `${i * 0.08}s` }}
            key={t}
          >
            <Stat label={l} num={n} type={t} />
          </div>
        ))}
      </div>
      <section className="panel table-panel" style={{marginTop:22}}><div className="panel-top"><h3>Recent Hospital Appointments</h3><a onClick={() => setPage("adminAppointments")}>View all →</a></div><table><thead><tr><th>Date & time</th><th>Employee</th><th>Doctor</th><th>Status</th></tr></thead><tbody>{appointments.map((appointment) => <tr key={appointment.id}><td>{appointment.slot.date} · {appointment.slot.startTime?.slice(0,5)}</td><td>{appointment.employee.name}</td><td>{appointment.doctor.name}</td><td><span className={'pill ' + (appointment.status === 'BOOKED' ? 'yellow' : appointment.status === 'CANCELLED' ? 'red' : 'green')}>{appointment.status}</span></td></tr>)}{dashboard && !appointments.length && <tr><td colSpan="4" className="empty-state">No appointments have been booked at this hospital yet.</td></tr>}</tbody></table></section>
      {error && <p className="login-error">{error}</p>}
    </AdminLayout>
  );
}
export function AdminDoctors({ setPage }) {
  const [query, setQuery] = useState("");
  const [department, setDepartment] = useState("All Departments");
  const [status, setStatus] = useState("All Status");
  const [databaseDoctors, setDatabaseDoctors] = useState([]);
  const [error, setError] = useState("");
  const loadDoctors = () => api.adminDoctors().then(setDatabaseDoctors).catch((e) => setError(e.message));
  useEffect(() => { loadDoctors(); }, []);
  const toggleDoctor = async (doctor) => {
    try { await api.updateDoctorStatus(doctor.id, doctor.status !== "Active"); await loadDoctors(); }
    catch (e) { setError(e.message); }
  };
  const data = databaseDoctors.map((d) => ({
    id: d.id,
    name: d.name,
    specialty: d.specialization,
    department: d.department?.name || "General Medicine",
    status: d.isActive ? "Active" : "Inactive",
    experience: `${d.experienceYears}+ years experience`,
    initial: d.name.split(" ").slice(-2).map(x => x[0]).join(""),
    color: "#e5f2ff",
  }));
  const visible = data.filter(
    (d) =>
      (department === "All Departments" || d.department === department) &&
      (status === "All Status" || d.status === status) &&
      `${d.name} ${d.department} ${d.specialty}`
        .toLowerCase()
        .includes(query.toLowerCase()),
  );
  return (
    <AdminLayout page="adminDoctors" setPage={setPage}>
      <div className="admin-head">
        <div>
          <h1>Doctors</h1>
          <p>Manage doctors, their details and availability.</p>
        </div>
        <Btn onClick={() => setPage("addDoctor")}>
          <Plus size={16} />
          Add Doctor
        </Btn>
      </div>
      <div className="admin-filters">
        <div className="searchbar">
          <Search size={16} />
          <input
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Search by name, department or specialization..."
          />
        </div>
        <select
          value={department}
          onChange={(e) => setDepartment(e.target.value)}
        >
          <option>All Departments</option>
          {[
            "Cardiology",
            "General Medicine",
            "Orthopedics",
            "Dermatology",
            "Pediatrics",
            "Neurology",
            "Gynecology",
            "ENT",
          ].map((x) => (
            <option key={x}>{x}</option>
          ))}
        </select>
        <select value={status} onChange={(e) => setStatus(e.target.value)}>
          <option>All Status</option>
          <option>Active</option>
          <option>Inactive</option>
        </select>
      </div>
      <p className="admin-result-count">
        Showing {visible.length} of {data.length} doctors
      </p>
      <DoctorTable data={visible} onToggle={toggleDoctor} onSchedule={(doctor) => { sessionStorage.setItem("medi-slot-admin-doctor-id", doctor.id); setPage("schedule"); }} />
      {error && <p className="login-error">{error}</p>}
    </AdminLayout>
  );
}
function LegacySchedule({ setPage }) {
  const [active, setActive] = useState([
    true,
    true,
    true,
    true,
    true,
    false,
    false,
  ]);
  const [leaves, setLeaves] = useState([
    {
      date: "12 Jun 2025",
      from: "09:00 AM",
      to: "01:00 PM",
      reason: "Personal Leave",
    },
    {
      date: "20 Jun 2025",
      from: "02:00 PM",
      to: "04:00 PM",
      reason: "Conference",
    },
  ]);
  const [saved, setSaved] = useState(false);
  const days = [
    "Monday",
    "Tuesday",
    "Wednesday",
    "Thursday",
    "Friday",
    "Saturday",
    "Sunday",
  ];
  return (
    <AdminLayout page="schedule" setPage={setPage}>
      <div className="admin-head">
        <div>
          <h1>Doctor Schedule</h1>
          <p>Manage working days, timings and availability.</p>
        </div>
        <select>
          <option>Dr. Amit Sharma (Cardiology)</option>
          <option>Dr. Neha Verma (General Medicine)</option>
        </select>
      </div>
      <section className="panel schedule-panel">
        <div className="panel-top">
          <h3>Weekly Schedule</h3>
          <Btn
            onClick={() =>
              setActive((a) => a.map((x, i) => (i === 5 ? true : x)))
            }
          >
            <Plus size={14} />
            Enable Saturday
          </Btn>
        </div>
        {days.map((d, i) => (
          <div className="schedule-row" key={d}>
            <b>{d}</b>
            {active[i] ? (
              <>
                <input type="time" defaultValue="09:00" />
                <input type="time" defaultValue={i === 2 ? "13:00" : "16:00"} />
                <select>
                  <option>15 minutes</option>
                  <option>30 minutes</option>
                </select>
              </>
            ) : (
              <span className="not-working">Not Working</span>
            )}
            <button
              aria-label={`toggle ${d}`}
              className={"toggle " + (active[i] ? "on" : "")}
              onClick={() =>
                setActive((a) => a.map((x, j) => (j === i ? !x : x)))
              }
            ></button>
          </div>
        ))}
        <Btn className="schedule-save" onClick={() => setSaved(true)}>
          Save Weekly Schedule
        </Btn>
        {saved && (
          <p className="saved-note">
            <Check size={14} /> Schedule changes saved.
          </p>
        )}
        <hr />
        <div className="panel-top">
          <h3>Blocked Slots / Leave</h3>
          <Btn
            onClick={() =>
              setLeaves((l) => [
                ...l,
                {
                  date: "25 Jun 2025",
                  from: "11:00 AM",
                  to: "12:00 PM",
                  reason: "New blocked slot",
                },
              ])
            }
          >
            <Plus size={14} />
            Add Leave / Block Slot
          </Btn>
        </div>
        <table>
          <thead>
            <tr>
              <th>Date</th>
              <th>From</th>
              <th>To</th>
              <th>Reason</th>
              <th>Action</th>
            </tr>
          </thead>
          <tbody>
            {leaves.map((l, i) => (
              <tr key={l.date + l.reason}>
                <td>{l.date}</td>
                <td>{l.from}</td>
                <td>{l.to}</td>
                <td>{l.reason}</td>
                <td>
                  <button
                    className="delete-leave"
                    onClick={() =>
                      setLeaves((x) => x.filter((_, j) => j !== i))
                    }
                  >
                    <Trash2 size={15} />
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>
    </AdminLayout>
  );
}
export function Schedule({ setPage }) {
  const dayNames = ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"];
  const emptySchedule = dayNames.map((_, index) => ({ dayOfWeek: index + 1, active: false, startTime: "09:00", endTime: "16:00", slotDurationMinutes: 30 }));
  const [doctorRows, setDoctorRows] = useState([]);
  const [doctorId, setDoctorId] = useState("");
  const [schedules, setSchedules] = useState(emptySchedule);
  const [error, setError] = useState("");
  const [saved, setSaved] = useState("");
  useEffect(() => { api.adminDoctors().then((rows) => { setDoctorRows(rows); const chosen = sessionStorage.getItem("medi-slot-admin-doctor-id"); setDoctorId(chosen && rows.some((doctor) => String(doctor.id) === chosen) ? chosen : rows[0] ? String(rows[0].id) : ""); }).catch((e) => setError(e.message)); }, []);
  useEffect(() => {
    if (!doctorId) return;
    api.adminDoctor(doctorId).then((doctor) => {
      const byDay = new Map((doctor.weeklyTimings || []).map((schedule) => [schedule.dayOfWeek, schedule]));
      setSchedules(dayNames.map((_, index) => {
        const source = byDay.get(index + 1);
        return source ? { dayOfWeek: index + 1, active: true, startTime: source.startTime.slice(0, 5), endTime: source.endTime.slice(0, 5), slotDurationMinutes: source.slotDurationMinutes } : { dayOfWeek: index + 1, active: false, startTime: "09:00", endTime: "16:00", slotDurationMinutes: 30 };
      })); setSaved("");
    }).catch((e) => setError(e.message));
  }, [doctorId]);
  const updateDay = (index, patch) => setSchedules((current) => current.map((schedule, row) => row === index ? { ...schedule, ...patch } : schedule));
  const save = async () => {
    setError(""); setSaved("");
    try { const response = await api.replaceDoctorSchedule(doctorId, schedules.filter((schedule) => schedule.active).map(({ active, ...schedule }) => schedule)); setSaved(`Availability saved. ${response.slotsCreated} new booking slots generated.`); }
    catch (e) { setError(e.message); }
  };
  return <AdminLayout page="schedule" setPage={setPage}><div className="admin-head"><div><h1>Doctor Schedule</h1><p>Manage working days, timings and appointment availability.</p></div><select value={doctorId} onChange={(e) => setDoctorId(e.target.value)}><option value="">Select doctor</option>{doctorRows.map((doctor) => <option value={doctor.id} key={doctor.id}>{doctor.name} · {doctor.department?.name}</option>)}</select></div><section className="panel schedule-panel"><div className="panel-top"><h3>Weekly Schedule</h3><span className="muted">Changes immediately update employee booking availability.</span></div>{schedules.map((schedule, index) => <div className="schedule-row" key={schedule.dayOfWeek}><b>{dayNames[index]}</b>{schedule.active ? <><input type="time" value={schedule.startTime} onChange={(e) => updateDay(index, { startTime: e.target.value })}/><input type="time" value={schedule.endTime} onChange={(e) => updateDay(index, { endTime: e.target.value })}/><select value={schedule.slotDurationMinutes} onChange={(e) => updateDay(index, { slotDurationMinutes: Number(e.target.value) })}><option value="15">15 minutes</option><option value="30">30 minutes</option><option value="45">45 minutes</option><option value="60">60 minutes</option></select></> : <span className="not-working">Not Working</span>}<button aria-label={`toggle ${dayNames[index]}`} className={'toggle ' + (schedule.active ? 'on' : '')} onClick={() => updateDay(index, { active: !schedule.active })}></button></div>)}<Btn className="schedule-save" disabled={!doctorId} onClick={save}>Save Weekly Schedule</Btn>{saved && <p className="saved-note"><Check size={14}/>{saved}</p>}{error && <p className="login-error">{error}</p>}</section></AdminLayout>;
}
export function AddDoctor({ setPage }) {
  const [departments, setDepartments] = useState([]);
  const [form, setForm] = useState({name:"",departmentId:"",specialization:"",qualification:"",experienceYears:"",email:"",phone:"",consultationFee:"",about:"",profileImageUrl:"",workingDays:[1,2,3,4,5],startTime:"09:00",endTime:"17:00",slotDurationMinutes:15,active:true});
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);
  useEffect(() => { api.adminDepartments().then(setDepartments).catch(() => setError("Unable to load departments.")); }, []);
  const save = async (event) => { event.preventDefault(); setSaving(true); setError(""); try { await api.createAdminDoctor({...form, departmentId:Number(form.departmentId), experienceYears:Number(form.experienceYears), consultationFee: form.consultationFee ? Number(form.consultationFee) : null, slotDurationMinutes:Number(form.slotDurationMinutes)}); setPage("adminDoctors"); } catch (e) { setError(e.message); } finally { setSaving(false); } };
  return (
    <AdminLayout page="adminDoctors" setPage={setPage}>
      <div className="admin-head">
        <div>
          <h1>Add Doctor</h1>
          <p>Enter doctor details and schedule information.</p>
        </div>
      </div>
      <form className="add-doctor" onSubmit={save}>
        <section className="form-group"><h3>Basic Information</h3><label>Full Name *<input required value={form.name} onChange={e=>setForm({...form,name:e.target.value})}/></label><label>Department *<select required value={form.departmentId} onChange={e=>setForm({...form,departmentId:e.target.value})}><option value="">Select department</option>{departments.map(d=><option value={d.id} key={d.id}>{d.name}</option>)}</select></label><label>Specialisation *<input required value={form.specialization} onChange={e=>setForm({...form,specialization:e.target.value})}/></label><label>Experience (Years) *<input required min="0" type="number" value={form.experienceYears} onChange={e=>setForm({...form,experienceYears:e.target.value})}/></label><label>Qualifications *<input required value={form.qualification} onChange={e=>setForm({...form,qualification:e.target.value})}/></label></section><section className="form-group"><h3>Contact & Additional Details</h3>{[["email","Email","email"],["phone","Phone","text"],["consultationFee","Consultation Fee (₹)","number"],["profileImageUrl","Profile Image URL","url"]].map(([key,label,type])=><label key={key}>{label}<input type={type} value={form[key]} onChange={e=>setForm({...form,[key]:e.target.value})}/></label>)}<label>About<textarea value={form.about} onChange={e=>setForm({...form,about:e.target.value})}/></label></section>
        <section className="form-group"><h3>Working Schedule</h3><label>Working Days<div>{["Mon","Tue","Wed","Thu","Fri","Sat","Sun"].map((day,index)=><button type="button" className={form.workingDays.includes(index+1)?"day-selected":""} key={day} onClick={()=>setForm({...form,workingDays:form.workingDays.includes(index+1)?form.workingDays.filter(x=>x!==index+1):[...form.workingDays,index+1]})}>{day}</button>)}</div></label><label>Start Time<input type="time" value={form.startTime} onChange={e=>setForm({...form,startTime:e.target.value})}/></label><label>End Time<input type="time" value={form.endTime} onChange={e=>setForm({...form,endTime:e.target.value})}/></label><label>Slot Duration<select value={form.slotDurationMinutes} onChange={e=>setForm({...form,slotDurationMinutes:e.target.value})}><option value="15">15 minutes</option><option value="30">30 minutes</option><option value="45">45 minutes</option></select></label><label><input type="checkbox" checked={form.active} onChange={e=>setForm({...form,active:e.target.checked})}/> Active</label></section>
        {error && <p className="login-error">{error}</p>}
        <div className="form-actions">
          <Btn type="button" kind="outline" onClick={() => setPage("adminDoctors")}>
            Cancel
          </Btn>
          <Btn type="submit" disabled={saving}>{saving ? "Saving…" : "Save Doctor"}</Btn>
        </div>
      </form>
    </AdminLayout>
  );
}
export function AdminAppointments({ setPage }) {
  const [status, setStatus] = useState("All Status");
  const [doctor, setDoctor] = useState("All Doctors");
  const [date, setDate] = useState("");
  const [rows, setRows] = useState([]);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  const load = async () => {
    setLoading(true);
    setError("");
    try {
      const page = await api.adminAppointments({
        date: date || undefined,
        doctorId: doctor === "All Doctors" ? undefined : doctor,
        status: status === "All Status" ? undefined : status,
      });
      setRows(page.content || []);
    } catch (e) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  };
  useEffect(() => { load(); }, []);
  const changeStatus = async (id, action) => {
    try {
      await api.updateAdminAppointment(id, action);
      await load();
    } catch (e) {
      setError(e.message);
    }
  };
  const doctorOptions = [...new Map(rows.map((row) => [row.doctor.id, row.doctor.name])).entries()];
  const displayStatus = (value) => value.charAt(0) + value.slice(1).toLowerCase();
  const time = (value) => new Date(`1970-01-01T${value}`).toLocaleTimeString("en-IN", { hour: "2-digit", minute: "2-digit" });
  return (
    <AdminLayout page="adminAppointments" setPage={setPage}>
      <div className="admin-head">
        <div>
          <h1>All Appointments</h1>
          <p>View and manage appointments for your hospital.</p>
        </div>
      </div>
      <div className="admin-filters">
        <input type="date" value={date} onChange={(e) => setDate(e.target.value)} />
        <select value={doctor} onChange={(e) => setDoctor(e.target.value)}>
          <option>All Doctors</option>
          {doctorOptions.map(([id, name]) => (
            <option key={id} value={id}>{name}</option>
          ))}
        </select>
        <select value={status} onChange={(e) => setStatus(e.target.value)}>
          <option>All Status</option>
          <option value="BOOKED">Booked</option>
          <option value="CONFIRMED">Confirmed</option>
          <option value="CANCELLED">Cancelled</option>
          <option value="COMPLETED">Completed</option>
          <option value="NO_SHOW">No show</option>
        </select>
        <Btn onClick={load}>
          <Search size={15} />
          Search
        </Btn>
      </div>
      <section className="panel table-panel admin-appointments">
        <table>
          <thead>
            <tr>
              <th>#</th>
              <th>Date & Time</th>
              <th>Employee Name</th>
              <th>Doctor</th>
              <th>Department</th>
              <th>Status</th>
              <th>Action</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((row, i) => (
              <tr key={row.id}>
                <td>{i + 1}</td>
                <td>
                  {row.slot.date}
                  <br />{time(row.slot.startTime)}
                </td>
                <td>{row.employee.name}</td>
                <td>{row.doctor.name}</td>
                <td>{row.doctor.department?.name || "—"}</td>
                <td>
                  <span
                    className={
                      "pill " +
                      (row.status === "CONFIRMED" || row.status === "COMPLETED"
                        ? "green"
                        : row.status === "BOOKED"
                          ? "yellow"
                          : "red")
                    }
                  >
                    {displayStatus(row.status)}
                  </span>
                </td>
                <td>
                  {row.status === "BOOKED" ? (
                    <>
                      <button
                        className="icon-btn approve"
                        title="Confirm appointment"
                        onClick={() => changeStatus(row.id, "confirm")}
                      >
                        <Check size={14} />
                      </button>
                      <button
                        className="icon-btn reject"
                        title="Cancel appointment"
                        onClick={() => changeStatus(row.id, "cancel")}
                      >
                        <X size={14} />
                      </button>
                    </>
                  ) : (
                    <span>—</span>
                  )}
                </td>
              </tr>
            ))}
            {!loading && !rows.length && <tr><td colSpan="7" className="empty-state">No appointments have been booked for this hospital.</td></tr>}
          </tbody>
        </table>
      </section>
      {loading && <p className="muted">Loading appointments…</p>}
      {error && <p className="login-error">{error}</p>}
    </AdminLayout>
  );
}
export function HospitalProfile({ setPage }) {
  const [form, setForm] = useState(null);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [saving, setSaving] = useState(false);
  const clockValue = (value) => (value || "").slice(0, 5);
  const load = () => api.adminHospital().then((h) => setForm({ ...h, openingTime: clockValue(h.openingTime), closingTime: clockValue(h.closingTime) })).catch((e) => setError(e.message));
  useEffect(() => { load(); }, []);
  const set = (key) => (event) => setForm({ ...form, [key]: event.target.value });
  const save = async (event) => {
    event.preventDefault();
    setSaving(true); setError(""); setNotice("");
    try {
      const saved = await api.updateAdminHospital(form);
      setForm({ ...saved, openingTime: clockValue(saved.openingTime), closingTime: clockValue(saved.closingTime) });
      setNotice("Hospital details saved.");
    } catch (e) { setError(e.message); } finally { setSaving(false); }
  };
  return (
    <AdminLayout page="hospitalProfile" setPage={setPage}>
      <div className="admin-head">
        <div>
          <h1>Hospital Profile</h1>
          <p>Manage your hospital’s information and contact details.</p>
        </div>
      </div>
      {error && <p className="login-error" style={{ margin: "0 30px 12px" }}>{error}</p>}
      {notice && <p className="settings-ok">{notice}</p>}
      {!form ? (!error && <p className="muted" style={{ margin: "0 30px" }}>Loading hospital details…</p>) : (
        <form className="hospital-form" onSubmit={save}>
          <div className="form-group">
            <h3>Basic Information</h3>
            <label>Hospital Name<input value={form.name} readOnly /></label>
            <label>City<input value={form.city} readOnly /></label>
            <label>Address *<input required value={form.address} onChange={set("address")} /></label>
            <label>Contact Number *<input required value={form.phone} onChange={set("phone")} /></label>
            <label>Email *<input required type="email" value={form.email} onChange={set("email")} /></label>
            <p className="settings-note">Name and city are managed by Motherson. Contact the network admin to change them.</p>
          </div>
          <div className="form-group">
            <h3>Working Hours</h3>
            <div className="work-hours">
              <label>Opening Time<input type="time" required value={form.openingTime} onChange={set("openingTime")} /></label>
              <label>Closing Time<input type="time" required value={form.closingTime} onChange={set("closingTime")} /></label>
            </div>
          </div>
          <div className="form-actions">
            <Btn type="button" kind="outline" onClick={load}>Reset</Btn>
            <Btn type="submit" disabled={saving}>{saving ? "Saving…" : "Save Changes"}</Btn>
          </div>
        </form>
      )}
    </AdminLayout>
  );
}

const isoDay = (date) => new Intl.DateTimeFormat("en-CA", { timeZone: "Asia/Kolkata" }).format(date);

export function Reports({ setPage }) {
  const [from, setFrom] = useState(isoDay(new Date(Date.now() - 29 * 86400000)));
  const [to, setTo] = useState(isoDay(new Date()));
  const [report, setReport] = useState(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const load = async () => {
    setLoading(true); setError("");
    try { setReport(await api.adminReport({ from, to })); } catch (e) { setError(e.message); } finally { setLoading(false); }
  };
  useEffect(() => { load(); }, []);
  const s = report?.summary;
  const stats = s ? [
    ["Total Appointments", s.total, "today"],
    ["Upcoming (Booked + Confirmed)", s.booked + s.confirmed, "pending"],
    ["Completed", s.completed, "confirmed"],
    ["Cancelled / No-show", s.cancelled + s.noShow, "cancelled"],
  ] : [];
  const peak = Math.max(1, ...(report?.byDay || []).map((d) => d.total));
  const label = (value) => new Intl.DateTimeFormat("en-GB", { day: "2-digit", month: "short", weekday: "short" }).format(new Date(`${value}T12:00:00`));
  const exportCsv = () => {
    if (!report) return;
    const rows = [["Doctor", "Department", "Total", "Completed", "Cancelled", "No-show"],
      ...report.byDoctor.map((d) => [d.doctorName, d.department, d.total, d.completed, d.cancelled, d.noShow])];
    const csv = rows.map((r) => r.map((c) => `"${String(c).replace(/"/g, '""')}"`).join(",")).join("\n");
    const link = document.createElement("a");
    link.href = URL.createObjectURL(new Blob([csv], { type: "text/csv;charset=utf-8;" }));
    link.download = `appointments-report-${report.from}-to-${report.to}.csv`;
    link.click();
    URL.revokeObjectURL(link.href);
  };
  return (
    <AdminLayout page="reports" setPage={setPage}>
      <div className="admin-head">
        <div>
          <h1>Reports</h1>
          <p>{report ? `Appointment activity at ${report.hospitalName}.` : "Appointment activity for your hospital."}</p>
        </div>
        <Btn kind="outline" type="button" onClick={exportCsv} disabled={!report || !report.byDoctor.length}>
          <Download size={15} />
          Export CSV
        </Btn>
      </div>
      <div className="admin-filters">
        <input type="date" value={from} max={to} onChange={(e) => setFrom(e.target.value)} aria-label="From date" />
        <input type="date" value={to} min={from} onChange={(e) => setTo(e.target.value)} aria-label="To date" />
        <Btn onClick={load}>
          <Search size={15} />
          {loading ? "Loading…" : "Apply"}
        </Btn>
      </div>
      {error && <p className="login-error" style={{ margin: "0 30px 12px" }}>{error}</p>}
      <div className="stats">
        {stats.map(([l, n, t]) => <Stat key={t} label={l} num={n} type={t} />)}
      </div>
      {report && (
        <>
          <section className="panel table-panel" style={{ margin: "0 30px 22px" }}>
            <div className="panel-top"><h3>Appointments by Doctor</h3></div>
            <table>
              <thead><tr><th>Doctor</th><th>Department</th><th>Total</th><th>Completed</th><th>Cancelled</th><th>No-show</th></tr></thead>
              <tbody>
                {report.byDoctor.map((d) => (
                  <tr key={d.doctorId}><td>{d.doctorName}</td><td>{d.department}</td><td>{d.total}</td><td>{d.completed}</td><td>{d.cancelled}</td><td>{d.noShow}</td></tr>
                ))}
                {!report.byDoctor.length && <tr><td colSpan="6" className="empty-state">No appointments in this period.</td></tr>}
              </tbody>
            </table>
          </section>
          <section className="panel" style={{ margin: "0 30px 30px" }}>
            <div className="panel-top"><h3>Appointments per Day</h3></div>
            <div className="report-bars">
              {report.byDay.map((d) => (
                <div className="report-bar" key={d.date}>
                  <span>{label(d.date)}</span>
                  <i style={{ width: `${(d.total / peak) * 100}%` }} />
                  <b>{d.total}</b>
                </div>
              ))}
              {!report.byDay.length && <p className="muted">Nothing to chart for this period.</p>}
            </div>
          </section>
        </>
      )}
    </AdminLayout>
  );
}

export function AdminSettings({ setPage }) {
  const [account, setAccount] = useState(null);
  const [name, setName] = useState("");
  const [passwords, setPasswords] = useState({ currentPassword: "", newPassword: "", confirm: "" });
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [busy, setBusy] = useState(false);
  useEffect(() => { api.adminSettings().then((a) => { setAccount(a); setName(a.name); }).catch((e) => setError(e.message)); }, []);

  const saveProfile = async (event) => {
    event.preventDefault();
    setError(""); setNotice(""); setBusy(true);
    try {
      const saved = await api.updateAdminProfile(name);
      setAccount(saved);
      const stored = JSON.parse(sessionStorage.getItem("medi-slot-user") || "{}");
      sessionStorage.setItem("medi-slot-user", JSON.stringify({ ...stored, name: saved.name }));
      setNotice("Profile updated.");
    } catch (e) { setError(e.message); } finally { setBusy(false); }
  };
  const savePassword = async (event) => {
    event.preventDefault();
    setError(""); setNotice("");
    if (passwords.newPassword !== passwords.confirm) { setError("The new passwords do not match."); return; }
    setBusy(true);
    try {
      await api.changeAdminPassword(passwords.currentPassword, passwords.newPassword);
      setPasswords({ currentPassword: "", newPassword: "", confirm: "" });
      setNotice("Password changed. Use the new password next time you sign in.");
    } catch (e) { setError(e.message); } finally { setBusy(false); }
  };
  const setPw = (key) => (event) => setPasswords({ ...passwords, [key]: event.target.value });
  return (
    <AdminLayout page="settings" setPage={setPage}>
      <div className="admin-head">
        <div>
          <h1>Settings</h1>
          <p>Manage your administrator account.</p>
        </div>
      </div>
      {error && <p className="login-error" style={{ margin: "0 30px 12px" }}>{error}</p>}
      {notice && <p className="settings-ok">{notice}</p>}
      {account && (
        <div className="hospital-form">
          <form className="form-group" onSubmit={saveProfile}>
            <h3>Account</h3>
            <label>Full Name *<input required value={name} onChange={(e) => setName(e.target.value)} /></label>
            <label>Email (login)<input value={account.email} readOnly /></label>
            <label>Hospital<input value={account.hospitalName} readOnly /></label>
            <div className="form-actions"><Btn type="submit" disabled={busy}>Save Profile</Btn></div>
          </form>
          <form className="form-group" onSubmit={savePassword}>
            <h3>Change Password</h3>
            <label>Current Password *<input required type="password" autoComplete="current-password" value={passwords.currentPassword} onChange={setPw("currentPassword")} /></label>
            <label>New Password * (min. 6 characters)<input required minLength={6} type="password" autoComplete="new-password" value={passwords.newPassword} onChange={setPw("newPassword")} /></label>
            <label>Confirm New Password *<input required minLength={6} type="password" autoComplete="new-password" value={passwords.confirm} onChange={setPw("confirm")} /></label>
            <div className="form-actions"><Btn type="submit" disabled={busy}>Update Password</Btn></div>
          </form>
        </div>
      )}
    </AdminLayout>
  );
}
