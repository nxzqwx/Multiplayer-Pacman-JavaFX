# Pacman Multiplayer Hub

A multi-threaded Pacman game featuring a real-time synchronized chat system, demonstrating socket programming and concurrent execution in Java.

## 🚀 Features
- **Socket Programming:** Real-time TCP/IP communication between Client and Server.
- **Multi-threading:** Asynchronous message listening to prevent UI freezing.
- **Collision Logic:** Uses `PixelReader` for precise maze boundary detection.
- **Automated Docs:** Includes an XML Generator for source file indexing and Javadocs.

## 🛠️ Tech Stack
- **Language:** Java 8
- **Networking:** Java Sockets (TCP/IP)
- **GUI:** JavaFX
- **Documentation:** XML, Javadoc

## 🏗️ Architecture

The system uses a central **Server** to broadcast messages and multiple **Clients** that handle game logic and UI.