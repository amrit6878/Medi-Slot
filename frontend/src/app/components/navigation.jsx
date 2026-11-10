import React, { useEffect, useState, useRef } from "react";
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
import { Logo, Avatar, Btn } from "./ui";
import { clearSession } from "../../api";

function UserMenu({ admin, setPage }) {
  const [open, setOpen] = useState(false);
  const menuRef = useRef(null);
  // Click outside / Escape closes the menu (hover and click both open it).
  useEffect(() => {
    if (!open) return undefined;
    const closeOutside = (event) => { if (menuRef.current && !menuRef.current.contains(event.target)) setOpen(false); };
    const closeEscape = (event) => { if (event.key === "Escape") setOpen(false); };
    document.addEventListener("mousedown", closeOutside);
    document.addEventListener("keydown", closeEscape);
    return () => { document.removeEventListener("mousedown", closeOutside); document.removeEventListener("keydown", closeEscape); };
  }, [open]);
  let user = {};
  try {
    user = JSON.parse(sessionStorage.getItem("medi-slot-user") || "{}");
  } catch {
    user = {};
  }
  const name = user.name || (admin ? "Hospital Admin" : "Motherson Employee");
  const email = user.email || "employee@motherson.com";
  const initials = name
    .split(" ")
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0])
    .join("")
    .toUpperCase();

  const logout = () => {
    clearSession();
    setOpen(false);
    setPage("home");
  };

  return (
    <div className="user-menu" ref={menuRef} onMouseEnter={() => setOpen(true)} onMouseLeave={() => setOpen(false)}>
      <button
        className="avatar user user-menu-trigger"
        onClick={() => setOpen(true)}
        aria-label="Open profile menu"
        aria-expanded={open}
      >
        {initials || "AS"}
      </button>
      {open && (
        <div className="profile-dropdown-wrap">
          <div className="profile-dropdown">
            <div className="profile-dropdown-user">
              <b>{name}</b>
              <span>{email}</span>
            </div>
            <button className="profile-logout" onClick={logout}>
              Logout
            </button>
          </div>
        </div>
      )}
    </div>
  );
}

export function Top({ page, setPage, admin = false, onMenu }) {
  return (
    <header className={admin ? "admin-top top" : "top"}>
      <Logo admin={admin} onHome={() => setPage("home")} />
      <div className="top-spacer" />
      {!admin && (
        <nav>
          <button onClick={() => setPage("home")}>Home</button>
          <button onClick={() => setPage("hospitals")}>Hospitals</button>
          <button onClick={() => setPage("appointments")}>
            My Appointments
          </button>
        </nav>
      )}
      <button className="mobile-menu" onClick={onMenu}>
        <Menu size={20} />
      </button>
            {admin ? (
        <>
          <Bell size={18} />
          <UserMenu admin setPage={setPage} />
        </>
      ) : sessionStorage.getItem("medi-slot-token") ? (
        <UserMenu setPage={setPage} />
      ) : null}
    </header>
  );
}
export function SideItem({ icon: I, text, active, onClick }) {
  return (
    <button
      className={"side-item " + (active ? "active" : "")}
      onClick={onClick}
    >
      <I size={16} />
      {text}
    </button>
  );
}
export function EmployeeLayout({ page, setPage, children }) {
  const [open, setOpen] = useState(false);
  return (
    <>
      <Top page={page} setPage={setPage} onMenu={() => setOpen(!open)} />
      <aside className={"employee-side " + (open ? "open" : "")}>
        <Logo onHome={() => setPage("home")} />
        <SideItem
          icon={Hospital}
          text="Hospitals"
          active={[
            "hospitals",
            "hospitalExplore",
            "doctors",
            "doctor",
            "slots",
          ].includes(page)}
          onClick={() => setPage("hospitals")}
        />
        <SideItem
          icon={CalendarDays}
          text="My Appointments"
          active={page === "appointments"}
          onClick={() => setPage("appointments")}
        />
        <SideItem
          icon={UserRound}
          text="My Profile"
          active={page === "profile"}
          onClick={() => setPage("profile")}
        />
      </aside>
      <main className="portal-main page-enter">{children}</main>
    </>
  );
}
export function AdminLayout({ page, setPage, children }) {
  const [open, setOpen] = useState(false);
  return (
    <div className="admin-layout">
      <aside className={"admin-side " + (open ? "open" : "")}>
        <Logo admin onHome={() => setPage("home")} />
        {[
          ["admin", LayoutDashboard, "Dashboard"],
          ["adminAppointments", CalendarDays, "Appointments"],
          ["adminDoctors", Stethoscope, "Doctors"],
          ["schedule", ClipboardList, "Schedules"],
          ["hospitalProfile", Hospital, "Hospital Profile"],
          ["reports", FileText, "Reports"],
          ["settings", Settings, "Settings"],
        ].map(([p, I, t]) => (
          <SideItem
            key={p}
            icon={I}
            text={t}
            active={page === p}
            onClick={() => setPage(p)}
          />
        ))}
      </aside>
      <div className="admin-content">
        <Top
          admin
          page={page}
          setPage={setPage}
          onMenu={() => setOpen(!open)}
        />
        {children}
      </div>
    </div>
  );
}
