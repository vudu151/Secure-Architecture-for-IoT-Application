import 'dart:convert';
import 'package:http/http.dart' as http;
import 'package:shared_preferences/shared_preferences.dart';
import '../constants/constants.dart';

class ApiService {
  static final ApiService _instance = ApiService._internal();
  factory ApiService() => _instance;
  ApiService._internal();

  final http.Client _client = http.Client();

  Future<Map<String, String>> _getHeaders() async {
    final prefs = await SharedPreferences.getInstance();
    final token = prefs.getString(AppConstants.keyAccessToken);
    
    final headers = {
      'Content-Type': 'application/json',
      'Accept': 'application/json',
    };
    
    if (token != null && token.isNotEmpty) {
      headers['Authorization'] = 'Bearer $token';
    }
    
    return headers;
  }

  Future<http.Response> get(String path) async {
    final url = Uri.parse('${AppConstants.apiBaseUrl}$path');
    final headers = await _getHeaders();
    
    var response = await _client.get(url, headers: headers);
    
    if (response.statusCode == 401) {
      final refreshed = await _attemptTokenRefresh();
      if (refreshed) {
        final retryHeaders = await _getHeaders();
        response = await _client.get(url, headers: retryHeaders);
      }
    }
    
    return response;
  }

  Future<http.Response> post(String path, Map<String, dynamic>? body) async {
    final url = Uri.parse('${AppConstants.apiBaseUrl}$path');
    final headers = await _getHeaders();
    final bodyStr = body != null ? jsonEncode(body) : null;
    
    var response = await _client.post(url, headers: headers, body: bodyStr);
    
    if (response.statusCode == 401) {
      final refreshed = await _attemptTokenRefresh();
      if (refreshed) {
        final retryHeaders = await _getHeaders();
        response = await _client.post(url, headers: retryHeaders, body: bodyStr);
      }
    }
    
    return response;
  }

  Future<http.Response> put(String path, Map<String, dynamic>? body) async {
    final url = Uri.parse('${AppConstants.apiBaseUrl}$path');
    final headers = await _getHeaders();
    final bodyStr = body != null ? jsonEncode(body) : null;
    
    var response = await _client.put(url, headers: headers, body: bodyStr);
    
    if (response.statusCode == 401) {
      final refreshed = await _attemptTokenRefresh();
      if (refreshed) {
        final retryHeaders = await _getHeaders();
        response = await _client.put(url, headers: retryHeaders, body: bodyStr);
      }
    }
    
    return response;
  }

  Future<http.Response> delete(String path) async {
    final url = Uri.parse('${AppConstants.apiBaseUrl}$path');
    final headers = await _getHeaders();
    
    var response = await _client.delete(url, headers: headers);
    
    if (response.statusCode == 401) {
      final refreshed = await _attemptTokenRefresh();
      if (refreshed) {
        final retryHeaders = await _getHeaders();
        response = await _client.delete(url, headers: retryHeaders);
      }
    }
    
    return response;
  }

  Future<bool> _attemptTokenRefresh() async {
    final prefs = await SharedPreferences.getInstance();
    final refreshToken = prefs.getString(AppConstants.keyRefreshToken);
    
    if (refreshToken == null || refreshToken.isEmpty) {
      await _clearAuthData();
      return false;
    }

    try {
      final url = Uri.parse('${AppConstants.apiBaseUrl}/auth/refresh');
      final response = await http.post(
        url,
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode({'refreshToken': refreshToken}),
      );

      if (response.statusCode == 200) {
        final jsonResponse = jsonDecode(response.body);
        if (jsonResponse['success'] == true) {
          final data = jsonResponse['data'];
          final newAccessToken = data['accessToken'];
          final newRefreshToken = data['refreshToken'];
          
          await prefs.setString(AppConstants.keyAccessToken, newAccessToken);
          await prefs.setString(AppConstants.keyRefreshToken, newRefreshToken);
          return true;
        }
      }
    } catch (e) {
      // Log or handle network error
    }

    // If refresh fails, clear token data
    await _clearAuthData();
    return false;
  }

  Future<void> _clearAuthData() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove(AppConstants.keyAccessToken);
    await prefs.remove(AppConstants.keyRefreshToken);
    await prefs.remove(AppConstants.keyUserId);
    await prefs.remove(AppConstants.keyUserEmail);
    await prefs.remove(AppConstants.keyUserRole);
    await prefs.remove(AppConstants.keyUserFullName);
  }
}
