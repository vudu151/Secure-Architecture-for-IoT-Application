import 'dart:async';
import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:qr_flutter/qr_flutter.dart';
import 'package:intl/intl.dart';
import '../../models/booking_model.dart';

class BookingDetailScreen extends StatefulWidget {
  final BookingModel booking;

  const BookingDetailScreen({super.key, required this.booking});

  @override
  State<BookingDetailScreen> createState() => _BookingDetailScreenState();
}

class _BookingDetailScreenState extends State<BookingDetailScreen> {
  Timer? _timer;
  Duration _timeLeft = Duration.zero;

  @override
  void initState() {
    super.initState();
    _startTimer();
  }

  @override
  void dispose() {
    _timer?.cancel();
    super.dispose();
  }

  void _startTimer() {
    if (widget.booking.status != BookingStatus.CONFIRMED) return;

    // Check-in timeout: bookedFrom + 20 minutes
    final deadline = widget.booking.bookedFrom.add(const Duration(minutes: 20));
    _calculateTimeLeft(deadline);

    _timer = Timer.periodic(const Duration(seconds: 1), (timer) {
      _calculateTimeLeft(deadline);
    });
  }

  void _calculateTimeLeft(DateTime deadline) {
    final now = DateTime.now();
    if (now.isAfter(deadline)) {
      setState(() {
        _timeLeft = Duration.zero;
      });
      _timer?.cancel();
    } else {
      setState(() {
        _timeLeft = deadline.difference(now);
      });
    }
  }

  String _formatDuration(Duration duration) {
    String twoDigits(int n) => n.toString().padLeft(2, '0');
    final minutes = twoDigits(duration.inMinutes.remainder(60));
    final seconds = twoDigits(duration.inSeconds.remainder(60));
    return "$minutes:$seconds";
  }

