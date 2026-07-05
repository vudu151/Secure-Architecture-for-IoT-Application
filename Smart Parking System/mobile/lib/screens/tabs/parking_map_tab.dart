import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:provider/provider.dart';
import 'package:intl/intl.dart';
import '../../providers/auth_provider.dart';
import '../../providers/parking_provider.dart';
import '../../providers/booking_provider.dart';
import '../../models/slot_model.dart';
import '../../models/vehicle_model.dart';

class ParkingMapTab extends StatefulWidget {
  const ParkingMapTab({super.key});

  @override
  State<ParkingMapTab> createState() => _ParkingMapTabState();
}

class _ParkingMapTabState extends State<ParkingMapTab> {
  String _selectedZone = 'A';

  @override
  Widget build(BuildContext context) {
    final parkingProvider = Provider.of<ParkingProvider>(context);
    final authProvider = Provider.of<AuthProvider>(context);
    
    final filteredSlots = parkingProvider.slots
        .where((slot) => slot.zone.toUpperCase() == _selectedZone)
        .toList();

    return Scaffold(
      backgroundColor: Colors.grey[100],
      appBar: AppBar(
        title: Text(
          'Parking Slots Map',
          style: GoogleFonts.outfit(fontWeight: FontWeight.bold, color: Colors.indigo[900]),
        ),
        backgroundColor: Colors.white,
        elevation: 0,
        actions: [
          IconButton(
            icon: Icon(Icons.refresh, color: Colors.indigo[900]),
            onPressed: () => parkingProvider.fetchSlots(),
          )
        ],
      ),
      body: Column(
        children: [
          // Header / Info Bar
          Container(
            color: Colors.white,
            padding: const EdgeInsets.symmetric(vertical: 10, horizontal: 20),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text('Driver Balance', style: TextStyle(color: Colors.grey, fontSize: 13)),
                    const SizedBox(height: 2),
                    Text(
                      NumberFormat.currency(locale: 'vi_VN', symbol: 'đ')
                          .format(authProvider.user?.balance ?? 0),
                      style: GoogleFonts.outfit(
                        fontSize: 16,
                        fontWeight: FontWeight.bold,
                        color: Colors.indigo[800],
                      ),
                    ),
                  ],
                ),
                // Zone Selector Tab
                Row(
                  children: ['A', 'B'].map((zone) {
                    final isSelected = _selectedZone == zone;
                    return GestureDetector(
                      onTap: () => setState(() => _selectedZone = zone),
                      child: Container(
                        margin: const EdgeInsets.only(left: 10),
                        padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 8),
                        decoration: BoxDecoration(
                          color: isSelected ? Colors.indigo[800] : Colors.grey[200],
                          borderRadius: BorderRadius.circular(10),
                        ),
                        child: Text(
                          'Zone $zone',
                          style: TextStyle(
                            color: isSelected ? Colors.white : Colors.black85,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                      ),
                    );
                  }).toList(),
                ),
              ],
            ),
          ),
          const SizedBox(height: 10),
          // Map Status Legends
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 20.0),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                _buildLegendItem('Available', Colors.green),
                _buildLegendItem('Reserved', Colors.amber),
                _buildLegendItem('Occupied', Colors.red),
                _buildLegendItem('Maintenance', Colors.grey),
              ],
            ),
          ),
          const SizedBox(height: 15),
          // Slots Grid Map
          Expanded(
            child: parkingProvider.isLoading
                ? const Center(child: CircularProgressIndicator())
                : filteredSlots.isEmpty
                    ? const Center(child: Text('No slots registered in this zone'))
                    : GridView.builder(
                        padding: const EdgeInsets.symmetric(horizontal: 20),
                        gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                          crossAxisCount: 3,
                          crossAxisSpacing: 15,
                          mainAxisSpacing: 15,
                          childAspectRatio: 0.95,
                        ),
                        itemCount: filteredSlots.size,
                        itemBuilder: (context, index) {
                          final slot = filteredSlots[index];
                          return _buildSlotCard(slot);
                        },
                      ),
          ),
        ],
      ),
    );
  }

  Widget _buildLegendItem(String title, Color color) {
    return Row(
      children: [
        Container(
          width: 12,
          height: 12,
          decoration: BoxDecoration(
            color: color,
            shape: BoxShape.circle,
          ),
        ),
        const SizedBox(width: 5),
        Text(title, style: const TextStyle(fontSize: 11, color: Colors.black54)),
      ],
    );
  }

  Widget _buildSlotCard(SlotModel slot) {
    Color cardColor;
    Color textColor = Colors.white;
    IconData icon;

    switch (slot.status) {
      case SlotStatus.AVAILABLE:
        cardColor = Colors.green[600]!;
        icon = Icons.local_parking;
        break;
      case SlotStatus.RESERVED:
        cardColor = Colors.amber[600]!;
        icon = Icons.bookmark_added;
        break;
      case SlotStatus.OCCUPIED:
        cardColor = Colors.red[600]!;
        icon = Icons.directions_car_filled_rounded;
        break;
      case SlotStatus.MAINTENANCE:
        cardColor = Colors.grey[500]!;
        icon = Icons.build_circle_outlined;
        break;
    }

    return GestureDetector(
      onTap: () {
        if (slot.status == SlotStatus.AVAILABLE) {
          _showBookingBottomSheet(slot);
        } else {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: Text('Slot ${slot.slotCode} is current not available (${slot.status.name})'),
              behavior: SnackBarBehavior.floating,
            ),
          );
        }
      },
      child: Container(
        decoration: BoxDecoration(
          color: cardColor,
          borderRadius: BorderRadius.circular(15),
          boxShadow: [
            BoxShadow(
              color: cardColor.withOpacity(0.3),
              blurRadius: 6,
              offset: const Offset(0, 3),
            )
          ],
        ),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(icon, color: textColor, size: 30),
            const SizedBox(height: 6),
            Text(
              slot.slotCode,
              style: GoogleFonts.outfit(
                fontSize: 18,
                fontWeight: FontWeight.bold,
                color: textColor,
              ),
            ),
            Text(
              slot.status.name,
              style: TextStyle(
                fontSize: 10,
                color: textColor.withOpacity(0.8),
              ),
            ),
          ],
        ),
      ),
    );
  }

  void _showBookingBottomSheet(SlotModel slot) {
    final bookingProvider = Provider.of<BookingProvider>(context, listen: false);
    VehicleModel? selectedVehicle;
    
    // Default duration is 2 hours
    DateTime bookedFrom = DateTime.now();
    DateTime bookedUntil = DateTime.now().add(const Duration(hours: 2));

    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (context) {
        return StatefulBuilder(
          builder: (context, setModalState) {
            final vehicles = bookingProvider.vehicles;
            if (selectedVehicle == null && vehicles.isNotEmpty) {
              selectedVehicle = vehicles.firstWhere(
                (v) => v.isDefault, 
                orElse: () => vehicles.first
              );
            }

            final estimatedCost = (bookedUntil.difference(bookedFrom).inMinutes / 60.0) * 5000.0;

            return Container(
              decoration: const BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.vertical(top: Radius.circular(25)),
              ),
              padding: const EdgeInsets.symmetric(horizontal: 25.0, vertical: 20.0),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  Center(
                    child: Container(
                      width: 50,
                      height: 5,
                      decoration: BoxDecoration(
                        color: Colors.grey[300],
                        borderRadius: BorderRadius.circular(5),
                      ),
                    ),
                  ),
                  const SizedBox(height: 20),
                  Text(
                    'Reserve Slot ${slot.slotCode}',
                    style: GoogleFonts.outfit(
                      fontSize: 22,
                      fontWeight: FontWeight.bold,
                      color: Colors.indigo[900],
                    ),
                  ),
                  Text(
                    'Zone: ${slot.zone} | Standard rate: 5,000 đ / hour',
                    style: const TextStyle(color: Colors.grey),
                  ),
                  const Divider(height: 30),
                  
                  // Vehicle Selector
                  const Text('Select Your Vehicle', style: TextStyle(fontWeight: FontWeight.bold)),
                  const SizedBox(height: 10),
                  vehicles.isEmpty
                      ? Container(
                          padding: const EdgeInsets.all(15),
                          decoration: BoxDecoration(
                            color: Colors.amber[50],
                            borderRadius: BorderRadius.circular(10),
                            border: Border.all(color: Colors.amber[200]!),
                          ),
                          child: const Text(
                            'Please add a vehicle first in your Profile tab before booking.',
                            style: TextStyle(color: Colors.amber[800]),
                          ),
                        )
                      : Container(
                          padding: const EdgeInsets.symmetric(horizontal: 15),
                          decoration: BoxDecoration(
                            border: Border.all(color: Colors.grey[300]!),
                            borderRadius: BorderRadius.circular(10),
                          ),
                          child: DropdownButtonHideUnderline(
                            child: DropdownButton<VehicleModel>(
                              value: selectedVehicle,
                              isExpanded: true,
                              items: vehicles.map((v) {
                                return DropdownMenuItem<VehicleModel>(
                                  value: v,
                                  child: Text('${v.licensePlate} (${v.vehicleType})'),
                                );
                              }).toList(),
                              onChanged: (v) {
                                setModalState(() => selectedVehicle = v);
                              },
                            ),
                          ),
                        ),
                  const SizedBox(height: 20),
                  
                  // Duration Display
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          const Text('From', style: TextStyle(color: Colors.grey, fontSize: 12)),
                          Text(
                            DateFormat('HH:mm, dd/MM').format(bookedFrom),
                            style: const TextStyle(fontWeight: FontWeight.bold),
                          ),
                        ],
                      ),
                      const Icon(Icons.arrow_forward_rounded, color: Colors.grey),
                      Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          const Text('Until (Estimated)', style: TextStyle(color: Colors.grey, fontSize: 12)),
                          Text(
                            DateFormat('HH:mm, dd/MM').format(bookedUntil),
                            style: const TextStyle(fontWeight: FontWeight.bold),
                          ),
                        ],
                      ),
                    ],
                  ),
                  const Divider(height: 30),
                  
                  // Estimated Cost
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      const Text('Estimated Cost:', style: TextStyle(fontSize: 16)),
                      Text(
                        NumberFormat.currency(locale: 'vi_VN', symbol: 'đ').format(estimatedCost),
                        style: GoogleFonts.outfit(
                          fontSize: 20,
                          fontWeight: FontWeight.bold,
                          color: Colors.indigo[800],
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 30),
                  
                  // Submit Button
                  ElevatedButton(
                    onPressed: selectedVehicle == null
                        ? null
                        : () async {
                            Navigator.of(context).pop(); // Close bottom sheet
                            
                            // Check wallet balance
                            final authProvider = Provider.of<AuthProvider>(context, listen: false);
                            if ((authProvider.user?.balance ?? 0.0) < estimatedCost) {
                              ScaffoldMessenger.of(context).showSnackBar(
                                const SnackBar(
                                  content: Text('Insufficient balance. Please topup your wallet first.'),
                                  backgroundColor: Colors.redAccent,
                                  behavior: SnackBarBehavior.floating,
                                ),
                              );
                              return;
                            }

                            final error = await bookingProvider.createBooking(
                              slotId: slot.id,
                              vehicleId: selectedVehicle!.id,
                              bookedFrom: bookedFrom,
                              bookedUntil: bookedUntil,
                            );

                            if (error == null) {
                              ScaffoldMessenger.of(context).showSnackBar(
                                SnackBar(
                                  content: const Text('Slot reserved successfully!'),
                                  backgroundColor: Colors.green[600],
                                  behavior: SnackBarBehavior.floating,
                                ),
                              );
                              // Refresh slot maps
                              Provider.of<ParkingProvider>(context, listen: false).fetchSlots();
                            } else {
                              ScaffoldMessenger.of(context).showSnackBar(
                                SnackBar(
                                  content: Text(error),
                                  backgroundColor: Colors.redAccent,
                                  behavior: SnackBarBehavior.floating,
                                ),
                              );
                            }
                          },
                    style: ElevatedButton.styleFrom(
                      backgroundColor: Colors.indigo[800],
                      foregroundColor: Colors.white,
                      padding: const EdgeInsets.symmetric(vertical: 16),
                      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(15)),
                    ),
                    child: const Text('Confirm Booking', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
                  ),
                  const SizedBox(height: 10),
                ],
              ),
            );
          },
        );
      },
    );
  }
}
