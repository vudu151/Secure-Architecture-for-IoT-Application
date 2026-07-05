import 'dart:convert';
import 'package:flutter/material.dart';
import '../models/slot_model.dart';
import '../services/api_service.dart';
import '../services/websocket_service.dart';

class ParkingProvider with ChangeNotifier {
  List<SlotModel> _slots = [];
  bool _isLoading = false;

  List<SlotModel> get slots => _slots;
  bool get isLoading => _isLoading;

  final ApiService _apiService = ApiService();
  final WebSocketService _wsService = WebSocketService();

  ParkingProvider() {
    // Listen to WebSocket broadcasts for real-time slot updates
    _wsService.messages.listen((slotData) {
      _handleWebSocketUpdate(slotData);
    });
  }

  Future<void> fetchSlots() async {
    _isLoading = true;
    notifyListeners();

    try {
      final response = await _apiService.get('/slots');
      if (response.statusCode == 200) {
        final body = jsonDecode(response.body);
        if (body['success'] == true) {
          final List<dynamic> data = body['data'];
          _slots = data.map((json) => SlotModel.fromJson(json)).toList();
        }
      }
    } catch (e) {
      print("Error fetching slots: $e");
    }

    _isLoading = false;
    notifyListeners();
  }

  void _handleWebSocketUpdate(Map<String, dynamic> slotData) {
    try {
      final updatedSlot = SlotModel.fromJson(slotData);
      final index = _slots.indexWhere((s) => s.id == updatedSlot.id);
      
      if (index != -1) {
        _slots[index] = updatedSlot;
        notifyListeners();
        print("WS: Updated slot ${updatedSlot.slotCode} status to ${updatedSlot.status.name}");
      }
    } catch (e) {
      print("WS Error processing slot payload: $e");
    }
  }
}
