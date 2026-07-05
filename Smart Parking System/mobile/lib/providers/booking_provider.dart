import 'dart:convert';
import 'package:flutter/material.dart';
import '../models/booking_model.dart';
import '../models/vehicle_model.dart';
import '../models/transaction_model.dart';
import '../services/api_service.dart';

class BookingProvider with ChangeNotifier {
  List<BookingModel> _bookings = [];
  List<VehicleModel> _vehicles = [];
  List<TransactionModel> _transactions = [];
  bool _isLoading = false;

  List<BookingModel> get bookings => _bookings;
  List<VehicleModel> get vehicles => _vehicles;
  List<TransactionModel> get transactions => _transactions;
  bool get isLoading => _isLoading;

  final ApiService _apiService = ApiService();

  Future<void> fetchBookings() async {
    _isLoading = true;
    notifyListeners();

    try {
      final response = await _apiService.get('/bookings/my');
      if (response.statusCode == 200) {
        final body = jsonDecode(response.body);
        if (body['success'] == true) {
          final List<dynamic> data = body['data'];
          _bookings = data.map((json) => BookingModel.fromJson(json)).toList();
        }
      }
    } catch (e) {
      print("Error fetching bookings: $e");
    }

    _isLoading = false;
    notifyListeners();
  }

  Future<void> fetchVehicles() async {
    _isLoading = true;
    notifyListeners();

    try {
      final response = await _apiService.get('/vehicles/my');
      if (response.statusCode == 200) {
        final body = jsonDecode(response.body);
        if (body['success'] == true) {
          final List<dynamic> data = body['data'];
          _vehicles = data.map((json) => VehicleModel.fromJson(json)).toList();
        }
      }
    } catch (e) {
      print("Error fetching vehicles: $e");
    }

    _isLoading = false;
    notifyListeners();
  }

  Future<void> fetchTransactions() async {
    _isLoading = true;
    notifyListeners();

    try {
      final response = await _apiService.get('/transactions/my');
      if (response.statusCode == 200) {
        final body = jsonDecode(response.body);
        if (body['success'] == true) {
          final List<dynamic> data = body['data'];
          _transactions = data.map((json) => TransactionModel.fromJson(json)).toList();
        }
      }
    } catch (e) {
      print("Error fetching transactions: $e");
    }

    _isLoading = false;
    notifyListeners();
  }

  Future<String?> createBooking({
    required int slotId,
    required int vehicleId,
    required DateTime bookedFrom,
    required DateTime bookedUntil,
  }) async {
    _isLoading = true;
    notifyListeners();

    try {
      final response = await _apiService.post(
        '/bookings',
        {
          'slotId': slotId,
          'vehicleId': vehicleId,
          'bookedFrom': bookedFrom.toIso8601String(),
          'bookedUntil': bookedUntil.toIso8601String(),
        },
      );

      final body = jsonDecode(response.body);
      _isLoading = false;
      notifyListeners();

      if (response.statusCode == 200 && body['success'] == true) {
        await fetchBookings(); // Refresh bookings list
        return null; // Success
      } else {
        return body['message'] ?? 'Failed to create booking';
      }
    } catch (e) {
      _isLoading = false;
      notifyListeners();
      return 'Network error: Failed to reach backend';
    }
  }

  Future<String?> cancelBooking(int bookingId) async {
    _isLoading = true;
    notifyListeners();

    try {
      final response = await _apiService.post('/bookings/$bookingId/cancel', null);
      final body = jsonDecode(response.body);
      _isLoading = false;
      notifyListeners();

      if (response.statusCode == 200 && body['success'] == true) {
        await fetchBookings();
        return null;
      } else {
        return body['message'] ?? 'Failed to cancel booking';
      }
    } catch (e) {
      _isLoading = false;
      notifyListeners();
      return 'Network error';
    }
  }

  Future<String?> addVehicle(String licensePlate, String vehicleType) async {
    _isLoading = true;
    notifyListeners();

    try {
      final response = await _apiService.post(
        '/vehicles',
        {
          'licensePlate': licensePlate,
          'vehicleType': vehicleType.toUpperCase(),
        },
      );

      final body = jsonDecode(response.body);
      _isLoading = false;
      notifyListeners();

      if (response.statusCode == 200 && body['success'] == true) {
        await fetchVehicles();
        return null;
      } else {
        return body['message'] ?? 'Failed to add vehicle';
      }
    } catch (e) {
      _isLoading = false;
      notifyListeners();
      return 'Network error';
    }
  }

  Future<String?> deleteVehicle(int vehicleId) async {
    _isLoading = true;
    notifyListeners();

    try {
      final response = await _apiService.delete('/vehicles/$vehicleId');
      final body = jsonDecode(response.body);
      _isLoading = false;
      notifyListeners();

      if (response.statusCode == 200 && body['success'] == true) {
        await fetchVehicles();
        return null;
      } else {
        return body['message'] ?? 'Failed to delete vehicle';
      }
    } catch (e) {
      _isLoading = false;
      notifyListeners();
      return 'Network error';
    }
  }

  Future<double?> topup(double amount) async {
    _isLoading = true;
    notifyListeners();

    try {
      final response = await _apiService.post(
        '/wallet/topup',
        {'amount': amount},
      );

      final body = jsonDecode(response.body);
      _isLoading = false;
      notifyListeners();

      if (response.statusCode == 200 && body['success'] == true) {
        await fetchTransactions(); // Refresh transactions
        return (body['data'] as num).toDouble(); // Return new balance
      }
    } catch (e) {
      print("Error topping up: $e");
    }

    _isLoading = false;
    notifyListeners();
    return null;
  }
}