  @override
  Widget build(BuildContext context) {
    final isConfirmed = widget.booking.status == BookingStatus.CONFIRMED;
    final isCheckedIn = widget.booking.status == BookingStatus.CHECKED_IN;

    return Scaffold(
      backgroundColor: Colors.indigo[900],
      appBar: AppBar(
        title: Text(
          'Parking Ticket',
          style: GoogleFonts.outfit(fontWeight: FontWeight.bold, color: Colors.white),
        ),
        backgroundColor: Colors.transparent,
        elevation: 0,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back_ios, color: Colors.white),
          onPressed: () => Navigator.of(context).pop(),
        ),
      ),
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsets.symmetric(horizontal: 25, vertical: 10),
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                // Ticket Card
                Container(
                  decoration: BoxDecoration(
                    color: Colors.white,
                    borderRadius: BorderRadius.circular(25),
                    boxShadow: [
                      BoxShadow(
                        color: Colors.black.withOpacity(0.3),
                        blurRadius: 15,
                        spreadRadius: 5,
                      )
                    ],
                  ),
                  child: Column(
                    children: [
                      // Header Section of Ticket
                      Container(
                        padding: const EdgeInsets.all(20),
                        decoration: BoxDecoration(
                          color: Colors.indigo[50],
                          borderRadius: const BorderRadius.vertical(top: Radius.circular(25)),
                        ),
                        child: Row(
                          mainAxisAlignment: MainAxisAlignment.spaceBetween,
                          children: [
                            Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text(
                                  'SLOT CODE',
                                  style: TextStyle(color: Colors.indigo[300], fontSize: 10, fontWeight: FontWeight.bold),
                                ),
                                Text(
                                  widget.booking.slotCode,
                                  style: GoogleFonts.outfit(
                                    fontSize: 28,
                                    fontWeight: FontWeight.bold,
                                    color: Colors.indigo[900],
                                  ),
                                ),
                              ],
                            ),
                            Column(
                              crossAxisAlignment: CrossAxisAlignment.end,
                              children: [
                                Text(
                                  'ZONE',
                                  style: TextStyle(color: Colors.indigo[300], fontSize: 10, fontWeight: FontWeight.bold),
                                ),
                                Text(
                                  'Zone ${widget.booking.zone}',
                                  style: GoogleFonts.outfit(
                                    fontSize: 22,
                                    fontWeight: FontWeight.bold,
                                    color: Colors.indigo[900],
                                  ),
                                ),
                              ],
                            ),
                          ],
                        ),
                      ),
                      
                      // QR Code Container
                      Padding(
                        padding: const EdgeInsets.symmetric(vertical: 25.0, horizontal: 40.0),
                        child: Container(
                          padding: const EdgeInsets.all(15),
                          decoration: BoxDecoration(
                            border: Border.all(color: Colors.indigo[100]!, width: 2),
                            borderRadius: BorderRadius.circular(20),
                          ),
                          child: QrImageView(
                            data: widget.booking.qrCodeData,
                            version: QrVersions.auto,
                            size: 200.0,
                            foregroundColor: Colors.indigo[900],
                          ),
                        ),
                      ),
                      
                      // Ticket details
                      Padding(
                        padding: const EdgeInsets.symmetric(horizontal: 25.0),
                        child: Column(
                          children: [
                            _buildTicketRow('Booking Code', widget.booking.bookingCode),
                            const SizedBox(height: 10),
                            _buildTicketRow('License Plate', widget.booking.vehiclePlate),
                            const SizedBox(height: 10),
                            _buildTicketRow('Status', widget.booking.status.name),
                            const SizedBox(height: 10),
                            _buildTicketRow(
                              'Time Window', 
                              '${DateFormat('HH:mm').format(widget.booking.bookedFrom)} - ${DateFormat('HH:mm').format(widget.booking.bookedUntil)}'
                            ),
                            if (widget.booking.checkedInAt != null) ...[
                              const SizedBox(height: 10),
                              _buildTicketRow('Check-In At', DateFormat('HH:mm, dd/MM').format(widget.booking.checkedInAt!)),
                            ],
                          ],
                        ),
                      ),
                      
                      // Ticket Divider
                      const SizedBox(height: 20),
                      Row(
                        children: List.generate(
                          30, 
                          (index) => Expanded(
                            child: Container(
                              color: index % 2 == 0 ? Colors.transparent : Colors.grey[300],
                              height: 2,
                            ),
                          ),
                        ),
                      ),
                      
                      // Info / Instructions Section
                      Padding(
                        padding: const EdgeInsets.all(25.0),
                        child: Column(
                          children: [
                            if (isConfirmed) ...[
                              Text(
                                'Check-in Deadline Counter',
                                style: TextStyle(color: Colors.grey[500], fontSize: 12),
                              ),
                              const SizedBox(height: 5),
                              Text(
                                _timeLeft == Duration.zero ? 'EXPIRED' : _formatDuration(_timeLeft),
                                style: GoogleFonts.outfit(
                                  fontSize: 32,
                                  fontWeight: FontWeight.bold,
                                  color: Colors.amber[800],
                                ),
                              ),
                              const SizedBox(height: 10),
                              const Text(
                                'Scan this QR code at the entrance scanner, or wait for the ANPR camera to automatically verify your license plate.',
                                textAlign: TextAlign.center,
                                style: TextStyle(color: Colors.black54, fontSize: 12),
                              ),
                            ],
                            if (isCheckedIn) ...[
                              const Icon(Icons.verified_user_rounded, color: Colors.green, size: 40),
                              const SizedBox(height: 5),
                              Text(
                                'CHECKED IN',
                                style: GoogleFonts.outfit(
                                  fontSize: 20,
                                  fontWeight: FontWeight.bold,
                                  color: Colors.green[700],
                                ),
                              ),
                              const SizedBox(height: 10),
                              const Text(
                                'Scan this QR code at the exit gate scanner to pay, process check-out and open the barrier automatically.',
                                textAlign: TextAlign.center,
                                style: TextStyle(color: Colors.black54, fontSize: 12),
                              ),
                            ],
                            if (!isConfirmed && !isCheckedIn) ...[
                              const Icon(Icons.history, color: Colors.grey, size: 40),
                              const SizedBox(height: 5),
                              Text(
                                'TICKET USED',
                                style: GoogleFonts.outfit(
                                  fontSize: 18,
                                  fontWeight: FontWeight.bold,
                                  color: Colors.grey[600],
                                ),
                              ),
                            ]
                          ],
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildTicketRow(String label, String value) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        Text(label, style: const TextStyle(color: Colors.grey, fontSize: 13)),
        Text(
          value,
          style: TextStyle(fontWeight: FontWeight.bold, color: Colors.indigo[900], fontSize: 13),
        ),
      ],
    );
  }
}
