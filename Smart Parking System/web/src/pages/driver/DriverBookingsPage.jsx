import React, { useEffect, useState } from 'react';
import {
  Card, Table, Tag, Typography, Button, Space, message, Popconfirm, Empty, Spin, Modal, QRCode, Divider,
} from 'antd';
import { ReloadOutlined, CloseOutlined, CarOutlined, QrcodeOutlined, ScanOutlined } from '@ant-design/icons';
import { bookingApi } from '../../api/bookingApi';
import axiosInstance from '../../api/axiosInstance';
import dayjs from 'dayjs';

const { Text } = Typography;

const statusConfig = {
  PENDING:    { color: 'gold',    label: 'Đang chờ' },
  CONFIRMED:  { color: 'blue',    label: 'Đã xác nhận' },
  CHECKED_IN: { color: 'green',   label: 'Đã vào bãi' },
  CHECKED_OUT:{ color: 'cyan',    label: 'Đã ra bãi' },
  COMPLETED:  { color: 'default', label: 'Hoàn thành' },
  CANCELLED:  { color: 'red',     label: 'Đã hủy' },
  EXPIRED:    { color: 'default', label: 'Hết hạn' },
};

const DriverBookingsPage = () => {
  const [bookings, setBookings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [cancelling, setCancelling] = useState(null);
  const [qrModalVisible, setQrModalVisible] = useState(false);
  const [selectedBooking, setSelectedBooking] = useState(null);

  const fetchBookings = async () => {
    setLoading(true);
    try {
      const res = await bookingApi.getMyBookings({ page: 0, size: 50 });
      const raw = res.data?.data ?? res.data;
      setBookings(Array.isArray(raw) ? raw : (raw?.content ?? []));
    } catch (e) {
      message.error('Không thể tải danh sách đặt chỗ');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchBookings();
  }, []);

  const handleCancel = async (id) => {
    setCancelling(id);
    try {
      await bookingApi.cancelBooking(id);
      message.success('Đã hủy đặt chỗ thành công');
      fetchBookings();
    } catch (e) {
      message.error(e.response?.data?.message || 'Hủy đặt chỗ thất bại');
    } finally {
      setCancelling(null);
    }
  };

  const showQrCode = (record) => {
    setSelectedBooking(record);
    setQrModalVisible(true);
  };

  const handleSimulateScan = async () => {
    if (!selectedBooking) return;
    try {
      message.loading({ content: 'Đang mô phỏng quét mã tại cổng...', key: 'scan' });
      await axiosInstance.post(`/api/v1/devices/verify-qr?gateId=gate1&qrData=${encodeURIComponent(selectedBooking.qrCodeData)}`);
      message.success({ content: 'Quét mã thành công! Barrier đang mở!', key: 'scan', duration: 3 });
      setQrModalVisible(false);
      fetchBookings(); // Tải lại để cập nhật trạng thái mới (CHECKED_IN)
    } catch (e) {
      message.error({ content: e.response?.data?.message || 'Quét mã thất bại', key: 'scan', duration: 3 });
    }
  };

  const columns = [
    {
      title: 'Mã đặt chỗ',
      dataIndex: 'bookingCode',
      key: 'bookingCode',
      render: (v) => <Text code style={{ fontSize: 12 }}>{v}</Text>,
    },
    {
      title: 'Chỗ đỗ',
      dataIndex: 'slotCode',
      key: 'slotCode',
      render: (v) => (
        <Tag color="blue" icon={<CarOutlined />}>
          {v || '#undefined'}
        </Tag>
      ),
    },
    {
      title: 'Biển số xe',
      dataIndex: 'vehiclePlate',
      key: 'vehiclePlate',
      render: (v) => <Text strong>{v || '#undefined'}</Text>,
    },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      key: 'status',
      render: (status) => {
        const cfg = statusConfig[status] || { color: 'default', label: status };
        return <Tag color={cfg.color}>{cfg.label}</Tag>;
      },
      filters: Object.entries(statusConfig).map(([k, v]) => ({ text: v.label, value: k })),
      onFilter: (value, record) => record.status === value,
    },
    {
      title: 'Từ',
      dataIndex: 'bookedFrom',
      key: 'bookedFrom',
      render: (v) => v ? dayjs(v).format('DD/MM/YYYY HH:mm') : '-',
      sorter: (a, b) => new Date(a.bookedFrom) - new Date(b.bookedFrom),
    },
    {
      title: 'Đến',
      dataIndex: 'bookedUntil',
      key: 'bookedUntil',
      render: (v) => v ? dayjs(v).format('DD/MM/YYYY HH:mm') : '-',
    },
    {
      title: 'Tổng tiền',
      dataIndex: 'totalAmount',
      key: 'totalAmount',
      render: (v) => v != null ? `${Number(v).toLocaleString('vi-VN')} ₫` : '-',
      align: 'right',
    },
    {
      title: 'Hành động',
      key: 'action',
      render: (_, record) => {
        const canCancel = ['PENDING', 'CONFIRMED'].includes(record.status);
        const hasQr = record.qrCodeData && record.status !== 'CANCELLED';
        return (
          <Space>
            {hasQr && (
              <Button 
                type="primary" 
                size="small" 
                icon={<QrcodeOutlined />}
                onClick={() => showQrCode(record)}
              >
                Mã QR
              </Button>
            )}
            {canCancel && (
              <Popconfirm
                title="Hủy đặt chỗ?"
                description="Bạn có chắc muốn hủy đặt chỗ này không?"
                onConfirm={() => handleCancel(record.id)}
                okText="Hủy đặt chỗ"
                cancelText="Không"
                okButtonProps={{ danger: true }}
              >
                <Button
                  danger
                  size="small"
                  icon={<CloseOutlined />}
                  loading={cancelling === record.id}
                >
                  Hủy
                </Button>
              </Popconfirm>
            )}
          </Space>
        );
      },
    },
  ];

  return (
    <>
      <Card
        title="Lịch sử đặt chỗ"
        extra={
          <Button icon={<ReloadOutlined />} onClick={fetchBookings} loading={loading}>
            Làm mới
          </Button>
        }
        style={{ borderRadius: 12, boxShadow: '0 2px 8px rgba(0,0,0,0.06)' }}
      >
        {loading ? (
          <div style={{ textAlign: 'center', padding: 40 }}><Spin size="large" /></div>
        ) : bookings.length === 0 ? (
          <Empty description="Bạn chưa có lần đặt chỗ nào" />
        ) : (
          <Table
            dataSource={bookings}
            columns={columns}
            rowKey="id"
            pagination={{ pageSize: 10, showSizeChanger: true }}
            scroll={{ x: 900 }}
          />
        )}
      </Card>

      <Modal
        title="Mã QR ra vào bãi đỗ"
        open={qrModalVisible}
        onCancel={() => setQrModalVisible(false)}
        footer={[
          <Button key="close" onClick={() => setQrModalVisible(false)}>
            Đóng
          </Button>
        ]}
        centered
      >
        {selectedBooking && (
          <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', textAlign: 'center', padding: '20px 0' }}>
            <QRCode
              value={selectedBooking.qrCodeData || ''}
              size={200}
              status={selectedBooking.status === 'EXPIRED' ? 'expired' : 'active'}
            />
            <div style={{ marginTop: 20 }}>
              <Text strong style={{ fontSize: 16 }}>{selectedBooking.bookingCode}</Text>
              <br />
              <Text type="secondary">Chỗ đỗ: {selectedBooking.slotCode} - Biển số: {selectedBooking.vehiclePlate}</Text>
              <br />
              <Text type="secondary">
                Hiệu lực: {dayjs(selectedBooking.bookedFrom).format('HH:mm DD/MM')} - {dayjs(selectedBooking.bookedUntil).format('HH:mm DD/MM')}
              </Text>
            </div>
            <div style={{ marginTop: 15 }}>
              <Tag color="cyan">Vui lòng quét mã này tại cổng Barrier để ra/vào bãi</Tag>
            </div>
            
            <Divider dashed />
            
            <Button 
              type="primary" 
              icon={<ScanOutlined />} 
              onClick={handleSimulateScan}
              disabled={selectedBooking.status === 'EXPIRED' || selectedBooking.status === 'CANCELLED'}
              style={{ backgroundColor: '#52c41a', borderColor: '#52c41a' }}
            >
              Demo: Quét mã này tại cổng
            </Button>
            <div style={{ marginTop: 8 }}>
              <Text type="secondary" style={{ fontSize: 12 }}>
                (Nút này dùng để giả lập hành động quét QR tại cổng vật lý)
              </Text>
            </div>
          </div>
        )}
      </Modal>
    </>
  );
};

export default DriverBookingsPage;
