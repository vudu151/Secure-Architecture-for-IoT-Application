import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:provider/provider.dart';
import 'package:intl/intl.dart';
import '../../providers/booking_provider.dart';
import '../../models/booking_model.dart';
import '../booking/booking_detail_screen.dart';

class BookingsTab extends StatelessWidget {
  const BookingsTab({super.key});

  @override
  Widget build(BuildContext context) {
    final bookingProvider = Provider.of<BookingProvider>(context);

    final activeBookings = bookingProvider.bookings
        .where((b) => b.status == BookingStatus.CONFIRMED || b.status == BookingStatus.CHECKED_IN)
        .toList();

    final historyBookings = bookingProvider.bookings
        .where((b) => b.status != BookingStatus.CONFIRMED && b.status != BookingStatus.CHECKED_IN)
        .toList();

    return DefaultTabController(
      length: 2,
      child: Scaffold(
        backgroundColor: Colors.grey[100],
        appBar: AppBar(
          title: Text(
            'My Bookings',
            style: GoogleFonts.outfit(fontWeight: FontWeight.bold, color: Colors.indigo[900]),
          ),
          backgroundColor: Colors.white,
          elevation: 0,
          bottom: TabBar(
            labelColor: Colors.indigo[800],
            unselectedLabelColor: Colors.grey[500],
            indicatorColor: Colors.indigo[800],
            indicatorWeight: 3,
            labelStyle: GoogleFonts.outfit(fontWeight: FontWeight.bold, fontSize: 15),
            tabs: const [
              Tab(text: 'Active'),
              Tab(text: 'History'),
            ],
          ),
        ),
        body: bookingProvider.isLoading
            ? const Center(child: CircularProgressIndicator())
            : TabBarView(
                children: [
                  _buildBookingsList(context, activeBookings, true),
                  _buildBookingsList(context, historyBookings, false),
                ],
              ),
      ),
    );
  }

