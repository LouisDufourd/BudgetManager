# BudgetManager

**BudgetManager** is a personal finance management application designed to help users track their income and expenses. Built with Kotlin and using modern UI practices, this application aims to make budgeting easy and accessible.

![BudgetManager](https://cdn.discordapp.com/attachments/778608828417441812/1276061194385489993/image.png?ex=66c82818&is=66c6d698&hm=72ac3ee008d66c019c2e899bd988009bd5de2ce6cd9ce51563a03e8f21d48423&)  <!-- Replace with an actual screenshot or logo if available -->

## Features

- **Track Income and Expenses**: Log and categorize your transactions.
- **User-Friendly Interface**: Intuitive and responsive UI for an excellent user experience.

## Getting Started

### Prerequisites

To run BudgetManager, you need:

- **JDK 11 or higher**: Ensure you have Java Development Kit (JDK) version 11 or later installed.
- **Gradle**: For building the project. Gradle Wrapper is included with the project.

### Installation

1. **Clone the Repository**

   ```sh
   git clone https://github.com/LouisDufourd/BudgetManager.git
   ```

2. **Navigate to the Project Directory**
   ```sh
   cd BudgetManager
   ```
3. **Build the Project**
   
   Use Gradle to build the project. You can run:
   ```sh
    ./gradlew build
    ```
4. **Run the Application**
    
    To run the application, use:
   ```sh
   ./gradlew run
   ```
   Alternatively, you can find the executable JAR in the build/libs directory and run it with:
   ```sh
   java -jar build/libs/BudgetManager.jar
   ```

## Usage

1. **Launch the Application**

   Start the application using the provided commands.

2. **Load Transactions from CSV**

   To use the app, you need to download the list of transactions in CSV format. Here’s how to load your transactions:

    - Download the CSV file containing your transactions.
    - Open the application and go to `File -> Load from CSV File`.
    - Choose the downloaded CSV file to import your transactions into the application.