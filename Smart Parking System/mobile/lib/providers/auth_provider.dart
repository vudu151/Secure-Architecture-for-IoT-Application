import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;
import 'package:shared_preferences/shared_preferences.dart';
import '../constants/constants.dart';
import '../models/user_model.dart';
import '../services/api_service.dart';
import '../services/websocket_service.dart';

class AuthProvider with ChangeNotifier {
  UserModel? _user;
  bool _isAuthenticated = false;
  bool _isLoading = false;

  UserModel? get user => _user;
  bool get isAuthenticated => _isAuthenticated;
  bool get isLoading => _isLoading;

  final ApiService _apiService = ApiService();

  Future<bool> checkAuth() async {
    _isLoading = true;
    notifyListeners();

    final prefs = await SharedPreferences.getInstance();
    final token = prefs.getString(AppConstants.keyAccessToken);
    
    if (token == null || token.isEmpty) {
      _isAuthenticated = false;
      _isLoading = false;
      notifyListeners();
      return false;
    }

    try {
      // Call endpoint to get current user balance & profile info
      final response = await _apiService.get('/wallet/balance');
      if (response.statusCode == 200) {
        final email = prefs.getString(AppConstants.keyUserEmail) ?? '';
        final fullName = prefs.getString(AppConstants.keyUserFullName) ?? '';
        final phone = prefs.getString(AppConstants.keyUserFullName) ?? ''; // Fallback
        final role = prefs.getString(AppConstants.keyUserRole) ?? 'DRIVER';
        final id = prefs.getInt(AppConstants.keyUserId) ?? 0;
        
        final body = jsonDecode(response.body);
        final balance = (body['data'] as num).toDouble();

        _user = UserModel(
          id: id,
          email: email,
          fullName: fullName,
          phone: phone,
          role: role,
          balance: balance,
          isActive: true,
        );
        _isAuthenticated = true;
        
        // Auto connect WebSocket
        WebSocketService().connect();
      } else {
        _isAuthenticated = false;
        _user = null;
      }
    } catch (e) {
      _isAuthenticated = false;
      _user = null;
    }

    _isLoading = false;
    notifyListeners();
    return _isAuthenticated;
  }

  Future<String?> login(String email, String password) async {
    _isLoading = true;
    notifyListeners();

    try {
      final url = Uri.parse('${AppConstants.apiBaseUrl}/auth/login');
      final response = await http.post(
        url,
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode({
          'email': email,
          'password': password,
        }),
      );

      final body = jsonDecode(response.body);
      if (response.statusCode == 200 && body['success'] == true) {
        final data = body['data'];
        final accessToken = data['accessToken'];
        final refreshToken = data['refreshToken'];

        final prefs = await SharedPreferences.getInstance();
        await prefs.setString(AppConstants.keyAccessToken, accessToken);
        await prefs.setString(AppConstants.keyRefreshToken, refreshToken);

        // JWT decoding simplified to get payload details or we can use profile API
        // For simplicity, we decode JWT subject & claims in standard base64 if needed, 
        // but here we can just perform a wallet query to check connectivity and fetch details
        await prefs.setString(AppConstants.keyUserEmail, email);
        
        _isAuthenticated = true;
        _isLoading = false;
        
        // Load actual balance & details
        await checkAuth();
        return null; // No error
      } else {
        _isLoading = false;
        notifyListeners();
        return body['message'] ?? 'Login failed';
      }
    } catch (e) {
      _isLoading = false;
      notifyListeners();
      return 'Network error: Please check your connection';
    }
  }

  Future<String?> register(String fullName, String email, String phone, String password) async {
    _isLoading = true;
    notifyListeners();

    try {
      final url = Uri.parse('${AppConstants.apiBaseUrl}/auth/register');
      final response = await http.post(
        url,
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode({
          'fullName': fullName,
          'email': email,
          'phone': phone,
          'password': password,
        }),
      );

      final body = jsonDecode(response.body);
      _isLoading = false;
      notifyListeners();

      if (response.statusCode == 200 && body['success'] == true) {
        return null; // Success
      } else {
        return body['message'] ?? 'Registration failed';
      }
    } catch (e) {
      _isLoading = false;
      notifyListeners();
      return 'Network error: Please check your connection';
    }
  }

  Future<void> logout() async {
    _isLoading = true;
    notifyListeners();

    try {
      await _apiService.post('/auth/logout', null);
    } catch (e) {
      // Ignore network errors on logout
    }

    final prefs = await SharedPreferences.getInstance();
    await prefs.clear();

    _user = null;
    _isAuthenticated = false;
    _isLoading = false;
    
    WebSocketService().disconnect();
    
    notifyListeners();
  }

  void updateBalance(double newBalance) {
    if (_user != null) {
      _user = UserModel(
        id: _user!.id,
        email: _user!.email,
        fullName: _user!.fullName,
        phone: _user!.phone,
        role: _user!.role,
        balance: newBalance,
        isActive: _user!.isActive,
      );
      notifyListeners();
    }
  }
}
