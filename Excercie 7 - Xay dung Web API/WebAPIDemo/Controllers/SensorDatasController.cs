using Microsoft.AspNetCore.Mvc;
using WebAPIDemo.Models;
using WebAPIDemo.Services;

namespace WebAPIDemo.Controllers
{
    /// <summary>
    /// Controller API quản lý dữ liệu cảm biến IoT
    /// </summary>
    [Route("api/[controller]")]
    [ApiController]
    public class SensorDatasController : ControllerBase
    {
        private readonly SensorDataService _sensorDataService;

        public SensorDatasController(SensorDataService sensorDataService)
        {
            _sensorDataService = sensorDataService;
        }

        /// <summary>Lấy danh sách tất cả dữ liệu cảm biến</summary>
        [HttpGet]
        public async Task<ActionResult<List<SensorData>>> Get()
        {
            var data = await _sensorDataService.GetAllAsync();
            return Ok(data);
        }

        /// <summary>Lấy dữ liệu cảm biến theo Id</summary>
        [HttpGet("{id:int}")]
        public async Task<ActionResult<SensorData>> Get(int id)
        {
            var sensorData = await _sensorDataService.GetByIdAsync(id);
            if (sensorData == null)
                return NotFound(new { message = $"Không tìm thấy SensorData với Id = {id}" });

            return Ok(sensorData);
        }

        /// <summary>Lấy dữ liệu cảm biến theo DeviceId</summary>
        [HttpGet("device/{deviceId}")]
        public async Task<ActionResult<List<SensorData>>> GetByDevice(string deviceId)
        {
            var data = await _sensorDataService.GetByDeviceIdAsync(deviceId);
            return Ok(data);
        }

        /// <summary>Thêm dữ liệu cảm biến mới</summary>
        /// <remarks>
        /// Body mẫu:
        /// {
        ///   "deviceId": "DEVICE_001",
        ///   "temperature": 30.5,
        ///   "humidity": 70.2,
        ///   "createdAt": "2026-05-09T10:00:00"
        /// }
        /// </remarks>
        [HttpPost]
        public async Task<ActionResult<SensorData>> Post(SensorData sensorData)
        {
            var created = await _sensorDataService.CreateAsync(sensorData);
            return CreatedAtAction(nameof(Get), new { id = created.Id }, created);
        }

        /// <summary>Cập nhật dữ liệu cảm biến theo Id</summary>
        [HttpPut("{id:int}")]
        public async Task<IActionResult> Put(int id, SensorData sensorData)
        {
            var updated = await _sensorDataService.UpdateAsync(id, sensorData);
            if (!updated)
                return NotFound(new { message = $"Không tìm thấy SensorData với Id = {id}" });

            return NoContent();
        }

        /// <summary>Xóa dữ liệu cảm biến theo Id</summary>
        [HttpDelete("{id:int}")]
        public async Task<IActionResult> Delete(int id)
        {
            var deleted = await _sensorDataService.DeleteAsync(id);
            if (!deleted)
                return NotFound(new { message = $"Không tìm thấy SensorData với Id = {id}" });

            return NoContent();
        }
    }
}
