import React, { useEffect, useState } from "react";
import { CalendarDays, ChevronLeft, MapPin, Star } from "lucide-react";
import { api } from "../../api";
import { mapDoctor } from "../data/mockData";
import { Avatar, Btn } from "../components/ui";
import { EmployeeLayout } from "../components/navigation";

const DAY_NAMES = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"];
const clock = (value) =>
  new Date(`1970-01-01T${value}`).toLocaleTimeString("en-IN", { hour: "2-digit", minute: "2-digit" });

// [1,2,3,4,5,6] -> "Mon – Sat"; [1,3] -> "Mon, Wed"
function dayRanges(days) {
  const out = [];
  let start = days[0];
  let prev = days[0];
  for (let i = 1; i <= days.length; i += 1) {
    const day = days[i];
    if (day === prev + 1) { prev = day; continue; }
    out.push(start === prev ? DAY_NAMES[start - 1] : `${DAY_NAMES[start - 1]} – ${DAY_NAMES[prev - 1]}`);
    start = day;
    prev = day;
  }
  return out.join(", ");
}

// Groups weekdays that share the same hours: [{ days: "Mon – Sat", hours: "09:00 AM – 05:00 PM" }]
function timingLines(weekly) {
  const groups = new Map();
  weekly.forEach((t) => {
    const key = `${t.startTime}|${t.endTime}`;
    groups.set(key, [...(groups.get(key) || []), t.dayOfWeek]);
  });
  return [...groups.entries()].map(([key, days]) => {
    const [start, end] = key.split("|");
    return { days: dayRanges(days.sort((a, b) => a - b)), hours: `${clock(start)} – ${clock(end)}` };
  });
}

export function Doctor({ setPage }) {
  const doctorId = sessionStorage.getItem("medi-slot-doctor-id");
  const [doctor, setDoctor] = useState(null);
  const [error, setError] = useState("");

  useEffect(() => {
    if (!doctorId) { setPage("doctors"); return; }
    api.doctor(doctorId).then((data) => setDoctor(mapDoctor(data))).catch((e) => setError(e.message));
  }, [doctorId, setPage]);

  if (!doctor) {
    return (
      <EmployeeLayout page="doctor" setPage={setPage}>
        <button className="back" onClick={() => setPage("doctors")}>
          <ChevronLeft size={16} />
          Back to Doctors
        </button>
        {error ? <p className="login-error">{error}</p> : <p className="muted">Loading doctor profile…</p>}
      </EmployeeLayout>
    );
  }

  const lines = timingLines(doctor.weeklyTimings);
  return (
    <EmployeeLayout page="doctor" setPage={setPage}>
      <button className="back" onClick={() => setPage("doctors")}>
        <ChevronLeft size={16} />
        Back to Doctors
      </button>
      <section className="profile-hero">
        <Avatar doctor={doctor} size="large" />
        <div>
          <h2>{doctor.name}</h2>
          <p className="rating">
            <Star size={15} fill="currentColor" /> {doctor.rating} Reviews
          </p>
          <p>{doctor.specialty}</p>
          <p className="details">
            {doctor.qualification ? `${doctor.qualification} · ` : ""}{doctor.experience}
          </p>
          <p className="details">
            <MapPin size={14} />
            {doctor.hospital}
          </p>
        </div>
      </section>
      <section className="doctor-detail">
        <div className="tabs">
          <button className="selected">About</button>
          <button>Timings</button>
          <button>Reviews</button>
        </div>
        <h3>About Doctor</h3>
        <p>{doctor.about || `${doctor.name} is a ${doctor.specialty.toLowerCase()} at ${doctor.hospital}.`}</p>
        <hr />
        <h3>Available Timings</h3>
        {lines.length ? lines.map((line) => (
          <p key={line.days}>
            <CalendarDays size={15} /> {line.days} &nbsp; {line.hours}
          </p>
        )) : <p>No weekly timings have been published yet.</p>}
        <Btn onClick={() => setPage("slots")}>Book an Appointment</Btn>
      </section>
    </EmployeeLayout>
  );
}
