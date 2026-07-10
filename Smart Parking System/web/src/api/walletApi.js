import axiosInstance from './axiosInstance';

export const walletApi = {
  getBalance: () =>
    axiosInstance.get('/wallet/balance'),

  topup: (amount) =>
    axiosInstance.post('/wallet/topup', { amount }),

  getMyTransactions: (params) =>
    axiosInstance.get('/transactions/my', { params }),
};

export default walletApi;
