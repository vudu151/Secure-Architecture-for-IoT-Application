import 'dart:async';
import 'dart:convert';
import 'package:web_socket_channel/web_socket_channel.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../constants/constants.dart';

class WebSocketService {
  static final WebSocketService _instance = WebSocketService._internal();
  factory WebSocketService() => _instance;
  WebSocketService._internal();

  WebSocketChannel? _channel;
  bool _isConnected = false;
  final _messageController = StreamController<Map<String, dynamic>>.broadcast();

  Stream<Map<String, dynamic>> get messages => _messageController.stream;
  bool get isConnected => _isConnected;

  Future<void> connect() async {
    if (_isConnected) return;

    final prefs = await SharedPreferences.getInstance();
    final token = prefs.getString(AppConstants.keyAccessToken);
    
    if (token == null) {
      print("WS: Cannot connect, no access token found");
      return;
    }

    try {
      final wsUri = Uri.parse(AppConstants.wsUrl);
      _channel = WebSocketChannel.connect(wsUri);
      _isConnected = true;
      print("WS: Connected to $wsUri");

      // Send STOMP CONNECT frame
      _sendStompFrame(
        'CONNECT',
        {
          'accept-version': '1.1,1.2',
          'heart-beat': '10000,10000',
          'Authorization': 'Bearer $token',
        },
        null,
      );

      _channel!.stream.listen(
        (message) {
          _handleRawMessage(message as String);
        },
        onError: (error) {
          print("WS Error: $error");
          _handleDisconnect();
        },
        onDone: () {
          print("WS Connection Closed");
          _handleDisconnect();
        },
      );
    } catch (e) {
      print("WS Connection exception: $e");
      _isConnected = false;
    }
  }

  void _handleDisconnect() {
    _isConnected = false;
    _channel = null;
    // Auto reconnect after 5 seconds
    Timer(const Duration(seconds: 5), () {
      print("WS: Attempting auto reconnect...");
      connect();
    });
  }

  void subscribe(String destination) {
    if (!_isConnected) return;
    print("WS: Subscribing to $destination");
    _sendStompFrame(
      'SUBSCRIBE',
      {
        'id': 'sub-${destination.hashCode}',
        'destination': destination,
      },
      null,
    );
  }

  void _sendStompFrame(String command, Map<String, String> headers, String? body) {
    if (_channel == null) return;
    
    var frame = '$command\n';
    headers.forEach((key, value) {
      frame += '$key:$value\n';
    });
    frame += '\n'; // Blank line between headers and body
    if (body != null) {
      frame += body;
    }
    frame += '\x00'; // Null character terminator for STOMP
    
    _channel!.sink.add(frame);
  }

  void _handleRawMessage(String rawFrame) {
    // Basic STOMP Frame Parser
    try {
      final lines = rawFrame.split('\n');
      if (lines.isEmpty) return;

      final command = lines[0].trim();
      if (command == 'CONNECTED') {
        print("WS: STOMP Connection established");
        // Auto subscribe to slots topic once connected
        subscribe('/topic/slots');
        return;
      }

      if (command == 'MESSAGE') {
        // Find body (after double newlines)
        final doubleNewlineIndex = rawFrame.indexOf('\n\n');
        if (doubleNewlineIndex == -1) return;

        var body = rawFrame.substring(doubleNewlineIndex + 2);
        // Remove NULL character terminator
        if (body.endsWith('\x00')) {
          body = body.substring(0, body.length - 1);
        }
        body = body.trim();

        if (body.isNotEmpty) {
          final data = jsonDecode(body) as Map<String, dynamic>;
          _messageController.add(data);
        }
      }
    } catch (e) {
      print("WS Error parsing STOMP frame: $e");
    }
  }

  void disconnect() {
    if (_channel != null) {
      _sendStompFrame('DISCONNECT', {}, null);
      _channel!.sink.close();
      _isConnected = false;
      _channel = null;
      print("WS: Disconnected manually");
    }
  }
}
