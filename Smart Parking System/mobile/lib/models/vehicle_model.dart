class VehicleModel {
  final int id;
  final String licensePlate;
  final String vehicleType;
  final bool isDefault;

  VehicleModel({
    required this.id,
    required this.licensePlate,
    required this.vehicleType,
    required this.isDefault,
  });

  factory VehicleModel.fromJson(Map<String, dynamic> json) {
    return VehicleModel(
      id: json['id'] ?? 0,
      licensePlate: json['licensePlate'] ?? '',
      vehicleType: json['vehicleType'] ?? 'CAR',
      isDefault: json['isDefault'] ?? false,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'licensePlate': licensePlate,
      'vehicleType': vehicleType,
      'isDefault': isDefault,
    };
  }
}
