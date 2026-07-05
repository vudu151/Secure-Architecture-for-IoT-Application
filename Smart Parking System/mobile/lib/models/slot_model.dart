enum SlotStatus { AVAILABLE, OCCUPIED, RESERVED, MAINTENANCE }

class SlotModel {
  final int id;
  final String slotCode;
  final String zone;
  final SlotStatus status;
  final String? sensorId;

  SlotModel({
    required this.id,
    required this.slotCode,
    required this.zone,
    required this.status,
    this.sensorId,
  });

  factory SlotModel.fromJson(Map<String, dynamic> json) {
    return SlotModel(
      id: json['id'] ?? 0,
      slotCode: json['slotCode'] ?? '',
      zone: json['zone'] ?? '',
      status: _parseStatus(json['status']),
      sensorId: json['sensorId'],
    );
  }

  static SlotStatus _parseStatus(String? statusStr) {
    switch (statusStr?.toUpperCase()) {
      case 'OCCUPIED':
        return SlotStatus.OCCUPIED;
      case 'RESERVED':
        return SlotStatus.RESERVED;
      case 'MAINTENANCE':
        return SlotStatus.MAINTENANCE;
      case 'AVAILABLE':
      default:
        return SlotStatus.AVAILABLE;
    }
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'slotCode': slotCode,
      'zone': zone,
      'status': status.name,
      'sensorId': sensorId,
    };
  }
}
