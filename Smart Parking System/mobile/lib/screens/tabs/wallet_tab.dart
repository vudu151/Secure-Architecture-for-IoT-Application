import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:provider/provider.dart';
import 'package:intl/intl.dart';
import '../../providers/auth_provider.dart';
import '../../providers/booking_provider.dart';
import '../../models/transaction_model.dart';

class WalletTab extends StatelessWidget {
  const WalletTab({super.key});

  @override
  Widget build(BuildContext context) {
    final authProvider = Provider.of<AuthProvider>(context);
    final bookingProvider = Provider.of<BookingProvider>(context);

    return Scaffold(
      backgroundColor: Colors.grey[100],
      appBar: AppBar(
        title: Text(
          'My Wallet',
          style: GoogleFonts.outfit(fontWeight: FontWeight.bold, color: Colors.indigo[900]),
        ),
        backgroundColor: Colors.white,
        elevation: 0,
        actions: [
          IconButton(
            icon: Icon(Icons.refresh, color: Colors.indigo[900]),
            onPressed: () {
              bookingProvider.fetchTransactions();
              authProvider.checkAuth();
            },
          )
        ],
      ),
      body: bookingProvider.isLoading
          ? const Center(child: CircularProgressIndicator())
          : Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                // 1. Virtual Card
                _buildVirtualCard(context, authProvider),
                
                // 2. Quick Actions
                Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 10),
                  child: Row(
                    children: [
                      Expanded(
                        child: ElevatedButton.icon(
                          onPressed: () => _showTopUpDialog(context),
                          icon: const Icon(Icons.add_circle_outline, size: 20),
                          label: const Text('Top Up Wallet'),
                          style: ElevatedButton.styleFrom(
                            backgroundColor: Colors.indigo[800],
                            foregroundColor: Colors.white,
                            padding: const EdgeInsets.symmetric(vertical: 14),
                            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
                
                // 3. Transactions Section Title
                const Padding(
                  padding: EdgeInsets.only(left: 20.0, top: 15.0, bottom: 8.0),
                  child: Text(
                    'Transaction History',
                    style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16, color: Colors.black85),
                  ),
                ),
                
                // 4. Transactions List
                Expanded(
                  child: bookingProvider.transactions.isEmpty
                      ? Center(
                          child: Column(
                            mainAxisAlignment: MainAxisAlignment.center,
                            children: [
                              Icon(Icons.history_toggle_off_rounded, size: 50, color: Colors.grey[400]),
                              const SizedBox(height: 5),
                              const Text('No transactions recorded yet', style: TextStyle(color: Colors.grey)),
                            ],
                          ),
                        )
                      : ListView.builder(
                          padding: const EdgeInsets.symmetric(horizontal: 20),
                          itemCount: bookingProvider.transactions.length,
                          itemBuilder: (context, index) {
                            final tx = bookingProvider.transactions[index];
                            return _buildTransactionItem(tx);
                          },
                        ),
                ),
              ],
            ),
    );
  }

  Widget _buildVirtualCard(BuildContext context, AuthProvider auth) {
    final currencyFormat = NumberFormat.currency(locale: 'vi_VN', symbol: 'đ');
    
    return Container(
      margin: const EdgeInsets.all(20),
      height: 200,
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(25),
        gradient: const LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [
            Color(0xFF6366F1), // Indigo
            Color(0xFF4F46E5), // Indigo dark
            Color(0xFF312E81), // Deep navy
          ],
        ),
        boxShadow: [
          BoxShadow(
            color: Colors.indigo[800]!.withOpacity(0.3),
            blurRadius: 12,
            offset: const Offset(0, 6),
          )
        ],
      ),
      padding: const EdgeInsets.all(25),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                'Smart Parking Wallet',
                style: GoogleFonts.outfit(
                  color: Colors.white70,
                  fontSize: 14,
                  fontWeight: FontWeight.w500,
                  letterSpacing: 1.0,
                ),
              ),
              const Icon(Icons.wifi_rounded, color: Colors.white70, size: 24),
            ],
          ),
          Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Text(
                'CURRENT BALANCE',
                style: TextStyle(color: Colors.white54, fontSize: 10, letterSpacing: 0.8),
              ),
              const SizedBox(height: 2),
              Text(
                currencyFormat.format(auth.user?.balance ?? 0),
                style: GoogleFonts.outfit(
                  color: Colors.white,
                  fontSize: 28,
                  fontWeight: FontWeight.bold,
                ),
              ),
            ],
          ),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                auth.user?.fullName.toUpperCase() ?? 'SMART PARKING DRIVER',
                style: GoogleFonts.outfit(
                  color: Colors.white,
                  fontSize: 14,
                  fontWeight: FontWeight.bold,
                  letterSpacing: 1.0,
                ),
              ),
              // Dummy card logo
              Container(
                width: 45,
                height: 30,
                decoration: BoxDecoration(
                  color: Colors.white.withOpacity(0.15),
                  borderRadius: BorderRadius.circular(8),
                ),
                child: Center(
                  child: Text(
                    'RFID',
                    style: GoogleFonts.outfit(color: Colors.white, fontSize: 10, fontWeight: FontWeight.bold),
                  ),
                ),
              ),
            ],
          )
        ],
      ),
    );
  }

  Widget _buildTransactionItem(TransactionModel tx) {
    final isTopUp = tx.transactionRef.startsWith('TOPUP');
    final currencyFormat = NumberFormat.currency(locale: 'vi_VN', symbol: 'đ');

    return Card(
      elevation: 0,
      color: Colors.white,
      margin: const EdgeInsets.only(bottom: 10),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      child: ListTile(
        leading: Container(
          padding: const EdgeInsets.all(8),
          decoration: BoxDecoration(
            color: isTopUp ? Colors.green[50] : Colors.red[50],
            shape: BoxShape.circle,
          ),
          child: Icon(
            isTopUp ? Icons.arrow_upward_rounded : Icons.arrow_downward_rounded,
            color: isTopUp ? Colors.green[700] : Colors.red[700],
            size: 20,
          ),
        ),
        title: Text(
          isTopUp ? 'Wallet Top Up' : 'Parking Charge Checkout',
          style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 14),
        ),
        subtitle: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              DateFormat('HH:mm, dd/MM/yyyy').format(tx.createdAt),
              style: TextStyle(color: Colors.grey[500], fontSize: 11),
            ),
            if (tx.bookingCode != null) ...[
              const SizedBox(height: 2),
              Text(
                'Ticket Ref: ${tx.bookingCode}',
                style: TextStyle(color: Colors.indigo[300], fontSize: 10, fontWeight: FontWeight.bold),
              )
            ]
          ],
        ),
        trailing: Text(
          '${isTopUp ? "+" : "-"} ${currencyFormat.format(tx.amount)}',
          style: GoogleFonts.outfit(
            fontWeight: FontWeight.bold,
            fontSize: 15,
            color: isTopUp ? Colors.green[700] : Colors.red[700],
          ),
        ),
      ),
    );
  }

  void _showTopUpDialog(BuildContext context) {
    final amountController = TextEditingController();
    final bookingProvider = Provider.of<BookingProvider>(context, listen: false);
    final authProvider = Provider.of<AuthProvider>(context, listen: false);

    showDialog(
      context: context,
      builder: (context) {
        return AlertDialog(
          title: Text(
            'Wallet Top Up',
            style: GoogleFonts.outfit(fontWeight: FontWeight.bold, color: Colors.indigo[900]),
          ),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              const Text('Enter amount to top up your virtual RFID wallet (VND):'),
              const SizedBox(height: 15),
              TextField(
                controller: amountController,
                keyboardType: TextInputType.number,
                decoration: InputDecoration(
                  labelText: 'Amount (đ)',
                  prefixIcon: const Icon(Icons.account_balance_wallet_outlined),
                  border: OutlineInputBorder(borderRadius: BorderRadius.circular(10)),
                ),
              ),
              const SizedBox(height: 15),
              // Quick Amounts
              Wrap(
                spacing: 10,
                children: [50000, 100000, 200000].map((val) {
                  return ActionChip(
                    label: Text(NumberFormat.compact().format(val)),
                    onPressed: () {
                      amountController.text = val.toString();
                    },
                  );
                }).toList(),
              ),
            ],
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.of(context).pop(),
              child: const Text('Cancel'),
            ),
            ElevatedButton(
              onPressed: () async {
                final double? amt = double.tryParse(amountController.text);
                if (amt == null || amt <= 0) {
                  ScaffoldMessenger.of(context).showSnackBar(
                    const SnackBar(content: Text('Please enter a valid amount')),
                  );
                  return;
                }

                Navigator.of(context).pop();

                final newBalance = await bookingProvider.topup(amt);
                if (newBalance != null) {
                  // Update AuthProvider balance state
                  authProvider.updateBalance(newBalance);
                  ScaffoldMessenger.of(context).showSnackBar(
                    SnackBar(
                      content: Text('Topped up successfully! New balance: ${NumberFormat.currency(locale: "vi_VN", symbol: "đ").format(newBalance)}'),
                      backgroundColor: Colors.green,
                    ),
                  );
                } else {
                  ScaffoldMessenger.of(context).showSnackBar(
                    const SnackBar(content: Text('Topup failed. Please check connection.')),
                  );
                }
              },
              style: ElevatedButton.styleFrom(backgroundColor: Colors.indigo[800], foregroundColor: Colors.white),
              child: const Text('Top Up'),
            ),
          ],
        );
      },
    );
  }
}
