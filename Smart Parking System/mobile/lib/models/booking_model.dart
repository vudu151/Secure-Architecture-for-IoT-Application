enum BookingStatus { PENDING, CONFIRMED, CHECKED_IN, CHECKED_OUT, COMPLETED, CANCELLED, EXPIRED }

class BookingModel {
  final int id;
  final String slotCode;
  final String zone;
  final String vehiclePlate;
  final String bookingCode;
  final String qrCodeData;
  final BookingStatus status;
  final DateTime bookedFrom;
  final DateTime bookedUntil;
  final DateTime? checkedInAt;
  final DateTime? checkedOutAt;
  final double totalAmount;
  final DateTime createdAt;

  BookingModel({
    required this.id,
    required this.slotCode,
    required this.zone,
    required this.vehiclePlate,
    required this.bookingCode,
    required this.qrCodeData,
    required this.status,
    required this.bookedFrom,
    required this.bookedUntil,
    this.checkedInAt,
    this.checkedOutAt,
    required this.totalAmount,
    required this.createdAt,
  });

  factory BookingModel.fromJson(Map<String, dynamic> json) {
    return BookingModel(
      id: json['id'] ?? 0,
      slotCode: json['slotCode'] ?? '',
      zone: json['zone'] ?? '',
      vehiclePlate: json['vehiclePlate'] ?? '',
      bookingCode: json['bookingCode'] ?? '',
      qrCodeData: json['qrCodeData'] ?? '',
      status: _parseStatus(json['status']),
      bookedFrom: DateTime.parse(json['bookedFrom']),
      bookedUntil: DateTime.parse(json['bookedUntil']),
      checkedInAt: json['checkedInAt'] != null ? DateTime.parse(json['checkedInAt']) : null,
      checkedOutAt: json['checkedOutAt'] != null ? DateTime.parse(json['checkedOutAt']) : null,
      totalAmount: (json['totalAmount'] as num?)?.toDouble() ?? 0.0,
      createdAt: DateTime.parse(json['createdAt'] ?? DateTime.now().toIso8601String()),
    );
  }

  static BookingStatus _parseStatus(String? statusStr) {
    switch (statusStr?.toUpperCase()) {
      case 'PENDING':
        return BookingStatus.PENDING;
      case 'CONFIRMED':
        return BookingStatus.CONFIRMED;
      case 'CHECKED_IN':
        return BookingStatus.CHECKED_IN;
      case 'CHECKED_OUT':
        return BookingStatus.CHECKED_OUT;
      case 'COMPLETED':
        return BookingStatus.COMPLETED;
      case 'CANCELLED':
        return BookingStatus.CANCELLED;
      case 'EXPIRED':
        return BookingStatus.EXPIRED;
      default:
        return BookingStatus.PENDING;
    }
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'slotCode': slotCode,
      'zone': zone,
      'vehiclePlate': vehiclePlate,
      'bookingCode': bookingCode,
      'qrCodeData': qrCodeData,
      'status': status.name,
      'bookedFrom': bookedFrom.toIso8601String(),
      'bookedUntil': bookedUntil.toIso8601String(),
      'checkedInAt': checkedInAt?.toIso8601String(),
      'checkedOutAt': checkedOutAt?.toIso8601String(),
      'totalAmount': totalAmount,
      'createdAt': createdAt.toIso8601String(),
    };
  }
}
