class TransactionModel {
  final int id;
  final String? bookingCode;
  final double amount;
  final String paymentMethod;
  final String paymentStatus;
  final String transactionRef;
  final DateTime createdAt;

  TransactionModel({
    required this.id,
    this.bookingCode,
    required this.amount,
    required this.paymentMethod,
    required this.paymentStatus,
    required this.transactionRef,
    required this.createdAt,
  });

  factory TransactionModel.fromJson(Map<String, dynamic> json) {
    return TransactionModel(
      id: json['id'] ?? 0,
      bookingCode: json['bookingCode'],
      amount: (json['amount'] as num?)?.toDouble() ?? 0.0,
      paymentMethod: json['paymentMethod'] ?? 'WALLET',
      paymentStatus: json['paymentStatus'] ?? 'PENDING',
      transactionRef: json['transactionRef'] ?? '',
      createdAt: DateTime.parse(json['createdAt']),
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'bookingCode': bookingCode,
      'amount': amount,
      'paymentMethod': paymentMethod,
      'paymentStatus': paymentStatus,
      'transactionRef': transactionRef,
      'createdAt': createdAt.toIso8601String(),
    };
  }
}
