namespace WebAPIDemo.Models
{
    /// <summary>
    /// Model lưu thông tin phiên bản thiết bị/firmware
    /// </summary>
    public class AppVersion
    {
        public int Id { get; set; }

        public string DeviceId { get; set; } = string.Empty;

        public int Version { get; set; }

        public DateTime UpdatedAt { get; set; } = DateTime.Now;
    }
}
