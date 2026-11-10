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
export function Logo({ admin = false, onHome }) {
  return (
    <button
      type="button"
      className={"logo " + (admin ? "admin-logo" : "")}
      onClick={onHome}
      aria-label="Go to home"
    >
      <img src="/motherson-logo.png" alt="Motherson" />
      {admin && <small>Hospital Admin</small>}
    </button>
  );
}
export function Avatar({ doctor, size = "" }) {
  return (
    <div className={"avatar " + size} style={{ background: doctor?.color }}>
      {doctor?.initial || "AS"}
    </div>
  );
}
export function Btn({ children, onClick, kind = "", className = "", ...props }) {
  return (
    <button onClick={onClick} className={"btn " + kind + " " + className} {...props}>
      {children}
    </button>
  );
}
