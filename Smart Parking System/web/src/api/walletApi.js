import axiosInstance from './axiosInstance';

export const walletApi = {
  getBalance: () =>
    axiosInstance.get('/wallet/balance'),

  topup: (amount) =>
    axiosInstance.post('/wallet/topup', { amount }),

  getTransactions: (params) =>
    axiosInstance.get('/wallet/transactions', { params }),
};

export default walletApi;
