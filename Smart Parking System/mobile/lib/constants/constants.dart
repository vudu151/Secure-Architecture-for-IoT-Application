class AppConstants {
  // Use 10.0.2.2 for Android Emulator to connect to host's localhost
  // Use localhost or host IP for iOS Simulator or physical devices
  static const String apiBaseUrl = 'http://10.0.2.2:8080/api/v1';
  static const String wsUrl = 'ws://10.0.2.2:8080/ws/websocket';
  
  // Storage Keys
  static const String keyAccessToken = 'access_token';
  static const String keyRefreshToken = 'refresh_token';
  static const String keyUserEmail = 'user_email';
  static const String keyUserId = 'user_id';
  static const String keyUserRole = 'user_role';
  static const String keyUserFullName = 'user_fullname';
}
