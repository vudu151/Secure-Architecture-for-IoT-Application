import React, { useEffect, useState } from 'react';
import { Card, Table, Tag, Badge, Space, message } from 'antd';
import { adminApi } from '../api/adminApi';
import { formatDateTime } from '../constants';

const DevicesPage = () => {
  const [loading, setLoading] = useState(true);
  const [devices, setDevices] = useState([]);

  const loadDevices = async () => {
    try {
      const { data } = await adminApi.getDevices();
      const deviceList = data.data || data;
      setDevices(Array.isArray(deviceList) ? deviceList : []);
      setLoading(false);
    } catch (error) {
      setLoading(false);
      message.error('Không thể tải thông tin thiết bị');
    }
  };

  useEffect(() => {
    loadDevices();

    // Auto-refresh every 30 seconds
    const interval = setInterval(() => {
      loadDevices();
    }, 3000);

    return () => clearInterval(interval);
  }, []);

  const columns = [
    {
      title: 'Mã thiết bị (Device UID)',
      dataIndex: 'deviceUid',
      key: 'deviceUid',
      render: (text) => <code>{text}</code>,
      sorter: (a, b) => a.deviceUid.localeCompare(b.deviceUid),
    },
    {
      title: 'Loại thiết bị',
      dataIndex: 'deviceType',
      key: 'deviceType',
      render: (type) => {
        const isRPi = type === 'RASPBERRY_PI' || type === 'CAMERA' || type === 'BARRIER';
        return (
          <Tag color={isRPi ? 'blue' : 'green'}>
            {type === 'RASPBERRY_PI' ? 'Raspberry Pi (Gate)' : 'ESP32 (Slot)'}
          </Tag>
        );
      },
      filters: [
        { text: 'Raspberry Pi', value: 'RASPBERRY_PI' },
        { text: 'ESP32', value: 'ESP32' },
      ],
      onFilter: (value, record) => record.deviceType === value,
    },
    {
      title: 'Vị trí lắp đặt',
      dataIndex: 'location',
      key: 'location',
      render: (text) => text || 'Chưa cập nhật',
    },
    {
      title: 'Trạng thái',
      dataIndex: 'isOnline',
      key: 'isOnline',
      render: (isOnline) => (
        <Badge
          status={isOnline ? 'success' : 'error'}
          text={isOnline ? 'Trực tuyến (Online)' : 'Ngoại tuyến (Offline)'}
        />
      ),
      filters: [
        { text: 'Online', value: true },
        { text: 'Offline', value: false },
      ],
      onFilter: (value, record) => record.isOnline === value,
    },
    {
      title: 'Tín hiệu cuối (Last Heartbeat)',
      dataIndex: 'lastHeartbeat',
      key: 'lastHeartbeat',
      render: (text) => formatDateTime(text),
    },
    {
      title: 'Phiên bản Firmware',
      dataIndex: 'firmwareVersion',
      key: 'firmwareVersion',
      render: (text) => <Tag color="purple">{text || 'v1.0.0'}</Tag>,
    },
  ];

  return (
    <Card className="glass-card" title="Danh sách thiết bị trong hệ thống" bordered={false}>
      <Table
        dataSource={devices}
        columns={columns}
        rowKey="id"
        loading={loading}
        pagination={{ pageSize: 10 }}
        size="middle"
      />
    </Card>
  );
};

export default DevicesPage;
