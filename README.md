# MediScan — Intelligent Health Symptom Checker

MediScan is an educational Android application developed for the **Software Design in Intelligent Systems** course. It allows users to select symptoms, receive a preliminary rule-based assessment, and explore relevant health information from MedlinePlus.

## Project Overview

The purpose of MediScan is to demonstrate how an intelligent software system can process user input, apply predefined decision rules, and retrieve information from an external health information service.

## Features

- **Symptom selection** — Select symptoms such as fever, cough, headache, and nausea.
- **Rule-based assessment** — Apply predefined rules to symptoms, severity, and temperature.
- **Safety warnings** — Display warnings for selected emergency symptoms.
- **Health information search** — Retrieve related health topics from MedlinePlus.
- **Assessment history** — Save and review previous assessments locally on the device.
- **User-friendly interface** — A mobile interface built with Jetpack Compose.

*Note: Include assessment history in this list only if it has been implemented and tested successfully in your current version.*

## Technologies Used

- Kotlin
- Android Studio
- Jetpack Compose
- Kotlin Coroutines
- MedlinePlus Web Service API
- Android SharedPreferences for local assessment history

## Intelligent System Design

MediScan demonstrates the following concepts:

1. **Input processing** — Collecting and validating symptom information.
2. **Rule-based reasoning** — Evaluating predefined conditions to produce a preliminary assessment.
3. **External API integration** — Retrieving health information from MedlinePlus.
4. **Decision support** — Presenting explanations and general safety advice.
5. **Error handling** — Handling missing inputs and unavailable online services.

## How to Run

1. Clone or download this repository.
2. Open the project in Android Studio.
3. Allow Gradle synchronization to finish.
4. Connect an Android device or start an emulator.
5. Run the `app` configuration.

An internet connection is required to retrieve health information from MedlinePlus.

## Medical Disclaimer

MediScan is an educational prototype and is not a medical diagnostic tool. Its rule-based assessments are simplified demonstrations and are not clinically validated. The app does not replace professional medical advice, diagnosis, or treatment. Seek urgent medical assistance for emergency symptoms.

## Project Information

- **Project:** MediScan
- **Course:** Software Design in Intelligent Systems
- **Platform:** Android
- **Repository:** https://github.com/maftuna-coder/MediScan

## System Architecture

The diagram illustrates the main components of MediScan, including
the Android user interface, rule-based assessment engine,
MedlinePlus health information service, and local assessment history.

![MediScan System Architecture](https://github.com/maftuna-coder/MediScan/blob/master/MediScan_Architecture.png)
