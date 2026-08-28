# AquaTech

AquaTech is a comprehensive Android application designed to streamline water-related service management, connecting customers with skilled technicians. The platform integrates real-time tracking, AI-powered assistance, and seamless communication to enhance the service experience.

## 🚀 Key Features

*   **AquaBuddy (AI Assistant):** Integrated AI chatbot powered by Google Dialogflow to provide instant support and answer common queries.
*   **Real-time Service Tracking:** Live location tracking of technicians using OpenStreetMap (OSMDroid), ensuring transparency and estimated arrival times.
*   **Seamless Communication:** In-app messaging and voice call capabilities between customers and technicians.
*   **Technician Management:** Comprehensive dashboard for admins to manage personnel, verify credentials, and monitor performance.
*   **Service Analytics:** Monthly performance reports and service logs for better business insights.
*   **Dynamic Notifications:** Real-time updates on service status, technician arrival, and system alerts.

## 🛠 Tech Stack

*   **Language:** Java (Native Android Development)
*   **UI/UX:** XML with Material Design Components
*   **Backend:** Firebase
    *   **Authentication:** Secure user login and registration.
    *   **Realtime Database:** Synchronized data for chat and service tracking.
    *   **Storage:** Cloud storage for profile images and valid ID verifications.
*   **APIs & Libraries:**
    *   **Google Dialogflow:** Natural Language Processing (NLP) for AquaBuddy.
    *   **OpenStreetMap (osmdroid):** Geolocation and mapping services.
    *   **Glide:** Optimized image loading and caching.
    *   **gRPC & OkHttp:** Robust communication protocols for AI and voice services.

## 📂 Project Structure

*   `app/src/main/java/.../aquatech`: Contains the core logic, including Activities, Adapters, and Models.
*   `app/src/main/res/layout`: XML design files for all app screens.
*   `app/src/main/res/drawable`: Vector assets and UI resources.

## ⚙️ Setup & Installation

1.  **Clone the Repository:**
    ```bash
    git clone https://github.com/jandr003/AquaTech-Backup-Project.git
    ```
2.  **Open in Android Studio:**
    Import the project and allow Gradle to sync.
3.  **Firebase Configuration:**
    *   Add your `google-services.json` to the `app/` directory.
    *   Enable Email/Password authentication in the Firebase Console.
    *   Set up Realtime Database and Storage rules.
4.  **Dialogflow Integration:**
    *   Provide your service account credentials for Dialogflow access.
5.  **Build & Run:**
    Deploy the app to an emulator or physical device (Min SDK: 26).

## 📄 License
This project is for backup and development purposes. All rights reserved.

---
Developed as a robust solution for modernized water service management.
