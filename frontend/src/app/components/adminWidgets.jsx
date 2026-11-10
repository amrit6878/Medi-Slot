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
import { Avatar, Btn } from "./ui";
import { doctors } from "../data/mockData";
export const Stat = ({ label, num, type }) => (
  <div className={"stat " + type}>
    <span>
      {type === "confirmed" ? (
        <Check />
      ) : type === "pending" ? (
        <Clock3 />
      ) : type === "cancelled" ? (
        <X />
      ) : (
        <CalendarDays />
      )}
    </span>
    <div>
      <small>{label}</small>
      <b>{num}</b>
      <em>
        {type === "today"
          ? "+2 from yesterday"
          : type === "confirmed"
            ? "67% of total"
            : type === "pending"
              ? "25% of total"
              : "8% of total"}
      </em>
    </div>
  </div>
);
export function Chart({ setPage }) {
  const [active, setActive] = useState(null);
  const days = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"];
  const data = [11, 17, 12, 9, 15, 10, 13];
  return (
    <section className="panel chart">
      <h3>
        Appointments This Week{" "}
        <a onClick={() => setPage("reports")}>View Reports →</a>
      </h3>
      <div className="bars">
        {data.map((n, i) => (
          <button
            className={"bar-wrap " + (active === i ? "active" : "")}
            onClick={() => setActive(i)}
            key={i}
          >
            <span style={{ height: n * 7 }}></span>
            <small>{days[i]}</small>
            {active === i && <em>{n} appointments</em>}
          </button>
        ))}
      </div>
    </section>
  );
}
export function Department() {
  const [focus, setFocus] = useState(0);
  const items = [
    "General Medicine (32%)",
    "Orthopedics (24%)",
    "Cardiology (18%)",
    "Dermatology (14%)",
    "Others (12%)",
  ];
  return (
    <section className="panel donut-panel">
      <h3>Appointments by Department</h3>
      <div className={"donut donut-focus-" + focus}>
        <b>
          {[22, 16, 12, 10, 8][focus]}
          <small>{items[focus].split(" ")[0]} total</small>
        </b>
      </div>
      <ul className="legend">
        {items.map((x, i) => (
          <li
            className={focus === i ? "selected-legend" : ""}
            onClick={() => setFocus(i)}
            key={x}
          >
            {x}
          </li>
        ))}
      </ul>
    </section>
  );
}
export function Today({ setPage }) {
  return (
    <section className="panel table-panel">
      <h3>
        Today's Appointments{" "}
        <a onClick={() => setPage("adminAppointments")}>View All →</a>
      </h3>
      <table>
        <thead>
          <tr>
            <th>Time</th>
            <th>Patient / Employee</th>
            <th>Doctor</th>
            <th>Department</th>
            <th>Status</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          {["Rajesh Kumar", "Priya Singh", "Vikram Mehta", "Anjali Tiwari"].map(
            (x, i) => (
              <tr key={x}>
                <td>{["09:00 AM", "10:00 AM", "11:30 AM", "02:00 PM"][i]}</td>
                <td>{x}</td>
                <td>{doctors[i].name}</td>
                <td>{doctors[i].specialty}</td>
                <td>
                  <span
                    className={
                      "pill " + (i === 1 || i === 3 ? "yellow" : "green")
                    }
                  >
                    {i === 1 || i === 3 ? "Pending" : "Confirmed"}
                  </span>
                </td>
                <td>
                  <button
                    className="view"
                    onClick={() => setPage("adminAppointments")}
                  >
                    View
                  </button>
                </td>
              </tr>
            ),
          )}
        </tbody>
      </table>
    </section>
  );
}
export function Available({ setPage }) {
  return (
    <section className="panel available">
      <h3>
        Doctors Available Today{" "}
        <a onClick={() => setPage("adminDoctors")}>View All →</a>
      </h3>
      {doctors.slice(0, 4).map((d) => (
        <div key={d.name}>
          <Avatar doctor={d} />
          <p>
            <b>{d.name}</b>
            <br />
            <small>
              {d.specialty}
              <br />
              {d.time}
            </small>
          </p>
          <button className="pill green" onClick={() => setPage("schedule")}>
            Available
          </button>
        </div>
      ))}
    </section>
  );
}
export function DoctorTable({ data, onToggle, onSchedule }) {
  return (
      <section className="panel table-panel doctor-table">
        <table>
          <thead>
            <tr>
              <th>#</th>
              <th>Name</th>
              <th>Department</th>
              <th>Specialization</th>
              <th>Experience</th>
              <th>Status</th>
              <th>Action</th>
            </tr>
          </thead>
          <tbody>
            {data.map((d, i) => {
              const active = d.status === "Active";
              return (
                <tr key={d.name}>
                  <td>{i + 1}</td>
                  <td>
                    <b>{d.name}</b>
                  </td>
                  <td>{d.department}</td>
                  <td>{d.specialty}</td>
                  <td>
                    {d.experience.replace("+ years experience", " years")}
                  </td>
                  <td>
                    <button
                      className={"pill " + (active ? "green" : "red")}
                      onClick={() => onToggle?.(d)}
                    >
                      {active ? "Active" : "Inactive"}
                    </button>
                  </td>
                  <td>
                    <button
                      className="icon-btn"
                      title="Edit booking schedule"
                      onClick={() => onSchedule?.(d)}
                    >
                      <Pencil size={15} />
                    </button>
                    <button
                      className="icon-btn"
                      title="Activate or deactivate doctor"
                      onClick={() => onToggle?.(d)}
                    >
                      <MoreHorizontal size={15} />
                    </button>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </section>
  );
}
export function FormGroup({ title, fields }) {
  return (
    <div className="form-group">
      <h3>{title}</h3>
      {fields.map((f, i) => (
        <label key={f}>
          {f}
          {f === "Working Days" ? (
            <div className="checks">
              {["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"].map((x, j) => (
                <span key={x}>
                  <input type="checkbox" defaultChecked={j < 5} />
                  {x}
                </span>
              ))}
            </div>
          ) : f === "About" ? (
            <textarea defaultValue="Specializes in interventional cardiology with focus on minimally invasive procedures." />
          ) : (
            <input
              defaultValue={
                i === 0 && title === "Basic Information"
                  ? "Dr. Ananya Gupta"
                  : ""
              }
            />
          )}
        </label>
      ))}
    </div>
  );
}
