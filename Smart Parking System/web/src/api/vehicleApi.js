import axiosInstance from './axiosInstance';

export const vehicleApi = {
  getMyVehicles: () =>
    axiosInstance.get('/vehicles/my'),

  addVehicle: (data) =>
    axiosInstance.post('/vehicles', data),

  deleteVehicle: (id) =>
    axiosInstance.delete(`/vehicles/${id}`),
};

export default vehicleApi;
