import React, { useCallback, useReducer, useState } from "react";
import "../styles.css";
import "../admin.css";
import "../enhancements.css";
import "../admin-interactions.css";
import {
  Home,
  Login,
  Hospitals,
  Doctors,
  HospitalExplore,
  Slots,
  Confirmation,
  Appointments,
  Profile,
} from "./pages/employeePages";
import { Doctor } from "./pages/doctorProfile";
import {
  AdminDashboard,
  AdminDoctors,
  Schedule,
  AddDoctor,
  AdminAppointments,
  HospitalProfile,
  Reports,
  AdminSettings,
  SuperAdminDashboard,
} from "./pages/adminPages";

export default function App() {
  const [page, setRoute] = useState("home");
  const [, refresh] = useReducer((n) => n + 1, 0);
  // Always re-render on navigation, even to the same page (e.g. logging out while on "home").
  const setPage = useCallback((next) => { setRoute(next); refresh(); }, []);
  // The session is kept in sessionStorage by the login flow. Reading it here
  // makes the shared landing-page header react correctly after navigation.
  const isAuthenticated = Boolean(sessionStorage.getItem("medi-slot-token"));
  const role = sessionStorage.getItem("medi-slot-role"); // employee | admin | superAdmin
  const pages = {
    home: <Home setPage={setPage} isAuthenticated={isAuthenticated} />,
    login: <Login setPage={setPage} />,
    hospitals: <Hospitals setPage={setPage} />,
    hospitalExplore: <HospitalExplore setPage={setPage} />,
    doctors: <Doctors setPage={setPage} />,
    doctor: <Doctor setPage={setPage} />,
    slots: <Slots setPage={setPage} />,
    confirmation: <Confirmation setPage={setPage} />,
    appointments: <Appointments setPage={setPage} />,
    profile: <Profile setPage={setPage} />,
    admin: <AdminDashboard setPage={setPage} />,
    adminDoctors: <AdminDoctors setPage={setPage} />,
    addDoctor: <AddDoctor setPage={setPage} />,
    schedule: <Schedule setPage={setPage} />,
    adminAppointments: <AdminAppointments setPage={setPage} />,
    hospitalProfile: <HospitalProfile setPage={setPage} />,
    reports: <Reports setPage={setPage} />,
    settings: <AdminSettings setPage={setPage} />,
    superAdmin: <SuperAdminDashboard setPage={setPage} />,
  };
  // Route guard: every portal page is only reachable with the matching role,
  // otherwise the visitor is sent to the login screen.
  const employeePages = ["hospitals", "hospitalExplore", "doctors", "doctor", "slots", "confirmation", "appointments", "profile"];
  const adminPages = ["admin", "adminDoctors", "addDoctor", "schedule", "adminAppointments", "hospitalProfile", "reports", "settings"];
  let current = pages[page] ? page : role === "admin" ? "admin" : role === "superAdmin" ? "superAdmin" : "home";
  if (employeePages.includes(current) && role !== "employee") current = "login";
  if (adminPages.includes(current) && role !== "admin") current = "login";
  if (current === "superAdmin" && role !== "superAdmin") current = "login";
  return (
    <>
      {current === "home" && !isAuthenticated && (
        <div className="login-float">
          <button onClick={() => setPage("login")}>Login</button>
        </div>
      )}
      {pages[current]}
    </>
  );
}
