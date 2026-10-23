# AquaTech Mobile Application

[![Android SDK](https://img.shields.io/badge/Android_SDK-34.0.0-3DDC84?style=flat-square&logo=android&logoColor=white)](https://developer.android.com/)
[![Java JDK](https://img.shields.io/badge/Java-JDK_11-ED8B00?style=flat-square&logo=openjdk&logoColor=white)](https://www.oracle.com/java/)
[![Gradle](https://img.shields.io/badge/Gradle-8.9-02303A?style=flat-square&logo=gradle&logoColor=white)](https://gradle.org/)
[![Material Components](https://img.shields.io/badge/Material_Design-1.12.0-757575?style=flat-square&logo=materialdesign&logoColor=white)](https://m3.material.io/)
[![Firebase BoM](https://img.shields.io/badge/Firebase_BoM-33.1.2-FFCA28?style=flat-square&logo=firebase&logoColor=black)](https://firebase.google.com/)
[![Dialogflow](https://img.shields.io/badge/Dialogflow_AI-4.89.0-FF6F00?style=flat-square&logo=dialogflow&logoColor=white)](https://cloud.google.com/dialogflow)
[![osmdroid](https://img.shields.io/badge/osmdroid-6.1.20-7EBC6F?style=flat-square&logo=openstreetmap&logoColor=white)](https://github.com/osmdroid/osmdroid)
[![Google Play Location](https://img.shields.io/badge/Play_Services-21.3.0-4285F4?style=flat-square&logo=google&logoColor=white)](https://developers.google.com/android/guides/setup)
[![Glide](https://img.shields.io/badge/Glide-4.16.0-2196F3?style=flat-square&logo=android&logoColor=white)](https://github.com/bumptech/glide)
[![gRPC OkHttp](https://img.shields.io/badge/gRPC_OkHttp-1.79.0-244C5A?style=flat-square&logo=grpc&logoColor=white)](https://grpc.io/)

AquaTech is a native Android enterprise mobile solution built for **Aqua Smartguard Corp** in **Makati City**, Philippines. It serves as a centralized platform for managing advanced water purification services, logistics scheduling, field technician operations, customer support, and real-time analytics.

The application features an organized, responsive interface designed to make service requesting, technician tracking, and system administration seamless across Android phones, foldables, and tablets.

## Features

### Customer Features
- **Service Requests:** Submit requests for Filter Preventive Maintenance, Installation Kits, and Parts Replacement.
- **Live GPS Tracking:** Track technician arrival in real-time on OpenStreetMap (Osmdroid).
- **Service Status Monitoring:** Real-time progression tracking (`Pending`, `In Progress`, `Completed`, `Rescheduled`).
- **Service Receipts & Ratings:** View digital service receipts and submit 5-star technician performance reviews.
- **Profile Management:** Edit profile details, select from 8 custom customer avatars, view verified email badges, and send password reset requests.
- **Notifications Hub:** Instant alerts for request status changes, technician arrival, chat messages, and calls.

### Technician Features
- **Technician Dashboard:** View assigned jobs and manage field service workflows.
- **Arrival & Maintenance Flow:** One-tap status updates (`YOU ARRIVED AT LOCATION` -> `MAINTENANCE ON-GOING`).
- **Proof Uploads:** Upload proof-of-service photos and record technician service remarks.
- **In-App Communication:** Direct customer chat messaging and voice calling.

### Administrative Control
- **Admin Command Center:** Real-time open request ticket management and technician assignment.
- **Technician Management:** Add, edit, verify, and assign avatars for field technicians.
- **Monthly Audit Analytics:** View completion rate rings, system uptime metrics, average response times, and weekly growth bar charts.
- **PDF Export:** Export official monthly audit reports and customer receipts as formatted PDFs.

### AquaBuddy AI Assistant
- **Automated Support:** AI-powered chatbot backed by Google Cloud Dialogflow over gRPC streams.
- **Service Guidance:** Interactive answers for maintenance schedules, water filter questions, and troubleshooting.

### User Interface
- **Material Design 3:** Modern 20dp rounded cards, soft 360-degree elevation drop shadows, and clean typography.
- **Compact Header System:** Standardized 64dp `#2196F3` header bar with back navigation and title alignment.
- **Responsive Layouts:** Adaptive layouts optimized for phones, foldables, and tablets.

## Technologies

| Technology | Version | Purpose |
| --- | --- | --- |
| **Android SDK** | 34.0.0 (API 34) | Core Android application framework (Android 14) |
| **Java** | JDK 11 | Application programming language |
| **Material Components** | 1.12.0 | Material Design 3 UI controls and widgets |
| **AndroidX ConstraintLayout**| 2.1.4 | Responsive, flat layout hierarchy |
| **Firebase Realtime Database**| BoM 33.1.2 | Real-time NoSQL cloud data synchronization |
| **Firebase Auth** | BoM 33.1.2 | User authentication and role-based security |
| **Firebase Storage** | BoM 33.1.2 | Cloud storage for proof images and profile avatars |
| **Google Cloud Dialogflow** | 4.89.0 | Natural language AI engine for AquaBuddy Chatbot |
| **gRPC OkHttp** | 1.79.0 | Streaming network protocol for AI response delivery |
| **Osmdroid (OpenStreetMap)** | 6.1.20 | Interactive map rendering and GPS location markers |
| **Play Services Location** | 21.3.0 | Fused Location GPS tracking for technician logistics |
| **Glide** | 4.16.0 | Image loading, caching, and full-screen media previews |

### Development Tools

| Tool | Version | Purpose |
| --- | --- | --- |
| **Android Studio** | Ladybug 2026.1.4 | Integrated Development Environment (IDE) |
| **Gradle** | 8.9 | Automated build pipeline and dependency manager |
| **JUnit** | 4.13.2 | Unit testing framework |
| **Espresso** | 3.6.1 | Android UI testing framework |

Versions listed above are based on the project's Gradle configuration (`build.gradle`).

## Responsive Design

AquaTech adapts to phone, foldable, and tablet screen sizes through responsive ConstraintLayout guidelines, adaptive maximum width caps (`app:layout_constraintWidth_max="480dp"`), and flexible spacing. Navigation, service forms, technician lists, analytics charts, and settings sections are designed to remain accessible and comfortable across all device form factors.

## Project Purpose

AquaTech provides a centralized enterprise platform for **Aqua Smartguard Corp** to streamline water purification service requests, optimize field technician logistics, automate customer support with AI, and provide administrative analytics.

## Development Status

**Post-Thesis Independent Development & Enterprise Ready**

> [!NOTE]
> **Post-Thesis Independent Development**
>
> After graduating, I continued working on AquaTech independently to improve its features and overall functionality. My ongoing work includes refining the Service Request, Service Receipt, and Service Request Success modules, along with updating the PDF layouts for a cleaner and more consistent output.
>
> I am also improving the Technician Dashboard, particularly the workflow for managing pending requests, in-progress services, report submission, and service completion. In addition, I am working on the Chat Messaging, Voice Calling, and AI Chatbot features, while making sure the application works consistently across Android phones and tablets.

AquaTech is a completed, enterprise-ready mobile solution focused on water purification management, featuring real-time GPS tracking, AI chatbot support, PDF report generation, and responsive layouts across Android devices.

---

**Aqua Smartguard Corp**
GF, Alco Building, Makati City, 1209 Metro Manila
*Proprietary Enterprise Software - All Rights Reserved.*
