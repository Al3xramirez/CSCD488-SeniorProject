# SyllabusSync

## Overview
The customer wants a class-organizer dashboard that helps students organize their class courses and daily academic lives into a structured, actionable view of their workload, deadlines, grading impacts, and availability. This system allows instructors/TAs to create course instances with students uploading class syllabus’s which the application then extracts and organizes key syllabus information such as assignment due dates, exams, professor office hours, late work policies, attendance, exams, grading scale/weights, etc. Students can join courses via a class code using a .edu account and then input their personal availability. Using the extracted course data and students' personal schedule, the dashboard presents an integrated calendar and timeline of upcoming work, highlights how assignments affect students' grade (points/weight, suggests time allocations (e.g, estimated study hours per week/day), and notifies students for certain things like heavy work weeks ahead. The system also reduces office-hour conflicts by showing compatibility between student availability and instructors/TA office hours, also while supporting meeting scheduling beyond office hours by enabling all parties to declare free times, sending notifications when meetings are proposed, accepted, or declined. 
---

## Problem
- Syllabi are long and hard to reference quickly  
- Students forget deadlines and office hours  
- Scheduling meetings requires back-and-forth emails  
- Office hours often conflict with student schedules  

---

## Solution
SyllabusSync extracts key information from a syllabus and matches student availability with instructor office hours to make scheduling simple.

---

## Intended Users
- **Students**: track deadlines, view office hours, schedule meetings  
- **Professors / TAs**: manage office hours and propose meeting times  

---

## Features
- Upload syllabus PDF or manually enter information  
- Extract important dates, office hours, links, and policies  
- Calendar view with reminders  
- Student and professor availability input 
- Office-hour compatibility view 
- Simple meeting scheduling and confirmations/notifications

---

## General Flow
1. User signs up with a `.edu` email  
2. Student uploads a syllabus  
3. App extracts and organizes course information  
4. Student adds their availability  
5. App shows compatible office-hour times with professor
6. Instructor proposes a meeting  
7. Student confirms and receives reminders  

---

## Tech Stack (proposed/planned)
- **Frontend**: Vue.js 
- **Backend**: springboot  
- **Database**: MySQL  

---

## Platform
- Web application  
- Works on any device with a browser  

---

## Team
- Raul Reano  
- Alejandro Ramirez
- Benjamin Burns  