  Widget _buildBookingsList(BuildContext context, List<BookingModel> list, bool isActiveTab) {
    if (list.isEmpty) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(Icons.bookmark_border_rounded, size: 70, color: Colors.grey[400]),
            const SizedBox(height: 10),
            Text(
              isActiveTab ? 'No active bookings found' : 'No booking history found',
              style: TextStyle(color: Colors.grey[600], fontSize: 16),
            ),
          ],
        ),
      );
    }

    return ListView.builder(
      padding: const EdgeInsets.symmetric(horizontal: 15, vertical: 15),
      itemCount: list.length,
      itemBuilder: (context, index) {
        final booking = list[index];
        return _buildBookingCard(context, booking, isActiveTab);
      },
    );
  }

  Widget _buildBookingCard(BuildContext context, BookingModel booking, bool isActiveTab) {
    Color statusColor;
    switch (booking.status) {
      case BookingStatus.CONFIRMED:
        statusColor = Colors.amber[700]!;
        break;
      case BookingStatus.CHECKED_IN:
        statusColor = Colors.green[600]!;
        break;
      case BookingStatus.COMPLETED:
        statusColor = Colors.indigo[700]!;
        break;
      case BookingStatus.CANCELLED:
        statusColor = Colors.red[600]!;
        break;
      case BookingStatus.EXPIRED:
        statusColor = Colors.grey[600]!;
        break;
      default:
        statusColor = Colors.grey;
    }

    return Card(
      elevation: 2,
      margin: const EdgeInsets.only(bottom: 15),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(15)),
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Top row: Slot info & Status Tag
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Row(
                  children: [
                    Container(
                      padding: const EdgeInsets.all(8),
                      decoration: BoxDecoration(
                        color: Colors.indigo[50],
                        borderRadius: BorderRadius.circular(8),
                      ),
                      child: Icon(Icons.local_parking_rounded, color: Colors.indigo[800], size: 20),
                    ),
                    const SizedBox(width: 10),
                    Text(
                      'Slot ${booking.slotCode}',
                      style: GoogleFonts.outfit(
                        fontSize: 18,
                        fontWeight: FontWeight.bold,
                        color: Colors.indigo[900],
                      ),
                    ),
                  ],
                ),
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                  decoration: BoxDecoration(
                    color: statusColor.withOpacity(0.1),
                    borderRadius: BorderRadius.circular(20),
                    border: Border.all(color: statusColor.withOpacity(0.4)),
                  ),
                  child: Text(
                    booking.status.name,
                    style: TextStyle(color: statusColor, fontWeight: FontWeight.bold, fontSize: 12),
                  ),
                ),
              ],
            ),
            const Divider(height: 25),
            // Middle section: Vehicle & Time
            _buildInfoRow(Icons.directions_car_filled_outlined, 'Vehicle', booking.vehiclePlate),
            const SizedBox(height: 8),
            _buildInfoRow(
              Icons.access_time_rounded, 
              'Time Window', 
              '${DateFormat('HH:mm').format(booking.bookedFrom)} - ${DateFormat('HH:mm, dd/MM').format(booking.bookedUntil)}'
            ),
            if (booking.checkedInAt != null) ...[
              const SizedBox(height: 8),
              _buildInfoRow(
                Icons.login_rounded, 
                'Checked In', 
                DateFormat('HH:mm, dd/MM/yyyy').format(booking.checkedInAt!)
              ),
            ],
            if (booking.checkedOutAt != null) ...[
              const SizedBox(height: 8),
              _buildInfoRow(
                Icons.logout_rounded, 
                'Checked Out', 
                DateFormat('HH:mm, dd/MM/yyyy').format(booking.checkedOutAt!)
              ),
              const SizedBox(height: 8),
              _buildInfoRow(
                Icons.attach_money_rounded, 
                'Amount Charged', 
                NumberFormat.currency(locale: 'vi_VN', symbol: 'đ').format(booking.totalAmount)
              ),
            ],
            // Bottom section: Actions
            if (isActiveTab) ...[
              const SizedBox(height: 15),
              Row(
                mainAxisAlignment: MainAxisAlignment.end,
                children: [
                  if (booking.status == BookingStatus.CONFIRMED) ...[
                    OutlinedButton(
                      onPressed: () => _confirmCancel(context, booking.id),
                      style: OutlinedButton.styleFrom(
                        foregroundColor: Colors.red[600],
                        side: BorderSide(color: Colors.red[300]!),
                        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
                      ),
                      child: const Text('Cancel'),
                    ),
                    const SizedBox(width: 10),
                  ],
                  ElevatedButton.icon(
                    onPressed: () {
                      Navigator.of(context).push(
                        MaterialPageRoute(
                          builder: (_) => BookingDetailScreen(booking: booking),
                        ),
                      );
                    },
                    icon: const Icon(Icons.qr_code_rounded, size: 18),
                    label: Text(booking.status == BookingStatus.CONFIRMED ? 'Ticket QR' : 'Check-out QR'),
                    style: ElevatedButton.styleFrom(
                      backgroundColor: Colors.indigo[800],
                      foregroundColor: Colors.white,
                      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
                    ),
                  ),
                ],
              ),
            ],
          ],
        ),
      ),
    );
  }

  Widget _buildInfoRow(IconData icon, String label, String value) {
    return Row(
      children: [
        Icon(icon, color: Colors.grey[500], size: 18),
        const SizedBox(width: 8),
        Text('$label: ', style: TextStyle(color: Colors.grey[600], fontSize: 13)),
        Expanded(
          child: Text(
            value,
            style: const TextStyle(fontWeight: FontWeight.w600, color: Colors.black85, fontSize: 13),
            overflow: TextOverflow.ellipsis,
          ),
        ),
      ],
    );
  }

  void _confirmCancel(BuildContext context, int bookingId) {
    showDialog(
      context: context,
      builder: (context) {
        return AlertDialog(
          title: const Text('Cancel Booking'),
          content: const Text('Are you sure you want to cancel this booking? This slot will be released for other drivers.'),
          actions: [
            TextButton(
              onPressed: () => Navigator.of(context).pop(),
              child: const Text('No'),
            ),
            TextButton(
              onPressed: () async {
                Navigator.of(context).pop();
                final bookingProvider = Provider.of<BookingProvider>(context, listen: false);
                final error = await bookingProvider.cancelBooking(bookingId);
                if (error == null) {
                  ScaffoldMessenger.of(context).showSnackBar(
                    const SnackBar(
                      content: Text('Booking cancelled successfully'),
                      backgroundColor: Colors.green,
                    ),
                  );
                } else {
                  ScaffoldMessenger.of(context).showSnackBar(
                    SnackBar(
                      content: Text(error),
                      backgroundColor: Colors.redAccent,
                    ),
                  );
                }
              },
              child: const Text('Yes, Cancel', style: TextStyle(color: Colors.red)),
            ),
          ],
        );
      },
    );
  }
}
