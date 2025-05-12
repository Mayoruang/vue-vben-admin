# Drone9 - Drone Management System

A comprehensive management system for drone registration, approval, and monitoring.

## Project Structure

- **Backend**: Spring Boot application with REST API endpoints for drone registration and management
  - Controllers, services, and repositories for handling drone data
  - WebSocket integration for real-time updates
  - JWT authentication for secure access

- **Frontend**: Vue.js application built on vue-vben-admin framework
  - Registration management interface with statistics and filtering
  - Approval/rejection functionality with detailed information
  - Real-time updates via WebSocket

## Features

- **Drone Registration Flow**: Complete workflow for drone registration, approval, and management
- **Real-time Updates**: WebSocket integration for immediate status changes
- **System Monitoring**: Dashboard for monitoring system components and overall health
- **User Authentication**: Secure login and role-based access control

## Technologies

- **Backend**: Spring Boot, Spring Security, Spring Data JPA, PostgreSQL, WebSocket
- **Frontend**: Vue.js, Ant Design, WebSocket
- **Infrastructure**: Docker services for PostgreSQL, InfluxDB, and EMQX

## Getting Started

1. Clone the repository
2. Run the backend Spring Boot application
3. Start the frontend Vue.js application
4. Access the system at http://localhost:3100

## License

This project is proprietary software. 