using Microsoft.EntityFrameworkCore;
using WebAPIDemo.Models;

namespace WebAPIDemo.Services
{
    /// <summary>
    /// Service xử lý nghiệp vụ cho SensorData với SQL Server
    /// </summary>
    public class SensorDataService
    {
        private readonly WebAPIDemoContext _context;

        public SensorDataService(WebAPIDemoContext context)
        {
            _context = context;
        }

        /// <summary>Lấy tất cả dữ liệu cảm biến</summary>
        public async Task<List<SensorData>> GetAllAsync()
        {
            return await _context.SensorDatas
                .OrderByDescending(s => s.CreatedAt)
                .ToListAsync();
        }

        /// <summary>Lấy dữ liệu cảm biến theo Id</summary>
        public async Task<SensorData?> GetByIdAsync(int id)
        {
            return await _context.SensorDatas.FindAsync(id);
        }

        /// <summary>Lấy danh sách dữ liệu theo DeviceId</summary>
        public async Task<List<SensorData>> GetByDeviceIdAsync(string deviceId)
        {
            return await _context.SensorDatas
                .Where(s => s.DeviceId == deviceId)
                .OrderByDescending(s => s.CreatedAt)
                .ToListAsync();
        }

        /// <summary>Thêm dữ liệu cảm biến mới</summary>
        public async Task<SensorData> CreateAsync(SensorData sensorData)
        {
            sensorData.CreatedAt = DateTime.Now;
            _context.SensorDatas.Add(sensorData);
            await _context.SaveChangesAsync();
            return sensorData;
        }

        /// <summary>Cập nhật dữ liệu cảm biến</summary>
        public async Task<bool> UpdateAsync(int id, SensorData sensorData)
        {
            var existing = await _context.SensorDatas.FindAsync(id);
            if (existing == null) return false;

            existing.DeviceId = sensorData.DeviceId;
            existing.Temperature = sensorData.Temperature;
            existing.Humidity = sensorData.Humidity;
            existing.CreatedAt = sensorData.CreatedAt;

            await _context.SaveChangesAsync();
            return true;
        }

        /// <summary>Xóa dữ liệu cảm biến</summary>
        public async Task<bool> DeleteAsync(int id)
        {
            var sensorData = await _context.SensorDatas.FindAsync(id);
            if (sensorData == null) return false;

            _context.SensorDatas.Remove(sensorData);
            await _context.SaveChangesAsync();
            return true;
        }
    }
}
