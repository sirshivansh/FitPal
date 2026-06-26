# 🏋️ FitPal

**FitPal** is a modern fitness and nutrition calculator that helps users estimate their daily calorie and macronutrient requirements based on their body metrics and fitness goals.

Built using **Google AI Studio**, FitPal provides personalized recommendations for calorie intake, protein, fat, and carbohydrates using evidence-based formulas.

---

## ✨ Features

* 🔥 Calculate **Basal Metabolic Rate (BMR)** using the **Mifflin–St Jeor Equation**
* 🎯 Set personalized calorie goals for:

  * Weight Loss
  * Maintenance
  * Muscle Gain
* 📉 Customizable calorie deficit
* 🥩 Personalized protein recommendations
* 🥑 Healthy fat recommendations
* 🍚 Automatic carbohydrate calculation
* 📱 Clean and responsive user interface
* ⚡ Fast and lightweight

---

## 📊 Calculations

### Basal Metabolic Rate (BMR)

FitPal uses the **Mifflin–St Jeor Equation**, one of the most widely accepted equations for estimating resting energy expenditure.

**Men**

```
BMR = (10 × weight) + (6.25 × height) − (5 × age) + 5
```

**Women**

```
BMR = (10 × weight) + (6.25 × height) − (5 × age) − 161
```

---

### Macronutrients

Protein, fat, and carbohydrates are calculated based on user-selected fitness goals.

Default recommendations:

| Macro         | Recommendation     |
| ------------- | ------------------ |
| Protein       | 2.0 g/kg           |
| Fat           | 0.7 g/kg           |
| Carbohydrates | Remaining calories |

---

## 🚀 Tech Stack

* Google AI Studio
* Android Studio
* Kotlin
* Material Design

---

## 📦 Getting Started

### Prerequisites

* Android Studio
* Gemini API Key

### Installation

1. Clone the repository

```bash
git clone https://github.com/yourusername/FitPal.git
```

2. Open the project in Android Studio.

3. Create a `.env` file in the project root.

```env
GEMINI_API_KEY=YOUR_API_KEY
```

4. Sync Gradle.

5. Run the application on an emulator or Android device.

---

## 🎯 Future Improvements

* BMI Calculator
* Body Fat Percentage Estimator
* Water Intake Calculator
* Meal Planner
* Progress Tracking
* Weight History Charts
* Dark Mode
* Fitness Goal History
* Google Fit Integration

---

## 🤝 Contributing

Contributions, issues, and feature requests are welcome.

Feel free to fork the repository and submit a pull request.

---

## 📄 License

This project is licensed under the MIT License.

---

## 👨‍💻 Author

**Shivansh Mishra**

Computer Engineering Student • Java Developer • AI Enthusiast

---

⭐ If you found this project helpful, consider giving it a star!
